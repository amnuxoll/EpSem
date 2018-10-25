package framework;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class OutputStreamContainer extends HashMap<String, OutputStream>{
    /**
     * contruct a output stream container with an initial pair
     * @param key the key string
     * @param val the stream to associate with key
     */
    public OutputStreamContainer(String key, OutputStream val){
        super();
        put(key,val);
    }

    @Override
    public OutputStream get(Object str){
        if(super.get(str) == null){
            super.put((String)str, System.out);
        }

        return super.get(str);
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
