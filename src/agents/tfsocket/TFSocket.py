import sys
import socket
import os
import random
import traceback

def log(s):
    f = open("output.txt", "a")
    f.write(s)
    f.write("\n")
    f.close()    

log("Running Python-TensorFlow agent")
os.remove("output.txt")

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
                log(str(count))
                count+=1
                data = conn.recv(1024)
                
                while not data:
                    data = conn.recv(1024)
                    timeout += 10
                    if (timeout > 1000):
                        log("ERROR: Timeout receiving next input from Java environment")
                        exit(-1)
                    # log("ERROR: Timeout receiving next input from Java environment")
                    # break
                log(f"received from Java env: {data}")
                #check for sentinel
                strData = data.decode("utf-8")
                if (strData.startswith("%%%alphabet:")):
                    alphabet = list(strData[12:])
                    log(f"new alphabet: {alphabet}")
                    letter = random.choice(alphabet)
                    log(f"sending {letter}")
                    conn.sendall(letter.encode("ASCII"))
                #must be a history                
                else:  
                    # Send a random letter back to the Java environment
                    letter = random.choice(alphabet)
                    log(f"sending {letter}")
                    conn.sendall(letter.encode("ASCII"))
except Exception as error:
    log("Exception:" + str(error))
    log("-----")
    try:
        f = open("output.txt", "a")
        traceback.print_tb(error.__traceback__, None, f)
        f.close()
    except Exception as errerr:
        log("Exception exception!:" + str(errerr))
    log("--- end of report ---")
