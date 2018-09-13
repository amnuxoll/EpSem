package framework;

/**
 * An Environment will take a {@link IEnvironmentDescription} and manage an {@link IAgent} as it
 * explores while trying to find the goal.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
class Environment {
    private IEnvironmentDescription environmentDescription;
    private int currentState;

    /**
     * Create an environment based on the provided environmentDescription.
     * @param environmentDescription A {@link IEnvironmentDescription} that {@link Environment} can use to track an agent's progress.
     */
    public Environment(IEnvironmentDescription environmentDescription) {
        if (environmentDescription == null)
            throw new IllegalArgumentException("environmentDescription cannot be null");
        this.currentState = 0;
        this.environmentDescription = environmentDescription;
    }

    /**
     * Get the moves allowed in this {@link Environment} instance.
     * @return An array of {@link Move} that is valid in this {@link Environment}.
     */
    public Move[] getMoves() {
        return this.environmentDescription.getMoves();
    }

    /**
     * Apply a {@link Move} to this {@link Environment} instance.
     * @param move The {@link Move} to apply.
     * @return The {@link SensorData} that resutled from the move.
     */
    public SensorData tick(Move move) {
        if (move == null)
            throw new IllegalArgumentException("move cannot be null");
        int lastState = this.currentState;
        this.currentState = this.environmentDescription.transition(this.currentState, move);
        boolean hitGoal = this.environmentDescription.isGoalState(this.currentState);
        SensorData sensorData = new SensorData(hitGoal);
        this.environmentDescription.applySensors(lastState, move, this.currentState, sensorData);
        return sensorData;
    }

    /**
     * Resets the {@link Environment} by randomly relocating the current state.
     */
    public void reset() {
        int nonGoalStateCount = this.environmentDescription.getNumStates();
        this.currentState = Services.retrieve(IRandomizer.class).getRandomNumber(nonGoalStateCount);
    }

    /**
     * Gets the current state of the {@link IAgent}.
     * @return The current state as defined by its index.
     */
    public int getCurrentState() {
        return this.currentState;
    }
}