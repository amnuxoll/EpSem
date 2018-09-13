package framework;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A FileResultWriterProvider will generate {@link FileResultWriter}s bound to specific output directories.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FileResultWriterProvider implements IResultWriterProvider {
    private final static String outputRootDirectory = Paths.get(System.getProperty("user.home"), "fsm_output").toString();
    private String timestampDirectory;

    /**
     * Create an instance of a {@link FileResultWriterProvider}.
     */
    public FileResultWriterProvider() {
        Date myDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateString = sdf.format(myDate);
        this.timestampDirectory = Paths.get(FileResultWriterProvider.outputRootDirectory, dateString).toString();
    }

    /**
     * Get an instance of a {@link FileResultWriter} for the given agent.
     * @param agent The name of the agent used to create an output file name.
     * @return an instance of a {@link FileResultWriter}.
     */
    @Override
    public IResultWriter getResultWriter(String agent) throws Exception {
        if (agent == null)
            throw new IllegalArgumentException("agent cannot be null");
        if (agent == "")
            throw new IllegalArgumentException("agent cannot be empty");
        return new FileResultWriter(Paths.get(this.timestampDirectory,  agent + ".csv").toString());
    }
}
