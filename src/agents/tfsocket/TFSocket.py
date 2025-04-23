import sys
import socket
import os
import random
import traceback
import shutil
import math
import re
from TFSocketModel import TFSocketModel as tfmodel
from TFSocketEnv import TFSocketEnv as tfenv
from TFSocketUtils import log

# Define global constants
TRAINING_THRESHOLD = 10  # Number of goals that must be found using random steps before training

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

def process_history_sentinel(strData, environment, model):
    '''
    First use strData to update the TFSocketEnv object's variables.
    Then create and train the model.
    Simulate the model's ideal path to the goal.
    Commit the agent's action first step to the Java socket.

    Params:
        strData, the agent's window (of size WINDOW_SIZE) of the total history from the java agent
        environment, TFSocketEnv object of the environment's details
        model, a TFSocketModel object
    '''
    # Constant vars
    TRAINING_THRESHOLD = 10 # Number of goals to find randomly before training the models
    MIN_HISTORY_LENGTH = 10 # Mimimum history length that the model requires to train

    # Use the random pseudo-model until we've collected enough data to train on
    if (
        environment.num_goals < TRAINING_THRESHOLD
        or len(environment.history) < MIN_HISTORY_LENGTH
    ):
        environment.next_step = '*' # Select the random pseudo-model
        environment.history_since_last_goal += environment.next_step
        return model # Return early to send random action

    # If looping is suspected, use the random pseuo-model and retrain the model
    loop_threshold = 2*environment.avg_steps # Suspect looping threshold
    if environment.steps_since_last_goal >= loop_threshold:
        # Ensure the model is only retrained once until it escapes the loop
        if environment.retrained == False:
            environment.retrained = True
            log(f'\nDetected looping, going random!')
            model = train_model(environment, model)

        # Enable the random pseudo-model
        environment.next_step = '*'
        environment.history_since_last_goal += environment.next_step
        return model

    # If we reached a goal, we must resimulate or retrain the model
    if strData[-1:].isupper():
        environment.retrained = False # Reset the retrained bool once the model has left the loop
        
        # When the agent reaches the TRAINING_THRESHOLD create the model
        if environment.num_goals == TRAINING_THRESHOLD:
            log('\nAgent has reached the training threshold')
            model = train_model(environment, model) # Create and train model
        else:
            # Re-simulate the model
            model.simulate_model()

        # Calculate epilson
        calculate_epsilon(environment)
    
    # Perform E-Greedy exploit/explore decision making
    environment.local_epsilon = (abs(environment.avg_steps - environment.steps_since_last_goal)) / (environment.avg_steps)
    epsilon = environment.global_epsilon * environment.local_epsilon
    if random.random() < epsilon:
        environment.next_step = '*'
        environment.history_since_last_goal += environment.next_step
        return model # Return early to send random action

    # Update the model and get the next step
    if len(model.sim_path) <= 0:
        model.simulate_model()
        
    # Take the first step of the simulated path to goal
    environment.next_step = model.sim_path[0].lower()
    environment.history_since_last_goal += model.sim_path[0]
    
    return model

def calculate_epsilon(environment):
    '''
    Calculate an epsilon value from the number of steps the agent has taken, via a sigmoid function
    '''
    # TODO: print out and graph all of the sigmoids being defined, and when they are being re-defined.
    # There is possibly some bugs in this function:
    # 1. When the agent detects that it is learning again.
    # 2. When the agent detects that it is forgetting again and forgetting more than last time.
    # And yet, epsilon is still improving the model, though if these are bugs it could be improving the model more
    
    x = environment.num_goals # Update the sigmoid function's "x" value
    environment.global_epsilon = max(0, environment.global_epsilon)
    # environment.avg_of_last_five_goals # Average number of steps the agent has taken to find the last 5 goals
    
    # Last iteration's perc_forgetting, perc_forgetting default value is 0.0
    prev_perc_forgetting = environment.perc_forgetting

    # Calculate the rate of "forgetting" as a percentage of the total_avg
    environment.perc_forgetting = math.log(environment.avg_of_last_five_goals/environment.avg_steps, 2)
    
    # Oh no! The agent is "forgetting" (performing worse over time)
    if not prev_perc_forgetting > 0 and environment.perc_forgetting > 0:
        # log('The agent appears to be forgetting')
        # log('Generating negative sigmoid function')
        # log(f'perc_forgetting = {environment.perc_forgetting:.3f}')
        environment.h_shift = 7 + x # Offset by 7 for a TRAINING_THRESHOLD = 10
        environment.upper_bound = environment.global_epsilon + environment.perc_forgetting
    
    # TODO: Should we define the decreasing sigmoid here?
    # i.e., re-defining the upper/lower bounds and h_shift of the sigmoid function
    # Yay! The agent is improving and no longer "forgetting"
    # elif prev_perc_forgetting > 0 and not environment.perc_forgetting > 0:
    #     log('The agent appears to be learning')
    
    # Dang! The agent is still forgetting
    elif prev_perc_forgetting > 0 and environment.perc_forgetting > 0:
        # TODO: is this a good idea or should this block get scrapped.
        # If the agent in "forgetting" at an increasing rate, then increase epsilon by the same amount
        if environment.perc_forgetting > prev_perc_forgetting:
            environment.upper_bound += (environment.perc_forgetting - prev_perc_forgetting)

    # Calculate the next interation of epsilon with the sigmoid function
    environment.global_epsilon = environment.upper_bound * (1 / (1 + math.exp((x - environment.h_shift))))

    # log(f'global_epsilon: {environment.global_epsilon:.3f}')
    # log(f'x: {environment.num_goals}\nh_shift: {environment.h_shift}\nbounds: [0,{environment.upper_bound:.3f}]\n')

def train_model(environment, model):
    '''
    If the random-action data gathering is complete, create a model
    using the environment's history
    '''
    # log(f'Training model')
    model = tfmodel(environment)
    model.train_model()
    model.simulate_model()

    return model

def update_environment(strData, environment):
    '''
    Update environment variables from strData:
        history
        steps_since_last_goal
        num_goals
        avg_steps

    Params:
        strData, the agent's window (of size WINDOW_SIZE) of the total history from the java agent
        environment, TFSocketEnv object of the environment's details
    
    Returns: The updated TFSocketEnv object
    '''
    environment.next_step = ''
    
    # Update history
    window = strData[11:]
    # log(f'Window received: {window}')
    if len(window) == 1:
        window = window.lower()
    environment.history = environment.history + str(window[-1:])

    # Update num_goals and steps_since_last_goal
    environment.steps_since_last_goal +=1
    if window[-1:].isupper():
        pattern = rf'([a-z]*[A-Z])(?=[a-z]*$)'
        matchre = re.search(pattern, (environment.history)).group()
        steps_taken = '.'.join(matchre[i:i+10] for i in range(0, len(matchre), 10))
        actions_chosen = '.'.join(environment.history_since_last_goal[i:i+10] for i in range(0, len(environment.history_since_last_goal), 10))
        
        environment.num_goals += 1

        print_out  = f'\nThe Agent found Goal #{environment.num_goals:<3} in {environment.steps_since_last_goal:>3} steps'
        print_out += f'\nsteps taken:    {steps_taken}'
        print_out += f'\nactions chosen: {actions_chosen}'

        # Update average steps per goal
        environment.update_avg_steps()
        environment.steps_since_last_goal = 0
        environment.history_since_last_goal = ''
        
        print_out += f'\ngoals stepAvg:  {environment.avg_steps:.3f}'
        print_out += f'\n5 goal stepAvg: {environment.avg_of_last_five_goals:.3f}'
        print_out += f'\nperc_improving: {-100*environment.perc_forgetting:.2f}%' # (pos=learning, neg=forgetting)
        print_out += f'\nglobal_epsilon: {environment.global_epsilon:.3f}'
        log(f'{print_out}')

    else:
        # Update average steps per goal
        environment.update_avg_steps()
    
    return environment

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
    conn.sendall(letter.encode('ASCII'))
    return letter

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
    model = None
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
                        log(f'Sending acknowledgment')
                        conn.sendall('$$$ack'.encode('ASCII'))

                    elif strData.startswith('$$$history:'):
                        # Update environment variables
                        environment = update_environment(strData, environment)
                        
                        # log('python agent received history:')
                        model = process_history_sentinel(strData, environment, model)
                        
                        if model != None and len(model.sim_path) > 0:
                            # Adjust the model's predicted steps_to_goal
                            if model.sim_path[0] == environment.next_step:
                                model.sim_path = model.sim_path[1:]
                            else:
                                # Re-simulate the model's predicted steps_to_goal
                                model.simulate_model()
                                                
                        # Send the model's prediction to the environment
                        send_letter(conn, environment.next_step, environment)

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
    try:
        main()
    except Exception as e:
        log(f'Exception: {e}')
        log('-----')
        try:
            f = open('pyout.txt', 'a')
            traceback.print_tb(e.__traceback__, None, f)
            f.close()
        except Exception as errerr:
            log(f'Exception exception!: {errerr}')