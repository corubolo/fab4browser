package org.simplx.io;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;

import static org.simplx.io.ParseStringUtil.*;
import static org.simplx.io.ToStringUtil.*;
import static org.testng.Assert.*;

public class TestParseStringUtil {
    @Test(dataProvider = "goodCharTests")
    public void testGoodChars(char ch, String str, int pos, int next) {
        goodCharTest(ch, str, pos, next, "original");
        goodCharTest(ch, str + "!;,", pos, next, "suffixed");
        goodCharTest(ch, ",;!" + str, pos + 3, next + 3, "prefixed");
        goodCharTest(ch, ",;!" + str + "!;,", pos + 3, next + 3, "surrounded");
    }

    private void goodCharTest(char ch, String str, int pos, int next,
            String mark) {

        int[] nextPos = {-1};
        assertEquals(parseNextJavaChar(str, pos, nextPos), ch,
                "char returned: " + mark);
        assertEquals(nextPos[0], next, "next position: " + mark);

        // Make sure a null nextPos causes no problems
        assertEquals(ch, parseNextJavaChar(str, pos, null),
                "null nextPos: " + mark);

        assertEquals(parseNextChar(str, pos, nextPos), ch,
                "char returned: " + mark);
        assertEquals(nextPos[0], next, "next position: " + mark);

        // Make sure a null nextPos causes no problems
        assertEquals(ch, parseNextChar(str, pos, null),
                "null nextPos: " + mark);
    }

    @DataProvider(name = "goodCharTests")
    Object[][] goodCharTests() {
        return new Object[][]{        // character tests
                {'a', "a", 0, 1},           // simplest case
                {'b', "ab", 1, 2},          // pick out end
                {'–', "a–c", 1, 2},         // pick out middle
                {'\0', "\\0", 0, 2},        // single octal char
                {'\0', "\\00", 0, 3},       // longer octal
                {'\0', "\\000", 0, 4},      // longest octal
                {'\377', "\\377", 0, 4},    // largest octal
                {'\0', "\\08", 0, 2},       // octal stops at next char ('8')
                {'\0', "\\008", 0, 3},      // longer octal zero
                {'\0', "\\0008", 0, 4},     // longest octal zero
                {'\377', "\\3778", 0, 4},   // largest octal
                {'\\', "\\\\u", 0, 2},      // backslash escapes itself
                {'\'', "\\'u", 0, 2},       // backslash escapes single quote
                {'\"', "\\\"u", 0, 2},      // backslash escapes double quote
                {'\n', "\\nu", 0, 2},       // backslash for special
                {'\r', "\\ru", 0, 2},       // backslash for special
                {'\t', "\\tu", 0, 2},       // backslash for special
                {'\b', "\\bu", 0, 2},       // backslash for special
                {'\f', "\\fu", 0, 2},       // backslash for special
                {'\u934f', "\\u934f", 0, 6},        // simple unicode escape
                {'\u934f', "\\uuuuuu934f", 0, 11},  // multi-u unicode escape
        };
    }

    @Test(dataProvider = "exceptionCharTests")
    public void testExceptionChars(String str, int pos) {
        exceptionCharTest(str, pos, "original");
        exceptionCharTest(str, pos, "prefix");
    }

    private void exceptionCharTest(String str, int pos, String mark) {
        // Try it with nextPos
        try {
            char ch = parseNextJavaChar(str, pos, new int[1]);
            fail("returned '" + toJavaChar(ch) + "'");
        } catch (Exception e) {
            assertSame(e.getClass(), IllegalArgumentException.class,
                    "exception type: " + mark);
        }

        // Try it without nextPos
        try {
            char ch = parseNextJavaChar(str, pos, null);
            fail("returned '" + toJavaChar(ch) + "'");
        } catch (Exception e) {
            assertSame(e.getClass(), IllegalArgumentException.class,
                    "null nextPos exception type: " + mark);
        }
    }

    @DataProvider(name = "exceptionCharTests")
    Object[][] exceptionCharTests() {
        return new Object[][]{        // character tests
                {"", 0},            // simplest case
                {"\\", 0},          // incomplete escape
                {"\\a", 0},         // invalid escape
                {"\\400", 0},       // octal overflow
                {"\\u00", 0},       // short unicode
                {"\\uq000", 0},     // not unicode
                {"\\u0q00", 0},     // not unicode
                {"\\u00q0", 0},     // not unicode
                {"\\u000q", 0},     // not unicode
        };
    }

    @Test
    public void testNonstrict() {
        assertEquals(parseString("\\a\\t\\a"), "a\ta");
    }
}