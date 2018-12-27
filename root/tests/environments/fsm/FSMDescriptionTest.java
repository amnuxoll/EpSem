package environments.fsm;

import framework.Move;
import framework.SensorData;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class FSMDescriptionTest {

    // constructor Tests
    @Test
    public void constructorNullTransitionTableThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescription(null));
    }

    @Test
    public void constructorEmptyTransitionTableThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescription(new FSMTransitionTable(new HashMap[0])));
    }

    @Test
    public void constructorNullEnumSetThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescription(new FSMTransitionTable(new HashMap[0]), null));
    }

    @Test
    public void constructorValidatesTransitionTableNotAllTransitionsContainSameMoveCount() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 1);
        transitionSet1.put(new Move("c"), 1);
        HashMap<Move, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Move("a"), 1);
        transitionSet2.put(new Move("b"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1,
                        transitionSet2
                };
        assertThrows(IllegalArgumentException.class, () -> new FSMDescription(new FSMTransitionTable(transitionTable)));
    }

    @Test
    public void constructorValidatesTransitionTableNotAllTransitionsContainSameMoves() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 1);
        transitionSet1.put(new Move("c"), 1);
        HashMap<Move, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Move("a"), 1);
        transitionSet2.put(new Move("b"), 1);
        transitionSet2.put(new Move("d"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1,
                        transitionSet2
                };
        assertThrows(IllegalArgumentException.class, () -> new FSMDescription(new FSMTransitionTable(transitionTable)));
    }

    // getSensorsToInclude Tests
    @Test
    public void getSensorsToIncludeDefaultsToNone() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        EnumSet<FSMDescription.Sensor> sensors = description.getSensorsToInclude();
        assertFalse(sensors.contains(FSMDescription.Sensor.EVEN_ODD));
        assertFalse(sensors.contains(FSMDescription.Sensor.NOISE));
    }

    @Test
    public void getSensorsToIncludeGivesSensors() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable), EnumSet.of(FSMDescription.Sensor.EVEN_ODD));
        EnumSet<FSMDescription.Sensor> sensors = description.getSensorsToInclude();
        assertTrue(sensors.contains(FSMDescription.Sensor.EVEN_ODD));
        assertFalse(sensors.contains(FSMDescription.Sensor.NOISE));
    }

    // getMoves Tests
    @Test
    public void getMovesFSMDescriptionInfersMoveSets() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 1);
        transitionSet1.put(new Move("c"), 1);
        HashMap<Move, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Move("a"), 1);
        transitionSet2.put(new Move("b"), 1);
        transitionSet2.put(new Move("c"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1,
                        transitionSet2
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        Move[] expectedMoves = new Move[] {
                        new Move("a"),
                        new Move("b"),
                        new Move("c"),
                };
        assertArrayEquals(expectedMoves, description.getMoves());
    }

    // transition Tests
    @Test
    public void transitionStateLessThanZeroThrowsException() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 1);
        transitionSet1.put(new Move("c"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertThrows(IllegalArgumentException.class, () -> description.transition(-1, new Move("a")));
    }

    @Test
    public void transitionStateTooLargeThrowsException() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 0);
        transitionSet1.put(new Move("b"), 0);
        transitionSet1.put(new Move("c"), 0);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertThrows(IllegalArgumentException.class, () -> description.transition(1, new Move("a")));
    }

    @Test
    public void transitionNullMoveThrowsException() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 0);
        transitionSet1.put(new Move("b"), 0);
        transitionSet1.put(new Move("c"), 0);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertThrows(IllegalArgumentException.class, () -> description.transition(0, null));
    }

    @Test
    public void transitionInvalidMoveThrowsException() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 0);
        transitionSet1.put(new Move("b"), 0);
        transitionSet1.put(new Move("c"), 0);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertThrows(IllegalArgumentException.class, () -> description.transition(0, new Move("d")));
    }

    @Test
    public void transitionReturnsNewState() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 2);
        transitionSet1.put(new Move("c"), 3);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertEquals(2, description.transition(0, new Move("b")));
    }

    // isGoalState Tests
    @Test
    public void isGoalState() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 2);
        transitionSet1.put(new Move("c"), 3);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertTrue(description.isGoalState(0));
        assertFalse(description.isGoalState(-1));
        assertFalse(description.isGoalState(1));
    }

    //getNumStates Tests
    @Test
    public void getNumStatesOneState() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertEquals(1, description.getNumStates());
    }

    @Test
    public void getNumStatesMultipleStates() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1,
                        transitionSet1,
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertEquals(3, description.getNumStates());
    }

    // applySensors Tests

    @Test
    public void applySensorsNullMoveThrowsException() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertThrows(IllegalArgumentException.class, () -> description.applySensors(0, null, 1, new SensorData(true)));
    }

    @Test
    public void applySensorsNullSensorDataThrowsException() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        assertThrows(IllegalArgumentException.class, () -> description.applySensors(0, new Move("a"), 1, null));
    }

    @Test
    public void applySensorsDisabledEvenOddDoesNothing() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable));
        SensorData sensorData = new SensorData(true);
        description.applySensors(0, new Move("a"),0, sensorData);
        assertFalse(sensorData.hasSensor("Even"));
    }

    @Test
    public void applySensorsEnabledEvenOddAddsToSensorDataEven() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable), EnumSet.of(FSMDescription.Sensor.EVEN_ODD));
        SensorData sensorData = new SensorData(true);
        description.applySensors(0, new Move("a"),0, sensorData);
        assertTrue(sensorData.hasSensor("EVEN_ODD"));
        assertEquals(true, sensorData.getSensor("EVEN_ODD"));
    }

    @Test
    public void applySensorsEnabledEvenOddAddsToSensorDataOdd() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1
                };
        FSMDescription description = new FSMDescription(new FSMTransitionTable(transitionTable), EnumSet.of(FSMDescription.Sensor.EVEN_ODD));
        SensorData sensorData = new SensorData(true);
        description.applySensors(0, new Move("a"),1, sensorData);
        assertTrue(sensorData.hasSensor("EVEN_ODD"));
        assertEquals(false, sensorData.getSensor("EVEN_ODD"));
    }
}
