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

package org.simplx.mock;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This handler uses regular expressions to match strings, such as URL paths.
 * Responses can be derived from the original string or not, as you choose.
 *
 * @author Ken Arnold
 * @param <Q> The type of the query object to be handled.
 */
public class MatchHandler<Q> extends MockHandler<Q, String> {
    private final Pattern matchPattern;
    private final Pattern replacePattern;
    private final String replaceWith;

    /**
     * Creates a new {@code MatchHandler} that will match strings with the given
     * pattern. If the path matches the {@code pattern}, then the result
     * returned will be the result of applying {@link String#replaceAll(String,String)}
     * to the original string, using {@code pattern} and {@code replaceWith} as
     * the arguments.
     * <p/>
     * To have a replacement that totally replaces the string (in other words,
     * that is independent of the string, not a modificaiton of it) use {@code
     * .*} in the pattern. For example, {@code new&nbsp;MatchHandler(".*wrong.*",
     * "<login valid=\"no\"/>")} would make any incoming string with "wrong" in
     * it return the XML {@code <login valid="no"/>}.
     * <p/>
     * This is equivalent to calling {@link #MatchHandler(String,String,String)}
     * with the two patterns being the same.
     *
     * @param pattern     The pattern to use for matching and (possibly)
     *                    modifying the path to get the response, as used in
     *                    {@link String#replaceAll(String,String)}.
     * @param replaceWith The replacement of the pattern, as used in {@link
     *                    String#replaceAll(String,String)}. For example, this
     *                    can include group replacements, such as {@code "$1"}
     *                    to use groups from {@code replacePattern}.
     */
    public MatchHandler(String pattern, String replaceWith) {
        this.replaceWith = replaceWith;
        matchPattern = Pattern.compile(pattern);
        replacePattern = matchPattern;
    }

    /**
     * Creates a new {@code MatchHandler} that has a separate replacement
     * pattern from the one that matches strings. If the string matches the
     * {@code matchPattern}, then the result returned will be the result of
     * applying {@link String#replaceAll(String,String)} to the original string,
     * using {@code replacePattern} and {@code replaceWith} as the arguments.
     *
     * @param matchPattern   The pattern for matching the URL string. If this is
     *                       matched, the returned result will be that specified
     *                       by the other two parameters.
     * @param replacePattern The pattern to use for replacing the string to get
     *                       the response, as used in {@link String#replaceAll(String,String)}.
     * @param replaceWith    The replacement of the pattern, as used in {@link
     *                       String#replaceAll(String,String)}. For example,
     *                       this can include group replacements, such as {@code
     *                       "$1"} to use groups from {@code replacePattern}.
     */
    public MatchHandler(String matchPattern, String replacePattern,
            String replaceWith) {

        this.replaceWith = replaceWith;
        this.matchPattern = Pattern.compile(matchPattern);
        this.replacePattern = Pattern.compile(replacePattern);
    }

    /**
     * If the string version of the request matches that specified in the
     * constructor, returns the response; otherwise, returns {@code null}.
     */
    @Override
    public String response(Mocker<Q, String> mocker, Q request) {
        String str = stringRequest(request);
        Matcher m = matchPattern.matcher(str);
        if (m.find())
            return getResponse(mocker, m, str);
        else
            return null;
    }

    /**
     * Returns the response, based on the value of the {@link Matcher} object
     * that found the match. This method is called if the matcher created for
     * the matching pattern finds a match in the string.
     * <p/>
     * This method is provided so you can write subclasses that do other things
     * than a simple {@link Matcher#replaceAll(String)} (such as using {@link
     * Matcher#replaceFirst(String)}).
     * <p/>
     * This default implementation simply replaces everything in the
     * "replaceAll" pattern with the "replaceWith" string.
     *
     * @param mocker  The mocker.
     * @param matcher The matcher that found the match.
     * @param str     The matched string.
     *
     * @return The response string for {@link MockHandler#response(Mocker,Object)}.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public String getResponse(Mocker<Q, String> mocker, Matcher matcher,
            CharSequence str) {

        return replacePattern.matcher(str).replaceAll(replaceWith);
    }
}
