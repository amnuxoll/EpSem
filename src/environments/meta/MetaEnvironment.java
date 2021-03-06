package environments.meta;

import framework.*;

/**
 * A MetaEnvironment is a special case of {@link IEnvironment} that wraps a different description
 * type. After a certain number of goals, the nested {@link IEnvironment} is regenerated. This is for
 * stress on an agent because it means that its memory will become stale and will be tested for resilience
 * in extreme adversity.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class MetaEnvironment implements IEnvironment {

    //region Class Variables

    private IEnvironmentProvider environmentProvider;
    private MetaConfiguration config;
    private IEnvironment currEnvironment;
    private int numGoals = 0;

    //endregion

    //region Constructors

    /**
     * Creates an instance of a {@link MetaEnvironment}.
     *
     * @param environmentProvider The provider to use for internal environment production.
     * @param config The {@link MetaConfiguration} that defines this environment.
     */
    public MetaEnvironment(IEnvironmentProvider environmentProvider, MetaConfiguration config) {
        if (environmentProvider == null)
            throw new IllegalArgumentException("environmentProvider cannot be null");
        if (config == null)
            throw new IllegalArgumentException("config cannot be null");
        this.environmentProvider = environmentProvider;
        this.currEnvironment = environmentProvider.getEnvironment();
        this.config = config;
    }
    //endregion

    //region IEnvironmentDescription

    /**
     * Gets all the valid actions for this {@link IEnvironment}.
     *
     * @return an array of valid {@link Action}.
     */
    @Override
    public Action[] getActions() {
        return this.currEnvironment.getActions();
    }

    /**
     * query to determine result of a action
     * @param action The action to make from the current state.
     * @return The sensor resulting from the action from currentState.
     */
    @Override
    public SensorData applyAction(Action action) {
        // We can't assume the validity of any input because we could be holding any IEnvironment,
        // so let our internal IEnvironment perform its own validation.
        SensorData result = this.currEnvironment.applyAction(action);
        if (result.isGoal() && ++this.numGoals % this.config.getResetGoalCount() == 0)
            this.currEnvironment = this.environmentProvider.getEnvironment();
        return result;
    }

    /**
     * Creates a copy of this {@link IEnvironment}.
     *
     * @return A copy of the environment. Be mindful of shallow vs deep copy when implementing to prevent contaminating
     * test runs.
     */
    @Override
    public IEnvironment copy() {
        // To be able to do this right we also need IEnvironmentProvider to be cloneable and that's not a priority right now.
        throw new RuntimeException();
    }

    /**
     * Validates the provided {@link Sequence} to indicate whether or not its execution would result in the goal
     * at any point.
     *
     * @param sequence the {@link Sequence} to validate.
     * @return true if the sequence would work (and the method is implemented); otherwise false.
     */
    @Override
    public boolean validateSequence(Sequence sequence) {
        return this.currEnvironment.validateSequence(sequence);
    }
    //endregion
}