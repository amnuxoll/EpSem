import sys
import socket
import os
import random
import traceback
from pytorchDQNEnv import pytorchDQNEnv as ptenv
from pytorchUtils import log

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
    log(f'sending {letter}')
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

    # If we reached a goal, we need to retrain all the models
    # TODO: put this in a helper method
    if strData[-1:].isupper():
        # When the agent reaches the TRAINING_THRESHOLD create the models
        if environment.num_goals == TRAINING_THRESHOLD:
            log(f'Reached training threshold: goal #{TRAINING_THRESHOLD}')
            # Set all models to None to prepare for training
            for index in range(len(models)):
                models[index] = None

        # Find the model that predicted this goal
        predicting_model = None
        for model in models: # NOTE: This will favor higher indexed models
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
                models[index].simulate_model()
            else:
                models[index] = None
        return models

    # If looping is suspected, use the random sudo-model and set all models to None for retraining
    loop_threshold = 2*environment.avg_steps # Suspect looping threshold
    if environment.steps_since_last_goal >= loop_threshold:
        # Reset the models for training
        if environment.steps_since_last_goal == loop_threshold:
            # log('Looks like the agent is stuck in a loop! Switching to random pseudo-model')
            # log(f'avg_steps={environment.avg_steps:.2f}, steps_since_last_goal={environment.steps_since_last_goal}')
            for index in range(len(models)):
                models[index] = None

        # Enable the random sudo-model
        environment.last_step = '*'
        return models

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

def main():
    '''
    main
    '''
    conn = None

    if os.path.isfile('pyout.txt'):
        os.remove('pyout.txt')
    log('Running Python-Pytorch agent')

    argc = len(sys.argv)
    if argc < 2:
        print('ERROR: Must pass port number as a parameter.')

    # Creating the python side of the socket
    portNum = int(sys.argv[1])
    timeout = 0
    log(f'port number: {portNum}')
    log('Creating server for the Java environment to connect to...')

    # Initialize FSM scoped variables
    log("before environment call")
    environment = ptenv()

    log('trying to connect...')
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
                        # Send the model's prediction to the environment
                        send_letter(conn, "*", environment)

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