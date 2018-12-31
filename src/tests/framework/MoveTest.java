package tests.framework;

import framework.Move;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class MoveTest {
    //region Constructor Tests
    @EpSemTest
    public void testConstructorNullNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Move(null));
    }

    @EpSemTest
    public void testConstructorEmptyNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Move(""));
    }
    //endregion

    //region getName Tests
    @EpSemTest
    public void testGetName() {
        Move move = new Move("move");
        assertEquals("move", move.getName());
    }
    //endregion

    //region equals Tests
    @EpSemTest
    public void testEqualsAreEqual() {
        Move move1 = new Move("move");
        Move move2 = new Move("move");
        assertEquals(move1, move2);
    }

    @EpSemTest
    public void testEqualsAreNotEqual() {
        Move move1 = new Move("move1");
        Move move2 = new Move("move2");
        assertNotEquals(move1, move2);
    }
    //endregion

    //region hashCode Tests
    @EpSemTest
    public void testHashCodeAreEqual() {
        Move move1 = new Move("move");
        Move move2 = new Move("move");
        assertEquals(move1.hashCode(), move2.hashCode());
    }
    //endregion

    //region toString Tests
    @EpSemTest
    public void toStringIsName()
    {
        Move move = new Move("move");
        assertEquals("move", move.toString());
    }
    //endregion
}
