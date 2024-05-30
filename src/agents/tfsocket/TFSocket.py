import sys
import socket
import os
import random

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
     
log("port number: " + str(portNum))
log("Creating server for the Java environment to connect to...")
with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
    sock.bind(("127.0.0.1", portNum))
    sock.listen()
    conn, addr = sock.accept()
    with conn:
        log(f"Connected by {addr}")
        while True:
            data = conn.recv(1024)
            log(f"received from Java env: {data}")
            if not data:
                print("ERROR: Timeout receiving next input from Java environment")
                break
            # Send a random letter back to the Java environment
            letter = random.choice(alphabet)
            conn.sendall(letter.encode("ASCII"))
