package environments.fsm;

import framework.Move;

import java.util.HashMap;
import java.util.Random;

/**
 * An FSMTransitionTableBuilder is used to build transition tables for a {@link FSMDescription}.
 * @author Zachary Paul Faltersack
 * @version 0.95
 *
 * based off of code from:
 * @author Kirkland Spector
 * @author Chandler Underwood
 *
 * based off of code from:
 * @author Hailee Kenney
 * @author Preben Ingvaldsen
 */
public class FSMTransitionTableBuilder {
    //region Class Variables
    private int alphabetSize;
    private int numStates;
    private Move[] moves;
    private Random random;
    //endregion

    //region Constructors
    /**
     * Create a {@link FSMTransitionTableBuilder}.
     * @param alphabetSize The number of moves to allow from each state.
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
        this.moves = new Move[alphabetSize];
        for(int i = 0; i < moves.length; ++i) {
            char next = (char)('a' + i);
            moves[i] = new Move(next  + "");
        }
    }
    //endregion

    //region Public Methods
    /**
     * Get the transition table built by this {@link FSMTransitionTableBuilder}.
     * @return The transition table.
     */
    public FSMTransitionTable getTransitionTable() {
        HashMap<Move, Integer>[] transitions = this.buildTransitionTable();
        return new FSMTransitionTable(transitions);
    }
    //endregion

    //region Private Methods
    private HashMap<Move, Integer>[] buildTransitionTable() {
        HashMap<Move, Integer>[] transitions = new HashMap[this.numStates];
        // All goal state transitions should loop back to the goal state
        HashMap<Move, Integer> goalStateTransitions = new HashMap<>();
        for (Move move : this.moves) {
            goalStateTransitions.put(move, this.numStates - 1);
        }
        transitions[this.numStates - 1] = goalStateTransitions;

        int maxTransitionsToGoal = (int)(transitions.length * this.moves.length * 0.04);
        if (maxTransitionsToGoal == 0)
            maxTransitionsToGoal = 1;
        this.pickTransitions(transitions,this.numStates - 1, this.random.nextInt(maxTransitionsToGoal) + 1, 0);
        return transitions;
    }

    private void pickTransitions(HashMap<Move, Integer>[] transitions, int initGoal, int numOfTransitions, int transitionsDone) {
        int initState = -1;
        for(int i = 0; i < numOfTransitions; i++) {
            //check to see if table is full
            if(transitionsDone == ((transitions.length-1)*this.moves.length))
                return;
            initState = this.random.nextInt(transitions.length);
            int moveIndex = this.random.nextInt(this.moves.length);

            if (transitions[initState] != null && transitions[initState].containsKey(this.moves[moveIndex])) {
                i--;
                continue;
            }
            HashMap<Move, Integer> rowTransitions = transitions[initState];
            if (rowTransitions == null)
                transitions[initState] = rowTransitions = new HashMap<>();
            rowTransitions.put(this.moves[moveIndex], initGoal);
            transitionsDone++;
        }
        this.pickTransitions(transitions, initState, 1, transitionsDone);
    }
    //endregion
}