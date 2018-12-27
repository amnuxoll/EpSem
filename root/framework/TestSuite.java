package framework;

import java.util.HashMap;
import java.util.Map;

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
    private HashMap<String, IResultWriter> resultWriters;

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
        NamedOutput namedOutput = NamedOutput.getInstance();
        namedOutput.write("framework", "Beginning test suite...");
        writeMetaData();

        int numberOfIterations = this.configuration.getNumberOfIterations();
        for (int i = 0; i < this.agentProviders.length; i++) {
            IAgentProvider agentProvider = this.agentProviders[i];
            namedOutput.write("framework", "Beginning agent: " + agentProvider.getAlias() + " " + i);
            for (int j = 0; j < numberOfIterations; j++) {
                namedOutput.write("framework", "");
                namedOutput.write("framework", "Beginning iteration: " + j);
                IAgent agent = agentProvider.getAgent();
                if (j == 0)
                    this.generateResultWriters(agentProvider.getAlias(), j, agent.getResultTypes());
                IEnvironmentDescription environmentDescription = this.environmentDescriptionProvider.getEnvironmentDescription();
                TestRun testRun = new TestRun(agent, environmentDescription, this.configuration.getNumberOfGoals());
                testRun.addGoalListener(this);
                this.beginNewRuns();
                testRun.execute();
            }
            this.completeTestRun();
        }
    }

    @Override
    public void goalReceived(GoalEvent event) {
        this.resultWriters.get("steps").logResult(event.getStepCountToGoal());
        for (Map.Entry<String, String> result : event.getAgentResults().entrySet())
        {
            this.resultWriters.get(result.getKey()).logResult(result.getValue());
        }
    }

    private void beginNewRuns()
    {
        for (IResultWriter writer : this.resultWriters.values())
        {
            writer.beginNewRun();
        }
    }

    private void completeTestRun()
    {
        for (IResultWriter writer : this.resultWriters.values())
        {
            writer.complete();
        }
        this.resultWriters.clear();
    }

    private void generateResultWriters(String agentAlias, int agentIndex, String[] resultTypes) throws Exception {
        this.resultWriters = new HashMap<>();
        this.resultWriters.put("steps", this.resultWriterProvider.getResultWriter(agentAlias, "agent_" + agentAlias + "_" + agentIndex + "_steps"));
        for (String resultType : resultTypes)
        {
            this.resultWriters.put(resultType, this.resultWriterProvider.getResultWriter(agentAlias, "agent_" + agentAlias + "_" + agentIndex + "_" + resultType));
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

    public IResultWriterProvider getResultWriterProvider() {
        return resultWriterProvider;
    }
}
