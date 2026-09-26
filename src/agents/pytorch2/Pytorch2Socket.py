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

        # new variables for sensor data
        self.sensor_names = []
        self.n_sensors = 0

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
        self.myobserver = DFAObserver(n_hist=self.n_hist, n_sensors=self.n_sensors, K=self.K, seed=self.seed)
        self.obs_dim    = self.n_hist * (self.n_sensors + 1) # changed from self.n_hist * (self.K + 1)
        self.n_actions  = self.K
        self.q          = QTrain.QNet(self.obs_dim, self.n_actions).to(self.device)
        self.qt         = QTrain.QNet(self.obs_dim, self.n_actions).to(self.device)

        self.qt.load_state_dict(self.q.state_dict())
        self.qt.eval()

        self.opt        = optim.Adam(self.q.parameters(), lr=self.lr)

        #This code is originally exec'd each time the agent starts a new run. It
        # was originally in the outer for-loop in Yuji's dqn_train() method.
        self.myobserver.reset()
        # self.myobserver.observe(0)
        self.myobserver.reset()
        #Note: changed 's' from Yuji's code to self.lastState
        # self.lastState = self.myobserver.encode().to(self.device) 
        self.lastState = None


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
    def logReward(self, r, sensor_bits, done):
        sensor_values = [
            int(bit)
            for bit in sensor_bits
        ]

        # previous action becomes the obs token
        obs_next = self.lastAction + 1 # observation token based on the previous action
        # actions are represented by numbers:
        # 0 = no previous action, initial observation
        # 1 = action 0
        # 2 = action 1
        # 3 = action 2

        # DFAObserver vocabulary: 0..K since self.dim_slot = K + 1
        # observer is appending this obs_next token to its history
        # since our self.n_hist = 3, we are remembering the last 3 tokens/actions
        self.myobserver.observe(obs_next, sensor_values)

        # Now we want to convert the history into the numerical state that goes into the NN
        # encode() --> creates a matrix, a 12-element vector representing the current state
        # for every token, its position in the matrix gets set to 1
        # Since K = 3, our matrix has 4 colomns (0, 1, 2, 3) representing each action
        # example: if our history is [0, 2, 1],
        # then the one-hot becomes:
        # token 0 --> [1, 0, 0, 0]
        # token 2 --> [0, 0, 1, 0]
        # token 1 --> [0, 1, 0, 0]
        # So: [
        # [1,0,0,0],
        # [0,0,1,0],
        # [0,1,0,0]
        #]
        # these are then flattended (.flatten()) to create the 12-element vector representing the current state
        # [1,0,0,0, 0,0,1,0, 0,1,0,0]
        currState = self.myobserver.encode().to(self.device) # originally sp, changed to currState

        # First observation: no previous
        if self.lastState is None:
            self.lastState = currState
            self.total_r = 0.0
            return

        # This stores one experience/replay tuple in our buffer
        # This is the data that the DQN learns from
        #
        # maybe add bitSet as a param
        self.buf.push(self.lastState, self.lastAction, r, currState, done)

        self.lastState = currState
        self.total_r += r # Not directly part of the NN learning, just keeps track of the total 
                            # reward in the current run/episode
        # recordGoal uses total_r to determine a success (positive is success)

        # records everything onto the device: cpu
        # if we wanted to use a CUDA GPU, then we'd move the tnesors onto that
        #
        # maybe add the bitSet as a param to the buffer?
        if len(self.buf) >= self.start_learning_after: # num random actions to take before learning
            # References: S: lastState, A: lastAction, R: reward, SP: currState, D: done
            S, A, R, SP, D = self.buf.sample(self.batch_size)
            S, A, R, SP, D = (S.to(self.device), A.to(self.device),
                              R.to(self.device), SP.to(self.device), D.to(self.device))

            # calculate the Q-value for the action actually taken
            # q = neural network, takes a state and outputs a Q-value for every possible action
            # q_sa contains the Q-value corresponding to the action that was actually taken
            # gather --> finding the prediction/Q-value for a specific action in a specific state
            # this action is the last-taken action in the last state
            q_sa = self.q(S).gather(1, A.unsqueeze(1)).squeeze(1)

            # tell PyTorch not to calculate gradients for the caluclations inside this block
            # because we are treating the target as a fixed value that we're trying to make our
            # current network prediction approach
            with torch.no_grad():
                # core Q-learning equation:
                # target = immediate reward + discounted value of the best next action
                # gamma, or the discount factor, = 0.99
                # this means future rewards are highly important, but slightly less important than immediate rewards
                # self.qt(SP) --> feeds next state into the target network
                # which outputs Q-values for every possible action in that next state
                # .max(1).values --> selects the highest predicted Q-value
                # "If I reach this next state, what's the best future value I currently know about?"
                # SO, it's 0.99 * future value
                # remember: PEMDAS
                target = R + (1.0 - D) * self.gamma * self.qt(SP).max(1).values

            # calculate the loss
            # "How different is the neural network's current prediction from the Q-learning target?"
            # mse --> mean squared error, measures (network prediction - target)^2
            # Remember: goal of training is to make q_sa ~= target
            loss = nn.functional.mse_loss(q_sa, target)
            self.opt.zero_grad() # clears old gradients, PyTorch accumulates gradients by default, need to do this before calculating a new gradient
            loss.backward() # PyTorch calculates: "How should each NN weight change to reduce this loss?"
            self.opt.step() # update the NN, NN weights are changed here
            self.grad_steps += 1 # increment this counter when the gradient updates

            # update the target network periodically
            # for every 200 gradient updates, we copy the weights into the target network
            # Both qt and q are networks, qt stays fixed for 200 training updates at a time
            # this is so that the target isn't constantly moving while we're trying to elarn toward it
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
        print(f'port number: {portNum}')
        timeout = 0
        log(f'port number: {portNum}')
        log('Creating server for the Java environment to connect to...')

        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
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

                        # Startup Sequence:
                        # $$$alphabet:
                        #     ↓
                        # store actions

                        # $$$sensors:
                        #     ↓
                        # store sensor names
                        #     ↓
                        # calculate observation dimension
                        #     ↓
                        # create observer
                        #     ↓
                        # create Q-network
                        if strData.startswith('$$$alphabet:'):
                            alphabet_string = strData[len("$$$alphabet:"):]
                            self.alphabet = list(alphabet_string)
                            # self.initModel() # we need to do this when we get the sensor names
                            log(f'New alphabet: {self.alphabet}')
                            log(f'Sending acknowledgment')
                            conn.sendall('$$$ack'.encode('ASCII'))

                            continue

                        elif strData.startswith("$$$sensors:"):
                            sensor_string = strData[len("$$$sensors:"):]

                            self.sensor_names = sensor_string.split(",")
                            log(f"Sensors: {self.sensor_names[0:]}")

                            self.n_sensors = len(self.sensor_names)

                            self.initModel() # now we have everything we need so call initModel()

                            continue

                        elif strData.startswith('hit me'):
                            # adjust processing to accept the reward and the bitSet of the sensors
                            # extract the reward from the hit me string
                            #NOTE:  No error checking here...
                            #r = float(strData[6:])

                            chunk = strData.split()
                            # chunk[0] --> "hit"
                            # chunk[1] --> "me"
                            # chunk[2] --> reward
                            # chunk[3] --> sensor bits as a string
                            r = float(chunk[2])

                            sensor_bits = chunk[3]
                            log("SensorData bits: " + sensor_bits) # prints entire bitSet

                            done = chunk[4].lower() == "true"
                            
                            #and put in 'r'
                            # changed from just r being passed to incorporate sensor data
                            self.logReward(r, sensor_bits, done)

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
