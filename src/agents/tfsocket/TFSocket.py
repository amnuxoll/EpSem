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
    '''
    if not environment.alphabet:
        log('ALPHABET has not been intialized')
        return None

    if letter == '*':
        letter = random.choice(environment.alphabet)
    log(f'sending {letter}')
    conn.sendall(letter.encode('ASCII'))
    return letter

def process_history_sentinel(strData, environment, models):
    '''
    Manage the step-history and use the models to generate the next
    step for the environment
    
    @Param:
        strData: The raw history data from the java proxy
    '''
    # Update entire_history
    window = strData[11:]
    log(f'Window received: {window}')
    if len(window) == 1:
        window = window.lower()
    environment.entire_history = environment.entire_history + str(window[-1:])

    # Update environment variables
    if window[-1:].isupper():
        environment.num_goals += 1
        log(f'Found goal #{environment.num_goals}')
        environment.steps_since_last_goal = 0
    else:
        environment.steps_since_last_goal +=1

    # Create and train models of various window_sizes then calculate which model simulates the shortest path to a Goal
    min_path_model = None
    max_iterations = 0 # Infinite loop precaution, should never run more than 2 times.
    while (min_path_model == None) and (max_iterations < 3):
        # If the random-action data gathering is complete, create models using entire history
        models = train_models(environment, models)
        # If the agent has gone 3*avg_steps without finding a goal, retrain all the models
        environment, models = terminate_loops(environment, models)
        # Find the model with the shortest sim_path
        min_path_model = select_min_path_model(models)
        max_iterations += 1

    # Select the next action from the shortest min_path_model, then adjust the sim_paths for all models
    models = get_best_prediction(environment, models, min_path_model)

    return models

def train_models(environment, models):
    '''
    If the random-action data gathering is complete, create a list of models
    using the environment's entire_history and a window_size defined by model_win_size_param
    
    Example: models[i].win_size = model_win_size_param[i]
        If model_win_size_param = [4, 5, 6], then...
            model[0].win_size = model_win_size_param[0] # = 4
            model[1].win_size = model_win_size_param[1] # = 5
            model[2].win_size = model_win_size_param[2] # = 6
    '''
    TRAINING_THRESHOLD = 100
    # Defines how many models and the win_size param for each model
    model_win_size_param = [4, 5, 6] # models[i].win_size = model_win_size_param[i] 

    # If we've reached the end of random-action data gathering, create models using entire history
    if environment.num_goals >= TRAINING_THRESHOLD:
        if environment.num_goals == TRAINING_THRESHOLD:
            pass # log(f'Reached training threshold: {TRAINING_THRESHOLD}')

        for index in range(len(model_win_size_param)): 
            if models[index] is None:
                # log(f'Training model (window_size={model_win_size_param[index]})')
                models[index] = tfmodel(environment, model_win_size_param[index])
                models[index].train_model()
                models[index].simulate_model()

    return models

def terminate_loops(environment, models):
    '''
    For each model in models:
        if the model has gone 3*avg_steps without finding a goal,
        set the model to None so it can be retrained
    '''
    if environment.steps_since_last_goal > 3*environment.avg_steps:
        # log(f"Looks like we're stuck in a loop!\n\tsteps_since_last_goal={environment.steps_since_last_goal}\n\tavg_steps={environment.avg_steps}")
        for index, model in enumerate(models):
            if model is not None:
                models[index] = None # Reset the model if we haven't reached a goal in a while
    environment.last_step = '*'

    return environment, models

def select_min_path_model(models):
    '''
    Re-train any models with an empty sim_path
    Return the model that has the shortest length sim_path (min_length=1)
    '''
    min_path_model = None
    for index in range(len(models)):
        if models[index] is not None:
            log(f'Model (window_size={models[index].window_size}) sim_path: {models[index].sim_path}')
            if len(models[index].sim_path) <= 0:
                # Set to None so the model can be re-trained
                models[index] = None
                continue

            if min_path_model is None:
                min_path_model = models[index]
            elif len(models[index].sim_path) < len(min_path_model.sim_path):
                min_path_model = models[index]

    return min_path_model

def get_best_prediction(environment, models, min_path_model):
    '''
    Update environment.last_step with the most recent prediction from the best
    model with the shortest sim_path.
    Then adjust each model's sim_path to reflect the environment change.
    '''
    # If there is a trained model, use it to select the next action
    if min_path_model is None:
        log(f'All models are None')
        return models
    
    environment.last_step = min_path_model.sim_path[0].lower()
    
    # Adjust sim_paths for all models based on selected action
    for index in range(len(models)):
        if models[index] is not None: 
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
                        log(f"Sending 'ack'") # Acknowledge
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