package tests.framework;

import framework.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class TestSuiteTest {
    //region Class Variables
    private TestSuiteConfiguration testConfiguration = new TestSuiteConfiguration(2, 3);
    //endregion

    //region constructor Tests
    @EpSemTest
    public void constructorNullTestSuiteConfigurationThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(null, new IEnvironmentProvider[] { new TestEnvironmentProvider() }, new IAgentProvider[] { new TestAgentProvider(false) }));
    }

    @EpSemTest
    public void constructorNullEnvironmentDescriptionProvidersThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, null, new IAgentProvider[] { new TestAgentProvider(false) }));
    }

    @EpSemTest
    public void constructorEmptyEnvironmentDescriptionProvidersThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, new IEnvironmentProvider[0], new IAgentProvider[] { new TestAgentProvider(false) }));
    }

    @EpSemTest
    public void constructorNullAgentProvidersThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, new IEnvironmentProvider[] { new TestEnvironmentProvider() }, null));
    }

    @EpSemTest
    public void constructorEmptyAgentProvidersThrowsException() {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, new IEnvironmentProvider[] { new TestEnvironmentProvider() }, new IAgentProvider[0]));
    }

    @EpSemTest
    public void constructorNullBeforeRunThrowsException()
    {
        assertThrows(IllegalArgumentException.class,() -> new TestSuite(this.testConfiguration, new IEnvironmentProvider[] { new TestEnvironmentProvider() }, new IAgentProvider[] { new TestAgentProvider(false) }, null));
    }
    //endregion

    //region run Tests
    @EpSemTest
    public void runInitializesAndExecutesSingleAgentSingleEnvironment() {
        IAgentProvider[] agentProviders = new IAgentProvider[] {
                        new TestAgentProvider(false)
                };
        IEnvironmentProvider[] environmentDescriptionProviders = new IEnvironmentProvider[] {
                new TestEnvironmentProvider()
        };
        TestSuite testSuite = new TestSuite(this.testConfiguration, environmentDescriptionProviders, agentProviders);
        TestResultWriterProvider resultWriterProvider = new TestResultWriterProvider();
        testSuite.run(resultWriterProvider);

        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[0]).generatedAgents);
        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestEnvironmentProvider)environmentDescriptionProviders[0]).generatedEnvironmentDescriptions);
        assertEquals(1, resultWriterProvider.generatedResultWriters.size());
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_0_steps"));
    }

    @EpSemTest
    public void runInitializesAndExecutesMultipleAgentsSingleEnvironment()  {
        IAgentProvider[] agentProviders = new IAgentProvider[] {
                        new TestAgentProvider(false),
                        new TestAgentProvider(false)
                };
        IEnvironmentProvider[] environmentDescriptionProviders = new IEnvironmentProvider[] {
                new TestEnvironmentProvider()
        };
        TestSuite testSuite = new TestSuite(this.testConfiguration, environmentDescriptionProviders, agentProviders);
        TestResultWriterProvider resultWriterProvider = new TestResultWriterProvider();
        testSuite.run(resultWriterProvider);

        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[0]).generatedAgents);
        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[1]).generatedAgents);
        assertEquals(2 * this.testConfiguration.getNumberOfIterations(), ((TestEnvironmentProvider)environmentDescriptionProviders[0]).generatedEnvironmentDescriptions);

        assertEquals(2, resultWriterProvider.generatedResultWriters.size());
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_0_steps"));
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_1_steps"));
    }

    @EpSemTest
    public void runInitializesAndExecutesSingleAgentMultipleEnvironments() {
        IAgentProvider[] agentProviders = new IAgentProvider[] {
                new TestAgentProvider(false)
        };
        IEnvironmentProvider[] environmentDescriptionProviders = new IEnvironmentProvider[] {
                new TestEnvironmentProvider(),
                new TestEnvironmentProvider()
        };
        TestSuite testSuite = new TestSuite(this.testConfiguration, environmentDescriptionProviders, agentProviders);
        TestResultWriterProvider resultWriterProvider = new TestResultWriterProvider();
        testSuite.run(resultWriterProvider);

        assertEquals(2 * this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[0]).generatedAgents);
        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestEnvironmentProvider)environmentDescriptionProviders[0]).generatedEnvironmentDescriptions);
        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestEnvironmentProvider)environmentDescriptionProviders[1]).generatedEnvironmentDescriptions);
        assertEquals(2, resultWriterProvider.generatedResultWriters.size());
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_0_steps"));
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_1_agent_AgAlias_0_steps"));
    }

    @EpSemTest
    public void runInitializesAndExecutesMultipleAgentsMultipleEnvironments() {
        IAgentProvider[] agentProviders = new IAgentProvider[] {
                new TestAgentProvider(false),
                new TestAgentProvider(false)
        };
        IEnvironmentProvider[] environmentDescriptionProviders = new IEnvironmentProvider[] {
                new TestEnvironmentProvider(),
                new TestEnvironmentProvider()
        };
        TestSuite testSuite = new TestSuite(this.testConfiguration, environmentDescriptionProviders, agentProviders);
        TestResultWriterProvider resultWriterProvider = new TestResultWriterProvider();
        testSuite.run(resultWriterProvider);

        assertEquals(2 * this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[1]).generatedAgents);
        assertEquals(2 * this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[0]).generatedAgents);
        assertEquals(2 * this.testConfiguration.getNumberOfIterations(), ((TestEnvironmentProvider)environmentDescriptionProviders[0]).generatedEnvironmentDescriptions);
        assertEquals(2 * this.testConfiguration.getNumberOfIterations(), ((TestEnvironmentProvider)environmentDescriptionProviders[1]).generatedEnvironmentDescriptions);
        assertEquals(4, resultWriterProvider.generatedResultWriters.size());
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_0_steps"));
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_1_agent_AgAlias_0_steps"));
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_1_steps"));
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_1_agent_AgAlias_1_steps"));
    }

    @EpSemTest
    public void runResultWritersCaptureAdditionalStatistics() {
        IAgentProvider[] agentProviders = new IAgentProvider[] {
                new TestAgentProvider(true)
        };
        IEnvironmentProvider[] environmentDescriptionProviders = new IEnvironmentProvider[] {
                new TestEnvironmentProvider()
        };
        TestSuite testSuite = new TestSuite(this.testConfiguration, environmentDescriptionProviders, agentProviders);
        TestResultWriterProvider resultWriterProvider = new TestResultWriterProvider();
        testSuite.run(resultWriterProvider);

        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestAgentProvider)agentProviders[0]).generatedAgents);
        assertEquals(this.testConfiguration.getNumberOfIterations(), ((TestEnvironmentProvider)environmentDescriptionProviders[0]).generatedEnvironmentDescriptions);
        assertEquals(2, resultWriterProvider.generatedResultWriters.size());
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_0_steps"));
        this.validateResultWriter(resultWriterProvider.generatedResultWriters.get("env_EnvAlias_0_agent_AgAlias_0_additionalStat"));
    }

    @EpSemTest
    public void runTriggersBeforeRun() {
        IAgentProvider[] agentProviders = new IAgentProvider[] {
                new TestAgentProvider(false)
        };
        IEnvironmentProvider[] environmentDescriptionProviders = new IEnvironmentProvider[] {
                new TestEnvironmentProvider()
        };
        AtomicBoolean beforeRunTriggered = new AtomicBoolean(false);
        Consumer<File> beforeRun = file -> { beforeRunTriggered.set(true); };
        TestSuite testSuite = new TestSuite(this.testConfiguration, environmentDescriptionProviders, agentProviders, beforeRun);
        TestResultWriterProvider resultWriterProvider = new TestResultWriterProvider();
        testSuite.run(resultWriterProvider);
        assertTrue(beforeRunTriggered.get());
    }
    //endregion

    //region Helper Methods
    private void validateResultWriter(TestResultWriter resultWriter) {
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCount);
        assertEquals(this.testConfiguration.getNumberOfIterations() * this.testConfiguration.getNumberOfGoals(), resultWriter.stepsLoggedCount);
        assertTrue(resultWriter.completed);
        assertEquals(this.testConfiguration.getNumberOfIterations(), resultWriter.iterationCountAtcompleted);
    }
    //endregion

    //region "mock" classes
    private class TestAgentProvider implements IAgentProvider {
        public int generatedAgents = 0;
        private boolean includeAdditionalStatistics;

        public TestAgentProvider(boolean includeAdditionalStatistics) {
            this.includeAdditionalStatistics = includeAdditionalStatistics;
        }

        @Override
        public IAgent getAgent() {
            this.generatedAgents++;
            return new TestAgent(this.includeAdditionalStatistics);
        }

        @Override
        public String getAlias() {
            return "AgAlias";
        }
    }

    private class TestAgent implements IAgent {
        private boolean includeAdditionalStatistics;

        public TestAgent(boolean includeAdditionalStatistics) {
            this.includeAdditionalStatistics = includeAdditionalStatistics;
        }
        @Override
        public void initialize(Action[] actions, IIntrospector introspector) {

        }

        @Override
        public Action getNextAction(SensorData sensorData) {
            return new Action("a");
        }

        @Override
        public String[] getStatisticTypes() {
            if (this.includeAdditionalStatistics)
                return new String[] { "additionalStat" };
            return new String[0];
        }

        @Override
        public ArrayList<Datum> getGoalData() {
            ArrayList<Datum> data = new ArrayList<>();
            if (this.includeAdditionalStatistics)
                data.add(new Datum("additionalStat", 0));
            return data;
        }
    }

    private class TestEnvironmentProvider implements IEnvironmentProvider {
        public int generatedEnvironmentDescriptions = 0;

        @Override
        public IEnvironment getEnvironment() {
            this.generatedEnvironmentDescriptions++;
            return new TestEnvironmentDescription();
        }

        @Override
        public String getAlias() {
            return "EnvAlias";
        }
    }

    private class TestEnvironmentDescription implements IEnvironment {

        @Override
        public Action[] getActions() {
            return new Action[] { new Action("a") };
        }

        @Override
        public SensorData applyAction(Action action) {
            return new SensorData(true);
        }

        @Override
        public boolean validateSequence(Sequence sequence) {
            return false;
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

        @Override
        public void close() {

        }
    }
    //endregion
}
