import tensorflow as tf
import contextlib
from TFSocket import log

class TFSocketModel():

    def __init__(self, window_size, entire_history):
        '''
        
        '''
        self.window_size = window_size
        self.history = entire_history
        
        # Defining the model function as a variable
        self.model = tf.keras.models.Sequential([
            tf.keras.layers.Dense(16, activation='relu'),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(2)
        ])

    def train_model(self, entire_history):
        '''
        Train and optimize a new ANN model
        
        Refer to the beginner tensorflow tutorial:
        https://www.tensorflow.org/tutorials/quickstart/beginner
        '''
        # Defining the inputs and expected outputs from a given history
        avg_steps = self.calc_avg_steps(entire_history)
        x_train = self.calc_input_tensors(entire_history)
        y_train = self.calc_desired_actions(entire_history, avg_steps)
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

        return avg_steps

    def calc_avg_steps(history):
        '''
        Calculate average steps to goal for a given history
        '''
        # Count the number of goals (skipping the first)
        num_historical_goals = 0
        for i in range(len(history)):
            if (i == 0):
                continue
            if (history[i].isupper()):
                num_historical_goals+=1

        avg_steps = len(history) / num_historical_goals
        log(f'Avg steps: {avg_steps}')
        return avg_steps

    def calc_input_tensors(self, history):
        '''
        Convert a blind FSM action history into an array of input tensors
        '''
        # Sanity check
        if (len(history) < self.window_size):
            log('ERROR:  history too short for training!')
            return

        # Iterate over a range that skips the first window_size indexes so we have a full window for each input
        # generate an input tensor for each one
        history_range = list(range(len(history)))[self.window_size:]
        tensor = []
        for i in history_range:
            window = history[i-self.window_size:i]
            tensor.append(self.flatten(window))

        return tensor

    def get_action(self, history):
        '''
        Uses the model to generate a next step for the environment
        '''

        one_input = []
        one_input.append(self.flatten(history))
        one_input = tf.constant(one_input)
        predictions = self.model(one_input)

        last_step = 'a'
        # Predictions now returns as a tensor of shape (1,2)
        log(f'Chance of a: {predictions[0][0]}\nChance of b: {predictions[0][1]}')
        if (predictions[0][0] < predictions[0][1]):
            last_step = 'b'
        log('Model is not None, sending prediction: ' + last_step)
        
        return last_step

    ##
    # calc_desired_actions
    #
    # the output of this function should line up with the output of 
    # calc_input_tensors for the same history
    #
    # history - in a letter-based format (e.g., aabaBbbabaabaBbbab...)
    # cutoff - distance from curr pos to goal at which the agent prefers 
    #          NOT to take the historical action
    def calc_desired_actions(self, history, cutoff):
        '''
        Calculates the desired action for each window in a history
        '''
        #sanity check
        if (len(history) < self.window_size):
            log('ERROR:  history too short for training!')
            return

        #iterate over a range starting at index window_size in the history
        hrange = list(range(len(history)))[self.window_size:]
        ret = []
        for i in hrange:
            #calculate how many steps from this position to the next goal
            num_steps = 0
            while(history[i + num_steps].islower()):
                num_steps += 1

            #calculate the index of the action of this position
            val = 0
            if ( (history[i].lower() == 'b') or (history[i].lower() == 'B') ):
                val = 1

            #if the cutoff is exceeded then we want the other action instead
            if (num_steps >= cutoff):
                val = 1 - val

            #add to result
            ret.append(val)

        return ret

    def flatten(window):
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