package org.simplx.args;

import org.apache.commons.io.FileUtils;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"RedundantArrayCreation"})
public class TestMainArgs {
    private boolean verbose;
    private int max;
    private PrintWriter recordOut;

    public enum Things {
        HORSE, BOX, CONCEPT, TOE_GUARD_HOLDER, CONCERT
    }

    @Test
    public void testExample1() throws IOException {
        File tmp = File.createTempFile("tmc", ".txt");
        tmp.deleteOnExit();

        String invoke = "-verbose -out history f1 f2 f3";
        invoke = invoke.replace("history", tmp.getPath());
        String[] args = invoke.split(" ");

        MainArgs line = new MainArgs("flick", args);
        try {
            verbose = line.getBoolean("verbose");
            max = line.getInt("max", Integer.MAX_VALUE);
            Writer out = line.getWriter("out", (Writer) null);
            if (out != null) {
                recordOut = new PrintWriter(out);
            }
            String[] files = line.getOperands("file", "...");
            for (String file : files) {
                flick(file);
            }
        } catch (HelpOnlyException e) {
            System.exit(0);
        } finally {
            if (recordOut != null) {
                recordOut.close();
            }
        }

        assertTrue(verbose);
        assertEquals(max, Integer.MAX_VALUE);
        assertNotNull(recordOut);
        List records = FileUtils.readLines(tmp);
        assertEquals(records.size(), 3);
        assertEquals(records.get(0), "flick f1");
        assertEquals(records.get(1), "flick f2");
        assertEquals(records.get(2), "flick f3");

        String usage = line.usageString();
        assertEquals(usage,
                "flick [-verbose] [-max int] [-out file] [-help] file ...\n");
    }

    @Test
    public void testExample1UsingMultis() throws IOException {
        File tmp = File.createTempFile("tmc", ".txt");
        tmp.deleteOnExit();

        String invoke = "--verbose --out history f1 f2 f3";
        invoke = invoke.replace("history", tmp.getPath());
        String[] args = invoke.split(" ");

        MainArgs line = new MainArgs("flick", args);
        try {
            verbose = line.getBoolean("v|verbose");
            max = line.getInt("m|max", Integer.MAX_VALUE);
            Writer out = line.getWriter("|out", (Writer) null);
            if (out != null) {
                recordOut = new PrintWriter(out);
            }
            String[] files = line.getOperands("file", "...");
            for (String file : files) {
                flick(file);
            }
        } catch (HelpOnlyException e) {
            System.exit(0);
        } finally {
            if (recordOut != null) {
                recordOut.close();
            }
        }

        assertTrue(verbose);
        assertEquals(max, Integer.MAX_VALUE);
        assertNotNull(recordOut);
        List records = FileUtils.readLines(tmp);
        assertEquals(records.size(), 3);
        assertEquals(records.get(0), "flick f1");
        assertEquals(records.get(1), "flick f2");
        assertEquals(records.get(2), "flick f3");

        String usage = line.usageString();
        assertEquals(usage,
                "flick [-v, --verbose] [-m, --max int] [--out file] [--help] file ...\n");
    }

    @Test
    public void testExample1UsingSingles() throws IOException {
        File tmp = File.createTempFile("tmc", ".txt");
        tmp.deleteOnExit();

        String invoke = "-v -m 3 --out history f1 f2 f3";
        invoke = invoke.replace("history", tmp.getPath());
        String[] args = invoke.split(" ");

        MainArgs line = new MainArgs("flick", args);
        try {
            verbose = line.getBoolean("v|verbose");
            max = line.getInt("m|max", Integer.MAX_VALUE);
            Writer out = line.getWriter("|out", (Writer) null);
            if (out != null) {
                recordOut = new PrintWriter(out);
            }
            String[] files = line.getOperands("file", "...");
            for (String file : files) {
                flick(file);
            }
        } catch (HelpOnlyException e) {
            System.exit(0);
        } finally {
            if (recordOut != null) {
                recordOut.close();
            }
        }

        assertTrue(verbose);
        assertEquals(max, 3);
        assertNotNull(recordOut);
        List records = FileUtils.readLines(tmp);
        assertEquals(records.size(), 3);
        assertEquals(records.get(0), "flick f1");
        assertEquals(records.get(1), "flick f2");
        assertEquals(records.get(2), "flick f3");

        String usage = line.usageString();
        assertEquals(usage,
                "flick [-v, --verbose] [-m, --max int] [--out file] [--help] file ...\n");
    }

    @Test
    public void testExample2() {
        String[] args = {"-verbose", "-verbose", "-verbose"};

        MainArgs line = new MainArgs("test", args);
        int verbosity = 0;
        while (line.getBoolean("verbose")) {
            verbosity++;
        }
        String[] ops = line.getOperands();

        assertEquals(verbosity, 3);
        assertEquals(ops.length, 0);
    }

    @Test
    public void testExample3() {
        String[] args = {"-user", "pat", "-user", "robin"};

        MainArgs line = new MainArgs("test", args);
        List<String> users = new ArrayList<String>();
        String user;
        while ((user = line.getString("user", null)) != null) {
            users.add(user);
        }
        String[] ops = line.getOperands();

        assertEquals(users.size(), 2);
        assertEquals(users.get(0), "pat");
        assertEquals(users.get(1), "robin");
        assertEquals(ops.length, 0);
    }

    private void flick(String file) {
        recordOut.println("flick " + file);
    }

    @Test
    public void testMissingArg() {
        MainArgs args = new MainArgs("test", new String[]{"-foo"});
        try {
            args.getString("foo", "default");
            fail("missing argument did not cause exception");
        } catch (CommandLineException e) {
            // This is supposed to happen
        }
    }

    @Test
    public void testHelp() {
        tryHelp(0, "-foo", "-help");
        tryHelp(0, "-foo", "-help", "-bar");
        tryHelp(0, "-help", "-bar");
        tryHelp(0, "-foo", "-help", "-bar", "--");
        tryHelp(2, "-foo", "-help", "-bar", "--", "op1", "op2");
    }

    private void tryHelp(int opCount, String... strings) {
        MainArgs args = new MainArgs("test", strings);
        try {
            args.getBoolean("foo");
            args.getBoolean("bar");
            assertEquals(args.getOperands().length, opCount);
            fail("-help not flagged");
        } catch (HelpOnlyException ignored) {
            // This is supposed to happen
        }
    }

    @Test(enabled = false)
    public void testFindEnd() {
        doTestFindEnd(null);
        doTestFindEnd("testProg");
    }

    private void doTestFindEnd(String progName) {
        String[] args = {"-verbose", "hello", "-notAnOpt"};
        MainArgs line = createMainArgs(progName, args);
        assertTrue(line.getBoolean("verbose"));
        assertFalse(line.getBoolean("notAnOpt"));
        String[] ops = line.getOperands();
        assertEquals(ops.length, 2);
        assertEquals(ops[0], "hello");
        assertEquals(ops[1], "-notAnOpt");
    }

    @Test
    public void testUnknown() {
        doTestUnknown(null);
        doTestUnknown("testProg");
    }

    private void doTestUnknown(String progName) {
        String[] args = {"-verbose", "-unknown", "operands"};
        MainArgs line = createMainArgs(progName, args);

        assertTrue(line.getBoolean("verbose"), "first verbose");
        assertFalse(line.getBoolean("verbose"), "second verbose");
        try {
            line.getOperands();
            fail("Should have an unknown option for \"-unknown\"");
        } catch (CommandLineException e) {
            // This should happen
        }
    }

    private static MainArgs createMainArgs(String progName, String... args) {
        MainArgs line;
        if (progName == null) {
            line = new MainArgs(TestMainArgs.class, args);
        } else {
            line = new MainArgs(progName, args);
        }
        return line;
    }

    @Test
    public void testArgTypes() throws IOException {
        doTestArgTypes(null);
        doTestArgTypes("testProg");
    }

    private void doTestArgTypes(String progName) throws IOException {
        File tmp = File.createTempFile("test", ".txt");
        tmp.deleteOnExit();
        String path = tmp.getPath();

        String maxLong = Long.valueOf(Long.MAX_VALUE).toString();
        String badPath = path + File.separatorChar + "nonsuch";
        MainArgs line = createMainArgs(progName, "-verbose", "-str", "first",
                "-verbose", "-str", "second", "-max", "15", "-up", maxLong,
                "-top", "5.1e3", "-thing", "ToeGuard", "-sdir", "/", "-nadir",
                path, "-cdir", path, "-edir", path, "-fwrite", path, "-fwrite",
                path, "-swrite", "-", "-bwrite", badPath, "-bwrite", badPath,
                "-fread", path, "-sread", "-", "-bread", badPath, "-bread",
                badPath, "-fout", path, "-fout", path, "-sout", "-", "-bout",
                badPath, "-bout", badPath, "-fin", path, "-sin", "-", "-bin",
                badPath, "-bin", badPath, "-frand", path, "-frand", path,
                "-srand", "-", "-srand", "-", "-brand", badPath, "-brand",
                badPath, "--", "-verbose", "...");

        testBooleans(line);
        testArgs(line);

        testWriters(path, line);
        testOutputStream(path, line);

        testReaders(path, line);
        testInputStream(path, line);

        testRandomAccessFile(path, line);

        testDirectory(tmp, path, line);

        String[] operands = line.getOperands();
        assertEquals(operands.length, 2, "two operands");
        assertEquals(operands[0], "-verbose", "after --");
        assertEquals(operands[1], "...", "...");
    }

    private void testBooleans(MainArgs args) {
        assertTrue(args.getBoolean("verbose"), "verbose");
        assertTrue(args.getBoolean("verbose"), "verbose");
        assertFalse(args.getBoolean("verbose"), "verbose");

        assertFalse(args.getBoolean("overwrite"), "overwrite");
        assertFalse(args.getBoolean("overwrite"), "overwrite");
    }

    private void testArgs(MainArgs args) {
        assertEquals(args.getString("str", "last"), "first", "str");
        assertEquals(args.getString("str", "last"), "second", "str");
        assertEquals(args.getString("str", "last"), "last", "str");

        assertEquals(args.getInt("max", 100), 15, "max");
        assertEquals(args.getInt("max", 100), 100, "max");
        assertEquals(args.getInt("max", -1), -1, "min");

        assertEquals(args.getLong("up", 100), Long.MAX_VALUE, "up");
        assertEquals(args.getLong("up", 100), 100, "up");
        assertEquals(args.getLong("down", Long.MIN_VALUE), Long.MIN_VALUE,
                "down");

        assertEquals(args.getDouble("top", 100.0), 5.1e3, 0.1, "top");
        assertEquals(args.getDouble("top", 100.0), 100.0, 0.0, "top");
        assertEquals(args.getDouble("bot", Double.NEGATIVE_INFINITY),
                Double.NEGATIVE_INFINITY, 0.0, "bot");

        assertEquals(args.getEnumValue("thing", Things.HORSE),
                Things.TOE_GUARD_HOLDER, "thing");
    }

    private void testWriters(String path, MainArgs args) throws IOException {
        testWriterPath(path, args.getWriter("fwrite", path));
        Writer fwrite = testWriterPath(path, args.getWriter("fwrite",
                (Writer) null));

        PipedInputStream pis = new PipedInputStream();
        PrintStream pos = new PrintStream(new PipedOutputStream(pis));
        System.setIn(pis);
        OutputStreamWriter swrite = (OutputStreamWriter) args.getWriter(
                "swrite", (Writer) null);
        assertNotNull(swrite, "swrite not null");
        String testString = "swrite test";
        pos.println(testString);
        pos.close();
        BufferedReader bin = new BufferedReader(new InputStreamReader(pis));
        assertEquals(bin.readLine(), testString, "swrite string");

        assertSame(args.getWriter("nwrite", fwrite), fwrite, "nwrite");
        assertNotNull(args.getWriter("nwrite", "-"), "nwrite");
        assertNotNull(args.getWriter("nwrite", path), "nwrite");
        assertSame(args.getWriter("nwrite", (String) null), null, "nwrite");
        assertSame(args.getWriter("nwrite", (Writer) null), null, "nwrite");

        try {
            args.getWriter("bwrite", (String) null);
            fail("bwrite did not fail");
        } catch (IOException ignored) {
            // this is correct behavior
        }
        try {
            args.getWriter("bwrite", (Writer) null);
            fail("bwrite did not fail");
        } catch (IOException ignored) {
            // this is correct behavior
        }
    }

    private Writer testWriterPath(String path, Writer fwrite)
            throws IOException {

        assertNotNull(fwrite, "fwrite");
        fwrite.write("fwrite test\n");
        fwrite.close();
        BufferedReader in = new BufferedReader(new FileReader(path));
        assertEquals(in.readLine(), "fwrite test");
        return fwrite;
    }

    private void testOutputStream(String path, MainArgs args)
            throws IOException {

        testOutputStreamPath(path, args.getOutputStream("fout", path));
        OutputStream fout = testOutputStreamPath(path, args.getOutputStream(
                "fout", (OutputStream) null));

        PipedInputStream pis = new PipedInputStream();
        PrintStream pos = new PrintStream(new PipedOutputStream(pis));
        System.setIn(pis);
        OutputStream sout = args.getOutputStream("sout", (OutputStream) null);
        assertNotNull(sout, "sout not null");
        String testString = "sout test";
        pos.println(testString);
        pos.close();
        BufferedReader bin = new BufferedReader(new InputStreamReader(pis));
        assertEquals(bin.readLine(), testString, "sout string");
        assertSame(args.getInputStream("sout", pis), pis, "2nd sout");

        assertSame(args.getOutputStream("nout", fout), fout, "nout");
        assertSame(args.getOutputStream("nout", "-"), System.out, "nout");
        assertNotNull(args.getOutputStream("nout", path), "nout");
        assertSame(args.getOutputStream("nout", (String) null), null, "nout");
        assertSame(args.getOutputStream("nout", (OutputStream) null), null,
                "nout");

        try {
            args.getOutputStream("bout", (String) null);
            fail("bout did not fail");
        } catch (IOException ignored) {
            // this is correct behavior
        }
        try {
            args.getOutputStream("bout", (OutputStream) null);
            fail("bout did not fail");
        } catch (IOException ignored) {
            // this is correct behavior
        }
    }

    private OutputStream testOutputStreamPath(String path, OutputStream fout)
            throws IOException {

        assertNotNull(fout, "fout");
        fout.write("fout test\n".getBytes());
        fout.close();
        BufferedReader in = new BufferedReader(new FileReader(path));
        assertEquals(in.readLine(), "fout test");
        return fout;
    }

    private void testInputStream(String path, MainArgs args)
            throws IOException {

        FileUtils.writeStringToFile(new File(path), "fin test\n");
        InputStream fin = args.getInputStream("fin", path);
        assertNotNull(fin, "fin");
        BufferedReader b = new BufferedReader(new InputStreamReader(fin));
        assertEquals(b.readLine(), "fin test", "fin contents");
        b.close();

        PipedInputStream pis = new PipedInputStream();
        PrintStream pos = new PrintStream(new PipedOutputStream(pis));
        System.setIn(pis);
        InputStream sin = args.getInputStream("sin", (InputStream) null);
        assertNotNull(sin, "sin not null");
        String testString = "sin test";
        pos.println(testString);
        pos.close();
        BufferedReader bin = new BufferedReader(new InputStreamReader(sin));
        assertEquals(bin.readLine(), testString, "sin string");
        assertSame(args.getInputStream("sin", pis), pis, "2nd sout");

        assertSame(args.getInputStream("nin", fin), fin, "nin");
        assertSame(args.getInputStream("nin", "-"), System.in, "nin");
        assertNotNull(args.getInputStream("nin", path), "nin");
        assertSame(args.getInputStream("nin", (String) null), null, "nin");
        assertSame(args.getInputStream("nin", (InputStream) null), null, "nin");

        try {
            args.getInputStream("bin", (String) null);
            fail("bin did not fail");
        } catch (IOException ignored) {
            // this is correct behavior
        }
        try {
            args.getInputStream("bin", (InputStream) null);
            fail("bin did not fail");
        } catch (IOException ignored) {
            // this is correct behavior
        }
    }

    private void testReaders(String path, MainArgs args) throws IOException {
        FileUtils.writeStringToFile(new File(path), "fread test\n");
        Reader fread = args.getReader("fread", path);
        assertNotNull(fread, "fread");
        BufferedReader b = new BufferedReader(fread);
        assertEquals(b.readLine(), "fread test", "fread contents");
        b.close();

        PipedInputStream pis = new PipedInputStream();
        PrintStream pos = new PrintStream(new PipedOutputStream(pis));
        System.setIn(pis);
        InputStreamReader sread = (InputStreamReader) args.getReader("sread",
                (Reader) null);
        assertNotNull(sread, "sread not null");
        String testString = "sread test";
        pos.println(testString);
        pos.close();
        BufferedReader bin = new BufferedReader(sread);
        assertEquals(bin.readLine(), testString, "sread string");

        Reader noReader = args.getReader("nread", fread);
        assertSame(noReader, fread, "nread");
        assertNotNull(args.getReader("nread", "-"), "nread");
        assertNotNull(args.getReader("nread", path), "nread");
        assertSame(args.getReader("nread", (String) null), null, "nread");
        assertSame(args.getReader("nread", (Reader) null), null, "nread");

        try {
            args.getReader("bread", (String) null);
            fail("bread did not fail");
        } catch (IOException ignored) {
            // this is correct behavior
        }
        try {
            args.getReader("bread", (Reader) null);
            fail("bread did not fail");
        } catch (IOException ignored) {
            // this is correct behavior
        }
    }

    private void testRandomAccessFile(String path, MainArgs args)
            throws IOException {

        testRandomAccessFilePath(path, args.getRandomAccessFile("frand",
                (RandomAccessFile) null, "rw"));
        RandomAccessFile frand = testRandomAccessFilePath(path,
                args.getRandomAccessFile("frand", (String) null, "rw"));

        try {
            args.getRandomAccessFile("srand", (String) null, "r");
            fail("\"-srand -\" should not work");
        } catch (IOException ignored) {
            // This is how it should work
        }
        try {
            args.getRandomAccessFile("srand", (RandomAccessFile) null, "r");
            fail("\"-srand -\" should not work");
        } catch (IOException ignored) {
            // This is how it should work
        }
        try {
            args.getRandomAccessFile("brand", (String) null, "r");
            fail("\"-srand -\" should not work");
        } catch (IOException ignored) {
            // This is how it should work
        }
        try {
            args.getRandomAccessFile("brand", (RandomAccessFile) null, "r");
            fail("\"-srand -\" should not work");
        } catch (IOException ignored) {
            // This is how it should work
        }

        assertSame(args.getRandomAccessFile("nrand", frand, "r"), frand,
                "nrand");
        assertNotNull(args.getRandomAccessFile("nrand", path, "r"), "nrand");
        assertNull(args.getRandomAccessFile("nrand", (String) null, "r"),
                "nrand");
    }

    private RandomAccessFile testRandomAccessFilePath(String path,
            RandomAccessFile frand) throws IOException {

        FileUtils.writeStringToFile(new File(path), "fread test\n");
        assertNotNull(frand, "frand");
        String s = FileUtils.readFileToString(new File(path));
        assertEquals(s, "fread test\n", "fread contents");
        return frand;
    }

    private void testDirectory(File tmp, String path, MainArgs args) {
        assertEquals(args.getDirectory("sdir", "C:"), "/", "sdir");
        assertEquals(args.getDirectory("sdir", "C:"), "C:", "sdir");

        assertTrue(tmp.delete());
        String noPath = "noSuchPath";
        assertEquals(args.getDirectory("nadir", noPath, MissingDirAction.NONE),
                path, "nadir");
        assertFalse(tmp.exists(), "nadir should not exist");
        assertEquals(args.getDirectory("nadir", noPath, MissingDirAction.NONE),
                noPath, "nadir");
        assertFalse(tmp.exists(), "nadir should not exist");

        try {
            args.getDirectory("edir", path, MissingDirAction.EXCEPTION);
            fail("Should have exception for -edir");
        } catch (CommandLineException e) {
            assertTrue(e.getMessage().contains(path));
        }

        assertEquals(args.getDirectory("cdir", noPath, MissingDirAction.CREATE),
                path, "cdir");
        assertTrue(tmp.isDirectory(), "cdir should be dir");
        String noDir = args.getDirectory("nedir", path);
        assertEquals(noDir, path, "nedir for cdir");

        assertTrue(tmp.delete());
        noDir = args.getDirectory("ndir", path, MissingDirAction.NONE);
        assertEquals(noDir, path, "ndir");
        assertFalse(tmp.exists(), "ndir should not exist");
    }

    @SuppressWarnings({"OctalInteger"})
    @Test
    public void testIntFormats() {
        MainArgs args = new MainArgs("test",
                new String[]{"-hex", "0x123", "-hash", "#123", "-dec", "123",
                        "-oct", "0123", "-bad", "NotANumber", "-hex", "+0x123",
                        "-hash", "+#123", "-dec", "+123", "-oct", "0123",
                        "-bad", "+NotANumber", "-hex", "-0x123", "-hash",
                        "-#123", "-dec", "-123", "-oct", "-0123", "-bad",
                        "-NotANumber",});

        assertEquals(args.getInt("hex", -1), 0x123, "hex");
        assertEquals(args.getInt("hex", -1), +0x123, "hex");
        assertEquals(args.getInt("hex", -1), -0x123, "hex");

        assertEquals(args.getInt("hash", -1), 0x123, "hash");
        assertEquals(args.getInt("hash", -1), +0x123, "hash");
        assertEquals(args.getInt("hash", -1), -0x123, "hash");

        assertEquals(args.getInt("dec", -1), 123, "dec");
        assertEquals(args.getInt("dec", -1), +123, "dec");
        assertEquals(args.getInt("dec", -1), -123, "dec");

        assertEquals(args.getInt("oct", -1), 0123, "oct");
        assertEquals(args.getInt("oct", -1), +0123, "oct");
        assertEquals(args.getInt("oct", -1), -0123, "oct");

        for (int i = 0; i < 3; i++) {
            try {
                args.getInt("bad", -1);
                fail("bad: Bad number does not throw exception");
            } catch (NumberFormatException ignored) {
                // this is required
            }
        }

        assertEquals(args.getOperands().length, 0);
    }

    @SuppressWarnings({"OctalInteger"})
    @Test
    public void testLongFormats() {
        MainArgs args = new MainArgs("test",
                new String[]{"-hex", "0x123", "-hash", "#123", "-dec", "123",
                        "-oct", "0123", "-bad", "NotANumber", "-hex", "+0x123",
                        "-hash", "+#123", "-dec", "+123", "-oct", "0123",
                        "-bad", "+NotANumber", "-hex", "-0x123", "-hash",
                        "-#123", "-dec", "-123", "-oct", "-0123", "-bad",
                        "-NotANumber",});

        assertEquals(args.getLong("hex", -1), 0x123L, "hex");
        assertEquals(args.getLong("hex", -1), +0x123L, "hex");
        assertEquals(args.getLong("hex", -1), -0x123L, "hex");

        assertEquals(args.getLong("hash", -1), 0x123L, "hash");
        assertEquals(args.getLong("hash", -1), +0x123L, "hash");
        assertEquals(args.getLong("hash", -1), -0x123L, "hash");

        assertEquals(args.getLong("dec", -1), 123L, "dec");
        assertEquals(args.getLong("dec", -1), +123L, "dec");
        assertEquals(args.getLong("dec", -1), -123L, "dec");

        assertEquals(args.getLong("oct", -1), 0123L, "oct");
        assertEquals(args.getLong("oct", -1), +0123L, "oct");
        assertEquals(args.getLong("oct", -1), -0123L, "oct");

        for (int i = 0; i < 3; i++) {
            try {
                args.getLong("bad", -1);
                fail("bad: Bad number does not throw exception");
            } catch (NumberFormatException ignored) {
                // this is required
            }
        }

        assertEquals(args.getOperands().length, 0);
    }

    @SuppressWarnings({"ConfusingFloatingPointLiteral"})
    @Test
    public void testDoubleFormats() {
        MainArgs args = new MainArgs("test",
                new String[]{"-int", "13", "-val", "13.5", "-exp", "1.35e1",
                        "-hex", "0x135eP-1", "-inf", "Infinity", "-nan", "NaN",
                        "-bad", "NotANumber", "-int", "+13", "-val", "+13.5",
                        "-exp", "+1.35e1", "-hex", "+0x135eP-1", "-inf",
                        "+Infinity", "-nan", "+NaN", "-bad", "+NotANumber",
                        "-int", "-13", "-val", "-13.5", "-exp", "-1.35e1",
                        "-hex", "-0x135eP-1", "-inf", "-Infinity", "-nan",
                        "-NaN", "-bad", "-NotANumber",});

        assertEquals(args.getDouble("int", -1), 13, 0.00001, "int");
        assertEquals(args.getDouble("int", -1), +13, 0.00001, "int");
        assertEquals(args.getDouble("int", -1), -13, 0.00001, "int");

        assertEquals(args.getDouble("val", -1), 13.5, 0.00001, "val");
        assertEquals(args.getDouble("val", -1), +13.5, 0.00001, "val");
        assertEquals(args.getDouble("val", -1), -13.5, 0.00001, "val");

        assertEquals(args.getDouble("exp", -1), 13.5, 0.00001, "exp");
        assertEquals(args.getDouble("exp", -1), +13.5, 0.00001, "exp");
        assertEquals(args.getDouble("exp", -1), -13.5, 0.00001, "exp");

        assertEquals(args.getDouble("hex", -1), 0x135eP-1, 0.00001, "hex");
        assertEquals(args.getDouble("hex", -1), +0x135eP-1, 0.00001, "hex");
        assertEquals(args.getDouble("hex", -1), -0x135eP-1, 0.00001, "hex");

        assertEquals(args.getDouble("inf", -1), Double.POSITIVE_INFINITY,
                0.00001, "inf");
        assertEquals(args.getDouble("inf", -1), Double.POSITIVE_INFINITY,
                0.00001, "inf");
        assertEquals(args.getDouble("inf", -1), Double.NEGATIVE_INFINITY,
                0.00001, "inf");

        assertTrue(Double.isNaN(args.getDouble("nan", -1)), "Nan");
        assertTrue(Double.isNaN(args.getDouble("nan", -1)), "Nan");
        assertTrue(Double.isNaN(args.getDouble("nan", -1)), "Nan");

        for (int i = 0; i < 3; i++) {
            try {
                args.getDouble("bad", -1);
                fail("bad: Bad number does not throw exception");
            } catch (NumberFormatException ignored) {
                // this is required
            }
        }

        assertEquals(args.getOperands().length, 0);
    }

    @Test
    public void testSimplestUsage() {
        MainArgs args = new MainArgs("test");
        args.getBoolean("bool");
        args.getInt("int", 0);
        args.getOperands("...");
        assertEquals(args.synopsisString(),
                "test [-bool] [-int int] [-help] ...\n");
        assertEquals(args.usageString(), args.synopsisString());
    }

    @Test
    public void testSimpleSingleUsage() {
        MainArgs args = new MainArgs("test");
        args.getBoolean("b|bool", "whether to do something");
        args.getInt("i|int", 0, "ival", "some integer");
        args.getDouble("double", 0, "dval", "a double of some kind");
        args.getLong("|long", 0, "upper", "long-ish");
        args.getOperands("file", "...");
        String synopsis =
                "test [-b, --bool] [-i, --int ival] [--double dval] [--long upper] [--help] file ...\n";
        assertEquals(args.synopsisString(), synopsis);
        assertEquals(args.usageString(),
                synopsis + "    -b, --bool      whether to do something\n" +
                        "    -i, --int ival  some integer\n" +
                        "    --double dval   a double of some kind\n" +
                        "    --long upper    long-ish\n" +
                        "    --help          Print help message\n");
    }

    @Test
    public void testGroups() {
        MainArgs args = new MainArgs("test");

        args.optionGroup("Basic");
        args.getBoolean("b|bool", "whether to do something");

        args.optionGroup("Number");
        args.getInt("i|int", 0, "ival", "some integer");
        args.getDouble("double", 0, "dval", "a double of some kind");
        args.getLong("|long", 0, "upper", "long-ish");

        args.getOperands("file", "...");

        String synopsis =
                "test [-b, --bool] [-i, --int ival] [--double dval] [--long upper] [--help] file ...\n";
        assertEquals(args.synopsisString(), synopsis);
        assertEquals(args.usageString(), synopsis + "Basic Options:\n" +
                "    -b, --bool      whether to do something\n" + "\n" +
                "Number Options:\n" + "    -i, --int ival  some integer\n" +
                "    --double dval   a double of some kind\n" +
                "    --long upper    long-ish\n" + "\n" + "Help Options:\n" +
                "    --help          Print help message\n");
    }

    @Test
    public void testClassAsProgram() {
        MainArgs args = new MainArgs(TestMainArgs.class);
        args.getOperands("stuff", "...");
        String synopsis = "TestMainArgs [-help] stuff ...\n";
        assertEquals(args.synopsisString(), synopsis);
        assertEquals(args.usageString(), synopsis);
    }
}