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

package org.simplx.regex;

import java.io.StringWriter;
import java.util.BitSet;
import java.util.Formatter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** This class supports debugging help for regular expressions. */
public class RegexDebug {
    private final Pattern pattern;
    private int groupCount;
    private static final Pattern LINE_END = Pattern.compile("[\n\r]+");

    /**
     * Creates a new {@link RegexDebug} with the given pattern.
     *
     * @param pattern The pattern that will be compiled and used.
     */
    public RegexDebug(String pattern) {
        this(Pattern.compile(pattern));
    }

    /**
     * Creates a new {@link RegexDebug} with the given pattern.
     *
     * @param pattern The pattern that will be used.
     */
    public RegexDebug(Pattern pattern) {
        this.pattern = pattern;
        groupCount = -1;
    }

    /**
     * Displays all groups for every match for the given string.  See {@link
     * #displayMatchDetails(CharSequence,Formatter,int...)} for more
     * information.
     *
     * @param str The string to match against the pattern.
     * @param out Where to put the output.  This will be wrapped in a {@link
     *            Formatter} object and passed to {@link #displayMatches(CharSequence,Formatter)}.
     *
     * @return The number of matches.
     *
     * @see #displayMatchDetails(CharSequence,Formatter,int...)
     * @see #displayMatches(CharSequence,Appendable)
     */
    public int displayMatchDetails(CharSequence str, Appendable out) {
        return displayMatchDetails(str, new Formatter(out));
    }

    /**
     * Displays every match for the given string, showing the results for
     * specific groups. If the group list has only the value -1, displays all
     * groups (just like {@link #displayMatchDetails(CharSequence,Appendable)}.
     * See {@link #displayMatchDetails(CharSequence,Formatter,int...)} for more
     * information.
     *
     * @param str    The string to match against the pattern.
     * @param out    Where to put the output.  This will be wrapped in a {@link
     *               Formatter} object and passed to {@link #displayMatches(CharSequence,Formatter)}.
     * @param groups List of groups whose information will be displayed.  If
     *               only -1 is given, all groups will be displayed.
     *
     * @return The number of matches.
     *
     * @see #displayMatchDetails(CharSequence,Formatter,int...)
     * @see #displayMatches(CharSequence,Appendable,int...)
     */
    public int displayMatchDetails(CharSequence str, Appendable out,
            int... groups) {

        return displayMatchDetails(str, new Formatter(out), groups);
    }

    /**
     * Displays all groups for every match for the given string.  See {@link
     * #displayMatchDetails(CharSequence,Formatter,int...)} for more
     * information.
     *
     * @param str       The string to match against the pattern.
     * @param formatter The formatter to use to print the results.
     *
     * @return The number of matches.
     *
     * @see #displayMatchDetails(CharSequence,Formatter,int...)
     * @see #displayMatches(CharSequence,Formatter)
     */
    public int displayMatchDetails(CharSequence str, Formatter formatter) {
        return displayMatchDetails(str, formatter, -1);
    }

    /**
     * Displays every match for the given string.  The string is matched against
     * the pattern.  For each match, the input string is printed, as well as
     * markers to show the results for each group in that match.  (Group 0 is
     * the overall match.)
     * <p/>
     * In the output a {@code |} is used to show the start and end points of a
     * group (the actual end is marked, not the "one-past-the-end" value that is
     * actually returned by the matcher; this is clearer visually).  If the
     * start and end are the same, only one {@code |} is shown. If a group
     * matches an empty string, a {@code ^} is used to show where that was
     * placed.  If the group did not mactch at all (the group is returned as
     * {@code null} the row for that group is labelled "Empty". "Empties".
     * <p/>
     * After all the matches are shown, the number of matches is printed.
     *
     * @param str       The string to match against the pattern.
     * @param formatter The formatter to use to print the results.
     * @param groups    List of groups whose information will be displayed.  If
     *                  only -1 is given, all groups will be displayed.
     *
     * @return The number of matches.
     *
     * @see #displayMatches(CharSequence,Formatter,int...)
     */
    public int displayMatchDetails(CharSequence str, Formatter formatter,
            int... groups) {

        BitSet show = new BitSet();
        boolean showAll = groups.length == 1 && groups[0] < 0;
        if (!showAll) {
            for (int group : groups) {
                show.set(group);
            }
        }

        Matcher m = pattern.matcher(str);
        String tagFmt = null, emptyFmt = null;
        int matchCount;
        for (matchCount = 0; m.find(); matchCount++) {
            if (tagFmt == null) {
                int digits = digitsFor(str.length());
                String groupFmt = "group %" + digitsFor(getGroupCount(m)) +
                        "d: ";
                tagFmt = groupFmt + "%" + digits + "d - %" + digits +
                        "d: \"%s\"";
                emptyFmt = groupFmt + "Empty";
            }
            showStr(str, formatter);
            StringBuilder sb = new StringBuilder();
            for (int g = 0; g <= m.groupCount(); g++) {
                if (!showAll && !show.get(g)) {
                    continue;
                }

                sb.delete(0, sb.length());
                int start = m.start(g);
                int end = m.end(g) - 1;
                char marker;
                if (start < 0) {
                    start = end = 0;
                    marker = '.';
                } else if (start < end) {
                    marker = '[';
                } else if (start == end) {
                    marker = '|';
                } else {
                    start = end;
                    marker = '^';
                }
                sb.append(' ');
                for (int i = 0; i < start; i++) {
                    sb.append('.');
                }
                sb.append(marker);
                for (int i = start + 1; i < end; i++) {
                    sb.append('-');
                }
                if (start < end) {
                    sb.append(']');
                }
                for (int i = end + 1; i < str.length(); i++) {
                    sb.append('.');
                }
                formatter.format("%s ", sb);
                if (m.start(g) < 0) {
                    formatter.format(emptyFmt, g);
                } else {
                    formatter.format(tagFmt, g, m.start(g), m.end(g), m.group(
                            g));
                }
                formatter.format("%n");
            }
        }
        if (matchCount == 0) {
            showStr(str, formatter);
        }
        formatter.format("%d matches%n", matchCount);
        return matchCount;
    }

    private int getGroupCount(Matcher m) {
        if (groupCount < 0) {
            groupCount = m.groupCount();
        }
        return groupCount;
    }

    /**
     * Displays the combined matches in all groups for the given string.  This
     * is a summary of the data shown by {@link #displayMatchDetails(CharSequence,Appendable)
     * displayMatchDetails}. See {@link #displayMatches(CharSequence,Formatter,int...)}
     * for more information.
     *
     * @param str The string to match against the pattern.
     * @param out Where to put the output.  This will be wrapped in a {@link
     *            Formatter} object and passed to {@link #displayMatches(CharSequence,Formatter)}.
     *
     * @return The number of matches.
     *
     * @see #displayMatches(CharSequence,Formatter,int...)
     * @see #displayMatchDetails(CharSequence,Appendable)
     */
    public int displayMatches(CharSequence str, Appendable out) {
        return displayMatches(str, out, -1);
    }

    /**
     * Displays the combined matches for the given string, showing the results
     * for specific groups. This is a summary of the data shown by {@link
     * #displayMatchDetails(CharSequence,Appendable,int...)
     * displayMatchDetails}.  If the group list has only the value -1, displays
     * all groups (just like {@link #displayMatches(CharSequence,Appendable)}.
     * See {@link #displayMatches(CharSequence,Formatter,int...)} for more
     * information.
     *
     * @param str    The string to match against the pattern.
     * @param out    Where to put the output.  This will be wrapped in a {@link
     *               Formatter} object and passed to {@link #displayMatches(CharSequence,Formatter)}.
     * @param groups List of groups whose information will be displayed.  If
     *               only -1 is given, all groups will be displayed.
     *
     * @return The number of matches.
     *
     * @see #displayMatches(CharSequence,Formatter,int...)
     * @see #displayMatchDetails(CharSequence,Appendable,int...)
     */
    public int displayMatches(CharSequence str, Appendable out, int... groups) {
        return displayMatches(str, new Formatter(out), groups);
    }

    /**
     * Displays the combined matches in all groups for the given string.  This
     * is a summary of the data shown by {@link #displayMatchDetails(CharSequence,Appendable)
     * displayMatchDetails}. See {@link #displayMatches(CharSequence,Formatter,int...)}
     * for more information.
     *
     * @param str       The string to match against the pattern.
     * @param formatter The formatter to use to print the results.
     *
     * @return The number of matches.
     *
     * @see #displayMatches(CharSequence,Formatter,int...)
     * @see #displayMatchDetails(CharSequence,Appendable)
     */
    public int displayMatches(CharSequence str, Formatter formatter) {
        return displayMatches(str, formatter, -1);
    }

    /**
     * Displays the combined matches for the given string.  The string is
     * matched against the pattern.  The display starts with the string, and
     * then has a single row for each displayed group that shows all the matches
     * of that group in the string.  (Group 0 is the overall match.)
     * <p/>
     * In the output a {@code |} is used to show the start and end points of a
     * group (the actual end is marked, not the "one-past-the-end" value that is
     * actually returned by the matcher; this is clearer visually).  If the
     * start and end are the same, only one {@code |} is shown. If a group
     * matches an empty string, a {@code ^} is used to show where that was
     * placed.  The number of times the group did not mactch at all (the times
     * when the group is returned as {@code null} is shown as a number of
     * "Empties".
     * <p/>
     * After all the matches are shown, the number of matches is printed.
     *
     * @param str       The string to match against the pattern.
     * @param formatter The formatter to use to print the results.
     * @param groups    List of groups whose information will be displayed.  If
     *                  only -1 is given, all groups will be displayed.
     *
     * @return The number of matches.
     *
     * @see #displayMatchDetails(CharSequence,Formatter,int...)
     */
    public int displayMatches(CharSequence str, Formatter formatter,
            int... groups) {

        StringWriter sout = new StringWriter();
        int count = displayMatchDetails(str, sout, groups);
        if (count == 0) {
            formatter.format("%s", sout.toString());
            return count;
        }

        // Now we can know the group count
        if (groups.length == 1 && groups[0] < 0) {
            groups = new int[getGroupCount(null) + 1];
            for (int i = 0; i < groups.length; i++) {
                groups[i] = i;
            }
        }

        StringBuilder sb = new StringBuilder("(");
        for (int group : groups) {
            if (sb.length() > 1) {
                sb.append("|");
            }
            sb.append(group);
        }
        sb.append(")");
        String groupList = sb.toString();
        Pattern keepers = Pattern.compile(
                " group\\s+" + groupList + ": (Empty)?");
        String[] rawLines = LINE_END.split(sout.toString());

        Map<Integer, StringBuilder> displays =
                new TreeMap<Integer, StringBuilder>();
        Map<Integer, Integer> empties = new TreeMap<Integer, Integer>();

        for (int i = 1; i < rawLines.length - 1; i++) {
            String line = rawLines[i];
            Matcher m = keepers.matcher(line);
            if (m.find()) {
                int group = Integer.parseInt(m.group(1));
                if (m.start(2) > 0) {
                    if (!empties.containsKey(group)) {
                        empties.put(group, 1);
                    } else {
                        empties.put(group, empties.get(group) + 1);
                    }
                } else {
                    if (!displays.containsKey(group)) {
                        displays.put(group, new StringBuilder(line));
                    } else {
                        sb = displays.get(group);
                        for (int j = 0; j < m.start(); j++) {
                            char ch = line.charAt(j);
                            if (ch != ' ' && ch != '.') {
                                sb.setCharAt(j, ch);
                            }
                        }
                    }
                }
            }
        }

        // The first line is the string
        formatter.format("%s%n", rawLines[0]);

        // The lines in the middle are the groups
        for (Entry<Integer, StringBuilder> entry : displays.entrySet()) {
            sb = entry.getValue();
            int empty = empties.containsKey(entry.getKey()) ? empties.get(
                    entry.getKey()) : 0;
            int pos = sb.lastIndexOf(":");
            sb.replace(pos + 2, sb.length(), "Empties: ").append(empty);
            formatter.format("%s%n", sb);
        }

        // The last line is the count
        formatter.format("%s%n", rawLines[rawLines.length - 1]);
        return count;
    }

    private static void showStr(CharSequence str, Formatter formatter) {
        formatter.format("\"%s\"%n", str);
    }

    /**
     * Returns the number of digits required to display the given number.
     *
     * @param num The number.
     */
    public static int digitsFor(int num) {
        int digits;
        for (digits = 1; num >= 10; digits++) {
            num /= 10;
        }
        return digits;
    }
}