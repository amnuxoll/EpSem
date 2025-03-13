from TFSocketUtils import log

class TFSocketEnv:
    '''
    TFSocketEnv tracks the environment variables used by the TFSocket and TFSocketModels
    '''
    
    def __init__(self):
        self.alphabet = []              # The set of all valid actions an agent can take at any given point
        self.history = ''               # List of all actions the agent has taken so fara
        self.steps_since_last_goal = 0  # Self explanatory lol
        self.num_goals = 0              # Number of goals the agent has found so far
        self.avg_steps = 0              # Average number of steps the agent has taken to find a goal
        self.overall_alphabet = []      # If alphabet is [a,b,c] then overall_alphabet = [a,b,c,A,B,C]
        self.retrained = False          # When in a loop, make sure to retrain the models only once
        self.next_step = ''             # Agent's next action

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
