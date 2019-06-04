package utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Provides some helper methods to operate against {@link Exception}.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class ExceptionUtils {

    //region Public Static Methods

    /**
     * Extracts the exception's stack trace as a string for custom operations.
     *
     * @param exception the {@link Exception} from which to extract the stack trace.
     * @return the stack trace as a string.
     */
    public static String getStacktrace(Exception exception) {
        if (exception == null)
            throw new IllegalArgumentException("exception cannot be null.");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    //endregion
}
