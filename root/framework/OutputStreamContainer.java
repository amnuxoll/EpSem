package framework;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;

public class OutputStreamContainer extends HashMap<String, OutputStream>{
    private String parentPath;

    /**
     * contruct a output stream container with a parent diretory
     * @param parentPath the path to put all files
     */
    public OutputStreamContainer(String parentPath){
        super();
        this.parentPath= parentPath;
    }

    public OutputStreamContainer(){
        this(".");
    }

    public void write(Object key, String data){
        if(!super.containsKey(key)){
            return;
        }

        try {
            get(key).write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * create a file output stream with the given path
     *
     * @param key the key to use for this output stream
     * @param path the path to the output stream
     *
     * @throws IOException if a file output stream could not be created
     */
    public void put(String key, String path) throws IOException {
        path=  Paths.get(this.parentPath, path).toString();
        File file= new File(path);
        File parentFile = file.getParentFile();
        if (parentFile != null)
            parentFile.mkdirs();
        file.createNewFile();
        this.put(key, new FileOutputStream(file));
    }

    public void closeAll(){
        for(OutputStream stream: values()){
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
