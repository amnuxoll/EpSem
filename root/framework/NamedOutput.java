package framework;

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeAll(){
        for(OutputStream stream: this.outputStreams.values()){
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //endregion
}
