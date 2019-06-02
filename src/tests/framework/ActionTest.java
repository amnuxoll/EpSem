package tests.framework;

import framework.Action;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class ActionTest {
    //region Constructor Tests

    @EpSemTest
    public void testConstructorNullNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Action((String) null));
    }

    @EpSemTest
    public void testConstructorEmptyNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Action(""));
    }

    @EpSemTest
    public void testCopyConstructorNullActionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Action((Action) null));
    }

    @EpSemTest
    public void testCopyConstructor() {
        Action orig = new Action("action");
        Action copy = new Action(orig);
        assertNotSame(orig, copy);
        assertEquals(orig, copy);
    }
    //endregion

    //region getName Tests

    @EpSemTest
    public void testGetName() {
        Action action = new Action("action");
        assertEquals("action", action.getName());
    }

    //endregion

    //region equals Tests

    @EpSemTest
    public void testEqualsAreEqual() {
        Action action1 = new Action("move");
        Action action2 = new Action("move");
        assertEquals(action1, action2);
    }

    @EpSemTest
    public void testEqualsAreNotEqual() {
        Action action1 = new Action("action1");
        Action action2 = new Action("action2");
        assertNotEquals(action1, action2);
    }

    //endregion

    //region hashCode Tests

    @EpSemTest
    public void testHashCodeAreEqual() {
        Action action1 = new Action("move");
        Action action2 = new Action("move");
        assertEquals(action1.hashCode(), action2.hashCode());
    }

    //endregion

    //region toString Tests

    @EpSemTest
    public void toStringIsName()
    {
        Action action = new Action("action");
        assertEquals("action", action.toString());
    }

    //endregion
}
