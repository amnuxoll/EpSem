import sys
import socket
import os
import random
import traceback
import tensorflow as tf
print("TensorFlow version:", tf.__version__)

#This must be kept in sync with the Java side!
WINDOW_SIZE = 10

def log(s):
    f = open("pyout.txt", "a")
    f.write(s)
    f.write("\n")
    f.close()

def sendLetter():
    letter = random.choice(alphabet)
    log(f"sending {letter}")
    conn.sendall(letter.encode("ASCII"))
    return letter

def calc_history(entire_history):
    entire_history += last_step.upper()
    log(f"History in function: {entire_history}")
    curr_count = 0
    total = 0
    num_goals = 0
    for i in range(len(entire_history)):
        if i == 0:
            curr_count+=1
            continue
        if entire_history[i].upper() == entire_history[i]:
            curr_count+=1
            num_goals+=1
            total+=curr_count
            curr_count = 0
        else:
            curr_count+=1
    log(f"Avg steps: {total/num_goals}")
    with open("collecting-data-points.txt",'a') as file:
        file.write(entire_history + "\n")



def get_tensors():
    i = 0
    inputs = []
    with open("collecting-data-points.txt",'r') as file:
        curr_line = file.readline()
        while curr_line is not None and curr_line != "":
            window = curr_line[i:i+10]
            inputs.append(flatten(window))
            if len(inputs) >= 3:
                log(str(inputs))
                getNextAction(inputs)
            i+=1
## 
# flatten
# 
# a window has this format:  aabaBbbab
# the output is 3x window size doubles with the first 10 
# being whether or not an 'a'/'A' is in that position
# the second set is for b/B.  The third set is goal sensor.
#
# TODO:  this is hard-coded for a two-letter alphabet.  change
#        the code to handle any alphabet size
# TODO:  in the future we may want to handle sensors. 
#        Example:   01a11B00b00a 
def flatten(window):
    """convert a window to an input tensor"""
    #split the window into goal part and action part
    ays = [0.0 for i in range(len(window))]
    bees = [0.0 for i in range(len(window))]
    goals = [0.0 for i in range(len(window))]
    for i in range(len(window)):
        let = window[i]
        if let == 'a' or let == 'A':
            ays[i] = 1.0
        else:
            bees[i] = 1.0
        if let == 'A' or let == 'B':
            goals[i] = 1.0
    # ays = []
    # bees = []
    # goals = []
    # for let in window:
    #     if ((let == 'a') or (let == 'A')):
    #         ays.append(1.0)
    #         bees.append(0.0)
    #     else:  #assume 'b' or 'B'
    #         ays.append(0.0)
    #         bees.append(1.0)
            
    #     if ((let == 'A') or (let == 'B')):
    #         goals.append(1.0)
    #     else:
    #         goals.append(0.0)
    
    # #if the history is too small pad with 0.0's to get it to the proper size
    # #TODO: More efficient way to do this?
    # while (len(ays) < WINDOW_SIZE):
    #     ays.insert(0, 0.0)
    #     bees.insert(0, 0.0)
    #     goals.insert(0, 0.0)
        
    return ays + bees + goals
    
# predictions = model(x_train[:1])
# predictions
def getNextAction(inputs):
    model = tf.keras.models.Sequential([
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(2)
    ])
    inputs = tf.constant(inputs)
    predictions = model(inputs[:1])
    tf.nn.softmax(predictions).numpy()
    loss_fn = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)

log("Running Python-TensorFlow agent")
os.remove("pyout.txt")

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
                log(f"received from Java env: {data}")
                #check for sentinel
                strData = data.decode("utf-8")
                if (strData.startswith("$$$alphabet:")):
                    alphabet = list(strData[12:])
                    log(f"new alphabet: {alphabet}")
                    log(f"sending 'ack'")
                    conn.sendall("$$$ack".encode("ASCII"))
                elif (strData.startswith("$$$history:")):
                    history = strData[11:]
                    log(f"history received: {history}")
                    if len(history) == 1:
                        history = history.lower()
                    entire_history += str(history[-1:])
                    log(f"entire history: {entire_history}")
                    last_step = sendLetter()   
                elif (strData.startswith("$$$quit")):
                    calc_history(entire_history)
                    get_tensors()
                    log("python agent received quit signal:")
                    break
                else:  
                    # Send a random letter back to the Java environment
                    last_step = sendLetter()          
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
    
    