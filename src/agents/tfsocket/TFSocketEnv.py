from TFSocketUtils import log

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
        self.avg_steps = 0              # Average number of steps the agent has taken to find a goal

    def update_avg_steps(self):
        self.avg_steps = self.calc_avg_steps(self.entire_history)

    def calc_avg_steps(self, window):
        '''
        Calculate average steps to goal for a given window
        '''
        # Count the number of goals (skipping the first)
        num_historical_goals = 0
        for i in range(len(window)):
            if (i == 0):
                continue
            if (window[i].isupper()):
                num_historical_goals+=1

        avg_steps = len(window) / num_historical_goals
        log(f'Avg steps of window: {avg_steps}')
        return avg_steps
