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

# Ensure input file exists
if not os.path.exists('input.txt'):
    print('File not found')
    exit()

# Create transitions dictionary
transitions = dict()
goal_state = -1
with open('input.txt', 'r') as file:
    for line in file:
        # Parse line into transition from state 'lhs' to state 'rhs'
        line = line.strip().split() # ['s0', '->', 's1', '[label=a];']
        lhs = line[0][1:] # 's0'
        rhs = line[2][1:] # 's1'
        if lhs not in transitions.keys():
            transitions[lhs] = []
        transitions[lhs].append(rhs)

        # Define goal state as largest numbered state in transitions list
        if int(goal_state) < int(max(lhs, rhs)):
            goal_state = max(lhs, rhs)

print(f'transitions: {transitions}')
print(f'goal state: {goal_state}')

# Calculate the length of each state's optimal path to the goal state
steps_to_goal = [None] * int(goal_state) + [0]
for depth in range(int(goal_state) +1):
    for lhs in transitions:
        rhs_steps_to_goal = []
        for rhs in transitions[lhs]:
            # If transition is 1 step away from the goal
            if rhs == goal_state:
                steps_to_goal[int(lhs)] = 1
            if steps_to_goal[int(rhs)] is not None:
                rhs_steps_to_goal.append(steps_to_goal[int(rhs)])

        # If lhs has a transition to a state with a known path length to
        # the goal, the lhs length of path to goal is +1 more
        if len(rhs_steps_to_goal) != 0:
            steps_to_goal[int(lhs)] = min(rhs_steps_to_goal) + 1

# Average out the length to the goal state of all states
avg_steps = sum(steps_to_goal)/len(steps_to_goal)

print(f'steps to goal: {steps_to_goal}')
print(f'average steps to goal: {avg_steps}')