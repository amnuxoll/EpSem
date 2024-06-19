from TFSocketUtils import log

class TFSocketEnv:
    
    def __init__(self):
        self.alphabet = []
        self.entire_history = ''
        self.last_step = ''
        self.steps_since_last_goal = 0
        self.num_goals = 0
        self.avg_steps = 0

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
