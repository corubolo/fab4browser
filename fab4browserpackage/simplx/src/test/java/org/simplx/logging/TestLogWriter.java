package org.simplx.logging;

import org.apache.commons.lang.ArrayUtils;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.regex.Pattern;

public class TestLogWriter {
    @Test
    public void logPrefix() throws IOException {
        checkLogger("PRE:", Level.INFO);
    }

    @Test
    public void logNoPrefix() throws IOException {
        checkLogger("", Level.INFO);
    }

    @Test
    public void logNullPrefix() throws IOException {
        checkLogger(null, Level.INFO);
    }

    private void checkLogger(String prefix, Level logLevel) throws IOException {
        Logger logger = Logger.getLogger("testLogWriter");
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        SimpleFormatter formatter = new SimpleFormatter();
        StreamHandler handler = new StreamHandler(byteOut, formatter);
        logger.addHandler(handler);
        logger.setLevel(Level.FINE);

        String msg = "First |line\n|" + "Next line\n" + "Line after|\n|" +
                "Man|y\n" + "more\n" + "lines\n" + "than\n" + "tha|t\n" +
                "F|i|na|l| |li|n|e\n";
        char[] msgChars = msg.toCharArray();

        LogWriter out = new LogWriter(logger, logLevel, prefix);
        assertTrue(out.isLogging());
        int end = -1;
        for (; ;) {
            int start = end + 1;
            end = ArrayUtils.indexOf(msgChars, '|', start + 1);
            if (end < 0) {
                end = msgChars.length;
            }
            out.write(msgChars, start, end - start);
            if (end == msgChars.length) {
                break;
            }
        }
        handler.close();

        String noPipes = msg.replaceAll("\\|", "");
        Pattern addPrefixPat = Pattern.compile("^", Pattern.MULTILINE);
        String expStr;
        if (prefix == null) {
            expStr = noPipes;
        } else {
            expStr = addPrefixPat.matcher(noPipes).replaceAll(prefix);
        }

        String infoPrefix = logLevel.getName() + ": ";
        String logString = byteOut.toString();
        BufferedReader logIn = new BufferedReader(new StringReader(logString));
        String logLine;
        StringWriter actOut = new StringWriter();
        while ((logLine = logIn.readLine()) != null) {
            if (logLine.startsWith(infoPrefix)) {
                actOut.write(logLine.substring(infoPrefix.length()));
                actOut.write('\n');
            }
        }
        String actStr = actOut.toString();

        assertEquals(actStr, expStr);
    }
}