package org.apache.poi.ss.format;

/** The different kinds of formats that the formatter understands. */
public enum CellFormatType {

    /** The general (default) format; also used for <tt>"General"</tt>. */
    GENERAL {
        CellFormatter formatter(String pattern) {
            return new CellGeneralFormatter();
        }
        boolean isSpecial(char ch) {
            return false;
        }
    },
    /** A numeric format. */
    NUMBER {
        boolean isSpecial(char ch) {
            return false;
        }
        CellFormatter formatter(String pattern) {
            return new CellNumberFormatter(pattern);
        }
    },
    /** A date format. */
    DATE {
        boolean isSpecial(char ch) {
            return ch == '\'' || (ch <= '\u007f' && Character.isLetter(ch));
        }
        CellFormatter formatter(String pattern) {
            return new CellDateFormatter(pattern);
        }
    },
    /** An elapsed time format. */
    ELAPSED {
        boolean isSpecial(char ch) {
            return false;
        }
        CellFormatter formatter(String pattern) {
            return new CellElapsedFormatter(pattern);
        }
    },
    /** A text format. */
    TEXT {
        boolean isSpecial(char ch) {
            return false;
        }
        CellFormatter formatter(String pattern) {
            return new CellTextFormatter(pattern);
        }
    };

    /**
     * Returns <tt>true</tt> if the format is special and needs to be quoted.
     *
     * @param ch The character to test.
     *
     * @return <tt>true</tt> if the format is special and needs to be quoted.
     */
    abstract boolean isSpecial(char ch);

    /**
     * Returns a new formatter of the appropriate type, for the given pattern.
     * The pattern must be appropriate for the type.
     *
     * @param pattern The pattern to use.
     *
     * @return A new formatter of the appropriate type, for the given pattern.
     */
    abstract CellFormatter formatter(String pattern);
}