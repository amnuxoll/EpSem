package framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnvironmentTest {

    // constructor Tests
    @Test
    public void testConstructorNullTransitionDataThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Environment(null));
    }

    // getMoves Tests
    @Test
    public void getMovesReturnsMoves() {
        Environment environment = new Environment(new TestEnvironmentDescription());
        Move[] expectedMoves = new Move[] { new Move("move1"), new Move("move2"), new Move("move3") };
        assertArrayEquals(expectedMoves, environment.getMoves());
    }

    // tick Tests
    @Test
    public void tickNullMoveThrowsException() {
        Environment environment = new Environment(new TestEnvironmentDescription());
        assertThrows(IllegalArgumentException.class, () -> environment.tick(null));
    }

    @Test
    public void tickUpdatesState() {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        assertEquals(0, environment.getCurrentState());
        environment.tick(new Move("test"));
        assertEquals(0, description.receivedState);
        assertEquals(new Move("test"), description.receivedMove);
        assertEquals(13, environment.getCurrentState());
    }

    @Test
    public void tickSensorDataShowsGoalSuccessFalse() {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        Environment environment = new Environment(description);
        SensorData sensorData = environment.tick(new Move("test"));
        assertEquals(13, description.receivedIsGoalState);
        assertEquals(false, sensorData.isGoal());
    }

    @Test
    public void tickSensorDataShowsGoalSuccess() {
        TestEnvironmentDescription description = new TestEnvironmentDescription(true);
        Environment environment = new Environment(description);
        SensorData sensorData = environment.tick(new Move("test"));
        assertEquals(13, description.receivedIsGoalState);
        assertEquals(true, sensorData.isGoal());
    }

    @Test
    public void tickAppliesSensorsFromDescription() {
        TestEnvironmentDescription description = new TestEnvironmentDescription(true);
        Environment environment = new Environment(description);
        SensorData sensorData = environment.tick(new Move("test"));
        assertEquals(true, description.sensorsApplied);
        assertEquals(true, sensorData.hasSensor("sensorsApplied"));
    }

    // reset Tests
    @Test
    public void resetUsesEnvironmentDescriptionAndRandomizerToSetCurrentState() {
        TestEnvironmentDescription description = new TestEnvironmentDescription();
        TestRandomizer randomizer = new TestRandomizer();
        Services.register(IRandomizer.class, randomizer);
        Environment environment = new Environment(description);
        environment.reset();
        assertEquals(42, randomizer.receivedCeiling);
        assertEquals(7, environment.getCurrentState());
    }

    private class TestEnvironmentDescription implements IEnvironmentDescription {
        public int receivedState = -1;
        public Move receivedMove = null;
        public int receivedIsGoalState = -1;
        public boolean sensorsApplied = false;

        private boolean hitGoal;

        public TestEnvironmentDescription() {
            this.hitGoal = false;
        }

        public TestEnvironmentDescription(boolean hitGoal) {
            this.hitGoal = hitGoal;
        }

        @Override
        public Move[] getMoves() {
            return new Move[] { new Move("move1"), new Move("move2"), new Move("move3") };
        }

        @Override
        public int transition(int currentState, Move move) {
            this.receivedState = currentState;
            this.receivedMove = move;
            return 13;
        }

        @Override
        public boolean isGoalState(int state) {
            this.receivedIsGoalState = state;
            return this.hitGoal;
        }

        @Override
        public int getNumStates() {
            return 42;
        }

        @Override
        public void applySensors(int lastState, Move move, int currentState, SensorData sensorData) {
            this.sensorsApplied = true;
            sensorData.setSensor("sensorsApplied", true);
        }
    }

    private class TestRandomizer implements IRandomizer {

        public int receivedCeiling = -1;

        @Override
        public int getRandomNumber(int ceiling) {
            this.receivedCeiling = ceiling;
            return 7;
        }
    }
}
