package tests.utils;

import utils.ExceptionUtils;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class ExceptionUtilsTest {
    //region getStacktrace Tests
    @EpSemTest
    public void getStacktrace() {
        // This test is kind of weak, but the stacktrace will vary depending on build configurations so we're
        // going to have to settle for just verifying we got something back that wasn't empty.
        Exception exception = new Exception();
        String stacktrace = ExceptionUtils.getStacktrace(exception);
        assertNotEquals("", stacktrace);
    }

    @EpSemTest
    public void getStacktraceNullExceptionThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtils.getStacktrace(null));
    }
    //endregion
}
