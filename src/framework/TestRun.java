package framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestRun {
    //region Class Variables
    private IAgent agent;
    private IEnvironmentDescription environmentDescription;
    private int numberOfGoalsToFind;
    private Environment environment;
    private List<IGoalListener> goalListeners = new ArrayList<>();
    private List<IAgentFinishedListener> agentFinishedListeners = new ArrayList<>();
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
            SensorData sensorData = null; //TODO: Initialize first sensor data value?
            do {
                Move move = this.agent.getNextMove(sensorData);
                if(move == null) break;
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
            this.fireAgentFinishedEvent();
            this.agent.onTestRunComplete();
        } catch (Exception ex) {
            NamedOutput.getInstance().write("framework", ex);
        }
    }

    public synchronized void addGoalListener(IGoalListener listener) {
        this.goalListeners.add(listener);
    }

    public synchronized void addAgentFinishedListener(IAgentFinishedListener listener) {
        this.agentFinishedListeners.add(listener);
    }
    //endregion

    //region Private Methods
    private synchronized void fireGoalEvent(int stepsToGoal) throws IOException {
        GoalEvent goal = new GoalEvent(this, stepsToGoal, this.agent.getGoalData());
        for (IGoalListener listener : this.goalListeners) {
            listener.goalReceived(goal);
        }
    }

    private synchronized void fireAgentFinishedEvent() throws IOException {
        AgentFinishedEvent event = new AgentFinishedEvent(this, this.agent.getAgentFinishedData());
        for(IAgentFinishedListener listener : this.agentFinishedListeners){
            listener.agentFinished(event);
        }
    }
    //endregion
}
