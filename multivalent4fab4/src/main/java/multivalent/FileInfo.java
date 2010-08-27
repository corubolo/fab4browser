package multivalent;

import java.io.File;

public class FileInfo {
    /**
     * Run this class as a program
     *
     * @param args The command line arguments.
     *
     * @throws Exception Exception we don't recover from.
     */
    public static void main(String[] args) throws Exception {
        for (String path : args) {
            File file = new File(path);
            System.out.println("file = " + file);
        }
    }
}