package environments.fsm;

import framework.Move;
import utils.Randomizer;

import java.util.HashMap;
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
    private int alphabetSize;
    private int numStates;
    private Move[] moves;
    private HashMap<Move, Integer>[] transitionTable;
    private int transitionsDone = 0;
    private Randomizer randomizer;

    /**
     * Create a {@link FSMTransitionTableBuilder}.
     * @param alphabetSize The number of moves to allow from each state.
     * @param numStates The number of states in the FSM.
     */
    public FSMTransitionTableBuilder(int alphabetSize, int numStates, Randomizer randomizer) {
        if (alphabetSize < 1)
            throw new IllegalArgumentException("alphabetSize cannot be less than 1");
        if (numStates < 1)
            throw new IllegalArgumentException("numStates cannot be less than 1");
        this.randomizer = randomizer;
        this.alphabetSize = alphabetSize;
        this.numStates = numStates;
    }

    /**
     * Get the transition table built by this {@link FSMTransitionTableBuilder}.
     * @return The transition table.
     */
    public FSMTransitionTable getTransitionTable() {
        this.buildTransitionTable();
        return new FSMTransitionTable(this.transitionTable);
    }

    private void buildTransitionTable() {
        this.moves = new Move[alphabetSize];
        for(int i = 0; i < moves.length; ++i) {
            char next = (char)('a' + i);
            moves[i] = new Move(next  + "");
        }
        this.transitionTable = new HashMap[numStates];
        // All goal state transitions should loop back to the goal state
        HashMap<Move, Integer> goalStateTransitions = new HashMap<>();
        for (Move move : this.moves) {
            goalStateTransitions.put(move, numStates - 1);
        }
        transitionTable[numStates - 1] = goalStateTransitions;

        int maxTransitionsToGoal = (int)(transitionTable.length * moves.length * 0.04);
        if (maxTransitionsToGoal == 0)
            maxTransitionsToGoal = 1;
        this.pickTransitions(numStates - 1, this.randomizer.getRandomNumber(maxTransitionsToGoal) + 1);
    }

    private void pickTransitions(int initGoal, int numOfTransitions) {
        int initState = -1;
        for(int i = 0; i < numOfTransitions; i++) {
            //check to see if table is full
            if(this.transitionsDone == ((this.transitionTable.length-1)*this.moves.length))
                return;
            initState = this.randomizer.getRandomNumber(this.transitionTable.length);
            int moveIndex = this.randomizer.getRandomNumber(this.moves.length);

            if (this.transitionTable[initState] != null && this.transitionTable[initState].containsKey(this.moves[moveIndex])) {
                i--;
                continue;
            }
            HashMap<Move, Integer> rowTransitions = this.transitionTable[initState];
            if (rowTransitions == null)
                this.transitionTable[initState] = rowTransitions = new HashMap<>();
            rowTransitions.put(this.moves[moveIndex], initGoal);
            this.transitionsDone++;
        }
        pickTransitions(initState, 1);
    }
}
