import sys
import socket
import os
import random
import traceback
import contextlib
import tensorflow as tf

#This must be kept in sync with the Java side!
WINDOW_SIZE = 10
num_goals = 0
steps_since_last_goal = 0
model = None
avg_steps = 0

def log(s):
    f = open("pyout.txt", "a")
    f.write(s)
    f.write("\n")
    f.close()

def send_letter(letter):
    if(letter == '*'):
        letter = random.choice(alphabet)
    log(f"sending {letter}")
    conn.sendall(letter.encode("ASCII"))
    return letter

def calc_avg_steps(history):
    """calculate average steps to goal for a given history"""
    curr_count = 0   #steps since last goal (at a given point)
    total = 0

    #count the number of goals (skipping the first)
    num_goals = 0
    for i in range(len(history)):
        if i == 0:
            continue
        if history[i].isupper():
            num_goals+=1
    
    avg_steps = len(history) / num_goals
    log(f"Avg steps: {avg_steps}")
    return avg_steps

   
def create_model():
    """create and train a model for the agent to use"""
    global model, avg_steps
    
    model = tf.keras.models.Sequential([
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(2)
    ])
    avg_steps = calc_avg_steps(entire_history)
    x_train = calc_input_tensors(entire_history)
    y_train = calc_desired_actions(entire_history, avg_steps)
    x_train = tf.constant(x_train)
    y_train = tf.constant(y_train)
    predictions = model(x_train[:1])
    predictions = tf.nn.softmax(predictions).numpy() #convert to probabilities
    log(f"PREDICTIONS: {predictions}")
    loss_fn = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)
    loss_test = loss_fn(y_train[:1], predictions).numpy()
    log(f"loss_test={loss_test}")
    model.compile(optimizer='adam', loss=loss_fn, metrics=['accuracy'])
    with contextlib.redirect_stdout(open('trainout.txt', 'w')):
        model.fit(x_train, y_train, epochs=5, verbose=2)
        eval_result = model.evaluate(x_train,  y_train)
        print((f"eval_result={eval_result}"))
    return model
##
# calc_input_tensors
def calc_input_tensors(history):
    """convert a blind FSM action history into an array of input tensors"""
    #sanity check
    if (len(history) < 10):
        log("ERROR:  history too short for training!")
        return
    
    #iterate over a range that skips the first 10 indexes so we have a full window for each input
    #generate an input tensor for each one
    hrange = list(range(len(history)))[10:]
    ret = []
    for i in hrange:
        window = history[i-WINDOW_SIZE:i]
        ret.append(flatten(window))
        
    return ret

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
    """calculates the desired action for each window in a history"""
    #sanity check
    if (len(history) < 10):
        log("ERROR:  history too short for training!")
        return
    
    #iterate over a range starting at index 10 in the history
    hrange = list(range(len(history)))[10:]
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
            
    

## 
# flatten
# 
# a window has this format:  'aabaBbbab'
# the output is 3x window size doubles with the first 10 
# being whether or not an 'a'/'A' is in that position
# the second set is for b/B.  The third set is goal sensor.
#
# TODO:  this is hard-coded for a two-letter alphabet.  change
#        the code to handle any alphabet size
# TODO:  in the future we may want to handle sensors. 
#        Example input window:   01a11B00b00a 
def flatten(window):
    """convert a window to an input tensor"""
    if (len(window) != WINDOW_SIZE):
        log(f"ERROR:  flatten requires a window of size {WINDOW_SIZE}")
        return None
    
    #split the window into goal part and action part
    ays = [0.0 for i in range(WINDOW_SIZE)]
    bees = [0.0 for i in range(WINDOW_SIZE)]
    goals = [0.0 for i in range(WINDOW_SIZE)]
    for i in range(len(window)):
        let = window[i]
        if let == 'a' or let == 'A':
            ays[i] = 1.0
        else:
            bees[i] = 1.0
        if let == 'A' or let == 'B':
            goals[i] = 1.0
            
    return ays + bees + goals
    
#======================================================================
# TFSocket Agent
#----------------------------------------------------------------------
os.remove("pyout.txt")
log("Running Python-TensorFlow agent")
log(f"TensorFlow version: {tf.__version__}")


# hardcoded alphabet, will need to read actual from Java environment
alphabet = ['a','b']
argc = len(sys.argv)
if (argc < 2):
    print("ERROR:  Must pass port number as a parameter.")
    
portNum = int(sys.argv[1])
count = 1
timeout = 0
log("port number: " + str(portNum))
log("Creating server for the Java environment to connect to...")

entire_history = ""
last_step = ''
try:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", portNum))
        sock.listen()
        conn, addr = sock.accept()
        with conn:
            log(f"Connected by {addr}")
            while True:
                data = conn.recv(1024)
                
                while not data:
                    data = conn.recv(1024)
                    timeout += 10
                    if (timeout > 1000):
                        log("ERROR: Timeout receiving next input from Java environment")
                        exit(-1)
                #log(f"received from Java env: {data}")
                #check for sentinel
                strData = data.decode("utf-8")
                if (strData.startswith("$$$alphabet:")):
                    alphabet = list(strData[12:])
                    log(f"new alphabet: {alphabet}")
                    log(f"sending 'ack'")
                    conn.sendall("$$$ack".encode("ASCII"))
                elif (strData.startswith("$$$history:")):
                    history = strData[11:]
                    log(f"window received: {history}")
                    if len(history) == 1:
                        history = history.lower()
                    entire_history += str(history[-1:])

                    #Did we reach the goal?
                    if (history[-1:].isupper()):
                        steps_since_last_goal = 0
                        num_goals += 1
                        log(f"Found goal #{num_goals}")
                        if ((num_goals >= 400) and (model is None)):
                            log("Creating model, reached 400 goals")
                            create_model()
                            log(f"new model = {model}")
                            log(f"avg_steps={avg_steps}")
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
                        log(f"Chance of a: {predictions[0][0]}\nChance of b: {predictions[0][1]}")
                        if (predictions[0][0] < predictions[0][1]):
                            last_step = 'b'
                        log("Model is not None, sending prediction: " + last_step)
                    
                    #send it
                    last_step = send_letter(last_step)
                elif (strData.startswith("$$$quit")):
                    #add the last step to complete the history.
                    log("python agent received quit signal:")
                    break
                else:  
                    # Should never happen...
                    # if it does, send a random letter back to the Java environment
                    last_step = send_letter()          
except Exception as error:
    log("Exception:" + str(error))
    log("-----")
    try:
        f = open("pyout.txt", "a")
        traceback.print_tb(error.__traceback__, None, f)
        f.close()
    except Exception as errerr:
        log("Exception exception!:" + str(errerr))
    log("--- end of report ---")
    

log("Agent Exit")
