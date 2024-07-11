import os

'''
This is a utility script to find the optimal path from all states to a goal state

The idea behind this script was the that, at least one state is 1 step away from the goal state.
So, every state that has a transition to that state is 2 steps away from the goal state.
Then every state after those states is 3 steps away from the goal state, and so on.

NOTE: This script has not been comprehensively tested and may not work for all cases, so make sure to double
check the output if they seem off. It has worked for all the cases I have tested it on so far which where FSMs
with 20 states and an alphabet size of 5.
'''
# input.txt should be in the following format:
#   s0 -> s3 [label=a]
#   s0 -> s1 [label=b]
#   s1 -> s0 [label=a]
#   s1 -> s2 [label=b]
#   s2 -> s1 [label=a]
#   s2 -> s2 [label=b]
#   ...
if not os.path.exists("input.txt"):
    print("File not found")
    exit()

# Expected format for goal state: s3
# NOTE: The goal state should NOT be in the input.txt file
goal_state = input("Enter the goal state: ")

# The variable that holds the transitions for each state
#    With the input.txt file above, the transitions variable will look like this:
#    {
#        's0': ['s3', 's1'],
#        's1': ['s0', 's2'],
#        's2': ['s1', 's2'],
#        ...
#    }
transitions = dict()

# The variable that holds the steps to the goal state for each state
#    With the input.txt file above, the steps_to_goal variable will look like this:
#    {
#        's0': 1,
#        's1': 2,
#        's2': 3,
#        ...
#    }
steps_to_goal = dict()

# Initialize the transitions variable
with open("input.txt", "r") as file:
    # A line in input.txt will look like this: s0 -> s3 [label=a]
    for line in file:
        # After this line of code, a line will look like this: ['s0', '->', 's3', '[label=a]']
        line = line.strip().split()
        # If the state is not in the transitions variable, add it
        if line[0] not in transitions:
            transitions[line[0]] = []
        # Add the state to the list of transitions for the current state
        transitions[line[0]].append(line[2])

# A variable created so we can iterate over all of the states
states = list(transitions.keys())

# A variable to keep track of the states that have been processed 
# where processed means that we have a number of steps to the goal state that may or may not be the lowest
finished_states = []

# Iterate over all the states
for state in states:
    # Do the first step and find all the states that are 1 step away from the goal state
    if goal_state in transitions[state]:
        steps_to_goal[state] = 1
        finished_states.append(state)
        
unreachable_states = [] # NOTE: Not ever actually used but it stopped hanging when I put it in so I haven't touched it

# Keep iterating until all states have been processed
while not all(s in finished_states for s in states):
    # Iterate over all states
    for curr_state in states:
        # Don't process states that have already been processed
        if curr_state in finished_states:
            continue
        
        # Loop through all the transitions (that have already been processed) and find the lowest number of steps to goal
        lowest_steps = 1000000
        for transition in transitions[curr_state]:
            # If no transitions for a state were processed yet, then we break and continue
            # This is where I suspect the infinite loop was happening but it hasn't happened since I added the unreachable_states variable
            # so it is unconfirmed
            if all(t not in finished_states for t in transitions[curr_state]):
                print(f"State {curr_state} is not reachable from any finished state")
                unreachable_states.append(curr_state)
                break
            
            # When we have a transition that has been processed, we find the number of steps to the goal state
            # and compare it to the lowest number of steps we have found so far
            if transition in finished_states:
                curr_step = steps_to_goal[transition]
                if curr_step < lowest_steps:
                    lowest_steps = curr_step
        # Hitting this means that no transitions for the current state were processed, so continue
        if lowest_steps == 1000000:
            continue
        
        # Else, we add 1 to the lowest number of steps and add it to the steps_to_goal variable
        # This is because we need to take a step from the current state to the transition state
        # and then the number of steps from the transition state to the goal state
        steps_to_goal[curr_state] = lowest_steps + 1
        
        # This state has been processed so we add it to the finished_states variable
        finished_states.append(curr_state)
        
        # Doesn't seem to be necessary but I haven't touched it since I added it
        if curr_state in unreachable_states:
            unreachable_states.remove(curr_state)
            
# This loop is included because of a logic flaw in the code above
#   Say we have the following transitions, where s3 is the goal state:
#       s0 -> s3 [label=a]
#       s0 -> s1 [label=b]
#       s1 -> s0 [label=a]
#       s1 -> s2 [label=b]
#       s2 -> s1 [label=a]
#       s2 -> s4 [label=b]
#       s4 -> s3 [label=a]
#       s4 -> s3 [label=b]
#   The code above would give us values that look like the steps_to_goal examples above, however
#   s4 has a transition that goes to s3, which would make s2 only 2 steps away from the goal state instead of 3.
#   This wouldn't be caught by the code above since by the time we're processing s2, s1 is processed and s4 is not,
#   so s2 uses the value from s1 and we never go back to s2 to check if there's a shorter path after it's other transitions are processed.
#   This loop fixes that by going through all the states and checking if there's a shorter path after all the transitions are processed.
#   NOTE: May need to run this loop multiple times to get the correct values in the future
for state in states:
    # Get the current number of steps to the goal state and loop through all the transitions
    curr_lowest = steps_to_goal[state]
    for transition in transitions[state]:
        # If the transition is the goal state, we don't need to check it
        if transition == goal_state:
            continue
        # If using that transition gives us a lower number of steps to the goal state, we update the number of steps
        if steps_to_goal[transition] + 1 < curr_lowest:
            curr_lowest = steps_to_goal[transition] + 1
    steps_to_goal[state] = curr_lowest

# All the states have been processed so we can print out the results
output = []
# Format the output for each state
for key,value in steps_to_goal.items():
    output.append(f"{key:<3} to {goal_state} in {value:<2} steps")

# This first printout is for debugging purposes. The lines are sorted by the number of steps to the goal state
# so we can manually check it against the transitions of the FSM if we're unsure. 
# We would have this printout next to input.txt and we can see if the number of steps makes sense. For all the states
# that are 1 step away from the goal state, we can check if they have a transition to the goal state. For all the states that are
# 2 steps away from the goal state, we can check if they have a transition to a state that is 1 step away from the goal state AND no transition
# to the goal state. For states that are 3 steps away from the goal state, we can check if they have a transition to a state that is 2 steps away from
# the goal state and no transition to a state that is 1 step away from the goal state and no transition to a goal state, and so on.
output.sort(key=lambda x: int(x.split()[4]))
for line in output:
    print(line)

print("\nCopy the following lines into excel\n")

# This second printout is for the final output. We sort the lines by the state number so we can copy and paste into excel
# NOTE: We only print the number of steps to the goal state, not something like 's0:1', just '1'
output.sort(key=lambda x: int(x.split()[0][1:]))
for line in output:
    line = line.split()
    print(line[4])
            