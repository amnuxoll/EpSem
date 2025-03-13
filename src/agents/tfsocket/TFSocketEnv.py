import re
from TFSocketUtils import log

class TFSocketEnv:
    '''
    TFSocketEnv tracks the environment variables used by the TFSocket and TFSocketModels
    '''
    
    def __init__(self):
        self.alphabet = []              # The set of all valid actions an agent can take at any given point
        self.overall_alphabet = []      # If alphabet is [a,b,c] then overall_alphabet = [a,b,c,A,B,C]
        
        self.history = ''               # List of all steps the agent has taken so far
        self.history_since_last_goal='' # List of actions the agent has taken since last goal, including '*' when a random step was taken
        self.next_step = ''             # Agent's next action
        
        self.num_goals = 0              # Number of goals the agent has found so far
        self.avg_steps = 0              # Average number of steps the agent has taken to find a goal
        self.steps_since_last_goal = 0  # Self explanatory lol
        self.avg_of_last_five_goals = 0 # Average number of steps the agent has taken to find the last 5 goals
        self.avg_of_last_ten_goals = 0  # Average number of steps the agent has taken to find the last 10 goals
        
        self.epsilon = -1.0             # A value between 0, 1 for an explore vs exploit E-Greedy algorithm
        self.upper_bound = 1.0          # TODO: fill out comments for new egreedy vars
        self.h_shift = 17
        self.perc_forgetting = 0.0

        self.retrained = False          # When in a loop, make sure to retrain the models only once        

    def update_avg_steps(self):
        '''
        Calculate average steps to goal from the history
        '''
        # Count the number of goals (skipping the first)
        num_historical_goals = 0
        for i in range(len(self.history)):
            if (i == 0):
                continue
            if (self.history[i].isupper()):
                num_historical_goals += 1

        if num_historical_goals <= 0:
            self.avg_steps = len(self.history)
        else:
            self.avg_steps = len(self.history) / num_historical_goals
    
    
    def update_rolling_step_averages(self):
    
        def avg_steps_of_last_num_goals(num):
            num_goals_to_avg = num
            last_num_goals_pattern = rf'([a-z]*[A-Z]){{{num_goals_to_avg}}}(?=[a-z]*$)'
            last_five_goals = re.search(last_num_goals_pattern, str(self.history))
            substring = last_five_goals.group(0) if last_five_goals else ''
            self.avg_of_last_five_goals = len(substring) / num_goals_to_avg 
    
        self.avg_of_last_five_goals = avg_steps_of_last_num_goals(5)
        self.avg_of_last_ten_goals = avg_steps_of_last_num_goals(10)
