package tests.utils;

import utils.DirectoryUtils;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class DirectoryUtilsTest {
    //region generateNewOutputDirectory Tests
    @EpSemTest
    public void generateNewOutputDirectory()
    {
        File outputDirectory = DirectoryUtils.generateNewOutputDirectory();
        try {
            assertTrue(outputDirectory.exists());
            File parentDirectory = outputDirectory.getParentFile();
            assertEquals(Paths.get(System.getProperty("user.home"), "fsm_output").toString(), parentDirectory.getAbsolutePath());
        } finally {
            outputDirectory.delete();
        }
    }
    //endregion

    //region getTimestamp Tests
    @EpSemTest
    public void getTimestamp()
    {
        long time = 303030;
        String timestamp = DirectoryUtils.getTimestamp(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        assertEquals(sdf.format(new Date(time)), timestamp);

    }
    //endregion
}
