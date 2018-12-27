package framework;


import agents.juno.JunoAgent;
import utils.ExceptionStackTraceToString;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
class TestRun {

    private IAgent agent;
    private IEnvironmentDescription environmentDescription;
    private Environment environment;
    private int numberOfGoalsToFind;
    private List<IGoalListener> goalListeners = new ArrayList();

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

    public void execute() {
        try {
            int goalCount = 0;
            int moveCount = 0;
            this.agent.initialize(this.environmentDescription.getMoves(), sequence -> this.environmentDescription.validateSequence(this.environment.getCurrentState(), sequence));
            SensorData sensorData = null;
            do {
                Move move = this.agent.getNextMove(sensorData);
                sensorData = environment.tick(move);
                moveCount++;

                if (sensorData.isGoal()) {
                    this.agent.onGoalFound();
                    this.fireGoalEvent(moveCount);
                    goalCount++;
                    moveCount = 0;
                    environment.reset();
                }
            } while (goalCount < this.numberOfGoalsToFind);
            this.agent.onTestRunComplete();
        } catch (Exception ex) {
            NamedOutput namedOutput = NamedOutput.getInstance();
            namedOutput.write("framework", "TestRun failed with exception: " + ex.getMessage());
            namedOutput.write("framework", ExceptionStackTraceToString.getString(ex));
        }
    }

    public synchronized void addGoalListener(IGoalListener listener) {
        this.goalListeners.add(listener);
    }

    private synchronized void fireGoalEvent(int stepsToGoal) {
        GoalEvent goal = new GoalEvent(this, stepsToGoal, this.agent.getResultWriterData());
        for (IGoalListener listener : this.goalListeners) {
            listener.goalReceived(goal);
        }
    }
}
