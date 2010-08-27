package multivalent.std.adaptor;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Feb 22, 2010
 */


import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.util.Arrays;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
//import org.mozilla.universalchardet.UniversalDetector;

public class CodepageDetector {


    /**
     * This class represents the best guess from the code page identification algorithms.
     * It also contains a {@link pb} since the algorithms will consume part of the original input stream.
     */
    public static class Match {
        public Match() {

        }

        public String encoding;
        /**
         * ISO code for detected language
         */
        public String language;
        /**
         * A [0,1] confidence value on the match
         */
        public double confidence;
        /**
         * a rewinded input stream used to detect the characters
         */
        public PushbackInputStream pb;

    }

    /**
     * The type of detector to be used. Currently, only the IBM_ICUJ is integrated.
     */
    public enum DetectorType {
        IBM_ICUJ, MOZILLA_UNIVERSAL, MOZILLA_CHARDEC
    }

    public static final DetectorType DEFAULT_DETECTOR = DetectorType.IBM_ICUJ;

    private static DetectorType t = DEFAULT_DETECTOR;


    public static void setDetectorType(DetectorType tt) {
        t = tt;
    }


    public static DetectorType getDetectorType() {
        return t;
    }

    /**
     * Identifies the codepage of the text or markup in the provided input stream.
     *
     * @param is       The text or markup input stream, that will be consumed. A Pushback Input stream will be returned in Match for full access.
     * @param declared The declared codepage, for example from HTTP headers or HTML meta information.
     * @param markup Indicates the codepage identification algorithms to ignore markup in the document. Set to true if the content is markup (HTML or XML)
     * @return The best {@link Match} from the defined method.
     * @throws IOException
     */
    static public Match detectCodepage(InputStream is, String declared, boolean markup) throws IOException {
        /* we want to be able to reuse the content input stream */
        PushbackInputStream p = new PushbackInputStream(is, 8000);
        Match m = null;
        byte[] buff = new byte[8000];
        DataInputStream i;
        i = new DataInputStream(p);
        int c = i.read(buff);


        switch (t) {
            case IBM_ICUJ:
                m = detectICU(buff,c, declared, markup);
            case MOZILLA_UNIVERSAL:
                detectMozillaUniversal(buff,c );
                break;
            case MOZILLA_CHARDEC:
                detectMozillaChardec(buff,c );
                break;
            default:
                break;
        }
        /* even when there is no match, we want to return a usable input stream */
        if (m != null) {
            m.pb = p;
        } else {
            m = new Match();
            m.pb = p;
        }
        return m;
    }


    private static void detectMozillaChardec(byte[] buff, int c) {

        // Not implemented since superceeded by the Universal version
    }


    private static Match detectMozillaUniversal(byte[] buff, int c) {
//        UniversalDetector det = new UniversalDetector(null);
//        det.handleData(buff, 0, c);
//        det.dataEnd();
//        String encoding = det.getDetectedCharset();
//        if (encoding == null)
//            return null;
//        Match im = new Match();
//        im.confidence = 50.0 / 100.0;
//        im.language = null;
//        im.encoding = encoding;
//        System.out.println("ENC: " + encoding);
//        return im;
        return null;
    }


    private static Match detectICU(byte[] buff, int c, String enc, Boolean markup)  {
        CharsetDetector cd = new CharsetDetector();
        if (buff.length != c)
            buff= Arrays.copyOfRange(buff, 0, c);
        cd.setText(buff);
        if (enc !=null)
            cd.setDeclaredEncoding(enc);
        cd.enableInputFilter(markup);
        CharsetMatch m = cd.detect();
        Match im = new Match();
        im.confidence = m.getConfidence() / 100.0;
        im.language = m.getLanguage();
        im.encoding = m.getName();
        return im;
    }
}
