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
public class EnvironmentTest {
    //region Constructor Tests
    @EpSemTest
    public void testConstructorNullEnvironmentDescriptionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Environment(null));
    }
    //endregion

    //region tick Tests
    @EpSemTest
    public void tickNullMoveThrowsException() {
        Environment environment = new Environment(new TestEnvironmentDescription());
        assertThrows(IllegalArgumentException.class, () -> environment.tick(null));
    }

    @EpSemTest
    public void tickMaintainsState() {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        SensorData sensorData = environment.tick(new Action("move1"));
        assertFalse(sensorData.isGoal());
        sensorData = environment.tick(new Action("move2"));
        assertFalse(sensorData.isGoal());
        sensorData = environment.tick(new Action("move3"));
        assertTrue(sensorData.isGoal());
    }
    //endregion

    //region reset Tests
    @EpSemTest
    public void resetUpdatesCurrentState() {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        environment.tick(new Action("move1"));
        environment.reset();
        // bonus move2 here should be NOP if we reset
        SensorData sensorData = environment.tick(new Action("move2"));
        assertFalse(sensorData.isGoal());
        // now demonstrate the original path to goal succeeds.
        sensorData = environment.tick(new Action("move1"));
        assertFalse(sensorData.isGoal());
        sensorData = environment.tick(new Action("move2"));
        assertFalse(sensorData.isGoal());
        sensorData = environment.tick(new Action("move3"));
        assertTrue(sensorData.isGoal());
    }
    //endregion

    //region validateSequence Tests
    @EpSemTest
    public void validateSequence() {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        Sequence sequence = new Sequence(description.getActions());
        assertTrue(environment.validateSequence(sequence));
        assertFalse(environment.validateSequence(sequence.getSubsequence(1)));
    }

    @EpSemTest
    public void validateSequenceNullSequenceThrowsException()
    {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        assertThrows(IllegalArgumentException.class, () -> environment.validateSequence(null));
    }
    //endregion

    //region Helper Class
    private class TestEnvironmentDescription implements IEnvironmentDescription {
        private ArrayList<HashMap<Action, Integer>> transitions;

        public TestEnvironmentDescription() {
            this.transitions = new ArrayList<>();
            this.transitions.add(new HashMap<>());
            this.transitions.get(0).put(new Action("move1"), 1);
            this.transitions.get(0).put(new Action("move2"), 0);
            this.transitions.get(0).put(new Action("move3"), 0);
            this.transitions.add(new HashMap<>());
            this.transitions.get(1).put(new Action("move1"), 1);
            this.transitions.get(1).put(new Action("move2"), 2);
            this.transitions.get(1).put(new Action("move3"), 1);
            this.transitions.add(new HashMap<>());
            this.transitions.get(2).put(new Action("move1"), 2);
            this.transitions.get(2).put(new Action("move2"), 2);
            this.transitions.get(2).put(new Action("move3"), 3);
            this.transitions.add(new HashMap<>());
            this.transitions.get(3).put(new Action("move1"), 3);
            this.transitions.get(3).put(new Action("move2"), 3);
            this.transitions.get(3).put(new Action("move3"), 3);
        }

        @Override
        public Action[] getActions() {
            return new Action[] { new Action("move1"), new Action("move2"), new Action("move3") };
        }

        @Override
        public TransitionResult transition(int currentState, Action action) {
            int newState = this.transitions.get(currentState).get(action);
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
