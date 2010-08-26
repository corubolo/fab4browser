package org.simplx.regex;

import org.simplx.regex.RegexUtils.Replacer;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRegexUtils {
    @Test
    public void testSimpleReplace() {
        assertEquals(RegexUtils.replaceAll(Pattern.compile("y"), "xyzzy", "q"),
                "xqzzq");
        assertEquals(RegexUtils.replaceAll(Pattern.compile("[a-my]"), "xyzzy",
                "$0!"), "xy!zzy!");
    }

    @Test
    public void testFunctionReplace() {
        Replacer replacer = new Replacer() {
            int i = 0;

            public String replace(Matcher m) {
                return "$0[" + (i++) + "]";
            }
        };
        assertEquals(RegexUtils.replaceAll(Pattern.compile("[dfy]"), "xyzzy",
                replacer), "xy[0]zzy[1]");
    }
}