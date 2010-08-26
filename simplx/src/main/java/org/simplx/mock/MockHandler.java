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
 * Specifies one way to handle an incoming request, as used by {@link Mocker}.
 *
 * @author Ken Arnold
 * @param <Q> The type of query comming into the mocker.
 * @param <A> The type of response returned by the mocker.
 */
public abstract class MockHandler<Q, A>
        implements Comparable<MockHandler<Q, A>> {
    private float priority;
    private final int tieBreaker;

    private static int nextTieBreaker = 1;

    /** Creates a new object with a priority of 0.0. */
    public MockHandler() {
        this(0.0f);
    }

    /**
     * Creates a new object with the given priority.
     *
     * @param priority The priority.
     */
    public MockHandler(float priority) {
        this.priority = priority;
        tieBreaker = getNexTieBreaker();
    }

    private static synchronized int getNexTieBreaker() {
        return nextTieBreaker++;
    }

    /**
     * The response this handler provides for the given request, or {@code null}
     * if this handler has no response to give.
     *
     * @param mocker  The {@link Mocker}.
     * @param request The request.
     *
     * @return The response from this handler, or {@code null}.
     */
    public abstract A response(Mocker<Q, A> mocker, Q request);

    /**
     * Returns the priority of this handler. A lower value means that the
     * handler will be examined sooner. A priority of zero is considered normal,
     * as a matter of custom.
     *
     * @return The priority of this handler.
     */
    public float getPriority() {
        return priority;
    }

    /**
     * Sets the priority of this object. Do not change this after the handler
     * has been added to the {@link Mocker}.
     *
     * @param priority The new priority.
     */
    public void setPriority(float priority) {
        this.priority = priority;
    }

    /**
     * Returns a value less than, equal to, or greater than zero as the order of
     * this handler is less than, equal to, or greater than the other one.
     *
     * @param that The other handler.
     *
     * @return The ordering for this handler vs. the other.
     */
    public int compareTo(MockHandler<Q, A> that) {
        int cmp = Float.compare(getPriority(), that.getPriority());
        if (cmp != 0)
            return cmp;
        return tieBreaker - that.tieBreaker;
    }

    /**
     * Returns {@code true} if the other object is a {@code HttpHandler} with
     * the same priority as this one.
     *
     * @param that The other object.
     *
     * @return {@code true} if the other object is a {@code HttpHandler} with
     *         the same priority as this one.
     */
    @Override
    public boolean equals(Object that) {
        if (that instanceof MockHandler) {
            //noinspection unchecked
            return equals((MockHandler<Q, A>) that);
        }
        return false;
    }

    /**
     * Returns {@code true} if the other {@code MockHandler} is the same object
     * as this one.
     *
     * @param that The other {@code HttpHandler}.
     *
     * @return {@code true} if the other {@code HttpHandler} has the same
     *         priority as this one.
     */
    @SuppressWarnings({"ObjectEquality"})
    public boolean equals(MockHandler<Q, A> that) {
        return this == that;
    }

    /**
     * Returns a hash code based for this object.
     *
     * @return A hash code based for this object.
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Returns the string version of the request.  If the request is {@code
     * null}, returns {@code null}. Otherwise, this returns the result of {@link
     * Object#toString()}.
     *
     * @param request The request object.
     *
     * @return The string version of the request.
     */
    protected String stringRequest(Q request) {
        return request == null ? null : request.toString();
    }
}
