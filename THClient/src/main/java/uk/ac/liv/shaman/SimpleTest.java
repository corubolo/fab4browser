package uk.ac.liv.shaman;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;

import uk.ac.liverpool.shaman.thumbnailserviceclient.FontInformation;
import uk.ac.liverpool.shaman.thumbnailserviceclient.IOException_Exception;
import uk.ac.liverpool.shaman.thumbnailserviceclient.MalformedURLException_Exception;
import uk.ac.liverpool.shaman.thumbnailserviceclient.ThumbnailServiceService;

public class SimpleTest {

    public static void main(String[] args) throws IOException,
    IOException_Exception, MalformedURLException_Exception {
        ThumbnailServiceService s = new ThumbnailServiceService(
                new URL(
                        "http://localhost:8080/Thumbnails/Thumbnails"),
                        // "http://bodoni.lib.liv.ac.uk:8080/Thumbnails-0.0.1-SNAPSHOT/Thumbnails"),
                        new QName("http://shaman.liv.ac.uk/", "ThumbnailServiceService"));
        FileOutputStream f = new FileOutputStream("out.png");
        //String arg0 = "http://www.ctan.org/tex-archive/info/lshort/english/lshort.pdf";
//        f.write(s.getThumbnailServicePort().generateThumbnail(
//                arg0, 300, 600,
//                "png", "", 1));
        File source = new File("/Users/fabio/Downloads/1--976069938.pdf");
        DataInputStream di = new DataInputStream(new FileInputStream(source));
       byte[] data = new byte[(int)source.length()];
        di.readFully(data);
        System.out.println(data.length);
        f.write(s.getThumbnailServicePort().generateThumbnailFromData(
                data, 300, 600,
                "png", "", 0));
        List<String> ss = s.getThumbnailServicePort().getSupportedOutputType();
        ss = s.getThumbnailServicePort().getSupportedMimeTypes();

//        System.out.println(s.getThumbnailServicePort().extraxtXmlText(
//                arg0, 300, 600,
//                "png", "", 1));
        List<FontInformation> fi =   s.getThumbnailServicePort().extractFontInformationFromData(
                data);
        
        
        for (FontInformation fs: fi){
            printModel(System.out, fs);
        }
        // "file:///Users/fabio/Lisbon_1page.ppt"

        // "http://media.ugs.com/teamcenter/jtfiles/conrod.jt"
    }

    public static void printModel (PrintStream ps, Object o){
        Method[] fi = o.getClass().getDeclaredMethods();
        for (Method f: fi){
            if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers()) && ( f.getName().startsWith("get") || f.getName().startsWith("is"))) {
                ps.print(f.getName().substring(3) + " = ");
                try {
                    ps.println(f.invoke(o));
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
        ps.println();
    }
}
