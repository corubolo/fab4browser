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

package org.simplx.args;

import org.simplx.args.advanced.ArrayMapper;
import org.simplx.args.advanced.CharMapper;
import org.simplx.args.advanced.CommandLineAdvanced;
import org.simplx.args.advanced.EnumMapper;
import org.simplx.args.advanced.FileMapper;
import org.simplx.args.advanced.InputStreamMapper;
import org.simplx.args.advanced.Mapper;
import org.simplx.args.advanced.NumberMapper;
import org.simplx.args.advanced.ObjectMapper;
import org.simplx.args.advanced.OutputStreamMapper;
import org.simplx.args.advanced.RandomAccessFileMapper;
import org.simplx.args.advanced.ReaderMapper;
import org.simplx.args.advanced.StringMapper;
import org.simplx.args.advanced.WriterMapper;
import org.simplx.io.ParseStringUtil;
import org.simplx.logging.SimplxLogging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.logging.Level.*;
import static org.simplx.args.CommandLine.HelpBehavior.PRINT_AND_EXIT;
import static org.simplx.args.CommandLine.HelpBehavior.PRINT_AND_RETURN;
import static org.simplx.args.CommandOpt.NO_DEFAULT;
import static org.simplx.args.CommandOpt.NO_MULTI;
import static org.simplx.logging.SimplxLogging.logFormat;

/**
 * This class provides a command line parsing mechanism, primarily for use with
 * <tt>main</tt> methods.  It makes it easy for you to mark fields that should
 * be set by options with the annotation {@link CommandOpt}, and then handles
 * everything based on those annotations.
 * <p/>
 * The documentation for class this may appear long, but this is an easy system
 * to use. What it does is complex, but what you need to tell it is minimal. The
 * only advanced technique has to do with writing custom "mappers" for new
 * types, but this is unnecessary for almost all uses, so you can safely ignore
 * it until you find yourself stuck doing complicated things to decode and
 * validate options. Then you might consider a custom mapper.
 * <p/>
 * You use the {@link CommandOpt} annotation to mark the fields that should be
 * set from command line arguments.  These can be either static or non-static
 * fields. You then create a {@link CommandLine} object, passing in the object
 * on which you want fields should be.  (You can instead pass in a {@link Class}
 * object, in which case only static fields from that class will be used.)  You
 * can then pass in the command line arguments to the {@link #apply} method,
 * which will parse the command line options, setting fields as appropriate.
 * Then {@link #apply} returns the rest of the command line that appears after
 * all the options.
 * <p/>
 * For example, suppose you have the following class:
 * <pre>
 * public class Config {
 *     {@code @}CommandOpt
 *     boolean verbose;
 *     {@code @}CommandOpt
 *     Reader in;
 *     {@code @}CommandOpt
 *     Writer out;
 * }
 * </pre>
 * You can then read values for these options using the following code:
 * <pre>
 * public static void main(String[] args) throws Exception {
 *     Config config = new Config();
 *     CommandLine line = new CommandLine(config, "Prog");
 *     List&lt;String> operands = line.apply(args);
 * }
 * </pre>
 * This would parse a command line such as
 * <pre>
 * Prog -verbose -in srcFile -out resultFile left right up down
 * </pre>
 * This would set <tt>verbose</tt> to <tt>true</tt>, create a {@link
 * BufferedReader} that reads from the file <tt>srcFile</tt>, and create a
 * {@link BufferedWriter} that writes to the file <tt>resultFile</tt>.  The
 * {@link #apply} method makes all this happen, and then returns the operands
 * (the strings that follow the options) as a list of strings containing
 * <tt>"left"</tt>, <tt>"right"</tt>, <tt>"up"</tt>, and <tt>"down"</tt>.
 * <p/>
 * It also recognizes <tt>--help</tt> and <tt>-?</tt> options that will print
 * out a usage message and immediately exit.
 * <pre>
 * Prog [-help] [-verbose] [-in file] [-out file] [ops ...]
 * </pre>
 * The usage message can be affected, is you will see later.
 * <p/>
 * <h3>Single and Multi-only Command Lines</h3>
 * <p/>
 * By default, the option has the same name as the field. You can override this
 * with the annotation's {@link CommandOpt#multi} value.
 * <p/>
 * But many command line uses like to have single-character ways to specify an
 * option.  For example, this is how the POSIX and other Unix standards define
 * command line syntax.  You can do this using the annotation's {@link
 * CommandOpt#single} value.
 * <p/>
 * If you specify {@code single}, the option can be specified with either
 * single- or multi-character options.  For example,
 * <pre>
 *     {@code @}CommandOpt(single = 'v')
 *     boolean verbose;
 * </pre>
 * means that the user can turn on the verbose option with either
 * <tt>--verbose</tt> or <tt>-v</tt>.
 * <p/>
 * Note that if any options have single-character options, then the
 * multi-character version must be specified with two dashes.  Only if there are
 * no single-character options at all can the user use a single dash for
 * multi-character options (as is shown above).  This will be reflected in
 * generated usage messages.
 * <p/>
 * If you want an option that is available <em>only</em> in single-character
 * form, you can set {@link CommandOpt#multi()} to {@link CommandOpt#NO_MULTI}.
 * <p/>
 * If the user specifies an unknown option, or does anything else which
 * conflicts with the derived command line syntax, will generate a {@link
 * CommandLineException} with descriptive text.
 * <p/>
 * On the other hand, if your configuration is malformed, you will get an {@link
 * IllegalArgumentException} or a {@link NullPointerException}.
 * <p/>
 * <h3>Parsing Command Lines</h3>
 * <p/>
 * Multi-character options can always be specified with two dashes; in the
 * above, <tt>--verbose</tt> is recognized whether any options have
 * single-character forms or not.
 * <p/>
 * If no option has a single-character form, then you can use a single dash for
 * options, as in the first example above, where <tt>-verbose</tt> is actually
 * the preferred form.
 * <p/>
 * As shown with {@code -verbose}, boolean variables are set to <tt>true</tt> if
 * the option is specified; otherwise they are left as <tt>false</tt>. (This can
 * be changed, as described later.)  Multi-characters options are each specified
 * in separate command-line arguments.  Single-character boolean options can be
 * put together.  For example, if the boolean options <tt>x</tt>, <tt>y</tt>,
 * and <tt>z</tt> are available, they can be specified as <tt>-x</tt>
 * <tt>-y</tt> <tt>-z</tt>, <tt>-xyz</tt>, <tt>-xy</tt> <tt>-z</tt>, and so on.
 * <p/>
 * All other options have values that are specified on the command line
 * following the option name.  For multi-character options, this is always the
 * next argument on the command line.  This can be done for single-character
 * options, or the value can immediately follow.  For example, if <tt>n</tt> is
 * a single-character numeric option, it could be specified either as
 * <tt>-n</tt> <tt>12</tt> or <tt>-n12</tt>.  This can also be combined with
 * boolean options, in the form of <tt>-xyzn</tt> <tt>12</tt> or
 * <tt>-xyzn12</tt>.  For obvious reasons, you can put as many booleans as you
 * like before the non-boolean option in the group, but you can't put more than
 * one non-boolean option in a single group.
 * <p/>
 * The user can specify that options should be read from a file by using the
 * special <tt>-@</tt> <tt><i>file</i></tt> option.  The file must exist, but
 * may be empty.  The file will be opened, and the text read from it to create a
 * new array of strings, which will then be processed before proceding on with
 * the rest of the original arguments.
 * <p/>
 * Arguments are scanned for options until the first argument is found that does
 * not start with a <tt>-</tt>, or until the end of the arguments are reached.
 * An argument that is just <tt>--</tt> (and nothing else) also signals the end
 * of options.  Once argument processing stops, any default values are applied,
 * and the rest of the arguments &mdash; the <i>operands</i> &mdash; are
 * returned as list of strings.  (If <tt>--</tt> is used, that is not included
 * in the operands.)
 * <p/>
 * Be careful: Options being read from a file using <tt>-@</tt> are just like
 * any other arguments in the list.  If one of them signals the end of the
 * options, all the rest of the strings will be operands, including those in the
 * original argument array.  The arguments
 * <pre>
 * -x -@optFile -y
 * </pre>
 * seem like they specify options <i>x</i> and <i>y</i>, but if the file
 * contains <tt>--</tt>, it will be equivalent to
 * <pre>
 * -x -- -y
 * </pre>
 * and <tt>-y</tt> will become one of the operands.  Like many powers, this can
 * be used for good or evil, but it certainly should be used with caution.
 * <p/>
 * <h3>Value Types</h3>
 * <p/>
 * Options that have values &mdash; that is, every option but boolean options
 * &mdash; are parsed according to the type of the field.  This is done by using
 * mappers, which you can define yourself.  The standard mappers handle the
 * following Java types: <dl> <dt><tt>String</tt> <dd>String fields are simply
 * set to the specified string. <dt><tt>char</tt>, <tt>Character</tt>
 * <dd>Character fields are set to the single character decoded from the value.
 * Any legal Java character specification is accepted, including {@code \}{@code
 * u} notation. <dt>Numbers (<tt>byte</tt>, <tt>Byte</tt>, <tt>short</tt>,
 * <tt>Short</tt>, <tt>int</tt>, <tt>Integer</tt>, <tt>long</tt>, <tt>Long</tt>,
 * <tt>float</tt>, <tt>Float</tt>, <tt>double</tt>, <tt>Double</tt>) <dd>Numbers
 * are parsed as they are by the various constructors {@link Byte}, {@link
 * Short}, etc. In addition, leading plus signs are handled properly.
 * <dt>Enumerations <dd>Enumeration fields are parsed by value names.  The
 * string must match one element name in the enum. An abbreviation of any unique
 * starting string is accepted by default. For example, if the enum had elements
 * <tt>MIN</tt>, <tt>AVG</tt>, and <tt>MAX</tt>, the user could use any of
 * <tt>a</tt>, <tt>av</tt>, or <tt>avg</tt> to match <tt>AVG</tt>.  But
 * <tt>m</tt> would cause an error since it could match either <tt>MIN</tt> or
 * <tt>MAX</tt>, whose shortest abbreviations are <tt>mi</tt> and <tt>ma</tt>,
 * respectively. The string is compared against the enum element names both with
 * and without any underscore they may have.  For example, if {@code GO_HOME} is
 * an enum member, the command-line argument to match it can be {@code GO_HOME},
 * {@code go_home}, {@code GoHome}, or even {@code gOhOmE}.  (If the enum has
 * two elements whose names differ only by the presence of underscores, the
 * behavior is undefined.) <dt><tt>java.io.File</tt> <dd>A <tt>File</tt> option
 * is filled in with a new object constructed with the value string.
 * <dt><tt>Reader</tt>, <tt>Writer</tt>, <tt>InputStream</tt>,
 * <tt>OutputStream</tt> <dd>This streams are created by opening the file named
 * by the string. For reading, if the name is <tt>-</tt> <tt>System.in</tt> is
 * used.  For writing, <tt>-</tt> means <tt>System.out</tt>, as does
 * <tt>-1</tt>; <tt>-2</tt> means <tt>System.err</tt>. <dt><tt>RandomAccessFile</tt>
 * <dd>This file is opened on the named file. If you have set {@link
 * CommandOpt#mode()} then that is passed as the mode parameter to {@link
 * RandomAccessFile#RandomAccessFile(String,String)}, otherwise the mode is
 * <tt>"r"</tt>. </dl> Any other field type is handled by finding a constructor
 * for the class that takes a single {@link String} parameter, and assigning the
 * result of that construction to the field.  If there is none, you will get an
 * {@link IllegalArgumentException}.
 * <p/>
 * You can also have fields that are arrays of values.  This lets the user
 * specify any number of values for an option.  Each specified option will be
 * appended to the array.  The user can also specify multiple elements at one
 * time, using the value splitter.  The default value separator is a comma.  For
 * example, the user might say <tt>-v1,2,3</tt> or <tt>-v1</tt> <tt>-v2</tt>
 * <tt>-v3</tt> or <tt>-v1,2</tt> <tt>-v3</tt>.
 * <p/>
 * The values are read using the same mapper that would be used for the array's
 * component type.  For example, an <tt>int[]</tt> field will read each value
 * the same way it is read for a single <tt>int</tt> field.
 * <p/>
 * An empty string, whether for default or specified by the user, means zero
 * elements. So a default of <tt>""</tt> means a default of a zero-length array.
 * And <tt>apply("-v", "")</tt> adds nothing to the array, although it does
 * create a zero-length array for the field if the field is <tt>null</tt>.
 * <p/>
 * <h3>Default Values</h3>
 * <p/>
 * You can use {@link CommandOpt#deflt} to specify a default value for an
 * option.
 * <p/>
 * If no {@link CommandOpt#deflt deflt} is provided, the value of the field will
 * be left alone unless the option is specified explicitly in the arguments.
 * <p/>
 * For boolean options, only a value of <tt>"true"</tt> has any effect. Without
 * that, or with any other default string, the default value is <tt>false</tt>,
 * and speficying the option on the command line sets it to <tt>true</tt>.  If
 * the specified default is <tt>"true"</tt> the opposite happens: If the option
 * is specified, the value will be set to <tt>false</tt>; otherwise it will be
 * <tt>true</tt>.
 * <p/>
 * For all other types, if the annotation provides a {@link CommandOpt#deflt
 * deflt}, and the option is not specified in the arguments, that string will be
 * applied exactly as if it was the value specified in the arguments.
 * <p/>
 * <h3>Checks</h3>
 * <p/>
 * Certain basic checks can potentially be applied to an option.  For all types,
 * you can apply any simple boolean method as a check by name.  For example, the
 * {@link File#exists()} method can be applied as a check by setting the {@link
 * CommandOpt#checks} value to the string <tt>"exists"</tt>.  This works for any
 * such method: that is, for any method that is not static, takes no parameters,
 * and returns a <tt>boolean</tt> or <tt>Boolean</tt> value.
 * <p/>
 * Such checks are enforced after setting the field.  If a violation of a check
 * is found, the user is given an error message via a {@link
 * CommandLineException}.
 * <p/>
 * You can specify the opposite of a check by preceding it with an exclamation
 * mark.  For example, a {@link CommandOpt#checks checks} value of
 * <tt>"!exists"</tt> will give the user an error if the specified file
 * <em>does</em> exist.
 * <p/>
 * Some checks are actually commands.  For example, the mapper for {@link File}
 * defines a <tt>"mkdir"</tt> action that will create a directory at the
 * specified path if it does not exist, and if it does, will verify that is a
 * directory.  Any <tt>void</tt> method that is not static and takes no
 * parameters can be used as a command.  These will only generate an error if
 * they throw an exception.
 * <p/>
 * You can specify any number of checks, separating them in the {@link
 * CommandOpt#checks checks} string seaprated by commas.  Whitespace around
 * these commas and around any exclamation mark is ignored.
 * <p/>
 * Translating strings into field values is done by a {@link Mapper}, which can
 * also define other checks.
 * <p/>
 * <h3>Usage Messages</h3>
 * <p/>
 * With all this information, one important feature provided by this class is to
 * generate usage messages.
 * <p/>
 * First there is a brief usage message.  This is a list of the possible options
 * and (for non-booleans) their option types.  An example shown previously is:
 * <pre>
 * Prog [-help] [-verbose] [-in file] [-out file] [ops ...]
 * </pre>
 * These are built out of the following parts: <dl> <dt>program name <dd>The
 * default program name is the {@link Class#getSimpleName()} return for the
 * class of the object that has the fields.  You can specify otherwise using one
 * of the constructors that lets you do so. <dt>option names <dd>These are the
 * field names by default, but this can be overridden using {@link
 * CommandOpt#multi}. Single-character version are obtained from {@link
 * CommandOpt#single()}. <dt>option value descriptions <dd>For non-boolean
 * options, the usage message shows a "type"-implying name for the value.  The
 * default for this is defined by the mapper.  In the example above, the mappers
 * for both {@link Reader} and {@link Writer} say that their default value name
 * is <tt>"file"</tt>.  For a specific field you can override this with {@link
 * CommandOpt#valueName()}. <dt>operand description <dd>The values that appear
 * after all the options are called <i>operands</i>. The default description for
 * them is what is shown above: <tt>"[ops ...]"</tt>. You can override this in
 * the {@link CommandLine} constructor. </dl> As an example, we could adjust the
 * brief usage message by modifying the original class:
 * <pre>
 * public class Config {
 *     {@code @}CommandOpt(single = 'v')
 *     boolean verbose;
 *     {@code @}CommandOpt(valueName = "sourceFile")
 *     Reader in;
 *     {@code @}CommandOpt(valueName = "outputFile")
 *     Writer out;
 * }
 * </pre>
 * and by using the the constructor:
 * <pre>
 *     CommandLine line = new CommandLine(config, "process", "direction ...");
 * </pre>
 * Now the brief usage message, as generated by invoking {@link
 * #briefUsage(Writer)}, would be
 * <pre>
 * process [-?|--help] [-v|--verbose] [--in sourceFile] [--out outputFile]
 *     direction ...
 * </pre>
 * <p/>
 * Fuller descriptions can be generated by {@link CommandLine#fullUsage(Writer)}.
 * These use the {@link CommandOpt#desc} value for a description of the option
 * and its effect, and the <tt>description</tt> value of the constructor {@link
 * #CommandLine(Object,String,String,String)}.  For example
 * <pre>
 * public class Config {
 *     {@code @}CommandOpt(single = 'v',
 *                     desc = "Verbose message during execution")
 *     boolean verbose;
 *     {@code @}CommandOpt(valueName = "sourceFile",
 *                     desc = "Source input file; by default, System.in")
 *     Reader in;
 *     {@code @}CommandOpt(valueName = "outputFile",
 *                     desc = "File for output; by default, System.out")
 *     Writer out;
 * }
 * </pre>
 * and by chosing the the constructor:
 * <pre>
 *     CommandLine line = new CommandLine(config, "process", "direction ...",
 *         "This command reads a source, applies the directions given" +
 *         " on the command line, and generates a resulting output file." +
 *         " At least one direction must be given.");
 * </pre>
 * The full message, as generated by {@link #fullUsage(Writer)}, would be
 * <pre>
 * process [-?|--help] [-v|--verbose] [--in sourceFile] [--out outputFile]
 *     direction ...
 * This command reads a source, applies the directions given on the command
 * line, and generates a resulting output file. At least one direction must be
 * given.
 * <p/>
 * -?|--help         Print usage message
 * -v|--verbose      Verbose message during execution
 * --in sourceFile   Source input file; by default, System.in
 * --out outputFile  File for output; by default, System.out
 * </pre>
 * Notice that the messages are wrapped by the usage generating methods.  You
 * can set the line width that is used with {@link #setLineWidth(int)}.  And you
 * can turn off wrapping entirely by setting the width to zero.
 * <p/>
 * The other part of formatting you can control is how far indented the
 * per-option descriptions are, using {@link #setOptionDescriptionIndent(int)}
 * to change it away from the default value of 24.  This value is used to line
 * up these per-option descriptions.  Whatever the value is, if an option's
 * summary is longer than that, the option's description values will start on
 * the next line.
 * <p/>
 * <h3>Advanced Features</h3>
 * <p/>
 * The features you will most likley need are expressed in this class.  There
 * are advanced features you can probably ignore, revealed by the object
 * returned by {@link #getAdvancedKnobs()}.  These let you <ul> <li>add new
 * custom interpreters for field types, which sometimes is helpful, although you
 * can usually get the effect you need simply by post-processing the options
 * after they have been parsed <li>change some features of how the command line
 * is interpreted <li> parse values from other sources in the same way as for a
 * command line </ul> The standard mappers were implicitly described above, in
 * the list of types that are accepted.  For each of these, there is a mapper
 * that will be applied.  These standard mappers are universal.  You can define
 * custom mappers for particular types, and set them on a particular {@link
 * CommandLine} object.
 * <p/>
 * Generally you will not need to think about mappers.  If you do, read the
 * documentation in {@link Mapper}.
 */
public class CommandLine {
    private static final Logger logger = SimplxLogging.loggerFor(
            CommandLine.class);

    private final Object holder;
    private final List<Field> fields;
    private final Set<Field> seen;
    private final Map<CharSequence, Field> multiOptions;
    private final Map<CharSequence, Field> singleOptions;
    private final List<Mapping> customMappings;
    private boolean tryAllTypes;
    private HelpBehavior helpBehavior;
    private final String programName;
    private final String operandDisplay;
    private final String programDesc;
    private int lineWidth;
    private int optionDescriptionIndent;
    private String multiDashes;
    private Pattern splitterPattern;
    private String splitterDisplay;

    @CommandOpt(single = '@', multi = NO_MULTI)
    private File optionFile;

    @CommandOpt(multi = "help", single = '?', desc = "Print usage message")
    private boolean singleHelpOnly;
    @CommandOpt(multi = "help", desc = "Print usage message")
    private boolean multiHelpOnly;

    private static final Pattern COMMAS = Pattern.compile(",");

    private static final List<Mapping> GLOBAL_MAPPINGS;

    private static final Pattern OPTION_SPLIT = Pattern.compile("(?<=\\]) ");
    private static final Pattern SPACE_SPLIT = Pattern.compile(" +");
    private static final Pattern OPTION_FILE_SPLIT = Pattern.compile(
            "(?<!\\\\)\\s+", Pattern.MULTILINE);
    private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

    private static final Mapper BASIC_MAPPER = new ObjectMapper();

    private static final Field OPTION_FILE_FIELD;

    static {
        try {
            OPTION_FILE_FIELD = CommandLine.class.getDeclaredField(
                    "optionFile");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Cannot find field", e);
        }

        GLOBAL_MAPPINGS = new ArrayList<Mapping>();
        addGlobal(new StringMapper(), String.class);
        addGlobal(new CharMapper(), Character.class, char.class);
        addGlobal(new NumberMapper(), Long.class, long.class, Integer.class,
                int.class, Short.class, short.class, Byte.class, byte.class);
        addGlobal(BASIC_MAPPER, Boolean.class, boolean.class, Double.class,
                double.class, Float.class, float.class);
        addGlobal(new EnumMapper(), Enum.class);
        addGlobal(new FileMapper(), File.class);
        addGlobal(new ReaderMapper(), Reader.class);
        addGlobal(new WriterMapper(), Writer.class);
        addGlobal(new InputStreamMapper(), InputStream.class);
        addGlobal(new OutputStreamMapper(), OutputStream.class);
        addGlobal(new RandomAccessFileMapper(), RandomAccessFile.class);
        addGlobal(new ArrayMapper(), Object[].class, boolean[].class,
                char.class, byte[].class, short[].class, int[].class,
                long[].class, float[].class, double[].class);
    }

    private static class Mapping {
        final Mapper mapper;
        final Set<Class> types;

        Mapping(Mapper mapper, Class... types) {
            if (mapper == null)
                throw new NullPointerException("mapper");
            for (Class type : types) {
                if (type == null)
                    throw new NullPointerException("type");
            }
            this.types = new HashSet<Class>(Arrays.asList(types));
            this.mapper = mapper;
        }
    }

    /**
     * This enumeration gives the options for what to do if the user specifies
     * <tt>--help</tt> or <tt>-?</tt> to get a usage message. See {@link
     * CommandLine#setHelpBehavior(HelpBehavior)} for details of how these
     * setting affect behavior.
     *
     * @see CommandLine#setHelpBehavior(HelpBehavior)
     */
    public enum HelpBehavior {
        /** Print the full usage message and then invoke {@link System#exit}(0). */
        PRINT_AND_EXIT,
        /** Return without printing anything. */
        RETURN,
        /** Print the full usage message and then return. */
        PRINT_AND_RETURN
    }

    /**
     * Creates a {@link CommandLine} for the given object.  The program name
     * will be the simple type name of the class of the owner object or, if the
     * owner object is a {@link Class} object, the object itself.  This is
     * equivalent to
     * <pre>
     * CommandLine(owner, null, null, null)
     * </pre>
     *
     * @param holder The object (or {@link Class} object) holder that has fields
     *               to be set.
     */
    public CommandLine(Object holder) {
        this(holder, null);
    }

    /**
     * Creates a {@link CommandLine} for the given object, specifying the
     * program name.  This is equivalent to
     * <pre>
     * CommandLine(owner, programName, null, null)
     * </pre>
     *
     * @param holder      The object (or {@link Class} object) holder that has
     *                    fields to be to be set.
     * @param programName The name for the program in usage messages.
     */
    public CommandLine(Object holder, String programName) {
        this(holder, programName, null);
    }

    /**
     * Creates a {@link CommandLine} for the given object, specifying the
     * program name and operand display.  This is equivalent to
     * <pre>
     * CommandLine(owner, programName, operandDesc, null)
     * </pre>
     *
     * @param holder         The object (or {@link Class} object) holder that
     *                       has fields to be fields to be set.
     * @param programName    The name for the program in usage messages.
     * @param operandDisplay The display for operands in the brief usage
     *                       message.
     */
    public CommandLine(Object holder, String programName,
            String operandDisplay) {
        this(holder, programName, operandDisplay, null);
    }

    /**
     * Creates a {@link CommandLine} for the given object, specifying the
     * program name, operand display, and program description.  This is
     * equivalent to
     *
     * @param holder         The object (or {@link Class} object) holder that
     *                       has fields to be fields to be set.
     * @param programName    The name for the program in usage messages.  If
     *                       this is <tt>null</tt>, the class's simple name will
     *                       be used.
     * @param operandDisplay The display for operands in the brief usage
     *                       message.  If this is <tt>null</tt>, <tt>"[ops
     *                       ...]"</tt> will be used.
     * @param programDesc    The description of this program for the full usage
     *                       message.  If this is <tt>null</tt>, there will be
     *                       no description used.
     */
    public CommandLine(Object holder, String programName, String operandDisplay,
            String programDesc) {

        if (holder == null)
            throw new NullPointerException("options");

        this.holder = holder;

        fields = new ArrayList<Field>();
        seen = new HashSet<Field>(fields.size());

        multiOptions = new HashMap<CharSequence, Field>();
        singleOptions = new HashMap<CharSequence, Field>();

        customMappings = new ArrayList<Mapping>();
        tryAllTypes = true;
        helpBehavior = PRINT_AND_EXIT;
        splitterPattern = SPLIT_PATTERN;
        splitterDisplay = SPLIT_PATTERN.pattern();

        boolean staticOnly = holder instanceof Class;
        Class type = (staticOnly ? (Class) holder : holder.getClass());

        if (programName == null)
            programName = type.getSimpleName();
        this.programName = programName;

        if (operandDisplay == null)
            operandDisplay = "[ops ...]";
        this.operandDisplay = operandDisplay;

        this.programDesc = programDesc;

        getFields(type, staticOnly);

        if (fields.size() == 0) {
            logFormat(logger, WARNING, "No %s attributes found in %s",
                    CommandOpt.class.getSimpleName(), type);
        }

        boolean singlesSpecified = false;
        for (Field field : fields) {
            CommandOpt anno = field.getAnnotation(CommandOpt.class);
            if (anno.single() != 0) {
                singlesSpecified = true;
                break;
            }
        }

        addSpecialFields(singlesSpecified);

        logFormat(logger, FINE, "using singles: %s", singlesSpecified);
        multiDashes = (singlesSpecified ? "--" : "-");
        for (Field field : fields) {
            CommandOpt anno = field.getAnnotation(CommandOpt.class);
            String multi = nameFor(field, anno);
            char single = anno.single();
            if (single == '\0' && multi == null) {
                throw new IllegalArgumentException(
                        "field has no multi or single option: " + field);
            }
            if (multi != null) {
                String option = multiDashes + multi;
                multiOptions.put(option, field);
            }
            if (single != '\0')
                singleOptions.put("-" + single, field);
            else
                multiOptions.put("--" + multi, field);
        }

        String colStr = getColStr("80");
        try {
            lineWidth = Integer.parseInt(colStr);
        } catch (NumberFormatException ignored) {
            lineWidth = 80;
        }
        optionDescriptionIndent = 24;
    }

    private void addSpecialFields(boolean singlesSpecified) {
        try {
            String name = singlesSpecified ? "singleHelpOnly" : "multiHelpOnly";
            Field helpField = getClass().getDeclaredField(name);
            fields.add(0, helpField);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                    "Cannot find Field object for help field!", e);
        }
        fields.add(OPTION_FILE_FIELD);
    }

    @SuppressWarnings({"CallToSystemGetenv"})
    private String getColStr(String colStr) {
        try {
            colStr = System.getenv("COLS");
            if (colStr == null)
                colStr = System.getenv("COLUMNS");
        } catch (SecurityException ignored) {
        }
        return colStr;
    }

    private static void addGlobal(Mapper mapper, Class... types) {
        GLOBAL_MAPPINGS.add(new Mapping(mapper, types));
    }

    private void getClassFields(Class type, Map<Character, Field> singles,
            boolean staticOnly) {

        if (type == null || type == Object.class)
            return;

        getClassFields(type.getSuperclass(), singles, staticOnly);

        try {
            for (Field field : type.getDeclaredFields()) {
                if (staticOnly && !Modifier.isStatic(field.getModifiers()))
                    continue;
                CommandOpt anno = field.getAnnotation(CommandOpt.class);
                if (anno != null)
                    addField(singles, field, anno);
            }
        } catch (SecurityException e) {
            logFormat(logger, INFO, "Not examining %s for @MainArg: %s", type,
                    e);
        }
    }

    @SuppressWarnings({"ParameterHidesMemberVariable"})
    private void getFields(Class type, boolean staticOnly) {
        Map<Character, Field> singles = new HashMap<Character, Field>();
        getClassFields(type, singles, staticOnly);
    }

    private void addField(Map<Character, Field> singles, Field field,
            CommandOpt anno) {

        if (Modifier.isFinal(field.getModifiers())) {
            throw new IllegalArgumentException(
                    "@MainArg not allowed on final field " + field);
        }
        logFormat(logger, FINE, "field: %s", field);
        try {
            field.setAccessible(true);
        } catch (SecurityException ignored) {
            logFormat(logger, FINE, "cannot set accessible: %s", field);
        }
        fields.add(field);
        if (anno.single() != '\0') {
            Field other = singles.get(anno.single());
            if (other == null)
                singles.put(anno.single(), field);
            else {
                throw new IllegalArgumentException(
                        "duplicate single with " + field.getName() + " and " +
                                other.getName());
            }
        }
    }

    /**
     * Apply the command line arguments to the object passed to the constructor.
     * When these have been applied, any default values specified in a {@link
     * CommandOpt#deflt} will be applied.
     *
     * @param args An array of arguments to apply.
     *
     * @return The list of operands that follow the specified options.
     */
    public List<String> apply(String... args) {
        int numOptions = applySome(args, 0, args.length);

        setDefaults();

        if (isHelpOnly()) {
            if (helpBehavior == PRINT_AND_EXIT ||
                    helpBehavior == PRINT_AND_RETURN)
                fullUsage(new OutputStreamWriter(System.out));
            if (helpBehavior == PRINT_AND_EXIT) {
                System.exit(0);
            }
        }

        List<String> operands = Arrays.asList(args).subList(numOptions,
                args.length);
        logFormat(logger, FINE, "operands start at %d: %s", numOptions,
                operands);
        return operands;
    }

    private void setDefaults() {
        logFormat(logger, FINE, "setting defaults for unspecified values");
        for (Field field : fields) {
            if (!seen.contains(field)) {
                CommandOpt anno = field.getAnnotation(CommandOpt.class);
                if (anno.deflt().equals(NO_DEFAULT)) {
                    logFormat(logger, FINER, "  no default: %s", field);
                } else {
                    setValue(anno.deflt(), field, null);
                }
            }
        }
    }

    /**
     * Apply some arguments to the object.  This does not apply defaults at the
     * end of processing.
     *
     * @param args  The command line arguments to apply.
     * @param start The first argument to use.
     * @param end   One past the last argument to use.
     *
     * @return The number arguments that are options.  If this is less than
     *         {@code args.length}, the rest of the elements of {@code args} are
     *         operands.
     */
    private int applySome(String[] args, int start, int end) {
        CharSequence[] seqs = new CharSequence[args.length];
        System.arraycopy(args, 0, seqs, 0, args.length);
        logFormat(logger, FINE, "applying args: %s", Arrays.asList(seqs));

        int i;
        int incr;
        for (i = start; i < end; i += incr) {
            CharSequence seq = seqs[i];
            logFormat(logger, FINE, "arg %2d: \"%s\"", i, seq);
            if (seq.length() == 0 || seq.charAt(0) != '-')
                break;
            if (seq.equals("--")) {
                i++;
                break;
            }

            boolean fromSingle = false;
            Field field = multiOptions.get(seq);

            CharSequence asSingle = null;
            if (field == null && singleOptions.size() > 0 && seq.charAt(1) !=
                    '-') {
                asSingle = seq.subSequence(0, 2);
                field = singleOptions.get(asSingle);
                fromSingle = field != null;
            }

            CharSequence specifiedAs = fromSingle ? asSingle : seq;

            if (field == null) {
                throw new CommandLineException(this,
                        "Unknown option: " + specifiedAs);
            }

            int incrVal; // var inside loop ensures compiler error if incr unset
            boolean usedAll = !fromSingle || seq.length() == 2;
            if (isBoolean(field)) {
                setValue(null, field, specifiedAs);
                incrVal = (usedAll ? 1 : 0);
                if (!usedAll) {
                    if (seq instanceof String) {
                        seqs[i] = seq = new StringBuilder(seq);
                    }
                    StringBuilder sb = (StringBuilder) seq;
                    sb.delete(1, 2);
                }
            } else {
                incrVal = (usedAll ? 2 : 1);
                CharSequence valStr;
                if (!usedAll) {
                    valStr = seq.subSequence(2, seq.length());
                } else {
                    if (i + 1 >= seqs.length) {
                        throw new CommandLineException(this,
                                specifiedAs + ": Missing value");
                    }
                    valStr = seqs[i + 1];
                }
                setValue(valStr, field, specifiedAs);
            }
            incr = incrVal;
            logFormat(logger, FINE, "increment is %d", incr);
        }
        return i;
    }

    /**
     * Returns the behavior this command line processor should have if the user
     * asks for the usage message using <tt>--help</tt> or <tt>-?</tt>. See
     * {@link #setHelpBehavior(HelpBehavior)} for details.
     *
     * @return <tt>true</tt> if this object should stop the VM if the user asks
     *         for the usage message.
     */
    public HelpBehavior getHelpBehavior() {
        return helpBehavior;
    }

    /**
     * Sets what this command line processor should do if the user asks for the
     * usage message using <tt>--help</tt> or <tt>-?</tt>.
     * <p/>
     * Users will expect that if they run a program with <tt>--help</tt> they
     * will get the usage message and nothing else will be affected.  Typically
     * they will not provide any other options or parameters.  Therefore, the
     * default for this feature is {@link HelpBehavior#PRINT_AND_EXIT
     * PRINT_AND_EXIT}, even though library code should rarely call {@link
     * System#exit}.  The status code value for the <tt>exit</tt> is zero
     * (printing a usage message is always successful).
     * <p/>
     * You should use {@link HelpBehavior#PRINT_AND_RETURN PRINT_AND_RETURN} if
     * you are parsing command lines from a part of the program or library that
     * shouldn't assume that the virtual machine is running for the sole purpose
     * of running your program. If you do, and a user uses <tt>--help</tt>, it
     * can bring down the entire Java virtual machine. Only the fact that
     * exiting is the correct behavior 99% of the time makes it reasonable to
     * have such a powerful side effect be the default behavior.
     * <p/>
     * You should use {@link HelpBehavior#RETURN RETURN} if you want to be able
     * to examine the options and operands that the user gave in order to
     * provide a customized usage message.  For example, if your program has
     * several major subcommands, you might want to be able to provide a help
     * message specialized for any subcommand the user specified.
     * <p/>
     * In both of the "return" cases, you can use {@link #isHelpOnly()} to find
     * out if the user asked for just a usage message.  Whatever you do, do not
     * use the "return" behaviors to do what you would have done had the user
     * not specified <tt>--help</tt> because the user will be very surprised,
     * and depending on what ends up happening, quite upset.
     *
     * @param helpBehavior The behavior to use.
     */
    public void setHelpBehavior(HelpBehavior helpBehavior) {
        if (helpBehavior == null)
            throw new NullPointerException("helpBehavior");
        this.helpBehavior = helpBehavior;
    }

    /**
     * Returns <tt>true</tt> if, after calling {@link #apply}, the options
     * <tt>--usage</tt> or <tt>-?</tt> were found.  This means that the user is
     * asking only for a usage message, which has been displayed.  If this
     * returns <tt>true</tt>, you should exit the program without any further
     * processing.
     *
     * @return <tt>true</tt> if the options asked for only help.
     */
    public boolean isHelpOnly() {
        return singleHelpOnly || multiHelpOnly;
    }

    /**
     * Returns <tt>true</tt> if any field type will be accepted that has a
     * single-string public constructor.
     */
    public boolean isTryAllTypes() {
        return tryAllTypes;
    }

    /**
     * Sets whether any field type will be accepted that has a single-string
     * public constructor.  The default is <tt>true</tt>.
     *
     * @param tryAllTypes <tt>false</tt> if types not explicitly in either
     *                    custom or globally-specified mapper is not given for
     *                    appear as field types.
     */
    public void setTryAllTypes(boolean tryAllTypes) {
        this.tryAllTypes = tryAllTypes;
    }

    /** Returns the line width used for wrapping the usage messages. */
    public int getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets the line width used for wrapping the usage messages.  The default is
     * to use the environment variable <tt>"COLS"</tt> or <tt>"COLUMNS"</tt> to
     * look for a numeric value.  If that is not available, the default is 80.
     * <p/>
     * A value of zero means not to reformat the lines at all.
     *
     * @param lineWidth The line width to use for wrapping usage messages.
     */
    public void setLineWidth(int lineWidth) {
        if (lineWidth < 0)
            throw new IllegalArgumentException("lineWidth < 0");
        this.lineWidth = lineWidth;
    }

    /**
     * Returns the indent used for descriptions of specific options in full
     * usage messages.
     */
    public int getOptionDescriptionIndent() {
        return optionDescriptionIndent;
    }

    /**
     * Sets the indent used for descriptions of specific options in full usage
     * messages.  The default is 24.
     *
     * @param optionDescriptionIndent The indent to use for descriptions of
     *                                specific options in full usage messages.
     */
    @SuppressWarnings({"MethodParameterNamingConvention"})
    public void setOptionDescriptionIndent(int optionDescriptionIndent) {
        this.optionDescriptionIndent = optionDescriptionIndent;
    }

    /** Returns the object that holds the options being set. */
    public Object getHolder() {
        return holder;
    }

    private void setValue(CharSequence valStr, Field field,
            CharSequence specifiedAs) {

        CommandOpt anno = field.getAnnotation(CommandOpt.class);
        setValue(valStr, field, field.getType(), anno, specifiedAs,
                specifiedAs == null);
    }

    private Object setValue(CharSequence valStr, Field field, Class type,
            CommandOpt anno, CharSequence specifiedAs, boolean asDefault) {

        if (specifiedAs == null) {
            if (field != null)
                specifiedAs = multiDashes + nameFor(field, anno);
            else
                specifiedAs = anno == null ? "" : anno.multi();
        }

        try {
            if (isBoolean(type)) {
                if (anno == null || anno.deflt().equals(NO_DEFAULT))
                    valStr = "true";
                else {
                    boolean b = Boolean.parseBoolean(anno.deflt());

                    // When setting defaults, specifiedAs is null, and in that
                    // case we want to invert the sense of the default. That is,
                    // if it specified on the command line, set to the opposite
                    // of the default
                    if (!asDefault)
                        b = !b;

                    valStr = String.valueOf(b);
                }
            }

            logFormat(logger, FINE, "setting %s to %s",
                    field != null ? field.getName() : type, valStr);
            Mapper mapper = mapperFor(type);
            Object val = mapper.map(valStr, field, type, anno, this);
            Object onObject = holder;
            if (field != null) {
                if (field.getDeclaringClass() == CommandLine.class)
                    onObject = this;
                field.set(onObject, val);
                seen.add(field);
            }
            if (anno != null) {
                for (String action : COMMAS.split(anno.checks())) {
                    runCheck(mapper, action, val, field, type, anno,
                            specifiedAs);
                }
            }

            //noinspection ObjectEquality
            if (field == OPTION_FILE_FIELD)
                consumeOptionFile();
            return val;
        } catch (CommandLineException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandLineException(this,
                    specifiedAs + ": " + "Cannot set to \"" + valStr + '"', e);
        }
    }

    private void consumeOptionFile() throws IOException {
        // Reading arguments from files may recurse: The options in the file
        // may have its own "-@".  So this has to be written to be safe for
        // that.  Which is why, for example, we get the value of the path
        // once, before reading any options
        File path = optionFile;
        logFormat(logger, FINE, "vvvv--- Reading options from " + path);
        FileReader in = null;
        StringBuilder sb = new StringBuilder();
        try {
            in = new FileReader(path);
            char[] buf = new char[1024 * 8];
            int len;
            while ((len = in.read(buf)) > 0) {
                sb.append(buf, 0, len);
            }
        } finally {
            if (in != null)
                in.close();
        }

        String[] args = OPTION_FILE_SPLIT.split(sb);

        int start = 0;
        if (args.length > start && args[0].length() == 0)
            start = 1;

        int end = args.length;
        if (args.length > 0 && args[end - 1].length() == 0)
            end = args.length - 1;

        for (int i = start; i < end; i++) {
            args[i] = ParseStringUtil.parseString(args[i]);
        }

        applySome(args, start, end);
        logFormat(logger, FINE, "vvvv--- Done reading options from " + path);
    }

    private void runCheck(Mapper mapper, String check, Object val, Field field,
            Class type, CommandOpt anno, CharSequence specifiedAs) {

        check = check.trim();

        // nothing to do
        if (check.length() == 0)
            return;

        boolean opposite = check.charAt(0) == '!';
        if (opposite)
            check = check.substring(1).trim();

        if (check.length() == 0)
            throw new IllegalArgumentException("'!' without check");

        try {
            Class retType;
            Object ret = mapper.check(check, val, field, type, anno, this);
            if (ret == null) {
                Method m = type.getMethod(check);
                retType = m.getReturnType();
                if (isBoolean(retType) || retType == Void.TYPE)
                    ret = m.invoke(val);
            } else {
                retType = ret.getClass();
            }
            if (isBoolean(retType) && ((Boolean) ret) == opposite) {
                throw new CommandLineException(this,
                        specifiedAs + ": " + val + " fails check: " +
                                (opposite ? "!" : "") + check);
            }
        } catch (CommandLineException e) {
            throw e;
        } catch (Exception e) {
            if (specifiedAs == null)
                specifiedAs = "";
            throw new CommandLineException(this,
                    specifiedAs + ": " + check + " causes exception", e);
        }
    }

    private static boolean isBoolean(Class type) {
        return type == Boolean.class || type == boolean.class;
    }

    private Mapper mapperFor(Class<?> type) {
        Mapper mapper = findMapper(customMappings, type);
        if (mapper == null)
            mapper = findMapper(GLOBAL_MAPPINGS, type);

        if (mapper != null)
            return mapper;

        if (tryAllTypes)
            return BASIC_MAPPER;
        else {
            throw new IllegalArgumentException(
                    "Unknown type for argument: " + type);
        }
    }

    private Mapper findMapper(Iterable<Mapping> mappings, Class type) {
        for (Mapping mapping : mappings) {
            for (Class mapType : mapping.types) {
                if (mapType.isAssignableFrom(type)) {
                    return mapping.mapper;
                }
            }
        }
        return null;
    }

    private static boolean isBoolean(Field field) {
        return isBoolean(field.getType());
    }

    /**
     * Write the full usage message to the given writer.  The full message
     * includes the brief message, plus any description for the program and
     * those for the options.
     *
     * @param writer The writer to use.
     */
    public void fullUsage(Writer writer) {
        writeUsage(writer, true);
    }

    /**
     * Write the full usage message to the given output stream.  The full
     * message includes the brief message, plus any description for the program
     * and those for the options.
     *
     * @param out The output stream to use.
     */
    public void fullUsage(OutputStream out) {
        OutputStreamWriter osw = new OutputStreamWriter(out);
        writeUsage(osw, true);
        try {
            osw.flush();
        } catch (IOException ignored) {
        }
    }

    /**
     * Write the brief usage message to the given writer.  The brief message
     * includes just the command line syntax.
     *
     * @param writer The writer to use.
     */
    public void briefUsage(Writer writer) {
        writeUsage(writer, false);
    }

    /**
     * Write the brief usage message to the given output stream.  The brief
     * message includes just the command line syntax.
     *
     * @param out The output stream to use.
     */
    public void briefUsage(OutputStream out) {
        OutputStreamWriter osw = new OutputStreamWriter(out);
        writeUsage(osw, false);
        try {
            osw.flush();
        } catch (IOException ignored) {
        }
    }

    private void writeUsage(Writer writer, boolean fullUsage) {
        PrintWriter out;

        if (writer instanceof PrintWriter)
            out = (PrintWriter) writer;
        else
            out = new PrintWriter(writer);

        StringBuffer sb = new StringBuffer();

        sb.append(programName);
        boolean hasOptionDesc = false;
        int colWidth = 0;
        for (Field field : fields) {
            //noinspection ObjectEquality
            if (field == OPTION_FILE_FIELD)
                continue;            // skip the '@' pseudo-field

            sb.append(' ');
            sb.append('[');

            int before = sb.length();
            CommandOpt anno = writeOption(sb, field);
            int after = sb.length();
            colWidth = Math.max(after - before, colWidth);

            sb.append(']');
            hasOptionDesc |= anno.desc().length() > 0;
        }
        sb.append(" ");
        sb.append(operandDisplay);

        StringBuffer str = format(sb, OPTION_SPLIT, 4);
        out.write(str.toString());

        if (fullUsage) {
            if (programDesc != null && programDesc.length() > 0) {
                sb.replace(0, sb.length(), programDesc);
                str = format(sb, SPACE_SPLIT, 0);
                out.write('\n');
                out.write(str.toString());
            }

            if (hasOptionDesc) {
                // add 2 to have space between desc width and the desc
                colWidth = Math.min(colWidth + 2, optionDescriptionIndent);
                out.write('\n');
                for (Field field : fields) {
                    //noinspection ObjectEquality
                    if (field == OPTION_FILE_FIELD)
                        continue;            // skip the '@' pseudo-field

                    out.write('\n');

                    sb.delete(0, sb.length());
                    CommandOpt anno = writeOption(sb, field);
                    int thisLen = sb.length();

                    if (anno.desc().length() == 0) {
                        str = sb;
                    } else {
                        if (thisLen >= colWidth) {
                            sb.append('\n');
                            thisLen = 0;
                        }
                        for (int i = thisLen; i < colWidth; i++)
                            sb.append(' ');

                        sb.append(anno.desc());
                        str = format(sb, SPACE_SPLIT, colWidth);
                    }
                    out.write(str.toString());
                }
            }
        }
        out.write('\n');

        out.flush();
    }

    private CommandOpt writeOption(StringBuffer sb, Field field) {
        CommandOpt anno = field.getAnnotation(CommandOpt.class);

        String multi = nameFor(field, anno);
        char single = anno.single();

        boolean hasSingle = single != '\0';
        boolean hasMulti = !multi.equals(NO_MULTI);
        if (hasSingle) {
            sb.append('-');
            sb.append(single);
        }
        if (hasSingle && hasMulti)
            sb.append('|');
        if (hasMulti) {
            sb.append(multiDashes);
            sb.append(multi);
        }
        if (!isBoolean(field)) {
            CharSequence valDesc = anno.valueName();
            if (valDesc.length() == 0) {
                Mapper mapper = mapperFor(field.getType());
                valDesc = mapper.defaultValueName(field, field.getType(), anno,
                        this);
            }
            sb.append(' ');
            sb.append(valDesc);
        }

        return anno;
    }

    private StringBuffer format(StringBuffer sb, Pattern split,
            int lineStartIndent) {

        if (lineWidth == 0)
            return sb;

        StringBuilder indentB = new StringBuilder("\n");
        for (int i = 0; i < lineStartIndent; i++)
            indentB.append(' ');
        String indent = indentB.toString();

        Matcher m = split.matcher(sb);
        StringBuffer nb = new StringBuffer(sb.length());
        int linePos = 0;
        int prevLen = 0;
        boolean found;
        do {
            found = m.find();
            int before = nb.length();
            if (found)
                m.appendReplacement(nb, m.group());
            else
                m.appendTail(nb);
            int after = nb.length();
            int thisLen = after - before;
            if (linePos + thisLen > lineWidth) {
                nb.replace(before - prevLen, before, indent);
                linePos = lineStartIndent + thisLen - 1;
            } else {
                linePos += thisLen;
            }
            if (found)
                prevLen = m.group().length();
        } while (found);
        return nb;
    }

    private static String nameFor(Field field, CommandOpt anno) {
        String multi = anno.multi();
        if (multi.equals(NO_MULTI))
            return null;
        if (multi.length() > 0)
            return multi;
        return field.getName();
    }

    /**
     * Returns knobs used for advanced features.  Most users will not need
     * these.  They let you add custom mappers, and to decode string values as
     * if they were to be assigned to options.
     *
     * @return Knobs for advanced features.
     */
    public CommandLineAdvanced getAdvancedKnobs() {
        return new CommandLineAdvanced() {
            @Override
            public void addMapping(Mapper mapper, Class... types) {
                customMappings.add(new Mapping(mapper, types));
            }

            @Override
            public Object getValueFor(String valueStr, Class type,
                    CommandOpt anno) {

                if (valueStr == null)
                    throw new NullPointerException("valueStr");
                if (type == null)
                    throw new NullPointerException("type");
                return setValue(valueStr, null, type, anno, null, false);
            }

            @Override
            public Mapper getMapperFor(Class<?> type) {
                if (type == null)
                    throw new NullPointerException("type");
                return mapperFor(type);
            }

            @Override
            public Pattern getValueSplitter() {
                return splitterPattern;
            }

            @Override
            public String getValueSplitterDisplay() {
                return splitterDisplay;
            }

            @Override
            public void setValueSplitter(Pattern splitterPattern,
                    String splitterDisplay) {

                if (splitterPattern == null)
                    throw new NullPointerException("splitterPattern");
                if (splitterDisplay == null)
                    throw new NullPointerException("splitterDisplay");
                CommandLine.this.splitterPattern = splitterPattern;
                CommandLine.this.splitterDisplay = splitterDisplay;
            }
        };
    }
}
