package utils;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DirectoryUtils {
    public final static String outputRootDirectory = Paths.get(System.getProperty("user.home"), "fsm_output").toString();

    public static String generateNewOutputDirectory()
    {
        return Paths.get(DirectoryUtils.outputRootDirectory, DirectoryUtils.getTimestamp()).toString();
    }

    public static String getTimestamp()
    {
        Date myDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(myDate);
    }
}
