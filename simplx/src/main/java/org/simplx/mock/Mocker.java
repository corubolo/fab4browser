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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class is a superclass for mock services.  It abstracts queries and
 * responses so they can be responded to by matchers.
 *
 * @author Ken Arnold
 * @param <Q> The type of queries for the service.
 * @param <A> The type of responses for the service.
 */
public class Mocker<Q, A> {
    private final List<Q> history = new ArrayList<Q>();
    private final SortedSet<MockHandler<Q, A>> handlers =
            new TreeSet<MockHandler<Q, A>>();
    private Thread server;
    private volatile boolean shutDown = false;

    /**
     * Adds a new handler for the service to use.
     *
     * @param handler The new handler.
     *
     * @return The handler that was passed in.
     */
    public <T extends MockHandler<Q, A>> T addHandler(T handler) {
        getHandlers().add(handler);
        return handler;
    }

    /**
     * Returns the handlers. This is the actual set used, and changes made to it
     * will be reflected in future calls to this server.
     *
     * @return The set of handlers.
     */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    public SortedSet<MockHandler<Q, A>> getHandlers() {
        return handlers;
    }

    /**
     * Shuts down this service. This function interrupts the running thread (if
     * any), and waits to join it before proceeding.
     *
     * @throws InterruptedException Joining the server thread was interrupted.
     */
    public void shutDown() throws InterruptedException {
        shutDown = true;
        server.interrupt();
        server.join();
    }

    /**
     * Returns {@code true} if this service has been shut down.
     *
     * @return {@code true} if this service has been shut down.
     */
    public boolean isShutDown() {
        return shutDown;
    }

    /**
     * Returns the history of URLs sent to this server. This allows you to
     * examine the actual set of requests, for example to ensure that it was
     * sent properly.  The returned list is unmodifiable.
     *
     * @return The history of URLs sent to this server.
     */
    public List<Q> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Returns the server thread for the service, if any.
     *
     * @return The server thread for the service, if any.
     */
    protected Thread getServer() {
        return server;
    }

    /**
     * Sets the server thread for the service.  If this is changed while the
     * service  is running, you are responsible for managing threads that
     * precede the one in the last {@link #setServer} call that executes.
     *
     * @param server The server thread for the service.
     */
    protected void setServer(Thread server) {
        this.server = server;
    }

    /**
     * Adds a request to the request history.
     *
     * @param request The request to add.
     */
    protected void addHistory(Q request) {
        history.add(request);
    }

    /**
     * Returns the response for the given request.
     *
     * @param request The request.
     *
     * @return The response for the given request.
     */
    protected A getResponse(Q request) {
        A response = null;
        for (MockHandler<Q, A> handler : handlers) {
            response = handler.response(this, request);
            if (response != null)
                break;
        }
        return response;
    }
}
