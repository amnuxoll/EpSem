from pytorchUtils import log
import random

class pytorchDQNEnv:
    '''
    Tracks environment variables
    '''

    def __init__(self):
        self.alphabet = []              # The set of all valid actions an agent can take at any given point
        self.entire_history = ''        # List of all actions the agent has taken so far
        self.last_step = ''             # Agent's most recent action
        self.steps_since_last_goal = 0  # Steps since last goal
        self.num_goals = 0              # Number of goals the agent has found so far
        self.avg_steps = 0              # Average number of steps the agent has taken to find a goal
        self.overall_alphabet = []      # If alphabet is [a,b,c] then overall_alphabet = [a,b,c,A,B,C]
        self.window_size = 5            # Set a default window size

    def update_avg_steps(self):
        '''
        Calculate average steps to goal from the entire_history.
        '''
        # Count the number of goals
        num_historical_goals = sum(1 for c in self.entire_history if c.isupper())

        if num_historical_goals <= 0:
            self.avg_steps = len(self.entire_history)
        else:
            self.avg_steps = len(self.entire_history) / num_historical_goals
            
    def get_state(self):
        '''
        Get the current state representation for the model.
        '''
        window = self.entire_history[-self.window_size:]
        state_vector = self.flatten(window)
        return state_vector, window

    def step(self, state, action):
        '''
        Simulate taking an action in the environment.
        '''
        # Unpack the state
        state_vector, window = state
        # Update the environment's history
        self.entire_history += action
        # Generate the next window
        next_window = window[-(self.window_size - 1):] + action
        next_state_vector = self.flatten(next_window)
        reward = self.get_reward(action)
        done = action.isupper()  # Terminal if action is a goal
        return (next_state_vector, next_window), reward, done

    def get_reward(self, action):
        '''
        Define the reward function.
        '''
        if action.isupper():
            return 1.0  # Reward for reaching a goal
        else:
            return -0.1  # Small penalty to encourage reaching goals
    
    def flatten(self, window):
        '''
        Convert a window into a numerical state representation.
        '''
        indexes = {char: idx for idx, char in enumerate(self.overall_alphabet)}
        
        state_vector = [0.0] * (len(self.overall_alphabet) * self.window_size)
        num_actions = len(self.overall_alphabet)
        window_size = self.window_size
        
        for i, char in enumerate(window):
            if char in indexes:
                key = indexes[char]
                index = i + key * window_size
                # Add debugging statements
                # log(f"i: {i}, char: '{char}', key: {key}, index: {index}, state_vector length: {len(state_vector)}")
                if index >= len(state_vector) or index < 0:
                    log(f"Error: Calculated index {index} is out of bounds.")
                state_vector[index] = 1.0
            else:
                log(f"Warning: Character '{char}' not in overall_alphabet.")
        # log("Inside flatten...")
        # log(f"state_vector: {state_vector}")
        # log(f"window: {window}")
        # log("Leaving flatten...")
        return state_vector