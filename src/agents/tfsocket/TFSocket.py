import sys
import socket
import os
import random
import traceback
import shutil
from TFSocketModel import TFSocketModel as tfmodel
from TFSocketEnv import TFSocketEnv as tfenv
from TFSocketUtils import log

# Define global constants
TRAINING_THRESHOLD = 10  # Number of random steps to take before training models (MIN=10)

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
    global TRAINING_THRESHOLD

    # Update environment variables
    environment = update_environment(strData, environment)

    # Use the random pseudo-model until we've collected enough data to train on
    if environment.num_goals < TRAINING_THRESHOLD:
        environment.last_step = '*' # Select the random pseudo-model
        return models # Return early to send random action

    # If looping is suspected, use the random sudo-model and set all models to None for retraining
    loop_threshold = 2*environment.avg_steps # Suspect looping threshold
    if environment.steps_since_last_goal >= loop_threshold:
        # Reset the models for training
        
        # TODO: Try checking if we should be resetting to train everytime we're over the 
        # loop threshold, i.e get rid of this if and un-indent the for loop
        if environment.steps_since_last_goal == loop_threshold:
            # log('Looks like the agent is stuck in a loop! Switching to random pseudo-model')
            # log(f'avg_steps={environment.avg_steps:.2f}, steps_since_last_goal={environment.steps_since_last_goal}')
            # RETRAIN HERE
            # log("Called for retrain in PHS #2")
            for index in range(len(models)):
                models[index] = None

        # Enable the random sudo-model
        environment.last_step = '*'
        return models

    # If we reached a goal, we must resimulate or retrain the models
    # TODO: put this in a helper method
    if strData[-1:].isupper():
        # When the agent reaches the TRAINING_THRESHOLD create the models
        if environment.num_goals == TRAINING_THRESHOLD:
            log(f'Reached training threshold: goal #{TRAINING_THRESHOLD}')
            models = train_models(environment, models, model_window_sizes)

        # Find the model that predicted this goal
        # predicting_model = None
        # for model in models: # NOTE: This will favor higher indexed models
        #     if (
        #         model is not None
        #         and model.sim_path is not None
        #         and len(model.sim_path) == 0
        #     ):
        #         predicting_model = model
        #         break

        # Adjust model sizes
        # TODO: Try something binary-search-like?
        # if predicting_model is not None and predicting_model.window_size >= 2:
            # log(f'new model sizes centered around {predicting_model.window_size}')
            # model_window_sizes[0] = predicting_model.window_size - 1
            # model_window_sizes[1] = predicting_model.window_size
            # model_window_sizes[2] = predicting_model.window_size + 1

        # Re-simulate models that correctly simulated the goal and trigger a retrain for the other models
        models[0].simulate_model()
        # for index in range(len(models)):
        #     if models[index] is not None and models[index] == predicting_model:
        #         models[index].simulate_model()
        #     else:
        #         # RETRAIN HERE
        #         # log("Called for retrain in PHS #1")
        #         # models[index] = None
        #         pass

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
        environment.num_goals += 1
        log(f'The agent found Goal #{environment.num_goals:<3} in {environment.steps_since_last_goal:>3} steps')
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
                # RETRAIN HERE
                # log("Called for retrain in select_min_path_model")
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
                pass
                # models[index].sim_path = models[index].sim_path[1:]

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
    models = [None]
    # Defines how many models and the win_size param for each model
    # These numbers will change dynamically as the agent discovers a near-optimal size
    model_window_sizes = [5] # models[i].win_size = model_window_sizes[i] 
    cwd = os.getcwd()
    
    for entry in os.listdir(cwd):
        dir_path = os.path.join(cwd, entry)
        if os.path.isdir(dir_path):
            if entry.startswith('tuning_dir'):
                shutil.rmtree(dir_path)

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
                        log(f'Sending "ack"') # Acknowledge
                        conn.sendall('$$$ack'.encode('ASCII'))

                    elif strData.startswith('$$$history:'):
                        # log('python agent received history:')
                        models = process_history_sentinel(strData, environment, models, model_window_sizes)
                        
                        if len(models) != 0 and models[0] != None and len(models[0].sim_path) > 0:
                            if models[0].sim_path[0] == environment.last_step:
                                models[0].sim_path = models[0].sim_path[1:]
                            else:
                                models[0].simulate_model()
                        
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