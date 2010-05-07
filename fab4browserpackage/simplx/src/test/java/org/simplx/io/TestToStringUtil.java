package org.simplx.io;

import org.apache.commons.lang.StringUtils;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.simplx.io.ToStringUtil;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TestToStringUtil {
    @Test
    public void testChars() {
        assertEquals(ToStringUtil.toJavaChar('x'), "x");
        assertEquals(ToStringUtil.toJavaChar('\n'), "\\n");
        assertEquals(ToStringUtil.toJavaChar('\r'), "\\r");
        assertEquals(ToStringUtil.toJavaChar('\b'), "\\b");
        assertEquals(ToStringUtil.toJavaChar('\f'), "\\f");
        assertEquals(ToStringUtil.toJavaChar('\t'), "\\t");
        assertEquals(ToStringUtil.toJavaChar('"'), "\\\"");
        assertEquals(ToStringUtil.toJavaChar('\''), "\\'");
        assertEquals(ToStringUtil.toJavaChar('\\'), "\\\\");
        assertEquals(ToStringUtil.toJavaChar('\u53fa'), "\u53fa");

        assertEquals(ToStringUtil.toJavaChar('x', true), "x");
        assertEquals(ToStringUtil.toJavaChar('\n', true), "\\n");
        assertEquals(ToStringUtil.toJavaChar('\r', true), "\\r");
        assertEquals(ToStringUtil.toJavaChar('\b', true), "\\b");
        assertEquals(ToStringUtil.toJavaChar('\f', true), "\\f");
        assertEquals(ToStringUtil.toJavaChar('\t', true), "\\t");
        assertEquals(ToStringUtil.toJavaChar('"', true), "\\\"");
        assertEquals(ToStringUtil.toJavaChar('\'', true), "\\'");
        assertEquals(ToStringUtil.toJavaChar('\\', true), "\\\\");
        assertEquals(ToStringUtil.toJavaChar('\u53fa', true), "\u53fa");

        assertEquals(ToStringUtil.toJavaChar('x', false), "x");
        assertEquals(ToStringUtil.toJavaChar('\n', false), "\n");
        assertEquals(ToStringUtil.toJavaChar('\r', false), "\r");
        assertEquals(ToStringUtil.toJavaChar('\b', false), "\\b");
        assertEquals(ToStringUtil.toJavaChar('\f', false), "\f");
        assertEquals(ToStringUtil.toJavaChar('\t', false), "\t");
        assertEquals(ToStringUtil.toJavaChar('"', false), "\\\"");
        assertEquals(ToStringUtil.toJavaChar('\'', false), "\\'");
        assertEquals(ToStringUtil.toJavaChar('\\', false), "\\\\");
        assertEquals(ToStringUtil.toJavaChar('\u53fa', false), "\u53fa");
    }

    @Test
    public void testString() {
        assertEquals(ToStringUtil.toJavaString("x\n\r\b\f\t\"'\\\u53fa"),
                "x\\n\\r\\b\\f\\t\\\"\\'\\\\\u53fa");
        assertEquals(ToStringUtil.toJavaString("x\n\r\b\f\t\"'\\\u53fa", true),
                "x\\n\\r\\b\\f\\t\\\"\\'\\\\\u53fa");
        assertEquals(ToStringUtil.toJavaString("x\n\r\b\f\t\"'\\\u53fa", false),
                "x\n\r\\b\f\t\\\"\\'\\\\\u53fa");
    }

    @Test
    public void testDisplayString() {
        checkDisplayString("null", null);
        checkDisplayString("'x'", 'x');
        checkDisplayString("'\\n'", '\n');
        checkDisplayString("\"x\\n\\r\\b\\f\\t\\\"\\'\\\\\u53fa\"",
                "x\n\r\b\f\t\"'\\\u53fa");
        checkDisplayString("15", 15);
        checkDisplayString("15L", 15L);
        checkDisplayString("1.5f", 1.5f);
        checkDisplayString("1.5", 1.5);

        long now = System.currentTimeMillis();
        assertEquals(ToStringUtil.displayString(now, true),
                ToStringUtil.toDateString(now));
        assertEquals(ToStringUtil.displayString(now, false), now + "L");
    }

    private void checkDisplayString(String expected, Object obj) {
        assertEquals(ToStringUtil.displayString(obj, true), expected);
        assertEquals(ToStringUtil.displayString(obj, false), expected);
    }

    @Test
    public void testToString() {
        class TClass {
            public int i = 1;
            public long l = 15;
            public float f = 15.3f;
            public double d = 15.3;
            public Object n = null;
            public String s = "hello";
            public Date epoch = new Date(0);

            @SuppressWarnings({"FieldMayBeStatic"})
            private final char c = '\b';
            private final long date = System.currentTimeMillis();

            public char getCharVal() {
                return c;
            }

            public long getDate() {
                return date;
            }
        }

        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                DateFormat.MEDIUM, Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        ToStringUtil.setDateFormat(format);

        TClass tobj = new TClass();
        String[] exp = {
                "charVal='\\b'", "d=15.3", "date=" + ToStringUtil.toDateString(
                        tobj.date), "epoch=Jan 1, 1970 12:00:00 AM", "f=15.3f",
                "i=1", "l=15L", "n=null", "s=\"hello\""
        };

        assertEquals(ToStringUtil.toString(tobj), StringUtils.join(exp, "\n"));
        assertEquals(ToStringUtil.toString(tobj, "|"), StringUtils.join(exp,
                "|"));

        class TSClass extends TClass {
            @Override
            public String toString() {
                return "TSClass:toString";
            }
        }

        assertEquals(ToStringUtil.toString(new TSClass()), "TSClass:toString");
        assertEquals(ToStringUtil.toString(new TSClass(), "|"),
                "TSClass:toString");
    }

    // Test that it is possible to sublcass this class
    static class CanExtend extends ToStringUtil {
    }

    @Test
    public void testExtensible() {
        assertNotNull(new CanExtend());
    }
}
