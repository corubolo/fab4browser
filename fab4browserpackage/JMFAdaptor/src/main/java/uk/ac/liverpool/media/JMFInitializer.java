/**
 * Class to initialise the JMF registry on webstart
 * Copied from here 
 * see:
 * http://www.randelshofer.ch/blog/2008/12/getting-jmf-to-work-with-java-web-start/
 * 
 */

package uk.ac.liverpool.media;

import com.sun.media.util.Registry;
import java.io.*;
import java.util.*;

public class JMFInitializer {
    
    
    public static boolean initJMF() {
        boolean success = false;
        InputStream in = null;
        try {
            in = JMFInitializer.class.getResourceAsStream("/jmf.properties");
            success = readJMFRegistry(in);
        } finally {
           if (in != null) {
               try {
                   in.close();
               } catch (IOException e) {}
           }
        }
        return success;
    }

    private static boolean readJMFRegistry(InputStream ris) {
        if (ris == null) {
            return false;
        }

        try {
            ObjectInputStream ois = new ObjectInputStream(ris);

            int tableSize = ois.readInt();
            int version = ois.readInt();
            if (version > 200) {
                System.err.println("Version number mismatch.nThere could be" +
" errors in reading the registry");
            }
            HashMap<String, Object> hash = new HashMap<String, Object>();
            for (int i = 0; i < tableSize; i++) {
                String key = ois.readUTF();
                boolean failed = false;
                byte[] serObject;
                try {
                     Object value = ois.readObject();
                     hash.put(key, value);
                } catch (ClassNotFoundException cnfe) {
                     failed = true;
                } catch (OptionalDataException ode) {
                     failed = true;
                }
            }
            ois.close();
            ris.close();
            for (Map.Entry<String, Object> entry : hash.entrySet()) {
                 Registry.set(entry.getKey(), entry.getValue());
             }

         } catch (IOException ioe) {
             System.err.println("IOException in readJMFRegistry: " + ioe);
             return false;
         } catch (Throwable t) {
             return false;
         }
         return true;
    }
}
