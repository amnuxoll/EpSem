package utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionStackTraceToString {
    public static String getString(Exception exception)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}
