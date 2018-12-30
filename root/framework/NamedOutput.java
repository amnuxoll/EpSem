package framework;

import utils.ExceptionUtils;
import java.io.*;
import java.util.HashMap;

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
    public static NamedOutput getInstance()
    {
        return instance;
    }
    //endregion

    //region Public Methods
    public void configure(String key, OutputStream stream)
    {
        this.outputStreams.put(key, stream);
    }

    public void write(String key, String data){
        if (!this.outputStreams.containsKey(key)) {
            this.outputStreams.put(key, System.out);
        }

        try {
            this.outputStreams.get(key).write(data.getBytes());
        } catch (IOException ex) {
            // To avoid any possible exception recursion we'll print internal errors directly to std out.
            ex.printStackTrace();
        }
    }

    public void write(String key, Exception exception) {
        this.write(key, "Exception logged: " + exception.getMessage());
        this.write(key, ExceptionUtils.getStacktrace(exception));
    }

    public void closeAll(){
        for(OutputStream stream: this.outputStreams.values()){
            try {
                stream.close();
            } catch (IOException ex) {
                // To avoid any possible exception recursion we'll print internal errors directly to std out.
                ex.printStackTrace();
            }
        }
    }
    //endregion
}
