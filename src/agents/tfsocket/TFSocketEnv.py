from TFSocketUtils import log
import re

class TFSocketEnv:
    '''
    TFSocketEnv tracks the environment variables used by the TFSocket and TFSocketModels
    '''
    
    def __init__(self):
        self.alphabet = []              # The set of all valid actions an agent can take at any given point
        self.entire_history = ''        # List of all actions the agent has taken so fara
        self.last_step = ''             # Agent's most recent action
        self.steps_since_last_goal = 0  # Self explanatory lol
        self.num_goals = 0              # Number of goals the agent has found so far
        self.avg_steps = 0              # Average number of steps the agent has taken to find a goal for the entire history
        self.rolling_avg_steps = 0      # Average number of steps the agent has taken to find a goal for the last 10 goals reached
        self.overall_alphabet = []      # If alphabet is [a,b,c] then overall_alphabet = [a,b,c,A,B,C]

    def update_avg_steps(self):
        '''
        Calculate average steps to goal from the entire_history
        And calculate a rolling average of the last # NUM_GOALS_TO_AVG of steps to goal

        NOTE: We skip the first because the envrionment considers the initial state of the agent as a goal.
        This is a artifact of the java environment manager that other agents rely on.
        So we opted to ignore it here, as opposed to removing the feature from the java envrionment manager.
        '''
        NUM_GOALS_TO_AVG = 3 # NOTE: This value was choosen arbitrarily and may perform minorly differently at different FSM environments
        
        # Count the number of goals (skipping the first)
        num_historical_goals = 0
        for i in range(len(self.entire_history)):
            if i == 0:
                continue
            if self.entire_history[i].isupper():
                num_historical_goals += 1

        if num_historical_goals <= 0:
            self.avg_steps = len(self.entire_history)
        else:
            self.avg_steps = len(self.entire_history) / num_historical_goals
            
        # Calculate a rolling average of the number of steps to reach the goal for the
        # last 10 goals that were reached
        if self.num_goals < NUM_GOALS_TO_AVG:
            return # Not enough history to calculate
        pattern = rf'([a-z]*[A-Z]){{{NUM_GOALS_TO_AVG}}}(?=[a-z]*\Z)'
        match = re.search(pattern, str(self.entire_history))
        substring = match.group(0) if match else ''
        self.rolling_avg_steps = len(substring) / NUM_GOALS_TO_AVG
        log(f'\tsubstring: {substring}')
        log(f'\trolling_avg_steps: {self.rolling_avg_steps:.3f}')