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

/*
 * Parts of this code are:
 * Copyright 2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the Sun Community
 * Source License v 3.0/Jini Technology Specific Attachment v1.0
 * (the "License"). You may not use this file except in compliance
 * with the License. You may obtain a copy of the License
 * at http://www.sun.com/jini/ . Software distributed under the
 * License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the
 * License.
 *
 * The Reference Code is Jini Technology Core Platform code, v
 * 1.2. The Developer of the Reference Code is Sun Microsystems,
 * Inc.
 *
 * Contributor(s): Sun Microsystems, Inc.
 *
 * The contents of this file comply with the Jini Technology Core
 * Platform Compatibility Kit, v 1.2A.
 *
 * Tester(s): Sun Microsystems, Inc.
 * Java 2 SDK, Standard Edition, Version 1.4.0 and Version 1.3.1_02 for Solaris SPARC/x86
 * Java 2 SDK, Standard Edition, Version 1.4.0 and Version 1.3.1_02 for Linux (Intel x86)
 * Java 2 SDK, Standard Edition, Version 1.4.0 and Version 1.3.1_02 for Microsoft Windows
 *
 */
// Note: The license allows this use  -arnold
package org.simplx.args;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.simplx.logging.SimplxLogging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class parses a command line that uses multi-character options, such as
 * {@code -verbose} or {@code -help}.
 * <p/>
 * To use {@code MainArgs}, create a {@code MainArgs} object with the array of
 * strings you wish to parse (typically the array passed to the program's {@code
 * main} method), and then consume options from it, providing default values in
 * case the option is not specified by the user. When you have consumed all the
 * options, you invoke the {@code MainArgs} object's {@link #getOperands} method
 * to return the remaining operands on the command line. If ``{@code --}'' is
 * specified it is neither an option nor an operand, just a separator between
 * the two lists. The {@link CommandLineException} is used to signal errors in
 * the construction of the strings, that is, a user error, such as specifying a
 * option that takes an argument but forgetting to provide that argument.
 * <p/>
 * Here is an example:
 * <pre>
 * public static void main(String[] args) throws IOException {
 *      MainArgs line = new MainArgs("flicker", args);
 *      try {
 *          verbose = line.getBoolean("verbose");
 *          max = line.getInt("max", Integer.MAX_VALUE);
 *          Writer out = line.getWriter("out", (Writer) null);
 *          if (out != null)
 *              recordOut = new PrintWriter(out);
 *          String[] files = line.getOperands();
 *          for (String file : files) {
 *              flick(file);
 *          }
 *      } catch (HelpOnlyException e) {
 *          System.exit(0);
 *      } finally {
 *          if (recordOut != null)
 *              recordOut.close();
 *      }
 * }
 * </pre>
 * This program has three possible options: <ul> <li>"verbose", which is a
 * boolean that says whether to generate extra output. The field {@code verbose}
 * will store whether this option was specified or not. <li>"max", which is the
 * maximum number of lines to process in each file. If no maximum is specified,
 * then the maximum will be the largest possible integer, which in effect means
 * "no maximum". The field {@code max} will store this value. <li>"out", which
 * names a file that will be used to record what happens. The field {@code
 * recordOut} will be {@code null} if this is not specified, or a {@code
 * PrintWriter} to that file if it is. </ul> After any options, the arguments
 * will contain files that need to be processed.
 * <p/>
 * So here is a possible invocation of the program:
 * <pre>
 * flicker -verbose -out history f1 f2 f3
 * </pre>
 * In this case, after processing the arguments, the {@code verbose} field will
 * be {@code true}, there will be no maximum, and records will be made to the
 * file "history".  The operation will be run on three files: f1, f2, and f3.
 * <p/>
 * The following invocation will print out the usage:
 * <pre>
 * flicker -help
 * </pre>
 * This will print
 * <pre>
 * flicker [-verbose] [-max int] [-out file] [-help] file ...
 * </pre>
 * <p/>
 * You must call {@link #getOperands} for proper behavior, even if you do not
 * use any operands in your command. {@code getOperands} checks for several user
 * errors, including unknown options. If you do not expect to use operands, you
 * should check the return value of {@code getOperands} and complain if any are
 * specified.
 * <p/>
 * The order that the options are gathered by "get" calls is not relevant.  You
 * can order them in any way that makes sense to you.
 * <p/>
 * The order that the user puts the options in is only relevant when the same
 * option is fetched more than once.  In this case, the first invocation will
 * return the first value for the option, the second the second value, and so
 * on.  If you ask for the value of the option more times than the user provided
 * it, your extra requests will act as if the option is not specified.  (After
 * all, it wasn't specified the third, fourth, and fifth times, for example.)
 * You can use this to gather options in a loop, such as:
 * <p/>
 * In other words, "-verbose -out file" and "-out file -verbose" are the same.
 * But "-user pat -user robin" and "-user robin -user pat" are not.
 * <p/>
 * You can use multiple invocations to set a verbosity level:
 * <pre>
 *    MainArgs line = new MainArgs(args);
 *    int verbosity = 0;
 *    while (line.getBoolean("verbose"))
 *        verbosity++;
 * </pre>
 * or to collect a list of specifications:
 * <pre>
 *    MainArgs line = new MainArgs(args);
 *    List<String> users = new ArrayList<String>();
 *    String user;
 *    while ((user = line.getString("user", null)) != null)
 *        users.add(user);
 * </pre>
 * No options can be consumed after {@code getOperands} is invoked. Failure to
 * follow this rule is a programmer error that will result in an {@link
 * IllegalStateException}.
 * <p/>
 * {@code MainArgs} provides you several methods to get I/O streams from the
 * command line. If these do not suffice for your particular needs, you can get
 * the argument as a {@code String} and do your own processing.
 * <p/>
 * <h3>Combined Multiple and Single Charater Options</h3>
 * <p/>
 * Many programs want to allow the user to specify single character shortcuts
 * for the most common options.  This class handles that by allowing the option
 * specification to have both, separated by a {@code |} character.  For example,
 * <tt>getBoolean("v|verbose")</tt> means that the verbose option can be
 * specified as either {@code -v} or {@code --verbose}.  (In such commands,
 * {@code "--"} is used for the multi-char version of the option.)
 * <p/>
 * If an option does not have a single-character version, you can simply leave
 * out that part of the option specification: {@code "|version"} means that the
 * option {@code --version} has no single character equivalent.  Similary,
 * {@code "v|"} means that there is no multi-character version of the {@code -v}
 * option.  (Single-character-only options are painful to the user and
 * unnecessary, because there is an unlimited number of choices for
 * multi-character equivalents.)
 * <p/>
 * Single character options can be combined in a shorter form.  If the command
 * has single character options {@code x}, {@code y}, and {@code z}, you can
 * specify all three together, as in {@code -xyz}, which is equivalent to {@code
 * -x -y -z}.  If an option takes a parameter, you can merge it in as well.  If
 * the command also had a {@code o} option for an output file, you could say
 * {@code -xyzofile}, {@code -xyzo file}, {@code -xyz -ofile}, or {@code -xyz -o
 * file}.
 * <p/>
 * <h3>Usage Descriptions</h3>
 * <p/>
 * When you ask for the value of options, the object builds up knowledge of the
 * expected usage.  For example, when you call <tt>getBoolean("verbose")</tt>,
 * the class knows there is a boolean option named "verbose".  From this kind of
 * information, you can get a usage message for the user.
 *
 * @author Ken Arnold
 * @see StringTokenizer
 */
public class MainArgs {
    private static final Logger logger = SimplxLogging.loggerFor(
            MainArgs.class);

    /** The args provided. */
    private final String[] args;

    /** The arguments ones have been used. */
    private final BitSet used;

    /** The list of known options for the usage message. */
    private final List<Opt> options;

    /** The program name (if specified). */
    private final String programName;

    /** This command line specifies single character equivalents. */
    private boolean hasSingles;

    /** The current group name for options. */
    private int curGroup;

    /** The list of option group names. */
    private final List<String> groups;

    /** Description of the overall program. */
    private String[] programDesc = new String[0];

    /** Has some description text. */
    private boolean hasDescs;

    /** The operands have been fetched via getOperands(). */
    private boolean operandsFetched;

    /** The operands description. */
    private String[] operandsDesc;

    /** Whether we should accept enum abbreviations. */
    private boolean abbreviatedEnums = true;

    private int nextOptOrderNum = 0;

    private static final Pattern STRIP_NAME_PATTERN = Pattern.compile("_");

    // I wouldn't do this stateful stuff if I could return more than one
    // value from a method -- it didn't seem worth creating a new object
    // to hold the necessary values on each call to findOpt(). So I've
    // ensured that only one parsing method can be executing at a time
    // and "returned" values via this side effect. YUCK!
    private int foundStr;                   // found in which string
    private String foundOpt;                // which String was found

    /**
     * Creates a new {@link MainArgs} object that will return specified options,
     * arguments, and operands. The program name will be the simple name of the
     * class, that is, the class name with the package name stripped off.
     *
     * @param mainClass The class that has the {@code main} method.
     * @param args      The command line arguments.
     *
     * @see #MainArgs(String,String...)
     */
    public MainArgs(Class mainClass, String... args) {
        this(mainClass.getSimpleName(), args);
    }

    /**
     * Creates a new {@link MainArgs} object that will return specified options,
     * arguments, and operands. The {@code prog} parameter is the program name.
     *
     * @param programName The name to use for the program.
     * @param args        The command line arguments.
     */
    public MainArgs(String programName, String... args) {
        if (logger.isLoggable(Level.FINE)) {
            SimplxLogging.logFormat(logger, Level.FINE, "MainArgs(%s)",
                    programName);
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                SimplxLogging.logFormat(logger, Level.FINE, "    %d: %s", i,
                        arg);
            }
        }

        this.programName = programName;
        this.args = args.clone();
        used = new BitSet(args.length);
        options = new ArrayList<Opt>();
        curGroup = 0;
        groups = new ArrayList<String>();
        groups.add("");
    }

    /**
     * Add descriptive text for the program itself.  This will be included in
     * the usage message.
     *
     * @param desc The desriptive text.  Each string will be shown on a line of
     *             its own.
     */
    public void programDescription(String... desc) {
        programDesc = desc.clone();
        hasDescs |= desc.length > 0;
    }

    /** Used to store known option types so we can generate a usage message. */
    private class Opt {
        /** This particular option has no single-char equivalent. */
        static final char HAS_NO_SINGLE = '\uffff';

        /** The option. */
        final String multi;

        /** The single char of it. */
        final char single;

        /** The argument type. */
        final String argType;

        /** Option can be specified more than once. */
        boolean repeatable;

        final String paramName;
        final String[] desc;

        final int group;
        final int order;

        Opt(String option, String argType, String... doc) {
            this.argType = argType;
            int or = option.indexOf('|');
            if (or < 0) {
                single = HAS_NO_SINGLE;
                multi = option;
            } else if (or == 0) {
                single = HAS_NO_SINGLE;
                hasSingles =
                        true;  // this has no singles, but singles are specified
                multi = option.substring(1);
            } else if (or == 1) {
                hasSingles = true;
                single = option.charAt(or - 1);
                multi = option.substring(2);
            } else {
                throw new IllegalArgumentException(
                        "'|' at illegal position in \"" + option + '"');
            }

            group = curGroup;
            order = nextOptOrderNum++;
            if (argType == null) {
                paramName = "";
                desc = doc.clone();
            } else {
                paramName = nullToEmpty(doc.length > 0 ? doc[0] : argType);
                desc = (String[]) ArrayUtils.subarray(doc, 1, doc.length);
            }
            hasDescs |= desc.length > 0;
        }

        boolean matches(String arg) {
            if (arg.charAt(0) != '-') {
                return false;
            }

            int dashLen = 1;
            if (hasSingles) {
                if (arg.length() == 2 && arg.charAt(1) == single) {
                    return true;
                }
                if (arg.charAt(1) != '-') {
                    return false;
                }
                dashLen = 2;
            }
            return arg.length() - dashLen == multi.length() &&
                    arg.regionMatches(dashLen, multi, 0, multi.length());
        }

        String helpString(String prefix) {
            StringBuilder sb = new StringBuilder();

            sb.append(prefix);

            toString(sb);

            if (desc.length > 0) {
                String spaces = prefix + StringUtils.repeat(" ", 16);
                if (sb.length() < spaces.length()) {
                    sb.append(spaces.substring(sb.length()));
                }
                sb.append(desc[0]);
                for (int i = 1; i < desc.length; i++) {
                    sb.append('\n');
                    sb.append(prefix).append(desc[i]);
                }
            }
            sb.append('\n');

            return sb.toString();
        }

        private void toString(StringBuilder sb) {
            if (!hasSingles) {
                sb.append('-').append(multi);
            } else {
                if (single != HAS_NO_SINGLE) {
                    sb.append('-').append(single).append(", ");
                }
                sb.append("--").append(multi);
            }

            if (argType != null && argType.length() != 0) {
                sb.append(' ').append(paramName);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Sets the current option group.  The option group name is used for usage
     * messages. When you ask for the value of an option, that option is placed
     * in the current option group. In a usage message, options will be shown by
     * group, and within a group, in the order they are requested.  You can set
     * the option group back to one used previously.
     *
     * @param groupName The name of the current option group.
     */
    public void optionGroup(String groupName) {
        curGroup = groups.indexOf(groupName);
        if (curGroup < 0) {
            groups.add(groupName);
            curGroup = groups.size() - 1;
        }
        hasDescs |= groups.size() > 0;
    }

    /**
     * Returns {@code true} if the given option is specified on the command
     * line.
     *
     * @param option The name of the option.
     * @param doc    Usage documentation for the option.  Each string will be
     *               printed on its own line, exept the first which will be put
     *               on the same line as the option itself if it fits.
     *
     * @return {@code true} if the option is specified; otherwise {@code
     *         false}.
     */
    public boolean getBoolean(String option, String... doc) {
        Opt o = addOpt(option, null, doc);
        boolean retval = false;
        if (findOpt(o, option)) {
            retval = true;
        }
        return retval;
    }

    /**
     * Returns the argument for the given option. This is a workhorse routine
     * shared by all the methods that get options with arguments.
     *
     * @param option The name of the option.
     * @param type   The type of the option for the usage message.
     * @param doc    Usage documentation for the option.
     *
     * @return If the option has been specified and is still unused, returns the
     *         argument for the option; otherwise return {@code null}.
     *
     * @throws CommandLineException No argument was present.
     */
    private String getArgument(String option, String type, String... doc)
            throws CommandLineException {

        Opt o = addOpt(option, type, doc);
        if (findOpt(o, option)) {
            return optArg();
        }
        return null;
    }

    /**
     * Returns the argument of the given string option from the command line. If
     * the option is not specified, return {@code defaultValue}.
     *
     * @param option       The name of the option.
     * @param defaultValue The value to return if the option is not specified.
     * @param doc          Usage documentation for the option.  The first string
     *                     will be the name of the parameter in the usage.  If
     *                     there are any other strings, each string will be
     *                     printed on its own line, exept the first which will
     *                     be put on the same line as the option itself if it
     *                     fits.
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws CommandLineException No argument was present.
     */
    public String getString(String option, String defaultValue, String... doc)
            throws CommandLineException {

        String str = getArgument(option, "str", doc);
        return str != null ? str : defaultValue;
    }

    /**
     * Returns the argument of the given {@code int} option from the command
     * line. If the option is not specified, return {@code defaultValue}. The
     * number is parsed according to {@link Integer#parseInt(String)}, except
     * that a leading plus sign is accepted.
     *
     * @param option       The name of the option.
     * @param defaultValue The value to return if the option is not specified.
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...) getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws NumberFormatException The argument is not a valid number.
     * @throws CommandLineException  No argument was present.
     */
    public int getInt(String option, int defaultValue, String... doc)
            throws CommandLineException, NumberFormatException {

        String str = getArgument(option, "int", doc);
        try {
            if (str == null) {
                return defaultValue;
            }
            // ignore leading plus
            if (str.length() > 0 && str.charAt(0) == '+') {
                str = str.substring(1);
            }
            return Integer.decode(str);
        } catch (NumberFormatException e) {
            throw numException(e, option);
        }
    }

    /**
     * Returns the argument of the given {@code long} option from the command
     * line. If the option is not specified, return {@code defaultValue}. The
     * number is parsed according to {@link Long#decode(String)}, except that a
     * leading plus sign is accepted.
     *
     * @param option       The option specification.
     * @param defaultValue The value to return if the option is not specified.
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws NumberFormatException The argument is not a valid number.
     * @throws CommandLineException  No argument was present.
     */
    public long getLong(String option, long defaultValue, String... doc)
            throws CommandLineException, NumberFormatException {

        String str = getArgument(option, "long", doc);
        try {
            if (str == null) {
                return defaultValue;
            }
            // ignore leading plus
            if (str.length() > 0 && str.charAt(0) == '+') {
                str = str.substring(1);
            }
            return Long.decode(str);
        } catch (NumberFormatException e) {
            throw numException(e, option);
        }
    }

    /**
     * Returns the value of the given {@code double} from the command line. If
     * the option is not specified, return {@code defaultValue}. The number is
     * parsed according to {@link Double#valueOf(String)}.
     *
     * @param option       The option specification.
     * @param defaultValue The value to return if the option is not specified.
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws NumberFormatException The argument is not a valid number.
     * @throws CommandLineException  No argument was present.
     */
    public double getDouble(String option, double defaultValue, String... doc)
            throws CommandLineException, NumberFormatException {

        String str = getArgument(option, "val", doc);
        try {
            if (str == null) {
                return defaultValue;
            }
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            throw numException(e, option);
        }
    }

    /**
     * Returns <tt>true</tt> if this object accepts abbreviations for enum
     * values.
     *
     * @return <tt>true</tt> if this object accepts abbreviations for enum
     *         values.
     */
    public boolean isAbbreviatedEnums() {
        return abbreviatedEnums;
    }

    /**
     * Sets whether this object accepts abbreviations for enum values.  By
     * default, this is <tt>true</tt>.
     * <p/>
     * Abbreviations are any string that matches exactly one of the possible
     * values.
     *
     * @param abbreviatedEnums <tt>true</tt> if this object should accept
     *                         abbreviations for enum values.
     */
    public void setAbbreviatedEnums(boolean abbreviatedEnums) {
        this.abbreviatedEnums = abbreviatedEnums;
    }

    /**
     * Returns a value from an enum class.  The enum is the one that contains
     * the default value.  The default value cannot be <tt>null</tt>.  If you
     * need to have a <tt>null</tt> default, use {@link #getEnumValue(String,Enum,Class,String...)}.
     * <p/>
     * See {@link #getEnumValue(String,Enum,Class,String...)} for a description
     * of how the argument is processed.
     *
     * @param option       The option.
     * @param defaultValue The enum value to use if the option is not
     *                     specified.
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...)} getString}).
     * @param <T>          The type of the enum; derived from {@code
     *                     defaultValue}.
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @see #getEnumValue(String, Enum, Class, String...)
     */
    public <T extends Enum<T>> T getEnumValue(String option, T defaultValue,
            String... doc) {

        Class<T> enumClass = defaultValue.getDeclaringClass();
        return getEnumValue(option, defaultValue, enumClass, doc);
    }

    /**
     * Returns a value selected from an enum class.  The argument string names
     * an element of the enum.  The string must match one element name in the
     * enum. An abbreviation of any unique starting string is accepted by
     * default. For example, if the enum had elements <tt>MIN</tt>,
     * <tt>AVG</tt>, and <tt>MAX</tt>, the user could use any of <tt>a</tt>,
     * <tt>av</tt>, or <tt>avg</tt> to match <tt>AVG</tt>.  But <tt>m</tt> would
     * cause an error since it could match either <tt>MIN</tt> or <tt>MAX</tt>,
     * whose shortest abbreviations are <tt>mi</tt> and <tt>ma</tt>,
     * respectively.
     * <p/>
     * The string is compared against the enum element names both with and
     * without any underscore they may have.  For example, if {@code GO_HOME} is
     * an enum member, the command-line argument to match it can be {@code
     * GO_HOME}, {@code go_home}, {@code GoHome}, or even {@code gOhOmE}.  (If
     * the enum has two elements whose names differ only by the presence of
     * underscores, the behavior is undefined.)
     * <p/>
     * You can turn off abbreviations, requiring only full enum member names,
     * using {@link #setAbbreviatedEnums(boolean)}.
     *
     * @param option       The option.
     * @param defaultValue The enum value to use if the option is not
     *                     specified.
     * @param enumClass    The class of the enum to be parsed.
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...)} getString}).
     * @param <T>          The type of the enum; derived from {@code
     *                     defaultValue}.
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @see #setAbbreviatedEnums(boolean)
     */
    public <T extends Enum<T>> T getEnumValue(String option, T defaultValue,
            Class<T> enumClass, String... doc) {

        String typeName = enumClass.getSimpleName();
        String str = getArgument(option, typeName, doc);
        if (str == null)
            return defaultValue;
        T[] possibles = enumClass.getEnumConstants();
        EnumSet<T> matches = EnumSet.noneOf(enumClass);
        for (T e : possibles) {
            String name = e.toString();
            if (str.equalsIgnoreCase(name))
                return e;
            String stripped = STRIP_NAME_PATTERN.matcher(name).replaceAll("");
            if (str.equalsIgnoreCase(stripped))
                return e;

            if (abbreviatedEnums && (abbreviation(str, name) || abbreviation(
                    str, stripped))) {
                matches.add(e);
            }
        }

        // No exact match, see if it is an abbreviation
        if (abbreviatedEnums) {
            if (matches.size() == 0) {
                throw new CommandLineException(
                        str + " unknown in " + enumClass.getSimpleName());
            }
            if (matches.size() == 1)
                return matches.iterator().next();
            else if (matches.size() > 1) {
                String msg =
                        "Ambiguous option \"" + str + "\": Could be " + matches;
                throw new CommandLineException(msg);
            }
        }

        return defaultValue;
    }

    private static boolean abbreviation(String str, String name) {
        return str.regionMatches(true, 0, name, 0, str.length());
    }

    /**
     * Returns a {@link Writer} that is the result of creating a new {@link
     * FileWriter} object for the file named by the given option. If the option
     * is {@code "-"}, the returned writer writes to {@code System.out}, and is
     * <em>not</em> a {@link FileWriter}. (You can therefore test if it is
     * writing to a file by checking if the returned writer is an instance of
     * {@link FileWriter}.) If the option is not specified, return {@code
     * defaultValue}.
     *
     * @param option       The option specification.
     * @param defaultValue The value to return if the option is not specified.
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public Writer getWriter(String option, Writer defaultValue, String... doc)
            throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                return defaultValue;
            }
            if (path.equals("-")) {
                return new OutputStreamWriter(System.out);
            }
            return new FileWriter(path);
        } catch (IOException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns a {@link Writer} that is the result of creating a new {@link
     * FileWriter} object for the file named by the given option. If the option
     * is {@code "-"}, the returned writer writes to {@code System.out}, and is
     * <em>not</em> a {@link FileWriter}. (You can therefore test if it is
     * writing to a file by checking if the returned writer is an instance of
     * {@link FileWriter}.) If the option is not specified, the string {@code
     * path} is used as the file name. If {@code path} is {@code null} then
     * {@code null} is returned.
     *
     * @param option      The option specification.
     * @param defaultPath The path to use if the option is not specified or
     *                    {@code null} if no reader is to be returned in that
     *                    case.
     * @param doc         Usage documentation for the option (@see {@link
     *                    #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or the file specified by {@code
     *         defaultPath}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public Writer getWriter(String option, String defaultPath, String... doc)
            throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                if (defaultPath == null) {
                    return null;
                }
                path = defaultPath;
            }
            if (path.equals("-")) {
                return new OutputStreamWriter(System.out);
            }
            return new FileWriter(path);
        } catch (IOException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns a {@link Reader} that is the result of creating a new {@link
     * FileReader} object for the file named by the given option. If the option
     * is {@code "-"}, the returned reader reads from {@code System.in}, and is
     * <em>not</em> a {@link FileReader}. (You can therefore test if it is
     * reading from a file by checking if the returned reader is an instance of
     * {@link FileReader}.) If the option is not specified, returns {@code
     * defaultValue}.
     *
     * @param option       The option specification.
     * @param defaultValue The value to return if the option is not specified.
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    public Reader getReader(String option, Reader defaultValue, String... doc)
            throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                return defaultValue;
            }
            if (path.equals("-")) {
                return new InputStreamReader(System.in);
            }
            return new FileReader(path);
        } catch (FileNotFoundException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns a {@link Reader} that is the result of creating a new {@link
     * FileReader} object for the file named by the given option. If the option
     * is {@code "-"}, the returned reader reads from {@code System.in}, and is
     * <em>not</em> a {@link FileReader}. (You can therefore test if it is
     * reading from a file by checking if the returned reader is an instance of
     * {@link FileReader}.) If the option is not specified, the string {@code
     * path} is used as the file name. If {@code path} is {@code null} then
     * {@code null} is returned.
     *
     * @param option      The option specification.
     * @param defaultPath The path to use if the option is not specified or
     *                    {@code null} if no reader is to be returned in that
     *                    case.
     * @param doc         Usage documentation for the option (@see {@link
     *                    #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or the file specified by {@code
     *         defaultPath}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    public Reader getReader(String option, String defaultPath, String... doc)
            throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                if (defaultPath == null) {
                    return null;
                }
                path = defaultPath;
            }
            if (path.equals("-")) {
                return new InputStreamReader(System.in);
            }
            return new FileReader(path);
        } catch (FileNotFoundException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns an {@link OutputStream} that is the result of creating a new
     * {@link FileOutputStream} object for the file named by the given option.
     * If the option is {@code "-"}, the returned stream writes to {@code
     * System.out}, and is <em>not</em> a {@link FileOutputStream}. (You can
     * therefore test if it is writing to a file by checking if the returned
     * stream is an instance of {@link FileOutputStream}.) If the option is not
     * specified, returns {@code defaultValue}.
     *
     * @param option       The option specification.
     * @param defaultValue The value to return if the option is not specified.
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public OutputStream getOutputStream(String option,
            OutputStream defaultValue, String... doc)
            throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                return defaultValue;
            }
            if (path.equals("-")) {
                return System.out;
            }
            return new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns an {@link OutputStream} that is the result of creating a new
     * {@link FileOutputStream} object for the file named by the given option.
     * If the option is {@code "-"}, the returned stream writes to {@code
     * System.out}, and is <em>not</em> a {@link FileOutputStream}. (You can
     * therefore test if it is writing to a file by checking if the returned
     * stream is an instance of {@link FileOutputStream}.) If the option is not
     * specified, the string {@code path} is used as the file name. If {@code
     * path} is {@code null} then {@code null} is returned.
     *
     * @param option      The option specification.
     * @param defaultPath The path to use if the option is not specified or
     *                    {@code null} if no reader is to be returned in that
     *                    case.
     * @param doc         Usage documentation for the option (@see {@link
     *                    #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or the file specified by {@code
     *         defaultPath}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public OutputStream getOutputStream(String option, String defaultPath,
            String... doc) throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                if (defaultPath == null) {
                    return null;
                }
                path = defaultPath;
            }
            if (path.equals("-")) {
                return System.out;
            }
            return new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns an {@link InputStream} that is the result of creating a new
     * {@link FileInputStream} object for the file named by the given option. If
     * the option is {@code "-"}, the returned stream reads from {@code
     * System.in}, and is <em>not</em> a {@link FileInputStream}. (You can
     * therefore test if it is reading from a file by checking if the returned
     * stream is an instance of {@link FileInputStream}.) If the option is not
     * specified, returns {@code defaultValue}.
     *
     * @param option       The option.
     * @param defaultValue The value to return if the option is not specified.
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    public InputStream getInputStream(String option, InputStream defaultValue,
            String... doc) throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                return defaultValue;
            }
            if (path.equals("-")) {
                return System.in;
            }
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns an {@link InputStream} that is the result of creating a new
     * {@link FileInputStream} object for the file named by the given option. If
     * the option is {@code "-"}, the returned stream reads from {@code
     * System.in}, and is <em>not</em> a {@link FileInputStream}. (You can
     * therefore test if it is reading from a file by checking if the returned
     * stream is an instance of {@link FileInputStream}.) If the option is not
     * specified, the string {@code path} is used as the file name. If {@code
     * path} is {@code null} then {@code null} is returned.
     *
     * @param option      The option.
     * @param defaultPath The path to use if the option is not specified.
     * @param doc         Usage documentation for the option (@see {@link
     *                    #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or the file specified by {@code
     *         defaultPath}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    public InputStream getInputStream(String option, String defaultPath,
            String... doc) throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                if (defaultPath == null) {
                    return null;
                }
                path = defaultPath;
            }
            if (path.equals("-")) {
                return System.in;
            }
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns a {@link RandomAccessFile} that is the result of creating a new
     * {@link RandomAccessFile} object for the file named by the given option,
     * using the given {@code mode}. If the option is not specified, return
     * {@code defaultValue}.
     *
     * @param option       The option.
     * @param defaultValue The value to return if the option is not specified.
     * @param mode         The mode parameter for {@link RandomAccessFile#RandomAccessFile(String,String)}
     * @param doc          Usage documentation for the option (@see {@link
     *                     #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    public RandomAccessFile getRandomAccessFile(String option,
            RandomAccessFile defaultValue, String mode, String... doc)
            throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                return defaultValue;
            }
            return new RandomAccessFile(path, mode);
        } catch (FileNotFoundException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns a {@link RandomAccessFile} that is the result of creating a new
     * {@link RandomAccessFile} object for the file named by the given option,
     * using the given {@code mode}. If the option is not specified, the string
     * {@code path} is used as the file name. If {@code path} is {@code null}
     * then {@code null} is returned.
     *
     * @param option      The option.
     * @param defaultPath The path to use if the option is not specified.
     * @param mode        The mode parameter for {@link RandomAccessFile#RandomAccessFile(String,String)}
     * @param doc         Usage documentation for the option (@see {@link
     *                    #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws IOException          There was a problem opening the file.
     * @throws CommandLineException No argument was present.
     */
    public RandomAccessFile getRandomAccessFile(String option,
            String defaultPath, String mode, String... doc)
            throws IOException, CommandLineException {

        String path = getArgument(option, "file", doc);
        try {
            if (path == null) {
                if (defaultPath == null) {
                    return null;
                }
                path = defaultPath;
            }
            return new RandomAccessFile(path, mode);
        } catch (FileNotFoundException e) {
            throw ioException(option, path, e);
        }
    }

    /**
     * Returns a directory specified by the user. If the option is not
     * specified, {@code path} is used. If the path is of an existing entity in
     * the file system, it must be a directory. If {@code path} is {@code null}
     * then {@code null} is returned and no directory is created.
     *
     * @param option      The option.
     * @param defaultPath The path to use if the option is not specified.
     * @param doc         Usage documentation for the option (@see {@link
     *                    #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws CommandLineException No argument was present.
     */
    public String getDirectory(String option, String defaultPath, String... doc)
            throws CommandLineException {

        String dir = getArgument(option, "dir", doc);
        return parseDirectory(option, dir, defaultPath, MissingDirAction.NONE);
    }

    /**
     * Returns a directory specified by the user. If the option is not
     * specified, {@code path} is used. If the path is of an existing entity in
     * the file system, it must be a directory.
     * <p/>
     * If the path is for a non-existing directory, the {@code ifMissing}
     * parameter says what to do: <li> {@link MissingDirAction#NONE}: Simply
     * return the path. <li> {@link MissingDirAction#EXCEPTION}: Throw a {@link
     * CommandLineException}. <li> {@link MissingDirAction#CREATE}: Create the
     * directory. If this is not posible, throw a {@link CommandLineException}.
     *
     * @param option      The option.
     * @param defaultPath The value to return if the option is not specified.
     * @param ifMissing   The action to take if the directory is missing.
     * @param doc         Usage documentation for the option (@see {@link
     *                    #getString(String,String,String...)} getString}).
     *
     * @return The value for the option, or {@code defaultValue}.
     *
     * @throws CommandLineException No argument was present.
     */
    public String getDirectory(String option, String defaultPath,
            MissingDirAction ifMissing, String... doc)
            throws CommandLineException {

        String path = getArgument(option, "dir", doc);
        return parseDirectory(option, path, defaultPath, ifMissing);
    }

    /**
     * Finds the given option somewhere in the command line. If the option is
     * not found, return {@code false}. Otherwise set {@code str}, {@code pos},
     * and {@code opt} fields, mark the option character as used, and then
     * return {@code true}.
     *
     * @param o       The option object.
     * @param lookFor The option.
     *
     * @return {@code true} if the option is specified.
     */
    private boolean findOpt(Opt o, String lookFor) {
        if (o == null) {
            return false;
        }

        for (int i = 0; i < args.length; i++) {
            if (used.get(i)) {
                // already consumed
                continue;
            }

            String arg = args[i];
            if (arg.charAt(0) != '-') {
                // not an option
                continue;
            }
            if (arg.equals("--")) {
                // "--" ends the list
                break;
            }
            if (o.matches(arg)) {
                foundStr = i;
                foundOpt = lookFor;
                used.set(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Return the current option's argument, marking its characters as used.
     *
     * @return The current option's argument.
     *
     * @throws CommandLineException No argument is given.
     */
    private String optArg() throws CommandLineException {
        if (foundStr + 1 >= args.length) {
            String msg = "Argument missing for -" + foundOpt;
            throw new CommandLineException(msg);
        }
        used.set(foundStr + 1);
        return args[foundStr + 1];
    }

    /**
     * Returns the command line operands that come after the options. This
     * checks to make sure that all specified options have been consumed -- any
     * options remaining at this point are assumed to be unknown options. If no
     * operands remain, an empty array is returned.
     * <p/>
     * This is also where {@code -help} is handled. If the user specifies {@code
     * -help}, and that option is not manually process by the code using {@link
     * #getBoolean(String, String...)} getBoolean}, then the method {@link
     * #usage} is invoked and {@link HelpOnlyException} is thrown. The program
     * is expected to catch this exception and simply exit successfully.
     *
     * @param operandsDesc Operand descriptions. In usage messages, this will be
     *                     printed out after the options usage is described.
     *                     Typically this ends with {@code "..."} if an
     *                     arbitrary number of operands can be specified.
     *
     * @return The operands that follow the options.
     *
     * @throws CommandLineException An unknown option was specified.
     * @throws HelpOnlyException    The user asked for usage/help information.
     * @see #synopsis()
     * @see #usage()
     */
    @SuppressWarnings({"ParameterHidesMemberVariable"})
    public String[] getOperands(String... operandsDesc)
            throws CommandLineException, HelpOnlyException {

        operandsFetched = true;
        this.operandsDesc = operandsDesc.clone();

        checkForHelp();

        StringBuilder unused = new StringBuilder();
        int count = 0;
        int a;
        for (a = 0; a < args.length; a++) {
            if (used.get(a)) {                      // skip used parameters
                continue;
            }
            if (!args[a].startsWith("-")) {         // first non-option argument
                break;
            }
            if (args[a].equals("--")) {             // "--" ends things
                a++;                                // skip the "--"
                break;
            }
            if (unused.length() > 1) {
                unused.append(' ');
            }
            unused.append(args[a]);
            count++;
        }
        if (unused.length() != 0) {
            String ustr = unused.toString();
            String plural = count > 0 ? "s" : "";
            String msg = "unknown/unused option" + plural + ": " + ustr;
            throw new CommandLineException(msg);
        }

        String[] remains = new String[args.length - a];
        System.arraycopy(args, a, remains, 0, remains.length);
        return remains;
    }

    private void checkForHelp() {
        // see if the user has already checked for and handled "help"
        for (Opt o : options) {
            if (o.multi.equals("help")) {
                return;
            }
        }

        // They haven't, so we will check
        if (groups.size() > 1) {
            optionGroup("Help");
        }

        boolean wantsHelp = hasDescs ? getBoolean("help",
                "Print help message") : getBoolean("help");
        if (wantsHelp) {
            usage();
            throw new HelpOnlyException();
        }
    }

    /**
     * Adds the given option of the given type to the list of known options;
     * {@code -help} is handled separately in {@link #getOperands}.
     *
     * @param opt     The option to add as known.
     * @param optType The type of option.
     * @param doc     Usage documentation for the option.
     *
     * @return The option object.
     *
     * @see #getOperands(String...)
     * @see #usage()
     */
    private Opt addOpt(String opt, String optType, String... doc) {
        // ensure this is a new, not a redundant, option.
        for (Opt o : options) {
            if (o.multi.equals(opt)) {
                o.repeatable = true;
                return o;                // already known
            }
        }

        Opt o = new Opt(opt, optType, doc);
        options.add(o);
        return o;
    }

    /**
     * Prints out the command's usage, inferred from the requested options. You
     * can override this to provide a more specific summary, or you can handle
     * "help" as a boolean option yourself. This implementation is only valid
     * after all known options have been requested and {@link #getOperands} has
     * been called.
     *
     * @param out Stream for the usage message.
     *
     * @see #getOperands
     * @see #synopsis
     */
    public void usage(PrintWriter out) {
        Opt[] opts = doSynopsis(out);

        // If there are no descriptive texts, just return
        if (!hasDescs) {
            return;
        }

        for (String desc : programDesc) {
            out.println(desc);
        }

        int lastGrp = -1;
        for (Opt opt : opts) {
            if (opt.group != lastGrp) {
                if (lastGrp != -1 || programDesc.length > 0) {
                    out.println();
                }
                String groupName = groups.get(opt.group);
                if (groupName.length() > 0) {
                    out.print(groupName);
                    out.println(" Options:");
                }
                lastGrp = opt.group;
            }
            out.print(opt.helpString("    "));
        }
    }

    /**
     * Prints out a synopsis the command's usage, inferred from the requested
     * options. You can override this to provide a more specific summary, or you
     * can handle "help" as a boolean option yourself. This implementation is
     * only valid after all known options have been requested and {@link
     * #getOperands} has been called.
     *
     * @param out Stream for the usage message.
     *
     * @see #getOperands
     */
    public void synopsis(PrintWriter out) {
        doSynopsis(out);
    }

    private Opt[] doSynopsis(PrintWriter out) {
        if (!operandsFetched) {
            throw new IllegalStateException(
                    "must call getOperands() before asking for usage");
        }

        // Order the options
        Opt[] opts = options.toArray(new Opt[options.size()]);
        Arrays.sort(opts, new Comparator<Opt>() {
            @Override
            public int compare(Opt o1, Opt o2) {
                if (o1.group != o2.group) {
                    return o1.group - o2.group;
                }
                return o1.order - o2.order;
            }
        });

        if (programName != null) {
            out.print(programName);
        }

        for (Opt opt : opts) {
            out.print(" [");
            out.print(opt);
            out.print("]");
        }
        for (String desc : operandsDesc) {
            out.print(' ');
            out.print(desc);
        }
        out.println();
        return opts;
    }

    /**
     * Prints out the command's usage on {@code System.out}. Equivalent to
     * <pre>
     * usage(System.out, "...");
     * </pre>
     *
     * @see #usage(PrintWriter)
     */
    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public void usage() {
        System.out.flush();
        PrintWriter pout = new PrintWriter(System.out);
        usage(pout);
        pout.flush();
    }

    /**
     * Prints out a synposis of the command's usage on {@code System.out}.
     *
     * @see #usage(PrintWriter)
     */
    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public void synopsis() {
        System.out.flush();
        PrintWriter pout = new PrintWriter(System.out);
        synopsis(pout);
        pout.flush();
    }

    /**
     * Returns the string that would be printed by {@link #usage(PrintWriter)}.
     *
     * @return The string that would be printed by {@link #usage(PrintWriter)}.
     *
     * @see #usage(PrintWriter)
     */
    public String usageString() {
        StringWriter out = new StringWriter();
        PrintWriter pout = new PrintWriter(out);
        usage(pout);
        pout.close();
        return out.toString();
    }

    /**
     * Returns the string that would be printed by {@link
     * #synopsis(PrintWriter)}.
     *
     * @return The string that would be printed by {@link #synopsis(PrintWriter)}.
     *
     * @see #synopsis(PrintWriter)
     */
    public String synopsisString() {
        StringWriter out = new StringWriter();
        PrintWriter pout = new PrintWriter(out);
        synopsis(pout);
        pout.close();
        return out.toString();
    }

    /**
     * Returns the result of parsing the given directory from the command line.
     * If {@code path} is {@code null} return {@code defaultPath}. If the path
     * is of an existing entity in the file system, it must be a directory. If
     * {@code defaultPath} is also {@code null}, this returns {@code null}.
     * <p/>
     * If the path is for a non-existing directory, the {@code ifMissing}
     * parameter says what to do: <li> {@link MissingDirAction#NONE}: Simply
     * return the path. <li> {@link MissingDirAction#EXCEPTION}: Throw a {@link
     * CommandLineException}. <li> {@link MissingDirAction#CREATE}: Create the
     * directory. If this is not posible, throw a {@link CommandLineException}.
     *
     * @param opt         The option this is being done for.
     * @param path        The path to parse.
     * @param defaultPath The path to use if {@code str} is {@code null}.
     * @param ifMissing   What to do if the directory does not exist.
     *
     * @return The final path.
     *
     * @throws CommandLineException The path exists already, but is not a
     *                              directory.
     */
    private static String parseDirectory(String opt, String path,
            String defaultPath, MissingDirAction ifMissing)
            throws CommandLineException {

        if (path == null) {
            if (defaultPath == null) {
                return null;
            }
            path = defaultPath;
        }

        File dir = new File(path);
        if (dir.exists()) {
            if (dir.isDirectory()) {
                return path;
            }
            throw new CommandLineException(
                    "Exists, but is not a directory: " + path);
        }

        switch (ifMissing) {
        case NONE:
            break;
        case EXCEPTION:
            throw new CommandLineException("No such directory: " + path);
        case CREATE:
            try {
                mkdirs(path);
            } catch (IOException e) {
                //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
                throw new CommandLineException(
                        "-" + opt + " " + e.getMessage());
            }
            break;
        }
        return path;
    }

    /**
     * Creates the given directory if needed, including any intermediate missing
     * directories.
     *
     * @param path The path for the directory.
     *
     * @throws IOException The path already exists as a file or {@link
     *                     File#mkdirs} returns {@code false}.
     */
    private static void mkdirs(String path) throws IOException {
        File dir = new File(path);
        if (dir.isDirectory()) {
            return;
        }

        if (dir.exists() && !dir.isDirectory()) {
            throw new IOException(
                    "mkdirs: " + dir + " exists but is not a directory");
        }

        if (!dir.mkdirs()) {
            throw new IOException(
                    "mkdirs: " + dir + " Cannot create directory");
        }
    }

    @SuppressWarnings({"TypeMayBeWeakened"})
    private static NumberFormatException numException(NumberFormatException e,
            String option) {

        NumberFormatException ne = new NumberFormatException(
                "-" + option + " " + e.getMessage());
        ne.initCause(e);
        return ne;
    }

    @SuppressWarnings({"TypeMayBeWeakened"})
    private static IOException ioException(String opt, String path,
            IOException e) {

        IOException ne = new IOException("-" + opt + " " + path);
        ne.initCause(e);
        return ne;
    }
}
