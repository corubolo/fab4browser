package org.apache.poi.ss.format;

import java.awt.*;

/**
 * This object contains the result of applying a cell format or cell format part
 * to a value.
 *
 * @see CellFormatPart#apply(Object)
 * @see CellFormat#apply(Object)
 */
public class CellFormatResult {
    /**
     * This is <tt>true</tt> if no condition was given that applied to the
     * value, or if the condition is satisfied.  If a condition is relevant, and
     * when applied the value fails the test, this is <tt>false</tt>.
     */
    public final boolean applies;

    /** The resulting text.  This will never be <tt>null</tt>. */
    public final String text;

    /**
     * The color the format sets, or <tt>null</tt> if the format sets no color.
     * This will always be <tt>null</tt> if {@link #applies} is <tt>false</tt>.
     */
    public final Color textColor;

    /**
     * Creates a new format result object.
     *
     * @param applies   The value for {@link #applies}.
     * @param text      The value for {@link #text}.
     * @param textColor The value for {@link #textColor}.
     */
    public CellFormatResult(boolean applies, String text, Color textColor) {
        this.applies = applies;
        this.text = text;
        this.textColor = (applies ? textColor : null);
    }
}