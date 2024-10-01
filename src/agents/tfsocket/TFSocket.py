import sys
import socket
import os
import random
import traceback
import math
import re
from TFSocketModel import TFSocketModel as tfmodel
from TFSocketEnv import TFSocketEnv as tfenv
from TFSocketUtils import log

'''
This is the main script for the TFSocket python agent
TFSocket communicates with the java-side proxy agent with the FSM framework

Note: all output from this script is printed to a file named 'pyout.txt'
    since the output of any print statements will be lost.

Commonly used variable names:
    conn: socket used to communicate with the java-side agent
    environment: instance of TFSocketEnv that describes the environment variables
    model: instance of the TFSocketModel class that has methods for training ANN
        using the agent's experience
    window: a subset of the agent's entire action history; the agent's last N
        actions where N = WINDOW_SIZE
'''

def send_letter(conn, letter, environment):
    '''
    Send an action to the environment through the socket
    If letter = '*' send a random action
    '''
    if not environment.alphabet:
        # log('ALPHABET has not been intialized')
        return None

    if letter == '*':
        letter = random.choice(environment.alphabet)
    # log(f'sending {letter}')
    conn.sendall(letter.encode('ASCII'))
    return letter

def calculate_epsilon(environment):
    '''
    Calculate an epsilon value from the number of steps the agent has taken, via a sigmoid function
    '''
    x = environment.num_goals # Update the sigmoid function's "x" value
    if environment.epsilon <= 0.0: # Initialize epsilon
        environment.epsilon ==  0.0
    
    # Find the rolling avg of steps_to_goal for the last 5 goals
    num_goals_to_avg = 5
    pattern = rf'([a-z]*[A-Z]){{{num_goals_to_avg}}}(?=[a-z]*$)'
    match = re.search(pattern, str(environment.entire_history))
    substring = match.group(0) if match else ''
    rolling_avg_steps = len(substring) / num_goals_to_avg
    
    # Last iteration's alert, unlearning_alert default value is True
    # prev_alert = environment.unlearning_alert
    # Last iteration's perc_unlearning, perc_unlearning default value is 0.0
    prev_perc_unlearning = environment.perc_unlearning

    # Calculate the rate of "unlearning" as a percentage of the total_avg
    environment.perc_unlearning = max(math.log(rolling_avg_steps/environment.avg_steps, 2), 0)
    
    # Oh no! The agent is "unlearning" (performing worse over time)
    if not prev_perc_unlearning > 0 and environment.perc_unlearning > 0:
        log('The agent appears to be unlearning')
        # log('Generating negative sigmoid function')
        log(f'perc_unlearning = {environment.perc_unlearning:.3f}')
        environment.h_shift = 7 + x # Offset by 7 for a TRAINING_THRESHOLD = 10
        environment.upper_bound = environment.epsilon + environment.perc_unlearning
    
    # Yay! The agent is improving and no longer "unlearning"
    elif prev_perc_unlearning > 0 and not environment.perc_unlearning > 0:
        log('The agent appears to be learning')
    
    # Dang! The agent is still unlearning
    elif prev_perc_unlearning > 0 and environment.perc_unlearning > 0:
        # If the agent in "unlearning" at an increasing rate, then increase epsilon by the same amount
        if environment.perc_unlearning > prev_perc_unlearning:
            environment.upper_bound += (environment.perc_unlearning - prev_perc_unlearning)

    # Calculate the next interation of epsilon with the sigmoid function    
    environment.epsilon = environment.upper_bound * (1 / (1 + math.exp((x - environment.h_shift))))

def process_history_sentinel(strData, environment, models, model_window_sizes):
    '''
    First use strData to update the TFSocketEnv object's variables.
    Then create and train a list of models of various window_sizes.
    Select the model in the list that will reach the goal in the fewest steps.
    Use the min_steps_model to commit the agent's action to the environment.

    Params:
        strData, the agent's window (of size WINDOW_SIZE) of the total history from the java agent
        environment, TFSocketEnv object of the environment's details
        models, list (hardcoded length of 3) of TFSocketModel objects of various window_sizes
    '''
    # Constant vars
    TRAINING_THRESHOLD = 10 # Number of goals to find randomly before training the models
    MIN_HISTORY_LENGTH = 10 # Mimimum history length that the model requires to train
        
    # Update environment variables
    environment = update_environment(strData, environment)
    loop_threshold = 2*environment.avg_steps # Suspect looping threshold

    # Use the random pseudo-model until we've collected enough data to train on
    if (
        environment.num_goals < TRAINING_THRESHOLD
        or len(environment.entire_history) < MIN_HISTORY_LENGTH
    ):
        environment.last_step = '*' # Select the random pseudo-model
        return models # Return early to send random action

    # If looping is suspected, use the random sudo-model and set all models to None for retraining
    elif environment.steps_since_last_goal >= loop_threshold:
        # Reset the models for training
        if environment.steps_since_last_goal == loop_threshold:
            # log('Looks like the agent is stuck in a loop! Switching to random pseudo-model')
            # log(f'avg_steps={environment.avg_steps:.2f}, steps_since_last_goal={environment.steps_since_last_goal}')
            for index in range(len(models)):
                models[index] = None

        # Enable the random sudo-model
        environment.last_step = '*'
        return models # Return early to send random action

    # If we reached a goal, we need to retrain all the models
    # TODO: put this in a helper method
    if strData[-1:].isupper():
        # Calculate epilson & perform E-Greedy exploit/explore decision making
        calculate_epsilon(environment)
        if random.random() < environment.epsilon:
            environment.last_step = '*'
            return models # Return early to send random action
        
        # Find the model that predicted this goal
        predicting_model = None
        for model in models: # NOTE: This will favor lower indexed models
            if (
                model is not None
                and model.sim_path is not None
                and len(model.sim_path) == 0
            ):
                predicting_model = model
                break
        
        # Adjust model sizes
        # TODO: Try something binary-search-like?
        if predicting_model is not None and predicting_model.window_size >= 2:
            # log(f'new model sizes centered around {predicting_model.window_size}')
            model_window_sizes[0] = predicting_model.window_size - 1
            model_window_sizes[1] = predicting_model.window_size
            model_window_sizes[2] = predicting_model.window_size + 1

        # Re-simulate models that correctly simulated the goal and trigger a retrain for the other models
        for index in range(len(models)):
            if models[index] is not None and models[index] == predicting_model:
                models[index].simulate_model() # resim model used to find the goal
            else:
                models[index] = None # trigger a model retrain

    
    # Create and train models of various window_sizes then select
    # Which model simulates the shortest path to a goal
    models, min_path_model = manage_models(environment, models, model_window_sizes)

    # Select the next action from the shortest min_path_model, then adjust the sim_paths for all models
    models = get_best_prediction(environment, models, min_path_model)

    return models

def update_environment(strData, environment):
    '''
    Update environment variables from strData:
        entire_history
        steps_since_last_goal
        num_goals
        avg_steps

    Params:
        strData, the agent's window (of size WINDOW_SIZE) of the total history from the java agent
        environment, TFSocketEnv object of the environment's details
    Returns: The updated TFSocketEnv object
    '''
    # Update entire_history
    window = strData[11:]
    # log(f'Window received: {window}')
    if len(window) == 1:
        window = window.lower()
    environment.entire_history = environment.entire_history + str(window[-1:])

    # Update num_goals and steps_since_last_goal
    environment.steps_since_last_goal +=1
    if window[-1:].isupper():
        log(f'The agent found Goal #{environment.num_goals:<3} in {environment.steps_since_last_goal:>3} steps')
        if environment.epsilon != -1.0: # epsilon is not its default value
            log(f'epsilon: {environment.epsilon:.3f}')
            # log(f'x: {environment.num_goals}\nh_shift: {environment.h_shift}\nbounds: [0,{environment.upper_bound:.3f}]\n')
        environment.num_goals += 1
        environment.steps_since_last_goal = 0

    # Update average steps per goal
    environment.update_avg_steps()

    return environment

def manage_models(environment, models, model_window_sizes):
    '''
    1. Train each model
    2. Simulate each model
    3. Select the best model

    Returns:
        models, list of TFSocketModel objects of various window_sizes
        min_path_model, the model with the shortest sim_path in models
    '''
    min_path_model = None
    max_iterations = 2 # Infinite loop precaution, should never need to run more than twice.
    index = 0
    while (min_path_model is None) and (index < max_iterations):
        # Create models using entire history
        models = train_models(environment, models, model_window_sizes)
        # Find the model with the shortest sim_path
        min_path_model = select_min_path_model(models, min_path_model)
        index += 1
    if min_path_model is None:
        log('ERROR: The training and selecting of models was unsuccessfull.')

    return models, min_path_model

def train_models(environment, models, model_window_sizes):
    '''
    If the random-action data gathering is complete, create a list of models
    using the environment's entire_history and a window_size defined by model_window_sizes

    Example: models[i].win_size = model_window_sizes[i]
        If model_window_sizes = [4, 5, 6], then...
            model[0].win_size = model_window_sizes[0] # = 4
            model[1].win_size = model_window_sizes[1] # = 5
            model[2].win_size = model_window_sizes[2] # = 6
    '''
    if all(model is None for model in models):
        # log(f'Training models')
        # If we've reached the end of random-action data gathering, create models using entire history
        for index in range(len(model_window_sizes)):
            models[index] = tfmodel(environment, model_window_sizes[index])
            models[index].train_model()
            models[index].simulate_model()

    return models

def select_min_path_model(models, min_path_model):
    '''
    Finds which model in models had the shortest sim_path and returns the model
    If any models have an empty sim_path, then set the model to None for it to be retrained
    
    If all models have an empty sim_path, then return None
    process_history_sentinal() will then retrain and re-run this function with
    a new list of models that are gaurenteed to have sim_paths of length >= 1
    '''
    for index in range(len(models)):
        if models[index] is not None:
            # At this point if a model is NOT None it is garenteed to have a sim_path that is NOT None
            if len(models[index].sim_path) <= 0:
                # Set to None so the model can be re-trained
                models[index] = None
                continue

            if min_path_model is None:
                min_path_model = models[index]
            # NOTE: '<=' favors larger indexed models, and '<' favors smaller index models
            elif len(models[index].sim_path) < len(min_path_model.sim_path):
                min_path_model = models[index]

    # Could possibly return None if all models are None
    return min_path_model

def get_best_prediction(environment, models, min_path_model):
    '''
    Update environment.last_step with the most recent prediction from the best
    model with the shortest sim_path.
    Then adjust each model's sim_path to reflect the environment change.
    '''
    # If there is a trained model, use it to select the next action
    if min_path_model is None:
        log(f'Error: All models are None; The training and selecting of models was unsuccessfull.')
        return models

    environment.last_step = min_path_model.sim_path[0].lower()

    # Adjust sim_paths for all models based on selected action
    for index in range(len(models)):
        if models[index] is not None:
            # log(f'Model (window_size={models[index].window_size}); sim_path to goal: {models[index].sim_path}')
            action = models[index].sim_path[0]
            if action.lower() != environment.last_step:
                # This model's prediction does not match action selected by min_path_model
                if models[index] is not None:
                    models[index].simulate_model()
            else:
                models[index].sim_path = models[index].sim_path[1:]

    return models

def main():
    '''
    main
    '''
    conn = None

    if os.path.isfile('pyout.txt'):
        os.remove('pyout.txt')
    log('Running Python-TensorFlow agent')

    argc = len(sys.argv)
    if argc < 2:
        print('ERROR: Must pass port number as a parameter.')

    # Creating the python side of the socket
    portNum = int(sys.argv[1])
    timeout = 0
    log(f'port number: {portNum}')
    log('Creating server for the Java environment to connect to...')

    # Initialize FSM scoped variables
    environment = tfenv()
    models = [None, None, None]
    # Defines how many models and the win_size param for each model
    # These numbers will change dynamically as the agent discovers a near-optimal size
    model_window_sizes = [4, 5, 6] # models[i].win_size = model_window_sizes[i] 
    # Chance of taking a random action
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.bind(('127.0.0.1', portNum))
            sock.listen()
            conn, addr = sock.accept()
            with conn:
                log(f'Connected by {addr}')
                while True:
                    data = conn.recv(1024)

                    while not data:
                        data = conn.recv(1024)
                        timeout += 10
                        if timeout > 1000:
                            log('ERROR: Timeout receiving next input from Java environment')
                            exit(-1)
                    # Connected to socket

                    # Handling socket communication
                    # Check for sentinels
                    strData = data.decode('utf-8') # Raw data from the java agent

                    if strData.startswith('$$$alphabet:'):
                        environment.alphabet = list(strData[12:])
                        environment.overall_alphabet = environment.alphabet + [let.upper() for let in environment.alphabet]
                        log(f'New alphabet: {environment.alphabet}')
                        log(f'Sending \'ack\'') # Acknowledge
                        conn.sendall('$$$ack'.encode('ASCII'))

                    elif strData.startswith('$$$history:'):
                        models = process_history_sentinel(strData, environment, models, model_window_sizes)
                        # Send the model's prediction to the environment
                        send_letter(conn, environment.last_step, environment)

                    elif strData.startswith('$$$quit'):
                        # Add the last step to complete the history.
                        log('python agent received quit signal:')
                        break

                    else:
                        # Should never happen...
                        log(f'ERROR received unknown sentinal from java agent: {strData}')
                        log('\tAborting')
                        break

    # Catch any error and print a stack trace
    except Exception as error:
        log(f'Exception: {error}')
        log('-----')
        try:
            f = open('pyout.txt', 'a')
            traceback.print_tb(error.__traceback__, None, f)
            f.close()
        except Exception as errerr:
            log(f'Exception exception!: {errerr}')
        log('--- end of report ---')

    log('Agent Exit')

if __name__ == '__main__':
    main()