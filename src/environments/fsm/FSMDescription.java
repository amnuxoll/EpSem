package environments.fsm;

import framework.*;

import java.util.*;

/**
 * An FSMDescription provides information to {@link framework.Environment} for running an experiment.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMDescription implements IEnvironmentDescription {
    //region Class Variables
    private FSMTransitionTable transitionTable;
    private Action[] actions;
    private EnumSet<Sensor> sensorsToInclude;
    private ArrayList<HashMap<Action, Integer>> transitionSensorTable;
    private Random random;

    // Variables for generating the rules
    private HashMap<Episode, HashMap<String, Double>> rules;

    //endregion

    //region Constructors
    /**
     * Create an instance of a {@link FSMDescription}.
     * @param transitionTable The transition table that indicates the structure of a FSM.
     */
    public FSMDescription(FSMTransitionTable transitionTable) {
        this(transitionTable, EnumSet.noneOf(Sensor.class));
    }

    /**
     * Create an instance of a {@link FSMDescription} that includes possible sensor data.
     * @param transitionTable The transition table that indicates the structure of a FSM.
     * @param sensorsToInclude The sensors to include when navigating the FSM.
     */
    public FSMDescription(FSMTransitionTable transitionTable, EnumSet<Sensor> sensorsToInclude) {
        if (transitionTable == null)
            throw new IllegalArgumentException("transitionTable cannot be null");
        if (sensorsToInclude == null)
            throw new IllegalArgumentException("sensorsToInclude cannot be null");
        this.random = new Random(42);
        this.transitionTable = transitionTable;
        this.sensorsToInclude = sensorsToInclude;
        this.actions = this.transitionTable.getTransitions()[0].keySet().toArray(new Action[0]);
        int numberOfStates = this.transitionTable.getNumberOfStates();
        this.transitionSensorTable = new ArrayList<>();
        for(int i = 0; i < numberOfStates; i++) {
            HashMap<Action, Integer> transitions = new HashMap<>();
            for (Action m : getActions()) {
                transitions.put(m, 0);
            }
            this.transitionSensorTable.add(transitions);
        }
    }
    //endregion

    //region IEnvironmentDescription Members
    /**
     * Get the {@link Action}s for this environment description.
     * @return The array of valid {@link Action}s.
     */
    @Override
    public Action[] getActions() {
        return this.actions;
    }

    /**
     * Get the transition state based on the given current state and action.
     * @param currentState The state to transition from.
     * @param action The action to make from the current state.
     * @return The new state.
     */
    @Override
    public TransitionResult transition(int currentState, Action action) {
        if (currentState < 0)
            throw new IllegalArgumentException("currentState cannot be less than 0");
        if (currentState >= this.transitionTable.getNumberOfStates())
            throw new IllegalArgumentException("currentState does not exist");
        if (action == null)
            throw new IllegalArgumentException("action cannot be null");
        HashMap<Action, Integer> transitions = this.transitionTable.getTransitions()[currentState];
        if (!transitions.containsKey(action))
            throw new IllegalArgumentException("action is invalid for this environment");
        this.transitionSensorTable.get(currentState).put(action, this.transitionSensorTable.get(currentState).get(action)+1);
        int newState = transitions.get(action);
        SensorData sensorData = new SensorData(this.transitionTable.isGoalState(newState));
        this.applySensors(currentState, action, newState, sensorData);
        return new TransitionResult(newState, sensorData);
    }

    @Override
    public int getRandomState() {

        int nonGoalStates = this.transitionTable.getNumberOfStates() - 1;
        return random.nextInt(nonGoalStates);
    }
    //endregion

    //region Public Methods
    public void tweakTable(int numSwaps, Random random) {
        if (numSwaps < 0)
            throw new IllegalArgumentException("numSwaps must be greater than or equal to zero.");
        if (random == null)
            throw new IllegalArgumentException("random cannot be null.");
        for(FSMTransitionTable.Tweak tweak : this.transitionTable.tweakTable(numSwaps, random))
        {
            //update the sensor table to reflect the "new" transitions
            this.transitionSensorTable.get(tweak.state).put(this.actions[tweak.move1], 0);
            this.transitionSensorTable.get(tweak.state).put(this.actions[tweak.move2], 0);
        }
    }
    //endregion

    //region Private Methods
    /**
     * Apply sensor data for the given state to the provided {@link SensorData}.
     * @param lastState The state that was transitioned from.
     * @param action The {@link Action} that was applied.
     * @param currentState The state that was transitioned to.
     * @param sensorData The {@link SensorData} to apply sensors to.
     */
    private void applySensors(int lastState, Action action, int currentState, SensorData sensorData) {
        if (this.sensorsToInclude.contains(Sensor.EVEN_ODD))
            this.applyEvenOddSensor(currentState, sensorData);
        if (this.sensorsToInclude.contains(Sensor.MOD_3)){
            this.applyMod3Sensor(currentState, sensorData);
        }
        this.applyWithinNSensors(currentState, sensorData);
        this.applyNoiseSensors(sensorData);
        if (this.sensorsToInclude.contains(Sensor.TRANSITION_AGE))
            this.applyTransitionAgeSensor(lastState, action, sensorData);
    }

    private void applyEvenOddSensor(int state, SensorData sensorData) {
        sensorData.setSensor(Sensor.EVEN_ODD.toString(), state % 2 == 0);
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

    private void applyNoiseSensor(int state, SensorData sensorData){
        boolean data = Math.random() > 0.5;
        sensorData.setSensor(Sensor.NOISE1.toString(), data);
    }

    private void applyWithinNSensors(int state, SensorData sensorData){
        for(int i = 0; i< 20; i++){
            if(sensorsToInclude.contains(Sensor.fromString("WITHIN_"+i))){
                sensorData.setSensor("WITHIN_" + i, this.transitionTable.getShortestSequences().get(state).size() <= i);
            }
        }
    }

    private void applyTransitionAgeSensor(int lastState, Action action, SensorData sensorData) {
        sensorData.setSensor("Transition age", this.transitionSensorTable.get(lastState).get(action) <= 5);
    }
    //endregion

    //region Enums
    /**
     * Define the available sensors in the environment.
     */
    public enum Sensor {
        /**
         * Identifies the sensor that determines if the current state is even or odd.
         */
        EVEN_ODD,
        MOD_3,

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
