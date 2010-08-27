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

package org.simplx.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This utility class contains methods helpful when working with (mostly
 * debugging) regular expressions.
 */
public class RegexUtils {
    /** Implement this interface to use {@link RegexUtils#replaceAll(Pattern,CharSequence,String)}. */
    public interface Replacer {
        /**
         * Return the string to use to replace the current match of the given
         * {@code Matcher}.
         *
         * @param m The matcher that has just matched a pattern.
         */
        String replace(Matcher m);
    }

    /** Do nothing.  This is a utility class. */
    public RegexUtils() {
    }

    /**
     * Replace all the occurrences of the pattern in the given string.
     * Equivalent to {@link String#replaceAll(String,String)}.  This exists so
     * that code that uses {@link #replaceAll(Pattern ,CharSequence, Replacer)}
     * can universally use this utilty class for all replacements, instead of
     * using switching between this class and the built-in function.
     *
     * @param pattern     The pattern to use.
     * @param inString    The string in which to do replacement.
     * @param replacement The replacement string pattern.
     *
     * @return The resulting string.
     */
    public static String replaceAll(Pattern pattern, CharSequence inString,
            String replacement) {

        return pattern.matcher(inString).replaceAll(replacement);
    }

    /**
     * Replace all occurrences of the pattern in the given string with the
     * return value of a function.  For each match of the pattern, the {@link
     * Replacer#replace(Matcher)} method will be called with the matcher.  The
     * value returned will be the value used as the replacement pattern.
     * Replacement arguments such as "$1" for the first group are properly
     * handled.
     * <p/>
     * This implements the {@link Matcher#appendReplacement(StringBuffer,String)}
     * pattern so you can just provide the function that calculates the
     * replacement.
     *
     * @param pattern  The pattern to use.
     * @param inString The string in which to do replacement.
     * @param replacer The object that will provide the replacement pattern
     *                 string.
     *
     * @return The resulting string.
     */
    public static String replaceAll(Pattern pattern, CharSequence inString,
            Replacer replacer) {

        Matcher m = pattern.matcher(inString);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, replacer.replace(m));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}