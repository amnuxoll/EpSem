package framework;

import org.junit.jupiter.api.Test;
import utils.Sequence;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSuiteTest {

    private TestSuiteConfiguration testConfiguration = new TestSuiteConfiguration(2, 3);

    // constructor Tests
    @Test
    public void constructorNullTestSuiteConfigurationThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(null, new TestResultWriterProvider(), new TestEnvironmentDescriptionProvider(), new IAgentProvider[] { new TestAgentProvider() }));
    }

    @Test
    public void constructorNullResultWriterProviderThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, null, new TestEnvironmentDescriptionProvider(), new IAgentProvider[] { new TestAgentProvider() }));
    }

    @Test
    public void constructorNullEnvironmentDescriptionProviderThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, new TestResultWriterProvider(), null, new IAgentProvider[] { new TestAgentProvider() }));
    }

    @Test
    public void constructorNullAgentProvidersThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, new TestResultWriterProvider(), new TestEnvironmentDescriptionProvider(), null));
    }

    @Test
    public void constructorEmptyAgentProvidersThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, new TestResultWriterProvider(), new TestEnvironmentDescriptionProvider(), new IAgentProvider[0]));
    }

    // run Tests
    @Test
    public void runInitializesAndExecutesSingleAgent() throws Exception {
        Services.register(IRandomizer.class, new Randomizer());
        TestResultWriterProvider resultWriterProvider = new TestResultWriterProvider();
        TestEnvironmentDescriptionProvider environmentDescriptionProvider = new TestEnvironmentDescriptionProvider();
        IAgentProvider[] agentProviders = new IAgentProvider[] {
                        new TestAgentProvider()
                };
        TestSuite testSuite = new TestSuite(this.testConfiguration, resultWriterProvider, environmentDescriptionProvider, agentProviders);
        testSuite.run();

        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[0]).generatedAgents);
        assertEquals(this.testConfiguration.getNumberOfIterations(), environmentDescriptionProvider.generatedEnvironmentDescriptions);

        assertEquals(1, resultWriterProvider.generatedResultWriters.size());
        TestResultWriter resultWriter = resultWriterProvider.generatedResultWriters.get("agent0");
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
        TestSuite testSuite = new TestSuite(this.testConfiguration, resultWriterProvider, environmentDescriptionProvider, agentProviders);
        testSuite.run();

        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[0]).generatedAgents);
        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[1]).generatedAgents);
        assertEquals(2 * this.testConfiguration.getNumberOfIterations(), environmentDescriptionProvider.generatedEnvironmentDescriptions);

        assertEquals(2, resultWriterProvider.generatedResultWriters.size());
        TestResultWriter resultWriter = resultWriterProvider.generatedResultWriters.get("agent0");
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCount);
        assertEquals(this.testConfiguration.getNumberOfIterations() * this.testConfiguration.getNumberOfGoals(), resultWriter.stepsLoggedCount);
        assertTrue(resultWriter.completed);
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCountAtcompleted);

        resultWriter = resultWriterProvider.generatedResultWriters.get("agent1");
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
    }

    private class TestAgent implements IAgent {

        @Override
        public void initialize(Move[] moves) {

        }

        @Override
        public Move getNextMove(SensorData sensorData) throws Exception {
            return new Move("a");
        }

        @Override
        public void addAgentListener(IAgentListener listener) {

        }
    }

    private class TestEnvironmentDescriptionProvider implements IEnvironmentDescriptionProvider {
        public int generatedEnvironmentDescriptions = 0;

        @Override
        public IEnvironmentDescription getEnvironmentDescription() {
            this.generatedEnvironmentDescriptions++;
            return new TestEnvironmentDescription();
        }
    }

    private class TestEnvironmentDescription implements IEnvironmentDescription {

        @Override
        public Move[] getMoves() {
            return new Move[] { new Move("a") };
        }

        @Override
        public int transition(int currentState, Move move) {
            return 0;
        }

        @Override
        public boolean isGoalState(int state) {
            return true;
        }

        @Override
        public int getNumGoalStates() {
            return 1;
        }

        @Override
        public int getNumStates() {
            return 1;
        }

        @Override
        public void applySensors(int lastState, Move move, int currentState, SensorData sensorData) {

        }

        @Override
        public void addEnvironmentListener(IEnvironmentListener listener) {

        }

        @Override
        public boolean validateSequence(int state, Sequence sequence) {
            return false;
        }
    }

    private class TestResultWriterProvider implements IResultWriterProvider {
        public HashMap<String, TestResultWriter> generatedResultWriters = new HashMap<>();

        @Override
        public IResultWriter getResultWriter(String agent) {
            TestResultWriter resultWriter = new TestResultWriter();
            this.generatedResultWriters.put(agent, resultWriter);
            return resultWriter;
        }

        @Override
        public String getOutputDirectory() {
            return null;
        }
    }

    private class TestResultWriter implements IResultWriter {
        public int iterationCount = 0;
        public int stepsLoggedCount = 0;
        public boolean completed = false;
        public int iterationCountAtcompleted = 0;

        @Override
        public void logStepsToGoal(int stepsToGoal) {
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
