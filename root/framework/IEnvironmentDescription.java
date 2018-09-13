package framework;

/**
 * An IEnvironmentDescription describes an environment shape that {@link Environment} can use to track
 * agent progress during a test run.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IEnvironmentDescription {

    /**
     * Get the valid moves for this environment description.
     * @return an array of {@link Move} for this environment description.
     */
    Move[] getMoves();

    /**
     * Get the next state from the given move at the current state.
     * @param currentState The state to transition from.
     * @param move The move to make from the current state.
     * @return The resulting state.
     */
    int transition(int currentState, Move move);

    /**
     * Determine whether or not the given state is a goal state.
     * @param state The state to test.
     * @return true if the state is a goal state; otherwise false.
     */
    boolean isGoalState(int state);

    /**
     * Get the number of states in this environment description.
     * @return The number of states.
     */
    int getNumStates();

    /**
     * Apply all sensor data for the given state to the provided sennsor data.
     * @param lastState The state that was transitioned from.
     * @param move The {@link Move} that was applied.
     * @param currentState The state that was transitioned to.
     * @param sensorData The {@link SensorData} to apply sensors to.
     */
    void applySensors(int lastState, Move move, int currentState, SensorData sensorData);
}
