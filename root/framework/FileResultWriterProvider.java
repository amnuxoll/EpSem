package framework;

import utils.DirectoryUtils;

import java.io.File;

/**
 * A FileResultWriterProvider will generate {@link FileResultWriter}s bound to specific output directories.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FileResultWriterProvider implements IResultWriterProvider {
    //region Class Variables
    private File outputDirectory;
    //endregion

    //region Constructors
    /**
     * Create an instance of a {@link FileResultWriterProvider}.
     */
    public FileResultWriterProvider(File directory) {
        if (directory == null)
            throw new IllegalArgumentException("directory cannot be null.");
        if (directory.isDirectory() == false)
            throw new IllegalArgumentException("provided file is not a directory.");
        this.outputDirectory = directory;
    }
    //endregion

    //region IResultWriterProvider Members
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
        return new FileResultWriter(agent, new File(this.outputDirectory,  file + "." + DirectoryUtils.getTimestamp(System.currentTimeMillis()) + ".csv"));
    }

    @Override
    public File getOutputDirectory()
    {
        return this.outputDirectory;
    }
    //endregion
}
