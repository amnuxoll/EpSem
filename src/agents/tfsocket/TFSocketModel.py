import tensorflow as tf
import contextlib
import random
from TFSocketUtils import log
import traceback

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
        self.window_size = window_size * (environment.num_sensors + 1)  # Accommodated to handle the amount of actions + sensors
        self.sim_path = None # Predicted shortest path to goal (e.g., abbabA)
        self.model = None
        

        

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
        if self.model is None:
            log('ERROR: Model should not be None in simulate_model()')
        window = self.environment.entire_history[-self.window_size:]
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

        max_len = 2*self.environment.avg_steps
        index = 0
        # Simulate future steps
        self.sim_path = first_sim_action
        while not self.sim_path[-1:].isupper():
            if len(self.sim_path) >= max_len:
                # log('sim_path reached max length, truncate to most probable goal prediction.')
                break # I suspect the while loop exits here virtually always

            # log(f'Model (window_size={window}); sim_path: {self.sim_path}')

            # Simulate the next step from entire_path and sim_path so far
            sim_hist = self.environment.entire_history + self.sim_path
            window = sim_hist[-self.window_size:]
            log(f'In the loop in simulate_model: window = {window}')
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

    def train_model(self):
        '''
        Train and optimize a new ANN model

        Refer to the beginner tensorflow tutorial:
        https://www.tensorflow.org/tutorials/quickstart/beginner
        '''
        
        # Defining the inputs and expected outputs from a given history
        self.environment.update_avg_steps()
        self.remove_duplicate_goal_paths()  # NOTE: We can maybe move this to remove duplicates after each goal instead
        x_train = self.calc_input_tensors(self.environment.entire_history)
        y_train = self.calc_desired_actions(self.filter_for_actions())
        x_train = tf.constant(x_train)
        y_train = tf.constant(y_train)

        # Defining the model's loss function
        loss_fn = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)
        
        # Defining the model function as a variable
        # Note: We are guessing the size of the first dense layer should
        # be about half the size of the input layer. We may want to do future
        # experiments to see what actually works best
        self.model = tf.keras.models.Sequential([
            tf.keras.layers.Dense(int(len(x_train)/2), activation='relu'),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(2*len(self.environment.alphabet), activation='softmax')   # Dense the layer with 2*len(alphabet) nodes for each action and goal actions
        ])
        
        # Optimize and train the model
        self.model.compile(optimizer='adam', loss=loss_fn, metrics=['accuracy'])
        try:
            with contextlib.redirect_stdout(open('trainout.txt', 'w')):
                self.model.fit(x_train, y_train, epochs=2, verbose=0)
        except Exception as e:
            log(f'ERROR: {e}')
            log(f'\tx_train with len: {len(x_train)} and shape: {x_train.shape}')
            log(f'\ty_train with len: {len(y_train)} and shape: {y_train.shape}')
            log(f'\tmodel: {self.model}')
            log(f'\tloss_fn: {loss_fn}')
            log(f'\tself.environment.entire_history: {self.environment.entire_history}')
            toPrint = []
            for elem in self.environment.entire_history:
                if elem in self.environment.overall_alphabet:
                    toPrint.append(elem)
            toPrint = ''.join(toPrint)
            log(f'\tself.environment.entire_history (actions):   {toPrint}, len: {len(toPrint)}')
            exit()
            
    def filter_for_actions(self):
        ret = []
        for char in self.environment.entire_history:
            if char in self.environment.overall_alphabet:
                ret.append(char)
        return ''.join(ret)
    
    def remove_duplicate_goal_paths(self):
        '''
        Remove duplicate actions from the history
        '''
        # Path that got us to a goal (e.g ['ababaB','bbbaB'])
        goal_paths = []
        last_goal = 0
        
        # Loop through the entire history and find the paths to a goal
        for i in range(len(self.environment.entire_history)):
            if self.environment.entire_history[i].isupper():
                goal_paths.append(self.environment.entire_history[last_goal:i+1])
                last_goal = i+1
        
        # Remove duplicate goal paths
        # Look up times are faster in sets than lists so use a set to check if an item is in the list
        # NOTE: May want to adjust this since we are now using sensors in the history
        seen = set()
        ret = []
        for path in goal_paths:
            if path not in seen:
                seen.add(path)
                ret.append(path)
        self.environment.entire_history = ''.join(ret)
        

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
        given_window_range = list(range(len(given_window)))[self.window_size::self.environment.num_sensors+1]
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
        log(f'in get_predictions: window = {window}')
        one_input = [self.flatten(window)]
        one_input = tf.constant(one_input)
        predictions = self.model(one_input)
        return predictions

    def calc_desired_actions(self, window):
        '''
        Calculates the desired action for each window in the entire_history

        The model wants expected outputs to be a set of integer categories.
        So we arbitrarily assign: a=0, A=1, b=2, B=3

        the output of this function should line up with the output of 
        calc_input_tensors for the same window

        window - in a letter-based format (e.g., aabaBbbabaabaBbbab...)
        '''
        # Sanity check
        if len(window) < self.window_size:
            log('ERROR: window too short for training!')
            return

        # Iterate over a range starting at index window_size in the window
        win_size = self.window_size // (self.environment.num_sensors + 1)
        hrange = list(range(len(window)))[win_size:]
        desired_actions = []
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
            if num_steps >= self.window_size: 
                orig = val
                while orig == val:
                    val = random.choice(range(len(self.environment.alphabet)))
                    
            # Add to result
            desired_actions.append(val)

        return desired_actions

    def flatten(self, window):
        '''
        Convert a window into an input tensor
        Helper function for @calc_input_tensors()
        
        A window has this format:      'aabaBbbab'
        the return object's length is (len(self.environment.alphabet) + 1)*window_size
        being whether or not an 'a'/'A' is in that position
        the second set is for b/B. The third set is goal sensor.

        TODO:  In the future we may want to handle sensors. 
            Example input window:      '01a11B00b00a'
        '''
        
        # Set the indexes of the alphabet with a dictionary
        log(f'Initial window: {window}')
        indexes = dict()
        for i in range(len(self.environment.alphabet)):
            indexes[i] = self.environment.alphabet[i]
        else:
            indexes['GOAL'] = i+1
            
        # win_size represents the number of actions we want in the window
        win_size = self.window_size // (self.environment.num_sensors + 1)
        
        return_tensor = []
        for action in self.environment.alphabet:
            return_tensor += [0.0 for i in range(win_size)]  # For each action, add a list of zeros
        return_tensor += [0.0 for i in range(win_size)]      # Add a list of zeros for the goals
        
        # Create a separate list for the actions and sensors in the window
        action_only_window = [action for action in window if action in self.environment.overall_alphabet]
        sensor_only_window = []
        for i in range(0,len(window),4):
            sensor_only_window.append(window[i:i+self.environment.num_sensors])
        
        # tensor_indexes is a variable to keep track of the indexes of the actions in the tensor
        # while the sensors are added in the tensor
        tensor_indexes = []
        for i in range(win_size):
            let = action_only_window[i]
            for key, val in indexes.items():
                if let.lower() == val:
                    return_tensor[i + key*win_size] = 1.0
                    if let.islower():
                        tensor_indexes.append(i + key*win_size)
                if let.isupper():
                    key = indexes['GOAL']
                    return_tensor[i + key*win_size] = 1.0  
                    tensor_indexes.append(i + key*win_size)          
                    break
        
        # sensor_index is a variable to keep track of which sensor/action pair we are on
        sensor_index = 0
        # Loop through and add each sensor data point in front of the appropriate action
        for t_index in tensor_indexes:
            # Loop through the sensors in reverse order due to how insert() works
            for i in range(self.environment.num_sensors)[::-1]:
                try:
                    return_tensor.insert(t_index, float(sensor_only_window[sensor_index][i]))
                except Exception as e:
                    log(f'Exception: {e}')
                    log('-----')
                    try:
                        f = open('pyout.txt', 'a')
                        traceback.print_tb(e.__traceback__, None, f)
                        f.close()
                    except Exception as errerr:
                        log(f'Exception exception!: {errerr}')
                    log(f'sensor_index: {sensor_index}')
                    log(f'sensor_only_window: {sensor_only_window}')
                    log(f'return_tensor: {return_tensor}')
                    log(f'tensor_indexes: {tensor_indexes}')
                    log(f'window: {window}')
                    log(f'self.environment.entire_history: {self.environment.entire_history}')
                    log('--- end of report ---')
                    exit()
            sensor_index += 1
            
            # Loop through the remaining indexes and check if the inserts affected their position in the tensor
            # If so, adjust the index accordingly
            for i in range(len(tensor_indexes))[sensor_index:]:
                if tensor_indexes[i] > t_index:
                    tensor_indexes[i] += self.environment.num_sensors

        return return_tensor