import sys
import socket
import os
import random
import traceback
from pytorchDQNModel import pytorchDQNModel as ptmodel
from pytorchDQNEnv import pytorchDQNEnv as ptenv
from pytorchUtils import log

# Define global constants
TRAINING_THRESHOLD = 10  # Number of random steps to take before training models (MIN=10)
model_trained = False

def send_letter(conn, letter, environment):
    '''
    Send an action to the environment through the socket
    If letter = '*' send a random action
    '''
    if not environment.alphabet:
        return None

    if letter == '*':
        letter = random.choice(environment.alphabet)
    conn.sendall(letter.encode('ASCII'))
    return letter

def process_history_sentinel(strData, environment, model):
    '''
    Update the environment and decide the next action using the model.
    '''
    global TRAINING_THRESHOLD, model_trained

    # Update environment variables
    environment = update_environment(strData, environment)

    # Use random actions until we have enough data to train
    if environment.num_goals < TRAINING_THRESHOLD:
        environment.last_step = '*'  # Random action
        return model  # Return early to send random action

    # Train the model if not already trained
    if not model_trained:
        model = ptmodel(environment)
        log(f'Training model after reaching {TRAINING_THRESHOLD} goals.')
        model.train_model()
        model_trained = True

    # Select action using the model
    state = environment.get_state()
    state_vector, window = state  # Unpack the state tuple
    action = model.select_action(state_vector)
    environment.last_step = action

    return model

def update_environment(strData, environment):
    '''
    Update environment variables from strData.
    '''
    # Update entire_history
    window = strData[11:]
    if len(window) == 1:
        window = window.lower()
    environment.entire_history += window[-1:]

    # Update num_goals and steps_since_last_goal
    environment.steps_since_last_goal += 1
    if window[-1:].isupper():
        environment.num_goals += 1
        log(f'The agent found Goal #{environment.num_goals:<3} in {environment.steps_since_last_goal:>3} steps')
        environment.steps_since_last_goal = 0

    # Update average steps per goal
    environment.update_avg_steps()

    return environment

def main():
    '''
    Main function to run the agent.
    '''
    global g_environment
    conn = None

    if os.path.isfile('pyout.txt'):
        os.remove('pyout.txt')
    log('Running Python-PyTorch agent')

    argc = len(sys.argv)
    if argc < 2:
        print('ERROR: Must pass port number as a parameter.')

    # Creating the python side of the socket
    portNum = int(sys.argv[1])
    timeout = 0
    log(f'port number: {portNum}')
    log('Creating server for the Java environment to connect to...')

    # Initialize environment and model
    environment = ptenv()
    model = None

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
                            log(environment.entire_history)
                            exit(-1)
                    # Connected to socket

                    # Handling socket communication
                    # Check for sentinels
                    strData = data.decode('utf-8')  # Raw data from the java agent

                    if strData.startswith('$$$alphabet:'):
                        environment.alphabet = list(strData[12:])
                        environment.overall_alphabet = environment.alphabet + [let.upper() for let in environment.alphabet]
                        log(f'New alphabet: {environment.alphabet}')
                        log(f'Sending "ack"')  # Acknowledge
                        conn.sendall('$$$ack'.encode('ASCII'))

                    elif strData.startswith('$$$history:'):
                        model = process_history_sentinel(strData, environment, model)
                        # Send the model's prediction to the environment
                        send_letter(conn, environment.last_step, environment)

                    elif strData.startswith('$$$quit'):
                        log('Python agent received quit signal.')
                        break

                    else:
                        log(f'ERROR received unknown sentinel from java agent: {strData}')
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