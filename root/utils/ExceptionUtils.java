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
    public static String getString(Exception exception)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return stringWriter.toString();
    }
    //endregion
}
