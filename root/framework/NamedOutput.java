package framework;

import java.io.*;
import java.util.HashMap;

public class NamedOutput {
    private static NamedOutput instance = new NamedOutput();

    private HashMap<String, OutputStream> outputStreams = new HashMap<>();

    private NamedOutput() { }

    public static NamedOutput getInstance()
    {
        return instance;
    }

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
}
