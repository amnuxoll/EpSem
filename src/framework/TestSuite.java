package framework;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A {@link TestSuite} allows for the definition of multiple agents and multiple environments and will run
 * the cross-product of the sets to optimally and with simplicity allow the gathering of a lot of data in a
 * single run of the engine.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuite implements IGoalListener {

    //region Class Variables

    /** The {@link TestSuiteConfiguration} to use for this suite. */
    private TestSuiteConfiguration configuration;

    /** The collection of {@link IEnvironmentProvider} to use for creating {@link TestRun}. */
    private IEnvironmentProvider[] environmentProviders;

    /** The collection of {@link IAgentProvider} to use for creating {@link TestRun}. */
    private IAgentProvider[] agentProviders;

    /** The map of {@link IResultWriter} to use for logging statistical data during test runs. */
    private HashMap<String, IResultWriter> resultWriters;

    /** A delegate to invoke prior to executing any test runs. */
    private Consumer<File> beforeRun;

    //endregion

    //region Constructors

    /**
     * Creates an instance of a {@link TestSuite}.
     *
     * @param configuration the {@link TestSuiteConfiguration} to define the tests being run.
     * @param environmentProviders the {@link IEnvironmentProvider} set to use for test runs.
     * @param agentProviders the {@link IAgentProvider} set to use for test runs.
     */
    public TestSuite(TestSuiteConfiguration configuration, IEnvironmentProvider[] environmentProviders, IAgentProvider[] agentProviders) {
        this(configuration, environmentProviders, agentProviders, rootDirectory -> { });
    }

    /**
     * Creates an instance of a {@link TestSuite}.
     *
     * @param configuration the {@link TestSuiteConfiguration} to define the tests being run.
     * @param environmentProviders the {@link IEnvironmentProvider} set to use for test runs.
     * @param agentProviders the {@link IAgentProvider} set to use for test runs.
     * @param beforeRun a delegate to invoke prior to executing any test runs. It should receive a directory where
     *                  output will be written.
     */
    public TestSuite(TestSuiteConfiguration configuration, IEnvironmentProvider[] environmentProviders, IAgentProvider[] agentProviders, Consumer<File> beforeRun) {
        if (configuration == null)
            throw new IllegalArgumentException("configuration cannot be null.");
        if (environmentProviders == null)
            throw new IllegalArgumentException("environmentProviders cannot be null.");
        if (environmentProviders.length == 0)
            throw new IllegalArgumentException("environmentProviders cannot be empty.");
        if (agentProviders == null)
            throw new IllegalArgumentException("agentProviders cannot be null.");
        if (agentProviders.length == 0)
            throw new IllegalArgumentException("agentProviders cannot be empty.");
        if (beforeRun == null)
            throw new IllegalArgumentException("beforeRun cannot be null");
        this.configuration = configuration;
        this.environmentProviders = environmentProviders;
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
            namedOutput.writeLine("framework", "Beginning test suite...");
            this.writeMetaData();

            int numberOfIterations = this.configuration.getNumberOfIterations();
            for (int environmentIndex = 0; environmentIndex < this.environmentProviders.length; environmentIndex++) {
                for (int agentIndex = 0; agentIndex < this.agentProviders.length; agentIndex++) {
                    IAgentProvider agentProvider = this.agentProviders[agentIndex];
                    namedOutput.writeLine("framework", "Beginning agent: " + agentProvider.getAlias() + " " + agentIndex);
                    for (int numberOfMachines = 0; numberOfMachines < numberOfIterations; numberOfMachines++) {
                        namedOutput.writeLine("framework");
                        namedOutput.writeLine("framework", "Beginning iteration: " + numberOfMachines);
                        IAgent agent = agentProvider.getAgent();
                        IEnvironment environment = this.environmentProviders[environmentIndex].getEnvironment();
                        if (numberOfMachines == 0)
                            this.generateResultWriters(resultWriterProvider, this.environmentProviders[environmentIndex].getAlias(), environmentIndex, agentProvider.getAlias(), agentIndex, agent.getStatisticTypes());
                        TestRun testRun = new TestRun(agent, environment, this.configuration.getNumberOfGoals());
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
        for (Datum datum : event.getAgentData()) {
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
        int i = 0;
        for (IEnvironmentProvider environmentDescriptionProvider : this.environmentProviders) {
            metadataBuilder.append("Environment: " + i++ + "\n");
            metadataBuilder.append("\tprovider type: " + environmentDescriptionProvider.getClass().getName() + "\n");
            metadataBuilder.append("\tWith Alias: " + environmentDescriptionProvider.getAlias() + "\n");
        }
        metadataBuilder.append("\n");
        metadataBuilder.append("== AGENTS ==\n");
        i = 0;
        for (IAgentProvider agentProvider : this.agentProviders) {
            metadataBuilder.append("Agent: " + i++ + "\n");
            metadataBuilder.append("\tAgent provider type: " + agentProvider.getClass().getName() + "\n");
            metadataBuilder.append("\tWith Alias: " + agentProvider.getAlias() + "\n");
        }
        NamedOutput.getInstance().writeLine("metadata", metadataBuilder.toString());
    }

    //endregion
}
