import tensorflow as tf
import contextlib
import random
import os
# import inspect
from TFSocketUtils import log

class TFSocketModel:
    '''
    TFSocketModel uses tensorflow to read a history of past
    steps into tensors then calculates a probable next
    step to reach the goal.
    '''

    def __init__(self, environment):
        '''
        define object variables and create the model function-variable
        '''
        self.environment = environment
        self.window_size = 5
        self.sim_path = None # Predicted shortest path to goal (e.g., abbabA)
        self.model = None
        self.is_tuned = False
        # tf.random.set_seed(42)

    def get_letter(self, prediction):
        '''
        given a set of predictions, returns the letter corresponding to the action with the highest confidence
        '''
        max_index = 0 # index of the largest value in prediction[0][max_index]
        
        # Sanity check that the # of predictions matches the # of predictions that need to be made
        if len(self.environment.overall_alphabet) != len(prediction[0]):
            log('ERROR: # of predictions does not match the # of predictions that need to be made')
            log(f'\toverall_alphabet = {self.environment.overall_alphabet}')
            log(f'\tprediction = {prediction}')
            exit(-1)
        
        for i in range(len(prediction[0])):
            if prediction[0][i] > prediction[0][max_index]:
                max_index = i
        
        return self.environment.overall_alphabet[max_index]

    def simulate_model(self):
        '''
        Simulate the model's prediction of the next step and place it in self.sim_path

        The model predicts what action is best for the next step. Then it appends that action
        to a local copy of the agent action history so as to create a predicted history. This,
        in turn, is used to make yet another prediction so the agent can look several steps
        into the future.

        The iteration stops when a goal is reached or, otherwise, when some maximum is reached.
        Because goal states are rare, the agent rarely predicts one. Thus, the maximum is
        usually reached. This is undesirable so this method inserts a 'artificial' goal
        at the point where a goal was predicted most strongly.
        '''
        # log("Simulating model")

        if self.model is None:
            log("ERROR: Model should not be None in simulate_model()")
        window = self.environment.history[-self.window_size:]
        predictions = self.get_predictions(window)
        first_sim_action = self.get_letter(predictions)

        # While a non-goal letter may be best, at each step we want to track which goal-letter
        # had the highest prediction and when it occurred
        # [a,b,c,A,B,C]
        predictions = predictions[0] # Reduce tensor to 1D list
        
        # TODO: Double check the correctness/efficiency of this algorithm, Penny does not like this
        goal_predictions = predictions[len(self.environment.alphabet):]
        max_prediction = max(goal_predictions)  # Get the max prediction value for only goal letters
        max_index = list(goal_predictions).index(max_prediction)+len(self.environment.alphabet)
        best_goal = {'prediction': max_prediction,
                     'letter': self.environment.overall_alphabet[max_index],
                     'index': 0}

        max_len = self.environment.avg_steps
        index = 0
        # Simulate future steps
        self.sim_path = first_sim_action
        while not self.sim_path[-1:].isupper():
            if len(self.sim_path) >= max_len:
                # log('sim_path reached max length, truncate to most probable goal prediction.')
                break # I suspect the while loop exits here virtually always

            # log(f'Model sim_path: {self.sim_path}')

            # Simulate the next step from entire_path and sim_path so far
            sim_hist = self.environment.history + self.sim_path
            window = sim_hist[-self.window_size:]
            predictions = self.get_predictions(window)
            self.sim_path += self.get_letter(predictions)

            # Update best_goal data
            index += 1
            predictions = predictions[0] # Reduce tensor to 1D list
            for i in range(len(self.environment.overall_alphabet))[len(self.environment.alphabet):]:
                if predictions[i] > best_goal['prediction']:
                    best_goal['prediction'] = predictions[i]
                    best_goal['letter'] = self.environment.overall_alphabet[i]
                    best_goal['index'] = index

        # If necessary, truncate the sim_path with an artificial goal
        if (len(self.sim_path) >= max_len) and (self.sim_path[-1:].islower()):
            self.sim_path = self.sim_path[:best_goal['index']]
            self.sim_path += best_goal['letter'] 

    # Define a model-building function for Keras Tuner
    def build_model(self):
        model = tf.keras.Sequential()
        model.add(tf.keras.Input(shape=(self.window_size, len(self.environment.overall_alphabet))))
        alph_size = len(self.environment.overall_alphabet)
        model.add(tf.keras.layers.LSTM(units=alph_size, activation='relu'))
        model.add(tf.keras.layers.Dropout(0.2))
        model.add(tf.keras.layers.Dense(units=alph_size, activation='relu'))
        model.add(tf.keras.layers.Dense(len(self.environment.overall_alphabet),activation='softmax'))
        
        # Tune the learning rate for the optimizer
        learning_rate = 1e-2
        
        loss_fn = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)
        model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
                    loss=loss_fn,
                    metrics=['accuracy'])
        return model
    
    def train_model(self):
        '''
        Train and optimize a new ANN model

        Refer to the beginner tensorflow tutorial:
        https://www.tensorflow.org/tutorials/quickstart/beginner
        '''
        # Defining the inputs and expected outputs from a given history
        self.environment.update_avg_steps()    
        x_train = self.calc_input_tensors(self.environment.history)
        y_train = self.calc_desired_actions(self.environment.history)
        x_train = tf.constant(x_train)
        y_train = tf.constant(y_train)
        log('training')
        self.model = self.build_model()
        
        try:
            with contextlib.redirect_stdout(open('trainout.txt', 'w')):
                self.model.fit(x_train, y_train, epochs=2, verbose=0)
        except Exception as e:
            log(f'ERROR: {e}')
            log(f'\tx_train: {x_train} with len: {len(x_train)} and shape: {x_train.shape}')
            log(f'\ty_train: {y_train} with len: {len(y_train)} and shape: {y_train.shape}')
            log(f'\tmodel: {self.model}')
            log(f'\tself.environment.history: {self.environment.history}')

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
        if len(given_window) < self.window_size:
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
        if self.model is None:
            log('ERROR: Model should not be None in get_predictions()')
            return None
        one_input = [self.flatten(window)]
        one_input = tf.constant(one_input)
        predictions = self.model(one_input)
        return predictions

    def calc_desired_actions(self, window):
        '''
        Calculates the desired action for each window in the history

        The model wants expected outputs to be a set of integer categories.
        So we arbitrarily assign: a=0, A=1, b=2, B=3

        the output of this function should line up with the output of 
        calc_input_tensors for the same window

        window - in a letter-based format (e.g., aabaBbbabaabaBbbab...)
        cutoff - distance from curr pos to goal at which the agent prefers 
                NOT to take the historical action
        '''
        # Sanity check
        if len(window) < self.window_size:
            log('ERROR: window too short for training!')
            return

        # Iterate over a range starting at index window_size in the window
        hrange = list(range(len(window)))[self.window_size:]
        desired_actions = []
        # log("In calc_desired_actions()")
        for i in hrange:
            # Calculate how many steps from this position to the next goal
            num_steps = 0
            while(window[i + num_steps].islower()):
                num_steps += 1
                if i + num_steps >= len(window):
                    break

            # Calculate the index of the action at this position, [a,b,c,A,B,C]
            # 0 = a, 1 = b, 2 = c, 3 = A, 4 = B, 5 = C

            val = self.environment.overall_alphabet.index(window[i])

            if val is None:
                log('ERROR: invalid character in window')
                return

            # If the cutoff is exceeded then we want the other action instead
            # Examples:
            #   I.   if val is 'a' (=0), set val to 'b' (=2)
            #   II.  if val is 'A' (=1), set val to 'b' (=2)
            #   III. if val is 'b' (=2), set val to 'a' (=0)
            #   IV.  if val is 'B' (=3), set val to 'a' (=0)
            if num_steps >= 0.25*self.environment.avg_steps:
                orig = val
                while orig == val:
                    val = random.choice(range(len(self.environment.alphabet)))  

            # Add to result
            desired_actions.append(val)

        return desired_actions

    def flatten(self, window):
        # Mapping characters to indices
        char_to_index = {char: idx for idx, char in enumerate(self.environment.overall_alphabet)}
        
        # One-hot encode each character in the window
        encoded_window = []
        for char in window:
            vector = [0] * len(self.environment.overall_alphabet)
            index = char_to_index[char]
            vector[index] = 1
            if char.isupper():
                index = char_to_index[char.lower()]
                vector[index] = 1
            encoded_window.append(vector)
        return encoded_window