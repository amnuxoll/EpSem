package utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Contains some utility methods for working with directories.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class DirectoryUtils {

    //region Static Variables

    /** Provides a standard directory in the user's home directory for all statistical output. */
    private final static File outputRootDirectory = new File(System.getProperty("user.home"), "fsm_output");

    //private final static File cenekRootDirectory = new File("/mnt/spinning_disks/", "fsm_output");
    //endregion

    //region Public Static Methods

    /**
     * Creates and returns a reference to a new output directory to use for an Agent test run.
     *
     * @return a {@link File} handle to the output directory.
     */
    public static File generateNewOutputDirectory()
    {
        File file = new File(DirectoryUtils.outputRootDirectory, DirectoryUtils.getTimestamp(System.currentTimeMillis()));
        file.mkdirs();
        return file;
    }

    public static File generateCenekOutputDirectory()
    {
        File file = new File(DirectoryUtils.outputRootDirectory, DirectoryUtils.getTimestamp(System.currentTimeMillis()));
        file.mkdirs();
        return file;
    }

    /**
     * Given a long, generates and returns the string representation of it in datetime format.
     *
     * @param timestamp the long to convert to a datetime format string.
     * @return the string representation of the given timestamp in datetime format.
     */
    public static String getTimestamp(long timestamp)
    {
        Date myDate = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(myDate);
    }
    //endregion
}
