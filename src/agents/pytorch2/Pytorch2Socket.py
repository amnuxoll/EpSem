import sys
import socket
import os
import random
import traceback
import shutil
from observer import Observer
import torch
from qnet import QNet
import torch.optim as optim
from collections import deque
import numpy as np

class ReplayBuffer:
    """
    keep last capacity values to discount/remember
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
        self.env = environment
        self.n_hist = 3
        self.episodes = 500
        self.gamma = 0.99  # discount factor
        self.lr = 1e-3  # learning rate
        self.batch_size = 64  # number of actions that is remembered for q-value
        self.buffer_capacity = 20_000
        self.start_learning_after = 500  # num random actions to take before learning
        self.target_update_every = 200
        self.eps_start = 1.0
        self.eps_end = 0.05
        self.eps_decay_steps = 5000
        self.seed = 0
        self.device = "cpu"

        self.global_step, self.grad_steps = 0, 0
        self.success_log, self.length_log = [], []
        self.done, self.total_r, self.steps = False, 0.0, 0

        self.episode_rewards = []

        self.K = len(self.env.alphabet)  # number of actions
        self.observer = Observer(n_hist=self.n_hist, K=self.K, seed=self.seed)
        self.obs_dim = self.n_hist * (self.K + 1)
        self.n_actions = self.K

        self.q = QNet(self.obs_dim, self.n_actions).to(self.device)
        self.qt = QNet(self.obs_dim, self.n_actions).to(self.device)
        self.qt.load_state_dict(self.q.state_dict())
        self.qt.eval()
        self.opt = optim.Adam(self.q.parameters(), lr=self.lr)
        self.buf = ReplayBuffer(self.buffer_capacity)

    def epsilon(self, step):
        if self.eps_decay_steps <= 0: return self.eps_end
        t = min(1.0, step / self.eps_decay_steps)
        return self.eps_start + (self.eps_end - self.eps_start) * t

    #This method is called each time the agent starts a new run
    def initModel(self):
        self.observer.reset()
        self.observer.observe(obs)
        s = self.observer.encode().to(self.device)

    #This method is called each time the agent completes a run
    def recordGoal(self):
        # this code below was outside
        self.success_log.append(1 if self.total_r > 0 else 0)
        self.length_log.append(self.steps)
        self.episode_rewards.append(self.total_r)

        if (ep + 1) % max(1, self.episodes // 10) == 0:
            wr = np.mean(self.success_log[-50:]) if self.success_log else 0.0
            print(f"Episode {ep + 1:4d}/{self.episodes} | recent win-rate(50)={wr:.2f} "
                  f"| steps={self.steps} total_r={self.total_r:.1f}")

    def getNextActionFromQ(self):
        # This code was in the outer for-loop.  It will need to be triggered each
        # time we reach a goal (or max steps).  It might make sense for this
        # if-statement to be at the end of the method instead of the beginning?

        #NOTE:  If the agent gets in a loop (seems like it won't happen)
        #       then code will be needed here to enforce a "max steps"
        #       and only take random actions



        if random.random() < self.epsilon(self.global_step):
            a = self.env.sample_action()
            return a
        else:
            with torch.no_grad():
                qvals = self.q(s.unsqueeze(0))
                a = int(torch.argmax(qvals, dim=1).item())
                return a

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
                            #TODO: Check this -->Initialize new run
                            self.initModel()
                            alphabet = list(strData[12:])
                            log(f'New alphabet: {alphabet}')
                            log(f'Sending acknowledgment')
                            conn.sendall('$$$ack'.encode('ASCII'))

                        elif strData.startswith('hit me'):
                            letter = self.getNextActionFromQ()
                            # Send the model's prediction to the environment
                            conn.sendall(letter.encode('ASCII'))
                            log(f'sending random action, {letter}')

                        elif strData.startswith('$$$quit'):
                            #TODO: Check this -->record goal here
                            self.recordGoal()
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