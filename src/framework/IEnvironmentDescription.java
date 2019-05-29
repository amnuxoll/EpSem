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
     * Get the valid actions for this environment description.
     * @return an array of {@link Action} for this environment description.
     */
    Action[] getActions();

    /**
     * Get the next state from the given action at the current state.
     * @param currentState The state to transition from.
     * @param action The action to make from the current state.
     * @return The resulting state.
     */
    TransitionResult transition(int currentState, Action action);

    /**
     * Get the number of states in this environment description.
     * @return The number of states.
     */
    int getRandomState();
    //endregion
}
