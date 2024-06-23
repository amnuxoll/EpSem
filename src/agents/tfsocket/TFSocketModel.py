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
            tf.keras.layers.Dense(4)
        ])
        
    def get_letter(self,prediction):
        max = 0
        for i in range(len(prediction[0])):
            if prediction[0][i] > prediction[0][max]:
                max = i
        match max:
            case 0:
                return 'a'
            case 1:
                return 'A'
            case 2:
                return 'b'
            case 3:
                return 'B'
        log("Error ocurred")
        return
        
    def simulate_model(self,prediction):
        '''
        Simulate the model's prediction of the next step
        '''
        def get_sim_input():
            self.environment.entire_history = self.environment.entire_history + self.get_letter(prediction)
            input = self.calc_input_tensors()
            input = tf.constant(input)
            log(f"self.model(input[:1]): {self.model(input[:1])}")
            return self.model(input[:1])
        
        temp = self.environment.entire_history
        prediction = get_sim_input()
        log(f"Prediction: {prediction}")
        sim_path = self.get_letter(prediction)
        while not ((self.get_letter(prediction) == 'A') or (self.get_letter(prediction) == 'B')):
            if len(sim_path) < 100:
                log(f"Simulation: {sim_path}")
            else:
                log("Simulation: Path too long, stopping.")
                break
            prediction = get_sim_input()
            sim_path += self.get_letter(prediction)
            
            
        self.environment.entire_history = temp
        log(f"Simulation: {sim_path}")
        
    def train_model(self):
        '''
        Train and optimize a new ANN model
        
        Refer to the beginner tensorflow tutorial:
        https://www.tensorflow.org/tutorials/quickstart/beginner
        '''
        # Defining the inputs and expected outputs from a given history
        self.environment.update_avg_steps()
        x_train = self.calc_input_tensors()
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
            
        self.simulate_model(self.model(x_train[:1]))

    def calc_input_tensors(self):
        '''
        Convert a blind FSM action window into an array of input tensors
        
        Example:
            if window_size = 3, and entire_history = 'abbBababAa'
            first it creates each window:
                abb, bbB, bBa, etc.
            then it converts each window to a tensor
                abb -> [1, 0, 0, 0, 1, 1, 0, 0, 0]
                bAb -> [0, 1, 0, 1, 1, 0, 0, 1, 0]
            then it creates a list of all the input tensors
        '''
        # Sanity check
        if (len(self.environment.entire_history) < self.window_size):
            log('ERROR: window too short for training!')
            return

        # Generate a list of the index of the last character in each history window
        # Example:
        #   If window_size = 3, and len(entire_history) = 10,
        #   then it will output [3, 4, 5, 6, 7, 8, 9]
        history_range = list(range(len(self.environment.entire_history)))[self.window_size:]
        tensor = []
        for i in history_range:
            window = self.environment.entire_history[i-self.window_size:i]
            tensor.append(self.flatten(window))

        return tensor

    def get_action(self, window):
        '''
        Uses the model to generate a next step for the environment
        '''
        one_input = []
        one_input.append(self.flatten(window))
        one_input = tf.constant(one_input)
        predictions = self.model(one_input)
        predictions = tf.nn.softmax(predictions).numpy()
        log('Predictions: ' + str(predictions))
        self.environment.last_step = 'a'
        # Predictions now returns as a tensor of shape (1,2)
        if (predictions[0][0] + predictions[0][1] < predictions[0][2] + predictions[0][3]):
            self.environment.last_step = 'b'
        log('Model is not None, sending prediction: ' + self.environment.last_step)

    def calc_desired_actions(self, window):
        '''
        Calculates the desired action for each window in the entire_history
        
        the output of this function should line up with the output of 
        calc_input_tensors for the same window

        window - in a letter-based format (e.g., aabaBbbabaabaBbbab...)
        cutoff - distance from curr pos to goal at which the agent prefers 
                 NOT to take the historical action
        '''
        #sanity check
        if (len(window) < self.window_size):
            log('ERROR:  window too short for training!')
            return

        #iterate over a range starting at index window_size in the window
        hrange = list(range(len(window)))[self.window_size:]
        desired_actions = []
        for i in hrange:
            #calculate how many steps from this position to the next goal
            num_steps = 0
            while(window[i + num_steps].islower()):
                num_steps += 1

            #calculate the index of the action of this position
            # 0 = a, 0.33 = A, 0.66 = b, 1 = B
            val = -1
            match window[i]:
                case 'a':
                    val = 0
                case 'A':
                    val = 0.33
                case 'b':
                    val = 0.66
                case 'B':
                    val = 1
                    
            if val == -1:
                log('ERROR:  invalid character in window')
                return
            

            #if the cutoff is exceeded then we want the other action instead
            # If val is an a or A, then send the opposite, lowercase action and vic versa
            if (num_steps >= self.window_size): 
                if val < 0.5:
                    val = 0.66
                else:
                    val = 0

            #add to result
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
            Example input window:   '01a11B00b00a'
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