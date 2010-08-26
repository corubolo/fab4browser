package multivalent;

import com.im.file.FileMagic;
import multivalent.IDInfo.Confidence;
import org.simplx.args.MainArgs;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import static multivalent.IDInfo.Confidence.*;

public class Identify {
    private static final Set<Constructor<? extends MediaAdaptor>> ADAPTORS;

    private static final Logger logger = Logger.getLogger(
            Identify.class.getCanonicalName());

    static {
        Multivalent mv = Multivalent.getInstance();
        Set<String> engineNames = mv.getEngineNames();
        ADAPTORS = new HashSet<Constructor<? extends MediaAdaptor>>(
                engineNames.size());
        for (String name : engineNames) {
            logger.finer("Type: \"" + name + "\"");
            try {
                Class type = Class.forName(name);
                if (MediaAdaptor.class.isAssignableFrom(type)) {
                    ADAPTORS.add(type.getConstructor());
                } else {
                    logger.finer("    not subtype of MediaAdaptor");
                }
            } catch (Exception e) {
                logger.finer("    " + e);
            }
        }
    }

    /**
     * Run this class as a program
     *
     * @param args The command line arguments.
     *
     * @throws Exception Exception we don't recover from.
     */
    public static void main(String[] args) throws Exception {
        //noinspection finally
        try {
            MainArgs cmdLine = new MainArgs("identify", args);
            Confidence min = cmdLine.getEnumValue("min", SUFFIX);
            Confidence max = cmdLine.getEnumValue("max", MAXIMUM);
            String[] paths = cmdLine.getOperands("file ...");

            for (String path : paths) {
                System.out.println(path + ":");
                try {
                    IDInfo[] infos = identify(min, max, path);
                    System.out.println("Info count: " + infos.length);
                    for (IDInfo info : infos) {
                        System.out.println("    " + info);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.flush();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    public static IDInfo[] identify(Confidence min, Confidence max, String path)
            throws IOException {

        SortedSet<IDInfo> infos = new TreeSet<IDInfo>(IDInfo.BEST_FIRST);
        for (Constructor<? extends MediaAdaptor> ctor : ADAPTORS) {
            try {
                MediaAdaptor adaptor = ctor.newInstance();
                adaptor.setInput(new File(path));
                Set<IDInfo> newInfos = adaptor.getTypeInfo(min, max, path,
                        true);
                for (IDInfo info : newInfos) {
                    logger.finer(ctor.getClass().getName() + ": " + info);
                    if (info != null)
                        infos.add(info);
                }
                infos.addAll(newInfos);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        if (MediaAdaptor.inRange(min, MAGIC, max)) {
            FileMagic file1 = new FileMagic("--mime", "-b", path);
            StringWriter out = new StringWriter();
            file1.setOutput(out);
            file1.execute();
            String mime = out.toString().trim();

            file1 = new FileMagic("-b", path);
            out = new StringWriter();
            file1.setOutput(out);
            file1.execute();
            String desc = out.toString().trim();

            infos.add(new IDInfo(MAGIC, file1, mime, null, desc));
        }
        //FileMagic file1 = new FileMagic();
        return infos.toArray(new IDInfo[infos.size()]);
    }
}