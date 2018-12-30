package environments.meta;

import framework.*;

import java.util.LinkedList;

public class MetaEnvironmentDescription implements IEnvironmentDescription {
    //region Class Variables
    private IEnvironmentDescriptionProvider environmentDescriptionProvider;
    private MetaConfiguration config;
    private IEnvironmentDescription currDescription;
    private int numGoals = 0;
    //endregion

    //region Constructors
    MetaEnvironmentDescription(IEnvironmentDescriptionProvider environmentDescriptionProvider, MetaConfiguration config) {
        if (environmentDescriptionProvider == null)
            throw new IllegalArgumentException("environmentDescriptionProvider cannot be null");
        if (config == null)
            throw new IllegalArgumentException("config cannot be null");
        this.environmentDescriptionProvider = environmentDescriptionProvider;
        this.currDescription = environmentDescriptionProvider.getEnvironmentDescription();
        this.config = config;
    }
    //endregion

    //region IEnvironmentDescription Members
    @Override
    public Move[] getMoves() {
        return this.currDescription.getMoves();
    }

    /**
     * query to determine result of a move
     * @param currentState The state to transition from.
     * @param move The move to make from the current state.
     * @return The state resulting from the move from currentState
     */
    @Override
    public TransitionResult transition(int currentState, Move move) {
        // We can't assume the validity of any input because we could be holding any type of IEnvironmentDescription,
        // so let our internal IEnvironmentDescription perform its own validation.
        TransitionResult result = this.currDescription.transition(currentState, move);
        if (result.getSensorData().isGoal() && ++this.numGoals % this.config.getResetGoalCount() == 0)
            this.currDescription = this.environmentDescriptionProvider.getEnvironmentDescription();
        return result;
    }

    @Override
    public int getRandomState() {
        return this.currDescription.getRandomState();
    }
    //endregion
}