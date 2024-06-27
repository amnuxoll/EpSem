import sys
import socket
import os
import random
import traceback
from TFSocketModel import TFSocketModel as tfmodel
from TFSocketEnv import TFSocketEnv as tfenv
from TFSocketUtils import log

# Define global constants
WINDOW_SIZE = 10 # This must be kept in sync with the Java side!
TRAINING_THRESHOLD = 150

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

def process_history_sentinel(strData, environment, models):
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
    global TRAINING_THRESHOLD

    # Update environment variables
    environment = update_environment(strData, environment)

    # Run the random sudo-model until we've collected enough data to train on
    if environment.num_goals < TRAINING_THRESHOLD:
        environment.last_step = '*' # Select the random sudo-model

        # When the agent reaches the TRAINING_THRESHOLD, set all models
        # to None; to prepare for training
        if environment.num_goals == TRAINING_THRESHOLD:
            log(f'Reached training threshold: goal #{TRAINING_THRESHOLD}')
            # Set all models to None to prepare for training
            for index in range(len(models)):
                models[index] = None

        return models # Either returns models unchanged, or as a list of None's

    # If looping is suspected, use the random sudo-model and set all models to None for retraining
    loop_threshold = 2*environment.avg_steps # Suspect looping threshold
    if environment.steps_since_last_goal >= loop_threshold:
        if environment.steps_since_last_goal == loop_threshold:
            log('Looks like the agent is stuck in a loop! Switching to random sudo-model')
            log(f'avg_steps={environment.avg_steps:.2f}, steps_since_last_goal={environment.steps_since_last_goal}')
            # Reset the model if we haven't reached a goal in a while
            for index in range(len(models)):
                models[index] = None
        # Enable the random sudo-model
        environment.last_step = '*'
        return models

    # Create and train models of various window_sizes then select
    # which model simulates the shortest path to a goal
    models, min_path_model = manage_models(environment, models)

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
    if window[-1:].isupper():
        log(f'The agent found Goal #{environment.num_goals:<3} in {environment.steps_since_last_goal:>3} steps')
        environment.num_goals += 1
        environment.steps_since_last_goal = 0
    else: # A goal was not reached
        environment.steps_since_last_goal +=1

    # Update average steps per goal
    environment.update_avg_steps()

    return environment

def manage_models(environment, models):
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
    while (min_path_model == None) and (index < max_iterations):
        # Create models using entire history
        models = train_models(environment, models)
        # Find the model with the shortest sim_path
        min_path_model = select_min_path_model(models, min_path_model)
        index += 1
    if min_path_model == None:
        log('ERROR: The training and selecting of models was unsuccessfull.')

    return models, min_path_model

def train_models(environment, models):
    '''
    If the random-action data gathering is complete, create a list of models
    using the environment's entire_history and a window_size defined by model_win_sizes

    Example: models[i].win_size = model_win_sizes[i]
        If model_win_sizes = [4, 5, 6], then...
            model[0].win_size = model_win_sizes[0] # = 4
            model[1].win_size = model_win_sizes[1] # = 5
            model[2].win_size = model_win_sizes[2] # = 6
    '''
    # Defines how many models and the win_size param for each model
    model_win_sizes = [3, 6, 9] # models[i].win_size = model_win_sizes[i] 

    # log(f'Training models')
    # If we've reached the end of random-action data gathering, create models using entire history
    for index in range(len(model_win_sizes)):
        if models[index] is None:
            models[index] = tfmodel(environment, model_win_sizes[index])
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
            elif len(models[index].sim_path) < len(min_path_model.sim_path):
                min_path_model = models[index]

    # Could possibly None it all models are None
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
                models[index] = None # This will trigger a retrain at next iteration
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
                    strData = data.decode('utf-8') # raw data from the java agent

                    if strData.startswith('$$$alphabet:'):
                        environment.alphabet = list(strData[12:])
                        log(f'New alphabet: {environment.alphabet}')
                        log(f'Sending "ack"') # Acknowledge
                        conn.sendall('$$$ack'.encode('ASCII'))

                    elif strData.startswith('$$$history:'):
                        models = process_history_sentinel(strData, environment, models)
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