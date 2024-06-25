import sys
import socket
import os
import random
import traceback
from TFSocketModel import TFSocketModel as tfmodel
from TFSocketEnv import TFSocketEnv as tfenv
from TFSocketUtils import log

# Define global constants
WINDOW_SIZE = 9 # This must be kept in sync with the Java side!

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

    if (letter == '*'):
        letter = random.choice(environment.alphabet)
    log(f'sending {letter}')
    conn.sendall(letter.encode('ASCII'))
    return letter

def check_if_goal(window, environment, model):
    '''
    Did we reach the goal?
    '''
    if (window[-1:].isupper()):
        environment.num_goals += 1
        log(f'Found goal #{environment.num_goals}')
        if ((environment.num_goals >= 100) and (model is None)):
            log('Creating model, reached 100 goals')
            model = tfmodel(environment, WINDOW_SIZE)
            model.train_model()
            model.simulate_model()
        environment.steps_since_last_goal = 0
    else:
        environment.steps_since_last_goal +=1

    return model

def process_history_sentinel(strData, environment, model):
    '''
    Manage the step-history and use the model to generate the next
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

    model = check_if_goal(window, environment, model)

    if (model is not None) and environment.steps_since_last_goal > 3*environment.avg_steps:
        log(f"Looks like we're stuck in a loop! steps_since_last_goal={environment.steps_since_last_goal} and avg_steps={environment.avg_steps}")
        model = None # Reset the model if we haven't reached a goal in a while
    environment.last_step = '*'

    # If there is a trained model, use it to select the next action
    if (model is not None):
        predictions = model.get_predictions(window)
        environment.last_step = model.get_letter(predictions).lower() 
        log(f'Model is not None, sending prediction: {environment.last_step}')
    
    return model

def main():
    '''
    main
    '''
    conn = None

    if os.path.isfile('pyout.txt'):
        os.remove('pyout.txt')
    log('Running Python-TensorFlow agent')

    argc = len(sys.argv)
    if (argc < 2):
        print('ERROR:  Must pass port number as a parameter.')

    # Creating the python side of the socket
    portNum = int(sys.argv[1])
    timeout = 0
    log('port number: ' + str(portNum))
    log('Creating server for the Java environment to connect to...')

    # Initialize FSM scoped variables
    environment = tfenv()
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
                        if (timeout > 1000):
                            log('ERROR: Timeout receiving next input from Java environment')
                            exit(-1)
                    # Connected to socket

                    # Handling socket communication
                    # Check for sentinels
                    strData = data.decode('utf-8') # raw data from the java agent

                    if (strData.startswith('$$$alphabet:')):
                        environment.alphabet = list(strData[12:])
                        log(f'New alphabet: {environment.alphabet}')
                        log(f"Sending 'ack'") # Acknowledge
                        conn.sendall('$$$ack'.encode('ASCII'))

                    elif (strData.startswith('$$$history:')):
                        model = process_history_sentinel(strData, environment, model)
                        # Send the model's prediction to the environment
                        send_letter(conn, environment.last_step, environment)

                    elif (strData.startswith('$$$quit')):
                        #add the last step to complete the history.
                        log('python agent received quit signal:')
                        break

                    else:  
                        # Should never happen...
                        log('ERROR received unknown sentinal from java agent: ' + str(strData))
                        log('\tAborting')
                        break

    # Catch any error and print a stack trace
    except Exception as error:
        log('Exception: ' + str(error))
        log('-----')
        try:
            f = open('pyout.txt', 'a')
            traceback.print_tb(error.__traceback__, None, f)
            f.close()
        except Exception as errerr:
            log('Exception exception!: ' + str(errerr))
        log('--- end of report ---')

    log('Agent Exit')

if __name__ == '__main__':
    main()