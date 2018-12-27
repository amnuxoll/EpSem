package framework;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuite implements IGoalListener {
    //region Class Variables
    private TestSuiteConfiguration configuration;
    private IEnvironmentDescriptionProvider environmentDescriptionProvider;
    private IAgentProvider[] agentProviders;
    private HashMap<String, IResultWriter> resultWriters;
    //endregion

    //region Constructors
    public TestSuite(TestSuiteConfiguration configuration, IEnvironmentDescriptionProvider environmentDescriptionProvider, IAgentProvider[] agentProviders) {
        if (configuration == null)
            throw new IllegalArgumentException("configuration cannot be null.");
        if (environmentDescriptionProvider == null)
            throw new IllegalArgumentException("environmentDescriptionProvider cannot be null.");
        if (agentProviders == null)
            throw new IllegalArgumentException("agentProviders cannot be null.");
        if (agentProviders.length == 0)
            throw new IllegalArgumentException("agentProviders cannot be empty.");
        this.configuration = configuration;
        this.environmentDescriptionProvider = environmentDescriptionProvider;
        this.agentProviders = agentProviders;
    }
    //endregion

    //region Public Methods
    /**
     * Executes the test suite and writes all result data to the provided {@code resultWriterProvider}.
     *
     * @param resultWriterProvider The {@link IResultWriterProvider} used to generate {@link IResultWriter}s.
     * @throws Exception
     */
    public void run(IResultWriterProvider resultWriterProvider) {
        try {
            NamedOutput namedOutput = NamedOutput.getInstance();
            namedOutput.write("framework", "Beginning test suite...");
            this.writeMetaData();

            int numberOfIterations = this.configuration.getNumberOfIterations();
            for (int i = 0; i < this.agentProviders.length; i++) {
                IAgentProvider agentProvider = this.agentProviders[i];
                namedOutput.write("framework", "Beginning agent: " + agentProvider.getAlias() + " " + i);
                for (int j = 0; j < numberOfIterations; j++) {
                    namedOutput.write("framework", "");
                    namedOutput.write("framework", "Beginning iteration: " + j);
                    IAgent agent = agentProvider.getAgent();
                    if (j == 0)
                        this.generateResultWriters(resultWriterProvider, agentProvider.getAlias(), j, agent.getResultTypes());
                    IEnvironmentDescription environmentDescription = this.environmentDescriptionProvider.getEnvironmentDescription();
                    TestRun testRun = new TestRun(agent, environmentDescription, this.configuration.getNumberOfGoals());
                    testRun.addGoalListener(this);
                    this.beginAgentTestRun();
                    testRun.execute();
                }
                this.completeAgentTestRuns();
            }
        } catch(Exception ex) {
            NamedOutput.getInstance().write("framework", ex);
        }
    }
    //endregion

    //region IGoalListener Members
    /**
     * Callback for receiving a goal {@link GoalEvent}.
     * @param event The event to receive.
     */
    @Override
    public void goalReceived(GoalEvent event) throws IOException {
        this.resultWriters.get("steps").logResult(event.getStepCountToGoal());
        for (Map.Entry<String, String> result : event.getAgentResults().entrySet())
        {
            this.resultWriters.get(result.getKey()).logResult(result.getValue());
        }
    }
    //endregion

    //region Private Methods
    private void beginAgentTestRun() throws IOException {
        for (IResultWriter writer : this.resultWriters.values())
        {
            writer.beginNewRun();
        }
    }

    private void completeAgentTestRuns() throws IOException {
        for (IResultWriter writer : this.resultWriters.values())
        {
            writer.complete();
        }
        this.resultWriters.clear();
    }

    private void generateResultWriters(IResultWriterProvider resultWriterProvider, String agentAlias, int agentIndex, String[] resultTypes) throws Exception {
        this.resultWriters = new HashMap<>();
        this.resultWriters.put("steps", resultWriterProvider.getResultWriter(agentAlias, "agent_" + agentAlias + "_" + agentIndex + "_steps"));
        for (String resultType : resultTypes)
        {
            this.resultWriters.put(resultType, resultWriterProvider.getResultWriter(agentAlias, "agent_" + agentAlias + "_" + agentIndex + "_" + resultType));
        }
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
    //endregion
}
