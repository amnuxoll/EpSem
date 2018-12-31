package framework;

/**
 * An IEnvironmentDescription describes an environment shape that {@link Environment} can use to track
 * agent progress during a test run.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IEnvironmentDescription {
    //region Methods
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
    TransitionResult transition(int currentState, Move move);

    /**
     * Get the number of states in this environment description.
     * @return The number of states.
     */
    int getRandomState();
    //endregion
}
