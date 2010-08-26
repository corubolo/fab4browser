package com.im.util;

import org.simplx.c.Pointer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLib {
    public static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private static final Pattern INT_PATTERN = Pattern.compile(
            "\\s*[-+]?(0x[0-9a-f]+|0[0-7]+|[0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOAT_PATTERN;

    static {
        // This code is copied from the javadoc for java.lang.Double.  Why they
        // didn't make this a field of Double is beyond me...
        String digits = "(\\p{Digit}+)";
        String hexDigits = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        String exp = "[eE][+-]?" + digits;
        String fpRegex = ("[\\x00-\\x20]*" +  // Optional leading "whitespace"
                "[+-]?(" + // Optional sign character
                "NaN|" +           // "NaN" string
                "Infinity|" +      // "Infinity" string

                // A decimal floating-point string representing a finite positive
                // number without a leading sign has at most five basic pieces:
                // Digits . Digits ExponentPart FloatTypeSuffix
                //
                // Since this method allows integer-only strings as input
                // in addition to strings of floating-point literals, the
                // two sub-patterns below are simplifications of the grammar
                // productions from the Java Language Specification, 2nd
                // edition, section 3.10.2.

                // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                "(((" + digits + "(\\.)?(" + digits + "?)(" + exp + ")?)|" +

                // . Digits ExponentPart_opt FloatTypeSuffix_opt
                "(\\.(" + digits + ")(" + exp + ")?)|" +

                // Hexadecimal strings
                "((" +
                // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                "(0[xX]" + hexDigits + "(\\.)?)|" +

                // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                "(0[xX]" + hexDigits + "?(\\.)" + hexDigits + ")" +

                ")[pP][+-]?" + digits + "))" + "[fFdD]?))" +
                "[\\x00-\\x20]*");// Optional trailing "whitespace"

        FLOAT_PATTERN = Pattern.compile("^\\s*" + fpRegex);
    }

    public static void abort() {
        throw new IllegalStateException("explict abort requested");
    }

    public static boolean isascii(char c) {
        return c >= 0 && c <= 0x7f;
    }

    public static boolean isascii(byte c) {
        return c >= 0;
    }

    public static int hextoint(char c) {
        return Character.digit(c, 16);
    }

    public static int hextoint(byte c) {
        return hextoint((char) c);
    }

    public static boolean isprint(char c) {
        return !Character.isISOControl(c) && Character.getType(c) !=
                Character.CONTROL;
    }

    public static boolean isprint(byte c) {
        return isprint((char) c);
    }

    public static String strerror(int error) {
        return "Error " + error;
    }

    public static Long strtoull(String line, int l, int[] t) {
        String remainder = line.substring(l);
        Matcher matcher = INT_PATTERN.matcher(remainder);
        Long val = null;
        if (matcher.find()) {
            t[0] = matcher.end();
            String numStr = matcher.group();
            numStr = numStr.trim();
            if (numStr.charAt(0) == '+') {
                // decode() doesn't handle leading +
                numStr = numStr.substring(1);
            }
            val = Long.decode(numStr);
        }
        return val;
    }

    public static Float strtof(String line, int l, int[] t) {
        String remainder = line.substring(l);
        Matcher matcher = FLOAT_PATTERN.matcher(remainder);
        Float val = null;
        if (matcher.find()) {
            t[0] = matcher.end();
            String numStr = matcher.group();
            numStr = numStr.trim();
            val = Float.parseFloat(numStr);
        }
        return val;
    }

    public static Float strtof(Pointer l, Pointer[] t) {
        int[] tincr = new int[1];
        Float val = strtof(l.toString(), 0, tincr);
        t[0] = l.plus(tincr[0]);
        return val;
    }

    public static Double strtod(String line, int l, int[] t) {
        String remainder = line.substring(l);
        Matcher matcher = FLOAT_PATTERN.matcher(remainder);
        Double val = null;
        if (matcher.find()) {
            t[0] = matcher.end();
            String numStr = matcher.group();
            numStr = numStr.trim();
            val = Double.parseDouble(numStr);
        }
        return val;
    }

    public static Double strtod(Pointer l, Pointer[] t) {
        int[] tincr = new int[1];
        Double val = strtod(l.toString(), 0, tincr);
        t[0] = l.plus(tincr[0]);
        return val;
    }

    public static Integer strtoul(String line, int l, int[] t) {
        String remainder = line.substring(l);
        Matcher matcher = INT_PATTERN.matcher(remainder);
        Integer val = null;
        if (matcher.find()) {
            t[0] = matcher.end();
            String numStr = matcher.group();
            numStr = numStr.trim();
            if (numStr.charAt(0) == '+') {
                // decode() doesn't handle leading +
                numStr = numStr.substring(1);
            }
            val = Integer.decode(numStr);
        }
        return val;
    }

    public static Integer strtoul(Pointer str, Pointer[] t, int base) {
        Integer val = strtol(str, t, base);
        if (val < 0 && str.get() != '-') {
            throw new IllegalArgumentException(
                    "A large postiive long became negative");
        }
        return val;
    }

    public static Integer strtol(Pointer str, Pointer[] t, int base) {
        t[0] = str.copy();
        Matcher matcher = INT_PATTERN.matcher(str.toString());
        Integer val = null;
        if (matcher.find()) {
            t[0].incr(matcher.end());
            String numStr = matcher.group();
            numStr = numStr.trim();
            if (numStr.charAt(0) == '+') {
                // decode() doesn't handle leading +
                numStr = numStr.substring(1);
            }
            if (base == 0) {
                val = Integer.decode(numStr);
            } else {
                val = Integer.parseInt(numStr, base);
            }
        }
        return val;
    }

    public static Long strtoull(Pointer str, Pointer[] t, int base) {
        Long val = strtoll(str, t, base);
        if (val < 0 && str.get() != '-') {
            throw new IllegalArgumentException(
                    "A large postiive long became negative");
        }
        return val;
    }

    public static Long strtoll(Pointer str, Pointer[] t, int base) {
        t[0] = str.copy();
        Matcher matcher = INT_PATTERN.matcher(str.toString());
        Long val = null;
        if (matcher.find()) {
            t[0].incr(matcher.end());
            String numStr = matcher.group();
            numStr = numStr.trim();
            if (numStr.charAt(0) == '+') {
                // decode() doesn't handle leading +
                numStr = numStr.substring(1);
            }
            if (base == 0) {
                val = Long.decode(numStr);
            } else {
                val = Long.parseLong(numStr, base);
            }
        }
        return val;
    }

    public static String strrchr(String str, char c) {
        int pos = str.lastIndexOf(c);
        if (pos < 0) {
            return null;
        } else {
            return str.substring(pos);
        }
    }

    public static String strchr(String str, char c) {
        int pos = str.indexOf(c);
        if (pos < 0) {
            return null;
        } else {
            return str.substring(pos);
        }
    }

    public static Pointer strchr(Pointer str, int c) {
        return str.strchr((byte) c);
    }

    public static String strchr(String str, byte c) {
        return strchr(str, (char) c);
    }

    public static boolean isspace(char c) {
        return Character.isWhitespace(c);
    }

    public static boolean isspace(byte c) {
        return Character.isWhitespace((char) c);
    }

    public static boolean islower(char c) {
        return Character.isLowerCase(c);
    }

    public static boolean islower(byte c) {
        return islower((char) c);
    }

    public static boolean isupper(char c) {
        return Character.isLowerCase(c);
    }

    public static boolean isupper(byte c) {
        return Character.isLowerCase((char) c);
    }

    public static char tolower(char c) {
        return Character.toLowerCase(c);
    }

    public static char tolower(byte c) {
        return Character.toLowerCase((char) c);
    }

    public static char toupper(char c) {
        return Character.toLowerCase(c);
    }

    public static char toupper(byte c) {
        return Character.toLowerCase((char) c);
    }

    public static char toChar(byte b) {
        if (b < 0x7f) {
            return (char) b;
        }
        return (char) (((short) b) & 0xff);
    }

    static int strcspn(String s1, String s2) {
        int span = s1.length();
        for (int i = 0; i < s2.length(); i++) {
            int pos = s1.indexOf(s2.charAt(i));
            if (pos >= 0) {
                span = Math.min(pos, span);
            }
        }
        if (span == s1.length()) {
            // never found an occurrance in s1 of anything from s2
            return span;
        } else {
            // span is the lowest index of a character, so the length is +1
            return span + 1;
        }
    }

    public static int strcspn(Pointer s1, String s2) {
        return s1.strcspn(s2);
    }

    public static int strlen(byte[] str) {
        int i;
        for (i = 0; str[i] != '\0'; i++) {
            continue;
        }
        return i;
    }

    public static int strlen(Pointer p) {
        return p.strlen();
    }

    public static byte[] fgets(BufferedInputStream f, byte[] buf) {
        try {
            int b;
            int i;
            int end = buf.length - 1;   // leave space for '\0' to mark the end
            for (i = 0; i < end; i++) {
                b = f.read();
                if (b < 0) {
                    buf[i] = '\0';
                    f.close();
                    return buf;
                }
                buf[i] = (byte) b;
                if (b == '\n') {
                    break;
                }
            }
            buf[i + 1] = '\0';
            return buf;
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isdigit(byte b) {
        return ('0' <= b && b <= '9');
    }

    public static boolean isalnum(byte b) {
        return Character.isLetter(b) || Character.isDigit(b);
    }

    public static int memcmp(Pointer m1, Pointer m2, int len) {
        return m1.strncmp(m2, len);
    }

    public static int strcmp(Pointer s1, Pointer s2) {
        return s1.strcmp(s2);
    }

    public static boolean isodigit(byte digit) {
        return digit >= '0' && digit <= '7';
    }

    public static int strcmp(String s1, String s2) {
        return s1.compareTo(s2);
    }

    public static Pointer memchr(Pointer sOrig, char c, int count) {
        Pointer s = sOrig.copy();
        while (--count >= 0) {
            if (s.get() == c) {
                return s;
            }
            s.incr();
        }
        return null;
    }

    public static int strncmp(String s1, String s2, int len) {
        if (s1.regionMatches(0, s2, 0, len)) {
            return 0;
        }
        return s1.compareTo(s2);
    }

    public static Pointer encode_utf8(Pointer bufOrig, int len, char[] ubuf,
            int ulen) {

        Pointer buf = bufOrig.copy();
        int i;
        Pointer end = new Pointer(buf, len);

        for (i = 0; i < ulen; i++) {
            if (ubuf[i] <= 0x7f) {
                if (end.minus(buf) < 1) {
                    return null;
                }
                buf.setIncr(ubuf[i]);
            } else if (ubuf[i] <= 0x7ff) {
                if (end.minus(buf) < 2) {
                    return null;
                }
                buf.setIncr((ubuf[i] >> 6) + 0xc0);
                buf.setIncr((ubuf[i] & 0x3f) + 0x80);
            } else if (ubuf[i] <= 0xffff) {
                if (end.minus(buf) < 3) {
                    return null;
                }
                buf.setIncr((ubuf[i] >> 12) + 0xe0);
                buf.setIncr((ubuf[i] >> 6 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] & 0x3f) + 0x80);
            } else if (ubuf[i] <= 0x1fffff) {
                if (end.minus(buf) < 4) {
                    return null;
                }
                buf.setIncr((ubuf[i] >> 18) + 0xf0);
                buf.setIncr((ubuf[i] >> 12 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] >> 6 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] & 0x3f) + 0x80);
            } else if (ubuf[i] <= 0x3ffffff) {
                if (end.minus(buf) < 5) {
                    return null;
                }
                buf.setIncr((ubuf[i] >> 24) + 0xf8);
                buf.setIncr((ubuf[i] >> 18 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] >> 12 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] >> 6 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] & 0x3f) + 0x80);
            } else if (ubuf[i] <= 0x7fffffff) {
                if (end.minus(buf) < 6) {
                    return null;
                }
                buf.setIncr((ubuf[i] >> 30) + 0xfc);
                buf.setIncr((ubuf[i] >> 24 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] >> 18 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] >> 12 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] >> 6 & 0x3f) + 0x80);
                buf.setIncr((ubuf[i] & 0x3f) + 0x80);
            } else {
                /* Invalid character */
                return null;
            }
        }

        return buf;
    }
}