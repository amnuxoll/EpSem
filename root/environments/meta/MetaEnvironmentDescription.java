package environments.meta;

import framework.*;
import framework.Sequence;
import java.util.LinkedList;

public class MetaEnvironmentDescription implements IEnvironmentDescription {
    //region Class Variables
    private IEnvironmentDescriptionProvider environmentDescriptionProvider;
    private IEnvironmentDescription currDescription;
    //how many transitions the agent took to reach the goal
    private LinkedList<Integer> successQueue= new LinkedList<>();
    //number of moves since current goal
    private int transitionCounter= 0;
    private int numGoals= 0;
    private MetaConfiguration config;
    //endregion

    //region Constructors
    MetaEnvironmentDescription(IEnvironmentDescriptionProvider environmentDescriptionProvider, MetaConfiguration config) {
        if (environmentDescriptionProvider == null) {
            throw new IllegalArgumentException("environmentDescriptionProvider cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.environmentDescriptionProvider = environmentDescriptionProvider;
        this.currDescription = environmentDescriptionProvider.getEnvironmentDescription();
        this.config= config;
    }
    //endregion

    //region IEnvironmentDescription Members
    @Override
    public Move[] getMoves() {
        return currDescription.getMoves();
    }

    /**
     * query to determine result of a move
     * @param currentState The state to transition from.
     * @param move The move to make from the current state.
     * @return The state resulting from the move from currentState
     */
    @Override
    public TransitionResult transition(int currentState, Move move) {
        transitionCounter++;
        // assume our internal IEnvironmentDescription is doing validation.
        return currDescription.transition(currentState, move);
    }

    @Override
    public int getRandomState() {
        return this.currDescription.getRandomState();
    }
    //endregion

    //region Public Methods
    /**
     *
     * @return number of moves since current goal
     */
    public int getTransitionCounter() {
        return this.transitionCounter;
    }

    /**
     *
     * @return the current several number of moves it took to reach the goal
     */
    public LinkedList<Integer> getSuccessQueue() {
        return this.successQueue;
    }
    //endregion

    //region Private Methods
    /**
     * takes the average of the current successQueueSize transition counts
     * and update the fsm description is the target threshold is reached
     */
    private void makeNewDescription() {
        this.successQueue.add(this.transitionCounter);
        while (this.successQueue.size() > this.config.getSuccessQueueMaxSize()) {
            this.successQueue.remove();
        }
        //if the average is less than our threshold and we have collected enough data
        if (this.numGoals%this.config.getTweakPoint() == 0) {
            //make a new environment
            this.currDescription = this.environmentDescriptionProvider.getEnvironmentDescription();
            //and clear the queue
            this.successQueue.clear();
        }
    }
    //endregion
}