package environments.fsm;

import framework.*;
import utils.Sequence;

import java.util.*;

/**
 * An FSMDescription provides information to {@link framework.Environment} for running an experiment.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMDescription implements IEnvironmentDescription {
    private HashMap<Move, Integer>[] transitionTable;
    private HashMap<Integer, ArrayList<Move>> shortestSequences;
    private Move[] moves;
    private EnumSet<Sensor> sensorsToInclude;
    private Sequence universalSequence;

    /**
     * Create an instance of a {@link FSMDescription}.
     * @param transitionTable The transition table that indicates the structure of a FSM.
     */
    public FSMDescription(HashMap<Move, Integer>[] transitionTable) {
        this(transitionTable, EnumSet.noneOf(Sensor.class));
    }

    /**
     * Create an instance of a {@link FSMDescription} that includes possible sensor data.
     * @param transitionTable The transition table that indicates the structure of a FSM.
     * @param sensorsToInclude The sensors to include when navigating the FSM.
     */
    public FSMDescription(HashMap<Move, Integer>[] transitionTable, EnumSet<Sensor> sensorsToInclude) {
        if (transitionTable == null)
            throw new IllegalArgumentException("transitionTable cannot be null");
        if (transitionTable.length == 0)
            throw new IllegalArgumentException("transitionTable cannot be empty");
        if (sensorsToInclude == null)
            throw new IllegalArgumentException("sensorsToInclude cannot be null");
        this.transitionTable = transitionTable;
        this.sensorsToInclude = sensorsToInclude;
        Set<Move> moveSet = this.transitionTable[0].keySet();
        this.moves = moveSet.toArray(new Move[0]);
        for (HashMap<Move, Integer> aTransitionTable : this.transitionTable) {
            moveSet = aTransitionTable.keySet();
            if (this.moves.length != moveSet.size())
                throw new IllegalArgumentException("transitionTable is not valid for FSM. All transitions must exist for each state.");
            for (Move move : this.moves) {
                if (!moveSet.contains(move))
                    throw new IllegalArgumentException("transitionTable is not valid for FSM. All transition moves must exist for each state.");
            }
        }
    }

    /**
     * Get the sensor data included in this environment.
     * @return The {@link EnumSet<Sensor>} indicating the active sensors in this environment.
     */
    public EnumSet<Sensor> getSensorsToInclude() {
        return this.sensorsToInclude;
    }

    /**
     * Get the {@link Move}s for this environment description.
     * @return The array of valid {@link Move}s.
     */
    @Override
    public Move[] getMoves() {
        return this.moves;
    }

    /**
     * Get the transition state based on the given current state and move.
     * @param currentState The state to transition from.
     * @param move The move to make from the current state.
     * @return The new state.
     */
    @Override
    public int transition(int currentState, Move move) {
        if (currentState < 0)
            throw new IllegalArgumentException("currentState cannot be less than 0");
        if (currentState >= this.transitionTable.length)
            throw new IllegalArgumentException("currentState does not exist");
        if (move == null)
            throw new IllegalArgumentException("move cannot be null");
        HashMap<Move, Integer> transitions = this.transitionTable[currentState];
        if (!transitions.containsKey(move))
            throw new IllegalArgumentException("move is invalid for this environment");
        return transitions.get(move);
    }

    /**
     * Determine whether or not the given state is a goal state.
     * @param state The state to test.
     * @return true if the state is the goal state; otherwise false.
     */
    @Override
    public boolean isGoalState(int state) {
        return state == (this.transitionTable.length - 1);
    }

    /**
     * Get the number of states in this environment description.
     * @return The number of states.
     */
    @Override
    public int getNumStates() {
        return this.transitionTable.length;
    }

    @Override
    public int getNumGoalStates(){
        return 1;
    }

    /**
     * Apply sensor data for the given state to the provided {@link SensorData}.
     * @param lastState The state that was transitioned from.
     * @param move The {@link Move} that was applied.
     * @param currentState The state that was transitioned to.
     * @param sensorData The {@link SensorData} to apply sensors to.
     */
    @Override
    public void applySensors(int lastState, Move move, int currentState, SensorData sensorData) {
        if (move == null)
            throw new IllegalArgumentException("move cannot be null");
        if (sensorData == null)
            throw new IllegalArgumentException("sensorData cannot be null");
        if (this.sensorsToInclude.contains(Sensor.EVEN_ODD))
            this.applyEvenOddSensor(currentState, sensorData);
        this.applyWithinNSensors(currentState, sensorData);
        if(this.sensorsToInclude.contains(Sensor.NOISE)){
            this.applyNoiseSensor(currentState, sensorData);
        }
    }

    private void applyEvenOddSensor(int state, SensorData sensorData) {
        sensorData.setSensor(Sensor.EVEN_ODD.toString(), state % 2 == 0);
    }

    private void applyNoiseSensor(int state, SensorData sensorData){
        boolean data = Math.random() > 0.5;
        sensorData.setSensor(Sensor.NOISE.toString(), data);
    }

    private void applyWithinNSensors(int state, SensorData sensorData){
        for(int i = 0; i< 20; i++){
            if(sensorsToInclude.contains(Sensor.fromString("WITHIN_"+i))){
                sensorData.setSensor("WITHIN_"+i, shortestSequences.get(state).size()<=i);
            }
        }
    }

    @Override
    public void addEnvironmentListener(IEnvironmentListener listener){}

    @Override
    public boolean validateSequence(int state, Sequence sequence) {
        int currentState= state;

        for(Move move : sequence.getMoves()){
            currentState= transition(currentState, move);
            if(isGoalState(currentState)){
                return true;
            }
        }

        return false;
    }

    /**
     * Define the available sensors in the environment.
     */
    public enum Sensor {
        /**
         * Identifies the sensor that determines if the current state is even or odd.
         */
        EVEN_ODD,

        WITHIN_1,
        WITHIN_2,
        WITHIN_4,
        WITHIN_8,
        WITHIN_10,
        WITHIN_20,
        /**
         * Identifies the noise sensor that can randomly be applied to a state.
         */
        NOISE;

        /**
         * Identifies the complete sensor set for the environment.
         */
        public static final EnumSet<Sensor> ALL_SENSORS = EnumSet.allOf(Sensor.class);

        public static final EnumSet<Sensor> NO_SENSORS = EnumSet.noneOf(Sensor.class);

        public static final EnumSet<Sensor> WITHIN_SENSORS = EnumSet.of(WITHIN_1,
                WITHIN_2,
                WITHIN_4,
                WITHIN_8,
                WITHIN_10,
                WITHIN_20);

        public static Sensor fromString(String in){
            for(Sensor s : Sensor.values()){
                if(s.toString().equals(in)){
                    return s;
                }
            }
            return null;
        }
    }
    public void setShortestSequences(HashMap<Integer, ArrayList<Move>> shortestSequences){
        this.shortestSequences = shortestSequences;
        this.universalSequence = computeUniversalSequence();
    }

    public Sequence getUniversalSequence(){
        return this.universalSequence;
    }

    private Sequence computeUniversalSequence(){
        ArrayList<Integer> states = new ArrayList<>(this.shortestSequences.keySet());
        states.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return shortestSequences.get(o1).size() - shortestSequences.get(o2).size();
            }
        });

        ArrayList<Move> universalSequence = new ArrayList<>();
        for(Integer i : states){
            int newState = i;
            for(Move m : universalSequence){
                newState = this.transition(newState, m);
                if(isGoalState(newState)) {
                    break;
                }
            }
            universalSequence.addAll(shortestSequences.get(newState));
        }
        return new Sequence( universalSequence.toArray(new Move[0]));
    }
}
