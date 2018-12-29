package framework;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EnvironmentTest {
    //region Constructor Tests
    @Test
    public void testConstructorNullEnvironmentDescriptionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Environment(null));
    }
    //endregion

    //region tick Tests
    @Test
    public void tickNullMoveThrowsException() {
        Environment environment = new Environment(new TestEnvironmentDescription());
        assertThrows(IllegalArgumentException.class, () -> environment.tick(null));
    }

    @Test
    public void tickMaintainsState() {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        SensorData sensorData = environment.tick(new Move("move1"));
        assertFalse(sensorData.isGoal());
        sensorData = environment.tick(new Move("move2"));
        assertFalse(sensorData.isGoal());
        sensorData = environment.tick(new Move("move3"));
        assertTrue(sensorData.isGoal());
    }
    //endregion

    //region reset Tests
    @Test
    public void resetUpdatesCurrentState() {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        environment.tick(new Move("move1"));
        environment.reset();
        // bonus move2 here should be NOP if we reset
        SensorData sensorData = environment.tick(new Move("move2"));
        assertFalse(sensorData.isGoal());
        // now demonstrate the original path to goal succeeds.
        sensorData = environment.tick(new Move("move1"));
        assertFalse(sensorData.isGoal());
        sensorData = environment.tick(new Move("move2"));
        assertFalse(sensorData.isGoal());
        sensorData = environment.tick(new Move("move3"));
        assertTrue(sensorData.isGoal());
    }
    //endregion

    //region validateSequence Tests
    @Test
    public void validateSequence() {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        Sequence sequence = new Sequence(description.getMoves());
        assertTrue(environment.validateSequence(sequence));
        assertFalse(environment.validateSequence(sequence.getSubsequence(1)));
    }

    @Test
    public void validateSequenceNullSequenceThrowsException()
    {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        assertThrows(IllegalArgumentException.class, () -> environment.validateSequence(null));
    }
    //endregion

    //region Helper Class
    private class TestEnvironmentDescription implements IEnvironmentDescription {
        private HashMap<Move, Integer>[] transitions;

        public TestEnvironmentDescription() {
            this.transitions = new HashMap[4];
            this.transitions[0] = new HashMap<>();
            this.transitions[0].put(new Move("move1"), 1);
            this.transitions[0].put(new Move("move2"), 0);
            this.transitions[0].put(new Move("move3"), 0);
            this.transitions[1] = new HashMap<>();
            this.transitions[1].put(new Move("move1"), 1);
            this.transitions[1].put(new Move("move2"), 2);
            this.transitions[1].put(new Move("move3"), 1);
            this.transitions[2] = new HashMap<>();
            this.transitions[2].put(new Move("move1"), 2);
            this.transitions[2].put(new Move("move2"), 2);
            this.transitions[2].put(new Move("move3"), 3);
            this.transitions[3] = new HashMap<>();
            this.transitions[3].put(new Move("move1"), 3);
            this.transitions[3].put(new Move("move2"), 3);
            this.transitions[3].put(new Move("move3"), 3);
        }

        @Override
        public Move[] getMoves() {
            return new Move[] { new Move("move1"), new Move("move2"), new Move("move3") };
        }

        @Override
        public TransitionResult transition(int currentState, Move move) {
            int newState = this.transitions[currentState].get(move);
            SensorData sensorData = new SensorData(newState == 3);
            return new TransitionResult(newState, sensorData);
        }

        @Override
        public int getRandomState() {
            return 0;
        }
    }
    //endregion
}
