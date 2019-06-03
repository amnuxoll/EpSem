package framework;

/**
 * An IEnvironment represents an environment in which some {@link IAgent} may operat.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IEnvironment {

    //region Methods

    /**
     * Gets all the valid actions for this {@link IEnvironment}.
     *
     * @return an array of valid {@link Action}.
     */
    Action[] getActions();

    /**
     * Executes the given {@link Action} in the {@link IEnvironment}.
     *
     * If NULL or an invalid/unrecognized {@link Action} are provided, this should be treated like a NOP
     * and the {@link SensorData} for the current state should be returned.
     *
     * @param action the {@link Action} to apply.
     * @return the resulting {@link SensorData}.
     */
    SensorData applyAction(Action action);

    //endregion

    //region Defaulted Methods for Introspection

    /**
     * Validates the provided {@link Sequence} to indicate whether or not its execution would result in the goal
     * at any point.
     *
     * @param sequence the {@link Sequence} to validate.
     * @return true if the sequence would work (and the method is implemented); otherwise false.
     */
    default boolean validateSequence(Sequence sequence) { return false; }

    //endregion

}
