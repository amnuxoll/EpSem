package framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link TestRun} will marshal calls between a single {@link IAgent} and a single {@link IEnvironment}
 * for a specific given N runs to the goal.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestRun implements IIntrospector {

    //region Class Variables

    /** The {@link IAgent} to run in the environment. */
    private IAgent agent;

    /** The {@link IEnvironment} to operate in. */
    private IEnvironment environment;

    /** The number of goals to find in the environment. */
    private int numberOfGoalsToFind;

    /** Subscribers to the goal found event. */
    private List<IGoalListener> goalListeners = new ArrayList<>();

    //endregion

    //region Constructors

    /**
     * Creates an instance of a {@link TestRun} for the given agent and environment.
     *
     * @param agent the {@link IAgent} to run.
     * @param environment the {@link IEnvironment} to run.
     * @param numberOfGoalsToFind the number of goals to find in the environment.
     * @throws IllegalArgumentException
     */
    public TestRun(IAgent agent, IEnvironment environment, int numberOfGoalsToFind) throws IllegalArgumentException {
        if (agent == null)
            throw new IllegalArgumentException("agent cannot be null");
        if (environment == null)
            throw new IllegalArgumentException("environment cannot be null");
        if (numberOfGoalsToFind < 1)
            throw new IllegalArgumentException("numberOfGoalsToFind cannot be less than 1");

        this.environment = environment;
        this.agent = agent;
        this.numberOfGoalsToFind = numberOfGoalsToFind;
    }

    //endregion

    //region Public Methods

    /**
     * Begins execution of the {@link TestRun}.
     */
    public void execute() {
        try {
            int goalCount = 0;
            int moveCount = 0;
            this.agent.initialize(this.environment.getActions(), this);
            SensorData sensorData = this.environment.applyAction(null);
            do {
                Action action = this.agent.getNextAction(sensorData);
                if(action == null) break;
                sensorData = this.environment.applyAction(action);
                moveCount++;

                if (sensorData.isGoal()) {
                    this.agent.onGoalFound();
                    this.fireGoalEvent(moveCount);
                    goalCount++;
                    moveCount = 0;
                }
            } while (goalCount < this.numberOfGoalsToFind);
            this.agent.onTestRunComplete();
        } catch (Exception ex) {
            NamedOutput.getInstance().write("framework", ex);
        }
    }

    /**
     * Allows new subscribers to the goal event to be registered.
     *
     * @param listener the goal event subscriber to register.
     */
    public synchronized void addGoalListener(IGoalListener listener) {
        this.goalListeners.add(listener);
    }

    //endregion

    //region Private Methods

    private synchronized void fireGoalEvent(int stepsToGoal) throws IOException {
        GoalEvent goal = new GoalEvent(this, stepsToGoal, this.agent.getGoalData());
        for (IGoalListener listener : this.goalListeners) {
            listener.goalReceived(goal);
        }
    }

    //endregion

    //region IIntrospector Members

    /**
     * Determines if the execution of the given {@link Sequence} would result in hitting a goal at any point.
     *
     * @param sequence the {@link Sequence} to validate.
     * @return true if the goal would be hit; otherwise false.
     */
    @Override
    public boolean validateSequence(Sequence sequence) {
        return this.environment.validateSequence(sequence);
    }

    //endregion
}
