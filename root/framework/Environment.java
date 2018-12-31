package framework;

/**
 * An Environment will take a {@link IEnvironmentDescription} and manage an {@link IAgent} as it
 * explores while trying to find the goal.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Environment implements IIntrospector {
    //region Class Variables
    private IEnvironmentDescription environmentDescription;
    private int currentState;
    //endregion

    //region Constructors
    /**
     * Create an environment based on the provided environmentDescription.
     * @param environmentDescription A {@link IEnvironmentDescription} that {@link Environment} can use to track an agent's progress.
     */
    public Environment(IEnvironmentDescription environmentDescription) {
        if (environmentDescription == null)
            throw new IllegalArgumentException("environmentDescription cannot be null");
        this.environmentDescription = environmentDescription;
        this.reset();
    }
    //endregion

    //region Public Methods
    /**
     * Apply a {@link Move} to this {@link Environment} instance.
     * @param move The {@link Move} to apply.
     * @return The {@link SensorData} that resutled from the move.
     */
    public SensorData tick(Move move) {
        if (move == null)
            throw new IllegalArgumentException("move cannot be null");
        TransitionResult result = this.environmentDescription.transition(this.currentState, move);
        this.currentState = result.getState();
        return result.getSensorData();
    }

    /**
     * Resets the {@link Environment} by randomly relocating the current state.
     */
    public void reset() {
        this.currentState = this.environmentDescription.getRandomState();
    }
    //endregion

    //region IIntrospector Members
    @Override
    public boolean validateSequence(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null.");
        int tempState = this.currentState;
        for (Move move : sequence.getMoves()){
            TransitionResult result = this.environmentDescription.transition(tempState, move);
            if (result.getSensorData().isGoal())
                return true;
            tempState = result.getState();
        }
        return false;
    }
    //endregion
}