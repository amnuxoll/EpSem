package tests;

import java.util.concurrent.Callable;
/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Assertions {
    //region Public Static Methods
    public static void assertThrows(Class exceptionType, Callable delegate) {
        try {
            delegate.call();
        } catch (Exception e) {
            if (e.getClass() != exceptionType) {
                e.printStackTrace();
                throw new AssertionFailedException("Exception thrown but is wrong type.", e);
            }
            return;
        }
        throw new AssertionFailedException("No exception was thrown.");
    }

    public static void assertThrows(Class exceptionType, Runnable delegate) {
        try {
            delegate.run();
        } catch (Exception e) {
            if (e.getClass() != exceptionType) {
                e.printStackTrace();
                throw new AssertionFailedException("Exception thrown but is wrong type.", e);
            }
            return;
        }
        throw new AssertionFailedException("No exception was thrown.");
    }

    public static void assertFalse(boolean value) {
        if (value)
            throw new AssertionFailedException("Expected false.");
    }

    public static void assertTrue(boolean value) {
        if (!value)
            throw new AssertionFailedException("Expected true.");
    }

    public static void assertEquals(double one, double two, double delta) {
        if (delta < Math.abs(one - two) / Math.max(Math.abs(one), Math.abs(two)))
            throw new AssertionFailedException(String.format("doubles %.3f and %.3f are not equal within delta %.3f", one, two, delta));
    }

    public static void assertEquals(Object object1, Object object2) {
        if (!object1.equals(object2))
            throw new AssertionFailedException("Objects did not match. Expected " + object1.toString() + " and recieved " + object2.toString());
    }

    public static void assertNotEquals(Object object1, Object object2) {
        if (object1.equals(object2))
            throw new AssertionFailedException("Objects matched.");
    }

    public static void fail(String reason) {
        throw new AssertionFailedException(reason);
    }

    public static void assertNull(Object object) {
        if (object != null)
            throw new AssertionFailedException("object was not null");
    }

    public static void assertNotNull(Object object) {
        if (object == null)
            throw new AssertionFailedException("object was null.");
    }

    public static void assertArrayEquals(Object[] array1, Object[] array2) {
        if (array1 == null && array2 != null)
            throw new AssertionFailedException("array1 is null and array2 is not");
        if (array1 != null && array2 == null)
            throw new AssertionFailedException("array1 is not null and array2 is.");
        if (array1 != null) {
            if (array1.length != array2.length)
                throw new AssertionFailedException("array1 and array2 have different lengths.");
            for (int i = 0; i < array1.length; ++i) {
                if (!array1[i].equals(array2[i]))
                    throw new AssertionFailedException("Arrays differ at index " + i);
            }
        }
    }

    public static void assertSame(Object instance1, Object instance2) {
        if (instance1 != instance2)
            throw new AssertionFailedException("objects are not same instance.");
    }
    //endregion
}
