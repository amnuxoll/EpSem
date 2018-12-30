package environments.fsm;

import framework.Move;
import framework.SensorData;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMDescriptionTest {
    //region constructor Tests
    @Test
    public void constructorNullTransitionTableThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescription(null));
    }

    @Test
    public void constructorNullEnumSetThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescription(this.getFsmTransitionTable(), null));
    }
    //endregion

    //region getMoves Tests
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
    //endregion

    //region transition Tests
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
        assertEquals(2, description.transition(0, new Move("b")).getState());
    }
    //endregion

    //region getRandomState Tests
    @Test
    public void getRandomState()
    {
        FSMDescription description = new FSMDescription(this.getFsmTransitionTable());
        int randomState = description.getRandomState();
        assertTrue(randomState >= 0);
        assertTrue(randomState < 2);
    }
    //endregion

    //region tweakTable Tests
    @Test
    public void tweakTable()
    {
        fail("implement this test");
    }
    //endregion

    //region Helper Methods
    private FSMTransitionTable getFsmTransitionTable()
    {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 1);
        transitionSet1.put(new Move("c"), 1);
        HashMap<Move, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Move("a"), 1);
        transitionSet2.put(new Move("b"), 1);
        transitionSet2.put(new Move("c"), 1);
        HashMap<Move, Integer> transitionSet3 = new HashMap<>();
        transitionSet3.put(new Move("a"), 1);
        transitionSet3.put(new Move("b"), 1);
        transitionSet3.put(new Move("c"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                transitionSet1,
                transitionSet2,
                transitionSet3
        };
        return new FSMTransitionTable(transitionTable);
    }
    //endregion
}
