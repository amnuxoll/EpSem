package environments.fsm;

import framework.*;

import java.util.*;

/**
 * An FSMEnvironment is an environment modeled as a Finite State Machine.
 *
 *
 * TODO -- Add back in some variation of the transition age sensor.
 * TODO -- Add a configuration that allows the FSM to tweak its transition table every N goals.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMEnvironment implements IEnvironment {
    //region Class Variables
    private FSMTransitionTable transitionTable;
    private Action[] actions;
    private EnumSet<Sensor> sensorsToInclude;
    //seed to DEBUG
    private static Random random = new Random(13);

    private int currentState;
    //endregion

    //region Constructors
    /**
     * Create an instance of a {@link FSMEnvironment}.
     * @param transitionTable The transition table that indicates the structure of a FSM.
     */
    public FSMEnvironment(FSMTransitionTable transitionTable) {
        this(transitionTable, EnumSet.noneOf(Sensor.class));
    }

    /**
     * Create an instance of a {@link FSMEnvironment} that includes possible sensor data.
     * @param transitionTable The transition table that indicates the structure of a FSM.
     * @param sensorsToInclude The sensors to include when navigating the FSM.
     */
    public FSMEnvironment(FSMTransitionTable transitionTable, EnumSet<Sensor> sensorsToInclude) {
        if (transitionTable == null)
            throw new IllegalArgumentException("transitionTable cannot be null");
        if (sensorsToInclude == null)
            throw new IllegalArgumentException("sensorsToInclude cannot be null");
        this.transitionTable = transitionTable;
        this.sensorsToInclude = sensorsToInclude;
        this.actions = this.transitionTable.getTransitions()[0].keySet().toArray(new Action[0]);
        this.currentState = this.getRandomState();

        //DEBUG: print the transition table
        //System.err.println("Transition table:");
        //System.err.println(this.transitionTable);

        //DEBUG: print the universal sequence
        Sequence uSeq = this.transitionTable.getUniversalSequence();
        System.err.println("Universal sequence: " + uSeq + " (" + uSeq.getLength() + " steps)");
        System.out.println(this.transitionTable.dotFormOutput());
    }

    private FSMEnvironment(FSMEnvironment toCopy) {
        this.transitionTable = toCopy.transitionTable;
        this.sensorsToInclude = toCopy.sensorsToInclude;
        this.actions = toCopy.actions;
        this.currentState = toCopy.currentState;
    }

    //endregion

    //region Accessors

    public int getCurrentState() { return this.currentState; }

    //endregion

    //region IEnvironment Members
    /**
     * Get the {@link Action}s for this environment description.
     * @return The array of valid {@link Action}s.
     */
    @Override
    public Action[] getActions() {
        return this.actions;
    }

    /**
     *
     * @param action null or invalid action return sensor data for current state with the GOAL flag set.
     * @return
     */
    @Override
    public SensorData applyAction(Action action) {
        SensorData sensorData;
        HashMap<Action, Integer> transitions = this.transitionTable.getTransitions()[this.currentState];
        if (action == null || !transitions.containsKey(action)) {
            sensorData = new SensorData(true);
            this.applySensors(this.currentState, sensorData);
            return sensorData;
        }
        this.currentState = transitions.get(action);
        if (this.transitionTable.isGoalState(this.currentState)) {
            sensorData = new SensorData(true);
            this.currentState = this.getRandomState();
        }
        else
            sensorData = new SensorData(false);
        this.applySensors(this.currentState, sensorData);

        return sensorData;
    }

    /**
     * Creates a copy of this {@link IEnvironment}.
     *
     * @return A copy of the environment. Be mindful of shallow vs deep copy when implementing to prevent contaminating
     * test runs.
     */
    @Override
    public IEnvironment copy() {
        return new FSMEnvironment(this);
    }

    @Override
    public boolean validateSequence(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null.");
        int tempState = this.currentState;
        for (Action action : sequence.getActions()) {
            tempState = this.transitionTable.getTransitions()[tempState].get(action);
            if (this.transitionTable.isGoalState(tempState))
                return true;
        }
        return false;
    }
    //endregion

    //region Private Methods
    private int getRandomState() {
        int nonGoalStates = this.transitionTable.getNumberOfStates() - 1;
        return random.nextInt(nonGoalStates);
    }

    /**
     * Apply sensor data for the given state to the provided {@link SensorData}.
     * @param currentState The state that was transitioned to.
     * @param sensorData The {@link SensorData} to apply sensors to.
     */
    private void applySensors(int currentState, SensorData sensorData) {
        if (this.sensorsToInclude.contains(Sensor.IS_EVEN))
            this.applyEvenSensor(currentState, sensorData);
        if (this.sensorsToInclude.contains(Sensor.IS_ODD))
            this.applyOddSensor(currentState, sensorData);
        if (this.sensorsToInclude.contains(Sensor.MOD_3)){
            this.applyMod3Sensor(currentState, sensorData);
        }
        this.applyWithinNSensors(currentState, sensorData);
        this.applyNoiseSensors(sensorData);
        this.applyCactiSensors(currentState, sensorData);
    }

    private void applyEvenSensor(int state, SensorData sensorData) {
        sensorData.setSensor(Sensor.IS_EVEN.toString(), state % 2 == 0);
    }

    private void applyOddSensor(int state, SensorData sensorData) {
        sensorData.setSensor(Sensor.IS_ODD.toString(), state % 2 == 1);
    }

    private void applyMod3Sensor(int state, SensorData sensorData){
        sensorData.setSensor(Sensor.MOD_3.toString(), state % 3 == 0);
    }

    private void applyNoiseSensors(SensorData sensorData) {
        if (this.sensorsToInclude.contains(Sensor.NOISE1))
            sensorData.setSensor(Sensor.NOISE1.toString(), Math.random() > 0.5);
        if (this.sensorsToInclude.contains(Sensor.NOISE2))
            sensorData.setSensor(Sensor.NOISE2.toString(), Math.random() > 0.5);
        if (this.sensorsToInclude.contains(Sensor.NOISE3))
            sensorData.setSensor(Sensor.NOISE3.toString(), Math.random() > 0.5);
        if (this.sensorsToInclude.contains(Sensor.NOISE4))
            sensorData.setSensor(Sensor.NOISE4.toString(), Math.random() > 0.5);
    }

    private void applyCactiSensors(int state, SensorData sensorData) {
        if (this.sensorsToInclude.contains(Sensor.CACTUS1))
            sensorData.setSensor(Sensor.CACTUS1.toString(), state == 0);
        if (this.sensorsToInclude.contains(Sensor.CACTUS2))
            sensorData.setSensor(Sensor.CACTUS2.toString(), state == 1);
        if (this.sensorsToInclude.contains(Sensor.CACTUS3))
            sensorData.setSensor(Sensor.CACTUS3.toString(), state == 2);
        if (this.sensorsToInclude.contains(Sensor.CACTUS4))
            sensorData.setSensor(Sensor.CACTUS4.toString(), state == 3);
        if (this.sensorsToInclude.contains(Sensor.CACTUS5))
            sensorData.setSensor(Sensor.CACTUS5.toString(), state == 4);
        if (this.sensorsToInclude.contains(Sensor.CACTUS6))
            sensorData.setSensor(Sensor.CACTUS6.toString(), state == 5);
        if (this.sensorsToInclude.contains(Sensor.CACTUS7))
            sensorData.setSensor(Sensor.CACTUS7.toString(), state == 6);
        if (this.sensorsToInclude.contains(Sensor.CACTUS8))
            sensorData.setSensor(Sensor.CACTUS8.toString(), state == 7);
        if (this.sensorsToInclude.contains(Sensor.CACTUS9))
            sensorData.setSensor(Sensor.CACTUS9.toString(), state == 8);
    }

    private void applyWithinNSensors(int state, SensorData sensorData){
//        for(int i = 0; i< 20; i++){
//            if(sensorsToInclude.contains(Sensor.fromString("WITHIN_"+i))){
//                sensorData.setSensor("WITHIN_" + i, this.transitionTable.getShortestSequences().get(state).size() <= i);
//            }
//        }
    }

    /** @return a string showing the shortest path to the goal from a given state */
    public String getShortestSequenceString(Integer currState) {
        ArrayList<Action> shortSeq = this.transitionTable.getShortestSequences().get(currState);
        StringBuilder result = new StringBuilder();
        for(Action act : shortSeq) {
            result.append(act.toString());
        }
        return result.toString();
    }

    /** @return a string showing the shortest blind path to the goal from a given state */
    public String getBlindPathString(Integer startState) {
        StringBuilder result = new StringBuilder("");
        Sequence uSeq = this.transitionTable.getUniversalSequence();
        int here = startState;
        for(Action act : uSeq.getActions()) {
            if (here == this.transitionTable.getNumberOfStates() - 1) {
                break;
            }

            result.append(act.toString());
            here = this.transitionTable.getTransitions()[here].get(act);
        }

        return result.toString();
    }//getBlindPathString

    //endregion

    //region Enums
    /**
     * Define the available sensors in the environment.
     */
    public enum Sensor {
        /**
         * Identifies the sensor that determines if the current state is even or odd.
         */
        IS_EVEN,
        IS_ODD,
        MOD_3,

        /** Identifies the sensor that turns on for exactly one state in the FSM */
        CACTUS1, //On if in state 0
        CACTUS2, //On if in state 1
        CACTUS3, // ...
        CACTUS4,
        CACTUS5,
        CACTUS6,
        CACTUS7,
        CACTUS8, // ...
        CACTUS9, //On in state 8

        WITHIN_1,
        WITHIN_2,
        WITHIN_4,
        WITHIN_8,
        WITHIN_10,
        WITHIN_20,
        /**
         * Identifies the noise sensor that can randomly be applied to a state.
         */
        NOISE1,
        NOISE2,
        NOISE3,
        NOISE4,
        TRANSITION_AGE;

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

        public static final EnumSet<Sensor> CACTI_SENSORS = EnumSet.of(CACTUS1,
                CACTUS2,
                CACTUS3,
                CACTUS4,
                CACTUS5,
                CACTUS6,
                CACTUS7,
                CACTUS8,
                CACTUS9);

        public static Sensor fromString(String in){
            for(Sensor s : Sensor.values()){
                if(s.toString().equals(in)){
                    return s;
                }
            }
            return null;
        }
    }
    //endregion
}
