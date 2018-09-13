package framework;

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

    private IResultWriter currentResultWriter;

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
        int numberOfIterations = this.configuration.getNumberOfIterations();
        for (int i = 0; i < this.agentProviders.length; i++) {
            System.out.println("Beginning agent: " + i);
            this.currentResultWriter = this.resultWriterProvider.getResultWriter("agent" + i);
            IAgentProvider agentProvider = this.agentProviders[i];
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
            this.currentResultWriter.beginNewRun();
            testRun.execute();
        }
        this.currentResultWriter.complete();
    }

    @Override
    public void goalReceived(GoalEvent event) {
        this.currentResultWriter.logStepsToGoal(event.getStepCountToGoal());
    }
}
