package org.simplx.args;

import org.simplx.args.advanced.CommandLineAdvanced;
import org.simplx.args.advanced.CommandOptValues;
import org.simplx.args.advanced.ObjectMapper;
import org.simplx.logging.BasicFormatter;
import org.simplx.logging.SimplxLogging;
import org.simplx.object.Reflx;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static java.util.logging.Level.ALL;
import static java.util.logging.Level.INFO;
import static org.simplx.args.TestCommandLine.Thing.*;
import static org.simplx.testng.Assert.*;

@SuppressWarnings({"FieldMayBeFinal"})
public class TestCommandLine {
    private static final Pattern TO_ESCAPE = Pattern.compile("[\\s\\\\]");
    public enum Thing {
        HORSE, BOX, CONCEPT, TOE_GUARD_HOLDER, CONCERT
    }

    private static final Pattern USAGE_PATTERN = Pattern.compile("\nusage:\n",
            Pattern.MULTILINE);

    private static final String[] TEST_MULTI_ARGS =
            {"--boolT", "--boolF", "--charV", "\\u0041", "--strV",
                    "¡Hola Jose!", "--byteV", "-3", "--shortV", "+3592",
                    "--intV", "-431666", "--longV", "+99", "--floatV", "1.9e15",
                    "--doubleV", "+Infinity", "--thingV", "Ho", "--fileV", ".",
                    "--readV", "-", "--writeV", "-1", "--inV", "-", "--outV",
                    "-2", "--valsV", "3,2", "--valsV", "1"};

    private static final String[] TEST_SINGLE_ARGS =
            {"-bB", "-c", "\\u0041", "-S", "¡Hola Jose!", "-y", "-3", "-s",
                    "+3592", "-i", "-431666", "-l", "+99", "-f", "1.9e15", "-d",
                    "+Infinity", "-t", "Ho", "-F", ".", "-r", "-", "-w", "-1",
                    "-I", "-", "-O", "-2", "-v", "3,2", "-v", "1"};

    private static final String[] TEST_MULTI_ONLY_ARGS =
            {"--boolT", "--boolF", "--charV", "\\u0041", "--strV",
                    "¡Hola Jose!", "--byteV", "-3", "--shortV", "+3592",
                    "--intV", "-431666", "--longV", "+99", "--floatV", "1.9e15",
                    "--doubleV", "+Infinity", "--thingV", "Ho", "--fileV", ".",
                    "--readV", "-", "--writeV", "-1", "--inV", "-", "--outV",
                    "-2", "--valsV", "3,2", "--valsV", "1"};

    private static final String[][] WHITESPACES =
            {{" "}, {"\t"}, {"\n"}, {" ", "  ", "   ", "\n"}, {"\t \t", "\n"},};

    @SuppressWarnings({"InnerClassMayBeStatic"})
    class ArrayFields {
        @CommandOpt(single = 'v', deflt = "-1,-2,-3")
        int[] vals;

        @CommandOpt(single = 'b', deflt = "1")
        byte[] bytes;

        @CommandOpt(single = 'l', deflt = "")
        long[] longs;

        @CommandOpt(single = 'f')
        float[] floats;
    }

    // Booleans are different.  For everything else, the suffixes mean:
    //    V means a value will be specified
    //    D means a value will be set from the def()
    //    N means no value will be set
    private static class BoolTest {
        @CommandOpt(deflt = "false", single = 'b',
                desc = "a boolean value that defaults to 'true'")
        private boolean boolT;
        @CommandOpt(deflt = "true", single = 'B',
                desc = "a boolean value that defaults to 'false'. This also" +
                        " has a lot of extra text so that we can see how" +
                        " wrapping is handled, which we hope will be handled" +
                        " correctly")
        boolean boolF;
        @CommandOpt(deflt = "true")
        protected boolean boolDT;
        @CommandOpt(deflt = "false")
        public boolean boolDF;
        @CommandOpt
        boolean boolN;
    }

    private static class BigTest extends BoolTest {
        @CommandOpt(single = 'c', deflt = "%")
        private char charV;
        @CommandOpt(deflt = "\\u004b",
                desc = "A character that might be any darn thing.")
        private char charD;
        @CommandOpt(deflt = CommandOpt.NO_DEFAULT)
        private char charN = 'q';

        @CommandOpt(single = 'S', deflt = "?????")
        String strV;
        @CommandOpt(deflt = "")
        String strD;
        @CommandOpt
        String strN;

        @CommandOpt(single = 'y', deflt = "77")
        protected byte byteV;
        @CommandOpt(deflt = "-1")
        protected byte byteD;
        @CommandOpt
        protected byte byteN = 1;
        @CommandOpt(single = 's', deflt = "11")
        protected short shortV;
        @CommandOpt(deflt = "-1")
        protected short shortD;
        @CommandOpt
        protected short shortN = 1;
        @CommandOpt(single = 'i', deflt = "22")
        protected int intV;
        @CommandOpt(deflt = "-1")
        protected int intD;
        @CommandOpt
        protected int intN = 1;
        @CommandOpt(single = 'l', deflt = "33")
        public long longV;
        @CommandOpt(deflt = "-1")
        public long longD;
        @CommandOpt
        public long longN = 1;
        @CommandOpt(single = 'f', deflt = "-99.9e9")
        float floatV;
        @CommandOpt(deflt = "-Infinity")
        float floatD;
        @CommandOpt
        float floatN = 1;
        @CommandOpt(single = 'd', deflt = "NaN")
        double doubleV;
        @CommandOpt(deflt = "NaN")
        double doubleD;
        @CommandOpt
        double doubleN = 1;

        @CommandOpt(single = 't', deflt = "BOX")
        Thing thingV;
        @CommandOpt(deflt = "t")
        Thing thingD;
        @CommandOpt
        Thing thingN = BOX;

        @CommandOpt(single = 'F', deflt = "NO_SUCH_FILE")
        File fileV;
        @CommandOpt(deflt = ".")
        File fileD;
        @CommandOpt
        File fileN;

        @CommandOpt(mode = "unBuffered", single = 'r')
        Reader readV;
        @CommandOpt(deflt = "-")
        Reader readD;
        @CommandOpt
        Reader readN;

        @CommandOpt(mode = "U", single = 'w')
        Writer writeV;
        @CommandOpt(deflt = "-")
        Writer writeD;
        @CommandOpt
        Writer writeN;

        @CommandOpt(mode = "unbuff", single = 'I')
        InputStream inV;
        @CommandOpt(deflt = "-")
        InputStream inD;
        @CommandOpt
        InputStream inN;

        @CommandOpt(mode = "u", single = 'O', deflt = "-1")
        OutputStream outV;
        @CommandOpt(deflt = "-2")
        OutputStream outD;
        @CommandOpt
        OutputStream outN;
// Need a real file for this, which is a bit complicated -- test separately
//        @MainArg
        //        RandomAccessFile randV;
//        @MainArg(def = "-")
//        RandomAccessFile randD;
        @CommandOpt
        RandomAccessFile randN;

        @CommandOpt(single = 'v', deflt = "-1,-2,-3")
        int[] valsV;
        @CommandOpt(deflt = "-1,-2,-3")
        int[] valsD;
        @CommandOpt
        int[] valsN;

        Object notouch;
    }

    private static class BoolTestMultiOnly {
        @CommandOpt(deflt = "false")
        private boolean boolT;
        @CommandOpt(deflt = "true")
        boolean boolF;
        @CommandOpt(deflt = "true")
        protected boolean boolDT;
        @CommandOpt(deflt = "false")
        public boolean boolDF;
        @CommandOpt
        boolean boolN;
    }

    private static class BigTestMultiOnly extends BoolTestMultiOnly {
        @CommandOpt
        private char charV;
        @CommandOpt(deflt = "\\u004b")
        private char charD;
        @CommandOpt(deflt = CommandOpt.NO_DEFAULT)
        private char charN = 'q';

        @CommandOpt
        String strV;
        @CommandOpt(deflt = "")
        String strD;
        @CommandOpt
        String strN;

        @CommandOpt
        protected byte byteV;
        @CommandOpt(deflt = "-1")
        protected byte byteD;
        @CommandOpt
        protected byte byteN = 1;
        @CommandOpt
        protected short shortV;
        @CommandOpt(deflt = "-1")
        protected short shortD;
        @CommandOpt
        protected short shortN = 1;
        @CommandOpt
        protected int intV;
        @CommandOpt(deflt = "-1")
        protected int intD;
        @CommandOpt
        protected int intN = 1;
        @CommandOpt
        public long longV;
        @CommandOpt(deflt = "-1")
        public long longD;
        @CommandOpt
        public long longN = 1;
        @CommandOpt
        float floatV;
        @CommandOpt(deflt = "-Infinity")
        float floatD;
        @CommandOpt
        float floatN = 1;
        @CommandOpt
        double doubleV;
        @CommandOpt(deflt = "NaN")
        double doubleD;
        @CommandOpt
        double doubleN = 1;

        @CommandOpt
        Thing thingV;
        @CommandOpt(deflt = "t")
        Thing thingD;
        @CommandOpt
        Thing thingN = BOX;

        @CommandOpt
        File fileV;
        @CommandOpt(deflt = ".")
        File fileD;
        @CommandOpt
        File fileN;

        @CommandOpt(mode = "unBuffered")
        Reader readV;
        @CommandOpt(deflt = "-")
        Reader readD;
        @CommandOpt
        Reader readN;

        @CommandOpt(mode = "U")
        Writer writeV;
        @CommandOpt(deflt = "-")
        Writer writeD;
        @CommandOpt
        Writer writeN;

        @CommandOpt(mode = "unbuff")
        InputStream inV;
        @CommandOpt(deflt = "-")
        InputStream inD;
        @CommandOpt
        InputStream inN;

        @CommandOpt(mode = "u")
        OutputStream outV;
        @CommandOpt(deflt = "-2")
        OutputStream outD;
        @CommandOpt
        OutputStream outN;
// Need a real file for this, which is a bit complicated -- test separately
//        @MainArg
        //        RandomAccessFile randV;
//        @MainArg(def = "-")
//        RandomAccessFile randD;
        @CommandOpt
        RandomAccessFile randN;

        @CommandOpt(deflt = "-1,-2,-3")
        int[] valsV;
        @CommandOpt(deflt = "-1,-2,-3")
        int[] valsD;
        @CommandOpt
        int[] valsN;

        Object notouch;
    }

    private static class StaticTop {
        @CommandOpt
        private static int topVal;
    }
    private static class StaticMid extends StaticTop {
        @CommandOpt
        private static int midVal;
    }
    private static class StaticLeaf extends StaticMid {
        @CommandOpt
        private static int leafVal;
    }

    @Test
    public void quickTest() {
        class Opts extends Reflx<Opts> {
            @CommandOpt
            public boolean bool;

            @CommandOpt
            public String str;

            @CommandOpt
            public String noDef;

            @CommandOpt(deflt = "Yay")
            public String hasDef;
        }

        Logger logger = SimplxLogging.loggerFor(CommandLine.class);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(ALL);
        Formatter formatter = new BasicFormatter();
        formatter.format(new LogRecord(INFO, "hello"));
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        Opts opts = new Opts();
        List<String> operands = new CommandLine(opts).apply("-bool", "-str",
                "string");

        assertTrue(opts.bool);
        assertEquals(opts.str, "string");
        assertNull(opts.noDef);
        assertEquals(opts.hasDef, "Yay");
        assertEquals(0, operands.size());
    }

    @Test
    public void bigTestMulti() {
        BigTest test = new BigTest();
        CommandLine line = new CommandLine(test);
        line.apply(TEST_MULTI_ARGS);

        validateBigTest(test);
    }

    @DataProvider(name = "fileText")
    public Iterator<Object[]> fileText() {
        List<Object[]> contents = new ArrayList<Object[]>();
        for (String[] spaces : WHITESPACES) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (String testMultiArg : TEST_MULTI_ARGS) {
                sb.append(spaces[i++]);
                if (i >= spaces.length)
                    i = 0;
                sb.append(TO_ESCAPE.matcher(testMultiArg).replaceAll("\\\\$0"));
            }
            sb.append(spaces[i]);
            addArray(contents, sb.toString());
        }
        return contents.iterator();
    }

    @Test(dataProvider = "fileText")
    public void testReadFile(String text) throws IOException {
        File f = File.createTempFile("scl", ".txt");
        Writer out = null;
        try {
            out = new FileWriter(f);
            out.write(text);
        } finally {
            if (out != null)
                out.close();
        }

        BigTest opts = new BigTest();
        CommandLine line = new CommandLine(opts);
        line.apply("-@", f.getPath());
        validateBigTest(opts);
    }

    @Test
    public void bigTestSingle() {
        BigTest test = new BigTest();
        CommandLine line = new CommandLine(test);
        line.apply(TEST_SINGLE_ARGS);

        validateBigTest(test);
    }

    @Test
    public void bigTestSingleMerged() {
        BigTest test = new BigTest();
        CommandLine line = new CommandLine(test);
        List<String> argList = new ArrayList<String>();
        argList.add(TEST_SINGLE_ARGS[0] + TEST_SINGLE_ARGS[1].substring(1) +
                TEST_SINGLE_ARGS[2]);
        for (int i = 3; i < TEST_SINGLE_ARGS.length; i += 2) {
            argList.add(TEST_SINGLE_ARGS[i] + TEST_SINGLE_ARGS[i + 1]);
        }
        String[] args = argList.toArray(new String[argList.size()]);
        line.apply(args);

        validateBigTest(test);
    }

    private void validateBigTest(BigTest test) {
        assertTrue(((BoolTest) test).boolT);
        assertFalse(test.boolF);
        assertTrue(test.boolDT);
        assertFalse(test.boolDF);
        assertFalse(test.boolN);

        assertEquals(test.charV, 'A');
        assertEquals(test.charD, 'K');
        assertEquals(test.charN, 'q');

        assertEquals(test.strV, "¡Hola Jose!");
        assertEquals(test.strD, "");
        assertEquals(test.strN, null);

        assertEquals(test.byteV, -3);
        assertEquals(test.byteD, -1);
        assertEquals(test.byteN, 1);

        assertEquals(test.shortV, 3592);
        assertEquals(test.shortD, -1);
        assertEquals(test.shortN, 1);

        assertEquals(test.intV, -431666);
        assertEquals(test.intD, -1);
        assertEquals(test.intN, 1);

        assertEquals(test.longV, 99);
        assertEquals(test.longD, -1);
        assertEquals(test.longN, 1);

        assertEquals(test.floatV, 1.9e15f, 1.0f);
        assertEquals(test.floatD, Float.NEGATIVE_INFINITY);
        assertEquals(test.floatN, 1.0f, 1.0e-10f);

        assertEquals(test.doubleV, Double.POSITIVE_INFINITY);
        assertTrue(Double.isNaN(test.doubleD));
        assertEquals(test.doubleN, 1.0, 1.0e-10);

        assertEquals(test.thingV, HORSE);
        assertEquals(test.thingD, TOE_GUARD_HOLDER);
        assertEquals(test.thingN, BOX);

        assertEquals(test.fileV.getPath(), ".");
        assertEquals(test.fileD.getPath(), ".");
        assertNull(test.fileN);

        assertSame(test.readV.getClass(), InputStreamReader.class);
        assertSame(test.readD.getClass(), BufferedReader.class);
        assertNull(test.readN);

        assertSame(test.writeV.getClass(), OutputStreamWriter.class);
        assertSame(test.writeD.getClass(), BufferedWriter.class);
        assertNull(test.writeN);

        assertSame(test.inV, System.in);
        assertNotSame(test.inD, System.in);
        assertSame(test.inD.getClass(), BufferedInputStream.class);
        assertNull(test.inN);

        assertSame(test.outV, System.err);
        assertNotSame(test.outD, System.err);
        assertSame(test.outD.getClass(), BufferedOutputStream.class);
        assertNull(test.outN);

        assertNull(test.randN);

        assertEquals(test.valsV, new int[]{3, 2, 1});
        assertEquals(test.valsD, new int[]{-1, -2, -3});
        assertNull(test.valsN);

        assertNull(test.notouch);
    }

    @Test
    public void bigTestMultiOnly() {
        oneMultiTest(TEST_MULTI_ONLY_ARGS);

        String[] oneDashArgs = TEST_MULTI_ARGS.clone();
        for (int i = 0; i < oneDashArgs.length; i++) {
            if (oneDashArgs[i].startsWith("--"))
                oneDashArgs[i] = oneDashArgs[i].substring(1);
        }
        oneMultiTest(oneDashArgs);
    }

    private void oneMultiTest(String[] args) {
        BigTestMultiOnly test = new BigTestMultiOnly();
        CommandLine line = new CommandLine(test);
        line.apply(args);
        validateBigTest(test);
    }

    private void validateBigTest(BigTestMultiOnly test) {
        assertTrue(((BoolTestMultiOnly) test).boolT);
        assertFalse(test.boolF);
        assertTrue(test.boolDT);
        assertFalse(test.boolDF);
        assertFalse(test.boolN);

        assertEquals(test.charV, 'A');
        assertEquals(test.charD, 'K');
        assertEquals(test.charN, 'q');

        assertEquals(test.strV, "¡Hola Jose!");
        assertEquals(test.strD, "");
        assertEquals(test.strN, null);

        assertEquals(test.byteV, -3);
        assertEquals(test.byteD, -1);
        assertEquals(test.byteN, 1);

        assertEquals(test.shortV, 3592);
        assertEquals(test.shortD, -1);
        assertEquals(test.shortN, 1);

        assertEquals(test.intV, -431666);
        assertEquals(test.intD, -1);
        assertEquals(test.intN, 1);

        assertEquals(test.longV, 99);
        assertEquals(test.longD, -1);
        assertEquals(test.longN, 1);

        assertEquals(test.floatV, 1.9e15f, 1.0f);
        assertEquals(test.floatD, Float.NEGATIVE_INFINITY);
        assertEquals(test.floatN, 1.0f, 1.0e-10f);

        assertEquals(test.doubleV, Double.POSITIVE_INFINITY);
        assertTrue(Double.isNaN(test.doubleD));
        assertEquals(test.doubleN, 1.0, 1.0e-10);

        assertEquals(test.thingV, HORSE);
        assertEquals(test.thingD, TOE_GUARD_HOLDER);
        assertEquals(test.thingN, BOX);

        assertEquals(test.fileV.getPath(), ".");
        assertEquals(test.fileD.getPath(), ".");
        assertNull(test.fileN);

        assertSame(test.readV.getClass(), InputStreamReader.class);
        assertSame(test.readD.getClass(), BufferedReader.class);
        assertNull(test.readN);

        assertSame(test.writeV.getClass(), OutputStreamWriter.class);
        assertSame(test.writeD.getClass(), BufferedWriter.class);
        assertNull(test.writeN);

        assertSame(test.inV, System.in);
        assertNotSame(test.inD, System.in);
        assertSame(test.inD.getClass(), BufferedInputStream.class);
        assertNull(test.inN);

        assertSame(test.outV, System.err);
        assertNotSame(test.outD, System.err);
        assertSame(test.outD.getClass(), BufferedOutputStream.class);
        assertNull(test.outN);

        assertNull(test.randN);

        assertEquals(test.valsV, new int[]{3, 2, 1});
        assertEquals(test.valsD, new int[]{-1, -2, -3});
        assertNull(test.valsN);

        assertNull(test.notouch);
    }

    @Test
    public void zeroLengthOp() {
        BigTest opts = new BigTest();
        CommandLine line = new CommandLine(opts);
        List<String> ops = line.apply("", "hello");
        assertEquals(ops.size(), 2);
        assertEquals(ops.get(0), "");
        assertEquals(ops.get(1), "hello");
    }

    @Test
    public void usageInException() {
        BigTest opts = new BigTest();
        CommandLine line = new CommandLine(opts);

        try {
            line.apply("-xyzzy");
            fail("accepted unknown option");
        } catch (CommandLineException e) {
            e.printStackTrace();
            // Check that the "usage" part appears exactly once
            assertEquals(USAGE_PATTERN.split(e.getMessage()).length, 2);
            assertEquals(USAGE_PATTERN.split(e.getLocalizedMessage()).length,
                    2);
        }
    }

    @Test
    public void testRepeatedBooleans() {
        tryRepatedBooleans("once", "--boolT", "--boolF", "--boolDT", "--boolDF",
                "--boolN");
        tryRepatedBooleans("twice", "--boolT", "--boolT", "--boolF", "--boolF",
                "--boolDT", "--boolDT", "--boolDF", "--boolDF", "--boolN",
                "--boolN");
    }

    private void tryRepatedBooleans(String desc, String... args) {
        BigTest opts = new BigTest();
        CommandLine line = new CommandLine(opts);
        line.apply(args);
        assertTrue(((BoolTest) opts).boolT, desc);
        assertFalse(opts.boolF, desc);
        assertTrue(opts.boolDF, desc);
        assertFalse(opts.boolDT, desc);
        assertTrue(opts.boolN, desc);
    }

    @Test
    public void noMulti() {
        class NoMulti {
            @CommandOpt(single = '1', multi = CommandOpt.NO_MULTI)
            boolean b1;
            @CommandOpt(single = '2', multi = CommandOpt.NO_MULTI)
            boolean b2;
        }

        NoMulti opts = new NoMulti();
        CommandLine line = new CommandLine(opts);
        line.apply("-12");
        assertTrue(opts.b1);
        assertTrue(opts.b2);

        try {
            String badOpt = "--" + CommandOpt.NO_MULTI;
            line.apply(badOpt);
            fail("accepted option '" + badOpt + "'");
        } catch (CommandLineException ignored) {
        }

        try {
            line.apply("--b1");
            fail("accepted option '--b1'");
        } catch (CommandLineException ignored) {
        }

        try {
            line.apply("-b1");
            fail("accepted option '-b1'");
        } catch (CommandLineException ignored) {
        }
    }

    @Test
    public void badArg() {
        class BadArg {
            @CommandOpt(multi = CommandOpt.NO_MULTI)
            boolean oops;
        }

        try {
            //noinspection ResultOfObjectAllocationIgnored
            new CommandLine(new BadArg());
            fail("accepted option with no usable name");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
    }

    @Test
    public void someMulti() {
        class SomeMulti {
            @CommandOpt(single = '1', multi = CommandOpt.NO_MULTI)
            boolean b1;
            @CommandOpt(single = '2')
            boolean b2;
        }

        SomeMulti opts = new SomeMulti();
        CommandLine line = new CommandLine(opts);
        line.apply("-12");
        assertTrue(opts.b1);
        assertTrue(opts.b2);

        opts.b2 = false;
        line.apply("--b2");
        assertTrue(opts.b2);

        try {
            line.apply("--b1");
            fail("accepted option '--b1'");
        } catch (CommandLineException ignored) {
        }

        try {
            line.apply("-b1");
            fail("accepted option '-b1'");
        } catch (CommandLineException ignored) {
        }
    }

    @Test
    public void testEnums() {
        BigTest test = new BigTest();
        CommandLine line = new CommandLine(test);
        line.apply("--thingV", "box", "--thingD", "ToeGuard_Holder", "--thingN",
                "HORSE");
        assertEquals(test.thingV, BOX);
        assertEquals(test.thingD, TOE_GUARD_HOLDER);
        assertEquals(test.thingN, HORSE);

        line.apply("--thingV", "concep", "--thingD", "concer");
        assertEquals(test.thingV, CONCEPT);
        assertEquals(test.thingD, CONCERT);

        try {
            line.apply("--thingV", "conce");
            fail("accepted ambiguous enum");
        } catch (CommandLineException e) {
            assertTrue(e.getMessage().contains("CONCERT"));
            assertTrue(e.getMessage().contains("CONCEPT"));
            assertFalse(e.getMessage().contains("HORSE"));
        }

        try {
            line.apply("--thingV", "qtip");
            fail("accepted unknown enum");
        } catch (CommandLineException ignored) {
        }
    }

    @Test
    public void testFile() throws IOException {
        class FileOpts {
            @CommandOpt(checks = "exists")
            File file;
            @CommandOpt(checks = "!exists")
            File notFile;
            @CommandOpt(checks = "isDirectory")
            File dir;
            @CommandOpt(checks = "!isDirectory")
            File notDir;
            @CommandOpt(checks = "mkdir")
            File makeDir;
            @CommandOpt(checks = "!isDirectory, mkdir, isDirectory")
            File makeAndTestDir;
            @CommandOpt(
                    checks = " exists ,!isDirectory,canRead,canWrite,isFile,\t   !\tisHidden,,!   canExecute")
            File several;
        }

        FileOpts test = new FileOpts();
        CommandLine line = new CommandLine(test);

        File tmpFile = File.createTempFile("tpa", ".txt");
        tmpFile.deleteOnExit();
        String fileName = tmpFile.toString();

        File tmpDir = File.createTempFile("tpa", ".dir");
        tmpDir.deleteOnExit();
        String dirName = tmpDir.toString();

        line.apply("-file", fileName);
        assertEquals(test.file, tmpFile);
        try {
            line.apply("-dir", fileName);
            fail("accepted non-dir: " + tmpFile);
        } catch (Exception ignored) {
            assertFalse(tmpDir.isDirectory());
        }
        line.apply("-notDir", fileName);
        assertEquals(test.notDir, tmpFile);
        line.apply("-several", fileName);
        assertEquals(test.several, tmpFile);

        assertTrue(tmpFile.delete());
        line.apply("-notFile", fileName);
        assertEquals(test.notFile, tmpFile);
        line.apply("-notDir", fileName);
        assertEquals(test.notDir, tmpFile);
        assertTrue(tmpDir.delete());
        line.apply("-notFile", dirName);
        assertEquals(test.notFile, tmpDir);
        line.apply("-notDir", dirName);
        assertEquals(test.notDir, tmpDir);

        line.apply("-makeDir", dirName);
        assertEquals(test.makeDir, tmpDir);
        assertTrue(tmpDir.isDirectory());

        // This should work even if the dir exists
        line.apply("-makeDir", dirName);
        assertEquals(test.makeDir, tmpDir);
        assertTrue(tmpDir.isDirectory());

        assertTrue(tmpDir.delete());
        line.apply("-makeAndTestDir", dirName);
        assertEquals(test.makeAndTestDir, tmpDir);
        assertTrue(tmpDir.isDirectory());
        assertTrue(tmpDir.delete());
    }

    @Test
    public void tryCustomMappers() {
        class CustomOpts {
            @CommandOpt(checks = "!isPositive")
            BigDecimal decimal;
        }

        CustomOpts test = new CustomOpts();
        CommandLine line = new CommandLine(test);
        CommandLineAdvanced adv = line.getAdvancedKnobs();
        line.setTryAllTypes(false);
        adv.addMapping(new ObjectMapper() {
            @Override
            public Boolean check(String checkName, Object val, Field field,
                    Class type, CommandOpt anno, CommandLine line) {

                if (checkName.equals("isPositive"))
                    return ((BigDecimal) val).compareTo(BigDecimal.ZERO) > 0;
                return null;
            }
        }, BigDecimal.class);
        String valueStr = "-1.23456789012345678901234567890";
        line.apply("-decimal", valueStr);
        assertEquals(test.decimal, new BigDecimal(valueStr));
    }

    @Test
    public void tryAllTypes() {
        class CustomOpts {
            @CommandOpt
            BigDecimal decimal;
        }

        CustomOpts test = new CustomOpts();
        CommandLine line = new CommandLine(test);
        String valueStr = "-1.23456789012345678901234567890";
        line.apply("-decimal", valueStr);
        assertEquals(test.decimal, new BigDecimal(valueStr));

        line.setTryAllTypes(false);
        try {
            line.apply("-decimal", valueStr);
            fail("Should not have accept unmapped type BigDecimal");
        } catch (CommandLineException e) {
            assertSame(e.getCause().getClass(), IllegalArgumentException.class);
        }
    }

    @Test
    public void testEndDetect() {
        BigTest test = new BigTest();
        CommandLine line = new CommandLine(test);

        List<String> ops = line.apply("--strV", "str", "--", "--strN", "uh-oh");
        assertEquals(test.strV, "str");
        assertNull(test.strN);
        assertEquals(ops.size(), 2);
        assertEquals(ops.get(0), "--strN");
        assertEquals(ops.get(1), "uh-oh");

        ops = line.apply("--strV", "str", "first", "--strN", "uh-oh");
        assertEquals(test.strV, "str");
        assertNull(test.strN);
        assertEquals(ops.size(), 3);
        assertEquals(ops.get(0), "first");
        assertEquals(ops.get(1), "--strN");
        assertEquals(ops.get(2), "uh-oh");

        ops = line.apply("--strV", "str", "-b", "--", "--strN", "uh-oh");
        assertEquals(test.strV, "str");
        assertNull(test.strN);
        assertTrue(((BoolTest) test).boolT);
        assertEquals(ops.size(), 2);
        assertEquals(ops.get(0), "--strN");
        assertEquals(ops.get(1), "uh-oh");

        ops = line.apply("--strV", "str", "-b", "first", "--strN", "uh-oh");
        assertEquals(test.strV, "str");
        assertNull(test.strN);
        assertTrue(((BoolTest) test).boolT);
        assertEquals(ops.size(), 3);
        assertEquals(ops.get(0), "first");
        assertEquals(ops.get(1), "--strN");
        assertEquals(ops.get(2), "uh-oh");

        ops = line.apply();
        assertEquals(ops.size(), 0);

        ops = line.apply("--strV", "str");
        assertEquals(test.strV, "str");
        assertEquals(ops.size(), 0);

        ops = line.apply("boo");
        assertEquals(ops.size(), 1);
        assertEquals(ops.get(0), "boo");

        ops = line.apply("--", "--strN", "uh-oh");
        assertEquals(ops.size(), 2);
        assertEquals(ops.get(0), "--strN");
        assertEquals(ops.get(1), "uh-oh");
    }

    @Test
    public void testEndDetectMultiOnly() {
        BigTestMultiOnly test = new BigTestMultiOnly();
        CommandLine line = new CommandLine(test);
        List<String> ops = line.apply("-strV", "str", "--", "-strN", "uh-oh");
        assertEquals(test.strV, "str");
        assertNull(test.strN);
        assertEquals(ops.size(), 2);
        assertEquals(ops.get(0), "-strN");
        assertEquals(ops.get(1), "uh-oh");

        ops = line.apply("-strV", "str", "first", "-strN", "uh-oh");
        assertEquals(test.strV, "str");
        assertNull(test.strN);
        assertEquals(ops.size(), 3);
        assertEquals(ops.get(0), "first");
        assertEquals(ops.get(1), "-strN");
        assertEquals(ops.get(2), "uh-oh");

        ops = line.apply();
        assertEquals(ops.size(), 0);

        ops = line.apply("-strV", "str");
        assertEquals(test.strV, "str");
        assertEquals(ops.size(), 0);

        ops = line.apply("boo");
        assertEquals(ops.size(), 1);
        assertEquals(ops.get(0), "boo");

        ops = line.apply("--", "-strN", "uh-oh");
        assertEquals(ops.size(), 2);
        assertEquals(ops.get(0), "-strN");
        assertEquals(ops.get(1), "uh-oh");
    }

    @Test
    public void testOverrideMulti() {
        class Override {
            @CommandOpt
            boolean bool1;
            @CommandOpt(multi = "bool2")
            boolean boolTwo;
            @CommandOpt(multi = "bool3", single = '3')
            boolean boolThree;
            @CommandOpt(single = '4')
            boolean bool4;
        }

        Override opts = new Override();
        CommandLine line = new CommandLine(opts);
        line.apply("--bool1", "--bool2", "--bool3", "--bool4");
        assertTrue(opts.bool1);
        assertTrue(opts.boolTwo);
        assertTrue(opts.boolThree);
        assertTrue(opts.bool4);

        opts = new Override();
        line = new CommandLine(opts);
        line.apply("--bool1", "--bool2", "-34");
        assertTrue(opts.bool1);
        assertTrue(opts.boolTwo);
        assertTrue(opts.boolThree);
        assertTrue(opts.bool4);
    }

    @Test
    public void testStatic() {
        CommandLine line = new CommandLine(StaticLeaf.class);

        line.apply("-topVal", "-1", "-midVal", "0", "-leafVal", "+1");
        assertEquals(StaticTop.topVal, -1);
        assertEquals(StaticMid.midVal, 0);
        assertEquals(StaticLeaf.leafVal, 1);
    }

    @Test
    public void testUsage() {
        BigTest opts = new BigTest();
        CommandLine line = new CommandLine(opts, "prog", "file...",
                "This is some descriptive text for the command, which ought" +
                        " to be formatted by the underlying usage() method." +
                        "  This lets us test whether that works or not.");
        line.setOptionDescriptionIndent(12);

        StringWriter out = new StringWriter();
        line.briefUsage(out);
        assertEquals(out.toString(),
                "prog [-?|--help] [-b|--boolT] [-B|--boolF] [--boolDT] [--boolDF] [--boolN]\n" +
                        "    [-c|--charV char] [--charD char] [--charN char] [-S|--strV str] [--strD str]\n" +
                        "    [--strN str] [-y|--byteV num] [--byteD num] [--byteN num] [-s|--shortV num]\n" +
                        "    [--shortD num] [--shortN num] [-i|--intV num] [--intD num] [--intN num]\n" +
                        "    [-l|--longV num] [--longD num] [--longN num] [-f|--floatV float]\n" +
                        "    [--floatD float] [--floatN float] [-d|--doubleV double] [--doubleD double]\n" +
                        "    [--doubleN double] [-t|--thingV thing] [--thingD thing] [--thingN thing]\n" +
                        "    [-F|--fileV path] [--fileD path] [--fileN path] [-r|--readV file]\n" +
                        "    [--readD file] [--readN file] [-w|--writeV file] [--writeD file]\n" +
                        "    [--writeN file] [-I|--inV file] [--inD file] [--inN file] [-O|--outV file]\n" +
                        "    [--outD file] [--outN file] [--randN file] [-v|--valsV num,...]\n" +
                        "    [--valsD num,...] [--valsN num,...] file...\n");

        out = new StringWriter();
        line.fullUsage(out);
        assertEquals(out.toString(),
                "prog [-?|--help] [-b|--boolT] [-B|--boolF] [--boolDT] [--boolDF] [--boolN]\n" +
                        "    [-c|--charV char] [--charD char] [--charN char] [-S|--strV str] [--strD str]\n" +
                        "    [--strN str] [-y|--byteV num] [--byteD num] [--byteN num] [-s|--shortV num]\n" +
                        "    [--shortD num] [--shortN num] [-i|--intV num] [--intD num] [--intN num]\n" +
                        "    [-l|--longV num] [--longD num] [--longN num] [-f|--floatV float]\n" +
                        "    [--floatD float] [--floatN float] [-d|--doubleV double] [--doubleD double]\n" +
                        "    [--doubleN double] [-t|--thingV thing] [--thingD thing] [--thingN thing]\n" +
                        "    [-F|--fileV path] [--fileD path] [--fileN path] [-r|--readV file]\n" +
                        "    [--readD file] [--readN file] [-w|--writeV file] [--writeD file]\n" +
                        "    [--writeN file] [-I|--inV file] [--inD file] [--inN file] [-O|--outV file]\n" +
                        "    [--outD file] [--outN file] [--randN file] [-v|--valsV num,...]\n" +
                        "    [--valsD num,...] [--valsN num,...] file...\n" +
                        "This is some descriptive text for the command, which ought to be formatted by\n" +
                        "the underlying usage() method.  This lets us test whether that works or not.\n" +
                        "\n" + "-?|--help   Print usage message\n" +
                        "-b|--boolT  a boolean value that defaults to 'true'\n" +
                        "-B|--boolF  a boolean value that defaults to 'false'. This also has a lot of\n" +
                        "            extra text so that we can see how wrapping is handled, which we hope\n" +
                        "            will be handled correctly\n" +
                        "--boolDT\n" + "--boolDF\n" + "--boolN\n" +
                        "-c|--charV char\n" + "--charD char\n" +
                        "            A character that might be any darn thing.\n" +
                        "--charN char\n" + "-S|--strV str\n" + "--strD str\n" +
                        "--strN str\n" + "-y|--byteV num\n" + "--byteD num\n" +
                        "--byteN num\n" + "-s|--shortV num\n" +
                        "--shortD num\n" + "--shortN num\n" +
                        "-i|--intV num\n" + "--intD num\n" + "--intN num\n" +
                        "-l|--longV num\n" + "--longD num\n" + "--longN num\n" +
                        "-f|--floatV float\n" + "--floatD float\n" +
                        "--floatN float\n" + "-d|--doubleV double\n" +
                        "--doubleD double\n" + "--doubleN double\n" +
                        "-t|--thingV thing\n" + "--thingD thing\n" +
                        "--thingN thing\n" + "-F|--fileV path\n" +
                        "--fileD path\n" + "--fileN path\n" +
                        "-r|--readV file\n" + "--readD file\n" +
                        "--readN file\n" + "-w|--writeV file\n" +
                        "--writeD file\n" + "--writeN file\n" +
                        "-I|--inV file\n" + "--inD file\n" + "--inN file\n" +
                        "-O|--outV file\n" + "--outD file\n" + "--outN file\n" +
                        "--randN file\n" + "-v|--valsV num,...\n" +
                        "--valsD num,...\n" + "--valsN num,...\n");

        line.setLineWidth(0);

        out = new StringWriter();
        line.briefUsage(out);
        assertEquals(out.toString(),
                "prog [-?|--help] [-b|--boolT] [-B|--boolF] [--boolDT] [--boolDF] [--boolN] [-c|--charV char] [--charD char] [--charN char] [-S|--strV str] [--strD str] [--strN str] [-y|--byteV num] [--byteD num] [--byteN num] [-s|--shortV num] [--shortD num] [--shortN num] [-i|--intV num] [--intD num] [--intN num] [-l|--longV num] [--longD num] [--longN num] [-f|--floatV float] [--floatD float] [--floatN float] [-d|--doubleV double] [--doubleD double] [--doubleN double] [-t|--thingV thing] [--thingD thing] [--thingN thing] [-F|--fileV path] [--fileD path] [--fileN path] [-r|--readV file] [--readD file] [--readN file] [-w|--writeV file] [--writeD file] [--writeN file] [-I|--inV file] [--inD file] [--inN file] [-O|--outV file] [--outD file] [--outN file] [--randN file] [-v|--valsV num,...] [--valsD num,...] [--valsN num,...] file...\n");

        out = new StringWriter();
        line.fullUsage(out);
        assertEquals(out.toString(),
                "prog [-?|--help] [-b|--boolT] [-B|--boolF] [--boolDT] [--boolDF] [--boolN] [-c|--charV char] [--charD char] [--charN char] [-S|--strV str] [--strD str] [--strN str] [-y|--byteV num] [--byteD num] [--byteN num] [-s|--shortV num] [--shortD num] [--shortN num] [-i|--intV num] [--intD num] [--intN num] [-l|--longV num] [--longD num] [--longN num] [-f|--floatV float] [--floatD float] [--floatN float] [-d|--doubleV double] [--doubleD double] [--doubleN double] [-t|--thingV thing] [--thingD thing] [--thingN thing] [-F|--fileV path] [--fileD path] [--fileN path] [-r|--readV file] [--readD file] [--readN file] [-w|--writeV file] [--writeD file] [--writeN file] [-I|--inV file] [--inD file] [--inN file] [-O|--outV file] [--outD file] [--outN file] [--randN file] [-v|--valsV num,...] [--valsD num,...] [--valsN num,...] file...\n" +
                        "This is some descriptive text for the command, which ought to be formatted by the underlying usage() method.  This lets us test whether that works or not.\n" +
                        "\n" + "-?|--help   Print usage message\n" +
                        "-b|--boolT  a boolean value that defaults to 'true'\n" +
                        "-B|--boolF  a boolean value that defaults to 'false'. This also has a lot of extra text so that we can see how wrapping is handled, which we hope will be handled correctly\n" +
                        "--boolDT\n" + "--boolDF\n" + "--boolN\n" +
                        "-c|--charV char\n" + "--charD char\n" +
                        "            A character that might be any darn thing.\n" +
                        "--charN char\n" + "-S|--strV str\n" + "--strD str\n" +
                        "--strN str\n" + "-y|--byteV num\n" + "--byteD num\n" +
                        "--byteN num\n" + "-s|--shortV num\n" +
                        "--shortD num\n" + "--shortN num\n" +
                        "-i|--intV num\n" + "--intD num\n" + "--intN num\n" +
                        "-l|--longV num\n" + "--longD num\n" + "--longN num\n" +
                        "-f|--floatV float\n" + "--floatD float\n" +
                        "--floatN float\n" + "-d|--doubleV double\n" +
                        "--doubleD double\n" + "--doubleN double\n" +
                        "-t|--thingV thing\n" + "--thingD thing\n" +
                        "--thingN thing\n" + "-F|--fileV path\n" +
                        "--fileD path\n" + "--fileN path\n" +
                        "-r|--readV file\n" + "--readD file\n" +
                        "--readN file\n" + "-w|--writeV file\n" +
                        "--writeD file\n" + "--writeN file\n" +
                        "-I|--inV file\n" + "--inD file\n" + "--inN file\n" +
                        "-O|--outV file\n" + "--outD file\n" + "--outN file\n" +
                        "--randN file\n" + "-v|--valsV num,...\n" +
                        "--valsD num,...\n" + "--valsN num,...\n");
    }

    @Test
    public void testUsageMultiOnly() {
        BigTestMultiOnly opts = new BigTestMultiOnly();
        CommandLine line = new CommandLine(opts, "prog", "file...",
                "This is some descriptive text for the command, which ought" +
                        " to be formatted by the underlying usage() method." +
                        "  This lets us test whether that works or not.");
        line.setOptionDescriptionIndent(12);

        StringWriter out = new StringWriter();
        line.briefUsage(out);
        assertEquals(out.toString(),
                "prog [-help] [-boolT] [-boolF] [-boolDT] [-boolDF] [-boolN] [-charV char]\n" +
                        "    [-charD char] [-charN char] [-strV str] [-strD str] [-strN str] [-byteV num]\n" +
                        "    [-byteD num] [-byteN num] [-shortV num] [-shortD num] [-shortN num]\n" +
                        "    [-intV num] [-intD num] [-intN num] [-longV num] [-longD num] [-longN num]\n" +
                        "    [-floatV float] [-floatD float] [-floatN float] [-doubleV double]\n" +
                        "    [-doubleD double] [-doubleN double] [-thingV thing] [-thingD thing]\n" +
                        "    [-thingN thing] [-fileV path] [-fileD path] [-fileN path] [-readV file]\n" +
                        "    [-readD file] [-readN file] [-writeV file] [-writeD file] [-writeN file]\n" +
                        "    [-inV file] [-inD file] [-inN file] [-outV file] [-outD file] [-outN file]\n" +
                        "    [-randN file] [-valsV num,...] [-valsD num,...] [-valsN num,...] file...\n");

        out = new StringWriter();
        line.fullUsage(out);
        assertEquals(out.toString(),
                "prog [-help] [-boolT] [-boolF] [-boolDT] [-boolDF] [-boolN] [-charV char]\n" +
                        "    [-charD char] [-charN char] [-strV str] [-strD str] [-strN str] [-byteV num]\n" +
                        "    [-byteD num] [-byteN num] [-shortV num] [-shortD num] [-shortN num]\n" +
                        "    [-intV num] [-intD num] [-intN num] [-longV num] [-longD num] [-longN num]\n" +
                        "    [-floatV float] [-floatD float] [-floatN float] [-doubleV double]\n" +
                        "    [-doubleD double] [-doubleN double] [-thingV thing] [-thingD thing]\n" +
                        "    [-thingN thing] [-fileV path] [-fileD path] [-fileN path] [-readV file]\n" +
                        "    [-readD file] [-readN file] [-writeV file] [-writeD file] [-writeN file]\n" +
                        "    [-inV file] [-inD file] [-inN file] [-outV file] [-outD file] [-outN file]\n" +
                        "    [-randN file] [-valsV num,...] [-valsD num,...] [-valsN num,...] file...\n" +
                        "This is some descriptive text for the command, which ought to be formatted by\n" +
                        "the underlying usage() method.  This lets us test whether that works or not.\n" +
                        "\n" + "-help       Print usage message\n" +
                        "-boolT\n" + "-boolF\n" + "-boolDT\n" + "-boolDF\n" +
                        "-boolN\n" + "-charV char\n" + "-charD char\n" +
                        "-charN char\n" + "-strV str\n" + "-strD str\n" +
                        "-strN str\n" + "-byteV num\n" + "-byteD num\n" +
                        "-byteN num\n" + "-shortV num\n" + "-shortD num\n" +
                        "-shortN num\n" + "-intV num\n" + "-intD num\n" +
                        "-intN num\n" + "-longV num\n" + "-longD num\n" +
                        "-longN num\n" + "-floatV float\n" + "-floatD float\n" +
                        "-floatN float\n" + "-doubleV double\n" +
                        "-doubleD double\n" + "-doubleN double\n" +
                        "-thingV thing\n" + "-thingD thing\n" +
                        "-thingN thing\n" + "-fileV path\n" + "-fileD path\n" +
                        "-fileN path\n" + "-readV file\n" + "-readD file\n" +
                        "-readN file\n" + "-writeV file\n" + "-writeD file\n" +
                        "-writeN file\n" + "-inV file\n" + "-inD file\n" +
                        "-inN file\n" + "-outV file\n" + "-outD file\n" +
                        "-outN file\n" + "-randN file\n" + "-valsV num,...\n" +
                        "-valsD num,...\n" + "-valsN num,...\n");

        line.setLineWidth(0);

        out = new StringWriter();
        line.briefUsage(out);
        assertEquals(out.toString(),
                "prog [-help] [-boolT] [-boolF] [-boolDT] [-boolDF] [-boolN] [-charV char] [-charD char] [-charN char] [-strV str] [-strD str] [-strN str] [-byteV num] [-byteD num] [-byteN num] [-shortV num] [-shortD num] [-shortN num] [-intV num] [-intD num] [-intN num] [-longV num] [-longD num] [-longN num] [-floatV float] [-floatD float] [-floatN float] [-doubleV double] [-doubleD double] [-doubleN double] [-thingV thing] [-thingD thing] [-thingN thing] [-fileV path] [-fileD path] [-fileN path] [-readV file] [-readD file] [-readN file] [-writeV file] [-writeD file] [-writeN file] [-inV file] [-inD file] [-inN file] [-outV file] [-outD file] [-outN file] [-randN file] [-valsV num,...] [-valsD num,...] [-valsN num,...] file...\n");

        out = new StringWriter();
        line.fullUsage(out);
        assertEquals(out.toString(),
                "prog [-help] [-boolT] [-boolF] [-boolDT] [-boolDF] [-boolN] [-charV char] [-charD char] [-charN char] [-strV str] [-strD str] [-strN str] [-byteV num] [-byteD num] [-byteN num] [-shortV num] [-shortD num] [-shortN num] [-intV num] [-intD num] [-intN num] [-longV num] [-longD num] [-longN num] [-floatV float] [-floatD float] [-floatN float] [-doubleV double] [-doubleD double] [-doubleN double] [-thingV thing] [-thingD thing] [-thingN thing] [-fileV path] [-fileD path] [-fileN path] [-readV file] [-readD file] [-readN file] [-writeV file] [-writeD file] [-writeN file] [-inV file] [-inD file] [-inN file] [-outV file] [-outD file] [-outN file] [-randN file] [-valsV num,...] [-valsD num,...] [-valsN num,...] file...\n" +
                        "This is some descriptive text for the command, which ought to be formatted by the underlying usage() method.  This lets us test whether that works or not.\n" +
                        "\n" + "-help       Print usage message\n" +
                        "-boolT\n" + "-boolF\n" + "-boolDT\n" + "-boolDF\n" +
                        "-boolN\n" + "-charV char\n" + "-charD char\n" +
                        "-charN char\n" + "-strV str\n" + "-strD str\n" +
                        "-strN str\n" + "-byteV num\n" + "-byteD num\n" +
                        "-byteN num\n" + "-shortV num\n" + "-shortD num\n" +
                        "-shortN num\n" + "-intV num\n" + "-intD num\n" +
                        "-intN num\n" + "-longV num\n" + "-longD num\n" +
                        "-longN num\n" + "-floatV float\n" + "-floatD float\n" +
                        "-floatN float\n" + "-doubleV double\n" +
                        "-doubleD double\n" + "-doubleN double\n" +
                        "-thingV thing\n" + "-thingD thing\n" +
                        "-thingN thing\n" + "-fileV path\n" + "-fileD path\n" +
                        "-fileN path\n" + "-readV file\n" + "-readD file\n" +
                        "-readN file\n" + "-writeV file\n" + "-writeD file\n" +
                        "-writeN file\n" + "-inV file\n" + "-inD file\n" +
                        "-inN file\n" + "-outV file\n" + "-outD file\n" +
                        "-outN file\n" + "-randN file\n" + "-valsV num,...\n" +
                        "-valsD num,...\n" + "-valsN num,...\n");
    }

    @Test
    public void testBaseExample() {
        class Config {
            @CommandOpt
            boolean verbose;

            @CommandOpt
            Reader in;

            @CommandOpt
            Writer out;
        }

        String[] args = {"--verbose", "left", "right", "up", "down"};

        Config config = new Config();
        CommandLine line = new CommandLine(config);
        List<String> operands = line.apply(args);
        assertEquals(operands.size(), 4);

        StringWriter out = new StringWriter();
        line.briefUsage(out);
        assertEquals(out.toString(),
                "Config [-help] [-verbose] [-in file] [-out file] [ops ...]\n");
    }

    @Test
    public void testBriefExampleUsage() {
        class Config {
            @CommandOpt(single = 'v')
            boolean verbose;

            @CommandOpt(valueName = "sourceFile")
            Reader in;

            @CommandOpt(valueName = "outputFile")
            Writer out;
        }

        String[] args = {"--verbose", "left", "right", "up", "down"};

        Config config = new Config();
        CommandLine line = new CommandLine(config, "process", "direction ...");
        List<String> operands = line.apply(args);
        assertEquals(operands.size(), 4);

        StringWriter out = new StringWriter();
        line.briefUsage(out);
        assertEquals(out.toString(),
                "process [-?|--help] [-v|--verbose] [--in sourceFile] [--out outputFile]\n" +
                        "    direction ...\n");
    }

    @Test
    public void testFullExampleUsage() {
        class Config {
            @CommandOpt(single = 'v', desc = "Verbose message during execution")
            boolean verbose;

            @CommandOpt(valueName = "sourceFile",
                    desc = "Source input file; by default, System.in")
            Reader in;

            @CommandOpt(valueName = "outputFile",
                    desc = "File for output; by default, System.out")
            Writer out;
        }

        String[] args = {"--verbose", "left", "right", "up", "down"};

        Config config = new Config();
        CommandLine line = new CommandLine(config, "process", "direction ...",
                "This command reads a source, applies the directions given" +
                        " on the command line, and generates a resulting output file." +
                        " At least one direction must be given.");
        line.setLineWidth(75);  // stops wrapping of the output in the javadoc
        List<String> operands = line.apply(args);
        assertEquals(operands.size(), 4);

        StringWriter out = new StringWriter();
        line.fullUsage(out);
        assertEquals(out.toString(),
                "process [-?|--help] [-v|--verbose] [--in sourceFile] [--out outputFile]\n" +
                        "    direction ...\n" +
                        "This command reads a source, applies the directions given on the command\n" +
                        "line, and generates a resulting output file. At least one direction must be\n" +
                        "given.\n" + "\n" +
                        "-?|--help         Print usage message\n" +
                        "-v|--verbose      Verbose message during execution\n" +
                        "--in sourceFile   Source input file; by default, System.in\n" +
                        "--out outputFile  File for output; by default, System.out\n");
    }

    private static void addArray(Collection<Object[]> tests, Object... objs) {
        tests.add(objs);
    }

    @DataProvider(name = "valueTests")
    public Iterator<Object[]> valueTests() {
        List<Object[]> tests = new ArrayList<Object[]>();
        addArray(tests, "str", String.class, null, "str");
        addArray(tests, "1", String.class, null, "1");
        addArray(tests, "str", String.class, new CommandOptValues(), "str");
        addArray(tests, "str", String.class, new CommandOptValues("str", "",
                "!isEmpty"), "str");
        return tests.iterator();
    }

    @Test(dataProvider = "valueTests")
    public void testGetValues(String valStr, Class type, CommandOpt anno,
            Object expected) {

        CommandLine line = new CommandLine(new Object());
        CommandLineAdvanced adv = line.getAdvancedKnobs();
        Object val = adv.getValueFor(valStr, type, anno);
        assertEquals(val, expected);
    }

    @Test
    public void testArrays() {
        doArrayTest(1);
        doArrayTest(1, 2);
        doArrayTest(1, 2, 3);
        doArrayTest(1, 2, 3, 4);
        doArrayTest(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        doArrayTest(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);

        ArrayFields opts = new ArrayFields();
        CommandLine line = new CommandLine(opts);
        line.apply("-v", "");
        assertEquals(opts.vals, new int[0]);

        line.apply("-v", "2");
        assertEquals(opts.vals, new int[]{2});

        line.apply("-v", "");
        assertEquals(opts.vals, new int[]{2});

        assertEquals(opts.bytes, new byte[]{1});
        assertEquals(opts.longs, new long[0]);

        assertNull(opts.floats);
    }

    private void doArrayTest(int... values) {
        ArrayFields opts = new ArrayFields();
        CommandLine line = new CommandLine(opts);

        List<String> argList = new ArrayList<String>();

        // As many multi options
        for (int value : values) {
            argList.add("--vals");
            argList.add(String.valueOf(value));
        }
        line.apply(argList.toArray(new String[argList.size()]));
        assertEquals(opts.vals, values);

        // As single values
        arrayOptionSetup(1, values);

        // As pairs of values
        arrayOptionSetup(2, values);

        // As pairs of values
        arrayOptionSetup(3, values);

        // As one single option
        arrayOptionSetup(values.length, values);
    }

    private void arrayOptionSetup(int perArg, int[] values) {
        ArrayFields opts = new ArrayFields();
        CommandLine line = new CommandLine(opts);
        List<String> argList = new ArrayList<String>();
        opts.vals = null;
        for (int i = 0; i < values.length; i += perArg) {
            StringBuilder sb = new StringBuilder("-v");
            int end = Math.min(values.length, i + perArg);
            for (int j = i; j < end; j++) {
                if (j > i)
                    sb.append(',');
                sb.append(values[j]);
            }
            argList.add(sb.toString());
        }
        line.apply(argList.toArray(new String[argList.size()]));
        assertEquals(opts.vals, values, argList.toString());
    }
}