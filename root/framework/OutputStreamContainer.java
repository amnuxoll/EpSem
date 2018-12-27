package framework;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;

public class OutputStreamContainer {
    private static OutputStreamContainer instance = new OutputStreamContainer();

    private HashMap<String, OutputStream> outputStreams = new HashMap<>();

    private OutputStreamContainer() { }

    public static OutputStreamContainer getInstance()
    {
        return instance;
    }

    public void configureOutput(String key, OutputStream stream)
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
}
