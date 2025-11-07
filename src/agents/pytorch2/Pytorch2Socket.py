import sys
import socket
import os
import random
import traceback
import shutil
import torch
from .qnet import QNet
import torch.optim as optim
from collections import deque

class ReplayBuffer:
    """
    keep last capacity values to discount/remember
    #TODO: python agent
    """
    def __init__(self, capacity=20_000):
        self.buf = deque(maxlen=capacity)
    def push(self, s, a, r, sp, done):
        self.buf.append((s, a, r, sp, done))
    def sample(self, batch_size):
        batch = random.sample(self.buf, batch_size)
        s, a, r, sp, d = zip(*batch)
        return (torch.stack(s),
                torch.tensor(a, dtype=torch.long),
                torch.tensor(r, dtype=torch.float32),
                torch.stack(sp),
                torch.tensor(d, dtype=torch.float32))
    def __len__(self): return len(self.buf)

#Use this  instead of print statements because print won't run when java launches this script
def log(s):
    f = open('pyout.txt','a')
    f.write(s)
    f.write('\n')
    f.close()
class QTrain:
    def __init__(self, environment):
        """
        Takes parameters from DQN_Train in Yuji's code and initializes them as
        instance variables
        """
        env = environment
        n_hist = 3
        episodes = 500
        gamma = 0.99  # discount factor
        lr = 1e-3  # learning rate
        batch_size = 64  # number of actions that is remembered for q-value
        buffer_capacity = 20_000
        start_learning_after = 500  # num random actions to take before learning
        target_update_every = 200
        eps_start = 1.0
        eps_end = 0.05
        eps_decay_steps = 5000
        seed = 0
        device = "cpu"

        global_step, grad_steps = 0, 0
        success_log, length_log = [], []

        episode_rewards = []

        K = len(env.alphabet)  # number of actions
        #TODO: Is this our Java agent and do we instantiate it later?
        #agent = DFAAgent(n_hist=n_hist, K=K, seed=seed)
        obs_dim = n_hist * (K + 1)
        n_actions = K

        q = QNet(obs_dim, n_actions).to(device)
        qt = QNet(obs_dim, n_actions).to(device)
        qt.load_state_dict(q.state_dict());
        qt.eval()
        opt = optim.Adam(q.parameters(), lr=lr)
        buf = ReplayBuffer(buffer_capacity)

    def epsilon(self, step):
        if self.eps_decay_steps <= 0: return self.eps_end
        t = min(1.0, step / self.eps_decay_steps)
        return self.eps_start + (self.eps_end - self.eps_start) * t

    def getNextActionFromQ(self):
        # This code was in the outer for-loop.  It will need to be triggered each
        # time we reach a goal (or max steps).  It might make sense for this
        # if-statement to be at the end of the method instead of the beginning?
        done, total_r, steps = False, 0.0, 0

        if (done or steps >= self.env.max_steps):
            #TODO: edit once we get our agent and environment in the Java portion
            #obs = env.reset()  # int token from env (0 on reset)
            #agent.reset()
            #agent.observe(obs)
            #s = agent.encode().to(device)
            pass

    def getQ(self):
        return self.q, {"success":self.success_log, "lengths":self.length_log,
                        "rewards":self.episode_rewards}

    def main(self):
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
                            #TODO: Calculate a random action; implement "inner loop" function of Yuji's code here?
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
        q = QTrain()
        q.main()
    except Exception as e:
        log(f'Exception: {e}')
        log('-----')
        try:
            f = open('pyout.txt', 'a')
            traceback.print_tb(e.__traceback__, None, f)
            f.close()
        except Exception as errerr:
            log(f'Exception exception!: {errerr}')