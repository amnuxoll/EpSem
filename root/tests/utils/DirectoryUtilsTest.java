package utils;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DirectoryUtilsTest {
    //region generateNewOutputDirectory Tests
    @Test
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
    @Test
    public void getTimestamp()
    {
        long time = 303030;
        String timestamp = DirectoryUtils.getTimestamp(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        assertEquals(sdf.format(new Date(time)), timestamp);

    }
    //endregion
}
