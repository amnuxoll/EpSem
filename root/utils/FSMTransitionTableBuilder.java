package utils;

import environments.fsm.FSMDescription;
import framework.Move;

import java.util.ArrayList;
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

    private HashMap<Integer, ArrayList<Move>> shortestSequences;
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
        this.shortestSequences = new HashMap<>();
        for(int i = 0; i<numStates-1; i++){
            shortestSequences.put(i, null);
        }
        shortestSequences.put(numStates-1, new ArrayList<>());
        this.buildTransitionTable();
    }

    /**
     * Get the transition table built by this {@link FSMTransitionTableBuilder}.
     * @return The transition table.
     */
    public HashMap<Move, Integer>[] getTransitionTable() {
        return this.transitionTable;
    }

    /**
     * Get the hash map from each state to the shortest sequence to the goal from that state
     * @return the table of shortest sequences
     */
    public HashMap<Integer, ArrayList<Move>> getShortestSequences() {
        return shortestSequences;
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

            /**
             * keeps track of the shortest sequence to goal from each state
             */
            ArrayList<Move> currentShortestSequence = shortestSequences.get(initState);
            if(currentShortestSequence == null || currentShortestSequence.size() > shortestSequences.get(initGoal).size() + 1){
                ArrayList<Move> initGoalSequence = (ArrayList<Move>)shortestSequences.get(initGoal).clone();
                initGoalSequence.add(0, moves[moveIndex]);
                shortestSequences.put(initState, initGoalSequence);
            }
            this.transitionsDone++;
        }
        pickTransitions(initState, 1);
    }
}
