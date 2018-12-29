package framework;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSuiteTest {

    private TestSuiteConfiguration testConfiguration = new TestSuiteConfiguration(2, 3);

    // constructor Tests
    @Test
    public void constructorNullTestSuiteConfigurationThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(null, new IEnvironmentDescriptionProvider[] { new TestEnvironmentDescriptionProvider() }, new IAgentProvider[] { new TestAgentProvider() }));
    }

    @Test
    public void constructorNullEnvironmentDescriptionProviderThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, null, new IAgentProvider[] { new TestAgentProvider() }));
    }

    @Test
    public void constructorNullAgentProvidersThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, new IEnvironmentDescriptionProvider[] { new TestEnvironmentDescriptionProvider() }, null));
    }

    @Test
    public void constructorEmptyAgentProvidersThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, new IEnvironmentDescriptionProvider[] { new TestEnvironmentDescriptionProvider() }, new IAgentProvider[0]));
    }

    // run Tests
    @Test
    public void runInitializesAndExecutesSingleAgent() throws Exception {
        TestResultWriterProvider resultWriterProvider = new TestResultWriterProvider();
        TestEnvironmentDescriptionProvider environmentDescriptionProvider = new TestEnvironmentDescriptionProvider();
        IAgentProvider[] agentProviders = new IAgentProvider[] {
                        new TestAgentProvider()
                };
        TestSuite testSuite = new TestSuite(this.testConfiguration, new IEnvironmentDescriptionProvider[] { environmentDescriptionProvider }, agentProviders);
        testSuite.run(resultWriterProvider);

        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[0]).generatedAgents);
        assertEquals(this.testConfiguration.getNumberOfIterations(), environmentDescriptionProvider.generatedEnvironmentDescriptions);
        assertEquals(1, resultWriterProvider.generatedResultWriters.size());
        TestResultWriter resultWriter = resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_0_steps");
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCount);
        assertEquals(this.testConfiguration.getNumberOfIterations() * this.testConfiguration.getNumberOfGoals(), resultWriter.stepsLoggedCount);
        assertTrue(resultWriter.completed);
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCountAtcompleted);
    }

    @Test
    public void runInitializesAndExecutesMultipleAgent() throws Exception {
        TestResultWriterProvider resultWriterProvider = new TestResultWriterProvider();
        TestEnvironmentDescriptionProvider environmentDescriptionProvider = new TestEnvironmentDescriptionProvider();
        IAgentProvider[] agentProviders = new IAgentProvider[] {
                        new TestAgentProvider(),
                        new TestAgentProvider()
                };
        TestSuite testSuite = new TestSuite(this.testConfiguration, new IEnvironmentDescriptionProvider[] { environmentDescriptionProvider }, agentProviders);
        testSuite.run(resultWriterProvider);

        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[0]).generatedAgents);
        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[1]).generatedAgents);
        assertEquals(2 * this.testConfiguration.getNumberOfIterations(), environmentDescriptionProvider.generatedEnvironmentDescriptions);

        assertEquals(2, resultWriterProvider.generatedResultWriters.size());
        TestResultWriter resultWriter = resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_0_steps");
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCount);
        assertEquals(this.testConfiguration.getNumberOfIterations() * this.testConfiguration.getNumberOfGoals(), resultWriter.stepsLoggedCount);
        assertTrue(resultWriter.completed);
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCountAtcompleted);

        resultWriter = resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_1_steps");
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCount);
        assertEquals(this.testConfiguration.getNumberOfIterations() * this.testConfiguration.getNumberOfGoals(), resultWriter.stepsLoggedCount);
        assertTrue(resultWriter.completed);
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCountAtcompleted);
    }


    private class TestAgentProvider implements IAgentProvider {
        public int generatedAgents = 0;

        @Override
        public IAgent getAgent() {
            this.generatedAgents++;
            return new TestAgent();
        }

        @Override
        public String getAlias() {
            return "AgAlias";
        }
    }

    private class TestAgent implements IAgent {

        @Override
        public void initialize(Move[] moves, IIntrospector introspector) {

        }

        @Override
        public Move getNextMove(SensorData sensorData) {
            return new Move("a");
        }

    }

    private class TestEnvironmentDescriptionProvider implements IEnvironmentDescriptionProvider {
        public int generatedEnvironmentDescriptions = 0;

        @Override
        public IEnvironmentDescription getEnvironmentDescription() {
            this.generatedEnvironmentDescriptions++;
            return new TestEnvironmentDescription();
        }

        @Override
        public String getAlias() {
            return "EnvAlias";
        }
    }

    private class TestEnvironmentDescription implements IEnvironmentDescription {

        @Override
        public Move[] getMoves() {
            return new Move[] { new Move("a") };
        }

        @Override
        public TransitionResult transition(int currentState, Move move) {
            return null;
        }

        @Override
        public int getRandomState() {
            return 0;
        }
    }

    private class TestResultWriterProvider implements IResultWriterProvider {
        public HashMap<String, TestResultWriter> generatedResultWriters = new HashMap<>();

        @Override
        public IResultWriter getResultWriter(String agent, String file) {
            TestResultWriter resultWriter = new TestResultWriter();
            this.generatedResultWriters.put(file, resultWriter);
            return resultWriter;
        }

        @Override
        public File getOutputDirectory() {
            return null;
        }
    }

    private class TestResultWriter implements IResultWriter {
        public int iterationCount = 0;
        public int stepsLoggedCount = 0;
        public boolean completed = false;
        public int iterationCountAtcompleted = 0;

        @Override
        public void logResult(String result) {
            this.stepsLoggedCount++;
        }

        @Override
        public void beginNewRun() {
            this.iterationCount++;
        }

        @Override
        public void complete() {
            this.completed = true;
            this.iterationCountAtcompleted = this.iterationCount;
        }
    }
}
