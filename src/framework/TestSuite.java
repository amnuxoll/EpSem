package framework;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A {@link TestSuite} allows for the definition of multiple agents and multiple environments and will run
 * the cross-product of the sets to optimally and with simplicity allow the gathering of a lot of data in a
 * single run of the engine.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuite {

    //region Class Variables

    /** The {@link TestSuiteConfiguration} to use for this suite. */
    private TestSuiteConfiguration configuration;

    /** The collection of {@link IEnvironmentProvider} to use for creating {@link TestRun}. */
    private IEnvironmentProvider[] environmentProviders;

    /** The collection of {@link IAgentProvider} to use for creating {@link TestRun}. */
    private IAgentProvider[] agentProviders;

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
        this.configuration = configuration;
        this.environmentProviders = environmentProviders;
        this.agentProviders = agentProviders;
    }

    //endregion

    //region Public Methods

    /**
     * Executes the test suite and writes all result data to the provided {@code resultWriterProvider}.
     *
     * @param resultCompiler The {@link IResultCompiler} used to manage test results.
     * @throws Exception
     */
    public void run(IResultCompiler resultCompiler) {
        try {
            NamedOutput namedOutput = NamedOutput.getInstance();
            namedOutput.writeLine("framework", "Beginning test suite...");
            this.writeMetaData();

            resultCompiler.configureOutputs(this.configuration.getNumberOfGoals(), this.configuration.getNumberOfIterations());

            for (int agentId = 0; agentId < this.agentProviders.length; agentId++) {
                IAgentProvider provider = this.agentProviders[agentId];
                resultCompiler.registerAgent(agentId, provider.getAlias(), provider.getAgent().getStatisticTypes());
            }
            for (int environmentId = 0; environmentId < this.environmentProviders.length; environmentId++) {
                resultCompiler.registerEnvironment(environmentId, this.environmentProviders[environmentId].getAlias());
            }
            resultCompiler.build();

            Instant start = Instant.now();
            int timeout = this.configuration.getTimeout();
            if (timeout > 0)
                this.runMultiThreaded(resultCompiler, timeout);
            else
                this.runSingleThreaded(resultCompiler);
            Instant finish = Instant.now();
            this.logDurationInMetadata(Duration.between(start, finish));

            resultCompiler.complete();
        } catch(Exception ex) {
            NamedOutput.getInstance().write("framework", ex);
        }
    }

    private void runSingleThreaded(IResultCompiler resultCompiler) {
        int numberOfIterations = this.configuration.getNumberOfIterations();
        for (int iteration = 0; iteration < numberOfIterations; iteration++) {
            for (int environmentId = 0; environmentId < this.environmentProviders.length; environmentId++) {
                IEnvironment environment = this.environmentProviders[environmentId].getEnvironment();
                for (int agentId = 0; agentId < this.agentProviders.length; agentId++) {
                    IAgent agent = this.agentProviders[agentId].getAgent();
                    TestRun testRun = new TestRun(agent, environment.copy(), this.configuration.getNumberOfGoals());

                    // Java is annoying
                    int finalEnvironmentId = environmentId;
                    int finalIteration = iteration;
                    int finalAgentId = agentId;
                    testRun.addGoalListener(goalEvent -> resultCompiler.logResult(finalIteration, finalAgentId, finalEnvironmentId, goalEvent.getGoalNumber(), goalEvent.getAgentData()));
                    testRun.run();
                }
            }
        }
    }

    private void runMultiThreaded(IResultCompiler resultCompiler, int timeout) throws InterruptedException {
        ExecutorService service = Executors.newCachedThreadPool();
        int numberOfIterations = this.configuration.getNumberOfIterations();
        for (int iteration = 0; iteration < numberOfIterations; iteration++) {
            for (int environmentId = 0; environmentId < this.environmentProviders.length; environmentId++) {
                IEnvironment environment = this.environmentProviders[environmentId].getEnvironment();
                for (int agentId = 0; agentId < this.agentProviders.length; agentId++) {
                    IAgent agent = this.agentProviders[agentId].getAgent();
                    TestRun testRun = new TestRun(agent, environment.copy(), this.configuration.getNumberOfGoals());

                    // Java is annoying
                    int finalEnvironmentId = environmentId;
                    int finalIteration = iteration;
                    int finalAgentId = agentId;
                    testRun.addGoalListener(goalEvent -> resultCompiler.logResult(finalIteration, finalAgentId, finalEnvironmentId, goalEvent.getGoalNumber(), goalEvent.getAgentData()));
                    service.execute(testRun);
                }
            }
        }
        service.shutdown();
        service.awaitTermination(timeout, TimeUnit.HOURS);
    }

    //endregion

    //region Private Methods

    private void writeMetaData(){
        StringBuilder metadataBuilder = new StringBuilder();
        metadataBuilder.append("== CONFIGURATION ==\n");
        metadataBuilder.append("Goal Count: " + configuration.getNumberOfGoals() + "\n");
        metadataBuilder.append("Number of Machines: " + configuration.getNumberOfIterations() + "\n");
        metadataBuilder.append("Timeout: " + configuration.getTimeout() + "\n");
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

    private void logDurationInMetadata(Duration duration) {
        StringBuilder metadataBuilder = new StringBuilder();
        metadataBuilder.append("== DURATION ==\n");
        metadataBuilder.append("Start: " + duration.toMillis() + "\n");
        NamedOutput.getInstance().writeLine("metadata", metadataBuilder.toString());
    }

    //endregion
}
