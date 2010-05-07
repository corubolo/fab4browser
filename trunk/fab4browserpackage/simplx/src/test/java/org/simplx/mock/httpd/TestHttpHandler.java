package org.simplx.mock.httpd;

import org.apache.commons.io.IOUtils;
import org.simplx.mock.MatchHandler;
import org.simplx.mock.MockHandler;
import org.simplx.mock.NumberedHandler;
import org.simplx.mock.StringHandler;
import org.simplx.mock.StringHandler.Contains;
import org.simplx.mock.StringHandler.Equals;
import org.simplx.mock.StringHandler.StartsWith;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URL;

public class TestHttpHandler {
    @Test
    public void testStringHandler() throws IOException, InterruptedException {
        MockHttpd httpd = new MockHttpd();

        try {
            httpd.addHandler(new StringHandler<String, String>("/first",
                    "The first string\n"));
            httpd.addHandler(new StringHandler<String, String>(new Equals(
                    "/second"), "Another string\n"));
            httpd.addHandler(new StringHandler<String, String>(new StartsWith(
                    "/th"), "Yet one more string\n"));
            httpd.addHandler(new StringHandler<String, String>(new Contains(
                    "our"), "Final string\n"));

            httpd.compareResult("/first", "The first string\n");
            httpd.compareResult("/second", "Another string\n");
            httpd.compareResult("/third", "Yet one more string\n");
            httpd.compareResult("/fourth", "Final string\n");
        } finally {
            httpd.shutDown();
        }
    }

    @Test
    public void testMatchHandler() throws IOException, InterruptedException {
        MockHttpd httpd = new MockHttpd();

        try {
            httpd.addHandler(new StringHandler<String, String>("/first",
                    "The first string\n"));
            httpd.addHandler(new MatchHandler("sec", "^.*$",
                    "Another string\n"));
            httpd.addHandler(new MatchHandler(".*/([0-9]+)$", "Got $1"));

            httpd.compareResult("/first", "The first string\n");
            httpd.compareResult("/second", "Another string\n");
            httpd.compareResult("/get/15", "Got 15");
            httpd.compareResult("/get/h15", "");
        } finally {
            httpd.shutDown();
        }
    }

    @Test
    public void testExample() throws IOException, InterruptedException {
        class MockSite {
            public boolean validLogin(URL loginURL, String user, String pwd)
                    throws IOException {
                URL login = new URL(loginURL,
                        "?user=" + user + "&password=" + pwd);
                String str = IOUtils.toString(login.openStream());
                return str.equals("<login valid=\"yes\"/>");
            }
        }
        MockSite site = new MockSite();

        MockHttpd httpd = new MockHttpd();
        try {
            httpd.addHandler(new MatchHandler(".*wrong.*",
                    "<login valid=\"no\"/>"));
            httpd.addHandler(new MatchHandler(".*good.*",
                    "<login valid=\"yes\"/>"));

            URL loginURL = new URL(httpd.getBase() + "/login");
            assertFalse(site.validLogin(loginURL, "robin", "wrongPassword"));
            assertTrue(site.validLogin(loginURL, "robin", "goodPassword"));
        } finally {
            httpd.shutDown();
        }
    }

    @Test
    public void testPriorities() throws IOException, InterruptedException {
        MockHttpd httpd = new MockHttpd();

        try {
            MockHandler h1 = new StringHandler<String, String>("/first",
                    "first handler\n");
            h1.setPriority(1.0f);
            MockHandler h2 = new StringHandler<String, String>("/first",
                    "second handler\n");
            h2.setPriority(2.0f);

            httpd.addHandler(h2);
            httpd.compareResult("/first", "second handler\n");
            httpd.addHandler(h1);
            httpd.compareResult("/first", "first handler\n");
            httpd.getHandlers().remove(h1);
            httpd.compareResult("/first", "second handler\n");
        } finally {
            httpd.shutDown();
        }
    }

    @Test
    public void testOverallNumbering()
            throws IOException, InterruptedException {
        MockHttpd httpd = new MockHttpd();

        try {
            MockHandler def = new StringHandler<String, String>("/",
                    "default\n");
            def.setPriority(1);
            httpd.addHandler(def);
            httpd.addHandler(new StringHandler<String, String>("/ping",
                    "OK\n"));
            httpd.addHandler(new NumberedHandler<String, String>(0,
                    "login OK\n"));
            httpd.addHandler(new NumberedHandler<String, String>(1, 11,
                    "value store OK\n"));
            httpd.addHandler(new NumberedHandler<String, String>(11,
                    "answer: 17.5\n"));

            httpd.compareResult("/", "login OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/ping", "OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "answer: 17.5\n");
            httpd.compareResult("/ping", "OK\n");
            httpd.compareResult("/", "default\n");
        } finally {
            httpd.shutDown();
        }
    }

    @Test
    public void testCountNumbering() throws IOException, InterruptedException {
        MockHttpd httpd = new MockHttpd();

        try {
            MockHandler def = new StringHandler<String, String>("/",
                    "default\n");
            def.setPriority(1);
            httpd.addHandler(def);
            httpd.addHandler(new StringHandler<String, String>("/ping",
                    "OK\n"));
            httpd.addHandler(new NumberedHandler<String, String>(0, false,
                    "login OK\n"));
            httpd.addHandler(new NumberedHandler<String, String>(1, 11, false,
                    "value store OK\n"));
            httpd.addHandler(new NumberedHandler<String, String>(11, false,
                    "answer: 17.5\n"));

            httpd.compareResult("/", "login OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/ping", "OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "value store OK\n");
            httpd.compareResult("/", "answer: 17.5\n");
            httpd.compareResult("/ping", "OK\n");
            httpd.compareResult("/", "default\n");
        } finally {
            httpd.shutDown();
        }
    }
}