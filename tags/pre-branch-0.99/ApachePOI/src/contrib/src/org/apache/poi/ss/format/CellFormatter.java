package org.apache.poi.ss.format;

import java.util.logging.Logger;
import java.util.Locale;

/** This is the abstract supertype for the various cell formatters. */
public abstract class CellFormatter {
    /** The original specified format. */
    protected final String format;

    /**
     * This is the locale used to get a consistent format result from which to
     * work.
     */
    public static final Locale LOCALE = Locale.US;

    /**
     * Creates a new formatter object, storing the format in {@link #format}.
     *
     * @param format The format.
     */
    public CellFormatter(String format) {
        this.format = format;
    }

    /** The logger to use in the formatting code. */
    static final Logger logger = Logger.getLogger(
            CellFormatter.class.getName());

    /**
     * Format a value according the format string.
     *
     * @param toAppendTo The buffer to append to.
     * @param value      The value to format.
     */
    public abstract void formatValue(StringBuffer toAppendTo, Object value);

    /**
     * Format a value according to the type, in the most basic way.
     *
     * @param toAppendTo The buffer to append to.
     * @param value      The value to format.
     */
    public abstract void simpleValue(StringBuffer toAppendTo, Object value);

    /**
     * Formats the value, returning the resulting string.
     *
     * @param value The value to format.
     *
     * @return The value, formatted.
     */
    public String format(Object value) {
        StringBuffer sb = new StringBuffer();
        formatValue(sb, value);
        return sb.toString();
    }

    /**
     * Formats the value in the most basic way, returning the resulting string.
     *
     * @param value The value to format.
     *
     * @return The value, formatted.
     */
    public String simpleFormat(Object value) {
        StringBuffer sb = new StringBuffer();
        simpleValue(sb, value);
        return sb.toString();
    }

    /**
     * Returns the input string, surrounded by quotes.
     *
     * @param str The string to quote.
     *
     * @return The input string, surrounded by quotes.
     */
    static String quote(String str) {
        return '"' + str + '"';
    }
}