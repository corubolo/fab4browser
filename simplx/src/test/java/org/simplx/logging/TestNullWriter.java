package org.simplx.logging;

import static org.simplx.logging.NullLogger.NULL_FILTER;
import static org.simplx.logging.NullLogger.NULL_LOGGER;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.util.logging.Filter;
import java.util.logging.Level;
import static java.util.logging.Level.OFF;
import java.util.logging.LogRecord;

public class TestNullWriter {
    static class MaxLevel extends Level {
        MaxLevel() {
            super("MAX", Integer.MAX_VALUE);
        }
    }

    private static final Level MAX = new MaxLevel();

    @Test
    public void testNullWriter() {
        NULL_LOGGER.log(MAX, "Maximum message");
        assertFalse(NULL_LOGGER.isLoggable(MAX));
        assertEquals(NULL_LOGGER.getLevel(), OFF);

        NULL_LOGGER.setLevel(Level.ALL);
        assertFalse(NULL_LOGGER.isLoggable(MAX));
        assertEquals(NULL_LOGGER.getLevel(), OFF);

        assertSame(NULL_FILTER, NULL_LOGGER.getFilter());
        NULL_LOGGER.setFilter(new Filter() {
            public boolean isLoggable(LogRecord record) {
                throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        assertSame(NULL_FILTER, NULL_LOGGER.getFilter());

        assertEquals(NULL_LOGGER.getLevel(), OFF);
        NULL_LOGGER.setLevel(Level.ALL);
        assertEquals(NULL_LOGGER.getLevel(), OFF);

        assertFalse(NULL_LOGGER.getUseParentHandlers());
        NULL_LOGGER.setUseParentHandlers(true);
        assertFalse(NULL_LOGGER.getUseParentHandlers());
    }

    @Test
    public void testNullFilter() {
        LogRecord lr = new LogRecord(MAX, "Never seen");
        assertFalse(NULL_FILTER.isLoggable(lr));
    }
}