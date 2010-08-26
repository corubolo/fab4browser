package org.simplx.regex;

import org.apache.commons.lang.StringUtils;
import org.simplx.io.TeeWriter;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRegexDebug {
    @Test
    public void testDigitCount() {
        assertEquals(RegexDebug.digitsFor(0), 1);
        assertEquals(RegexDebug.digitsFor(1), 1);
        assertEquals(RegexDebug.digitsFor(5), 1);
        assertEquals(RegexDebug.digitsFor(9), 1);
        assertEquals(RegexDebug.digitsFor(10), 2);
        assertEquals(RegexDebug.digitsFor(11), 2);
        assertEquals(RegexDebug.digitsFor(99), 2);
        assertEquals(RegexDebug.digitsFor(100), 3);
    }

    private static class TestDesc {
        final String pattern;
        final String str;
        final int count;
        final String[] expected;
        final String expectedStr;
        final Display displayer;

        TestDesc(Display displayer, String pattern, String str, int count,
                String... expected) {
            this.displayer = displayer;
            this.pattern = pattern;
            this.count = count;
            this.str = str;
            this.expected = expected;
            expectedStr = StringUtils.join(expected, "");
        }

        void execute() throws IOException {
            StringWriter sout = new StringWriter();
            Writer out = new TeeWriter(sout, new OutputStreamWriter(
                    System.out));

            RegexDebug rd = new RegexDebug(pattern);
            int found = displayer.display(rd, str, out);
            assertEquals(found, count);

            out.flush();
            assertEquals(sout.toString(), expectedStr);
        }

        void execute(int... groups) throws IOException {
            StringWriter sout = new StringWriter();
            Writer out = new TeeWriter(sout, new OutputStreamWriter(
                    System.out));

            BitSet show = new BitSet();
            boolean showAll = groups.length == 1 && groups[0] < 0;
            if (!showAll) {
                for (int i = 0; i < groups.length; i++) {
                    show.set(groups[i]);
                }
            }

            RegexDebug rd = new RegexDebug(pattern);
            int found = displayer.display(rd, str, out, groups);
            assertEquals(found, count);

            out.flush();
            StringBuilder sb = new StringBuilder();
            Pattern groupPattern = Pattern.compile("^ .* group\\s+([0-9]+)");
            for (String s : expected) {
                Matcher m = groupPattern.matcher(s);
                if (!m.find()) {
                    // not a line with groups
                    sb.append(s);
                } else {
                    if (showAll || show.get(Integer.parseInt(m.group(1)))) {
                        sb.append(s);
                    }
                }
            }
            assertEquals(sout.toString(), sb.toString());
        }
    }

    private interface Display {
        int display(RegexDebug rd, String str, Appendable out);

        int display(RegexDebug rd, String str, Appendable out, int... groups);
    }

    private static final Display DETAILS = new Display() {
        public int display(RegexDebug rd, String str, Appendable out) {
            return rd.displayMatchDetails(str, out);
        }

        public int display(RegexDebug rd, String str, Appendable out,
                int... groups) {
            return rd.displayMatchDetails(str, out, groups);
        }
    };
    private static final Display SUMMARY = new Display() {
        public int display(RegexDebug rd, String str, Appendable out) {
            return rd.displayMatches(str, out);
        }

        public int display(RegexDebug rd, String str, Appendable out,
                int... groups) {
            return rd.displayMatches(str, out, groups);
        }
    };

    private static final TestDesc MATCH_DETAILS_TEST = new TestDesc(DETAILS,
            "(x(y*))", "x xy xyyz xyyyyyyz q", 4, "\"x xy xyyz xyyyyyyz q\"\n",
            " |................... group 0:  0 -  1: \"x\"\n",
            " |................... group 1:  0 -  1: \"x\"\n",
            " ^................... group 2:  1 -  1: \"\"\n",
            "\"x xy xyyz xyyyyyyz q\"\n",
            " ..[]................ group 0:  2 -  4: \"xy\"\n",
            " ..[]................ group 1:  2 -  4: \"xy\"\n",
            " ...|................ group 2:  3 -  4: \"y\"\n",
            "\"x xy xyyz xyyyyyyz q\"\n",
            " .....[-]............ group 0:  5 -  8: \"xyy\"\n",
            " .....[-]............ group 1:  5 -  8: \"xyy\"\n",
            " ......[]............ group 2:  6 -  8: \"yy\"\n",
            "\"x xy xyyz xyyyyyyz q\"\n",
            " ..........[-----]... group 0: 10 - 17: \"xyyyyyy\"\n",
            " ..........[-----]... group 1: 10 - 17: \"xyyyyyy\"\n",
            " ...........[----]... group 2: 11 - 17: \"yyyyyy\"\n",
            "4 matches\n");
    private static final TestDesc EMPTY_MATCH_DETAILS_TEST = new TestDesc(
            DETAILS, "(\\bq{1,3})((z)|(w))?", "q qz qqqqwz yqz qwq -qq", 5,
            "\"q qz qqqqwz yqz qwq -qq\"\n",
            " |...................... group 0:  0 -  1: \"q\"\n",
            " |...................... group 1:  0 -  1: \"q\"\n",
            " ....................... group 2: Empty\n",
            " ....................... group 3: Empty\n",
            " ....................... group 4: Empty\n",
            "\"q qz qqqqwz yqz qwq -qq\"\n",
            " ..[]................... group 0:  2 -  4: \"qz\"\n",
            " ..|.................... group 1:  2 -  3: \"q\"\n",
            " ...|................... group 2:  3 -  4: \"z\"\n",
            " ...|................... group 3:  3 -  4: \"z\"\n",
            " ....................... group 4: Empty\n",
            "\"q qz qqqqwz yqz qwq -qq\"\n",
            " .....[-]............... group 0:  5 -  8: \"qqq\"\n",
            " .....[-]............... group 1:  5 -  8: \"qqq\"\n",
            " ....................... group 2: Empty\n",
            " ....................... group 3: Empty\n",
            " ....................... group 4: Empty\n",
            "\"q qz qqqqwz yqz qwq -qq\"\n",
            " ................[]..... group 0: 16 - 18: \"qw\"\n",
            " ................|...... group 1: 16 - 17: \"q\"\n",
            " .................|..... group 2: 17 - 18: \"w\"\n",
            " ....................... group 3: Empty\n",
            " .................|..... group 4: 17 - 18: \"w\"\n",
            "\"q qz qqqqwz yqz qwq -qq\"\n",
            " .....................[] group 0: 21 - 23: \"qq\"\n",
            " .....................[] group 1: 21 - 23: \"qq\"\n",
            " ....................... group 2: Empty\n",
            " ....................... group 3: Empty\n",
            " ....................... group 4: Empty\n", "5 matches\n");
    private static final TestDesc NO_MATCH_DETAILS_TEST = new TestDesc(DETAILS,
            "(x(y*))", "qwerty", 0, "\"qwerty\"\n", "0 matches\n");
    private static final TestDesc MATCHES_TEST = new TestDesc(SUMMARY,
            "(x(y*))", "x xy xyyz xyyyyyyz q", 4, "\"x xy xyyz xyyyyyyz q\"\n",
            " |.[].[-]..[-----]... group 0:  0 -  1: Empties: 0\n",
            " |.[].[-]..[-----]... group 1:  0 -  1: Empties: 0\n",
            " ^..|..[]...[----]... group 2:  1 -  1: Empties: 0\n",
            "4 matches\n");
    private static final TestDesc EMPTY_MATCHES_TEST = new TestDesc(SUMMARY,
            "(\\bq{1,3})((z)|(w))?", "q qz qqqqwz yqz qwq -qq", 5,
            "\"q qz qqqqwz yqz qwq -qq\"\n",
            " |.[].[-]........[]...[] group 0:  0 -  1: Empties: 0\n",
            " |.|..[-]........|....[] group 1:  0 -  1: Empties: 0\n",
            " ...|.............|..... group 2:  3 -  4: Empties: 3\n",
            " ...|................... group 3:  3 -  4: Empties: 4\n",
            " .................|..... group 4: 17 - 18: Empties: 4\n",
            "5 matches\n");
    private static final TestDesc NO_MATCHES_TEST = new TestDesc(SUMMARY,
            "(x(y*))", "qwerty", 0, "\"qwerty\"\n", "0 matches\n");

    @Test
    public void testMatchDetails() throws IOException {
        MATCH_DETAILS_TEST.execute();
    }

    @Test
    public void testMatchDetailsSubset() throws IOException {
        testSubsets(MATCH_DETAILS_TEST);
    }

    @Test
    public void testEmptyMatchDetails() throws IOException {
        EMPTY_MATCH_DETAILS_TEST.execute();
    }

    @Test
    public void testEmptyMatchDetailsSubset() throws IOException {
        testSubsets(EMPTY_MATCH_DETAILS_TEST);
    }

    @Test
    public void testNoMatchDetails() throws IOException {
        NO_MATCH_DETAILS_TEST.execute();
    }

    @Test
    public void testNoMatchDetailsSubset() throws IOException {
        testSubsets(NO_MATCH_DETAILS_TEST);
    }

    @Test
    public void testMatches() throws IOException {
        MATCHES_TEST.execute();
    }

    @Test
    public void testMatchesSubset() throws IOException {
        testSubsets(MATCHES_TEST);
    }

    @Test
    public void testEmptyMatches() throws IOException {
        EMPTY_MATCHES_TEST.execute();
    }

    @Test
    public void testEmptyMatchesSubset() throws IOException {
        testSubsets(EMPTY_MATCHES_TEST);
    }

    @Test
    public void testNoMatches() throws IOException {
        NO_MATCHES_TEST.execute();
    }

    @Test
    public void testNoMatchesSubset() throws IOException {
        testSubsets(NO_MATCHES_TEST);
    }

    private void testSubsets(TestDesc test) throws IOException {
        test.execute(0);
        test.execute(1);
        test.execute(2);
        test.execute(0, 1);
        test.execute(0, 2);
        test.execute(1, 2);
        test.execute(1, 2, 3);
        test.execute(-1);
    }
}