package utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class DirectoryUtils {
    //region Static Variables
    private final static File outputRootDirectory = new File(System.getProperty("user.home"), "fsm_output");
    //endregion

    //region Public Static Methods
    public static File generateNewOutputDirectory()
    {
        File file = new File(DirectoryUtils.outputRootDirectory, DirectoryUtils.getTimestamp(System.currentTimeMillis()));
        file.mkdirs();
        return file;
    }

    public static String getTimestamp(long timestamp)
    {
        Date myDate = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(myDate);
    }
    //endregion
}
