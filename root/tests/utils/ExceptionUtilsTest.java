package utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class ExceptionUtilsTest {
    //region getStacktrace Tests
    @Test
    public void getStacktrace() {
        // This test is kind of weak, but the stacktrace will vary depending on build configurations so we're
        // going to have to settle for just verifying we got something back that wasn't empty.
        Exception exception = new Exception();
        String stacktrace = ExceptionUtils.getStacktrace(exception);
        assertNotEquals("", stacktrace);
    }

    @Test
    public void getStacktraceNullExceptionThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtils.getStacktrace(null));
    }
    //endregion
}
