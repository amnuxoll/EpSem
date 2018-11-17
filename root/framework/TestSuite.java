package framework;

import java.io.*;
import java.nio.file.Paths;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuite implements IGoalListener, IEnvironmentListener {

    private TestSuiteConfiguration configuration;
    private IResultWriterProvider resultWriterProvider;
    private IEnvironmentDescriptionProvider environmentDescriptionProvider;
    private IAgentProvider[] agentProviders;

    private IResultWriter currentStepResultWriter;
    private IResultWriter currentDecisionResultWriter;

    public TestSuite(TestSuiteConfiguration configuration, IResultWriterProvider resultWriterProvider, IEnvironmentDescriptionProvider environmentDescriptionProvider, IAgentProvider[] agentProviders) {
        if (configuration == null)
            throw new IllegalArgumentException("configuration cannot be null.");
        if (resultWriterProvider == null)
            throw new IllegalArgumentException("resultWriterProvider cannot be null");
        if (environmentDescriptionProvider == null)
            throw new IllegalArgumentException("environmentDescriptionProvider cannot be null.");
        if (agentProviders == null)
            throw new IllegalArgumentException("agentProviders cannot be null.");
        if (agentProviders.length == 0)
            throw new IllegalArgumentException("agentProviders cannot be empty.");
        this.configuration = configuration;
        this.resultWriterProvider = resultWriterProvider;
        this.environmentDescriptionProvider = environmentDescriptionProvider;
        this.agentProviders = agentProviders;
    }

    public void run() throws Exception {
        System.out.println("Beginning test suite...");
        writeMetaData(resultWriterProvider.getOutputDirectory());

        int numberOfIterations = this.configuration.getNumberOfIterations();
        for (int i = 0; i < this.agentProviders.length; i++) {
            IAgentProvider agentProvider = this.agentProviders[i];
            System.out.println("Beginning agent: " + agentProvider.getAlias() + " " + i);
            this.currentStepResultWriter = this.resultWriterProvider.getResultWriter("agent_" + agentProvider.getAlias() + "_" + i + "_steps");
            this.currentDecisionResultWriter = this.resultWriterProvider.getResultWriter("agent_" + agentProvider.getAlias() + "_" + i + "_decisions");

            this.runAgent(agentProvider, numberOfIterations);
        }
    }

    private void runAgent(IAgentProvider agentProvider, int numberOfIterations) {
        for (int i = 0; i < numberOfIterations; i++) {
            System.out.println();
            System.out.println("Beginning iteration: " + i);
            IAgent agent = agentProvider.getAgent();
            IEnvironmentDescription environmentDescription = this.environmentDescriptionProvider.getEnvironmentDescription();
            TestRun testRun = new TestRun(agent, environmentDescription, this.configuration.getNumberOfGoals());
            testRun.addGoalListener(this);
            this.currentStepResultWriter.beginNewRun();
            this.currentDecisionResultWriter.beginNewRun();
            testRun.execute();

            OutputStreamContainer osc = Services.retrieve(OutputStreamContainer.class);
            if (osc != null) {
                osc.write("ratioOutputStream", "\n");
                osc.write("agentDidAGood", "\n");
                osc.write("agentDidAGoodOverall", "\n");
                osc.write("goodDecisionBail", "\n");
                osc.write("badDecisionBail", "\n");
                osc.write("properBails", "\n");
                osc.write("junoRatios", "\n");
            }
        }
        this.currentStepResultWriter.complete();
        this.currentDecisionResultWriter.complete();
    }

    @Override
    public void goalReceived(GoalEvent event) {
        this.currentStepResultWriter.logStepsToGoal(event.getStepCountToGoal());
        this.currentDecisionResultWriter.logStepsToGoal(event.getDecisionCountToGoal());
    }

    private void writeMetaData(String parentDirectory){
        OutputStreamContainer osc= Services.retrieve(OutputStreamContainer.class);
        if(osc == null){
            return;
        }
        
        //information about configuration:
        osc.write("metaData", configuration.getNumberOfGoals() + " goals on " +
                    configuration.getNumberOfIterations() + " environments\n");

        osc.write("metaData", "\n");

        //information about environment provider
        osc.write("metaData", "Environment provider type: " + environmentDescriptionProvider.getClass().getName() + "\n");

        osc.write("metaData", "\n");

        //information about environment
        IEnvironmentDescription environment= environmentDescriptionProvider.getEnvironmentDescription();
        osc.write("metaData", "Example of possible environment description:" + "\n");
        osc.write("metaData", "\tType: " + environment.getClass().getName() + "\n");
        osc.write("metaData", "\tNumber of moves: " + environment.getMoves().length + "\n");
        osc.write("metaData", "\tNumber of states: " + environment.getNumStates() + "\n");

        osc.write("metaData",  "\n");

        //information about agent provider
        for(int i=0; i < agentProviders.length; i++){
            osc.write("metaData", "Agent provider " + i + " type: " + agentProviders[i].getClass().getName() + "\n");
            IAgent agent= agentProviders[i].getAgent();
            osc.write("metaData", "Example of possible agent:" + "\n");
            osc.write("metaData", "\tType: " + agent.getClass().getName() + "\n");
            osc.write("metaData", "\tExtra Data:\n\t\t" + agent.getMetaData() + "\n");
            osc.write("metaData", "\n");
        }
    }

    @Override
    public void receiveEvent(EnvironmentEvent event) {
        if(event.getEventType() == EnvironmentEvent.EventType.DATA_BREAK){
            this.currentStepResultWriter.logStepsToGoal(0);
        }
    }

    public IResultWriterProvider getResultWriterProvider() {
        return resultWriterProvider;
    }
}
