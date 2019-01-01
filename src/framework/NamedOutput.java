package framework;

import utils.ExceptionUtils;
import java.io.*;
import java.util.HashMap;

/**
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
    public static NamedOutput getInstance() {
        return NamedOutput.instance;
    }
    //endregion

    //region Public Methods
    public void configure(String key, OutputStream stream) {
        if (key == null || key == "")
            throw new IllegalArgumentException("key cannot be empty or null");
        if (stream == null)
            throw new IllegalArgumentException("stream cannot be null.");
        this.outputStreams.put(key, stream);
    }

    public void writeLine(String key) {
        this.writeLine(key, "");
    }

    public void writeLine(String key, String data) {
        if (data == null)
            throw new IllegalArgumentException("data cannot be null.");
        this.write(key, data + "\n");
    }

    public void write(String key, String data) {
        if (key == null || key == "")
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

    public void write(String key, Exception exception) {
        if (exception == null)
            throw new IllegalArgumentException("exception cannot be null.");
        this.writeLine(key, "Exception logged: " + exception.getMessage());
        this.writeLine(key, ExceptionUtils.getStacktrace(exception));
    }

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
