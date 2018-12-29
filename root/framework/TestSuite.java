package framework;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuite implements IGoalListener {
    //region Class Variables
    private TestSuiteConfiguration configuration;
    private IEnvironmentDescriptionProvider[] environmentDescriptionProviders;
    private IAgentProvider[] agentProviders;
    private HashMap<String, IResultWriter> resultWriters;
    private Consumer<File> beforeRun;
    //endregion

    //region Constructors
    public TestSuite(TestSuiteConfiguration configuration, IEnvironmentDescriptionProvider[] environmentDescriptionProviders, IAgentProvider[] agentProviders) {
        this(configuration, environmentDescriptionProviders, agentProviders, file -> { });
    }

    public TestSuite(TestSuiteConfiguration configuration, IEnvironmentDescriptionProvider[] environmentDescriptionProviders, IAgentProvider[] agentProviders, Consumer<File> beforeRun) {
        if (configuration == null)
            throw new IllegalArgumentException("configuration cannot be null.");
        if (environmentDescriptionProviders == null)
            throw new IllegalArgumentException("environmentDescriptionProviders cannot be null.");
        if (environmentDescriptionProviders.length == 0)
            throw new IllegalArgumentException("environmentDescriptionProviders cannot be empty.");
        if (agentProviders == null)
            throw new IllegalArgumentException("agentProviders cannot be null.");
        if (agentProviders.length == 0)
            throw new IllegalArgumentException("agentProviders cannot be empty.");
        this.configuration = configuration;
        this.environmentDescriptionProviders = environmentDescriptionProviders;
        this.agentProviders = agentProviders;
        this.beforeRun = beforeRun;
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
            this.beforeRun.accept(resultWriterProvider.getOutputDirectory());
            NamedOutput namedOutput = NamedOutput.getInstance();
            namedOutput.write("framework", "Beginning test suite...\n");
            this.writeMetaData();

            int numberOfIterations = this.configuration.getNumberOfIterations();
            for (int environmentIndex = 0; environmentIndex < this.environmentDescriptionProviders.length; environmentIndex++) {
                for (int agentIndex = 0; agentIndex < this.agentProviders.length; agentIndex++) {
                    IAgentProvider agentProvider = this.agentProviders[agentIndex];
                    namedOutput.write("framework", "Beginning agent: " + agentProvider.getAlias() + " " + agentIndex + "\n");
                    for (int numberOfMachines = 0; numberOfMachines < numberOfIterations; numberOfMachines++) {
                        namedOutput.write("framework", "\n");
                        namedOutput.write("framework", "Beginning iteration: " + numberOfMachines + "\n");
                        IAgent agent = agentProvider.getAgent();
                        IEnvironmentDescription environmentDescription = this.environmentDescriptionProviders[environmentIndex].getEnvironmentDescription();
                        if (numberOfMachines == 0)
                            this.generateResultWriters(resultWriterProvider, this.environmentDescriptionProviders[environmentIndex].getAlias(), environmentIndex, agentProvider.getAlias(), agentIndex, agent.getStatisticTypes());
                        TestRun testRun = new TestRun(agent, environmentDescription, this.configuration.getNumberOfGoals());
                        testRun.addGoalListener(this);
                        this.beginAgentTestRun();
                        testRun.execute();
                    }
                    this.completeAgentTestRuns();
                }
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
        for (Datum datum : event.getAgentData())
        {
            this.resultWriters.get(datum.getStatistic()).logResult(datum.getDatum());
        }
    }
    //endregion

    //region Private Methods
    private void beginAgentTestRun() throws IOException {
        for (IResultWriter writer : this.resultWriters.values()) {
            writer.beginNewRun();
        }
    }

    private void completeAgentTestRuns() throws IOException {
        for (IResultWriter writer : this.resultWriters.values()) {
            writer.complete();
        }
        this.resultWriters.clear();
    }

    private void generateResultWriters(IResultWriterProvider resultWriterProvider, String environmentAlias, int environmentIndex, String agentAlias, int agentIndex, String[] resultTypes) throws Exception {
        Function<String, String> generateFileName = name -> "env_" + environmentAlias + "_" + environmentIndex +  "_agent_" + agentAlias + "_" + agentIndex + "_" + name;
        this.resultWriters = new HashMap<>();
        this.resultWriters.put("steps", resultWriterProvider.getResultWriter(agentAlias, generateFileName.apply("steps")));
        for (String resultType : resultTypes) {
            this.resultWriters.put(resultType, resultWriterProvider.getResultWriter(agentAlias, generateFileName.apply(resultType)));
        }
    }

    private void writeMetaData(){
        StringBuilder metadataBuilder = new StringBuilder();
        metadataBuilder.append("== CONFIGURATION ==\n");
        metadataBuilder.append("Goal Count: " + configuration.getNumberOfGoals() + "\n");
        metadataBuilder.append("Number of Machines: " + configuration.getNumberOfIterations() + "\n");
        metadataBuilder.append("\n");
        metadataBuilder.append("== ENVIRONMENTS ==\n");
        for (IEnvironmentDescriptionProvider environmentDescriptionProvider : this.environmentDescriptionProviders) {
            metadataBuilder.append("Environment provider type: " + environmentDescriptionProvider.getClass().getName() + "\n");
            metadataBuilder.append("With Alias: " + environmentDescriptionProvider.getAlias() + "\n");
        }
        metadataBuilder.append("\n");
        metadataBuilder.append("== AGENTS ==\n");
        for (IAgentProvider agentProvider : this.agentProviders) {
            metadataBuilder.append("Agent provider type: " + agentProvider.getClass().getName() + "\n");
            metadataBuilder.append("With Alias: " + agentProvider.getAlias() + "\n");
        }
        NamedOutput.getInstance().write("metadata", metadataBuilder.toString());
    }
    //endregion
}
