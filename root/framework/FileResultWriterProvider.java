package framework;

import java.io.File;
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
        this.timestampDirectory = Paths.get(FileResultWriterProvider.outputRootDirectory, this.getTimestamp()).toString();
    }

    /**
     * Get an instance of a {@link FileResultWriter} for the given agent.
     * @param agent The name of the agent used to create an output file name.
     * @param file A component of the output file name.
     * @return an instance of a {@link FileResultWriter}.
     */
    @Override
    public IResultWriter getResultWriter(String agent, String file) throws Exception {
        if (agent == null)
            throw new IllegalArgumentException("agent cannot be null");
        if (agent == "")
            throw new IllegalArgumentException("agent cannot be empty");
        if (file == null)
            throw new IllegalArgumentException("file cannot be null");
        if (file == "")
            throw new IllegalArgumentException("file cannot be empty");
        return new FileResultWriter(agent, Paths.get(this.timestampDirectory,  file + "." + this.getTimestamp() + ".csv").toString());
    }

    @Override
    public String getOutputDirectory(){
        return timestampDirectory;
    }

    private String getTimestamp()
    {
        Date myDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(myDate);
    }
}
