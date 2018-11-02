package framework;

import org.junit.jupiter.api.Test;
import utils.Sequence;

import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestRunTest {

    // constructor Tests
    @Test
    public void testConstructorNullAgentThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestRun(null, new TestEnvironmentDescription(), 1));
    }

    @Test
    public void testConstructorNullEnvironmentDescriptionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestRun(new TestAgent(), null, 1));
    }

    @Test
    public void testConstructorNumberOfGoalsToFindLessThan1ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestRun(new TestAgent(), new TestEnvironmentDescription(), 0));
    }

    // execute Tests
    @Test
    public void testExecuteInitializesAgentWithMoves() {
        TestAgent agent = new TestAgent();
        TestEnvironmentDescription environmentDescription = new TestEnvironmentDescription();
        TestRun testRun = new TestRun(agent, environmentDescription, 1);
        testRun.execute();
        assertArrayEquals(environmentDescription.getMoves(), agent.moves);
    }

    @Test
    public void testExecuteMarshalsCallsBetweenAgentAndEnvironmentSingleGoalWithResultWriter() {
        TestAgent agent = new TestAgent();
        TestEnvironmentDescription environment = new TestEnvironmentDescription();
        TestGoalListener goalListener = new TestGoalListener();
        TestRun testRun = new TestRun(agent, environment, 1);
        testRun.addGoalListener(goalListener);
        testRun.execute();

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

        assertArrayEquals(expectedEpisodicMemory, agent.episodes.toArray());
        assertArrayEquals(expectedResultWriterLogs, goalListener.logStatements.toArray());
    }

    @Test
    public void testExecuteMarshalsCallsBetweenAgentAndEnvironmentMultipleGoalsWithResultWriter() {
        Services.register(IRandomizer.class, new TestRandomizer());
        TestAgent agent = new TestAgent();
        TestEnvironmentDescription environment = new TestEnvironmentDescription();
        TestGoalListener goalListener = new TestGoalListener();
        TestRun testRun = new TestRun(agent, environment, 3);
        testRun.addGoalListener(goalListener);
        testRun.execute();

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

        assertArrayEquals(expectedEpisodicMemory, agent.episodes.toArray());
        assertArrayEquals(expectedResultWriterLogs, goalListener.logStatements.toArray());
    }

    private class TestAgent implements IAgent {
        public Move[] moves;

        public ArrayList<Episode> episodes = new ArrayList<>();

        private int moveIndex = 0;

        @Override
        public void initialize(Move[] moves) {
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
        public void addAgentListener(IAgentListener listener) {

        }
    }

    private  class TestEnvironmentDescription implements IEnvironmentDescription {
        private Move lastMove;
        @Override
        public Move[] getMoves() {
            return new Move[] { new Move("a"), new Move("b"), new Move("c") };
        }

        @Override
        public int transition(int currentState, Move move) {
            this.lastMove = move;
            return 0;
        }

        @Override
        public boolean isGoalState(int state) {
            return this.lastMove.getName() == "c";
        }

        @Override
        public int getNumGoalStates() {
            return 1;
        }

        @Override
        public int getNumStates() {
            return 0;
        }

        @Override
        public void applySensors(int lastState, Move move, int currentState, SensorData sensorData) {
            sensorData.setSensor(this.lastMove.getName(), this.lastMove.getName());
        }

        @Override
        public void addEnvironmentListener(IEnvironmentListener listener) {

        }

        @Override
        public boolean validateSequence(int state, Sequence sequence) {
            return false;
        }
    }

    private class TestGoalListener implements IGoalListener {
        public ArrayList<String> logStatements = new ArrayList<>();

        @Override
        public void goalReceived(GoalEvent event) {
            logStatements.add(event.getStepCountToGoal() + ",");
        }
    }

    private class TestRandomizer  implements IRandomizer {

        @Override
        public int getRandomNumber(int ceiling) {
            return 0;
        }
    }
}
