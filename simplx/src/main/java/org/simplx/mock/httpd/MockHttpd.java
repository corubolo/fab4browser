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

package org.simplx.mock.httpd;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.simplx.mock.MatchHandler;
import org.simplx.mock.MockHandler;
import org.simplx.mock.Mocker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a simple http-like server that can be used for testing.
 * The server runs as a thread in the current process, listening on a socket for
 * URL requests. When it gets a request, it looks through a list of
 * user-provided handlers for URLs. When it finds one that provides a response
 * for the incoming request, it returns that response to the caller. The list of
 * handlers can be prioritized, so that certain handlers will be checked before
 * others.
 * <p/>
 * The request type will be a string that is the part under the host
 * specification of the URL. For example, if the URL was {@code
 * "http://localhost:5153/login?user=foo"}, the path would be {@code
 * "/login?user=foo"}.
 * <p/>
 * The expected use is for code that gets information from a URL and processes
 * it. This class can be used to mock up that response at varying levels of
 * complexity, as your specific test code requires. For example, one handler
 * looks for regular expressions, and when a match is found, returns as the
 * result either a fixed string or the result of running a regular expression
 * "replaceAll" on the string. This could be used to mock up a response for a
 * specific series of tests. Suppose that the test code looked something like
 * this:
 * <pre>
 * URL loginURL = getLoginURL();
 * assertFalse(site.validLogin(loginURL, "user", "wrongPassword"));
 * assertTrue(site.validLogin(loginURL, "user", "goodPassword"));
 * </pre>
 * where {@code validLogin} is the function that will be tested for correctness
 * in handling server responses to login requests. To set up this test code, you
 * could do something like the following:
 * <pre>
 * MockHttpd httpd = new MockHttpd();
 * try {
 *     httpd.addHandler(
 *         new MatchHandler(".*wrong.*", "&lt;login valid=\"no\"/>"));
 *     httpd.addHandler(
 *         new MatchHandler(".*good.*", "&lt;login valid=\"yes\"/>"));
 * &nbsp;
 *     URL loginURL = new URL(httpd.getBase() + "/login");
 *     assertFalse(site.validLogin(loginURL, "robin", "wrongPassword"));
 *     assertTrue(site.validLogin(loginURL, "robin", "goodPassword"));
 * } finally {
 *     httpd.shutDown();
 * }
 * </pre>
 * <ul> <li>The method {@link #getBase()} returns the base URL for this
 * instance, which is typically something like {@code "http://localhost:5351"}.
 * <li>When the {@code validLong} method uses a URL to log in to the site, it
 * will presumably create a URL of the form {@link "http://localhost:5351/login?user=robin&amp;pwd=wrongPassword"}.
 * <li> When {@link MockHttpd} receives this request, it will ask each of the
 * handlers if they have a response for this URL. In this case, the first of the
 * {@link MatchHandler} objects will find the pattern ".*wrong.*" in the URL, so
 * it will return the associated string. <li>The {@code validLogin} method will
 * process that response, and should (according to the {@code assertFalse})
 * return {@code false}. <li>The scond call to {@code validLogin} will match the
 * ".*good.*" handler, and so return that matcher's response string, and {@code
 * validLogin} should return {@code true}. </ul>
 * <p/>
 * The handlers are stored in a {@link SortedSet SortedSet&lt;HttpHandler>}, and
 * are examined in order of priority. If two or more handlers share the same
 * priority, the order they are examined is undefined.
 *
 * @author Ken Arnold
 * @see MockHandler
 */
public class MockHttpd extends Mocker<String, String> {
    private static final Logger logger = Logger.getLogger(
            MockHttpd.class.getName());

    private final int port;
    private ServerSocket socket;
    private String base;

    /** The server thread that processes the requests. */
    private class ServerThread extends Thread {
        @Override
        public void run() {
            try {
                while (!isShutDown()) {
                    BufferedReader in = null;
                    OutputStream out = null;
                    try {
                        Socket socket = MockHttpd.this.socket.accept();
                        in = new BufferedReader(new InputStreamReader(
                                socket.getInputStream()));
                        String line = in.readLine();
                        String path = line.split(" ")[1];
                        addHistory(line);
                        logger.info(socket.getInetAddress().getHostAddress() +
                                " : " + line);

                        String response = getResponse(path);
                        if (response == null)
                            response = "";

                        out = socket.getOutputStream();
                        out.write(("HTTP/1.0 200 OK\nContent-Length: " +
                                response.length() + "\n\n").getBytes());
                        out.write(response.getBytes());
                    } catch (InterruptedIOException ex) {
                        // ignore
                    } catch (IOException e) {
                        if (!isShutDown())
                            logger.log(Level.WARNING, e.getMessage(), e);
                    } finally {
                        if (out != null)
                            out.close();
                        if (in != null)
                            in.close();
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();    //To change body of overridden methods use File | Settings | File Templates.
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                // not a problem, already closed
            }
        }
    }

    /**
     * Creates a new {@code MockHttpd} on any free port.
     *
     * @throws IOException There is a problem creating the server socket.
     * @see #getPort()
     */
    public MockHttpd() throws IOException {
        this(0);
    }

    /**
     * Creates a new {@code MockHttpd} on a specified port.
     *
     * @param port The port the server will listen on.
     *
     * @throws IOException There is a problem creating the server socket.
     */
    public MockHttpd(int port) throws IOException {
        socket = new ServerSocket(port);
        // get the actual port used, because the param might be 0
        this.port = socket.getLocalPort();
        base = "http://" + socket.getInetAddress().getHostName() + ":" +
                this.port;
        setServer(new ServerThread());
        getServer().start();
    }

    /**
     * Returns the base URL for communicating with this server. This will be a
     * value such as {@code "http://localhost:5316"}.
     *
     * @return The base URL for communicating with this server.
     */
    public String getBase() {
        return base;
    }

    /**
     * Returns the port on which this server is listening.
     *
     * @return The port on which this server is listening.
     */
    public int getPort() {
        return port;
    }

    /**
     * Compares the result of a GET on the path to the expected result. This is
     * a utility method for use with JUnit. The URL used will be the {@link
     * #getBase() base} URL followed by the given path. The returned string is
     * compared to the expected one using JUnit's {@link
     * Assert#assertEquals(String,Object,Object) assertEquals}.
     *
     * @param path     The path under the base URL. If the base URL is {@code
     *                 "http://locahost:5153"} and {@code path} is {@code
     *                 "foo"}, the path used will be {@code "http://localhost:5153/foo"}.
     *                 This top-level {@code "/"} is only added if it is
     *                 missing.
     * @param expected The expected result.
     *
     * @throws IOException An error in the communication with the server.
     */
    public void compareResult(String path, String expected) throws IOException {
        if (path.length() == 0 || path.charAt(0) != '/')
            path = "/" + path;
        URL url = new URL(getBase() + path);
        String actual = IOUtils.toString(url.openStream());
        Assert.assertEquals(url.toString(), expected, actual);
    }
}
