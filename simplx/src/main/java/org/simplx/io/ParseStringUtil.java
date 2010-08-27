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

import static org.simplx.io.ToStringUtil.toJavaChar;
import static org.simplx.io.ToStringUtil.toJavaString;

/**
 * This utility class provides methods to parse strings.  In general, its
 * methods are the inverse of comparable ones in {@link ToStringUtil}.
 */
public class ParseStringUtil {
    /**
     * Parse the character encoded by the given string, according to {@link
     * #parseNextChar(String,int,int[])}.
     *
     * @param str The string to parse.
     *
     * @return The parsed character.
     */
    public static char parseChar(String str) {
        return getNextChar(str, 0, null, false);
    }

    /**
     * Parse the Java character encoded by the given string, according to {@link
     * #parseNextJavaChar(String,int,int[])}.
     *
     * @param str The string to parse.
     *
     * @return The parsed character.
     */
    public static char parseJavaChar(String str) {
        return getNextChar(str, 0, null, true);
    }

    /**
     * Parse the next character encoded by the given string.  This decodes
     * standard Java character syntax, except that it allows <tt>\</tt> to
     * escape any character, not just those special to Java strings; for example
     * <tt>\q</tt> will return a <tt>'q'</tt>. See {@link
     * #parseNextJavaChar(String,int,int[])} for more details.
     *
     * @param str     The string to parse.
     * @param pos     The position in the string at which to start parsing.
     * @param nextPos If not <tt>null</tt>, will be filled in with the index in
     *                the string past the character just parsed.
     *
     * @return The parsed character.
     */
    public static char parseNextChar(String str, int pos, int[] nextPos) {
        return getNextChar(str, pos, nextPos, false);
    }

    /**
     * Parse the next character encoded by the given string.  This decodes
     * standard Java character syntax, except that it allows <tt>\</tt> to
     * escape any character, not just those special to Java strings. <ul> <li>If
     * the next character is not a backslash, it is returned. <li>If the
     * backslash is followed by one or more <tt>u</tt> characters, the following
     * four characters are hex digits, and the the <tt>\</tt><tt>u<i>xxxx</i></tt>
     * sequence is turned into a Unicode character. <li>If the backslash is
     * followed by a 0, 1, 2, or 3, the characters are interpreted as an octal
     * character constant. <li>Otherwise, the character after the backslash is
     * interpreted as a Java string special character.  For example, <tt>\t</tt>
     * is a tab.  If the character is not a Java special character sequence, you
     * will get an <tt>IllegalArgumentException</tt>. </ul>
     *
     * @param str     The string to parse.
     * @param pos     The position in the string at which to start parsing.
     * @param nextPos If not <tt>null</tt>, will be filled in with the index in
     *                the string past the character just parsed.
     *
     * @return The parsed character.
     */
    public static char parseNextJavaChar(String str, int pos, int[] nextPos) {
        return getNextChar(str, pos, nextPos, true);
    }

    private static char getNextChar(String str, int pos, int[] nextPos,
            boolean strict) {

        if (pos >= str.length())
            throw new IllegalArgumentException("No characters left in string");

        if (str.charAt(pos) != '\\') {
            setPos(nextPos, pos + 1);
            return str.charAt(pos);
        }

        int remain = str.length() - pos;
        if (remain == 1) {
            throw new IllegalArgumentException(
                    "\"\\\" is not a valid character specification");
        }

        int endPos = pos + 1;
        char ch = str.charAt(endPos);

        setPos(nextPos, pos + 2);
        switch (ch) {
        case 'n':
            return '\n';

        case 'r':
            return '\r';

        case 't':
            return '\t';

        case 'b':
            return '\b';

        case 'f':
            return '\f';

        case '\\':
        case '\'':
        case '\"':
            return ch;

        case 'u':
            int i = pos + 2;
            while (str.charAt(i) == 'u')    // any number of u's can be present
                i++;
            if (str.length() - i < 4) {
                throw new IllegalArgumentException("\"" + toJavaString(str) +
                        "\": requires 4 hex digits");
            }
            try {
                char res = (char) Integer.parseInt(str.substring(i, i + 4), 16);
                setPos(nextPos, i + 4);
                return res;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\"" + toJavaString(str) +
                        "\": " + e, e);
            }

        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
            int val = 0;
            try {
                val = ch - '0';
                ch = str.charAt(++endPos);
                if (ch >= '0' && ch <= '7') {
                    val = val * 8 + ch - '0';
                    ch = str.charAt(++endPos);
                    if (ch >= '0' && ch <= '7') {
                        val = val * 8 + ch - '0';
                        endPos++;
                    }
                }
            } catch (StringIndexOutOfBoundsException ignored) {
                // This means that we reached the end of the string instead of
                // another character after the octal specification.
                // That's OK, the value we have is the one we should work with
            }
            if (val > 0xff) {
                throw new IllegalArgumentException(
                        "Invalid octal character: \"" + str.substring(pos,
                                pos + 4));
            }
            setPos(nextPos, endPos);
            return (char) val;

        default:
            if (strict) {
                throw new IllegalArgumentException("\"\\" + toJavaChar(ch) +
                        "\": invalid escape");
            } else {
                return ch;
            }
        }
    }

    private static void setPos(int[] nextPos, int i) {
        if (nextPos != null) {
            nextPos[0] = i;
        }
    }

    /**
     * Parse an entire string as a sequence of calls to {@link
     * #parseNextChar(String,int,int[])}. This will be link to a {@code
     * StringBuilder}.
     *
     * @param str  The string to parse.
     * @param into The {@code StringBuilder} into which to parse it.  If
     *             <tt>null</tt>, a new {@code StringBuilder} is created.
     *
     * @return The {@code StringBuilder} into which the string was parsed.
     */
    public static StringBuilder parseString(String str, StringBuilder into) {
        return parseString(str, into, false);
    }

    /**
     * Parse an entire string as a sequence of calls to {@link
     * #parseNextJavaChar(String,int,int[])}. This will be appended to a {@code
     * StringBuilder}.
     *
     * @param str  The string to parse.
     * @param into The {@code StringBuilder} into which to parse it.  If
     *             <tt>null</tt>, a new {@code StringBuilder} is created.
     *
     * @return The {@code StringBuilder} into which the string was parsed.
     */
    public static StringBuilder parseJavaString(String str,
            StringBuilder into) {
        return parseString(str, into, true);
    }

    private static StringBuilder parseString(String str, StringBuilder into,
            boolean strict) {

        if (into == null)
            into = new StringBuilder();
        int i = 0;
        int[] nextPos = new int[1];
        while (i < str.length()) {
            into.append(getNextChar(str, i, nextPos, strict));
            i = nextPos[0];
        }
        return into;
    }

    /**
     * Parse an entire string as a sequence of calls to {@link
     * #parseNextChar(String,int,int[])}.
     *
     * @param str The string to parse.
     *
     * @return The resulting string.
     */
    public static String parseString(String str) {
        StringBuilder sb = parseString(str, null);
        return sb.toString();
    }

    /**
     * Parse an entire string as a sequence of calls to {@link
     * #parseNextJavaChar(String,int,int[])}.
     *
     * @param str The string to parse.
     *
     * @return The resulting string.
     */
    public static String parseJavaString(String str) {
        StringBuilder sb = parseJavaString(str, null);
        return sb.toString();
    }
}