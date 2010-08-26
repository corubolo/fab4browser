package org.simplx.logging;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.util.logging.Logger;

public class TestSimplxLogging {
    // Test that it is possible to sublcass this class
    static class CanExtend extends SimplxLogging {
    }

    @Test
    public void testExtensible() {
        assertNotNull(new CanExtend());
    }

    @Test
    public void testSimplxLogging() {
        Logger logger = SimplxLogging.loggerFor("String");
        assertEquals(logger.getName(), "java.lang.String");
        assertSame(logger, SimplxLogging.loggerFor("another string"));
        assertSame(logger, SimplxLogging.loggerFor(String.class));
        Object classObj = String.class;
        assertSame(logger, SimplxLogging.loggerFor(classObj));
    }
}