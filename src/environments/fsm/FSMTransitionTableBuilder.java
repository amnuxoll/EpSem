package environments.fsm;

import framework.Action;

import java.util.HashMap;
import java.util.Random;

/**
 * An FSMTransitionTableBuilder is used to build transition tables for a {@link FSMEnvironment}.
 * @author Zachary Paul Faltersack
 * @version 0.95
 *
 * based off of code from:
 * @author Kirkland Spector
 * @author Chandler Underwood
 *
 * based off of code from:
 * @author Hailee Kenney
 * @author Lilah Ingvaldsen
 */
public class FSMTransitionTableBuilder {
    //region Class Variables
    private int alphabetSize;
    private int numStates;
    private Action[] actions;
    private static Random random = new Random(13);
    //endregion

    //region Constructors
    /**
     * Create a {@link FSMTransitionTableBuilder}.
     * @param alphabetSize The number of actions to allow from each state.
     * @param numStates The number of states in the FSM.
     */
    public FSMTransitionTableBuilder(int alphabetSize, int numStates, Random random) {
        if (alphabetSize < 1)
            throw new IllegalArgumentException("alphabetSize cannot be less than 1");
        if (numStates < 1)
            throw new IllegalArgumentException("numStates cannot be less than 1");
        this.random = random;
        this.alphabetSize = alphabetSize;
        this.numStates = numStates;
        this.actions = new Action[alphabetSize];
        for(int i = 0; i < actions.length; ++i) {
            char next = (char)('a' + i);
            actions[i] = new Action(next  + "");
        }
    }
    //endregion

    //region Public Methods
    /**
     * Get the transition table built by this {@link FSMTransitionTableBuilder}.
     * @return The transition table.
     */
    public FSMTransitionTable getTransitionTable() {
        HashMap<Action, Integer>[] transitions = this.buildTransitionTable();
        return new FSMTransitionTable(transitions);
    }

    public int getNumStates() {
        return numStates;
    }

    public String getDetails() {
        return "Alpha_" + this.alphabetSize + "_States_" + this.numStates;
    }
    //endregion

    //region Private Methods
    @SuppressWarnings("unchecked")
    private HashMap<Action, Integer>[] buildTransitionTable() {
        HashMap<Action, Integer>[] transitions = new HashMap[this.numStates];
        // All goal state transitions should loop back to the goal state
        HashMap<Action, Integer> goalStateTransitions = new HashMap<>();
        for (Action action : this.actions) {
            goalStateTransitions.put(action, this.numStates - 1);
        }
        transitions[this.numStates - 1] = goalStateTransitions;

        int maxTransitionsToGoal = (int)(transitions.length * this.actions.length * 0.04);
        if (maxTransitionsToGoal == 0)
            maxTransitionsToGoal = 1;

        int trans = this.random.nextInt(maxTransitionsToGoal);
        this.pickTransitions(transitions,this.numStates - 1, trans + 1, 0);
        return transitions;
    }

    /**
     * randomly creates random transitions for each state in the FSM.
     * Note: This method does not guarantee a path from each state to the goal.
     * Caveaat:  This method is RECURSIVE!
     *
     * @param transitions  the current (incomplete) transition table
     * @param destState the destination state for transitions generated on this call
     * @param numOfTransitions how many transitions to create on this call
     * @param transitionsDone how many total transitions have been done so far (used to halt recursion)
     */
    private void pickTransitions(HashMap<Action, Integer>[] transitions, int destState, int numOfTransitions, int transitionsDone) {
        int initState = -1;
        for(int i = 0; i < numOfTransitions; i++) {
            //check to see if table is full
            if(transitionsDone == ((transitions.length-1)*this.actions.length))
                return;

            //pick a random state and action
            initState = this.random.nextInt(transitions.length);
            int moveIndex = this.random.nextInt(this.actions.length);

            //If we have already have a transition from initState using the random action, try again
            HashMap<Action, Integer> rowTransitions = transitions[initState];
            if (transitions[initState] != null && rowTransitions.containsKey(this.actions[moveIndex])) {
                i--;
                continue;
            }

            //add the random transition
            rowTransitions = transitions[initState];
            if (rowTransitions == null)
                transitions[initState] = rowTransitions = new HashMap<>();
            rowTransitions.put(this.actions[moveIndex], destState);
            transitionsDone++;
        }
        this.pickTransitions(transitions, initState, 1, transitionsDone);
    }

}
