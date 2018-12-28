package utils;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class DirectoryUtils {
    //region Static Variables
    public final static String outputRootDirectory = Paths.get(System.getProperty("user.home"), "fsm_output").toString();
    //endregion

    //region Public Static Methods
    public static File generateNewOutputDirectory()
    {
        File file = new File(DirectoryUtils.outputRootDirectory, DirectoryUtils.getTimestamp());
        file.mkdirs();
        return file;
    }

    public static String getTimestamp()
    {
        Date myDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(myDate);
    }
    //endregion
}
