import sys
import socket
import os
import random
import traceback
import shutil
import torch

#Use this instead instead of print statements because print won't run when java launches this script
def log(s):
    f = open('pyout.txt','a')
    f.write(s)
    f.write('\n')
    f.close()

def main():
    '''
    main
    '''
    conn = None

    alphabet = None

    if os.path.isfile('pyout.txt'):
        os.remove('pyout.txt')
    log('Running Python Pytorch 2 agent')

    argc = len(sys.argv)
    if argc < 2:
        log('ERROR: Must pass port number as a parameter.')

    # Creating the python side of the socket
    portNum = int(sys.argv[1])
    timeout = 0
    log(f'port number: {portNum}')
    log('Creating server for the Java environment to connect to...')

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
                        alphabet = list(strData[12:])
                        log(f'New alphabet: {alphabet}')
                        log(f'Sending acknowledgment')
                        conn.sendall('$$$ack'.encode('ASCII'))

                    elif strData.startswith('hit me'):
                        #TODO: Calculate a random action
                        letter = random.choice(alphabet)
                        # Send the model's prediction to the environment
                        conn.sendall(letter.encode('ASCII'))
                        log(f'sending random action, {letter}')

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
    
    log("Hello world!")

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