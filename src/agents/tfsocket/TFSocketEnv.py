from TFSocketUtils import log

class TFSocketEnv:
    '''
    TFSocketEnv tracks the environment variables used by the TFSocket and TFSocketModels
    '''
    
    def __init__(self):
        self.alphabet = []              # The set of all valid actions an agent can take at any given point
        self.entire_history = ''        # List of all actions the agent has taken so far
        self.last_step = ''             # Agent's most recent action
        self.steps_since_last_goal = 0  # Self explanatory lol
        self.num_goals = 0              # Number of goals the agent has found so far
        self.avg_steps = 0              # Average number of steps the agent has taken to find a goal
        self.overall_alphabet = []      # If alphabet is [a,b,c] then overall_alphabet = [a,b,c,A,B,C]
        self.num_sensors = 0            # Number of sensors in the environment

    # NOTE: Double check if this function is necessary
    # or if we can just update the avg_steps by doing (avg_steps + steps_since_last_goal) / num_goals + 1
    def update_avg_steps(self):
        '''
        Calculate average steps to goal from the entire_history
        '''
        num_historical_goals = 0
        for i in range(len(self.entire_history)):
            if (self.entire_history[i].isupper()):
                num_historical_goals += 1

        if num_historical_goals <= 0:
            self.avg_steps = len(self.entire_history)
        else:
            self.avg_steps = len(self.entire_history) / num_historical_goals
        # log("Average steps to goal: {}".format(self.avg_steps))
    
    def get_num_sensors(self, history):
        '''
        Get the number of sensors from the history string
        '''
        for char in history:
            # Assuming any character not in the alphabet is a sensor
            if char not in self.overall_alphabet:
                self.num_sensors += 1
            else:
                break
