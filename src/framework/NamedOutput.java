package framework;

import utils.ExceptionUtils;
import java.io.*;
import java.util.HashMap;

/**
 * {@link NamedOutput} is a singleton used to categorize general output. Further, should you wish to
 * generate longer-term general output by an agent or environment, a tag may be configured to write
 * to a file that is configured before a test run begins.
 *
 * This may be used as follows:
 *
 * 1. Writing to terminal
 * If used like this (with no additional configuration), {@link NamedOutput} will transparently route
 * the output to the terminal.
 * NamedOutput.getInstance().writeLine("myOutputTag", "My output is written to the terminal.");
 *
 * 2. Writing to a file (or any {@link OutputStream}
 * If used like this, any input will be streamed to the configured target.
 * NamedOutput.getInstance().configure("myOutputTag", new FileOutputStream(new File("output.txt")));
 * NamedOutput.getInstance().writeLine("myOutputTag", "My output is written to a file named output.txt.");
 *
 * Option 1 can be used to disambiguate data flows through an agent's logic and Option 2 can then track
 * specific flows in a file or even be used to save internal state at various points in an agent's lifetime
 * using the stat-gathering callbacks.
 *
 * The {@link TestSuite} contains a callback TestSuite.beforeRun that allows the files configured in Option 2
 * to be saved in the same directory as the standard framework statistical data.
 *
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class NamedOutput {

    //region Static Variables

    private static NamedOutput instance = new NamedOutput();

    //endregion

    //region Class Variables

    private HashMap<String, OutputStream> outputStreams = new HashMap<>();

    //endregion

    //region Constructors

    private NamedOutput() { }

    //endregion

    //region Public Static Methods

    /**
     * Gets the {@link NamedOutput} singleton.
     *
     * @return the singleton instance of {@link NamedOutput}.
     */
    public static NamedOutput getInstance() {
        return NamedOutput.instance;
    }

    //endregion

    //region Public Methods

    /**
     * Configures a specific output tag to write to the givenn {@link OutputStream}.
     *
     * @param key the tag to configure.
     * @param stream the stream to route output to.
     */
    public void configure(String key, OutputStream stream) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key cannot be empty or null");
        if (stream == null)
            throw new IllegalArgumentException("stream cannot be null.");
        this.outputStreams.put(key, stream);
    }

    /**
     * Writes an empty line for the given key.
     *
     * @param key the tag to write an empty line to.
     */
    public void writeLine(String key) {
        this.writeLine(key, "");
    }

    /**
     * Writes the given data and newline for the given key.
     *
     * @param key the tag to write the data to.
     * @param data the data to write.
     */
    public void writeLine(String key, String data) {
        if (data == null)
            throw new IllegalArgumentException("data cannot be null.");
        this.write(key, data + "\n");
    }

    /**
     * Writes the given data for the given key.
     *
     * @param key the tag to write the data to.
     * @param data the data to write.
     */
    public void write(String key, String data) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key cannot be null or empty.");
        if (data == null)
            throw new IllegalArgumentException("data cannot be null.");
        try {
            OutputStream stream = System.out;
            if (this.outputStreams.containsKey(key))
                stream = this.outputStreams.get(key);
            stream.write(data.getBytes());
        } catch (Exception ex) {
            // To avoid any possible exception recursion we'll print internal errors directly to std out.
            ex.printStackTrace();
        }
    }

    /**
     * Writes the given exception for the given key.
     *
     * @param key the tag to write the exception to.
     * @param exception the exception to write.
     */
    public void write(String key, Exception exception) {
        if (exception == null)
            throw new IllegalArgumentException("exception cannot be null.");
        this.writeLine(key, "Exception logged: " + exception.getMessage());
        this.writeLine(key, ExceptionUtils.getStacktrace(exception));
    }

    /**
     * Closes all configured streams.
     */
    public void closeAll() {
        for(OutputStream stream: this.outputStreams.values()){
            try {
                stream.close();
            } catch (Exception ex) {
                // To avoid any possible exception recursion we'll print internal errors directly to std out.
                ex.printStackTrace();
            }
        }
        this.outputStreams.clear();
    }

    //endregion
}
