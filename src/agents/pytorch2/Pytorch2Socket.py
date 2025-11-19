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
    def __init__(self):
        """
        Takes parameters from DQN_Train in Yuji's code and initializes them as
        instance variables
        """
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

        self.alpabet = []  #this gets init'd later
        self.K = 0         #this gets init'd later
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
        self.observer.observe(0)
        self.s = self.observer.encode().to(self.device) #changed s to 'self.'s to alter class variable of s
                                                        #makes it used within rest of class

    #This method is called each time the agent completes a run
    def recordGoal(self):
        # this code below was outside
        self.success_log.append(1 if self.total_r > 0 else 0)
        self.length_log.append(self.steps)
        self.episode_rewards.append(self.total_r)

        #finding the remainder of the next episode divided by the max of episodes or 10
        #if not at 10, then ten is max????
        #TEMP COMMENT OUT
        #if (ep + 1) % max(1, self.episodes // 10) == 0:
            #wr = np.mean(self.success_log[-50:]) if self.success_log else 0.0
            #print(f"Episode {ep + 1:4d}/{self.episodes} | recent win-rate(50)={wr:.2f} "
                  #f"| steps={self.steps} total_r={self.total_r:.1f}")

    #
    # logReward
    #
    # records the agent's reward for it's last action
    def logReward(self, r):
        
        #TODO:  calculate  obs_next = self.lastAction + 1 (where lastAction is converted to its integer version)
        obs_next = None #<-- Fix me!

        observer.observe(obs_next)
        sp = observer.encode().to(device)

        buf.push(s, a, r, sp, False)
        s = sp; total_r += r

        if len(buf) >= start_learning_after:
            S, A, R, SP, D = buf.sample(batch_size)
            S, A, R, SP, D = S.to(device), A.to(device), R.to(device), SP.to(device), D.to(device)

            q_sa = q(S).gather(1, A.unsqueeze(1)).squeeze(1)
            with torch.no_grad():
                target = R + (1.0 - D) * gamma * qt(SP).max(1).values
                loss = nn.functional.mse_loss(q_sa, target)

            opt.zero_grad(); loss.backward(); opt.step()
            grad_steps += 1
            if grad_steps % target_update_every == 0:
                qt.load_state_dict(q.state_dict())
        
            
    #
    # getNextActionFromQ
    #
    # This method is the "connection" between the framework code and the code
    # written by Yuji Sakabe that creates/manages the PyTorch model.
    # See his dqn_train.py to see what the original looked like.  
    #
    def getNextActionFromQ(self):
        # This code was in the outer for-loop.  It will need to be triggered each
        # time we reach a goal (or max steps).  It might make sense for this
        # if-statement to be at the end of the method instead of the beginning?

        #NOTE:  If the agent gets in a loop (seems like it won't happen)
        #       then code will be needed here to enforce a "max steps"
        #       and only take random actions
        #       to force exploring, when exploit is not working

        if random.random() < self.epsilon(self.global_step):
            a = self.env.sample_action()  #TODO: replace with something like random.choice()
        else:
            with torch.no_grad():
                qvals = self.q(self.s.unsqueeze(0)) #is this the same s as initmodel
                                                    # I added 'self.'s to use the class variable
                                                    #makes it usable outside initmodel
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
                            #Initialize new run
                            self.alphabet = list(strData[12:])
                            self.K = len(self.alphabet)
                            self.initModel()
                            log(f'New alphabet: {alphabet}')
                            log(f'Sending acknowledgment')
                            conn.sendall('$$$ack'.encode('ASCII'))

                        elif strData.startswith('hit me'):
                            r = -0.01  
                            #TODO:  extract the reward from the hit me string
                            #and put in 'r'
                            self.logReward(r)
                            letter = self.getNextActionFromQ()
                            #TODO:  save the letter  e selected as an int, in an
                            #instance var
                            self.lastAction = int(letter) #TODO: something LIKE this but a=0, b=1, etc.

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
