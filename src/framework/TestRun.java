package framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestRun implements IIntrospector {
    //region Class Variables
    private IAgent agent;
    private IEnvironment environment;
    private int numberOfGoalsToFind;
    private List<IGoalListener> goalListeners = new ArrayList<>();
    //endregion

    //region Constructors
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
    @Override
    public boolean validateSequence(Sequence sequence) {
        return this.environment.validateSequence(sequence);
    }
    //endregion
}
