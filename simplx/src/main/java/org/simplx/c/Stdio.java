/*
 * Copyright (c) 2009, 2010, Ken Arnold All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the myself nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @SimplxCopyright
 */

package org.simplx.c;

import org.simplx.regex.RegexUtils;
import org.simplx.regex.RegexUtils.Replacer;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is designed to help with C code based on the {@code FILE*} type in
 * {@code <stdio.h>}.  The output functions there are made available here.
 * <p/>
 * Most of the work is to help {@code printf}-using code translated from C to
 * Java.  As far as possible, it emulates C's {@code printf} on top of Java's
 * {@link Formatter#format(String,Object...)} Formatter.format}, the base for
 * {@link PrintStream#printf(String,Object...) printf}.  Although many
 * formatting rules are shared between the two, there are many differences,
 * subtle or otherwise.
 * <p/>
 * First, a terminology note: For this document you will see {@code format} used
 * to refer to the Java printf-like functionality, and {@code printf(3)} to
 * refer to the C printf function.
 * <p/>
 * The approach taken here is to make it possible to take a {@code printf(3)}
 * call from a C or C++ program and use the same format with the same values in
 * your Java code, and get appropriate results.  This includes the following:
 * <ul> <li><b>Unsigned formats:</b> C has unsigned integer types, and {@code
 * printf(3)} has the {@code %u} and {@code %U} formats to print out unsigned
 * integers. <li><b>Generous parsing:</b> {@code printf(3)} ignores irrelevant
 * flags, such as how to handle signs for strings. {@code format} throws
 * exceptions for them. {@code printf(3)} tries hard to apply flags where it is
 * possible to assign a meaning.  For example, if you use the {@code 0} flag to
 * put leading zeros on a string instead of a number, {@code printf(3)} will do
 * it, while {@code format} will throw an exception. <li><b>Small
 * differences:</b> {@code format} just does some things differently from {@code
 * printf(3)}.  Some of these are motivated (in C, {@code char} is a kind of
 * integer, in Java it is not, and the printf's reflect this), others just
 * happen ({@code %g} trims trailing zeros in {@code printf(3)}, but not in
 * {@code format}. </ul> <h3>Major Details</h3> The following are the details
 * you are most likely to find useful when using {@code Stdio}.
 * <p/>
 * <b>C Machine Model</b> &#8212; You may have noticed the term "appropriate
 * output" used to describe matching what {@code printf(3)} would produce.  This
 * is because C can have different behavior on different platforms, where Java's
 * behavior is nearly entirely specified.  The behavior of {@code printf(3)} can
 * only be specified in the context of particular choices within those allowed
 * by the C standard.  This code assumes the following, quite common, choices:
 * that integers are represented in two's-complement format; that {@code int}
 * values are 32 bits; that {@code long long} values are 64 bit; that
 * floating-point is IEEE 754, with {@code float} as 32 bits and {@code double}
 * as 64 bits.
 * <p/>
 * <b>%n</b> &#8212; In {@code printf(3)}, {@code %n} uses its corresponding
 * argument as an {@code int*} to store the number of characters output by the
 * printf up to that point.  In Java, you instead pass an array, into whose
 * first element will be stored that value.  The type of the array will be
 * {@code int} unless the length modifier says others; for example, {@code %ld}
 * will expect a {@code long} array.
 * <p/>
 * <b>%p</b> &#8212; The {@code %p} format in {@code printf(3)} prints out
 * pointers in hex.  There are no pointers in C.  The closest we can do is to
 * provide each object a unique integer by which it can be compared for identity
 * with other objects.  The first time an object is printed with {@code %c}, it
 * is assigned a unique non-zero integer.  Every time that object is used for a
 * {@code %p} with any {@code Stdio} object in the same Java virtual machine,
 * the same integer will be used.  These integers are stored via weak references
 * so they do not prevent the object being garbage collected.  In strict mode,
 * {@code %p} causes an exception.
 * <p/>
 * <b>Type Modifiers</b> &#8212; In C, it is important that you match the
 * variable type with the appropriate length modifier.  For example, you must
 * use {@code %hd} for a {@code short}, {@code %d} for an {@code int}, and
 * {@code %ld} for a {@code long}.  In Java this is not important -- {@code
 * format} uses the type of object is is given ({@link Short}, {@link Integer},
 * {@link Long}, ...) to know what kind of value it has.  The only value in
 * using the length modifiers with {@code Stdio} is to perform any truncation
 * that would be done in C.  For example, using {@code %hx} with an {@code int}
 * value will result in truncation of the {@code int} to a {@code short} before
 * printing.
 * <p/>
 * <b>Conversion</b> &#8212; In C, if you pass an {@code int} to a {@code %f},
 * you will get the {@code int} treated as a bitwise-converted {@code float}.
 * Similarly, a {@code float} use with {@code %d} would be bitwise-converted the
 * other way. Correspondingly, {@code Stdio} uses {@link
 * Float#intBitsToFloat(int)} and {@link Double#longBitsToDouble(long)} to
 * preserve this bitwise conversion behavior.
 * <p/>
 * <b>Newlines</b> &#8212; In C, the normal way to specify a newline using
 * {@code \n}.  {@code format} uses {@code %n} to represent a newline, which is
 * translated to the local systems newline sequence.  {@code Stdio} translates
 * {@code \n} in the {@code printf(3)} format string to {@code %n}.
 * <p/>
 * <h3>Strict Mode</h3>
 * <p/>
 * There are some constructs that are impossible or difficult to translate into
 * the Java {@code format} world.  The primary example of this is {@code %p},
 * which prints out pointers as hex numbers.  Java has no pointers, and the
 * information you can glean from pointers is simply not available.  The {@link
 * Stdio} object can be put in "strict" mode, where it will throw an exception
 * in any case where it cannot produce proper {@code printf(3)} output. You can
 * set strict mode by the {@link #setStrict(boolean)} method.
 * <p/>
 * When in strict mode, the following are not supported, and cause {@link
 * IllegalArgumentException}.
 * <p/>
 * <b>%p</b> &#8212; As noted above, {@code %p} is not supported.
 * <p/>
 * <b>AltiVec syntax</b> &#8212; The AltiVec format (an extension that allows
 * formats for arrays) is not supported.
 * <p/>
 * <b>%C</b> &#8212; In {@code printf(3)}, {@code %C} means to print out a
 * {@code wchar_t} wide character.  In Java all characters are wide so this is
 * treated the same {@code %c}, except in strict mode.
 * <p/>
 * <b>%S</b> &#8212; In {@code printf(3)}, {@code %S} means to print out a
 * {@code wchar_t*} wide character string.  In Java all strings are wide so this
 * is treated the same {@code %s}, except in strict mode.
 * <p/>
 * <b>Leading Zeros</b> &#8212; {@code printf(3)} support leading zeros as a
 * padding for any field type, including strings and characters.  Normally the
 * {@code 0} flag is dropped, but in strict mode you will get an exception.
 * <p/>
 * <b>Zero Precision</b> &#8212; In {@code printf(3)}, {@code %a}/{@code %A} a
 * precision of zero means that no decimal point will be displayed.  The {@code
 * 0} flag is ignored, but in "strict" you will get an exception.
 * <p/>
 * <b>Localization</b> &#8212; The documentation for the {@code '} flag says
 * that it uses the locale to do proper grouping by "thousands".  It is not very
 * specific about what this really means.  Localization touches on many things,
 * one of which is whether numbers are even grouped by "thousands" or by other
 * means.  The implementation of {@code '} is done by full localization, not
 * limited to the question of "thousands".  Whether this matches the
 * specification or not is unclear, so this difference (if it is one) is not
 * flagged by the "strict" flag.
 */
public class Stdio implements Closeable, Flushable, Appendable {
    /**
     * The stdio EOF (end-of-file) marker, which is used widely as an error
     * return value.  This is a stdio analog.
     */
    public static final int EOF = -1;

    private final Formatter formatter;
    private boolean strict = false;
    private boolean autoFlush = false;
    private boolean hasError;
    private IOException lastSeenException;

    /** The integer values mapped for %p. */
    @SuppressWarnings({"unchecked"})
    private static final Map<Object, Integer> pointerVals =
            new WeakHashMap<Object, Integer>();

    /** Used for translating \n to %n. */
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\n");

    /**
     * Used to find trailing zeros.  This is only concerned with trailing zeros
     * on floating-point numbers, both those with exponents and those without.
     */
    private static final Pattern ZERO_TRAILING_PATTERN = Pattern.compile(
            "([0-9]\\.[0-9]*?)(0*)([eE][-+]?[0-9]+)?( *)$");

    private static final int FLOAT_MAIN = 1;
    private static final int TRAILING_ZEROS = FLOAT_MAIN + 1;
    private static final int EXPONENT = TRAILING_ZEROS + 1;
    @SuppressWarnings({"UnusedDeclaration"})
    private static final int TRAILING_SPACES = EXPONENT + 1;

    /**
     * The pattern used to find C format statements in strings.  This is as
     * precise as possible so as to ignore anything that only mostly looks like
     * a printf pattern, but isn't.  printf(3) would ignore them, and so should
     * we.
     */
    static final Pattern C_FORMAT_PATTERN;

    static {
        String pat = "%                         # The leading %\n" +
                "([0-9]+\\$)?                   # Positional parameter\n" +
                "([-\\#0\\ +']*)?               # flags\n" + // "#" needs the \
                "([,;:_])?                      # AltiVec/SSE Vector sep char\n" +
                "(                              # Width is either ...\n" +
                "  ([0-9]+)|                    #   a specific number or ...\n" +
                "  \\*([0-9]+\\$)?              #   a '*', possibly positional\n" +
                ")?                             # (width is optional)\n" +
                "(?:\\.                         # Precision is '.' and either ...\n" +
                "  (([0-9]+)|                   #   a specific number or ...\n" +
                "  \\*([0-9]+\\$)?)             #   a '*', possibly positional\n" +
                ")?                             # (precision is optional)\n" +
                "([hljtzqLv]|hh|ll|vh|hv|vl|lv|vll|llv)?   # length modifier\n" +
                "([diouxXDOUeEfFgGaACcSspn%])   # The conversion spec\n";
        C_FORMAT_PATTERN = Pattern.compile(pat, Pattern.COMMENTS);
    }

    // The group numbers for the groups in C_FORMAT_PATTERN
    static final int ARG_POSITION = 1;
    static final int FLAGS = ARG_POSITION + 1;
    static final int ALTIVEC_SEP = FLAGS + 1;
    static final int WIDTH_SPEC = ALTIVEC_SEP + 1;
    static final int WIDTH_NUMBER = WIDTH_SPEC + 1;
    static final int WIDTH_FROM_ARGS = WIDTH_NUMBER + 1;
    static final int PRECISION_SPEC = WIDTH_FROM_ARGS + 1;
    static final int PRECISION_NUMBER = PRECISION_SPEC + 1;
    static final int PRECISION_FROM_ARGS = PRECISION_NUMBER + 1;
    static final int MODIFIER = PRECISION_FROM_ARGS + 1;
    static final int CONVERSION = MODIFIER + 1;

    static final String NO_ALT_CONVERSIONS = "cCdDiInNpPsSuU";
    static final String SIGNED_CONVERSIONS = "dDiIeEfFgGaA";
    static final String INT_CONVERSIONS = "diouDOUxX";
    static final String FLOAT_CONVERSIONS = "eEfFgGaA";

    /**
     * This maps a given modifier (hh, l, L, ...) to the conversions that
     * support it.  I.e., it can tell if %hhs is or is not supported.
     */
    private static final Map<String, String> MODIFIER_MAP;

    static {
        MODIFIER_MAP = new HashMap<String, String>();
        MODIFIER_MAP.put("hh", "diouxXn");
        MODIFIER_MAP.put("h", "diouxXn");
        MODIFIER_MAP.put("l", "diouxXnaAeEfFgGcs");
        MODIFIER_MAP.put("ll", "diouxXn");
        MODIFIER_MAP.put("L", "aAeEfFgG");
        MODIFIER_MAP.put("j", "diouxXn");
        MODIFIER_MAP.put("t", "diouxXn");
        MODIFIER_MAP.put("z", "diouxXn");
        MODIFIER_MAP.put("q", "diouxXn");
    }

    /**
     * See comment for replace().
     *
     * @see #replace(Matcher)
     */
    class ToJava implements Replacer {
        private final Object[] args;
        private int nextArg;

        ToJava(Object... args) {
            this.args = args;
            nextArg = 0;
        }

        /**
         * Replace a single C format specification/argument set with a Java
         * equivalent.
         *
         * @param m The matcher that has matched a single C format
         *          specification.
         *
         * @return The string with which to replace it.
         */
        public String replace(Matcher m) {
            // Make sure that our constants match the actual pattern
            assert m.groupCount() == CONVERSION;

            char rawConv = getConversion(m);

            // This method call order consumes parameters in the right order:
            String width = getWidth(m);             // #1 get any * width
            String rawPrecision = getPrecision(m);  // #2 get any * precision
            int pos = -1;
            if (rawConv != '%') {
                pos = positionalArg(m, ARG_POSITION); // #3 get the value
            }

            // Someday we may actually decide to handle this
            String altivecSpec = getAltiVec(m);

            String precision = applyPrecision(rawPrecision, rawConv);

            String flags = getFlags(m, rawConv, width);

            // Modifier is almost entirely handled in getModifier
            String modifier = getModifier(m, rawConv, pos);

            char conv = applyConversion(rawConv, pos);

            // The following code sets too many local variables to be a method
            if (isIn(conv, INT_CONVERSIONS) && precision.length() != 0) {
                // In C, precision is the minimum number of digits to print
                // Java decided to leave out precision for integers (ouch!)
                int p = Integer.parseInt(precision.substring(1));
                int w = 0;
                if (width.length() > 0) {
                    w = Integer.parseInt(width);
                }

                // If these flags are here, they count against the width
                int signWidth = 0;
                if (isIn(' ', flags) || isIn('+', flags) && !isIn(conv, "uU")) {
                    signWidth = 1;
                }

                // For hexadecimal "#", the 0x counts in the precision
                int hexWidth = 0;
                if (isIn(conv, "xX") && isIn('#', flags)) {
                    hexWidth = 2;
                }

                // First try out some easy cases
                if (p - hexWidth == w - signWidth && isIn('0', flags)) {
                    // In this case the precision has no actual effect
                    precision = "";
                } else if (p - hexWidth > w - signWidth) {
                    // Make the width be the precision and force leading 0's
                    width = Integer.toString(p + signWidth + hexWidth);
                    precision = "";
                    if (!isIn('0', flags)) {
                        flags += "0";   // This will force leading zeros
                    }
                    // Left-justification has no meaning with leading zero
                    flags = flags.replaceAll("-", "");
                } else {
                    // This is harder -- p < w and there are no leading 0's
                    StringBuilder sb = new StringBuilder();
                    Formatter f = new Formatter(sb, formatter.locale());
                    // Print using precision as width, with leading zeros
                    StringBuilder tmpFmt = new StringBuilder("%" + flags);
                    remove(tmpFmt, "-");
                    if (!isIn('0', flags)) {
                        tmpFmt.append('0');
                    }
                    tmpFmt.append(p + signWidth + hexWidth).append(conv);
                    f.format(tmpFmt.toString(), args[pos]);
                    args[pos] = sb.toString();
                    // Now the printing will use the resulting string in the real width
                    conv = 's';
                    precision = "";
                    // The only flag that might have meaning now is "-"
                    if (isIn('-', flags)) {
                        flags = "-";
                    } else {
                        flags = "";
                    }
                }
            }

            // The parts of the spec before and after the precision
            String prePrecision = flags + altivecSpec + width;
            String postPrecision = Character.toString(conv);

            // The parts of the spec before and after the width
            String preWidth = flags + altivecSpec;
            String postWidth = precision + conv;

            // The expected format to use in Java's printf
            String javaFmt = prePrecision + precision + postPrecision;

            switch (conv) {
            case '%':
                // This is just all kinds of weirdness -- Java wants nothing
                // specified for '%', but C allows all string styles
                javaFmt = formatPercent(javaFmt).toString();
                // We have to return because the general case adds pos and "%"
                return Matcher.quoteReplacement(javaFmt);

            case 'n':
                // We handle this specially via a Formattable, and we need to
                // lose the width, etc., which are ignored
                javaFmt = "s";
                args[pos] = new PositionRecorder(args[pos], modifier);
                break;

            case 'g':
            case 'G':
            case 'e':
            case 'E':
            case 'f':
            case 'F':
            case 'a':
            case 'A':
                javaFmt = handleFloatingPoint(m, pos, width, precision,
                        modifier, rawConv, prePrecision, postPrecision,
                        preWidth, postWidth, javaFmt);
                break;
            }

            // Argument position numbers start at 1, not 0
            int argPos = pos + 1;

            // Quote it in case something we return looks special during replace
            return Matcher.quoteReplacement("%" + argPos + "$" + javaFmt);
        }

        private String handleFloatingPoint(Matcher m, int argPos, String width,
                String precision, String modifier, char rawConv,
                String prePrecision, String postPrecision, String preWidth,
                String postWidth, String javaFmt) {

            Class<? extends Number> target = isIn('L', modifier) ?
                    Double.class :
                    Float.class;
            Number val = toFloating(args[argPos], target);
            args[argPos] = val;
            double dval = val.doubleValue();
            CharSequence result;

            if (dval == Double.POSITIVE_INFINITY ||
                    dval == Double.NEGATIVE_INFINITY || Double.isNaN(dval)) {
                // C uses "inf"/"INF", Java uses Infinity
                // C uses "nan"/"NAN", Java uses NaN
                // Different widths make this a hassle
                // Java also ignores leading zeros for inf and nan
                StringBuilder sb = new StringBuilder();
                Formatter fmt = new Formatter(sb, formatter.locale());
                StringBuilder tmpFmt = new StringBuilder("%");
                tmpFmt.append(prePrecision).append(".0").append(postPrecision);

                tmpFmt.replace(tmpFmt.length() - 1, tmpFmt.length(), "f");

                remove(tmpFmt, "#");

                // don't let the sign of the placeholder show up for NaN
                int pos = tmpFmt.indexOf("+");
                if (pos >= 0 && Double.isNaN(dval)) {
                    tmpFmt.replace(pos, pos + 1, " ");
                }

                double placeHolder = 111.0;
                if (dval < 0) {
                    placeHolder = -placeHolder;
                }
                fmt.format(tmpFmt.toString(), placeHolder);
                pos = sb.indexOf("111");
                String str = Double.isNaN(dval) ? "nan" : "inf";
                sb.replace(pos, pos + 3, str);
                result = useCorrectCase(rawConv, sb);
            } else {
                // non-special floating point value
                switch (rawConv) {
                case 'g':
                case 'G':
                    result = handleG("%" + javaFmt, m.group(FLAGS), val,
                            "%" + prePrecision, precision, postPrecision,
                            preWidth, width, postWidth);
                    break;

                case 'a':
                case 'A':
                    if (precision.length() > 0 && Integer.parseInt(
                            precision.substring(1)) == 0) {
                        strictCheck(
                                "Zero precision not supported for %" + rawConv);
                    }
                    result = handleHexFloat("%" + javaFmt, val, preWidth, width,
                            postWidth);
                    break;

                case 'f':
                case 'e':
                case 'F':
                case 'E':
                    StringBuilder sb = new StringBuilder();
                    Formatter fmt = new Formatter(sb, formatter.locale());
                    fmt.format("%" + javaFmt, val);
                    result = sb;
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unexpected format %" + rawConv);
                }
            }
            args[argPos] = result;
            return "s";
        }

        private String getAltiVec(Matcher m) {
            // DOC: AltiVec not supported, just ignored
            if (getGroup(m, ALTIVEC_SEP).length() > 0) {
                strictCheck("AltiVec not supported");
            }
            return "";
        }

        private char getConversion(Matcher m) {
            return getGroup(m, CONVERSION).charAt(0);
        }

        private char applyConversion(char conv, int pos) {
            switch (conv) {
            case 'd':
            case 'o':
            case 'x':
            case 'X':
            case 'e':
            case 'E':
            case 'f':
            case 'g':
            case 'G':
            case 'a':
            case 'A':
            case 's':
            case '%':
            case 'n':
                break;

            case 'U':
            case 'u':
                conv = 'd';
                args[pos] = toUnsigned(args[pos]);
                break;

            case 'O':
                // In C these act identically
                // Java has o, not O,
                // No need for a 'strict' check since the behavior is the same
                conv = 'o';
                break;

            case 'C':
                strictCheck("%C (wchar_t) conversion not done in Java printf");
                conv = 'c';
                //noinspection fallthrough
            case 'c':
                args[pos] = toChar(args[pos]);
                break;

            case 'S':
                // In C, %S means a string of wchar_t (16-bit chars)
                // In Java, %S means output string in upper case.
                // The Java equivalent is just %s -- all Java strings are 16-bit
                strictCheck("%S (wchar_t*) conversion not done in Java printf");
                conv = 's';
                break;

            case 'D':
            case 'i':
                conv = 'd';
                break;

            case 'F':
                // In C these act identically
                // Java has f, not F,
                // No need for a 'strict' check since the behavior is the same
                conv = 'f';
                break;

            case 'p':
                strictCheck("True 'p' not available: No pointers in Java");
                // 'p' acts like %#x
                conv = 'x';
                args[pos] = getPointerValue(args[pos]);
                break;

            default:
                throw new IllegalArgumentException(
                        "'" + conv + "' is not supported");
            }
            return conv;
        }

        private String getWidth(Matcher m) {
            return getValue(m, WIDTH_SPEC, WIDTH_NUMBER, WIDTH_FROM_ARGS);
        }

        private String getPrecision(Matcher m) {
            return getValue(m, PRECISION_SPEC, PRECISION_NUMBER,
                    PRECISION_FROM_ARGS);
        }

        private String applyPrecision(String rawPrecision, char conv) {
            String precision = rawPrecision;
            switch (conv) {
            case 'g':
            case 'G':
                if (precision.length() == 0) {
                    precision = "6";
                } else if (Integer.parseInt(precision) == 0) {
                    precision = "1";
                }
                break;

            case 'c':
            case 'C':
                // precisions aren't allowed in Java, and are ignored in C
                precision = "";
                break;
            }

            // technically, the leading '.' is part of precision specification
            if (precision.length() > 0) {
                precision = "." + precision;
            }
            return precision;
        }

        private String getValue(Matcher m, int conv, int number, int fromArg) {
            String val = getGroup(m, number);
            String convStr = getGroup(m, conv);
            if (convStr.startsWith("*")) {
                int p = positionalArg(m, fromArg);
                Number width = toInteger(args[p], Integer.class);
                val = width.toString();
            }
            return val;
        }

        private String getFlags(Matcher m, char conv, CharSequence width) {
            String flags = getGroup(m, FLAGS);
            StringBuilder sb = new StringBuilder(flags.length());
            for (int i = 0; i < flags.length(); i++) {
                char c = flags.charAt(i);
                switch (c) {
                case '#':
                    // flag not supported for these formats
                    if (!isIn(conv, NO_ALT_CONVERSIONS)) {
                        // 'g' supports this specially, not this way
                        if (conv != 'g' && conv != 'G') {
                            sb.append(c);
                        }
                    }
                    break;

                case '+':
                case ' ':
                    // flags only supported for these formats
                    if (isIn(conv, SIGNED_CONVERSIONS)) {
                        sb.append(c);
                    }
                    break;

                case '-':
                    // '-' cannot be used without a specified width
                    if (width.length() > 0) {
                        sb.append(c);
                    }
                    break;

                case '0':
                    // '-' overrides '0'
                    // Can't have '0' without a width
                    if (isIn(conv, INT_CONVERSIONS + FLOAT_CONVERSIONS) &&
                            !isIn('-', flags) && width.length() > 0) {
                        sb.append(c);
                    } else {
                        strictCheck(
                                "Leading zeros not implemented for %" + conv);
                    }
                    break;

                case '\'':
                    sb.append(',');
                    break;

                default:
                    throw new IllegalArgumentException(
                            "The '" + c + "' flag is not supported");
                }
            }

            // 'p' acts like %#x
            if (conv == 'p' && sb.indexOf("#") < 0) {
                sb.append('#');
            }

            return sb.toString();
        }

        private int positionalArg(Matcher m, int i) {
            String posSpec = getGroup(m, i);

            // If there is no positional argument, use the next one
            if (posSpec.length() != 0) {
                // Get the position (strip off trailing '$')
                String noDollar = posSpec.substring(0, posSpec.length() - 1);
                int pos = Integer.parseInt(noDollar);
                // indexes fort the argument array start at 0, not 1
                nextArg = pos - 1;
            }
            return nextArg++;
        }

        private String getModifier(Matcher m, char conv, int pos) {
            String modifier = getGroup(m, MODIFIER);

            // Absent any other information, treat integer types as 4 bytes
            if (modifier.length() == 0) {
                if (isIn(conv, INT_CONVERSIONS)) {
                    args[pos] = toInteger(args[pos], Integer.class).intValue();
                }
                return "";
            }

            String validConversions = MODIFIER_MAP.get(modifier);
            if (validConversions == null) {
                throw new IllegalArgumentException(
                        "Modifier \"" + modifier + "\" unsupported");
            }
            if (validConversions.indexOf(conv) < 0) {
                return "";
            }

            if (modifier.length() == 1) {
                switch (modifier.charAt(0)) {
                case 'h':
                    if (isIn(conv, INT_CONVERSIONS)) {
                        args[pos] = toInteger(args[pos], Integer.class)
                                .shortValue();
                    }
                    break;

                case 'l':
                    if (isIn(conv, FLOAT_CONVERSIONS) || conv == 'c' ||
                            conv == 's') {
                        // 'l' for floats is ignored.
                        // 'l' for chars is meaningless: chars are long in Java
                        // So we leave it "not known" so the modifier is removed
                        break;
                    }
                    // In our C machine model, an int is 32 bites, hence %ld is
                    // the same as %d -- an int (and also for %x, %o, ...)
                    //noinspection fallthrough
                case 'z':   // ptrdiff_t's best equiv -- the array index type
                case 't':   // size_t's best equiv -- the array index type
                    if (isIn(conv, INT_CONVERSIONS)) {
                        args[pos] = toInteger(args[pos], Integer.class)
                                .intValue();
                    }
                    break;

                case 'j':
                case 'q':
                    if (isIn(conv, INT_CONVERSIONS)) {
                        args[pos] = toInteger(args[pos], Long.class)
                                .longValue();
                    }
                    break;

                case 'L':
                    if (isIn(conv, FLOAT_CONVERSIONS)) {
                        Number num = toFloating(args[pos], Double.class);
                        args[pos] = num.doubleValue();
                    }
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unexpected length modifier: '" + modifier + "'");
                }
            } else if (modifier.equals("hh")) {
                if (conv != 'n') {
                    args[pos] = toInteger(args[pos], Integer.class).byteValue();
                }
            } else if (modifier.equals("ll")) {
                if (conv != 'n') {
                    args[pos] = toInteger(args[pos], Long.class).longValue();
                }
            } else {
                throw new IllegalArgumentException(
                        "Length modifier not supported: \"" + modifier + "\"");
            }
            return modifier;
        }
    }

    /**
     * A {@code Stdio} object tied to {@link System#out}, using the default
     * character set and locale.  Auto-flush is on for this object and cannot be
     * disabled.
     * <p/>
     * <b>Note</b> This field cannot be changed, even if you change {@code
     * System.out} using {@link System#setOut(PrintStream)}.  To allow this
     * would require one of the following changes: <ul> <li>Make this field not
     * {@code final}.  This would allow one section of the code to change it and
     * thereby break another section. <li>Make this into a function.  This would
     * make each use of it more awkward and expensive, and it is used a lot.
     * <li>Write native code to actually modify a {@code final} field (Which is
     * is how {@link System#setOut(PrintStream)} works). </ul> None of these
     * seem reasonable considering how rare it is that one needs to change it.
     * And even then, it would still be possible for someone to use {@link
     * System#setOut(PrintStream)} without changing this field.
     */
    public static final Stdio stdout = new Stdio(new Formatter(System.out),
            true);

    /**
     * A {@code Stdio} object tied to {@link System#err}, using the default
     * character set and locale.  Auto-flush is on for this object and cannot be
     * disabled.
     * <p/>
     * <b>Note:</b> See note for {@link #stdout}.
     *
     * @see #stdout
     */
    public static final Stdio stderr = new Stdio(new Formatter(System.err),
            true);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // make sure both are called, even if one fails
                try {
                    stdout.close();
                } finally {
                    stderr.close();
                }
            }
        });
    }

    /**
     * Creates a new Stdio object that writes to the given formatter.  The
     * formatter defines the locale that will be used.  Auto-flushing will be
     * off.
     *
     * @param formatter The formatter for the output.
     */
    public Stdio(Formatter formatter) {
        this(formatter, false);
    }

    /**
     * Creates a new Stdio object that sends to the given formatter.  The
     * formatter defines the locale that will be used.  Auto-flushing will be
     * set to the provided value.
     *
     * @param formatter The formatter for the output.
     * @param autoFlush <tt>true</tt> if every end-of-line output should flush
     *                  the stream.
     */
    public Stdio(Formatter formatter, boolean autoFlush) {
        if (formatter == null) {
            throw new NullPointerException("formatterer");
        }
        this.formatter = formatter;
        this.autoFlush = autoFlush;
    }

    private static String getGroup(MatchResult m, int i) {
        String grp = m.group(i);
        return grp == null ? "" : grp;
    }

    /**
     * Convert the given argument to a floating-point type, if necessary and
     * possible.  This method exists to handle char and integer-to-floating
     * conversions the way C does.  If the argument is a char, convert it to an
     * int. If it is an integer type (possibly after the conversion from a
     * char), convert it to the type specified by {@code floatClass} using a
     * bitwise conversion.
     * <p/>
     * If this is already a floating-point class, the original object is
     * returned. Note that this means that the return object might not be of the
     * type specified in {@code floatClass}.
     *
     * @param arg        The argument.
     * @param floatClass The floating-point class to use for an integer value.
     *
     * @return The object the argument was modified to be.
     */
    private Number toFloating(Object arg, Class<? extends Number> floatClass) {

        // This is how C would handle an integer in a floating place
        // (Not worrying about how (say) %f would handle a 64-bit value,
        // etc., because that's undefined.)
        Number num = toNumber(arg);
        if (num instanceof Float || num instanceof Double) {
            return num;
        } else if (floatClass == Double.class) {
            num = Double.longBitsToDouble(num.longValue());
        } else {
            num = Float.intBitsToFloat(num.intValue());
        }
        return num;
    }

    /**
     * Convert the given value to an integer type, if necessary and possible.
     * This method exists to handle char and floating-to-integer conversions the
     * way C does.  If the argument is a char, convert it to an int. If it is
     * floating point, convert it to the type specified by {@code intClass}
     * using a bitwise conversion.
     * <p/>
     * If this is already an integer class, the original object is returned.
     * Note that this means that the return object might not be of the type
     * specified in {@code intClass}.
     *
     * @param val      The value to (possibly) convert.
     * @param intClass The integer class to use for a floating-point value.
     *
     * @return The object the argument was modified to be.
     */
    private Number toInteger(Object val, Class<? extends Number> intClass) {
        Number num = toNumber(val);
        if (num instanceof Float || num instanceof Double) {
            if (intClass == Long.class) {
                return Double.doubleToLongBits(num.doubleValue());
            } else {
                return Float.floatToIntBits(num.floatValue());
            }
        }
        return num;
    }

    /**
     * Convert the object to a number if it isn't one already.  This handles
     * converting a Character object to an Integer object the way C converts a
     * char to an int. If the input is not a char, it is cast to Number,
     * ensuring that either a Number object is returned, or that a
     * ClassCastException is thrown because (for example) they were trying to
     * print a String as a number.
     *
     * @param val An object to convert.
     *
     * @return The original object or the equivalent Integer object if the
     *         original was a Character.
     */
    private Number toNumber(Object val) {
        Number num = null;
        if (val instanceof Character) {
            num = (int) ((Character) val).charValue();
        } else {
            num = (Number) val;
        }
        return num;
    }

    private CharSequence useCorrectCase(char conv, CharSequence result) {
        if (Character.isLowerCase(conv)) {
            return result.toString().toLowerCase();
        } else {
            return result.toString().toUpperCase();
        }
    }

    // Remove a string from a string buffer if present

    private static void remove(StringBuilder sb, String toRemove) {
        int pos = sb.indexOf(toRemove);
        if (pos >= 0) {
            sb.delete(pos, pos + toRemove.length());
        }
    }

    private Number toUnsigned(Object arg) {
        Number n = (Number) arg;
        long val = n.longValue();
        if (val >= 0) {
            return n;
        }

        // If possible, promote to next larger size without sign extension
        if (n instanceof Byte) {
            return val & 0xffL;
        } else if (n instanceof Short) {
            return val & 0xffffL;
        } else if (n instanceof Integer) {
            return val & 0xffffffffL;
        }
        // The hard case is a negative value outside the int range
        return new BigInteger(Long.toHexString(val), 16);
    }

    private Object toChar(Object arg) {
        // in C, an int here means the lowest byte
        // in Java it means a code point, and it must be valid
        if (arg instanceof Integer) {
            return ((Integer) arg).shortValue();
        }
        return arg;
    }

    private Integer getPointerValue(Object arg) {
        if (arg == null) {
            return 0;
        }

        synchronized (pointerVals) {
            Integer val = pointerVals.get(arg);
            if (val == null) {
                val = pointerVals.size() + 0x1000;
                pointerVals.put(arg, val);
            }
            return val;
        }
    }

    private CharSequence handleHexFloat(String format, Number val,
            String preWidth, String width, String postWidth) {

        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb, formatter.locale());
        f.format(format, val);

        int pos = findP(sb);

        if (pos > 0) {
            if (width.length() == 0 || Integer.parseInt(width) == 0) {
                sb.insert(pos, '+');
            } else {
                // Reduce width by 1, then we'll insert the added '+'
                sb.delete(0, sb.length());
                String newFmt = "%" + preWidth + (Integer.parseInt(width) - 1) +
                        postWidth;
                f.format(newFmt, val);
                pos = findP(sb);
                sb.insert(pos, '+');
            }
        }
        return sb;
    }

    CharSequence formatPercent(String javaFmt) {
        // C allows formatting specification for "%%", Java doesn't.
        // C treats this, effectively, as a %s applied to a %".
        // So we do that, but remember to come back and double the "%"
        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb, formatter.locale());
        String fmt = '%' + javaFmt.substring(0, javaFmt.length() - 1) + 's';
        f.format(fmt, "%");
        int pos = sb.indexOf("%");
        sb.insert(pos, '%');
        return sb;
    }

    CharSequence handleG(String format, String rawFlags, Object val,
            String prePrecision, String precision, String postPrecision,
            String preWidth, String width, String postWidth) {

        // Figure out how many zeroes need removing
        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb, formatter.locale());
        String overriddenPrecision = null;

        // Work around a bug in Java -- it fails on these precisions for values that aren't presented in 'e'
        if (precision.equals(".0") || precision.equals(".1")) {
            String nf = prePrecision + ".2" + postPrecision;
            f.format(nf, val);
            Matcher m = ZERO_TRAILING_PATTERN.matcher(sb);
            assert m.find();

            // This has no exponent, so it's the kind that trips the bug
            if (getGroup(m, EXPONENT).length() == 0) {
                if (val instanceof Double) {
                    val = (double) Math.round((Double) val);
                } else {
                    val = (float) Math.round(((Float) val).floatValue());
                }
                overriddenPrecision = precision;
                precision = ".2";
                format = prePrecision + precision + postPrecision;
            }
        }

        sb.delete(0, sb.length());
        f.format(format, val);

        // Java's '#' format doesn't remove trailing zeros, but C's does
        if (!isIn('#', rawFlags) || overriddenPrecision != null) {
//            new RegexDebug(ZERO_TRAILING_PATTERN).displayMatchDetails(sb,
//                    System.out);
            Matcher m = ZERO_TRAILING_PATTERN.matcher(sb);
            int toRemove;
            if (m.find() && (toRemove = m.group(TRAILING_ZEROS).length()) > 0) {
                // Remove decimal point if we remove the last digit before it
                int first = m.start(TRAILING_ZEROS);
                boolean removeDecimal = sb.charAt(first - 1) == '.';
                if (removeDecimal && !isIn('#', rawFlags)) {
                    toRemove++;
                    first--;
                }
                int w = width.length() == 0 ? 0 : Integer.parseInt(width);
                if (isIn('-', rawFlags)) { // left justified
                    for (int i = toRemove - 1; i >= 0; i--) {
                        boolean atWidth = sb.length() == w;
                        int pos = first + i;
                        sb.delete(pos, pos + 1);
                        if (atWidth) {
                            sb.append(' ');
                        }
                    }
                } else {
                    char ch = (isIn('0', rawFlags) ? '0' : ' ');
                    int pos = first + toRemove - 1;
                    int begin = m.start(0);
                    while (begin > 0 && sb.charAt(begin) != ch) {
                        begin--;
                    }
                    for (int i = toRemove; i > 0; i--) {
                        boolean atWdith = sb.length() == w;
                        sb.delete(pos, pos + 1);
                        if (atWdith) {
                            sb.insert(begin, ch);
                        } else {
                            pos--;
                        }
                    }
                }
            }
        } else {
            if (!isIn('.', sb.toString())) {
                int origWidth;
                if (width.length() == 0 || (origWidth = Integer.parseInt(
                        width)) == 0) {
                    sb.append('.');
                } else {
                    sb.delete(0, sb.length());
                    String widthSpec = "." + (origWidth - 1);
                    f.format(preWidth + widthSpec + postWidth + ".", val);
                }
            }
        }
        return sb;
    }

    private static int findP(StringBuilder sb) {
        int pos = sb.indexOf("p");
        if (pos < 0) {
            pos = sb.indexOf("P");
        }
        if (pos > 0 && sb.charAt(pos + 1) != '-') {
            return pos + 1;
        } else {
            return -1;
        }
    }

    /**
     * Returns {@code true} if this object is in auto-flush mode.  Auto-flush
     * mode, automatically flushes the output after each {@link
     * #printf(CharSequence,Object...) printf}.
     *
     * @return {@code true} if this object is in auto-flush mode.
     */
    public boolean isAutoFlush() {
        return autoFlush;
    }

    /**
     * Sets whether this object is in auto-flush mode.
     *
     * @param autoFlush {@code true} if this object should be in auto-flush
     *                  mode.
     */
    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = autoFlush;
    }

    /**
     * Returns {@code true} if this object is in strict mode.
     *
     * @return {@code true} if this object is in strict mode.
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * Sets whether this object is in strict mode.
     *
     * @param strict {@code true} if this object should be in strict mode.
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    private void strictCheck(String errmsg) {
        if (strict) {
            throw new IllegalArgumentException(errmsg + " (strict)");
        }
    }

    /**
     * Flushes the stream and checks to see if an error has occurred.  This is
     * analogous to {@link PrintStream#checkError()}.
     *
     * @return {@code true} if an error has occurred.
     *
     * @see PrintStream#checkError()
     */
    public boolean checkError() {
        flush();
        return hasAnError();
    }

    private boolean hasAnError() {
        //noinspection ThrowableResultOfMethodCallIgnored
        if (!hasError && formatter.ioException() != lastSeenException) {
            rememberFormatterException();
            hasError = true;
        }
        return hasError;
    }

    private void rememberFormatterException() {
        lastSeenException = formatter.ioException();
    }

    /**
     * Marks that an error has occurred.  Future calls to {@link #checkError()}
     * will return {@code true}.  This is analogous to {@link
     * PrintStream#setError()}.
     *
     * @see PrintStream#setError()
     */
    public void setError() {
        hasError = true;
    }

    /**
     * Clears any error condition.  Future calls to {@link #checkError()} will
     * return {@code false}.  This has no analog in {@link PrintStream}; it is
     * analogous to stdio's {@code clearerr()}.
     */
    public void clearError() {
        hasError = false;

        // prevents the existing exception from setting hasError again
        rememberFormatterException();
    }

    /**
     * Performs a C-style printf.  The output will be sent to the formatter
     * specified when this object was created, using that formatter's locale
     * where relevant.
     *
     * @param cFmt The C-style format string.
     * @param args The arguments to print.
     *
     * @return The character written, or {@link #EOF} if there is an error.
     */
    public int printf(CharSequence cFmt, Object... args) {
//        new RegexDebug(C_FORMAT_PATTERN).displayMatchDetails(cFmt, System.out);
        String fmt = RegexUtils.replaceAll(C_FORMAT_PATTERN, cFmt, new ToJava(
                args));

        // newlines should be %n
        fmt = NEWLINE_PATTERN.matcher(fmt).replaceAll("%n");

//        System.out.println("to: " + showJavaPrintf(fmt, args));
        int before = formatterPos();
        formatter.format(fmt, args);
        int after = formatterPos();
        if (autoFlush) {
            flush();
        }
        return (hasAnError() ? EOF : after - before);
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...)} printf}.  This is a
     * stdio analog, equivalent to {@link #printf(CharSequence,Object...)}.
     *
     * @param cFmt The C format string.
     * @param args The arguments for the print.
     *
     * @return The character written, or {@link #EOF} if there is an error.
     */
    public int fprintf(CharSequence cFmt, Object... args) {
        return printf(cFmt, args);
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf} into a new
     * {@link StringBuilder} buffer.  This is a simpler-to-use variant of {@link
     * #asprintf(StringBuilder[],CharSequence,Object...)} in the Java context.
     *
     * @param cFmt The C format string.
     * @param args The arguments for the print.
     *
     * @return The allocated buffer, or {@code null} if there is an error.
     */
    public StringBuilder asprintf(CharSequence cFmt, Object... args) {
        StringBuilder sb = new StringBuilder();
        if (sprintf(sb, cFmt, args) >= 0) {
            return sb;
        } else {
            return null;
        }
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf} into a string
     * buffer that is any {@link Appendable}, such as {@link StringBuilder},
     * {@link StringBuffer}, {@link PrintStream}, or {@link PrintWriter}.  This
     * is a stdio analog.
     *
     * @param sb   The buffer to print into.
     * @param cFmt The C format string.
     * @param args The arguments for the print
     *
     * @return The number of bytes printed, or {@link #EOF} if there is an
     *         error.
     */
    public int sprintf(Appendable sb, CharSequence cFmt, Object... args) {
        Formatter formatter = new Formatter(sb);
        Stdio stdio = new Stdio(formatter, true);
        return stdio.printf(cFmt, args);
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf} into a new
     * {@link StringBuilder} buffer.  That buffer is stored in {@code ret[0]}.
     * This is a stdio analog.
     *
     * @param ret  The array that will hold the buffer to print into.
     * @param cFmt The C format string.
     * @param args The arguments for the print
     *
     * @return The number of bytes printed, or {@link #EOF} if there is an
     *         error.
     */
    public int asprintf(StringBuilder[] ret, CharSequence cFmt,
            Object... args) {
        ret[0] = new StringBuilder();
        return sprintf(ret[0], cFmt, args);
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf} of up to {@code
     * max} characters into a string buffer that is any {@link Appendable}, such
     * as {@link StringBuilder}, {@link StringBuffer}, {@link PrintStream}, or
     * {@link PrintWriter}.  This is a stdio analog equivalent to {@link
     * #sprintf(Appendable,CharSequence,Object...) sprintf}.
     *
     * @param sb   The buffer to print into.
     * @param max  Ignored.
     * @param cFmt The C format string.
     * @param args The arguments for the print
     *
     * @return The number of bytes that could not be printed, or {@link #EOF} if
     *         there is an error.
     */
    public int snprintf(Appendable sb, int max, CharSequence cFmt,
            Object... args) {

        LimitedBuffer lb = new LimitedBuffer(sb, max);
        sprintf(lb, cFmt, args);
        return lb.ignored();
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf}.  This is a
     * stdio analog to {@link #printf(CharSequence,Object...) printf} with the
     * same syntax because in Java variable-argument methods take arrays of
     * parameters, just like C's {@code va_list}.  In other words, to pass a
     * variable number of arguments, or to pass on the list of arguments from
     * another variable-number-of-arguments method are equivalent.
     *
     * @param cFmt The C format string.
     * @param args The arguments for the print
     *
     * @return The number of bytes printed, or {@link #EOF} if there is an
     *         error.
     */
    public int vprintf(CharSequence cFmt, Object... args) {
        return fprintf(cFmt, args);
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf}.  This is a
     * stdio analog to {@link #fprintf(CharSequence,Object...) fprintf} with the
     * same syntax; see the documentation for {@link #vprintf(CharSequence,Object...)}
     * for an explanation.
     *
     * @param cFmt The C format string.
     * @param args The arguments for the print
     *
     * @return The number of bytes printed, or {@link #EOF} if there is an
     *         error.
     */
    public int vfprintf(String cFmt, Object... args) {
        return fprintf(cFmt, args);
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf} into a string
     * buffer that is any {@link Appendable}, such as {@link StringBuilder},
     * {@link StringBuffer}, {@link PrintStream}, or {@link PrintWriter}.  This
     * is a stdio analog to {@link #asprintf(StringBuilder[],CharSequence,Object...)
     * asprintf} with the same syntax; see the documentation for {@link
     * #vprintf(CharSequence,Object...)} for an explanation.
     *
     * @param sb   The buffer to print into.
     * @param cFmt The C format string.
     * @param args The arguments for the print
     *
     * @return The number of bytes printed, or {@link #EOF} if there is an
     *         error.
     */
    public int vsprintf(Appendable sb, CharSequence cFmt, Object... args) {
        return sprintf(sb, cFmt, args);
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf} into a new
     * {@link StringBuilder} buffer.  This is a stdio analog to {@link
     * #asprintf(StringBuilder[],CharSequence,Object...) asprintf} with the same
     * syntax; see the documentation for {@link #vprintf(CharSequence,Object...)}
     * for an explanation.
     *
     * @param ret  The array that will hold the buffer to print into.
     * @param cFmt The C format string.
     * @param args The arguments for the print
     *
     * @return The number of bytes printed, or {@link #EOF} if there is an
     *         error.
     */
    public int vasprintf(StringBuilder[] ret, CharSequence cFmt,
            Object... args) {
        return asprintf(ret, cFmt, args);
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf} into a new
     * {@link StringBuilder} buffer.  This is a stdio analog to {@link
     * #asprintf(CharSequence,Object...) asprintf} with the same syntax; see the
     * documentation for {@link #vprintf(CharSequence,Object...)} for an
     * explanation.
     *
     * @param cFmt The C format string.
     * @param args The arguments for the print
     *
     * @return The allocated buffer, or {@code null} if there is an error.
     */
    public Appendable vasprintf(CharSequence cFmt, Object... args) {
        return asprintf(cFmt, args);
    }

    /**
     * Perform a {@link #printf(CharSequence,Object...) printf} of up to {@code
     * max} characters into a string buffer that is any {@link Appendable}, such
     * as {@link StringBuilder}, {@link StringBuffer}, {@link PrintStream}, or
     * {@link PrintWriter}.  This is a stdio analog to {@link
     * #snprintf(Appendable,int,CharSequence,Object...) asprintf} with the same
     * syntax; see the documentation for {@link #vprintf(CharSequence,Object...)}
     * for an explanation.
     *
     * @param sb   The buffer to print into.
     * @param max  Ignored.
     * @param cFmt The C format string.
     * @param args The arguments for the print
     *
     * @return The number of bytes that could not be printed, or {@link #EOF} if
     *         there is an error.
     */
    public int vsnprintf(Appendable sb, int max, CharSequence cFmt,
            Object... args) {
        return snprintf(sb, max, cFmt, args);
    }

    private int formatterPos() {
        return formatter.out().toString().length();
    }

    /** Closes this object.  Also closes the formatter this object uses. */
    public void close() {
        formatter.close();
    }

    /** Flushes this object.  Also flushes the formatter this object uses. */
    public void flush() {
        formatter.flush();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Any {@link IOException} will be handled the same way that it would be in
     * {@link PrintStream}.
     */
    public Appendable append(char c) {
        try {
            formatter.out().append(c);
        } catch (InterruptedIOException ignored) {
            // This does not set the error state
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            setError();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Any {@link IOException} will be handled the same way that it would be in
     * {@link PrintStream}.
     */
    public Appendable append(CharSequence csq) {
        try {
            formatter.out().append(csq);
        } catch (InterruptedIOException ignored) {
            // This does not set the error state
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            setError();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Any {@link IOException} will be handled the same way that it would be in
     * {@link PrintStream}.
     */
    public Appendable append(CharSequence csq, int start, int end) {
        try {
            formatter.out().append(csq, start, end);
        } catch (InterruptedIOException ignored) {
            // This does not set the error state
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            setError();
        }
        return this;
    }

    static String showJavaPrintf(String fmt, Object... args) {
        StringBuilder sb = new StringBuilder();
        sb.append("printf(").append('"').append(fmt).append('"');
        for (Object arg : args) {
            sb.append(", ");
            if (arg instanceof String) {
                sb.append('"').append(escapeString(arg)).append('"');
            } else if (arg instanceof Character) {
                sb.append("'").append(escapeString(arg)).append("'");
            } else {
                sb.append(arg);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    // We will just assume that the tests use only chars common to C and Java

    static String escapeString(Object arg) {
        StringBuilder sb = new StringBuilder();
        String str = arg.toString();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
            case '\t':
                sb.append("\\t");
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\\':
            case '"':
                sb.append("\\");
                //noinspection fallthrough
            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Returns {@code true} if the given character appears in the string.
     *
     * @param ch  The character.
     * @param str The string to look in.
     *
     * @return {@code true} if the given character appears in the string.
     */
    private static boolean isIn(char ch, String str) {
        return str.indexOf(ch) >= 0;
    }

    /**
     * Clears any error condition remembered for this stream.  This is a stdio
     * analog, equivalent to {@link #clearError()}.
     */
    public void clearerr() {
        clearError();
    }

    /**
     * Closes this stream, if it hasn't yet been closed.  This is a stdio
     * analog, except that you can close the same file more than once with no
     * error.
     *
     * @return {@link #EOF} if there has been an error on this stream, otherwise
     *         0.
     */
    public int fclose() {
        close();
        return ferror();
    }

    /**
     * Returns {@link #EOF} if an error has occurred. This is a stdio analog.
     *
     * @return {@link #EOF} if there has been an error on this stream, otherwise
     *         0.
     */
    public int ferror() {
        return (hasAnError() ? EOF : 0);
    }

    /**
     * Flushes any pending output.  This is a stdio analog.
     *
     * @return {@link #EOF} if there has been an error on this stream, otherwise
     *         0.
     */
    public int fflush() {
        flush();
        return ferror();
    }

    /**
     * Print a single character.  This is a stdio analog.
     *
     * @param c The character to print.
     *
     * @return The character written, or {@link #EOF} if there is an error.
     */
    public int fputc(char c) {
        append(c);
        return (hasAnError() ? EOF : c);
    }

    /**
     * Puts out a string.  This is a stdio analog.
     *
     * @param str The string to put.
     *
     * @return {@link #EOF} if there has been an error on this stream, otherwise
     *         0.
     */
    public int fputs(CharSequence str) {
        append(str);
        return (hasAnError() ? EOF : 0);
    }

    /**
     * Writes out bytes (as characters).  This is a stdio analog.  The bytes are
     * first converted to a string using default {@link Charset}.  If you need
     * more control over which character set is being used, you should create
     * the string using your preferred {@link Charset} and use {@link
     * #fwrite(CharSequence,int)}.
     *
     * @param data   The source for data to write.
     * @param length The number of characters to write.
     *
     * @return The number written, which is only less than the number requested
     *         if there is an error.
     */
    public int fwrite(byte[] data, int length) {
        return fwrite(data, 0, length);
    }

    /**
     * Similar to {@link #fwrite(byte[],int)}, but allowing you to specify the
     * starting point in the array.
     *
     * @param data  The source for data to write.
     * @param start The index of the first character to write.
     * @param end   One past the index of the last character to write.
     *
     * @return The number written, which is only less than the number requested
     *         if there is an error.
     */
    public int fwrite(byte[] data, int start, int end) {
        CharSequence str = new String(data, start, end);
        return fwrite(str, start, end);
    }

    /**
     * Similar to {@link #fwrite(byte[],int)}, but using the entire array.
     *
     * @param data The source for data to write.
     *
     * @return The number written, which is only less than the number requested
     *         if there is an error.
     */
    public int fwrite(byte[] data) {
        return fwrite(data, 0, data.length);
    }

    /**
     * Writes out the string.
     *
     * @param str The string to write.
     *
     * @return The number written, which is only less than the number requested
     *         if there is an error.
     */
    public int fwrite(CharSequence str) {
        return fwrite(str, 0, str.length());
    }

    /**
     * Writes out up to {@code length} characters of the string.
     *
     * @param str    The string to write.
     * @param length The number of characters to write.
     *
     * @return The number written, which is only less than the number requested
     *         if there is an error.
     */
    public int fwrite(CharSequence str, int length) {
        return fwrite(str, 0, length);
    }

    /**
     * Writes out up to {@code length} characters of the string.
     *
     * @param str   The string to write.
     * @param start The index of the first character to write.
     * @param end   One past the index of the last character to write.
     *
     * @return The number written, which is only less than the number requested
     *         if there is an error.
     */
    public int fwrite(CharSequence str, int start, int end) {
        int begin = formatterPos();
        append(str, start, end);
        if (!hasAnError()) {
            return end - start;
        } else {
            return formatterPos() - begin;
        }
    }

    /**
     * Puts a single character.  This is a stdio analog.
     *
     * @param c The character to put.
     *
     * @return The character written, or {@link #EOF} if there is an error.
     */
    public int putc(char c) {
        return fputc(c);
    }

    /**
     * Puts a single character.  This is a stdio analog.
     *
     * @param c The character to put.
     *
     * @return The character written, or {@link #EOF} if there is an error.
     */
    public int putchar(char c) {
        return fputc(c);
    }

    /**
     * Puts out a string.  This is a stdio analog, equivalent to {@link
     * #fputs(CharSequence)}.
     *
     * @param str The string to put.
     *
     * @return {@link #EOF} if there has been an error on this stream, otherwise
     *         0.
     */
    public int puts(CharSequence str) {
        return fputs(str);
    }

    /**
     * Puts out a single 16-bit word (the lower 16 bits).  This is a stdio
     * analog.
     *
     * @param word The word to put.
     *
     * @return {@link #EOF} if there has been an error on this stream, otherwise
     *         0.
     */
    public int putw(int word) {
        append((char) ((word >>> 8) & 0xff));
        append((char) (word & 0xff));
        return ferror();
    }

    public static void resetPercentP() {
        pointerVals.clear();
    }
}