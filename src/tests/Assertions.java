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
        Assertions.assertThrows(exceptionType, delegate, null);
    }

    public static void assertThrows(Class exceptionType, Callable delegate, String message) {
        try {
            delegate.call();
        } catch (Exception e) {
            if (e.getClass() != exceptionType) {
                e.printStackTrace();
                StringBuilder builder = new StringBuilder("Expected exception '");
                builder.append(exceptionType.getName());
                builder.append("' but received '");
                builder.append(e.getClass().getName());
                builder.append("'.");
                Assertions.throwException(builder.toString(), message);
            }
            return;
        }
        StringBuilder builder = new StringBuilder("Expected exception '");
        builder.append(exceptionType.getName());
        builder.append("' but none was thrown.");
        Assertions.throwException(builder.toString(), message);
    }

    public static void assertThrows(Class exceptionType, Runnable delegate) {
        Assertions.assertThrows(exceptionType, delegate, null);
    }

    public static void assertThrows(Class exceptionType, Runnable delegate, String message) {
        try {
            delegate.run();
        } catch (Exception e) {
            if (e.getClass() != exceptionType) {
                e.printStackTrace();
                StringBuilder builder = new StringBuilder("Expected exception '");
                builder.append(exceptionType.getName());
                builder.append("' but received '");
                builder.append(e.getClass().getName());
                builder.append("'.");
                Assertions.throwException(builder.toString(), message);
            }
            return;
        }
        StringBuilder builder = new StringBuilder("Expected exception '");
        builder.append(exceptionType.getName());
        builder.append("' but none was thrown.");
        Assertions.throwException(builder.toString(), message);
    }

    public static void assertFalse(boolean value) {
        Assertions.assertFalse(value, null);
    }

    public static void assertFalse(boolean value, String message) {
        if (value)
            Assertions.throwException("Expected false", message);
    }

    public static void assertTrue(boolean value) {
        Assertions.assertTrue(value, null);
    }

    public static void assertTrue(boolean value, String message) {
        if (!value)
            Assertions.throwException("Expected true", message);
    }

    public static void assertEquals(double one, double two, double delta) {
        Assertions.assertEquals(one, two, delta, null);
    }

    public static void assertEquals(double one, double two, double delta, String message) {
        if (delta < Math.abs(one - two) / Math.max(Math.abs(one), Math.abs(two)))
            Assertions.throwException(String.format("doubles %.3f and %.3f are not equal within delta %.3f", one, two, delta), message);
    }

    public static void assertEquals(Object object1, Object object2) {
        Assertions.assertEquals(object1, object2, null);
    }

    public static void assertEquals(Object object1, Object object2, String message) {
        if (!object1.equals(object2))
            Assertions.throwException("Objects did not match. Expected " + object1.toString() + " and received " + object2.toString(), message);
    }

    public static void assertNotEquals(Object object1, Object object2) {
        Assertions.assertNotEquals(object1, object2, null);
    }

    public static void assertNotEquals(Object object1, Object object2, String message) {
        if (object1.equals(object2))
            Assertions.throwException("Objects matched.", message);
    }

    public static void fail(String reason) {
        throw new AssertionFailedException(reason);
    }

    public static void assertNull(Object object) {
        Assertions.assertNull(object, null);
    }

    public static void assertNull(Object object, String message) {
        if (object != null)
            Assertions.throwException("object was not null", message);
    }

    public static void assertNotNull(Object object) {
        Assertions.assertNotNull(object, null);
    }

    public static void assertNotNull(Object object, String message) {
        if (object == null)
            Assertions.throwException("object was null.", message);
    }

    public static void assertArrayEquals(Object[] array1, Object[] array2) {
        Assertions.assertArrayEquals(array1, array2, null);
    }

    public static void assertArrayEquals(Object[] array1, Object[] array2, String message) {
        if (array1 == null && array2 != null)
            Assertions.throwException("array1 is null and array2 is not", message);
        if (array1 != null && array2 == null)
            Assertions.throwException("array1 is not null and array2 is.", message);
        if (array1 != null) {
            if (array1.length != array2.length)
                Assertions.throwException("array1 and array2 have different lengths.", message);
            for (int i = 0; i < array1.length; ++i) {
                if (!array1[i].equals(array2[i]))
                    Assertions.throwException("Arrays differ at index " + i, message);
            }
        }
    }

    public static void assertSame(Object instance1, Object instance2) {
        Assertions.assertSame(instance1, instance2, null);
    }

    public static void assertSame(Object instance1, Object instance2, String message) {
        if (instance1 != instance2)
            Assertions.throwException("objects are not same instance.", message);
    }
    //endregion

    //region Private Methods
    private static void throwException(String defaultMessage, String userMessage) {
        StringBuilder builder = new StringBuilder(defaultMessage);
        if (userMessage != null) {
            builder.append(" :: ");
            builder.append(userMessage);
        }
        throw new AssertionFailedException(builder.toString());
    }
    //endregion
}
