import tensorflow as tf
import contextlib
from TFSocketUtils import log

class TFSocketModel:
    '''
    TFSocketModel uses tensorflow to read a history of past
    steps into tensors then calculates a probable next
    step to reach the goal.
    '''

    def __init__(self, environment, window_size):
        '''
        define object variables and create the model function-variable
        '''
        self.environment = environment
        self.window_size = window_size
        
        # Defining the model function as a variable
        self.model = tf.keras.models.Sequential([
            tf.keras.layers.Dense(16, activation='relu'),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(4, activation='softmax')
        ])
        
    def get_letter(self, prediction):
        '''
        given a set of predictions, returns the letter corresponding to the action with the highest confidence
        '''
        max_index = 0 # index of the largest value in prediction[0][max_index]
        for i in range(len(prediction[0])):
            if prediction[0][i] > prediction[0][max_index]:
                max_index = i
        match max_index:
            case 0:
                return 'a'
            case 1:
                return 'A'
            case 2:
                return 'b'
            case 3:
                return 'B'
        log(f'Error: There is no valid max-value (or most likely) of the prediction values: {prediction[0]}')
        return

    def simulate_model(self):
        '''
        Simulate the model's prediction of the next step. 
        
        The model predicts what action is best for the next step.  Then it appends that action
        to a local copy of the agent action history so as to create a predicted history.  This,
        in turn, is used to make yet another prediction so the agent can look several steps
        into the future.
        
        The iteration stops when a goal is reached or, otherwise, when some maximum is reached.  
        Because goal states are rare, the agent rarely predicts one.  Thus, the maximum is
        usually reached.  This is undesirable so this method inserts a 'artificial' goal
        at the point where a goal was predicted most strongly.  
        '''
        max_len = 50
        window = self.environment.entire_history[-self.window_size:]
        log('window=' + str(window))
        predictions = self.get_predictions(window)
        first_sim_action = self.get_letter(predictions)

        #while a non-goal letter may be best, at each step we want to track which GOAL 
        #letter had the highest prediction and when it occurred
        predictions = predictions[0]  #reduce tensor to 1D list
        best_goal_prediction = max(predictions[1], predictions[3])
        best_goal_letter = 'A'
        best_goal_index = 0
        index = 0
        if (best_goal_prediction == predictions[3]):
            best_goal_letter = 'B'

        #Simulate future steps
        sim_path = first_sim_action
        while not ( (sim_path[-1:] == 'A') or (sim_path[-1:] == 'B') ):
            if len(sim_path) < max_len:  #TODO:  use double avg_steps instead 
                log(f'Simulation: {sim_path}')
            else:
                log('Simulation: Path too long, stopping.')
                break
            sim_hist = self.environment.entire_history + sim_path
            window = sim_hist[-self.window_size:]
            log('window=' + str(window))
            predictions = self.get_predictions(window)
            next_sim_action = self.get_letter(predictions)
            sim_path += next_sim_action
            
            #update best_goal data
            index += 1
            predictions = predictions[0]  #reduce tensor to 1D list
            if (predictions[1] > best_goal_prediction):
                best_goal_prediction = predictions[1]
                best_goal_letter = 'A'
                best_goal_index = index
            if (predictions[3] > best_goal_prediction):
                best_goal_prediction = predictions[3]
                best_goal_letter = 'B'
                best_goal_index = index            

        #If necessary, truncate the sim_path with an artificial goal
        if (len(sim_path) == max_len) and (sim_path[-1:].islower()):
            sim_path = sim_path[:best_goal_index]
            sim_path += best_goal_letter

        log(f'Simulation prediction complete: {sim_path}')

    def train_model(self):
        '''
        Train and optimize a new ANN model
        
        Refer to the beginner tensorflow tutorial:
        https://www.tensorflow.org/tutorials/quickstart/beginner
        '''
        # Defining the inputs and expected outputs from a given history
        self.environment.update_avg_steps()
        x_train = self.calc_input_tensors(self.environment.entire_history)
        y_train = self.calc_desired_actions(self.environment.entire_history)
        x_train = tf.constant(x_train)
        y_train = tf.constant(y_train)

        # Defining the model's loss function
        predictions = self.model(x_train[:1])
        predictions = tf.nn.softmax(predictions).numpy() # Convert to probabilities
        loss_fn = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)

        # Optimize and train the model
        self.model.compile(optimizer='adam', loss=loss_fn, metrics=['accuracy'])
        with contextlib.redirect_stdout(open('trainout.txt', 'w')):
            self.model.fit(x_train, y_train, epochs=5, verbose=2)

    def calc_input_tensors(self, given_window):
        '''
        Convert a blind FSM action window into an array of input tensors
        NOTE: the given_window is often larger than the window_size
                but the 'window' is of window_size

        Example:
            if window_size = 3, and given_window = 'abbBababAa'
            first it creates each window:
                abb, bbB, bBa, etc.
            then it converts each window to a tensor
                abb -> [1, 0, 0, 0, 1, 1, 0, 0, 0]
                bAb -> [0, 1, 0, 1, 0, 1, 0, 1, 0]
            then it creates a list of all the input tensors
        '''
        # Sanity check
        if (len(given_window) < self.window_size):
            log('ERROR: window too short for training!')
            return

        # Generate a list of the index of the last character in each window_size chunk
        # Example:
        #   If window_size = 3, and len(given_window) = 10,
        #   then it will output [3, 4, 5, 6, 7, 8, 9]
        given_window_range = list(range(len(given_window)))[self.window_size:]
        tensor = []
        for i in given_window_range:
            window = given_window[i-self.window_size:i]
            tensor.append(self.flatten(window))

        return tensor

    def get_predictions(self, window):
        '''
        Uses the model to generate predictions about the next step for the environment
        
        This takes the form of a 2D array like this (to support TensorFlow):
            [[0.35471925 0.19711517 0.25747794 0.19068767]]
        '''
        one_input = [self.flatten(window)]
        one_input = tf.constant(one_input)
        predictions = self.model(one_input)
        predictions = tf.nn.softmax(predictions).numpy()
        log(f'Predictions: {predictions}')
        return predictions

    def calc_desired_actions(self, window):
        '''
        Calculates the desired action for each window in the entire_history
        
        The model wants expected outputs to be a set of integer categories.
        So we arbitrarily assign:  a=0, A=1, b=2, B=3
        
        the output of this function should line up with the output of 
        calc_input_tensors for the same window

        window - in a letter-based format (e.g., aabaBbbabaabaBbbab...)
        cutoff - distance from curr pos to goal at which the agent prefers 
                 NOT to take the historical action
        '''
        # Sanity check
        if (len(window) < self.window_size):
            log('ERROR:  window too short for training!')
            return

        # Iterate over a range starting at index window_size in the window
        hrange = list(range(len(window)))[self.window_size:]
        desired_actions = []
        for i in hrange:
            # Calculate how many steps from this position to the next goal
            num_steps = 0
            while(window[i + num_steps].islower()):
                num_steps += 1

            # Calculate the index of the action of this position
            # 0 = a, 1 = A, 2 = b, 3 = B
            val = -1
            match window[i]:
                case 'a':
                    val = 0
                case 'A':
                    val = 1
                case 'b':
                    val = 2
                case 'B':
                    val = 3
                    
            if val == -1:
                log('ERROR:  invalid character in window')
                return

            # If the cutoff is exceeded then we want the other action instead
            # Examples:
            #   I.   if val is 'a' (=0), set val to 'b' (=2)
            #   II.  if val is 'A' (=1), set val to 'b' (=2)
            #   III. if val is 'b' (=2), set val to 'a' (=0)
            #   IV.  if val is 'B' (=3), set val to 'a' (=0)
            if (num_steps >= self.window_size): 
                if val < 1.5:
                    val = 2
                else:
                    val = 0

            # Add to result
            desired_actions.append(val)

        return desired_actions

    def flatten(self, window):
        '''
        Convert a window into an input tensor
        Helper function for @calc_input_tensors()
        
        A window has this format:      'aabaBbbab'
        the return object's length is 3*window_size
        being whether or not an 'a'/'A' is in that position
        the second set is for b/B.  The third set is goal sensor.

        TODO:  This is hard-coded for a two-letter ALPHABET. Change
            the code to handle any ALPHABET size
        TODO:  In the future we may want to handle sensors. 
            Example input window:       '01a11B00b00a'
        '''
        win_size = len(window)

        # Split the window into goal part and action part
        ays = [0.0 for i in range(win_size)]
        bees = [0.0 for i in range(win_size)]
        goals = [0.0 for i in range(win_size)]

        for i in range(win_size):
            let = window[i]
            if let == 'a' or let == 'A':
                ays[i] = 1.0
            else:
                bees[i] = 1.0
            if let == 'A' or let == 'B':
                goals[i] = 1.0

        return ays + bees + goals