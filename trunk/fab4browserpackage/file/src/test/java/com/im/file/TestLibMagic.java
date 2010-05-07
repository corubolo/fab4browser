package com.im.file;

import org.apache.commons.io.IOUtils;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TestLibMagic {

    private MagicSet batchSet;

    private static boolean doneSetMagic = false;

    private static class TestStreamIterator implements Iterator<Object[]> {
        int nextTestNum = 0;
        Object[] nextParams = null;
        int state = READY_TO_SEARCH_FOR_NEXT;

        private static final int READY_TO_SEARCH_FOR_NEXT = 0;
        private static final int HAS_FILE_TO_RETURN = 1;
        private static final int READY_TO_RETURN_END_MARKER = 2;
        private static final int FINISHED = 3;

        @Override
        public boolean hasNext() {
            switch (state) {
            case HAS_FILE_TO_RETURN:
                return true;
            case READY_TO_SEARCH_FOR_NEXT:
                nextParams = new Object[3];
                int lastTry = nextTestNum + 10;
                System.out.println("lastTry = " + lastTry);
                do {
                    String base = "t" + nextTestNum++;
                    InputStream in = null;
                    InputStream out = null;
                    try {
                        in = getStream(base + ".in");
                        out = getStream(base + ".out");
                        if (in == null) {
                            nextParams[0] = base;
                        }
                        nextParams[0] = base;
                        nextParams[1] = in;
                        nextParams[2] = out;
                        System.out.println("Found " + Arrays.toString(
                                nextParams));
                        state = HAS_FILE_TO_RETURN;
                        return true;
                    } catch (Exception e) {
                        System.out.println(
                                "Tried " + base + ": " + e.getMessage());
                        // these may have been opened
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);

                        Arrays.fill(nextParams, null);
                    }
                } while (nextTestNum < lastTry);
                if (nextParams[0] == null) {
                    state = READY_TO_RETURN_END_MARKER;
                }
                return true;
            case READY_TO_RETURN_END_MARKER:
                return true;
            case FINISHED:
            default:
                return false;
            }
        }

        private InputStream getStream(String name) throws IOException {
            InputStream stream = getClass().getResourceAsStream(name);
            if (stream == null) {
                throw new IOException("Could not get resource " + name);
            }
            return stream;
        }

        @Override
        public Object[] next() {
            switch (state) {
            case READY_TO_SEARCH_FOR_NEXT:
                hasNext();  // it wasn't called already
                //noinspection fallthrough
            case HAS_FILE_TO_RETURN:
                state = READY_TO_SEARCH_FOR_NEXT;
                return nextParams;
            case READY_TO_RETURN_END_MARKER:
                state = FINISHED;
                return new Object[3];
            case FINISHED:
            default:
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @AfterClass
    public static void restoreMagicFile() {
        if (!doneSetMagic) {
            return;
        }
        System.getProperties().remove("MAGIC");
        doneSetMagic = false;
    }

    @DataProvider(name = "streams")
    public Iterator<Object[]> streams() {
        return new TestStreamIterator();
    }

    @Parameters({"flags"})
    @Test
    public void createBatch(@Optional("0") int flags) {
        batchSet = MagicSet.open(flags);
        assertTrue(batchSet.load(null));
    }

    @Test(dataProvider = "streams", dependsOnMethods = "createBatch")
    public void testStreamBatch(String name, InputStream in,
            InputStream expected) throws IOException {

        if (name == null) {
            batchSet.close();
            batchSet = null;
            return;
        }
        tryOneStream(batchSet, name, in, expected);
    }

    private void tryOneStream(MagicSet ms, String name, InputStream in,
            InputStream expected) throws IOException {

        try {
            System.out.println("name = " + name);
            if (ms == null) {
                ms = MagicSet.open(0);
                assertTrue(ms.load(null));
            }
            String actual = ms.stream(in);
            String expectedText = expected(name, expected);
            assertEquals(actual, expectedText, name);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(expected);
            if (ms != null) {
                ms.close();
            }
        }
    }

    private String expected(String name, InputStream expected)
            throws IOException {
        StringWriter out = new StringWriter();
        InputStreamReader in = new InputStreamReader(expected);
        char[] buf = new char[8 * 1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();

        StringBuilder sb = new StringBuilder(out.toString());

        // Strip off leading name (we're reading from a stream so it's not present)
        String filePrefix = name + ".in:";
        if (out.toString().startsWith(filePrefix)) {
            sb.delete(0, filePrefix.length() + 1);
        }

        return sb.toString().trim();
    }
}