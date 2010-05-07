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

/**
 * A handler for matching on basic string operations. The most basic operation
 * is to look for a path that contains a specified string, so a constructor to
 * do that is provided. But you can have other string checks if you like, some
 * of which are provided here for convenience.
 *
 * @author Ken Arnold
 * @param <Q> The type of query comming into the mocker.
 * @param <A> The type of response returned by the mocker.
 */
public class StringHandler<Q, A> extends MockHandler<Q, A> {
    private final A response;
    private final Checker checker;

    /**
     * A checker object that discovers if a given path appiles to a {@link
     * StringHandler}.
     */
    public interface Checker {
        /**
         * Returns {@code true} if the given path satisifies the check this
         * object specifies.
         *
         * @param path The path to check.
         *
         * @return {@code true} if the given path satisifies the check this
         *         object specifies.
         */
        boolean applies(String path);
    }

    /** A checker class that uses {@link String#equals(Object)}. */
    public static class Equals implements Checker {
        private final String str;

        /**
         * Creates a new {@code Equals} checker for the given string.
         *
         * @param str The string to pass to {@link String#equals(Object)}.
         */
        public Equals(String str) {
            this.str = str;
        }

        /**
         * Returns {@code true} if the path equals the string specified at
         * creation.
         *
         * @param path The path to check.
         *
         * @return {@code true} if the path equals the prefix specified at
         *         creation.
         */
        public boolean applies(String path) {
            return path.equals(str);
        }
    }

    /** A checker class that uses {@link String#startsWith(String)}. */
    public static class StartsWith implements Checker {
        private final String prefix;

        /**
         * Creates a new {@code StartsWith} checker for the given prefix.
         *
         * @param prefix The prefix to pass to {@link String#startsWith(String)}.
         */
        public StartsWith(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Returns {@code true} if the path starts with the prefix specified at
         * creation.
         *
         * @param path The path to check.
         *
         * @return {@code true} if the path starts with the prefix specified at
         *         creation.
         */
        public boolean applies(String path) {
            return path.startsWith(prefix);
        }
    }

    /** A checker class that uses {@link String#contains(CharSequence)}. */
    public static class Contains implements Checker {
        private final String str;

        /**
         * Creates a new {@code Contains} checker for the given string.
         *
         * @param str The prefix to pass to {@link String#contains(CharSequence)}.
         */
        public Contains(String str) {
            this.str = str;
        }

        /**
         * Returns {@code true} if the path contains the string specified at
         * creation.
         *
         * @param path The path to check.
         *
         * @return {@code true} if the path contains the string specified at
         *         creation.
         */
        public boolean applies(String path) {
            return path.contains(str);
        }
    }

    /**
     * Creates a handler that will return a response if the string contains the
     * given string.
     *
     * @param contains The string that the path would contain.
     * @param response The response to return of the string does contain {@code
     *                 contains}.
     *
     * @see Contains
     */
    public StringHandler(String contains, A response) {
        this(new Contains(contains), response);
    }

    /**
     * Creates a handler that will return a response if the given check says
     * that a path applies.
     *
     * @param checker  The checker to use to see if the path applies.
     * @param response The response to return of the string does contain {@code
     *                 contains}.
     *
     * @see Contains
     */
    public StringHandler(Checker checker, A response) {
        this.checker = checker;
        this.response = response;
    }

    /**
     * If the path applies to this handler, returns the response provided at
     * creation; otherwise returns {@code null}.
     *
     * @return The response provided at creation, if appropriate.
     */
    @Override
    public A response(Mocker<Q, A> mocker, Q path) {
        String str = stringRequest(path);
        return checker.applies(str) ? response : null;
    }
}
