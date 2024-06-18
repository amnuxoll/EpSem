import sys
import socket
import os
import random
import traceback
import contextlib
import tensorflow as tf

#This must be kept in sync with the Java side!
# Define global constants
WINDOW_SIZE = 9
ALPHABET = []

# Global variables
num_goals = 0
steps_since_last_goal = 0
model = None
avg_steps = 0
conn = None

def log(s):
    f = open('pyout.txt', 'a')
    f.write(s)
    f.write('\n')
    f.close()

def send_letter(letter):
    if not ALPHABET:
        log('ALPHABET has not been intialized')
        return None

    if(letter == '*'):
        letter = random.choice(ALPHABET)
    log(f'sending {letter}')
    conn.sendall(letter.encode('ASCII'))
    return letter

def calc_avg_steps(history):
    '''calculate average steps to goal for a given history'''
    # Count the number of goals (skipping the first)
    num_goals = 0
    for i in range(len(history)):
        if i == 0:
            continue
        if history[i].isupper():
            num_goals+=1
    
    avg_steps = len(history) / num_goals
    log(f'Avg steps: {avg_steps}')
    return avg_steps

   
def create_model():
    '''
    Train and optimize a new ANN model
    
    Refer to the beginner tensorflow tutorial:
    https://www.tensorflow.org/tutorials/quickstart/beginner
    
    Global variables being used:
        model
        avg_steps
        entire_history
    '''
    global model, avg_steps, entire_history

    # Defining the model function as a variable
    model = tf.keras.models.Sequential([
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(2)
    ])

    # Defining the inputs and expected outputs from a given history
    avg_steps = calc_avg_steps(entire_history)
    x_train = calc_input_tensors(entire_history)
    y_train = calc_desired_actions(entire_history, avg_steps)
    x_train = tf.constant(x_train)
    y_train = tf.constant(y_train)
    
    # Defining the model's loss function
    predictions = model(x_train[:1])
    predictions = tf.nn.softmax(predictions).numpy() # Convert to probabilities
    loss_fn = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)

    # Optimize and train the model
    model.compile(optimizer='adam', loss=loss_fn, metrics=['accuracy'])
    with contextlib.redirect_stdout(open('trainout.txt', 'w')):
        model.fit(x_train, y_train, epochs=5, verbose=2)

    return model

def calc_input_tensors(history):
    '''
    Convert a blind FSM action history into an array of input tensors
    '''
    # Sanity check
    if (len(history) < WINDOW_SIZE):
        log('ERROR:  history too short for training!')
        return

    # Iterate over a range that skips the first WINDOW_SIZE indexes so we have a full window for each input
    # generate an input tensor for each one
    history_range = list(range(len(history)))[WINDOW_SIZE:]
    tensor = []
    for i in history_range:
        window = history[i-WINDOW_SIZE:i]
        tensor.append(flatten(window))

    return tensor

##
# calc_desired_actions
#
# the output of this function should line up with the output of 
# calc_input_tensors for the same history
#
# history - in a letter-based format (e.g., aabaBbbabaabaBbbab...)
# cutoff - distance from curr pos to goal at which the agent prefers 
#          NOT to take the historical action
def calc_desired_actions(history, cutoff):
    '''calculates the desired action for each window in a history'''
    #sanity check
    if (len(history) < WINDOW_SIZE):
        log('ERROR:  history too short for training!')
        return
    
    #iterate over a range starting at index WINDOW_SIZE in the history
    hrange = list(range(len(history)))[WINDOW_SIZE:]
    ret = []
    for i in hrange:
        #calculate how many steps from this position to the next goal
        num_steps = 0
        while(history[i + num_steps].islower()):
            num_steps += 1
        
        #calculate the index of the action of this position
        val = 0
        if ( (history[i].lower() == 'b') or (history[i].lower() == 'B') ):
            val = 1
            
        #if the cutoff is exceeded then we want the other action instead
        if (num_steps >= cutoff):
            val = 1 - val
            
        #add to result
        ret.append(val)
    
    return ret
            
def flatten(window):
    '''
    Convert a window into an input tensor
    Helper function for @calc_input_tensors()
    
    A window has this format:      'aabaBbbab'
    the return object's length is 3*WINDOW_SIZE
    being whether or not an 'a'/'A' is in that position
    the second set is for b/B.  The third set is goal sensor.

    TODO:  This is hard-coded for a two-letter ALPHABET. Change
           the code to handle any ALPHABET size
    TODO:  In the future we may want to handle sensors. 
           Example input window:   '01a11B00b00a'
    '''
    win_size = len(window)

    # Split the window into goal part and action part
    ays = [0.0 for i in range(win_size)]
    bees = [0.0 for i in range(win_size)]
    goals = [0.0 for i in range(win_size)]

    for i in range(win_size):
        let = window[i]
        if let == 'a' or let == 'A':
            ays[i] = 1.0
        else:
            bees[i] = 1.0
        if let == 'A' or let == 'B':
            goals[i] = 1.0

    return ays + bees + goals




def main():
    
    global conn

    os.remove('pyout.txt')
    log('Running Python-TensorFlow agent')

    argc = len(sys.argv)
    if (argc < 2):
        print('ERROR:  Must pass port number as a parameter.')

    portNum = int(sys.argv[1])
    timeout = 0
    log('port number: ' + str(portNum))
    log('Creating server for the Java environment to connect to...')

    entire_history = ''
    last_step = ''
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
                    #log(f'received from Java env: {data}')
                    #check for sentinel
                    strData = data.decode('utf-8')
                    if (strData.startswith('$$$ALPHABET:')):
                        # TODO: ALPHABET is initialized but not used
                        ALPHABET = list(strData[12:])
                        log(f'new ALPHABET: {ALPHABET}')
                        log(f"sending 'ack'")
                        conn.sendall('$$$ack'.encode('ASCII'))
                    elif (strData.startswith('$$$history:')):
                        history = strData[11:]
                        log(f'window received: {history}')
                        if len(history) == 1:
                            history = history.lower()
                        entire_history += str(history[-1:])

                        #Did we reach the goal?
                        if (history[-1:].isupper()):
                            steps_since_last_goal = 0
                            num_goals += 1
                            log(f'Found goal #{num_goals}')
                            if ((num_goals >= 400) and (model is None)):
                                log('Creating model, reached 400 goals')
                                create_model()
                                log(f'new model = {model}')
                                log(f'avg_steps={avg_steps}')
                        else:
                            steps_since_last_goal +=1
                        
                        if (model is not None) and steps_since_last_goal > 3*avg_steps:
                            log(f"Looks like we're stuck in a loop! steps_since_last_goal={steps_since_last_goal} and avg_steps={avg_steps}")
                            model = None #reset the model if we haven't reached a goal in a while
                        last_step = '*'
                        
                        #If there is a trained model, use it to select the next action
                        if (model is not None):
                            one_input = []
                            one_input.append(flatten(history))
                            one_input = tf.constant(one_input)
                            # predictions = get_next_action(entire_history)
                            predictions = model(one_input)
                            
                            last_step = 'a'
                            # Predictions now returns as a tensor of shape (1,2)
                            log(f'Chance of a: {predictions[0][0]}\nChance of b: {predictions[0][1]}')
                            if (predictions[0][0] < predictions[0][1]):
                                last_step = 'b'
                            log('Model is not None, sending prediction: ' + last_step)
                        
                        #send it
                        last_step = send_letter(last_step)
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