package environments.meta;

import framework.*;

/**
 * A MetaEnvironment is a special case of {@link IEnvironment} that wraps a different description
 * type. After a certain number of goals, the nested {@link IEnvironment} is regenerated. This is for
 * stress on an agent because it means that its memory will become stale and will be tested for resilience
 * in extreme adversity.
 */
public class MetaEnvironment implements IEnvironment {
    //region Class Variables
    private IEnvironmentProvider environmentDescriptionProvider;
    private MetaConfiguration config;
    private IEnvironment currDescription;
    private int numGoals = 0;
    //endregion

    //region Constructors
    public MetaEnvironment(IEnvironmentProvider environmentDescriptionProvider, MetaConfiguration config) {
        if (environmentDescriptionProvider == null)
            throw new IllegalArgumentException("environmentDescriptionProvider cannot be null");
        if (config == null)
            throw new IllegalArgumentException("config cannot be null");
        this.environmentDescriptionProvider = environmentDescriptionProvider;
        this.currDescription = environmentDescriptionProvider.getEnvironment();
        this.config = config;
    }
    //endregion

    //region IEnvironmentDescription Members
    @Override
    public Action[] getActions() {
        return this.currDescription.getActions();
    }

    /**
     * query to determine result of a action
     * @param action The action to make from the current state.
     * @return The sensor resulting from the action from currentState.
     */
    @Override
    public SensorData applyAction(Action action) {
        // We can't assume the validity of any input because we could be holding any type of IEnvironmentDescription,
        // so let our internal IEnvironmentDescription perform its own validation.
        SensorData result = this.currDescription.applyAction(action);
        if (result.isGoal() && ++this.numGoals % this.config.getResetGoalCount() == 0)
            this.currDescription = this.environmentDescriptionProvider.getEnvironment();
        return result;
    }

    @Override
    public SensorData getNewStart() {
        return null;
    }

    @Override
    public Boolean validateSequence(Sequence sequence) {
        return null;
    }
    //endregion
}