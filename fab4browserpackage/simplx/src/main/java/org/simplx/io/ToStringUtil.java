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

package org.simplx.io;

import org.simplx.logging.SimplxLogging;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * String utilities that help with string-ifying objects.
 *
 * @author Ken Arnold
 */
public class ToStringUtil {

    private static final String[] JAVA_CHARS;
    private static final String[] JAVA_ESCAPED_CHARS;
    private static final char[] TO_CHAR;

    private static long earliestTime;
    private static long latestTime;
    private static long timeBoundsSet;

    private static DateFormat dateFormat;

    // caches of toString() information.
    private static final Map<Class, ToStringInfo[]> staticStringInfo =
            new WeakHashMap<Class, ToStringInfo[]>();
    private static final Map<Class, ToStringInfo[]> objStringInfo =
            new WeakHashMap<Class, ToStringInfo[]>();

    // says "just use toString" in our toString(Object) method
    private static final ToStringInfo[] USE_TOSTRING = new ToStringInfo[0];

    private static final Class[] NO_ARGS = new Class[0];

    /** Info for how to print out an object's properties. */
    private static class ToStringInfo implements Comparable<ToStringInfo> {
        private final String name;
        private final Member member;

        ToStringInfo(String name, Member member) {
            this.member = member;
            this.name = name + "=";
        }

        void append(StringBuilder buf, Object obj)
                throws IllegalAccessException, InvocationTargetException {

            buf.append(name);
            Object val;
            if (member instanceof Field)
                val = ((Field) member).get(obj);
            else
                val = ((Method) member).invoke(obj);
            buf.append(displayString(val, true));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ToStringInfo) {
                ToStringInfo that = (ToStringInfo) obj;
                return compareTo(that) == 0;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public int compareTo(ToStringInfo that) {
            return name.compareTo(that.name);
        }
    }

    static {

        JAVA_CHARS = new String[256];
        JAVA_ESCAPED_CHARS = new String[256];

        for (char c = 0; c < JAVA_CHARS.length; c++) {
            JAVA_ESCAPED_CHARS[c] = javaCharString(c);
            if (Character.isWhitespace(c))
                JAVA_CHARS[c] = Character.toString(c);
            else
                JAVA_CHARS[c] = JAVA_ESCAPED_CHARS[c];
        }

        TO_CHAR = new char[128];
        TO_CHAR['r'] = '\r';
        TO_CHAR['n'] = '\n';
        TO_CHAR['f'] = '\f';
        TO_CHAR['t'] = '\t';
        TO_CHAR['b'] = '\b';
        TO_CHAR['"'] = '\"';
        TO_CHAR['\''] = '\'';
        TO_CHAR['\\'] = '\\';

        setDateFormat(DateFormat.getDateTimeInstance());
    }

    /**
     * Sets the date format to use in generating strings.  By default, this is
     * the default locale's date format.
     *
     * @param format The date format to use in generating strings.
     *
     * @throws NullPointerException The format cannot be {@code null}.
     * @see DateFormat#getDateTimeInstance()
     */
    public static void setDateFormat(DateFormat format) {
        if (format == null)
            throw new NullPointerException("format");
        dateFormat = format;
    }

    /**
     * This public constructor is provided so that this utility class can be
     * extended.
     */
    public ToStringUtil() {
    }

    private static String javaCharString(char c) {
        switch (c) {
        case '\r':
            return "\\r";
        case '\n':
            return "\\n";
        case '\b':
            return "\\b";
        case '\f':
            return "\\f";
        case '\t':
            return "\\t";
        case '\"':
            return "\\\"";
        case '\'':
            return "\\\'";
        case '\\':
            return "\\\\";
        default:
            if (Character.isISOControl(c) || !Character.isDefined(c))
                return unicodeEscape(c);
            else
                return Character.toString(c);
        }
    }

    private static String unicodeEscape(char c) {
        if (c <= 0xf)
            return "\\u000" + Integer.toHexString(c);
        else if (c <= 0xff)
            return "\\u00" + Integer.toHexString(c);
        else if (c <= 0xfff)
            return "\\u0" + Integer.toHexString(c);
        else
            return "\\u" + Integer.toHexString(c);
    }

    /**
     * Equivalent to {@code toJavaString(str,true)}.
     *
     * @param str The string.
     *
     * @return A string that is a printable version of the input string.
     */
    public static String toJavaString(CharSequence str) {
        return toJavaString(str, true);
    }

    /**
     * Returns a string that is a printable version of the input string. This is
     * layered on {@link #toJavaChar(char,boolean)}.
     *
     * @param str              The string.
     * @param escapeWhitespace Whether whitespace should be put in to the string
     *                         as is.
     *
     * @see #toJavaChar(char, boolean)
     */
    public static String toJavaString(CharSequence str,
            boolean escapeWhitespace) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++)
            sb.append(toJavaChar(str.charAt(i), escapeWhitespace));
        return sb.toString();
    }

    /**
     * Equivalent to {@code toJavaChar(c,true)}.
     *
     * @param c The character.
     *
     * @return A string that is a printable version of the input string.
     */
    public static String toJavaChar(char c) {
        return toJavaChar(c, true);
    }

    /**
     * Returns a string that is a printable version of the input character. Most
     * characters will produce a single-character string. Characters that have
     * 'escaped' representations (such as {@code \t}) will produce strings with
     * that representation. Characters that are ISO control characters or that
     * are not defined as a unicode character will produce the
     * <tt>\\u<i>xxxx</i></tt> form.
     * <p/>
     * If {@code escapeWhitespace} is {@code false}, whitespace characters will
     * be returned as a single-character string. This is useful for keeping the
     * formatting in place.
     *
     * @param c                The character.
     * @param escapeWhitespace Whether whitespace should be put in to the string
     *                         as is.
     *
     * @return A string that is a printable version of the input character.
     */
    public static String toJavaChar(char c, boolean escapeWhitespace) {
        if (c >= JAVA_CHARS.length) {
            return javaCharString(c);
        } else {
            if (!escapeWhitespace && Character.isWhitespace(c))
                return JAVA_CHARS[c];
            else
                return JAVA_ESCAPED_CHARS[c];
        }
    }

    /**
     * Returns a display-oriented version of the object. <ul> <li>If it is
     * {@code null}, {@code "null"} <li>If it has a {@link Object#toString
     * toString} method, the result of that method <li> if it is a {@link
     * Character}, the result of {@link #toJavaChar}<li>if it is a {@link
     * String}, the result of {@link #toJavaString}<li>if it is a {@link Long},
     * the number will end in {@code L}<li>if it is a {@link Float}, the number
     * will end in {@code f}.</ul>
     * <p/>
     * If {@code guessTime} is {@code true} any long within ten years of roughly
     * "now" will be treated as a time and formatted accordingly. (The notion of
     * "now" is occasionally reset so this will work on long-running code.)
     *
     * @param obj       The object to display.
     * @param guessTime If {@code true}, guess if any {@link Long} represents a
     *                  time.
     *
     * @return A display-friendly string.
     */
    public static String displayString(Object obj, boolean guessTime) {
        if (obj == null)
            return "null";
        if (obj instanceof Character)
            return "'" + toJavaChar((Character) obj, true) + "'";
        else if (obj instanceof String)
            return '"' + toJavaString((CharSequence) obj) + '"';
        else if (obj instanceof Long) {
            long lval = (Long) obj;
            if (guessTime && probablyTime(lval)) {
                return toDateString(lval);
            }
            return obj.toString() + "L";
        } else if (obj instanceof Float) {
            return obj.toString() + "f";
        } else if (obj instanceof Date) {
            return toString((Date) obj);
        }
        return obj.toString();
    }

    /**
     * Returns the string for the date.  This uses the date formatter as
     * specified by {@link #setDateFormat(DateFormat)}.
     *
     * @param date The date object.
     */
    public static String toString(Date date) {
        return dateFormat.format(date);
    }

    /**
     * Returns the string for the given {@code long} as a date.  This is used by
     * {@link #displayString(Object,boolean)} when it guesses that a {@code
     * long} is a date.
     *
     * @param lval The {@code long}.
     *
     * @see #toString(Date)
     */
    public static String toDateString(long lval) {
        return dateFormat.format(new Date(lval));
    }

    private static boolean probablyTime(long lval) {
        if (System.currentTimeMillis() - timeBoundsSet > 60 * 60 * 1000) {
            Calendar cal = Calendar.getInstance();
            timeBoundsSet = cal.getTimeInMillis();
            cal.add(Calendar.YEAR, -10);
            earliestTime = cal.getTimeInMillis();
            cal.add(Calendar.YEAR, 20);
            latestTime = cal.getTimeInMillis();
        }
        return earliestTime < lval && lval < latestTime;
    }

    /**
     * Returns a string representation of the object, using {@code '\n'} as a
     * separator. Equivalent to <tt>toString(obj, '\n')</tt>.
     *
     * @param obj The object.
     *
     * @see #toString(Object, String)
     */
    public static String toString(Object obj) {
        return toString(obj, "\n");
    }

    /**
     * Returns a string for this object. If the object defines {@code toString}
     * then the result of that method is returned. If not, it puts together a
     * string built from the properties of the object via reflection. Properties
     * in this case are public fields, {@code getFoo()} methods, and {@code
     * isFoo()} methods.
     * <p/>
     * If the object passed in is a {@code Class} object then the static fields
     * and methods of the class will define the properties put into the string.
     * <p/>
     * This is primarily for debugging -- strings produced like this are almost
     * never user-friendly enough for customers.
     * <p/>
     * If this method encounters an exception, that will be embedded between
     * {@code <} and {@code >}. The exception itself will be logged as a warning
     * to the logger for the object as returned by {@link
     * SimplxLogging#loggerFor(Object)}.
     *
     * @param obj The object.
     * @param sep The separator between fields.
     *
     * @return A string representation of that object.
     */
    public static String toString(Object obj, String sep) {
        if (obj == null)
            return "null";

        ToStringInfo[] infos = getInfos(obj);
        if (infos == USE_TOSTRING)
            return obj.toString();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < infos.length; i++) {
            ToStringInfo info = infos[i];
            if (i > 0)
                sb.append(sep);
            try {
                info.append(sb, obj);
            } catch (Exception e) {
                Logger logger = SimplxLogging.loggerFor(obj);
                logger.log(Level.WARNING, e.toString(), e);
                sb.append("<").append(e).append(">");
            }
        }
        return sb.toString();
    }

    private static ToStringInfo[] getInfos(Object obj) {

        boolean useStatic = obj instanceof Class;
        Class type = useStatic ? (Class) obj : obj.getClass();
        ToStringInfo[] infos = useStatic ?
                staticStringInfo.get(type) :
                objStringInfo.get(type);

        if (infos != null)
            return infos;

        try {
            Method toString = type.getMethod("toString", NO_ARGS);
            if (!useStatic && !toString.getDeclaringClass().equals(
                    Object.class)) {
                objStringInfo.put(type, USE_TOSTRING);
                return USE_TOSTRING;
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }

        List<ToStringInfo> printers = new ArrayList<ToStringInfo>();

        Collection<String> printed = new TreeSet<String>(
                String.CASE_INSENSITIVE_ORDER);
        Field[] fields = type.getFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) != useStatic)
                continue;
            String name = field.getName();
            printers.add(new ToStringInfo(name, field));
            if (useStatic)
                printed.add(stripUnderscore(name));
            else
                printed.add(name);
        }

        Method[] methods = type.getMethods();
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers()) != useStatic)
                continue;
            if (method.getParameterTypes().length != 0)
                continue;

            // If we already know this from the variable itself, ignore
            String name = method.getName();
            if (printed.contains(name))
                continue;

            if (name.equals("getClass"))
                continue;
            if (name.startsWith("get"))
                name = name.substring(3);
            else if (name.startsWith("is"))
                name = name.substring(2);
            else
                continue;

            char[] chars = name.toCharArray();
            int p;
            for (p = 0; p < name.length(); p++) {
                if (Character.isLowerCase(chars[p]))
                    break;
            }

            if (p == 0) // must have initial cap
                continue;
            int end = Math.max(1, p - 1);
            for (int j = 0; j < end; j++)
                chars[j] = Character.toLowerCase(chars[j]);
            name = new String(chars);

            if (!printed.contains(name))
                printers.add(new ToStringInfo(name, method));
        }

        infos = printers.toArray(new ToStringInfo[printed.size()]);
        Arrays.sort(infos);

        if (useStatic)
            staticStringInfo.put(type, infos);
        else
            objStringInfo.put(type, infos);
        return infos;
    }

    private static String stripUnderscore(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) != '_')
                sb.append(name.charAt(i));
        }
        return sb.length() == name.length() ? name : sb.toString();
    }
}
