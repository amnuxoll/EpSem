import torch
import torch.nn as nn
import torch.optim as optim
import random
import math
from collections import deque
from pytorchUtils import log
from collections import namedtuple
import torch.nn.functional as F
Transition = namedtuple('Transition', ('state', 'action', 'next_state', 'reward'))

class ReplayMemory:
    def __init__(self):
        self.memory = []

    def push(self, *args):
        self.memory.append(Transition(*args))

    def __len__(self):
        return len(self.memory)

    def shuffle(self):
        random.shuffle(self.memory)

    def sample(self, batch_size):
        return self.memory[:batch_size]

class DQN(nn.Module):
    def __init__(self, input_size, output_size):
        super(DQN, self).__init__()
        self.layer1 = nn.Linear(input_size, 128)
        self.layer2 = nn.Linear(128, 128)
        self.layer3 = nn.Linear(128, output_size)

    def forward(self, x):
        x = x.view(x.size(0), -1)  # Flatten the input tensor
        x = torch.relu(self.layer1(x))
        x = torch.relu(self.layer2(x))
        return self.layer3(x)

class pytorchDQNModel:
    '''
    DQN Agent
    '''

    def __init__(self, environment):
        '''
        Initialize the model and related variables.
        '''
        self.environment = environment
        self.memory = ReplayMemory()
        self.batch_size = 128
        self.gamma = 0.99
        self.eps_start = 0.9
        self.eps_end = 0.05
        self.eps_decay = 1000
        self.target_update = 10
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.steps_done = 0
        self.trained = False

        # Input size is the length of the flattened window
        self.input_size = self.environment.window_size * len(self.environment.overall_alphabet)
        # Output size is the number of possible actions
        self.output_size = len(self.environment.overall_alphabet)
        log(f'Initializing DQN with input_size={self.input_size}, output_size={self.output_size}')
        self.policy_net = DQN(self.input_size, self.output_size).to(self.device)
        self.target_net = DQN(self.input_size, self.output_size).to(self.device)
        self.target_net.load_state_dict(self.policy_net.state_dict())
        self.optimizer = optim.AdamW(self.policy_net.parameters(), lr=1e-4, amsgrad=True)
        self.criterion = nn.SmoothL1Loss()

    def select_action(self, state_vector):
        '''
        Select an action based on the current state.
        '''
        sample = random.random()
        eps_threshold = self.eps_end + (self.eps_start - self.eps_end) * \
            math.exp(-1. * self.steps_done / self.eps_decay)
        self.steps_done += 1
        # log(f'eps_threshold: {eps_threshold}')
        # log(f'state_vector length: {len(state_vector)}')
        # log(f'state_vector: {state_vector}')
        if sample > eps_threshold:
            with torch.no_grad():
                # Create state_tensor without wrapping in an extra list
                state_tensor = torch.tensor([state_vector], dtype=torch.float32, device=self.device)
                # log(f'state_tensor shape: {state_tensor.shape}')  # Should be (1, window_size, feature_size)
                # Flatten the state tensor if necessary
                state_tensor = state_tensor.view(state_tensor.size(0), -1)  # Shape: (1, input_size)
                action_values = self.policy_net(state_tensor)
                action_index = action_values.max(1)[1].item()
                return self.environment.overall_alphabet[action_index]
        else:
            return random.choice(self.environment.alphabet)

    def optimize_model(self):
        '''
        Optimize the model using experience replay.
        '''
        if len(self.memory) < self.batch_size:
            return
        transitions = self.memory.sample(self.batch_size)
        batch_state, batch_action, batch_next_state, batch_reward = zip(*transitions)

        batch_state = torch.tensor(batch_state, dtype=torch.float32, device=self.device)
        batch_action_indices = [self.environment.overall_alphabet.index(a) for a in batch_action]
        batch_action = torch.tensor(batch_action_indices, dtype=torch.int64, device=self.device).unsqueeze(1)
        batch_reward = torch.tensor(batch_reward, dtype=torch.float32, device=self.device)

        non_final_mask = torch.tensor(tuple(map(lambda s: s is not None, batch_next_state)), device=self.device, dtype=torch.bool)
        non_final_next_states = torch.tensor([s for s in batch_next_state if s is not None], dtype=torch.float32, device=self.device)

        state_action_values = self.policy_net(batch_state).gather(1, batch_action)

        next_state_values = torch.zeros(self.batch_size, device=self.device)
        with torch.no_grad():
            next_state_values[non_final_mask] = self.target_net(non_final_next_states).max(1)[0]
        expected_state_action_values = (next_state_values * self.gamma) + batch_reward

        loss = self.criterion(state_action_values.squeeze(), expected_state_action_values)
        self.optimizer.zero_grad()
        loss.backward()
        torch.nn.utils.clip_grad_value_(self.policy_net.parameters(), 100)
        self.optimizer.step()

        # Update the target network
        if self.steps_done % self.target_update == 0:
            self.target_net.load_state_dict(self.policy_net.state_dict())

    def process_entire_history(self):
        '''
        Process the entire history to extract transitions and store them in memory.
        '''
        history = self.environment.entire_history
        window_size = self.environment.window_size
        num_actions = len(self.environment.overall_alphabet)

        # Initialize the starting index
        for idx in range(len(history) - window_size):
            window = history[idx:idx + window_size]
            next_window = history[idx + 1:idx + window_size + 1]

            # Ensure windows are of correct size
            if len(window) < window_size or len(next_window) < window_size:
                continue

            # Get state and next_state vectors
            state_vector = self.environment.flatten(window)
            next_state_vector = self.environment.flatten(next_window)

            # Get action and reward
            action = history[idx + window_size]
            reward = self.environment.get_reward(action)

            # Store the transition
            self.memory.push(state_vector, action, next_state_vector, reward)
            
    def train_model(self):
        '''
        Train the model using the collected experience from entire_history.
        '''
        # First, process the entire history to fill the memory
        self.process_entire_history()

        batch_size = 32  # Set an appropriate batch size
        num_epochs = 10  # Number of epochs to train over the dataset

        for epoch in range(num_epochs):
            # Shuffle the memory for each epoch
            self.memory.shuffle()

            # Iterate over the memory in batches
            for i in range(0, len(self.memory), batch_size):
                transitions = self.memory.sample(batch_size)
                # Prepare batch data
                batch = Transition(*zip(*transitions))

                state_batch = torch.tensor(batch.state, dtype=torch.float32, device=self.device)
                action_batch = torch.tensor([self.environment.overall_alphabet.index(a) for a in batch.action], device=self.device)
                reward_batch = torch.tensor(batch.reward, dtype=torch.float32, device=self.device)
                non_final_mask = torch.tensor(tuple(map(lambda s: s is not None, batch.next_state)), device=self.device, dtype=torch.bool)

                non_final_next_states = torch.tensor([s for s in batch.next_state if s is not None], dtype=torch.float32, device=self.device)

                # Compute Q(s_t, a)
                state_action_values = self.policy_net(state_batch).gather(1, action_batch.unsqueeze(1))

                # Compute V(s_{t+1}) for all next states.
                next_state_values = torch.zeros(batch_size, device=self.device)
                next_state_values[non_final_mask] = self.target_net(non_final_next_states).max(1)[0].detach()

                # Compute the expected Q values
                expected_state_action_values = (next_state_values * self.gamma) + reward_batch

                # Compute Huber loss
                loss = F.smooth_l1_loss(state_action_values.squeeze(), expected_state_action_values)

                # Optimize the model
                self.optimizer.zero_grad()
                loss.backward()
                self.optimizer.step()

            # Optionally update the target network
            if epoch % self.target_update == 0:
                self.target_net.load_state_dict(self.policy_net.state_dict())