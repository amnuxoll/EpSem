package framework;

import java.util.ArrayList;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuite implements IGoalListener {

    private TestSuiteConfiguration configuration;
    private IResultWriterProvider resultWriterProvider;
    private IEnvironmentDescriptionProvider environmentDescriptionProvider;
    private IAgentProvider[] agentProviders;

    private IResultWriter currentStepResultWriter;

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
        writeMetaData();

        int numberOfIterations = this.configuration.getNumberOfIterations();
        for (int i = 0; i < this.agentProviders.length; i++) {
            IAgentProvider agentProvider = this.agentProviders[i];
            System.out.println("Beginning agent: " + agentProvider.getAlias() + " " + i);
            this.currentStepResultWriter = this.resultWriterProvider.getResultWriter(agentProvider.getAlias(), "agent_" + agentProvider.getAlias() + "_" + i + "_steps");
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
            testRun.execute();
        }
        this.currentStepResultWriter.complete();
    }

    @Override
    public void goalReceived(GoalEvent event) {
        this.currentStepResultWriter.logStepsToGoal(event.getStepCountToGoal());
    }

    private void writeMetaData(){
        StringBuilder metadataBuilder = new StringBuilder();
        metadataBuilder.append(configuration.getNumberOfGoals() + " goals on " + configuration.getNumberOfIterations() + " environments\n");
        metadataBuilder.append("\n");
        metadataBuilder.append("Environment provider type: " + environmentDescriptionProvider.getClass().getName() + "\n");
        metadataBuilder.append("\n");
        IEnvironmentDescription environment= environmentDescriptionProvider.getEnvironmentDescription();
        metadataBuilder.append("Example of possible environment description:" + "\n");
        metadataBuilder.append("\tType: " + environment.getClass().getName() + "\n");
        metadataBuilder.append("\tNumber of moves: " + environment.getMoves().length + "\n");
        metadataBuilder.append("\tNumber of states: " + environment.getNumStates() + "\n");
        metadataBuilder.append("\n");
        //information about agent provider
        for(int i=0; i < agentProviders.length; i++){
            metadataBuilder.append("Agent provider " + i + " type: " + agentProviders[i].getClass().getName() + "\n");
            IAgent agent= agentProviders[i].getAgent();
            metadataBuilder.append("Example of possible agent:" + "\n");
            metadataBuilder.append("\tType: " + agent.getClass().getName() + "\n");
            metadataBuilder.append("\tExtra Data:\n\t\t" + agent.getMetaData() + "\n");
            metadataBuilder.append("\n");
        }
        NamedOutput.getInstance().write("metaData", metadataBuilder.toString());
    }

    public IResultWriterProvider getResultWriterProvider() {
        return resultWriterProvider;
    }
}
