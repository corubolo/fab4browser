package org.simplx.io;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

import static org.simplx.io.StringBuilderWriter.*;

public class TestStringBuilderWriter {
    @Test
    public void testInsertBuffer() {
        StringBuffer sb = new StringBuffer("abcde");
        StringBuilderWriter sbw = new StringBuilderWriter(sb, Mode.INSERT, 0);
        sbw.write('1');
        sbw.write('2');
        sbw.write('3');
        assertEquals(sbw.toString(), "123abcde");

        sbw = new StringBuilderWriter(sb, Mode.INSERT, 3);
        sbw.write("-=+");
        assertEquals(sbw.toString(), "123-=+abcde");

        sbw = new StringBuilderWriter(sb, Mode.INSERT, sb.length());
        sbw.write("_?.!:", 1, 3);
        assertEquals(sbw.toString(), "123-=+abcde?.!");
        assertEquals(sbw.toString(), sb.toString());
        assertSame(sbw.getStringBuffer(), sb);
    }

    @Test
    public void testOverwriteBuffer() {
        StringBuffer sb = new StringBuffer("abcde");
        StringBuilderWriter sbw = new StringBuilderWriter(sb, Mode.OVERWRITE,
                0);
        sbw.write('1');
        sbw.write('2');
        sbw.write('3');
        assertEquals(sbw.toString(), "123de");

        sbw = new StringBuilderWriter(sb, Mode.OVERWRITE, 3);
        sbw.write("-=+");
        assertEquals(sbw.toString(), "123-=+");

        sbw = new StringBuilderWriter(sb, Mode.OVERWRITE, sb.length());
        sbw.write("_?.!:", 1, 3);
        assertEquals(sbw.toString(), "123-=+?.!");
        assertEquals(sbw.toString(), sb.toString());
        assertSame(sbw.getStringBuffer(), sb);
    }

    @Test
    public void testAppendBuffer() {
        StringBuffer sb = new StringBuffer("abcde");
        StringBuilderWriter sbw = new StringBuilderWriter(sb);
        sbw.write('1');
        sbw.write('2');
        sbw.write('3');
        assertEquals(sbw.toString(), "abcde123");

        sbw = new StringBuilderWriter(sb, Mode.APPEND, 3);
        sbw.write("-=+");
        assertEquals(sbw.toString(), "abcde123-=+");

        sbw = new StringBuilderWriter(sb, Mode.APPEND, sb.length());
        sbw.write("_?.!:", 1, 3);
        assertEquals(sbw.toString(), "abcde123-=+?.!");
        assertEquals(sbw.toString(), sb.toString());
        assertSame(sbw.getStringBuffer(), sb);
    }

    @Test
    public void testInsertBuilder() {
        StringBuilder sb = new StringBuilder("abcde");
        StringBuilderWriter sbw = new StringBuilderWriter(sb, Mode.INSERT, 0);
        sbw.write('1');
        sbw.write('2');
        sbw.write('3');
        assertEquals(sbw.toString(), "123abcde");

        sbw = new StringBuilderWriter(sb, Mode.INSERT, 3);
        sbw.write("-=+");
        assertEquals(sbw.toString(), "123-=+abcde");

        sbw = new StringBuilderWriter(sb, Mode.INSERT, sb.length());
        sbw.write("_?.!:", 1, 3);
        assertEquals(sbw.toString(), "123-=+abcde?.!");
        assertEquals(sbw.toString(), sb.toString());
        assertSame(sbw.getStringBuilder(), sb);
    }

    @Test
    public void testOverwriteBuilder() {
        StringBuilder sb = new StringBuilder("abcde");
        StringBuilderWriter sbw = new StringBuilderWriter(sb, Mode.OVERWRITE,
                0);
        sbw.write('1');
        sbw.write('2');
        sbw.write('3');
        assertEquals(sbw.toString(), "123de");

        sbw = new StringBuilderWriter(sb, Mode.OVERWRITE, 3);
        sbw.write("-=+");
        assertEquals(sbw.toString(), "123-=+");

        sbw = new StringBuilderWriter(sb, Mode.OVERWRITE, sb.length());
        sbw.write("_?.!:", 1, 3);
        assertEquals(sbw.toString(), "123-=+?.!");
        assertEquals(sbw.toString(), sb.toString());
        assertSame(sbw.getStringBuilder(), sb);
    }

    @Test
    public void testAppendBuilder() {
        StringBuilder sb = new StringBuilder("abcde");
        StringBuilderWriter sbw = new StringBuilderWriter(sb);
        sbw.write('1');
        sbw.write('2');
        sbw.write('3');
        assertEquals(sbw.toString(), "abcde123");

        sbw = new StringBuilderWriter(sb, Mode.APPEND, 3);
        sbw.write("-=+");
        assertEquals(sbw.toString(), "abcde123-=+");

        sbw = new StringBuilderWriter(sb, Mode.APPEND, sb.length());
        sbw.write("_?.!:", 1, 3);
        assertEquals(sbw.toString(), "abcde123-=+?.!");
        assertEquals(sbw.toString(), sb.toString());
        assertSame(sbw.getStringBuilder(), sb);
    }
}