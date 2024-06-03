import sys
import socket
import os
import random
import traceback

def log(s):
    f = open("pyout.txt", "a")
    f.write(s)
    f.write("\n")
    f.close()

def log_port_numbers(port_num):
    file = open("port_numbers", "a")
    file.write(str(port_num))
    file.write("\n")
    file.close

def sendLetter():
    letter = random.choice(alphabet)
    log(f"sending {letter}")
    conn.sendall(letter.encode("ASCII"))

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
        log_port_numbers(portNum)
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
                if (strData.startswith("%%%alphabet:")):
                    alphabet = list(strData[12:])
                    log(f"new alphabet: {alphabet}")
                    log(f"sending 'ack'")
                    conn.sendall("%%%ack".encode("ASCII"))
                elif (strData.startswith("%%%history:")):
                    history = strData[11:].split("__")
                    log("history received:")
                    log("path history: " + history[0])
                    log("sensor data: " + history[1])
                    sendLetter()                
                elif (strData.startswith("%%%quit")):
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
