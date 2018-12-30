package framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
class TestRun {
    //region Class Variables
    private IAgent agent;
    private IEnvironmentDescription environmentDescription;
    private int numberOfGoalsToFind;
    private Environment environment;
    private List<IGoalListener> goalListeners = new ArrayList();
    //endregion

    //region Constructors
    public TestRun(IAgent agent, IEnvironmentDescription environmentDescription, int numberOfGoalsToFind) throws IllegalArgumentException {
        if (agent == null)
            throw new IllegalArgumentException("agent cannot be null");
        if (environmentDescription == null)
            throw new IllegalArgumentException("environmentDescription cannot be null");
        if (numberOfGoalsToFind < 1)
            throw new IllegalArgumentException("numberOfGoalsToFind cannot be less than 1");

        this.environmentDescription = environmentDescription;
        this.environment = new Environment(this.environmentDescription);
        this.agent = agent;
        this.numberOfGoalsToFind = numberOfGoalsToFind;
    }
    //endregion

    //region Public Methods
    public void execute() {
        try {
            int goalCount = 0;
            int moveCount = 0;
            this.agent.initialize(this.environmentDescription.getMoves(), this.environment);
            SensorData sensorData = null;
            do {
                Move move = this.agent.getNextMove(sensorData);
                sensorData = this.environment.tick(move);
                moveCount++;

                if (sensorData.isGoal()) {
                    this.agent.onGoalFound();
                    this.fireGoalEvent(moveCount);
                    goalCount++;
                    moveCount = 0;
                    this.environment.reset();
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
        GoalEvent goal = new GoalEvent(this, stepsToGoal, this.agent.getData());
        for (IGoalListener listener : this.goalListeners) {
            listener.goalReceived(goal);
        }
    }
    //endregion
}
