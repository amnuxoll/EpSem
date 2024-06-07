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
    ays = []
    bees = []
    goals = []
    for let in window:
        if ((let == 'a') or (let == 'A')):
            ays.append(1.0)
            bees.append(0.0)
        else:  #assume 'b' or 'B'
            ays.append(0.0)
            bees.append(1.0)
            
        if ((let == 'A') or (let == 'B')):
            goals.append(1.0)
        else:
            goals.append(0.0)
    
    #if the history is too small pad with 0.0's to get it to the proper size
    #TODO: More efficient way to do this?
    while (len(ays) < WINDOW_SIZE):
        ays.insert(0, 0.0)
        bees.insert(0, 0.0)
        goals.insert(0, 0.0)
        
    return ays + bees + goals
    
    
def getNextAction(window):
    inputs = flatten(window)
    model = tf.keras.models.Sequential([
  tf.keras.layers.Flatten(input_shape=(28, 28)),
  tf.keras.layers.Dense(128, activation='relu'),
  tf.keras.layers.Dropout(0.2),
  tf.keras.layers.Dense(10)
])

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
                    sendLetter()                
                elif (strData.startswith("$$$quit")):
                    log("python agent received quit signal:")
                    break
                else:  
                    # Send a random letter back to the Java environment
                    sendLetter()
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
