package tests.framework;

import framework.*;

import java.util.ArrayList;
import java.util.HashMap;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class TestRunTest {
    //region constructor Tests
    @EpSemTest
    public void constructorNullAgentThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestRun(null, new TestEnvironment(), 1));
    }

    @EpSemTest
    public void constructorNullEnvironmentDescriptionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestRun(new TestAgent(), null, 1));
    }

    @EpSemTest
    public void constructorNumberOfGoalsToFindLessThan1ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestRun(new TestAgent(), new TestEnvironment(), 0));
    }
    //endregion

    //region execute Tests
    @EpSemTest
    public void executeInitializesAgentWithMoves() {
        TestAgent agent = new TestAgent();
        TestEnvironment environmentDescription = new TestEnvironment();
        TestRun testRun = new TestRun(agent, environmentDescription, 1);
        testRun.run();
        assertArrayEquals(environmentDescription.getActions(), agent.actions);
    }

    @EpSemTest
    public void executeMarshalsCallsBetweenAgentAndEnvironmentSingleGoalWithResultWriter() {
        TestAgent agent = new TestAgent();
        TestEnvironment environment = new TestEnvironment();
        TestGoalListener goalListener = new TestGoalListener();
        TestRun testRun = new TestRun(agent, environment, 1);
        testRun.addGoalListener(goalListener);
        testRun.run();
        assertTrue(agent.testRunComplete);
        assertEquals(1, agent.goalCount);

        SensorData sensorA = new SensorData(false);
        sensorA.setSensor("a", "a");
        SensorData sensorB = new SensorData(false);
        sensorB.setSensor("b", "b");

        Episode episodeA = new Episode(new SensorData(true), new Action("a"));
        Episode episodeB = new Episode(sensorA, new Action("b"));
        Episode episodeC = new Episode(sensorB, new Action("c"));
        Episode[] expectedEpisodicMemory = new Episode[] {
                        episodeA, episodeB, episodeC
                };
        String[] expectedResultWriterLogs = new String[] {
                  "3,"
                };
        String[] additionalStatLogs = new String[] {
                "0,"
        };

        assertArrayEquals(expectedEpisodicMemory, agent.episodes.toArray());
        assertArrayEquals(expectedResultWriterLogs, goalListener.logStatements.get("steps").toArray());
        assertArrayEquals(additionalStatLogs, goalListener.logStatements.get("additionalStat").toArray());
    }

    @EpSemTest
    public void executeMarshalsCallsBetweenAgentAndEnvironmentMultipleGoalsWithResultWriter() {
        TestAgent agent = new TestAgent();
        TestEnvironment environment = new TestEnvironment();
        TestGoalListener goalListener = new TestGoalListener();
        TestRun testRun = new TestRun(agent, environment, 3);
        testRun.addGoalListener(goalListener);
        testRun.run();
        assertTrue(agent.testRunComplete);
        assertEquals(3, agent.goalCount);

        SensorData sensorA = new SensorData(false);
        sensorA.setSensor("a", "a");
        SensorData sensorB = new SensorData(false);
        sensorB.setSensor("b", "b");
        SensorData sensorC = new SensorData(true);
        sensorC.setSensor("c", "c");

        Episode episodeA = new Episode(sensorC, new Action("a"));
        Episode episodeB = new Episode(sensorA, new Action("b"));
        Episode episodeC = new Episode(sensorB, new Action("c"));

        // The first sensor data is the base GOAL template provided by the framework
        Episode[] expectedEpisodicMemory = new Episode[] {
                new Episode(new SensorData(true), new Action("a")), episodeB, episodeC,
                        episodeA, episodeB, episodeC,
                        episodeA, episodeB, episodeC
                };
        String[] expectedResultWriterLogs = new String[] {
                        "3,",
                        "3,",
                        "3,"
                };
        String[] additionalStatLogs = new String[] {
                "0,",
                "1,",
                "2,"
        };

        assertArrayEquals(expectedEpisodicMemory, agent.episodes.toArray());
        assertArrayEquals(expectedResultWriterLogs, goalListener.logStatements.get("steps").toArray());
        assertArrayEquals(additionalStatLogs, goalListener.logStatements.get("additionalStat").toArray());
    }
    //endregion

    //region "mock" classes
    private class TestAgent implements IAgent {
        public Action[] actions;

        public ArrayList<Episode> episodes = new ArrayList<>();
        public boolean testRunComplete = false;
        public int goalCount = 0;

        private int moveIndex = 0;

        @Override
        public void initialize(Action[] actions, IIntrospector introspector) {
            this.actions = actions;
        }

        @Override
        public Action getNextAction(SensorData sensorData) {
            Action action = this.actions[this.moveIndex++];
            Episode episode = new Episode(sensorData, action);
            this.episodes.add(episode);
            if (this.moveIndex >= this.actions.length)
                this.moveIndex = 0;
            return action;
        }

        @Override
        public void onGoalFound() {
            this.goalCount++;
        }

        @Override
        public void onTestRunComplete()
        {
            this.testRunComplete = true;
        }

        @Override
        public String[] getStatisticTypes()
        {
            return new String[] { "steps", "additionalStat "};
        }

        private int datumCount = 0;
        @Override
        public ArrayList<Datum> getGoalData()
        {
            ArrayList<Datum> data = new ArrayList<>();
            data.add(new Datum("additionalStat", datumCount++));
            return data;
        }
    }

    private  class TestEnvironment implements IEnvironment {
        @Override
        public Action[] getActions() {
            return new Action[] { new Action("a"), new Action("b"), new Action("c") };
        }

        @Override
        public boolean validateSequence(Sequence sequence) {
            return false;
        }

        @Override
        public SensorData applyAction(Action action) {
            if (action == null)
                return new SensorData(true);
            SensorData sensorData = new SensorData(action.equals(this.getActions()[2]));
            sensorData.setSensor(action.getName(), action.getName());
            return sensorData;
        }
    }

    private class TestGoalListener implements IGoalListener {
        public HashMap<String, ArrayList<String>> logStatements = new HashMap<>();

        @Override
        public void goalReceived(GoalEvent event) {
            if (!this.logStatements.containsKey("steps"))
                logStatements.put("steps", new ArrayList<>());
            //logStatements.get("steps").add(event.getStepCountToGoal() + ",");
            for (Datum agentData : event.getAgentData())
            {
                String statistic = agentData.getStatistic();
                if (!this.logStatements.containsKey(statistic))
                    logStatements.put(statistic, new ArrayList<>());
                logStatements.get(statistic).add(agentData.getDatum() + ",");
            }
        }
    }
    //endregion
}
