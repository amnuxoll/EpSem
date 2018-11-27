package environments.fsm;

import framework.IEnvironmentDescription;
import framework.IEnvironmentDescriptionProvider;
import framework.Move;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;

/**
 * An FSMDescriptionProvider generates new {@link FSMDescription}s for consecutive test runs.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMDescriptionProvider implements IEnvironmentDescriptionProvider {
    private int alphabetSize;
    private int numStates;
    private EnumSet<FSMDescription.Sensor> sensorsToInclude;

    /**
     * Create an instance of a {@link FSMDescriptionProvider}.
     * @param alphabetSize The number of moves to allow in the FSM.
     * @param numStates The number of states to allow in the FSM.
     * @param sensorsToInclude The sensor data to include when navigating the FSM.
     */
    public FSMDescriptionProvider(int alphabetSize, int numStates, EnumSet<FSMDescription.Sensor> sensorsToInclude) {
        if (alphabetSize < 1)
            throw new IllegalArgumentException("alphabetSize cannot be less than 1");
        if (numStates < 1)
            throw new IllegalArgumentException("numStates cannot be less than 1");
        if (sensorsToInclude == null)
            throw new IllegalArgumentException("sensorsToInclude cannot be null");
       this.alphabetSize = alphabetSize;
       this.numStates = numStates;
       this.sensorsToInclude = sensorsToInclude;
    }

    /**
     * Get a new instance of a {@link FSMDescription}.
     * @return The new {@link FSMDescription}.
     */
    @Override
    public IEnvironmentDescription getEnvironmentDescription() {
        FSMTransitionTableBuilder builder = new FSMTransitionTableBuilder(this.alphabetSize, this.numStates);
        HashMap<Move, Integer>[] transitionTable = builder.getTransitionTable();

        Move a = new Move("a");
        Move b = new Move("b");

        transitionTable= debugTT(a,b);

        FSMDescription toReturn = new FSMDescription(transitionTable, this.sensorsToInclude);
        //toReturn.setShortestSequences(builder.getShortestSequences());
        toReturn.setShortestSequences(builder.getShortestSequences());
        System.out.println("Universal sequence: "+toReturn.getUniversalSequence());
        return toReturn;
    }

    public HashMap<Move, Integer>[] debugTT(Move a, Move b){

        HashMap<Move, Integer>[] transitionTable = new HashMap[6];
        for(int i=0;i<transitionTable.length;i++){
            transitionTable[i]= new HashMap<>();
        }

        transitionTable[0].put(a, 4);
        transitionTable[0].put(b, 2);
        transitionTable[1].put(a, 4);
        transitionTable[1].put(b, 0);
        transitionTable[2].put(a, 2);
        transitionTable[2].put(b, 3);
        transitionTable[3].put(a, 5);
        transitionTable[3].put(b, 0);
        transitionTable[4].put(a, 1);
        transitionTable[4].put(b, 3);
        transitionTable[5].put(a, 5);
        transitionTable[5].put(b, 5);

        return transitionTable;
    }

    public HashMap<Integer, ArrayList<Move>> debugShortestSequences(Move a, Move b){
        HashMap<Integer, Move[]> ss= new HashMap<>();

        ss.put(0, new Move[]{a,b,a});
        ss.put(1, new Move[]{a,b,a});
        ss.put(2, new Move[]{b,a});
        ss.put(3, new Move[]{a});
        ss.put(4, new Move[]{b,a});
        ss.put(5, new Move[0]);

        HashMap<Integer, ArrayList<Move>> actual= new HashMap<>();

        for(Integer state : ss.keySet()){
            ArrayList<Move> list= new ArrayList<>(Arrays.asList(ss.get(state)));
            actual.put(state, list);
        }

        return actual;
    }
}
