package utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class ExceptionUtils {
    //region Public Static Methods
    public static String getStacktrace(Exception exception)
    {
        if (exception == null)
            throw new IllegalArgumentException("exception cannot be null.");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return stringWriter.toString();
    }
    //endregion
}
