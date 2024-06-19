import sys
import socket
import os
import random
import traceback
import TFSocketModel as tfmodel

#This must be kept in sync with the Java side!
# Define global constants
WINDOW_SIZE = 9
ALPHABET = []

# Global variables
model = None

def log(s):
    f = open('pyout.txt', 'a')
    f.write(s)
    f.write('\n')
    f.close()

def send_letter(conn, letter):
    '''
    Send an action to the environment through the socket
    '''
    if not ALPHABET:
        log('ALPHABET has not been intialized')
        return None

    if (letter == '*'):
        letter = random.choice(ALPHABET)
    log(f'sending {letter}')
    conn.sendall(letter.encode('ASCII'))
    return letter

def check_if_goal(history, steps_since_last_goal, entire_history, model):
    ''' Did we reach the goal? '''
    num_goals = 0
    avg_steps = 0

    if (history[-1:].isupper()):
        steps_since_last_goal = 0
        num_goals += 1
        log(f'Found goal #{num_goals}')
        if ((num_goals >= 400) and (model is None)):
            log('Creating model, reached 400 goals')
            model = tfmodel(WINDOW_SIZE, entire_history)
            avg_steps = model.train_model(entire_history)
    else:
        steps_since_last_goal +=1

    return model, steps_since_last_goal, avg_steps

def process_history_sentinel(strData, entire_history, steps_since_last_goal, model):
    '''
    Manage the step-history and use the model to generate the next
    step for the environment
    '''
    # Update entire_history
    history = strData[11:]
    log(f'Window received: {history}')
    if len(history) == 1:
        history = history.lower()
    updated_entire_history = entire_history + str(history[-1:])

    model, steps_since_last_goal, avg_steps = check_if_goal(history, steps_since_last_goal, updated_entire_history, model)

    if (model is not None) and steps_since_last_goal > 3*avg_steps:
        log(f"Looks like we're stuck in a loop! steps_since_last_goal={steps_since_last_goal} and avg_steps={avg_steps}")
        model = None # Reset the model if we haven't reached a goal in a while
    last_step = '*'

    # If there is a trained model, use it to select the next action
    if (model is not None):
        last_step = model.get_action(history)

    return updated_entire_history, last_step

def main():
    '''
    main
    '''
    global ALPHABET
    conn = None

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
    entire_history = ''
    last_step = ''
    steps_since_last_goal = 0
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
                    # Check for sentinel
                    strData = data.decode('utf-8')

                    if (strData.startswith('$$$alphabet:')):
                        ALPHABET = list(strData[12:])
                        log(f'New alphabet: {ALPHABET}')
                        log(f"Sending 'ack'") # Acknowledge
                        conn.sendall('$$$ack'.encode('ASCII'))

                    elif (strData.startswith('$$$history:')):
                        entire_history, last_step = process_history_sentinel(strData, entire_history, steps_since_last_goal, model)
                        # Send the model's prediction to the environment
                        last_step = send_letter(conn, last_step)

                    elif (strData.startswith('$$$quit')):
                        #add the last step to complete the history.
                        log('python agent received quit signal:')
                        break

                    else:  
                        # Should never happen...
                        # if it does, send a random letter back to the Java environment
                        last_step = send_letter() 

    except Exception as error:
        log('Exception:' + str(error))
        log('-----')
        try:
            f = open('pyout.txt', 'a')
            traceback.print_tb(error.__traceback__, None, f)
            f.close()
        except Exception as errerr:
            log('Exception exception!:' + str(errerr))
        log('--- end of report ---')

    log('Agent Exit')

if __name__ == '__main__':
    main()