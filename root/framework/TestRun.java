package framework;


import agents.juno.JunoAgent;
import agents.marz.MaRzAgent;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
class TestRun implements IAgentListener {

    private IAgent agent;
    private IEnvironmentDescription environmentDescription;
    private Environment environment;
    private int numberOfGoalsToFind;

    private int decisionCount= 0;
    private int goodDecisionCount= 0;
    private int goodDecisionBailCount= 0;
    private int badDecisionBailCount= 0;
    private int stepsSinceDecision= 0;
    private boolean currSequenceGood;

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
        agent.addAgentListener(this);
        this.numberOfGoalsToFind = numberOfGoalsToFind;
    }

    public void execute() {
        try {
            int goalCount = 0;
            int moveCount = 0;
            this.agent.initialize(this.environmentDescription.getMoves());
            SensorData sensorData = null;
            do {
                Move move = this.agent.getNextMove(sensorData);
                sensorData = environment.tick(move);
                //System.out.print(move + " -> " + sensorData + "; ");
                moveCount++;
                stepsSinceDecision++;

                if (sensorData.isGoal()) {
                    this.fireGoalEvent(moveCount);
                    goalCount++;
                    moveCount = 0;

                    environment.reset();
                }
            } while (goalCount < this.numberOfGoalsToFind);
        } catch (Exception ex) {
            System.out.println("TestRun failed with exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public synchronized void addGoalListener(IGoalListener listener) {
        this.goalListeners.add(listener);
    }

    public synchronized void removeGoalListener(IGoalListener listener) {
        this.goalListeners.remove(listener);
    }

    private synchronized void fireGoalEvent(int stepsToGoal) {
        GoalEvent goal = new GoalEvent(this, stepsToGoal, decisionCount);
        for (IGoalListener listener : this.goalListeners) {
            listener.goalReceived(goal);
        }

        writeGoalData();
    }

    private void writeGoalData(){
        OutputStreamContainer out= Services.retrieve(OutputStreamContainer.class);

        String data= decisionCount > 0 ?
                Double.toString((double)goodDecisionCount/decisionCount) : "";
        out.write("agentDidAGood", data + ",");

        data= decisionCount > 0 ?
                Double.toString((double)goodDecisionBailCount/goodDecisionCount) : "";
        out.write("goodDecisionBail", data +",");

        data= decisionCount > 0 ?
                Double.toString((double)badDecisionBailCount/(decisionCount-goodDecisionCount)) : "";
        out.write("badDecisionBail", data +",");
    }

    @Override
    public void receiveEvent(AgentEvent ae) {
        switch(ae.getType()){
            case DECISION_MADE:
                currSequenceGood =
                        environmentDescription.validateSequence(environment.getCurrentState(), ae.getChosenSequence());

                decisionCount++;
                stepsSinceDecision= 0;

                if (currSequenceGood) {
                    goodDecisionCount++;
                }

                break;
            case BAILED:
                if(currSequenceGood){
                    goodDecisionBailCount++;
                }
                else{
                    badDecisionBailCount++;
                }

                break;
        }
       if(ae.getType()==AgentEvent.EventType.DECISION_MADE) {
           currSequenceGood = environmentDescription.validateSequence(environment.getCurrentState(), ae.getChosenSequence());

           decisionCount++;

           if (currSequenceGood) {
               goodDecisionCount++;
           }
       }
    }
}
