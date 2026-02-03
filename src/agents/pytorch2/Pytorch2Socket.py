import re
import sys
import socket
import os
import random
import traceback
from dfa_observer import DFAObserver
import torch
import torch.optim as optim
import torch.nn as nn
from collections import deque

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
        self.n_hist                 = 3
        self.episodes               = 500
        self.gamma                  = 0.99  # discount factor
        self.lr                     = 1e-3  # learning rate
        self.batch_size             = 64    # number of actions that is remembered for q-value
        self.buffer_capacity        = 20_000
        self.start_learning_after   = 500   # num random actions to take before learning
        self.target_update_every    = 200
        self.eps_start              = 1.0   # epsilon value at start of training
        self.eps_end                = 0.05  # epsilon value at end of training
        self.eps_decay_steps        = 5000  # over how many steps to decay epsilon to end value
        self.seed                   = 0
        self.device                 = "cpu"

        self.global_step = 0
        self.grad_steps  = 0
        self.success_log = []
        self.length_log  = []
        self.done        = False
        self.total_r     = 0.0
        self.steps       = 0

        self.lastAction = 0   #the last action we took is saved here so we can
                              #log the state, action + reward combo together
                              #after the env
        self.lastState       = None
        self.episode_rewards = []

        self.alphabet = []  #this gets init'd in main() with '$$$alphabet' call

        #The following variables all get init'd in initModel()
        self.K = -1 
        self.myobserver = None
        self.obs_dim = -1
        self.n_actions = -1
        self.q = None
        self.qt = None

        self.buf = ReplayBuffer(self.buffer_capacity)
        

    # epsilon(step)
    #   epsilon is the probability of taking a random action vs best-known action
    #   at each step, aka exploration vs exploitation. It decays from eps_start to 
    #   eps_end over eps_decay_steps steps.
    #
    #   Parameters:
    #       step - current global step count
    #   
    def epsilon(self, step):
        # if we reach our limit of decay steps, return eps_end
        if self.eps_decay_steps <= 0: return self.eps_end

        # calculate linear decay based on the fraction of the decay limit 
        # we are currently at, if this is < 1.0 then we are still decaying
        t = min(1.0, step / self.eps_decay_steps)

        return self.eps_start + (self.eps_end - self.eps_start) * t


    # initModel()
    #   Creates a new Q-Learning model.  This method can't be called until
    #   we know the alphabet size. See line 272 in main().
    #
    def initModel(self):
        self.K          = len(self.alphabet)  # number of actions
        self.myobserver = DFAObserver(n_hist=self.n_hist, K=self.K, seed=self.seed)
        self.obs_dim    = self.n_hist * (self.K + 1)
        self.n_actions  = self.K
        self.q          = QTrain.QNet(self.obs_dim, self.n_actions).to(self.device)
        self.qt         = QTrain.QNet(self.obs_dim, self.n_actions).to(self.device)

        self.qt.load_state_dict(self.q.state_dict())
        self.qt.eval()

        self.opt        = optim.Adam(self.q.parameters(), lr=self.lr)

        #This code is originally exec'd each time the agent starts a new run. It
        # was originally in the outer for-loop in Yuji's dqn_train() method.
        self.myobserver.reset()
        self.myobserver.observe(0)
        #Note: changed 's' from Yuji's code to self.lastState
        self.lastState = self.myobserver.encode().to(self.device) 


    # recordGoal()
    #   This method is called each time the agent completes a run. This code was
    #   originally in the outer for-loop in Yuji's dqn_train() method.
    #
    def recordGoal(self):
        self.success_log.append(1 if self.total_r > 0 else 0)
        self.length_log.append(self.steps)
        self.episode_rewards.append(self.total_r)


    # getNextActionFromQ
    #   This method is the "connection" between the framework code and the code
    #   written by Yuji Sakabe that creates/manages the PyTorch model.
    #   See his dqn_train.py to see what the original looked like.
    #
    def getNextActionFromQ(self):

        #NOTE:  If the agent gets in a loop (seems like it won't happen)
        #       then code will be needed here to enforce a "max steps"
        #       and only take random actions
        #       to force exploring, when exploit is not working


        if random.random() < self.epsilon(self.global_step):
            log("python: choosing a random action")
            action = random.randint(0, len(self.alphabet)-1)
        else:
            log("python: choosing a PyTorch action")
            with torch.no_grad():
                qvals = self.q(self.lastState.unsqueeze(0))
                action = int(torch.argmax(qvals, dim=1).item())
        return action


    # logReward(r)
    #   Records the agent's reward (r) for its last action
    #
    def logReward(self, r):

        obs_next = self.lastAction + 1

        self.myobserver.observe(obs_next)
        currState = self.myobserver.encode().to(self.device) # originally sp, changed to currState

        self.buf.push(self.lastState, self.lastAction, r, currState, False)

        self.lastState = currState
        self.total_r += r

        if len(self.buf) >= self.start_learning_after: # num random actions to take before learning
            # References: S: lastState, A: lastAction, R: reward, SP: currState, D: done
            S, A, R, SP, D = self.buf.sample(self.batch_size)
            S, A, R, SP, D = (S.to(self.device), A.to(self.device),
                              R.to(self.device), SP.to(self.device), D.to(self.device))

            q_sa = self.q(S).gather(1, A.unsqueeze(1)).squeeze(1)

            with torch.no_grad():
                target = R + (1.0 - D) * self.gamma * self.qt(SP).max(1).values

            loss = nn.functional.mse_loss(q_sa, target)
            self.opt.zero_grad()
            loss.backward()
            self.opt.step()
            self.grad_steps += 1
            if self.grad_steps % self.target_update_every == 0:
                self.qt.load_state_dict(self.q.state_dict())

    def getQ(self):
        return self.q, {"success":self.success_log, "lengths":self.length_log,
                        "rewards":self.episode_rewards}

    class QNet(nn.Module):
        def __init__(self, obs_dim: int, n_actions: int, hidden: int = 64):
            super().__init__()
            self.net = nn.Sequential(
                nn.Linear(obs_dim, hidden), nn.ReLU(),
                nn.Linear(hidden, hidden), nn.ReLU(),
                nn.Linear(hidden, n_actions),
            )

        def forward(self, x): return self.net(x)

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
                            self.alphabet = list(strData[12:])
                            self.initModel()
                            log(f'New alphabet: {self.alphabet}')
                            log(f'Sending acknowledgment')
                            conn.sendall('$$$ack'.encode('ASCII'))

                        elif strData.startswith('hit me'):
                            #extract the reward from the hit me string
                            #NOTE:  No error checking here...
                            r = float(strData[6:])
                            
                            #and put in 'r'
                            self.logReward(r)

                            action = self.getNextActionFromQ() # action is returned as an integer index

                            #DEBUG (remove later)
                            log("received reward: " + str(r))
                            
                            self.lastAction = action

                            #convert action to a letter that the Java side is
                            #expecting
                            letter = self.alphabet[action]
                            
                            # Send the model's prediction to the environment
                            conn.sendall(letter.encode('ASCII'))
                            log(f'sending action, {letter}')
                            self.global_step += 1

                        elif strData.startswith('$$$quit'):
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
