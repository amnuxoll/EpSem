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
        assertThrows(IllegalArgumentException.class, () -> new TestRun(null, new TestEnvironmentDescription(), 1));
    }

    @EpSemTest
    public void constructorNullEnvironmentDescriptionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestRun(new TestAgent(), null, 1));
    }

    @EpSemTest
    public void constructorNumberOfGoalsToFindLessThan1ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestRun(new TestAgent(), new TestEnvironmentDescription(), 0));
    }
    //endregion

    //region execute Tests
    @EpSemTest
    public void executeInitializesAgentWithMoves() {
        TestAgent agent = new TestAgent();
        TestEnvironmentDescription environmentDescription = new TestEnvironmentDescription();
        TestRun testRun = new TestRun(agent, environmentDescription, 1);
        testRun.execute();
        assertArrayEquals(environmentDescription.getMoves(), agent.moves);
    }

    @EpSemTest
    public void executeMarshalsCallsBetweenAgentAndEnvironmentSingleGoalWithResultWriter() {
        TestAgent agent = new TestAgent();
        TestEnvironmentDescription environment = new TestEnvironmentDescription();
        TestGoalListener goalListener = new TestGoalListener();
        TestRun testRun = new TestRun(agent, environment, 1);
        testRun.addGoalListener(goalListener);
        testRun.execute();
        assertTrue(agent.testRunComplete);
        assertEquals(1, agent.goalCount);

        SensorData sensorA = new SensorData(false);
        sensorA.setSensor("a", "a");
        SensorData sensorB = new SensorData(false);
        sensorB.setSensor("b", "b");
        SensorData sensorC = new SensorData(true);
        sensorC.setSensor("c", "c");

        Episode episodeA = new Episode(new Move("a"));
        episodeA.setSensorData(sensorA);
        Episode episodeB = new Episode(new Move("b"));
        episodeB.setSensorData(sensorB);
        Episode episodeC = new Episode(new Move("c"));
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
        TestEnvironmentDescription environment = new TestEnvironmentDescription();
        TestGoalListener goalListener = new TestGoalListener();
        TestRun testRun = new TestRun(agent, environment, 3);
        testRun.addGoalListener(goalListener);
        testRun.execute();
        assertTrue(agent.testRunComplete);
        assertEquals(3, agent.goalCount);

        SensorData sensorA = new SensorData(false);
        sensorA.setSensor("a", "a");
        SensorData sensorB = new SensorData(false);
        sensorB.setSensor("b", "b");
        SensorData sensorC = new SensorData(true);
        sensorC.setSensor("c", "c");

        Episode episodeA = new Episode(new Move("a"));
        episodeA.setSensorData(sensorA);
        Episode episodeB = new Episode(new Move("b"));
        episodeB.setSensorData(sensorB);
        Episode episodeC = new Episode(new Move("c"));
        episodeC.setSensorData(sensorC);

        Episode[] expectedEpisodicMemory = new Episode[] {
                        episodeA, episodeB, episodeC,
                        episodeA, episodeB, episodeC,
                        episodeA, episodeB, new Episode(new Move("c"))
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
        public Move[] moves;

        public ArrayList<Episode> episodes = new ArrayList<>();
        public boolean testRunComplete = false;
        public int goalCount = 0;

        private int moveIndex = 0;

        @Override
        public void initialize(Move[] moves, IIntrospector introspector) {
            this.moves = moves;
        }

        @Override
        public Move getNextMove(SensorData sensorData) {
            if (this.episodes.size() > 0)
                this.episodes.get(this.episodes.size() - 1).setSensorData(sensorData);
            Move move = this.moves[this.moveIndex++];
            Episode episode = new Episode(move);
            this.episodes.add(episode);
            if (this.moveIndex >= this.moves.length)
                this.moveIndex = 0;
            return move;
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
            return new String[] { "additionalStat "};
        }

        private int datumCount = 0;
        @Override
        public ArrayList<Datum> getData()
        {
            ArrayList<Datum> data = new ArrayList<>();
            data.add(new Datum("additionalStat", datumCount++));
            return data;
        }
    }

    private  class TestEnvironmentDescription implements IEnvironmentDescription {
        @Override
        public Move[] getMoves() {
            return new Move[] { new Move("a"), new Move("b"), new Move("c") };
        }

        @Override
        public TransitionResult transition(int currentState, Move move) {
            SensorData sensorData = new SensorData(move.equals(this.getMoves()[2]));
            sensorData.setSensor(move.getName(), move.getName());
            return new TransitionResult(0, sensorData);
        }

        @Override
        public int getRandomState() {
            return 0;
        }
    }

    private class TestGoalListener implements IGoalListener {
        public HashMap<String, ArrayList<String>> logStatements = new HashMap<>();

        @Override
        public void goalReceived(GoalEvent event) {
            if (!this.logStatements.containsKey("steps"))
                logStatements.put("steps", new ArrayList<>());
            logStatements.get("steps").add(event.getStepCountToGoal() + ",");
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