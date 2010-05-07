package com.im.file;

import com.im.file.Bang.Parser;
import static com.im.file.ComprDesc.compr;
import static com.im.file.InternalUtils.*;
import static com.im.file.Magic.*;
import static com.im.util.CLib.*;
import com.im.util.ThreadedCopy;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.simplx.c.Pointer;
import org.simplx.c.Stdio;
import static org.simplx.c.Stdio.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * These functions operate on the magic database file which is described in
 * magic(4).  It is modele on libmagic(3).
 *
 * @author Måns Rullgård: Initial libmagic implementation, and configuration.
 * @author Christos Zoulas: API cleanup, error code and allocation handling.
 * @author Ken Arnold: Original Java translation.
 */
public class MagicSet implements Closeable {

    /** No flags. */
    public static final int MAGIC_NONE = 0x000000;
    /** Turn on debugging. */
    public static final int MAGIC_DEBUG = 0x000001;
    /** Follow symlinks. */
    public static final int MAGIC_SYMLINK = 0x000002;
    /** Check inside compressed files. */
    public static final int MAGIC_COMPRESS = 0x000004;
    /** Look at the contents of devices. */
    public static final int MAGIC_DEVICES = 0x000008;
    /** Return the MIME type. */
    public static final int MAGIC_MIME_TYPE = 0x000010;
    /** Return all matches. */
    public static final int MAGIC_CONTINUE = 0x000020;
    /** Print warnings to stderr. */
    public static final int MAGIC_CHECK = 0x000040;
    /**
     * Restore access time on exit. This is not public because Java cannot do
     * this.
     */
    static final int MAGIC_PRESERVE_ATIME = 0x000080;
    /** Don't translate unprintable chars. */
    public static final int MAGIC_RAW = 0x000100;
    /** Handle ENOENT etc as real errors. */
    public static final int MAGIC_ERROR = 0x000200;
    /** Return the MIME encoding. */
    public static final int MAGIC_MIME_ENCODING = 0x000400;
    static final int MAGIC_MIME = (MAGIC_MIME_TYPE | MAGIC_MIME_ENCODING);
    /** Return the Apple creator and type. */
    static final int MAGIC_APPLE = 0x000800;
    /** Don't check for compressed files. */
    public static final int MAGIC_NO_CHECK_COMPRESS = 0x001000;
    /** Don't check for tar files. */
    public static final int MAGIC_NO_CHECK_TAR = 0x002000;
    /** Don't check magic entries. */
    public static final int MAGIC_NO_CHECK_SOFT = 0x004000;
    /** Don't check application type. */
    public static final int MAGIC_NO_CHECK_APPTYPE = 0x008000;
    /**
     * Don't check for ELF details.  This is not public because ELF is not yet
     * implemented.
     */
    public static final int MAGIC_NO_CHECK_ELF = 0x010000;
    /** Don't check for text files. */
    public static final int MAGIC_NO_CHECK_TEXT = 0x020000;
    /** Don't check for cdf files. */
    static final int MAGIC_NO_CHECK_CDF = 0x040000;
    /** Don't check tokens. */
    public static final int MAGIC_NO_CHECK_TOKENS = 0x100000;
    /** Don't check text encodings. */
    static final int MAGIC_NO_CHECK_ENCODING = 0x200000;

    /* Defined for backwards compatibility (renamed) */
    public static final int MAGIC_NO_CHECK_ASCII = MAGIC_NO_CHECK_TEXT;

    /* Defined for backwards compatibility; do nothing */
    /** Don't check ascii/fortran. */
    public static final int MAGIC_NO_CHECK_FORTRAN = 0x000000;
    /** Don't check ascii/troff. */
    public static final int MAGIC_NO_CHECK_TROFF = 0x000000;

    static final int EVENT_HAD_ERR = 0x01;

    static final int MAGICNO = 0xcafef11e;
    static final int VERSIONNO = 1;
    static final int FILE_MAGICSIZE = 200;

    static final int FILE_LOAD = 0;
    static final int FILE_CHECK = 1;
    static final int FILE_COMPILE = 2;

    private static final String EXT = ".mjc";

    private static final String MAGIC = "Magdir" + EXT;

    /** max entries in any one magic file; or directory */
    static final int MAXMAGIS = 8192;
    /** max leng of text description/MIME type. */
    static final int MAXDESC = 64;

    static final char CHAR_COMPACT_BLANK = 'B';
    static final char CHAR_COMPACT_OPTIONAL_BLANK = 'b';
    static final char CHAR_IGNORE_LOWERCASE = 'c';
    static final char CHAR_IGNORE_UPPERCASE = 'C';
    static final char CHAR_REGEX_OFFSET_START = 's';

    static final int STRING_COMPACT_BLANK = BIT(0);
    static final int STRING_COMPACT_OPTIONAL_BLANK = BIT(1);
    static final int STRING_IGNORE_LOWERCASE = BIT(2);
    static final int STRING_IGNORE_UPPERCASE = BIT(3);
    static final int REGEX_OFFSET_START = BIT(4);
    static final int STRING_IGNORE_CASE =
            (STRING_IGNORE_LOWERCASE | STRING_IGNORE_UPPERCASE);
    static final int STRING_DEFAULT_RANGE = 100;

    private static final Bang[] bang = {
            new Bang("mime", new Parser() {
                public int parse(MagicSet ms, MagicEntry me, Pointer l) {
                    return ms.parse_mime(me, l);
                }
            }), new Bang("apple", new Parser() {
                public int parse(MagicSet ms, MagicEntry me, Pointer l) {
                    return ms.parse_apple(me, l);
                }
            }), new Bang("strength", new Parser() {
                public int parse(MagicSet ms, MagicEntry me, Pointer l) {
                    return ms.parse_strength(me, l);
                }
            }),
    };

    private static final String usg_hdr =
            "cont\toffset\ttype\topcode\tmask\tvalue\tdesc";

    private static final Pattern TEXT_PATTERN = Pattern.compile("\\btext\\b");
    static final Pattern C_FMT_PATTERN = Pattern.compile(
            "(?<!%)(%%*%)((-?[0-9]+)|\\*)?(\\.)?((-?[0-9]+)|\\*)?([a-z]+)");

    private static final int NODATA = ~0;
    private static final int MAXLINELEN = 300;
    /** how much of the file to look at. */
    public static final int HOWMANY = (256 * 1024);

    private int last_cont_level = 0;
    int[] file_formats = new int[FILE_NAMES_SIZE];
    String[] file_names = new String[FILE_NAMES_SIZE];

    private MList mlist = new MList();
    private final Cont c = new Cont();
    private final Out o = new Out();
    private int offset;
    private int error;
    int flags;
    private int event_flags;
    private String file;
    private int line;
    private final Search search = new Search();
    private final ValueType ms_value = new ValueType();
    private File outDir;

    private static class Cont {
        List<LevelInfo> li;
    }

    private static class Out {
        final StringBuilder buf = new StringBuilder();
        final Stdio bufPrintf = new Stdio(new Formatter(buf), true);
        final StringBuilder pbuf = new StringBuilder();
        final Stdio pbufPrintf = new Stdio(new Formatter(pbuf), true);
    }

    private static class Search {
        Pointer s;
        int s_len;
        int offset;
        int rm_len;
    }

    MagicSet() {
    }

    /**
     * Creates a {@link MagicSet} object and returns it. The {@code flags}
     * argument specifies how the other magic functions should behave.
     *
     * @param flags The flags that will affect the behavior of the searching.
     *
     * @return A {@link MagicSet} object.
     */
    public static MagicSet open(int flags) {
        MagicSet ms;

        ms = new MagicSet();

        ms.setflags(flags);

        ms.c.li = new ArrayList<LevelInfo>();

        ms.event_flags = 0;
        ms.error = -1;
        ms.mlist = null;
        ms.file = "unknown";
        ms.line = 0;
        return ms;
    }

    /** Closes the magic database and deallocates any resources used. */
    @Override
    public void close() {
        // Nothing to do; this maintains compatibility with original API, plus
        // leaves a hook for the future
    }

    /**
     * Returns a textual explanation of the last error, or {@code null} if there
     * was no error.
     *
     * @return A textual explanation of the last error, or {@code null} if there
     *         was no error.
     */
    public String error() {
        return ((event_flags & EVENT_HAD_ERR) != 0) ? new String(o.buf) : null;
    }

    /**
     * Returns a textual description of the contents of the filename argument,
     * or {@code null} if an error occurred.  If the filename is {@code null},
     * then {@code System.in} is used.
     *
     * @param filename The name of the file to examine, or {@code null} to use
     *                 {@code System.in}.
     *
     * @return A description of the file's type, or {@code null}.
     */
    public String file(String filename) {
        return file_or_fd(filename, System.in);
    }

    /**
     * Returns a textual description of the contents of the first {@code length}
     * bytes from the buffer argument.
     *
     * @param buffer The buffer to examine.
     * @param length The first {@code length} bytes will be examined.
     *
     * @return A description of the buffer's file type, or {@code null}.
     */
    public String buffer(byte[] buffer, int length) {
        if (file_reset() == -1) {
            return null;
        }

        /*
          * The main work is done here!
          * We have the file name and/or the data buffer to be identified.
          */
        if (file_buffer(null, null, buffer, length) == -1) {
            return null;
        }
        return file_getbuffer();
    }

    /**
     * Returns a textual description of the contents of the frst {@code length}
     * bytes of a stream.
     *
     * @param stream The buffer to examine.
     *
     * @return A description of the buffer's file type, or {@code null}.
     */
    public String stream(InputStream stream) {
        return file_or_fd(null, stream);
    }

    /**
     * Sets the flags described, as in the constructor. Note that using both
     * MIME flags together can also return extra information on the charset.
     *
     * @param flags The new flags.
     *
     * @see #open(int)
     */
    public void setflags(int flags) {
        this.flags = flags;
    }

    /**
     * This function can be used to check the validity of entries in the list of
     * database files passed in as {@code filename}, or {@code null} to validate
     * the default database.
     *
     * @param file       The name of the first file to examine, or {@code null}
     *                   for the default database.
     * @param otherFiles Any other filenames to examine, or {@code null} for the
     *                   default database.
     *
     * @return {@code true} on success and {@code false} on failure.
     */
    public boolean check(String file, String... otherFiles) {
        MagicSet ms = new MagicSet();
        MList ml = ms.file_apprentice(FILE_CHECK, arrayFor(file, otherFiles));
        return ml != null;
    }

    /**
     * Compiles the list of database files passed in {@code filenames}, or
     * {@code null} for the default database. The compiled files created are
     * named from the base name (name with any suffix removed) of each file
     * argument with {@code .mjc} appended to it.
     *
     * @param file       The name of the first file to examine, or {@code null}
     *                   for the default database.
     * @param otherFiles Any other filenames to examine, or {@code null} for the
     *                   default database.
     *
     * @return {@code true} on success and {@code false} on failure.
     */
    public boolean compile(String file, String... otherFiles) {
        outDir = null;
        MList ml = file_apprentice(FILE_COMPILE, arrayFor(file, otherFiles));
        return ml != null;
    }

    /**
     * Compiles the list of database files passed in {@code filenames}, or
     * {@code null} for the default database, putting the output in a specified
     * directory. The compiled files created are named from the base name (name
     * with any suffix removed) of each file argument with {@code .mjc} appended
     * to it.
     *
     * @param file       The name of the first file to examine, or {@code null}
     *                   for the default database.
     * @param otherFiles Any other filenames to examine, or {@code null} for the
     *                   default database.
     *
     * @return {@code true} on success and {@code false} on failure.
     */
    public boolean compileTo(String outDir, String file, String... otherFiles) {
        this.outDir = new File(outDir);
        MList ml = file_apprentice(FILE_COMPILE, arrayFor(file, otherFiles));
        return ml != null;
    }

    /**
     * Loads the list of database files in {@code filename}, or {@code null} for
     * the default database file before any magic queries can performed.  The
     * property {@code MAGIC} can be used to override the default database to be
     * a specified file.  This method adds {@code .mjc} to the database filename
     * as appropriate.
     *
     * @param file       The name of the first file to examine, or {@code null}
     *                   for the default database.
     * @param otherFiles Any other filenames to examine, or {@code null} for the
     *                   default database.
     *
     * @return 0 on success and -1 on failure.
     */
    public boolean load(String file, String... otherFiles) {
        MList ml = file_apprentice(FILE_LOAD, arrayFor(file, otherFiles));
        if (ml != null) {
            mlist = ml;
            return true;
        }
        return false;
    }

    private static String[] arrayFor(String first, String... others) {
        String[] array = new String[others.length + 1];
        array[0] = first;
        System.arraycopy(others, 0, array, 1, others.length);
        return array;
    }

    private String file_or_fd(String inname, InputStream fd) {
        byte[] buf;
        int nbytes = 0;        /* number of bytes read from a datafile */
        File sb = null;
        int errno;
        boolean opened = false;

        /*
         * some overlapping space for matches near EOF
         */
        buf = new byte[HOWMANY + 100];

        if (file_reset() == -1) {
            return null;
        }

        if (inname != null && !inname.equals("-")) {
            sb = new File(inname);
        }

        switch (file_fsmagic(inname, sb)) {
        case -1:                /* error */
            return null;
        case 0:                        /* nothing found */
            break;
        default:                /* matched it and printed type */
            return file_getbuffer();
        }

        errno = 0;
        try {
            if (inname != null) {
                try {
                    fd = new FileInputStream(inname);
                    opened = true;
                } catch (FileNotFoundException e) {
                    if (sb.exists()) {
                        if (unreadable_info(sb) == -1) {
                            return null;
                        }
                    } else {
                        file_error(0, "No such file or directory");
                    }
                    return file_getbuffer();
                }
            }

            /*
             * try looking at the first HOWMANY bytes
             */
            try {
                nbytes = fd.read(buf, 0, HOWMANY);

                if (nbytes < 0) {
                    // this marks an immediate EOF
                    nbytes = 0;
                }
            } catch (IOException e) {
                nbytes = -1;
            }
            if (nbytes < 0) {
                file_error(errno, "cannot read `%s'", inname);
                return null;
            }

            if (file_buffer(fd, inname, buf, nbytes) == -1) {
                return null;
            }
            return file_getbuffer();
        } finally {
            if (opened) {
                IOUtils.closeQuietly(fd);
            }
        }
    }

    private int unreadable_info(File sb) {
        /* We cannot open it, but we were able to stat it. */
        if (sb.canWrite()) {
            if (file_printf("writable, ") == -1) {
                return -1;
            }
        }
        if (sb.canExecute()) {
            if (file_printf("executable, ") == -1) {
                return -1;
            }
        }
        if (sb.isFile()) {
            if (file_printf("regular file, ") == -1) {
                return -1;
            }
        }
        if (file_printf("no read permission") == -1) {
            return -1;
        }
        return 0;
    }

    MList file_apprentice(int action, String... files) {
        int file_err, errs = -1;
        MList mlist;

        init_file_tables();

        mlist = new MList();
        mlist.next = mlist.prev = mlist;

        for (String file : files) {
            if (file == null) {
                file = System.getProperty("MAGIC");
            }
            file_err = apprentice_1(file, action, mlist);
            errs = Math.max(errs, file_err);
        }
        if (errs == -1) {
            file_error(0, "could not find any magic files!");
            return null;
        }
        return mlist;
    }

    private int parse_mime(MagicEntry me, Pointer line) {
        int i;
        Pointer l = line.copy();
        Magic m = me.mp.get(me.cont_count == 0 ? 0 : me.cont_count - 1);

        if (m.mimetype.get() != '\0') {
            file_magwarn("Current entry already has a MIME type `%s'," +
                    " new type `%s'", m.mimetype, l);
            return -1;
        }

        l = eatab(l);
        for (i = 0; l.get() != '\0' && ((isascii(l.get()) && isalnum(
                l.get())) || strchr("-+/.", l.get()) != null) && i < MAXDESC;
             m.mimetype.set(i++, l.getIncr())) {
            continue;
        }
        if (i == MAXDESC && m.mimetype.get(MAXDESC - 1) != '\0') {
            m.mimetype.set(MAXDESC - 1, '\0');
            if ((flags & MAGIC_CHECK) != 0) {
                file_magwarn("MIME type `%s' truncated %zu", m.mimetype, i);
            }
        } else {
            m.mimetype.set(i, '\0');
        }

        if (i > 0) {
            return 0;
        } else {
            return -1;
        }
    }

    private int parse_apple(MagicEntry me, Pointer line) {
        int i;
        Pointer l = line.copy();
        Magic m = me.mp.get(me.cont_count == 0 ? 0 : me.cont_count - 1);

        if (m.apple.get() != '\0') {
            file_magwarn("Current entry already has a APPLE type `%.8s'," +
                    " new type `%s'", m.mimetype, l);
            return -1;
        }

        l = eatab(l);
        for (i = 0; l.get() != '\0' && ((isascii(l.get()) && isalnum(
                l.get())) || strchr("-+/.", l.get()) != null) && i < MAXAPPLE;
             m.apple.set(i++, l.getIncr())) {
            continue;
        }
        if (i == MAXAPPLE && l.get() != '\0') {
            if ((flags & MAGIC_CHECK) != 0) {
                file_magwarn("APPLE type `%s' truncated %d", line, i);
            }
        }

        if (i > 0) {
            return 0;
        } else {
            return -1;
        }
    }

    private int parse_strength(MagicEntry me, Pointer lOrig) {
        Pointer l = lOrig.copy();
        Pointer[] t;
        Pointer el;
        long factor;
        Magic m = me.mp.get(0);

        if (m.factor_op != FILE_FACTOR_OP_NONE) {
            file_magwarn("Current entry already has a strength type: %c %d",
                    m.factor_op, m.factor);
            return -1;
        }
        l = eatab(l);
        switch (l.get()) {
        case FILE_FACTOR_OP_NONE:
        case FILE_FACTOR_OP_PLUS:
        case FILE_FACTOR_OP_MINUS:
        case FILE_FACTOR_OP_TIMES:
        case FILE_FACTOR_OP_DIV:
            m.factor_op = l.getIncr();
            break;
        default:
            file_magwarn("Unknown factor op `%c'", l.get());
            return -1;
        }
        l = eatab(l);
        t = new Pointer[1];
        factor = strtoul(l, t, 0);
        el = t[0];
        if (factor > 255) {
            file_magwarn("Too large factor `%lu'", factor);
        } else if (el.get() != '\0' && !isspace(el.get())) {
            file_magwarn("Bad factor `%s'", l);
        } else if (m.factor == 0 && m.factor_op == FILE_FACTOR_OP_DIV) {
            file_magwarn("Cannot have factor op `%c' and factor %u",
                    m.factor_op, m.factor);
        } else {
            // No errors, set the values
            m.factor = (byte) factor;
            return 0;
        }

        // we get here only if we warned about something
        m.factor_op = FILE_FACTOR_OP_NONE;
        m.factor = 0;
        return -1;
    }

    private void file_error_core(int error, String f, Object[] va, int lineno) {
        /* Only the first error is ok */
        if ((event_flags & EVENT_HAD_ERR) != 0) {
            return;
        }
        if (lineno != 0) {
            o.buf.delete(0, o.buf.length());
            file_printf("line %u: ", lineno);
        }
        file_vprintf(f, va);
        if (error > 0) {
            file_printf(" (%s)", strerror(error));
        }
        event_flags |= EVENT_HAD_ERR;
        this.error = error;
    }

    int file_printf(String fmt, Object... va) {
        return file_vprintf(fmt, va);
    }

    int file_printf(Pointer fmt, Object... va) {
        return file_printf(fmt.toString(), va);
    }

    private int file_vprintf(String fmt, Object[] va) {
        o.bufPrintf.printf(fmt, va);
        return 0;
    }

    void file_error(int error, String f, Object... args) {
        file_error_core(error, f, args, 0);
    }

    private void file_magerror(int error, String f, Object... args) {
        file_error_core(error, f, args, line);
    }

    private int apprentice_1(String fn, int action, MList mlist) {
        Magic[][] magic = new Magic[1][];
        int[] nmagic = new int[1];
        MList ml;
        int rv = -1;
        int mapped;

        if (action == FILE_COMPILE) {
            rv = apprentice_load(magic, nmagic, fn, action);
            if (rv != 0) {
                return -1;
            }
            rv = apprentice_compile(magic, nmagic, fn);
            return rv;
        }

        if ((rv = apprentice_map(magic, nmagic, fn)) == -1) {
            if ((flags & MAGIC_CHECK) != 0) {
                file_magwarn("using regular magic file `%s'", fn);
            }
            rv = apprentice_load(magic, nmagic, fn, action);
            if (rv != 0) {
                return -1;
            }
        }

        mapped = rv;

        if (magic == null) {
            return -1;
        }

        ml = new MList();
        ml.magic = magic[0];
        ml.nmagic = nmagic[0];
        ml.mapped = mapped;

        mlist.prev.next = ml;
        ml.prev = mlist.prev;
        ml.next = mlist;
        mlist.prev = ml;

        return 0;
    }

    private void file_magwarn(String f, Object... va) {
        /* cuz we use stdout for most, stderr here */
        stdout.flush();

        if (file != null) {
            stderr.printf("%s, %d: ", file, line);
        }
        stderr.printf("Warning: ");
        stderr.printf(f, va);
        stderr.printf("\n");
    }

    private int apprentice_map(Magic[][] magic, int[] nmagic, String fn) {
        InputStream fin = null;
        DataInputStream din = null;
        ObjectInputStream oin = null;
        String dbname = null;
        if (fn != null) {
            dbname = mkdbname(fn, 0);
        }
        try {
            try {
                if (dbname != null) {
                    fin = new FileInputStream(dbname);
                } else {
                    fin = getClass().getResourceAsStream(MAGIC);
                }
                din = new DataInputStream(fin);

                checkMatch(din, "Bad magic number in " + dbname, MAGICNO);
                checkMatch(din, "Version mismatch in " + dbname, VERSIONNO);
            } catch (Exception e) {
                return -1;
            }

            try {
                oin = new ObjectInputStream(fin);
                magic[0] = (Magic[]) oin.readObject();
                if ((flags & MAGIC_DEBUG) != 0) {
                    for (int i = 0; i < magic[0].length; i++) {
                        stdout.printf("%d: %s\n", i, magic[0][i]);
                    }
                }
                nmagic[0] = magic[0].length;
                return 1;
            } catch (Exception e) {
                throw new IllegalStateException("Corrupt magic file " + dbname,
                        e);
            }
        } finally {
            IOUtils.closeQuietly(fin);
        }
    }

    private void checkMatch(DataInputStream din, String msg, int expected)
            throws IOException {
        int fv = din.readInt();
        if (fv != expected) {
            file_error(0, msg + ": expected 0x%08x, got 0x%08x\n", expected,
                    fv);
        }
    }

    private int apprentice_compile(Magic[][] magic, int[] nmagic, String fn) {
        String dbname;

        dbname = mkdbname(fn, 1);

        if (dbname == null) {
            return -1;
        }

        FileOutputStream fout = null;
        DataOutputStream dout = null;
        ObjectOutputStream oout = null;
        try {
            fout = new FileOutputStream(dbname);
            dout = new DataOutputStream(fout);

            dout.writeInt(MAGICNO);
            dout.writeInt(VERSIONNO);
            dout.flush();

            oout = new ObjectOutputStream(fout);
            oout.writeObject(magic[0]);
            if ((flags & MAGIC_DEBUG) != 0) {
                for (int i = 0; i < magic[0].length; i++) {
                    stdout.printf("%d: %s\n", i, magic[0][i]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            file_error(0, "Error on file \"%s\": %s", dbname, e.getMessage());
        } finally {
            IOUtils.closeQuietly(fout);
            IOUtils.closeQuietly(dout);
            IOUtils.closeQuietly(oout);
        }

        return 0;
    }

    private String mkdbname(String fn, int strip) {
        fn = getPath(fn);
        if (strip != 0) {
            fn = FilenameUtils.getBaseName(fn);
        }

        if (outDir != null) {
            String base = FilenameUtils.getBaseName(fn);
            fn = getPath(new File(outDir, base).toString());
        }

        /* Compatibility with old code that looked in .mime */
        if ((flags & MAGIC_MIME) != 0) {
            StringBuilder sb = new StringBuilder(fn);
            if (fn.endsWith(EXT)) {
                sb.delete(sb.length() - EXT.length(), sb.length());
            }
            sb.append(".mime").append(EXT);
            String dbname = sb.toString();
            File memPath = new File(dbname);
            if (memPath.isFile()) {
                flags &= MAGIC_MIME_TYPE;
                return dbname;
            }
        }
        if (!fn.endsWith(EXT)) {
            fn += EXT;
        }

        /* Compatibility with old code that looked in .mime */
        if (fn.indexOf(".mime") > 0) {
            flags &= MAGIC_MIME_TYPE;
        }
        return fn;
    }

    private String getPath(String fn) {
        File file = new File(fn);
        try {
            fn = file.getCanonicalPath();
        } catch (IOException e) {
            fn = file.getAbsolutePath();
        }
        return fn;
    }

    private int apprentice_load(Magic[][] magic, int[] nmagic, String fn,
            int action) {

        int[] errs = new int[1];
        List<MagicEntry> entries = new ArrayList<MagicEntry>();
        int i, mentrycount = 0, starttest;
        String subfn;

        flags |= MAGIC_CHECK;        /* Enable checks for parsed files */

        /* print silly verbose header for USG compat. */
        if (action == FILE_CHECK) {
            stderr.printf("%s\n", usg_hdr);
        }

        /* load directory or file */
        /* FIXME: Read file names and sort them to prevent
           non-determinism. See Debian bug #488562. */
        if (fn == null) {
            load_1(action, fn, errs, entries);
        } else {
            File f = new File(fn);
            if (f.isDirectory()) {
                String[] subfiles = f.list();
                for (String d : subfiles) {
                    // Skip dot files
                    if (d.charAt(0) == '.') {
                        continue;
                    }
                    subfn = fn + File.separator + d;
                    File subf = new File(subfn);
                    if (subf.isFile()) {
                        load_1(action, subfn, errs, entries);
                    }
                }
            } else {
                load_1(action, fn, errs, entries);
            }
        }

        if (errs[0] == 0) {
            if ((flags & MAGIC_DEBUG) != 0) {
                stdout.printf("Finally %d entries\n", entries.size());
            }
            MagicEntry[] marray = entries.toArray(
                    new MagicEntry[entries.size()]);

            /* Set types of tests */
            for (i = 0; i < marray.length;) {
                Magic m = marray[i].mp.get(0);
                if (m.cont_level != 0) {
                    i++;
                    continue;
                }

                starttest = i;
                do {
                    Magic first = marray[i].mp.get(0);
                    String text = "text";
                    String binary = "binary";
                    set_test_type(marray[starttest].mp.get(0), marray[i].mp.get(
                            0));
                    if ((flags & MAGIC_DEBUG) == 0) {
                        continue;
                    }
                    // TODO: mp is used as a single object in the original code,
                    // but in other places its used as an array.  I'm using
                    // mp[0] here, but this inconsistency is worrying.
                    stderr.printf("%s%s%s: %s\n", first.mimetype,
                            first.mimetype.get() == '\0' ? "" : "; ",
                            first.desc.get() != 0 ?
                                    first.desc :
                                    "(no description)",
                            (first.flag & BINTEST) != 0 ? binary : text);
                    if ((first.flag & BINTEST) != 0) {
                        if (TEXT_PATTERN.matcher(first.desc.toString())
                                .find()) {
                            stderr.printf(
                                    "*** Possible binary test for text type\n");
                        }
                    }
                } while (++i < marray.length && marray[i].mp.get(
                        0).cont_level != 0);
            }

            Arrays.sort(marray, new Comparator<MagicEntry>() {
                public int compare(MagicEntry e1, MagicEntry e2) {
                    return apprentice_sort(e1, e2);
                }
            });

            /*
            * Make sure that any level 0 "default" line is last (if one exists).
            */
            for (i = 0; i < marray.length; i++) {
                Magic first = marray[i].mp.get(0);
                if (first.cont_level == 0 && first.type == FILE_DEFAULT) {
                    while (++i < marray.length) {
                        if (first.cont_level == 0) {
                            break;
                        }
                    }
                    if (i != marray.length) {
                        line = first.lineno; /* XXX - Ugh! */
                        file_magwarn("level 0 \"default\" did not sort last");
                    }
                    break;
                }
            }

            for (i = 0; i < marray.length; i++) {
                mentrycount += marray[i].cont_count;
            }

            magic[0] = new Magic[mentrycount];

            int pos = 0;
            for (i = 0; i < marray.length; i++) {
                for (Magic mpe : marray[i].mp) {
                    magic[0][pos++] = mpe;
                }
            }
        }
        if (errs[0] != 0) {
            magic[0] = null;
            nmagic[0] = 0;
            return errs[0];
        } else {
            nmagic[0] = mentrycount;
            return 0;
        }
    }

    private int apprentice_sort(MagicEntry ma, MagicEntry mb) {
        int sa = apprentice_magic_strength(ma.mp.get(0));
        int sb = apprentice_magic_strength(mb.mp.get(0));
        if (sa == sb) {
            return 0;
        } else if (sa > sb) {
            return -1;
        } else {
            return 1;
        }
    }

    static final int MULT = 10;

    private static int apprentice_magic_strength(Magic m) {
        int val = 2 * MULT;        /* baseline strength */

        switch (m.type) {
        case FILE_DEFAULT:        /* make sure this sorts last */
            if (m.factor_op != FILE_FACTOR_OP_NONE) {
                throw new IllegalStateException();
            }
            return 0;

        case FILE_BYTE:
            val += 1 * MULT;
            break;

        case FILE_SHORT:
        case FILE_LESHORT:
        case FILE_BESHORT:
            val += 2 * MULT;
            break;

        case FILE_LONG:
        case FILE_LELONG:
        case FILE_BELONG:
        case FILE_MELONG:
            val += 4 * MULT;
            break;

        case FILE_PSTRING:
        case FILE_STRING:
            val += m.vallen * MULT;
            break;

        case FILE_BESTRING16:
        case FILE_LESTRING16:
            val += m.vallen * MULT / 2;
            break;

        case FILE_SEARCH:
        case FILE_REGEX:
            val += m.vallen * Math.max(MULT / m.vallen, 1);
            break;

        case FILE_DATE:
        case FILE_LEDATE:
        case FILE_BEDATE:
        case FILE_MEDATE:
        case FILE_LDATE:
        case FILE_LELDATE:
        case FILE_BELDATE:
        case FILE_MELDATE:
        case FILE_FLOAT:
        case FILE_BEFLOAT:
        case FILE_LEFLOAT:
            val += 4 * MULT;
            break;

        case FILE_QUAD:
        case FILE_BEQUAD:
        case FILE_LEQUAD:
        case FILE_QDATE:
        case FILE_LEQDATE:
        case FILE_BEQDATE:
        case FILE_QLDATE:
        case FILE_LEQLDATE:
        case FILE_BEQLDATE:
        case FILE_DOUBLE:
        case FILE_BEDOUBLE:
        case FILE_LEDOUBLE:
            val += 8 * MULT;
            break;

        default:
            val = 0;
            stderr.printf("Bad type %d\n", m.type);
            throw new IllegalStateException("Bad type " + m.type);
        }

        switch (m.reln) {
        case 'x':        /* matches anything penalize */
        case '!':       /* matches almost anything penalize */
            val = 0;
            break;

        case '=':        /* Exact match, prefer */
            val += MULT;
            break;

        case '>':
        case '<':        /* comparison match reduce strength */
            val -= 2 * MULT;
            break;

        case '^':
        case '&':        /* masking bits, we could count them too */
            val -= MULT;
            break;

        default:
            stderr.printf("Bad relation %c\n", toChar(m.reln));
            throw new IllegalStateException("Bad relation " + toChar(m.reln));
        }

        /* ensure we only return 0 for FILE_DEFAULT */
        if (val == 0) {
            val = 1;
        }

        switch (m.factor_op) {
        case FILE_FACTOR_OP_NONE:
            break;
        case FILE_FACTOR_OP_PLUS:
            val += m.factor;
            break;
        case FILE_FACTOR_OP_MINUS:
            val -= m.factor;
            break;
        case FILE_FACTOR_OP_TIMES:
            val *= m.factor;
            break;
        case FILE_FACTOR_OP_DIV:
            val /= m.factor;
            break;
        default:
            throw new IllegalStateException();
        }

        /*
          * Magic entries with no description get a bonus because they depend
          * on subsequent magic entries to print something.
          */
        if (m.desc.get() == '\0') {
            val++;
        }
        return val;
    }

    private static void set_test_type(Magic mstart, Magic m) {
        switch (m.type) {
        case FILE_BYTE:
        case FILE_SHORT:
        case FILE_LONG:
        case FILE_DATE:
        case FILE_BESHORT:
        case FILE_BELONG:
        case FILE_BEDATE:
        case FILE_LESHORT:
        case FILE_LELONG:
        case FILE_LEDATE:
        case FILE_LDATE:
        case FILE_BELDATE:
        case FILE_LELDATE:
        case FILE_MEDATE:
        case FILE_MELDATE:
        case FILE_MELONG:
        case FILE_QUAD:
        case FILE_LEQUAD:
        case FILE_BEQUAD:
        case FILE_QDATE:
        case FILE_LEQDATE:
        case FILE_BEQDATE:
        case FILE_QLDATE:
        case FILE_LEQLDATE:
        case FILE_BEQLDATE:
        case FILE_FLOAT:
        case FILE_BEFLOAT:
        case FILE_LEFLOAT:
        case FILE_DOUBLE:
        case FILE_BEDOUBLE:
        case FILE_LEDOUBLE:
        case FILE_STRING:
        case FILE_PSTRING:
        case FILE_BESTRING16:
        case FILE_LESTRING16:
            /* binary test, set flag */
            mstart.flag |= BINTEST;
            break;
        case FILE_REGEX:
        case FILE_SEARCH:
            /* binary test if pattern is not text */
            if (file_looks_utf8(m.value.s(), m.vallen, null, null) <= 0) {
                mstart.flag |= BINTEST;
            }
            break;
        case FILE_DEFAULT:
            /* can't deduce anything; we shouldn't see this at the
                    top level anyway */
            break;
        case FILE_INVALID:
        default:
            /* invalid search type, but no need to complain here */
            break;
        }
    }

    private void load_1(int action, String fn, int[] errs,
            List<MagicEntry> entries) {

        stdout.printf("Loading %s\n", fn);
        byte[] line = new byte[8192];
        int lineno = 0;
        BufferedInputStream f = null;
        try {
            if (fn != null) {
                f = new BufferedInputStream(new FileInputStream(fn));
            } else {
                InputStream res = getClass().getResourceAsStream(MAGIC);
                f = new BufferedInputStream(res);
            }
            /* read and parse this file */
            for (this.line = 1; fgets(f, line) != null; this.line++) {
                int len;
                len = strlen(line);
                if (len == 0) {
                    /* null line, garbage, etc */
                    continue;
                }
                if (line[len - 1] == '\n') {
                    lineno++;
                    line[len - 1] = '\0'; /* delete newline */
                }
                if (line[0] == '\0') {
                    /* empty, do not parse */
                    continue;
                }
                if (line[0] == '#') {
                    /* comment, do not parse*/
                    continue;
                }
                if (line[0] == '!' && line[1] == ':') {
                    Bang b;
                    int i;
                    for (i = 0; i < bang.length; i++) {
                        b = bang[i];
                        if (len - 2 > b.name.length() && b.name.equals(
                                new String(line, 2, b.name.length()))) {
                            break;
                        }
                    }
//                    stdout.println("bang: " + new String(line));
                    if (i >= bang.length) {
                        file_error(0, "Unknown !: entry `%s'", new Pointer(
                                line));
                        errs[0]++;
                        continue;
                    }
                    if (entries.size() == 0) {
                        file_error(0, "No current entry for :!%s type",
                                bang[i].name);
                        errs[0]++;
                        continue;
                    }
                    if (bang[i].parser.parse(this, entries.get(
                            entries.size() - 1), new Pointer(line,
                            bang[i].name.length() + 2)) != 0) {
                        errs[0]++;
                    }
                    continue;
                }
                if (fn == null) {
                    file = "<" + MAGIC + ">";
                } else {
                    file = fn;
                }
                Pointer l = new Pointer(line);
                if (parse(entries, l, lineno, action) != 0) {
                    errs[0]++;
                }
            }
        } catch (IOException e) {
            errs[0]++;
        } finally {
            IOUtils.closeQuietly(f);
        }
    }

    private int parse(List<MagicEntry> mentryp, Pointer line, int lineno,
            int action) {

        int i;
        MagicEntry me;
        Magic m;
        Pointer l = line.copy();
        Pointer[] t = new Pointer[1];
        int op;
        int cont_level;

        cont_level = 0;

//        stdout.printf("[%3d] parsing %s:%d: %s\n", mentryp.size(), file, this.line, l);
        while (l.get() == '>') {
            l.incr();                /* step over */
            cont_level++;
        }

        if (cont_level == 0 || cont_level > last_cont_level) {
            if (file_check_mem(cont_level) == -1) {
                return -1;
            }
        }
        last_cont_level = cont_level;

        if (cont_level != 0) {
            if (mentryp.size() == 0) {
                file_error(0, "No current entry for continuation");
                return -1;
            }
            me = mentryp.get(mentryp.size() - 1);
            m = new Magic(file, this.line);
            me.mp.add(m);
            me.cont_count++;
            m.cont_level = cont_level;
        } else {
            me = new MagicEntry();
            mentryp.add(me);
            m = new Magic(file, this.line);
            me.mp.add(m);
            m.factor_op = FILE_FACTOR_OP_NONE;
            m.cont_level = 0;
            me.cont_count = 1;
        }
        m.lineno = lineno;

        if (l.get() == '&') {  /* m.cont_level == 0 checked below. */
            l.incr();            /* step over */
            m.flag |= OFFADD;
        }
        if (l.get() == '(') {
            l.incr();                /* step over */
            m.flag |= INDIR;
            if ((m.flag & OFFADD) != 0) {
                m.flag = (byte) ((m.flag & ~OFFADD) | INDIROFFADD);
            }

            if (l.get() == '&') {  /* m.cont_level == 0 checked below */
                l.incr();            /* step over */
                m.flag |= OFFADD;
            }
        }
        /* Indirect offsets are not valid at level 0. */
        if (m.cont_level == 0 && (m.flag & (OFFADD | INDIROFFADD)) != 0) {
            if ((flags & MAGIC_CHECK) != 0) {
                file_magwarn("relative offset at level 0");
            }
        }

        /* get offset, then skip over it */
        m.offset = strtoul(l, t, 0);
        if (l.equals(t[0])) {
            if ((flags & MAGIC_CHECK) != 0) {
                file_magwarn("offset `%s' invalid", l);
            }
        }
        l = t[0];

        if ((m.flag & INDIR) != 0) {
            m.in_type = FILE_LONG;
            m.in_offset = 0;
            /*
             * read [.lbs][+-]nnnnn)
             */
            if (l.get() == '.') {
                l.incr();
                switch (l.get()) {
                case 'l':
                    m.in_type = FILE_LELONG;
                    break;
                case 'L':
                    m.in_type = FILE_BELONG;
                    break;
                case 'm':
                    m.in_type = FILE_MELONG;
                    break;
                case 'h':
                case 's':
                    m.in_type = FILE_LESHORT;
                    break;
                case 'H':
                case 'S':
                    m.in_type = FILE_BESHORT;
                    break;
                case 'c':
                case 'b':
                case 'C':
                case 'B':
                    m.in_type = FILE_BYTE;
                    break;
                case 'e':
                case 'f':
                case 'g':
                    m.in_type = FILE_LEDOUBLE;
                    break;
                case 'E':
                case 'F':
                case 'G':
                    m.in_type = FILE_BEDOUBLE;
                    break;
                case 'i':
                    m.in_type = FILE_LEID3;
                    break;
                case 'I':
                    m.in_type = FILE_BEID3;
                    break;
                default:
                    if ((flags & MAGIC_CHECK) != 0) {
                        file_magwarn("indirect offset type `%c' invalid",
                                l.get());
                    }
                    break;
                }
                l.incr();
            }

            m.in_op = 0;
            if (l.get() == '~') {
                m.in_op |= FILE_OPINVERSE;
                l.incr();
            }
            if ((op = get_op(l.get())) != -1) {
                m.in_op |= op;
                l.incr();
            }
            if (l.get() == '(') {
                m.in_op |= FILE_OPINDIRECT;
                l.incr();
            }
            if (isdigit(l.get()) || l.get() == '-') {
                m.in_offset = strtol(l, t, 0);
                if (l.equals(t[0])) {
                    if ((flags & MAGIC_CHECK) != 0) {
                        file_magwarn("in_offset `%s' invalid", l);
                    }
                }
                l = t[0];
            }
            if (l.getIncr() != ')' ||
                    (m.in_op & FILE_OPINDIRECT) != 0 && l.getIncr() != ')') {
                if ((flags & MAGIC_CHECK) != 0) {
                    file_magwarn("missing ')' in indirect offset");
                }
            }
        }
        l = eatab(l);

        m.cond = get_cond(l, t);
        l = t[0];
        if (check_cond(m.cond, cont_level) == -1) {
            return -1;
        }

        l = eatab(l);

        if (l.get() == 'u') {
            l.incr();
            m.flag |= UNSIGNED;
        }

        m.type = TypeTblS.get_type(l, t);
        l = t[0];
        if (m.type == FILE_INVALID) {
            if ((flags & MAGIC_CHECK) != 0) {
                file_magwarn("type `%s' invalid", l);
            }
            return -1;
        }

        /* New-style anding: "0 byte&0x80 =0x80 dynamically linked" */
        /* New and improved: ~ & | ^ + - * / % -- exciting, isn't it? */

        m.mask_op = 0;
        if (l.get() == '~') {
            if (!IS_STRING(m.type)) {
                m.mask_op |= FILE_OPINVERSE;
            } else if ((flags & MAGIC_CHECK) != 0) {
                file_magwarn("'~' invalid for string types");
            }
            l.incr();
        }
        m.str_range = 0;
        m.str_flags = 0;
        m.num_mask = 0;
        if ((op = get_op(l.get())) != -1) {
            if (!IS_STRING(m.type)) {
                Long lval;
                l.incr();
                m.mask_op |= op;
                lval = strtoull(l, t, 0);
                l = t[0];
                m.num_mask = file_signextend(m, lval);
                l = eatsize(l);
            } else if (op == FILE_OPDIVIDE) {
                int have_range = 0;
                while (!isspace(l.getPreIncr())) {
                    switch (l.get()) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        if (have_range != 0 && (flags & MAGIC_CHECK) != 0) {
                            file_magwarn("multiple ranges");
                        }
                        have_range = 1;
                        m.str_range = strtoul(l, t, 0);
                        if (m.str_range == 0) {
                            file_magwarn("zero range");
                        }
                        l = t[0].plus(-1);
                        break;
                    case CHAR_COMPACT_BLANK:
                        m.str_flags |= STRING_COMPACT_BLANK;
                        break;
                    case CHAR_COMPACT_OPTIONAL_BLANK:
                        m.str_flags |= STRING_COMPACT_OPTIONAL_BLANK;
                        break;
                    case CHAR_IGNORE_LOWERCASE:
                        m.str_flags |= STRING_IGNORE_LOWERCASE;
                        break;
                    case CHAR_IGNORE_UPPERCASE:
                        m.str_flags |= STRING_IGNORE_UPPERCASE;
                        break;
                    case CHAR_REGEX_OFFSET_START:
                        m.str_flags |= REGEX_OFFSET_START;
                        break;
                    default:
                        if ((flags & MAGIC_CHECK) != 0) {
                            file_magwarn("string extension `%c' invalid",
                                    l.get());
                        }
                        return -1;
                    }
                    /* allow multiple '/' for readability */
                    if (l.get(1) == '/' && !isspace(l.get(2))) {
                        l.incr();
                    }
                }
                if (string_modifier_check(m) == -1) {
                    return -1;
                }
            } else {
                if ((flags & MAGIC_CHECK) != 0) {
                    file_magwarn("invalid string op: %c", t[0].get());
                }
                return -1;
            }
        }
        /*
          * We used to set mask to all 1's here, instead let's just not do
          * anything if mask = 0 (unless you have a better idea)
          */
        l = eatab(l);

        switch (l.get()) {
        case '>':
        case '<':
            m.reln = l.get();
            l.incr();
            if (l.get() == '=') {
                if ((flags & MAGIC_CHECK) != 0) {
                    file_magwarn("%c= not supported", m.reln);
                    return -1;
                }
                l.incr();
            }
            break;
        /* Old-style anding: "0 byte &0x80 dynamically linked" */
        case '&':
        case '^':
        case '=':
            m.reln = l.get();
            l.incr();
            if (l.get() == '=') {
                /* HP compat: ignore &= etc. */
                l.incr();
            }
            break;
        case '!':
            m.reln = l.get();
            l.incr();
            break;
        default:
            m.reln = '=';        /* the default relation */
            if (l.get() == 'x' && ((isascii(l.get(1)) && isspace(l.get(1))) ||
                    l.get(1) == '\0')) {
                m.reln = l.get();
                l.incr();
            }
            break;
        }

        /*
        * Grab the value part, except for an 'x' reln.
        */
        t[0] = l;
        if (m.reln != 'x' && getvalue(m, t, action) != 0) {
            return -1;
        }
        l = t[0];

        /*
          * TODO finish this macro and start using it!
          * #define offsetcheck {if (offset > HOWMANY-1)
          *	magwarn("offset too big"); }
          */

        /*
          * Now get last part - the description
          */
        l = eatab(l);
        if (l.get(0) == '\b') {
            l.incr();
            m.flag |= NOSPACE;
        } else if (l.get(0) == '\\' && l.get(1) == 'b') {
            l.incr();
            l.incr();
            m.flag |= NOSPACE;
        }
        for (i = 0; m.desc.set(i++, l.getIncr()) != '\0' && i < MAXDESC;) {
            continue;
        }
        if (i == MAXDESC) {
            m.desc.set(MAXDESC - 1, '\0');
            if ((flags & MAGIC_CHECK) != 0) {
                file_magwarn("description `%s' truncated", m.desc);
            }
        }

        /*
         * We only do this check while compiling, or if any of the magic
         * files were not compiled.
         */
        if ((flags & MAGIC_CHECK) != 0) {
            if (check_format(m) == -1) {
                return -1;
            }
        }
        if (action == FILE_CHECK) {
            file_mdump(m);
        }
        m.mimetype.set(0,
                '\0');                /* initialise MIME type to none */
        return 0;
    }

    private void file_mdump(Magic m) {
        char[] optyp = FILE_OPS.toCharArray();

        stderr.printf("[%d", m.lineno);
        stderr.printf(">>>>>>>> %d".substring(8 - (m.cont_level & 7)),
                m.offset);

        if ((m.flag & INDIR) != 0) {
            stderr.printf("(%s,",
                    /* Note: type is unsigned */
                    (m.in_type < file_names.length) ?
                            file_names[m.in_type] :
                            "*bad*");
            if ((m.in_op & FILE_OPINVERSE) != 0) {
                stderr.putc('~');
            }
            stderr.printf("%c%d),", ((m.in_op & FILE_OPS_MASK) < optyp.length) ?
                    optyp[m.in_op & FILE_OPS_MASK] :
                    '?', m.in_offset);
        }
        stderr.printf(" %s%s", ((m.flag & UNSIGNED)) != 0 ? "u" : "",
                /* Note: type is unsigned */
                (m.type < file_names.length) ? file_names[m.type] : "*bad*");
        if ((m.mask_op & FILE_OPINVERSE) != 0) {
            stderr.putc('~');
        }

        if (IS_STRING(m.type)) {
            if ((m.str_flags) != 0) {
                stderr.putc('/');
                if ((m.str_flags & STRING_COMPACT_BLANK) != 0) {
                    stderr.putc(CHAR_COMPACT_BLANK);
                }
                if ((m.str_flags & STRING_COMPACT_OPTIONAL_BLANK) != 0) {
                    stderr.putc(CHAR_COMPACT_OPTIONAL_BLANK);
                }
                if ((m.str_flags & STRING_IGNORE_LOWERCASE) != 0) {
                    stderr.putc(CHAR_IGNORE_LOWERCASE);
                }
                if ((m.str_flags & STRING_IGNORE_UPPERCASE) != 0) {
                    stderr.putc(CHAR_IGNORE_UPPERCASE);
                }
                if ((m.str_flags & REGEX_OFFSET_START) != 0) {
                    stderr.putc(CHAR_REGEX_OFFSET_START);
                }
            }
            if ((m.str_range) != 0) {
                stderr.printf("/%d", m.str_range);
            }
        } else {
            if ((m.mask_op & FILE_OPS_MASK) < optyp.length) {
                stderr.putc(optyp[m.mask_op & FILE_OPS_MASK]);
            } else {
                stderr.putc('?');
            }

            if ((m.num_mask) != 0) {
                stderr.printf("%8x", m.num_mask);
            }
        }
        stderr.printf(",%c", toChar(m.reln));

        if (m.reln != 'x') {
            switch (m.type) {
            case FILE_BYTE:
            case FILE_SHORT:
            case FILE_LONG:
            case FILE_LESHORT:
            case FILE_LELONG:
            case FILE_MELONG:
            case FILE_BESHORT:
            case FILE_BELONG:
                stderr.printf("%d", m.value.l());
                break;
            case FILE_BEQUAD:
            case FILE_LEQUAD:
            case FILE_QUAD:
                stderr.printf("%d", m.value.q());
                break;
            case FILE_PSTRING:
            case FILE_STRING:
            case FILE_REGEX:
            case FILE_BESTRING16:
            case FILE_LESTRING16:
            case FILE_SEARCH:
                file_showstr(stderr, m.value.s(), m.vallen);
                break;
            case FILE_DATE:
            case FILE_LEDATE:
            case FILE_BEDATE:
            case FILE_MEDATE:
                stderr.printf("%s,", file_fmttime(m.value.l(), 1));
                break;
            case FILE_LDATE:
            case FILE_LELDATE:
            case FILE_BELDATE:
            case FILE_MELDATE:
                stderr.printf("%s,", file_fmttime(m.value.l(), 0));
                break;
            case FILE_QDATE:
            case FILE_LEQDATE:
            case FILE_BEQDATE:
                stderr.printf("%s,", file_fmttime((int) m.value.q(), 1));
                break;
            case FILE_QLDATE:
            case FILE_LEQLDATE:
            case FILE_BEQLDATE:
                stderr.printf("%s,", file_fmttime((int) m.value.q(), 0));
                break;
            case FILE_FLOAT:
            case FILE_BEFLOAT:
            case FILE_LEFLOAT:
                stderr.printf("%G", m.value.f());
                break;
            case FILE_DOUBLE:
            case FILE_BEDOUBLE:
            case FILE_LEDOUBLE:
                stderr.printf("%G", m.value.d());
                break;
            case FILE_DEFAULT:
                /* XXX - do anything here? */
                break;
            default:
                stderr.puts("*bad*");
                break;
            }
        }
        stderr.printf(",\"%s\"]\n", m.desc);
    }

    private int check_format(Magic m) {
        Pointer ptr;

        for (ptr = m.desc.copy(); ptr.get() != '\0'; ptr.incr()) {
            if (ptr.get() == '%') {
                break;
            }
        }
        if (ptr.get() == '\0') {
            /* No format string; ok */
            return 1;
        }

        if (m.type >= file_formats.length) {
            file_magwarn("Internal error inconsistency between " +
                    "m.type and format strings");
            return -1;
        }
        if (file_formats[m.type] == FILE_FMT_NONE) {
            file_magwarn("No format string for `%s' with description " + "`%s'",
                    m.desc, file_names[m.type]);
            return -1;
        }

        ptr.incr();
        if (check_format_type(ptr, file_formats[m.type]) == -1) {
            /*
                  * TODO: this error message is unhelpful if the format
                  * string is not one character long
                  */
            file_magwarn("Printf format `%c' is not valid for type " +
                    "`%s' in description `%s'",
                    ptr.get() != '\0' ? ptr.get() : '?', file_names[m.type],
                    m.desc);
            return -1;
        }

        for (; ptr.get() != '\0'; ptr.incr()) {
            if (ptr.get() == '%') {
                file_magwarn(
                        "Too many format strings (should have at most one) " +
                                "for `%s' with description `%s'",
                        file_names[m.type], m.desc);
                return -1;
            }
        }
        return 0;
    }

    private static int check_format_type(Pointer l, int type) {
        Pointer ptr = l.copy();
        int quad = 0;
        if (ptr.get() == '\0') {
            /* Missing format string; bad */
            return -1;
        }

        switch (type) {
        case FILE_FMT_QUAD:
            quad = 1;
            //noinspection fallthrough
        case FILE_FMT_NUM:
            if (ptr.get() == '-') {
                ptr.incr();
            }
            if (ptr.get() == '.') {
                ptr.incr();
            }
            while (isdigit(ptr.get())) {
                ptr.incr();
            }
            if (ptr.get() == '.') {
                ptr.incr();
            }
            while (isdigit(ptr.get())) {
                ptr.incr();
            }
            if (quad != 0) {
                if (ptr.getIncr() != 'l') {
                    return -1;
                }
                if (ptr.getIncr() != 'l') {
                    return -1;
                }
            }

            switch (ptr.getIncr()) {
            case 'l':
                switch (ptr.getIncr()) {
                case 'i':
                case 'd':
                case 'u':
                case 'x':
                case 'X':
                    return 0;
                default:
                    return -1;
                }

            case 'h':
                switch (ptr.getIncr()) {
                case 'h':
                    switch (ptr.getIncr()) {
                    case 'i':
                    case 'd':
                    case 'u':
                    case 'x':
                    case 'X':
                        return 0;
                    default:
                        return -1;
                    }
                case 'd':
                    return 0;
                default:
                    return -1;
                }

            case 'i':
            case 'c':
            case 'd':
            case 'u':
            case 'x':
            case 'X':
                return 0;

            default:
                return -1;
            }

        case FILE_FMT_FLOAT:
        case FILE_FMT_DOUBLE:
            if (ptr.get() == '-') {
                ptr.incr();
            }
            if (ptr.get() == '.') {
                ptr.incr();
            }
            while (isdigit(ptr.get())) {
                ptr.incr();
            }
            if (ptr.get() == '.') {
                ptr.incr();
            }
            while (isdigit(ptr.get())) {
                ptr.incr();
            }

            switch (ptr.getIncr()) {
            case 'e':
            case 'E':
            case 'f':
            case 'F':
            case 'g':
            case 'G':
                return 0;

            default:
                return -1;
            }

        case FILE_FMT_STR:
            if (ptr.get() == '-') {
                ptr.incr();
            }
            while (isdigit(ptr.get())) {
                ptr.incr();
            }
            if (ptr.get() == '.') {
                ptr.incr();
                while (isdigit(ptr.get())) {
                    ptr.incr();
                }
            }

            switch (ptr.getIncr()) {
            case 's':
                return 0;
            default:
                return -1;
            }

        default:
            /* internal error */
            abort();
        }
        /*NOTREACHED*/
        return -1;
    }

    private int getvalue(Magic m, Pointer[] p, int action) {
        switch (m.type) {
        case FILE_BESTRING16:
        case FILE_LESTRING16:
        case FILE_STRING:
        case FILE_PSTRING:
        case FILE_REGEX:
        case FILE_SEARCH:
            p[0] = getstr(m, p[0], action == FILE_COMPILE);
            if (p[0] == null) {
                if ((flags & MAGIC_CHECK) != 0) {
                    file_magwarn("cannot get string from `%s'", m.value.s());
                }
                return -1;
            }
            return 0;
        case FILE_FLOAT:
        case FILE_BEFLOAT:
        case FILE_LEFLOAT:
            if (m.reln != 'x') {
                m.value.f(strtof(p[0], p));
            }
            return 0;
        case FILE_DOUBLE:
        case FILE_BEDOUBLE:
        case FILE_LEDOUBLE:
            if (m.reln != 'x') {
                m.value.d(strtod(p[0], p));
            }
            return 0;
        default:
            if (m.reln != 'x') {
                m.value.q(file_signextend(m, strtoull(p[0], p, 0)));
                p[0] = eatsize(p[0]);
            }
            return 0;
        }
    }

    private Pointer getstr(Magic m, Pointer origs, boolean warn) {
        Pointer s = origs.copy();
        Pointer p = m.value.s().copy();
        int plen = ValueType.MAXstring;
        Pointer origp = p.copy();
        Pointer pmax = p.plus(plen - 1);
        byte c;
        int val;

outer:
        while ((c = s.getIncr()) != '\0') {
            if (isspace(c)) {
                break;
            }
            if (p.compareTo(pmax) >= 0) {
                file_error(0, "string too long: `%s'", origs);
                return null;
            }
            if (c == '\\') {
                switch (c = s.getIncr()) {

                case '\0':
                    if (warn) {
                        file_magwarn("incomplete escape");
                    }
                    break outer;

                case '\t':
                    if (warn) {
                        file_magwarn("escaped tab found, use \\t instead");
                        warn = false;        /* already did */
                    }
                    //noinspection fallthrough
                default:
                    if (warn) {
                        if (isprint((char) c)) {
                            /* Allow escaping of ``relations'' */
                            if ("<>&^=!".indexOf(c) >= 0) {
                                file_magwarn("no need to escape `%c'", c);
                            }
                        } else {
                            file_magwarn("unknown escape sequence: \\%03o", c);
                        }
                    }
                    /* space, perhaps force people to use \040? */
                    //noinspection fallthrough
                case ' ':
                    /* Relations */
                case '>':
                case '<':
                case '&':
                case '^':
                case '=':
                case '!':
                    /* and baskslash itself */
                case '\\':
                    p.setIncr(c);
                    break;

                case 'a':
                    p.setIncr('\u0007');
                    break;

                case 'b':
                    p.setIncr('\b');
                    break;

                case 'f':
                    p.setIncr('\f');
                    break;

                case 'n':
                    p.setIncr('\n');
                    break;

                case 'r':
                    p.setIncr('\r');
                    break;

                case 't':
                    p.setIncr('\t');
                    break;

                case 'v':
                    p.setIncr('\u000b');
                    break;

                /* \ and up to 3 octal digits */
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    val = c - '0';
                    c = s.getIncr();    /* try for 2 */
                    if (c >= '0' && c <= '7') {
                        val = (val << 3) | (c - '0');
                        c = s.getIncr();    /* try for 3 */
                        if (c >= '0' && c <= '7') {
                            val = (val << 3) | (c - '0');
                        } else {
                            s.decr();
                        }
                    } else {
                        s.decr();
                    }
                    p.setIncr(val);
                    break;

                /* \x and up to 2 hex digits */
                case 'x':
                    val = 'x';        /* Default if no digits */
                    c = (byte) hextoint(s.getIncr());        /* Get next char */
                    if (c >= 0) {
                        val = c;
                        c = (byte) hextoint(s.getIncr());
                        if (c >= 0) {
                            val = (val << 4) + c;
                        } else {
                            s.decr();
                        }
                    } else {
                        s.decr();
                    }
                    p.setIncr(val);
                    break;
                }
            } else {
                p.setIncr(c);
            }
        }
        p.set('\0');
        m.vallen = p.minus(origp);
        if (m.type == FILE_PSTRING) {
            m.vallen++;
        }
        if (m.vallen == 0) {
            file_magwarn("vallen == 0");
        }
        return s;
    }

    private int string_modifier_check(Magic m) {
        if ((flags & MAGIC_CHECK) == 0) {
            return 0;
        }

        switch (m.type) {
        case FILE_BESTRING16:
        case FILE_LESTRING16:
            if (m.str_flags != 0) {
                file_magwarn("no modifiers allowed for 16-bit strings\n");
                return -1;
            }
            break;
        case FILE_STRING:
        case FILE_PSTRING:
            if ((m.str_flags & REGEX_OFFSET_START) != 0) {
                file_magwarn("'/%c' only allowed on regex and search\n",
                        CHAR_REGEX_OFFSET_START);
                return -1;
            }
            break;
        case FILE_SEARCH:
            if (m.str_range == 0) {
                file_magwarn("missing range; defaulting to %d\n",
                        STRING_DEFAULT_RANGE);
                m.str_range = STRING_DEFAULT_RANGE;
                return -1;
            }
            break;
        case FILE_REGEX:
            if ((m.str_flags & STRING_COMPACT_BLANK) != 0) {
                file_magwarn("'/%c' not allowed on regex\n",
                        CHAR_COMPACT_BLANK);
                return -1;
            }
            if ((m.str_flags & STRING_COMPACT_OPTIONAL_BLANK) != 0) {
                file_magwarn("'/%c' not allowed on regex\n",
                        CHAR_COMPACT_OPTIONAL_BLANK);
                return -1;
            }
            break;
        default:
            file_magwarn("coding error: m.type=%d\n", m.type);
            return -1;
        }
        return 0;
    }

    @SuppressWarnings({"SillyAssignment", "RedundantCast"})
    private long file_signextend(Magic m, long v) {
        if ((m.flag & UNSIGNED) == 0) {
            switch (m.type) {
            /*
             * Do not remove the casts below.  They are
             * vital.  When later compared with the data,
             * the sign extension must have happened.
             */
            case FILE_BYTE:
                // The original has a cast to char which doesn't sign extend
                v = (byte) v;
                break;
            case FILE_SHORT:
            case FILE_BESHORT:
            case FILE_LESHORT:
                v = (short) v;
                break;
            case FILE_DATE:
            case FILE_BEDATE:
            case FILE_LEDATE:
            case FILE_MEDATE:
            case FILE_LDATE:
            case FILE_BELDATE:
            case FILE_LELDATE:
            case FILE_MELDATE:
            case FILE_LONG:
            case FILE_BELONG:
            case FILE_LELONG:
            case FILE_MELONG:
            case FILE_FLOAT:
            case FILE_BEFLOAT:
            case FILE_LEFLOAT:
                v = (int) v;
                break;
            case FILE_QUAD:
            case FILE_BEQUAD:
            case FILE_LEQUAD:
            case FILE_QDATE:
            case FILE_QLDATE:
            case FILE_BEQDATE:
            case FILE_BEQLDATE:
            case FILE_LEQDATE:
            case FILE_LEQLDATE:
            case FILE_DOUBLE:
            case FILE_BEDOUBLE:
            case FILE_LEDOUBLE:
                v = (long) v;
                break;
            case FILE_STRING:
            case FILE_PSTRING:
            case FILE_BESTRING16:
            case FILE_LESTRING16:
            case FILE_REGEX:
            case FILE_SEARCH:
            case FILE_DEFAULT:
            case FILE_INDIRECT:
                break;
            default:
                if ((flags & MAGIC_CHECK) != 0) {
                    file_magwarn("cannot happen: m.type=%d\n", m.type);
                }
                return ~0L;
            }
        } else {
            // Strip off sign bits in case of previous sign extension
            switch (m.type) {
            case FILE_BYTE:
                v &= 0xff;
                break;
            case FILE_SHORT:
            case FILE_BESHORT:
            case FILE_LESHORT:
                v &= 0xffff;
                break;
            case FILE_DATE:
            case FILE_BEDATE:
            case FILE_LEDATE:
            case FILE_MEDATE:
            case FILE_LDATE:
            case FILE_BELDATE:
            case FILE_LELDATE:
            case FILE_MELDATE:
            case FILE_LONG:
            case FILE_BELONG:
            case FILE_LELONG:
            case FILE_MELONG:
                v &= 0xffffffff;
                break;
            case FILE_FLOAT:
            case FILE_BEFLOAT:
            case FILE_LEFLOAT:
            case FILE_QUAD:
            case FILE_BEQUAD:
            case FILE_LEQUAD:
            case FILE_QDATE:
            case FILE_QLDATE:
            case FILE_BEQDATE:
            case FILE_BEQLDATE:
            case FILE_LEQDATE:
            case FILE_LEQLDATE:
                // can't do the masking trick for longs
            case FILE_DOUBLE:
            case FILE_BEDOUBLE:
            case FILE_LEDOUBLE:
            case FILE_STRING:
            case FILE_PSTRING:
            case FILE_BESTRING16:
            case FILE_LESTRING16:
            case FILE_REGEX:
            case FILE_SEARCH:
            case FILE_DEFAULT:
            case FILE_INDIRECT:
                break;
            default:
                if ((flags & MAGIC_CHECK) != 0) {
                    file_magwarn("cannot happen: m.type=%d\n", m.type);
                }
                return ~0L;
            }
        }
        return v;
    }

    private int check_cond(byte cond, int cont_level) {
        byte last_cond;
        file_check_mem(cont_level); // Added
        last_cond = c.li.get(cont_level).last_cond;

        switch (cond) {
        case COND_IF:
            if (last_cond != COND_NONE && last_cond != COND_ELIF) {
                if ((flags & MAGIC_CHECK) != 0) {
                    file_magwarn("syntax error: `if'");
                }
                return -1;
            }
            last_cond = COND_IF;
            break;

        case COND_ELIF:
            if (last_cond != COND_IF && last_cond != COND_ELIF) {
                if ((flags & MAGIC_CHECK) != 0) {
                    file_magwarn("syntax error: `elif'");
                }
                return -1;
            }
            last_cond = COND_ELIF;
            break;

        case COND_ELSE:
            if (last_cond != COND_IF && last_cond != COND_ELIF) {
                if ((flags & MAGIC_CHECK) != 0) {
                    file_magwarn("syntax error: `else'");
                }
                return -1;
            }
            last_cond = COND_NONE;
            break;

        case COND_NONE:
            last_cond = COND_NONE;
            break;
        }

        c.li.get(cont_level).last_cond = last_cond;
        return 0;
    }

    private int file_check_mem(int level) {
        while (c.li.size() < level) {
            c.li.add(null);
        }
        LevelInfo li = new LevelInfo(0, 0, COND_NONE);
        c.li.add(li);
        return 0;
    }

    private static byte get_cond(Pointer l, Pointer[] t) {
        t[0] = l;
        CondTblS p = null;
        String lstr = l.toString();
        for (int i = 0; i < CondTblS.cond_tbl.length; i++) {
            p = CondTblS.cond_tbl[i];
            if (strncmp(lstr, p.name, p.name.length()) == 0 && isspace(l.get(
                    p.name.length()))) {
                if (t != null) {
                    t[0] = l.plus(p.name.length());
                }
                break;
            }
        }

        return p.cond;
    }

    private static int get_op(byte c) {
        switch (c) {
        case '&':
            return FILE_OPAND;
        case '|':
            return FILE_OPOR;
        case '^':
            return FILE_OPXOR;
        case '+':
            return FILE_OPADD;
        case '-':
            return FILE_OPMINUS;
        case '*':
            return FILE_OPMULTIPLY;
        case '/':
            return FILE_OPDIVIDE;
        case '%':
            return FILE_OPMODULO;
        default:
            return -1;
        }
    }

    private static boolean init_file_tables_done = false;

    private void init_file_tables() {
        if (init_file_tables_done) {
            return;
        }
        init_file_tables_done = true;

        file_names = new String[TypeTblS.type_tbl.length];
        file_formats = new int[file_names.length];
        for (TypeTblS p : TypeTblS.type_tbl) {
            assert (p.type < FILE_NAMES_SIZE);
            file_names[p.type] = p.name;
            file_formats[p.type] = p.format;
        }
    }

    int file_reset() {
        if (mlist == null) {
            file_error(0, "no magic files loaded");
            return -1;
        }
        o.buf.delete(0, o.buf.length());
        o.pbuf.delete(0, o.pbuf.length());
        event_flags &= ~EVENT_HAD_ERR;
        error = -1;
        return 0;
    }

    int file_fsmagic(String fn, File sb) {
        int mime = flags & MAGIC_MIME;

        if ((flags & MAGIC_APPLE) != 0) {
            return 0;
        }
        if (fn == null) {
            return 0;
        }

        if (sb.isDirectory()) {
            if (mime != 0) {
                if (handle_mime(mime, "x-directory") == -1) {
                    return -1;
                }
            } else if (file_printf("directory") == -1) {
                return -1;
            }
            return 1;
        }

        /*
          * regular file, check next possibility
          *
          * If stat() tells us the file has zero length, report here that
          * the file is empty, so we can skip all the work of opening and
          * reading the file.
          * But if the -s option has been given, we skip this optimization,
          * since on some systems, stat() reports zero size for raw disk
          * partitions.  (If the block special device really has zero length,
          * the fact that it is empty will be detected and reported correctly
          * when we read the file.)
          */
        if ((flags & MAGIC_DEVICES) == 0 && sb.exists() && sb.length() == 0) {
            if (mime != 0) {
                if (handle_mime(mime, "x-empty") == -1) {
                    return -1;
                }
            } else if (file_printf("empty") == -1) {
                return -1;
            }
            return 1;
        }
        return 0;
    }

    private int handle_mime(int mime, String str) {
        if ((mime & MAGIC_MIME_TYPE) != 0) {
            if (file_printf("application/%s", str) == -1) {
                return -1;
            }
            if ((mime & MAGIC_MIME_ENCODING) != 0 && file_printf(
                    "; charset=") == -1) {
                return -1;
            }
        }
        if ((mime & MAGIC_MIME_ENCODING) != 0 && file_printf("binary") == -1) {
            return -1;
        }
        return 0;
    }

    String file_getbuffer() {
        StringBuilder pbuf;

        if ((event_flags & EVENT_HAD_ERR) != 0) {
            return null;
        }

        if ((flags & MAGIC_RAW) != 0) {
            return o.buf.toString();
        }

        if (o.buf == null) {
            return null;
        }

        /* * 4 is for octal representation, + 1 is for NUL */
        pbuf = o.pbuf;

        for (int i = 0; i < o.buf.length(); i++) {
            char ch = o.buf.charAt(i);
            if (isprint(ch)) {
                pbuf.append(ch);
            } else {
                OCTALIFY(ch, pbuf);
            }
        }
        return o.pbuf.toString();
    }

    int file_buffer(InputStream fd, String inname, byte[] buf, int nb) {
        if (nb < 0) { // let the user say "-1" meaning "use as much as you want"
            if (buf != null) {
                nb = Math.min(buf.length, HOWMANY);
            } else {
                nb = HOWMANY;
            }
        }

        int m = 0, rv = 0, looks_text = 0;
        int mime = flags & MAGIC_MIME;
        byte[] ubuf = buf;
        char[][] u8buf = new char[1][];
        int[] ulen = new int[1];
        StringBuilder code = new StringBuilder();
        StringBuilder code_mime = new StringBuilder("binary");
        StringBuilder type = new StringBuilder();
        boolean done = false;

        if (nb == 0) {
            if ((mime == 0 || (mime & MAGIC_MIME_TYPE) != 0) && file_printf(
                    mime != 0 ? "application/x-empty" : "empty") == -1) {
                return -1;
            }
            return 1;
        } else if (nb == 1) {
            if ((mime == 0 || (mime & MAGIC_MIME_TYPE) != 0) && file_printf(
                    mime != 0 ?
                            "application/octet-stream" :
                            "very short file (no magic)") == -1) {
                return -1;
            }
            return 1;
        }

        if ((flags & MAGIC_NO_CHECK_ENCODING) == 0) {
            looks_text = file_encoding(ubuf, nb, u8buf, ulen, code, code_mime,
                    type);
        }

        /* try compression stuff */
        if ((flags & MAGIC_NO_CHECK_COMPRESS) == 0) {
            if ((m = file_zmagic(fd, inname, ubuf, nb)) != 0) {
                if ((flags & MAGIC_DEBUG) != 0) {
                    stderr.printf("zmagic %d\n", m);
                }
                done = true;
            }
        }

        /* Check if we have a tar file */
        if (!done && (flags & MAGIC_NO_CHECK_TAR) == 0) {
            if ((m = file_is_tar(ubuf, nb)) != 0) {
                if ((flags & MAGIC_DEBUG) != 0) {
                    stderr.printf("tar %d\n", m);
                }
                done = true;
            }
        }

        /* Check if we have a CDF file */
        if (!done && (flags & MAGIC_NO_CHECK_CDF) == 0) {
            if ((m = file_trycdf(fd, ubuf, nb)) != 0) {
                if ((flags & MAGIC_DEBUG) != 0) {
                    stderr.printf("cdf %d\n", m);
                }
                done = true;
            }
        }

        /* try soft magic tests */
        if (!done && (flags & MAGIC_NO_CHECK_SOFT) == 0) {
            if ((m = file_softmagic(new Pointer(ubuf), nb, BINTEST)) != 0) {
                if ((flags & MAGIC_DEBUG) != 0) {
                    stderr.printf("softmagic %d\n", m);
                }
                done = true;
            }
        }

        /* try text properties (and possibly text tokens) */
        if (!done && (flags & MAGIC_NO_CHECK_TEXT) == 0) {
            if ((m = file_ascmagic(ubuf, nb)) != 0) {
                if ((flags & MAGIC_DEBUG) != 0) {
                    stderr.printf("ascmagic %d\n", m);
                }
                done = true;
            }
            if (!done && (flags & MAGIC_NO_CHECK_ENCODING) == 0) {
                /* try to discover text encoding */
                if (looks_text == 0) {
                    if ((m = file_ascmagic_with_encoding(ubuf, nb, u8buf[0],
                            ulen[0], code, type)) != 0) {
                        if ((flags & MAGIC_DEBUG) != 0) {
                            stderr.printf("ascmagic/enc %d\n", m);
                        }
                        done = true;
                    }
                }
            }
        }

        if (!done) {
            /* give up */
            m = 1;
            if ((mime == 0 || (mime & MAGIC_MIME_TYPE) != 0) && file_printf(
                    mime != 0 ? "application/octet-stream" : "data") == -1) {
                rv = -1;
            }
        }

        if ((flags & MAGIC_MIME_ENCODING) != 0) {
            if ((flags & MAGIC_MIME_TYPE) != 0) {
                if (file_printf("; charset=") == -1) {
                    rv = -1;
                }
            }
            if (file_printf("%s", code_mime) == -1) {
                rv = -1;
            }
        }

        if (rv != 0) {
            return rv;
        }

        return m;
    }

    private int file_ascmagic_with_encoding(byte[] buf, int nbytes, char[] ubuf,
            int ulen, StringBuilder code, StringBuilder type) {

        Pointer utf8_buf = null, utf8_end;
        int mlen, i;
        Names p;
        int rv = -1;
        int mime = flags & MAGIC_MIME;

        String subtype = null;
        String subtype_mime = null;

        int has_escapes = 0;
        int has_backspace = 0;
        int seen_cr = 0;

        int n_crlf = 0;
        int n_lf = 0;
        int n_cr = 0;
        int n_nel = 0;

        int last_line_end = -1;
        int has_long_lines = 0;

        if ((flags & MAGIC_APPLE) != 0) {
            return 0;
        }

        nbytes = trim_nuls(buf, nbytes);

        /* If we have fewer than 2 bytes, give up. */
        if (nbytes <= 1) {
            rv = 0;
            return rv;
        }

        /* Convert ubuf to UTF-8 and try text soft magic */
        /* malloc size is a conservative overestimate; could be
            improved, or at least realloced after conversion. */
        mlen = ulen * 6;
        utf8_buf = new Pointer(new byte[mlen]);
        if ((utf8_end = encode_utf8(utf8_buf, mlen, ubuf, ulen)) == null) {
            return rv;
        }
        if ((rv = file_softmagic(utf8_buf, utf8_end.minus(utf8_buf),
                TEXTTEST)) != 0) {
            return rv;
        } else {
            rv = -1;
        }

        /* look for tokens from names.h - this is expensive! */
subtype_identification_loop:
        if ((flags & MAGIC_NO_CHECK_TOKENS) == 0) {

            i = 0;
            while (i < ulen) {
                int end;

                /* skip past any leading space */
                while (i < ulen && ISSPC(ubuf[i])) {
                    i++;
                }
                if (i >= ulen) {
                    break;
                }

                /* find the next whitespace */
                for (end = i + 1; end < nbytes; end++) {
                    if (ISSPC(ubuf[end])) {
                        break;
                    }
                }

                /* compare the word thus isolated against the token list */
                for (int j = 0; j < Names.names.length; j++) {
                    p = Names.names[j];
                    if (ascmatch(p.name, ubuf, i, end - i) != 0) {
                        subtype = MimeName.types[p.type].human;
                        subtype_mime = MimeName.types[p.type].mime;
                        break subtype_identification_loop;
                    }
                }

                i = end;
            }
        }

        /* Now try to discover other details about the file. */
        for (i = 0; i < ulen; i++) {
            if (ubuf[i] == '\n') {
                if (seen_cr != 0) {
                    n_crlf++;
                } else {
                    n_lf++;
                }
                last_line_end = i;
            } else if (seen_cr != 0) {
                n_cr++;
            }

            seen_cr = (ubuf[i] == '\r' ? 1 : 0);
            if (seen_cr != 0) {
                last_line_end = i;
            }

            if (ubuf[i] == 0x85) { /* X3.64/ECMA-43 "next line" character */
                n_nel++;
                last_line_end = i;
            }

            /* If this line is _longer_ than MAXLINELEN, remember it. */
            if (i > last_line_end + MAXLINELEN) {
                has_long_lines = 1;
            }

            if (ubuf[i] == '\033') {
                has_escapes = 1;
            }
            if (ubuf[i] == '\b') {
                has_backspace = 1;
            }
        }

        /* Beware, if the data has been truncated, the final CR could have
            been followed by a LF.  If we have HOWMANY bytes, it indicates
            that the data might have been truncated, probably even before
            this function was called. */
        if (seen_cr != 0 && nbytes < HOWMANY) {
            n_cr++;
        }

        if (strcmp(type.toString(), "binary") == 0) {
            rv = 0;
            return rv;
        }
        if (mime != 0) {
            if ((mime & MAGIC_MIME_TYPE) != 0) {
                if (subtype_mime != null) {
                    if (file_printf("%s", subtype_mime) == -1) {
                        return rv;
                    }
                } else {
                    if (file_printf("text/plain") == -1) {
                        return rv;
                    }
                }
            }
        } else {
            if (file_printf("%s", code) == -1) {
                return rv;
            }

            if (subtype != null) {
                if (file_printf(" %s", subtype) == -1) {
                    return rv;
                }
            }

            if (file_printf(" %s", type) == -1) {
                return rv;
            }

            if (has_long_lines != 0) {
                if (file_printf(", with very long lines") == -1) {
                    return rv;
                }
            }

            /*
             * Only report line terminators if we find one other than LF,
             * or if we find none at all.
             */
            if ((n_crlf == 0 && n_cr == 0 && n_nel == 0 && n_lf == 0) ||
                    (n_crlf != 0 || n_cr != 0 || n_nel != 0)) {
                if (file_printf(", with") == -1) {
                    return rv;
                }

                if (n_crlf == 0 && n_cr == 0 && n_nel == 0 && n_lf == 0) {
                    if (file_printf(" no") == -1) {
                        return rv;
                    }
                } else {
                    if (n_crlf != 0) {
                        if (file_printf(" CRLF") == -1) {
                            return rv;
                        }
                        if (n_cr != 0 || n_lf != 0 || n_nel != 0) {
                            if (file_printf(",") == -1) {
                                return rv;
                            }
                        }
                    }
                    if (n_cr != 0) {
                        if (file_printf(" CR") == -1) {
                            return rv;
                        }
                        if (n_lf != 0 || n_nel != 0) {
                            if (file_printf(",") == -1) {
                                return rv;
                            }
                        }
                    }
                    if (n_lf != 0) {
                        if (file_printf(" LF") == -1) {
                            return rv;
                        }
                        if (n_nel != 0) {
                            if (file_printf(",") == -1) {
                                return rv;
                            }
                        }
                    }
                    if (n_nel != 0) {
                        if (file_printf(" NEL") == -1) {
                            return rv;
                        }
                    }
                }

                if (file_printf(" line terminators") == -1) {
                    return rv;
                }
            }

            if (has_escapes != 0) {
                if (file_printf(", with escape sequences") == -1) {
                    return rv;
                }
            }
            if (has_backspace != 0) {
                if (file_printf(", with overstriking") == -1) {
                    return rv;
                }
            }
        }
        rv = 1;
        return rv;
    }

    private int file_ascmagic(byte[] buf, int nbytes) {
        char[][] ubuf = {null};
        int[] ulen = {0};
        int rv = 1;

        StringBuilder code = new StringBuilder();
        StringBuilder code_mime = new StringBuilder();
        StringBuilder type = new StringBuilder();

        if ((flags & MAGIC_APPLE) != 0) {
            return 0;
        }

        nbytes = trim_nuls(buf, nbytes);

        /* If file doesn't look like any sort of text, give up. */
        if (file_encoding(buf, nbytes, ubuf, ulen, code, code_mime, type) ==
                0) {
            rv = 0;
            return rv;
        }

        rv = file_ascmagic_with_encoding(buf, nbytes, ubuf[0], ulen[0], code,
                type);

        return rv;
    }

    private int file_softmagic(Pointer buf, int nbytes, int mode) {
        MList ml;
        int rv;
        for (ml = mlist.next; ml != mlist; ml = ml.next) {
            if ((rv = match(ml.magic, ml.nmagic, buf, nbytes, mode)) != 0) {
                return rv;
            }
        }

        return 0;
    }

    private int match(Magic[] magic, int nmagic, Pointer s, int nbytes,
            int mode) {

        int magindex = 0;
        int cont_level = 0;
        int need_separator = 0;
        int returnval = 0, e; /* if a match is found it is set to 1*/
        int firstline = 1; /* a flag to print X\n  X\n- X */
        int printed_something = 0;
        int print = (flags & (MAGIC_MIME | MAGIC_APPLE)) == 0 ? 1 : 0;

        if (file_check_mem(cont_level) == -1) {
            return -1;
        }

        for (magindex = 0; magindex < nmagic; magindex++) {
            int flush = 0;
            Magic m = magic[magindex];
            if ((flags & MAGIC_DEBUG) != 0) {
                stdout.printf("%d -- %d-%s\n", magindex, m.cont_level, m);
            }

            if ((m.flag & BINTEST) != mode) {
                /* Skip sub-tests */
                while (magindex + 1 < magic.length &&
                        magic[magindex + 1].cont_level != 0 &&
                        ++magindex < nmagic) {
                    continue;
                }
                continue; /* Skip to next top-level test*/
            }

            offset = m.offset;
            line = m.lineno;

            /* if main entry matches, print it... */
            switch (mget(s, m, nbytes, cont_level)) {
            case -1:
                return -1;
            case 0:
                flush = m.reln != '!' ? 1 : 0;
                break;
            default:
                if (m.type == FILE_INDIRECT) {
                    returnval = 1;
                }

                switch (magiccheck(m)) {
                case -1:
                    return -1;
                case 0:
                    flush++;
                    break;
                default:
                    flush = 0;
                    break;
                }
                break;
            }
            if (flush != 0) {
                /*
                 * main entry didn't match,
                 * flush its continuations
                 */
                while (magindex < nmagic - 1 &&
                        magic[magindex + 1].cont_level != 0) {
                    magindex++;
                    if ((flags & MAGIC_DEBUG) != 0) {
                        stdout.printf("%d -- %d-%s -- Skipping\n", magindex,
                                magic[magindex].cont_level, magic[magindex]);
                    }
                }
                continue;
            }

            /*
             * If we are going to print something, we'll need to print
             * a blank before we print something else.
             */
            if (m.desc != null && m.desc.get() != '\0') {
                need_separator = 1;
                printed_something = 1;
                if ((e = handle_annotation(m)) != 0) {
                    return e;
                }
                if (print_sep(firstline) == -1) {
                    return -1;
                }
            }

            if (print != 0 && mprint(m) == -1) {
                return -1;
            }

            c.li.get(cont_level).off = moffset(m);

            /* and any continuations that match */
            if (file_check_mem(++cont_level) == -1) {
                return -1;
            }

            while (magindex + 1 < magic.length &&
                    magic[magindex + 1].cont_level != 0 &&
                    ++magindex < nmagic) {
                m = magic[magindex];
                line = m.lineno; /* for messages */

                if (cont_level < m.cont_level) {
                    continue;
                }
                if (cont_level > m.cont_level) {
                    /*
                     * We're at the end of the level
                     * "cont_level" continuations.
                     */
                    cont_level = m.cont_level;
                }
                offset = m.offset;
                if ((m.flag & OFFADD) != 0) {
                    offset += c.li.get(cont_level - 1).off;
                }

                if (m.cond == COND_ELSE || m.cond == COND_ELIF) {
                    if (c.li.get(cont_level).last_match == 1) {
                        continue;
                    }
                }
                switch (mget(s, m, nbytes, cont_level)) {
                case -1:
                    return -1;
                case 0:
                    if (m.reln != '!') {
                        continue;
                    }
                    flush = 1;
                    break;
                default:
                    if (m.type == FILE_INDIRECT) {
                        returnval = 1;
                    }
                    flush = 0;
                    break;
                }

                switch (flush != 0 ? 1 : magiccheck(m)) {
                case -1:
                    return -1;
                case 0:
                    c.li.get(cont_level).last_match = 0;
                    break;
                default:
                    c.li.get(cont_level).last_match = 1;
                    if (m.type != FILE_DEFAULT) {
                        c.li.get(cont_level).got_match = 1;
                    } else if (c.li.get(cont_level).got_match != 0) {
                        c.li.get(cont_level).got_match = 0;
                        break;
                    }
                    /*
                     * If we are going to print something,
                     * make sure that we have a separator first.
                     */
                    if (m.desc != null && m.desc.get() != '\0') {
                        if ((e = handle_annotation(m)) != 0) {
                            return e;
                        }
                        if (printed_something == 0) {
                            printed_something = 1;
                            if (print_sep(firstline) == -1) {
                                return -1;
                            }
                        }
                    }
                    /*
                     * This continuation matched.  Print
                     * its message, with a blank before it
                     * if the previous item printed and
                     * this item isn't empty.
                     */
                    /* space if previous printed */
                    if (need_separator != 0 && (m.flag & NOSPACE) == 0 &&
                            m.desc != null && m.desc.get() != '\0') {
                        if (print != 0 && file_printf(" ") == -1) {
                            return -1;
                        }
                        need_separator = 0;
                    }
                    if (print != 0 && mprint(m) == -1) {
                        return -1;
                    }

                    c.li.get(cont_level).off = moffset(m);

                    if (m.desc != null && m.desc.get() != '\0') {
                        need_separator = 1;
                    }

                    /*
                     * If we see any continuations
                     * at a higher level,
                     * process them.
                     */
                    if (file_check_mem(++cont_level) == -1) {
                        return -1;
                    }
                    break;
                }
            }
            if (printed_something != 0) {
                firstline = 0;
                if (print != 0) {
                    returnval = 1;
                }
            }
            if ((flags & MAGIC_CONTINUE) == 0 && printed_something != 0) {
                return returnval; /* don't keep searching */
            }
        }
        return returnval;  /* This is hit if -k is set or there is no match */
    }

    private int moffset(Magic m) {
        switch (m.type) {
        case FILE_BYTE:
            return offset + 1;

        case FILE_SHORT:
        case FILE_BESHORT:
        case FILE_LESHORT:
            return offset + 2;

        case FILE_LONG:
        case FILE_BELONG:
        case FILE_LELONG:
        case FILE_MELONG:
            return offset + 4;

        case FILE_QUAD:
        case FILE_BEQUAD:
        case FILE_LEQUAD:
            return offset + 8;

        case FILE_STRING:
        case FILE_PSTRING:
        case FILE_BESTRING16:
        case FILE_LESTRING16:
            if (m.reln == '=' || m.reln == '!') {
                return offset + m.vallen;
            } else {
                ValueType p = ms_value;
                int t;

                if (m.value.s().get(0) == '\0') {
                    p.s().set(strcspn(p.s(), "\n"), '\0');
                }
                t = offset + strlen(p.s());
                if (m.type == FILE_PSTRING) {
                    t++;
                }
                return t;
            }

        case FILE_DATE:
        case FILE_BEDATE:
        case FILE_LEDATE:
        case FILE_MEDATE:
            return offset + 4;

        case FILE_LDATE:
        case FILE_BELDATE:
        case FILE_LELDATE:
        case FILE_MELDATE:
            return offset + 4;

        case FILE_QDATE:
        case FILE_BEQDATE:
        case FILE_LEQDATE:
            return offset + 8;

        case FILE_QLDATE:
        case FILE_BEQLDATE:
        case FILE_LEQLDATE:
            return offset + 8;

        case FILE_FLOAT:
        case FILE_BEFLOAT:
        case FILE_LEFLOAT:
            return offset + 4;

        case FILE_DOUBLE:
        case FILE_BEDOUBLE:
        case FILE_LEDOUBLE:
            return offset + 8;

        case FILE_REGEX:
            if ((m.str_flags & REGEX_OFFSET_START) != 0) {
                return search.offset;
            } else {
                return search.offset + search.rm_len;
            }

        case FILE_SEARCH:
            if ((m.str_flags & REGEX_OFFSET_START) != 0) {
                return search.offset;
            } else {
                return search.offset + m.vallen;
            }

        case FILE_DEFAULT:
            return offset;

        case FILE_INDIRECT:
            return offset;

        default:
            return 0;
        }
    }

    private int mprint(Magic m) {
        long v;
        float vf;
        double vd;
        int t = 0;
        ValueType p = ms_value;

        switch (m.type) {
        case FILE_BYTE:
            v = file_signextend(m, (long) p.b());
            switch (check_fmt(m)) {
            case -1:
                return -1;
            case 1:
                if (file_printf(m.desc, Character.toString((char) v)) == -1) {
                    return -1;
                }
                break;
            default:
                if (file_printf(m.desc, (byte) v) == -1) {
                    return -1;
                }
                break;
            }
            t = offset + 1;
            break;

        case FILE_SHORT:
        case FILE_BESHORT:
        case FILE_LESHORT:
            v = file_signextend(m, (long) p.h());
            switch (check_fmt(m)) {
            case -1:
                return -1;
            case 1:
                if (file_printf(m.desc, Short.toString((short) v)) == -1) {
                    return -1;
                }
                break;
            default:
                if (file_printf(m.desc, (short) v) == -1) {
                    return -1;
                }
                break;
            }
            t = offset + 2;
            break;

        case FILE_LONG:
        case FILE_BELONG:
        case FILE_LELONG:
        case FILE_MELONG:
            v = file_signextend(m, (long) p.l());
            switch (check_fmt(m)) {
            case -1:
                return -1;
            case 1:
                if (file_printf(m.desc, Integer.toString((int) v)) == -1) {
                    return -1;
                }
                break;
            default:
                if (file_printf(m.desc, (int) v) == -1) {
                    return -1;
                }
                break;
            }
            t = offset + 4;
            break;

        case FILE_QUAD:
        case FILE_BEQUAD:
        case FILE_LEQUAD:
            v = file_signextend(m, p.q());
            if (file_printf(m.desc, v) == -1) {
                return -1;
            }
            t = offset + 8;
            break;

        case FILE_STRING:
        case FILE_PSTRING:
        case FILE_BESTRING16:
        case FILE_LESTRING16:
            if (m.reln == '=' || m.reln == '!') {
                if (file_printf(m.desc, m.value.s()) == -1) {
                    return -1;
                }
                t = offset + m.vallen;
            } else {
                if (m.value.s().get(0) == '\0') {
                    p.s().set(strcspn(p.s(), "\n"), '\0');
                }
                if (file_printf(m.desc, p.s()) == -1) {
                    return -1;
                }
                t = offset + strlen(p.s());
                if (m.type == FILE_PSTRING) {
                    t++;
                }
            }
            break;

        case FILE_DATE:
        case FILE_BEDATE:
        case FILE_LEDATE:
        case FILE_MEDATE:
            if (file_printf(m.desc, file_fmttime(p.l(), 1)) == -1) {
                return -1;
            }
            t = offset + 4;
            break;

        case FILE_LDATE:
        case FILE_BELDATE:
        case FILE_LELDATE:
        case FILE_MELDATE:
            if (file_printf(m.desc, file_fmttime(p.l(), 0)) == -1) {
                return -1;
            }
            t = offset + 4;
            break;

        case FILE_QDATE:
        case FILE_BEQDATE:
        case FILE_LEQDATE:
            if (file_printf(m.desc, file_fmttime((int) p.q(), 1)) == -1) {
                return -1;
            }
            t = offset + 8;
            break;

        case FILE_QLDATE:
        case FILE_BEQLDATE:
        case FILE_LEQLDATE:
            if (file_printf(m.desc, file_fmttime((int) p.q(), 0)) == -1) {
                return -1;
            }
            t = offset + 8;
            break;

        case FILE_FLOAT:
        case FILE_BEFLOAT:
        case FILE_LEFLOAT:
            vf = p.f();
            switch (check_fmt(m)) {
            case -1:
                return -1;
            case 1:
                if (file_printf(m.desc, Float.toString(vf)) == -1) {
                    return -1;
                }
                break;
            default:
                if (file_printf(m.desc, vf) == -1) {
                    return -1;
                }
                break;
            }
            t = offset + 4;
            break;

        case FILE_DOUBLE:
        case FILE_BEDOUBLE:
        case FILE_LEDOUBLE:
            vd = p.d();
            switch (check_fmt(m)) {
            case -1:
                return -1;
            case 1:
                if (file_printf(m.desc, Double.toString(vd)) == -1) {
                    return -1;
                }
                break;
            default:
                if (file_printf(m.desc, vd) == -1) {
                    return -1;
                }
                break;
            }
            t = offset + 8;
            break;

        case FILE_REGEX: {
            int rval;

            rval = file_printf(m.desc, search.s);

            if (rval == -1) {
                return -1;
            }

            if ((m.str_flags & REGEX_OFFSET_START) != 0) {
                t = search.offset;
            } else {
                t = search.offset + search.rm_len;
            }
            break;
        }

        case FILE_SEARCH:
            if (file_printf(m.desc, search.s) == -1) {
                return -1;
            }
            if ((m.str_flags & REGEX_OFFSET_START) != 0) {
                t = search.offset;
            } else {
                t = search.offset + m.vallen;
            }
            break;

        case FILE_DEFAULT:
            if (file_printf(m.desc, m.value.s()) == -1) {
                return -1;
            }
            t = offset;
            break;

        case FILE_INDIRECT:
            t = offset;
            break;

        default:
            file_magerror(0, "invalid m.type (%d) in mprint()", m.type);
            return -1;
        }
        return t;
    }

    private int print_sep(int firstline) {
        if ((flags & MAGIC_MIME) != 0) {
            return 0;
        }
        if (firstline > 0) {
            return 0;
        }
        /*
         * we found another match
         * put a newline and '-' to do some simple formatting
         */
        return file_printf("\n- ");
    }

    private int handle_annotation(Magic m) {
        if ((flags & MAGIC_APPLE) != 0) {
            if (file_printf("%.8s", m.apple) == -1) {
                return -1;
            }
            return 1;
        }
        if ((flags & MAGIC_MIME_TYPE) != 0 && m.mimetype.get() != '\0') {
            if (file_printf("%s", m.mimetype) == -1) {
                return -1;
            }
            return 1;
        }
        return 0;
    }

    int magiccheck(Magic m) {
        long l = m.value.q();
        long v;
        float fl, fv;
        double dl, dv;
        int matched;
        ValueType p = ms_value;

        switch (m.type) {
        case FILE_BYTE:
            v = p.b();
            break;

        case FILE_SHORT:
        case FILE_BESHORT:
        case FILE_LESHORT:
            v = p.h();
            break;

        case FILE_LONG:
        case FILE_BELONG:
        case FILE_LELONG:
        case FILE_MELONG:
        case FILE_DATE:
        case FILE_BEDATE:
        case FILE_LEDATE:
        case FILE_MEDATE:
        case FILE_LDATE:
        case FILE_BELDATE:
        case FILE_LELDATE:
        case FILE_MELDATE:
            v = p.l();
            break;

        case FILE_QUAD:
        case FILE_LEQUAD:
        case FILE_BEQUAD:
        case FILE_QDATE:
        case FILE_BEQDATE:
        case FILE_LEQDATE:
        case FILE_QLDATE:
        case FILE_BEQLDATE:
        case FILE_LEQLDATE:
            v = p.q();
            break;

        case FILE_FLOAT:
        case FILE_BEFLOAT:
        case FILE_LEFLOAT:
            fl = m.value.f();
            fv = p.f();
            switch (m.reln) {
            case 'x':
                matched = 1;
                break;

            case '!':
                matched = fv != fl ? 1 : 0;
                break;

            case '=':
                matched = fv == fl ? 1 : 0;
                break;

            case '>':
                matched = fv > fl ? 1 : 0;
                break;

            case '<':
                matched = fv < fl ? 1 : 0;
                break;

            default:
                matched = 0;
                file_magerror(0,
                        "cannot happen with float: invalid relation `%c'",
                        m.reln);
                return -1;
            }
            return matched;

        case FILE_DOUBLE:
        case FILE_BEDOUBLE:
        case FILE_LEDOUBLE:
            dl = m.value.d();
            dv = p.d();
            switch (m.reln) {
            case 'x':
                matched = 1;
                break;

            case '!':
                matched = dv != dl ? 1 : 0;
                break;

            case '=':
                matched = dv == dl ? 1 : 0;
                break;

            case '>':
                matched = dv > dl ? 1 : 0;
                break;

            case '<':
                matched = dv < dl ? 1 : 0;
                break;

            default:
                matched = 0;
                file_magerror(0,
                        "cannot happen with double: invalid relation `%c'",
                        m.reln);
                return -1;
            }
            return matched;

        case FILE_DEFAULT:
            l = 0;
            v = 0;
            break;

        case FILE_STRING:
        case FILE_PSTRING:
            l = 0;
            v = file_strncmp(m.value.s(), p.s(), m.vallen, m.str_flags);
            break;

        case FILE_BESTRING16:
        case FILE_LESTRING16:
            l = 0;
            v = file_strncmp16(m.value.s(), p.s(), m.vallen, m.str_flags);
            break;

        case FILE_SEARCH: { /* search search.s for the string m.value.s() */
            int slen;
            int idx;

            if (search.s == null) {
                return 0;
            }

            slen = Math.min(m.vallen, ValueType.MAXstring);
            l = 0;
            v = 0;

            for (idx = 0; m.str_range == 0 || idx < m.str_range; idx++) {
                if (slen + idx > search.s_len) {
                    break;
                }

                v = file_strncmp(m.value.s(), search.s.plus(idx), slen,
                        m.str_flags);
                if (v == 0) {        /* found match */
                    search.offset += idx;
                    break;
                }
            }
            break;
        }
        case FILE_REGEX: {
            if (search.s == null) {
                return 0;
            }

            l = 0;
            try {
                Pattern pat = Pattern.compile(m.value.s().toString(),
                        Pattern.MULTILINE |
                                ((m.str_flags & STRING_IGNORE_CASE) != 0 ?
                                        Pattern.CASE_INSENSITIVE :
                                        0));
                Matcher matcher = pat.matcher(search.s.toString());
                if (matcher.find()) {
                    search.s = search.s.plus(matcher.start());
                    search.offset += matcher.start();
                    search.rm_len = matcher.end() - matcher.start();
                    v = 0;
                } else {
                    v = 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
                file_magerror(0, "regex error: %s", e.getMessage());
                v = -1;
            }
            if (v == -1) {
                return -1;
            }
            break;
        }
        case FILE_INDIRECT:
            return 1;
        default:
            file_magerror(0, "invalid type %d in magiccheck()", m.type);
            return -1;
        }

        v = file_signextend(m, v);
        l = file_signextend(m, l);

        switch (m.reln) {
        case 'x':
            if ((flags & MAGIC_DEBUG) != 0) {
                stderr.printf("%d == *any* = 1\n", v);
            }
            matched = 1;
            break;

        case '!':
            matched = v != l ? 1 : 0;
            if ((flags & MAGIC_DEBUG) != 0) {
                stderr.printf("%d != %d = %d\n", v, l, matched);
            }
            break;

        case '=':
            matched = v == l ? 1 : 0;
            if ((flags & MAGIC_DEBUG) != 0) {
                stderr.printf("%d == %d = %d [%d]\n", v, l, matched, m.type);
            }
            break;

        case '>':
            if ((m.flag & UNSIGNED) != 0) {
                // This is the logic of an unsigned greater-than test
                // roughly translated, this means:
                // "(v greater than l) XOR (v and l have different signs)"
                // although "0" is treated as positive because ">" works for 0
                matched = (v > l) ^ (v < 0) ^ (l < 0) ? 1 : 0;
                if ((flags & MAGIC_DEBUG) != 0) {
                    stderr.printf("0x%x > 0x%x = %d\n", v, l, matched);
                }
            } else {
                matched = v > l ? 1 : 0;
                if ((flags & MAGIC_DEBUG) != 0) {
                    stderr.printf("%d > %d = %d\n", v, l, matched);
                }
            }
            break;

        case '<':
            if ((m.flag & UNSIGNED) != 0) {
                // This is the logic of an unsigned greater-than test
                // roughly translated, this means:
                //     "(v less than l) XOR (v and l have different signs)"
                // although "0" is treated as positive because "<" works for 0
                matched = (v < l) ^ (v < 0) ^ (l < 0) ? 1 : 0;
                if ((flags & MAGIC_DEBUG) != 0) {
                    stderr.printf("0x%x < 0x%x = %d\n", v, l, matched);
                }
            } else {
                matched = v < l ? 1 : 0;
                if ((flags & MAGIC_DEBUG) != 0) {
                    stderr.printf("%d < %d = %d\n", v, l, matched);
                }
            }
            break;

        case '&':
            matched = (v & l) == l ? 1 : 0;
            if ((flags & MAGIC_DEBUG) != 0) {
                stderr.printf("(0x%x & 0x%x) == 0x%x = %d\n", v, l, l, matched);
            }
            break;

        case '^':
            matched = (v & l) != l ? 1 : 0;
            if ((flags & MAGIC_DEBUG) != 0) {
                stderr.printf("(0x%x & 0x%x) != 0x%x = %d\n", v, l, l, matched);
            }
            break;

        default:
            matched = 0;
            file_magerror(0, "cannot happen: invalid relation `%c'", m.reln);
            return -1;
        }

        return matched;
    }

    int mget(Pointer s, Magic m, int nbytes, int cont_level) {
        int offset = this.offset;
        int count = m.str_range;
        ValueType p = ms_value;

        if (mcopy(p, m.type, m.flag & INDIR, s, offset, nbytes, count) == -1) {
            return -1;
        }

        if ((flags & MAGIC_DEBUG) != 0) {
            TypeTblS.mdebug(offset, p.s(), ValueType.SIZE);
            file_mdump(m);
        }

        if ((m.flag & INDIR) != 0) {
            int off = m.in_offset;
            if ((m.in_op & FILE_OPINDIRECT) != 0) {
                ValueType q = new ValueType(new Pointer(s, offset + off));
                switch (m.in_type) {
                case FILE_BYTE:
                    off = q.b();
                    break;
                case FILE_SHORT:
                    off = q.h();
                    break;
                case FILE_BESHORT:
                    off = (short) ((q.hs()[0] << 8) | (q.hs()[1]));
                    break;
                case FILE_LESHORT:
                    off = (short) ((q.hs()[1] << 8) | (q.hs()[0]));
                    break;
                case FILE_LONG:
                    off = q.l();
                    break;
                case FILE_BELONG:
                case FILE_BEID3:
                    off = (q.hl()[0] << 24) | (q.hl()[1] << 16) |
                            (q.hl()[2] << 8) | (q.hl()[3]);
                    break;
                case FILE_LEID3:
                case FILE_LELONG:
                    off = (q.hl()[3] << 24) | (q.hl()[2] << 16) |
                            (q.hl()[1] << 8) | (q.hl()[0]);
                    break;
                case FILE_MELONG:
                    off = (q.hl()[1] << 24) | (q.hl()[0] << 16) |
                            (q.hl()[3] << 8) | (q.hl()[2]);
                    break;
                }
            }
            switch (m.in_type) {
            case FILE_BYTE:
                if (nbytes < (offset + 1)) {
                    return 0;
                }
                if (off != 0) {
                    switch (m.in_op & FILE_OPS_MASK) {
                    case FILE_OPAND:
                        offset = p.b() & off;
                        break;
                    case FILE_OPOR:
                        offset = p.b() | off;
                        break;
                    case FILE_OPXOR:
                        offset = p.b() ^ off;
                        break;
                    case FILE_OPADD:
                        offset = p.b() + off;
                        break;
                    case FILE_OPMINUS:
                        offset = p.b() - off;
                        break;
                    case FILE_OPMULTIPLY:
                        offset = p.b() * off;
                        break;
                    case FILE_OPDIVIDE:
                        offset = p.b() / off;
                        break;
                    case FILE_OPMODULO:
                        offset = p.b() % off;
                        break;
                    }
                } else {
                    offset = p.b();
                }
                if ((m.in_op & FILE_OPINVERSE) != 0) {
                    offset = ~offset;
                }
                break;
            case FILE_BESHORT:
                if (nbytes < (offset + 2)) {
                    return 0;
                }
                if (off != 0) {
                    switch (m.in_op & FILE_OPS_MASK) {
                    case FILE_OPAND:
                        offset = (short) ((p.hs()[0] << 8) | (p.hs()[1])) & off;
                        break;
                    case FILE_OPOR:
                        offset = (short) ((p.hs()[0] << 8) | (p.hs()[1])) | off;
                        break;
                    case FILE_OPXOR:
                        offset = (short) ((p.hs()[0] << 8) | (p.hs()[1])) ^ off;
                        break;
                    case FILE_OPADD:
                        offset = (short) ((p.hs()[0] << 8) | (p.hs()[1])) + off;
                        break;
                    case FILE_OPMINUS:
                        offset = (short) ((p.hs()[0] << 8) | (p.hs()[1])) - off;
                        break;
                    case FILE_OPMULTIPLY:
                        offset = (short) ((p.hs()[0] << 8) | (p.hs()[1])) * off;
                        break;
                    case FILE_OPDIVIDE:
                        offset = (short) ((p.hs()[0] << 8) | (p.hs()[1])) / off;
                        break;
                    case FILE_OPMODULO:
                        offset = (short) ((p.hs()[0] << 8) | (p.hs()[1])) % off;
                        break;
                    }
                } else {
                    offset = (short) ((p.hs()[0] << 8) | (p.hs()[1]));
                }
                if ((m.in_op & FILE_OPINVERSE) != 0) {
                    offset = ~offset;
                }
                break;
            case FILE_LESHORT:
                if (nbytes < (offset + 2)) {
                    return 0;
                }
                if (off != 0) {
                    switch (m.in_op & FILE_OPS_MASK) {
                    case FILE_OPAND:
                        offset = (short) ((p.hs()[1] << 8) | (p.hs()[0])) & off;
                        break;
                    case FILE_OPOR:
                        offset = (short) ((p.hs()[1] << 8) | (p.hs()[0])) | off;
                        break;
                    case FILE_OPXOR:
                        offset = (short) ((p.hs()[1] << 8) | (p.hs()[0])) ^ off;
                        break;
                    case FILE_OPADD:
                        offset = (short) ((p.hs()[1] << 8) | (p.hs()[0])) + off;
                        break;
                    case FILE_OPMINUS:
                        offset = (short) ((p.hs()[1] << 8) | (p.hs()[0])) - off;
                        break;
                    case FILE_OPMULTIPLY:
                        offset = (short) ((p.hs()[1] << 8) | (p.hs()[0])) * off;
                        break;
                    case FILE_OPDIVIDE:
                        offset = (short) ((p.hs()[1] << 8) | (p.hs()[0])) / off;
                        break;
                    case FILE_OPMODULO:
                        offset = (short) ((p.hs()[1] << 8) | (p.hs()[0])) % off;
                        break;
                    }
                } else {
                    offset = (short) ((p.hs()[1] << 8) | (p.hs()[0]));
                }
                if ((m.in_op & FILE_OPINVERSE) != 0) {
                    offset = ~offset;
                }
                break;
            case FILE_SHORT:
                if (nbytes < (offset + 2)) {
                    return 0;
                }
                if (off != 0) {
                    switch (m.in_op & FILE_OPS_MASK) {
                    case FILE_OPAND:
                        offset = p.h() & off;
                        break;
                    case FILE_OPOR:
                        offset = p.h() | off;
                        break;
                    case FILE_OPXOR:
                        offset = p.h() ^ off;
                        break;
                    case FILE_OPADD:
                        offset = p.h() + off;
                        break;
                    case FILE_OPMINUS:
                        offset = p.h() - off;
                        break;
                    case FILE_OPMULTIPLY:
                        offset = p.h() * off;
                        break;
                    case FILE_OPDIVIDE:
                        offset = p.h() / off;
                        break;
                    case FILE_OPMODULO:
                        offset = p.h() % off;
                        break;
                    }
                } else {
                    offset = p.h();
                }
                if ((m.in_op & FILE_OPINVERSE) != 0) {
                    offset = ~offset;
                }
                break;
            case FILE_BELONG:
            case FILE_BEID3:
                if (nbytes < (offset + 4)) {
                    return 0;
                }
                if (off != 0) {
                    switch (m.in_op & FILE_OPS_MASK) {
                    case FILE_OPAND:
                        offset = ((p.hl()[0] << 24) | (p.hl()[1] << 16) |
                                (p.hl()[2] << 8) | (p.hl()[3])) & off;
                        break;
                    case FILE_OPOR:
                        offset = (p.hl()[0] << 24) | (p.hl()[1] << 16) |
                                (p.hl()[2] << 8) | (p.hl()[3]) | off;
                        break;
                    case FILE_OPXOR:
                        offset = ((p.hl()[0] << 24) | (p.hl()[1] << 16) |
                                (p.hl()[2] << 8) | (p.hl()[3])) ^ off;
                        break;
                    case FILE_OPADD:
                        offset = ((p.hl()[0] << 24) | (p.hl()[1] << 16) |
                                (p.hl()[2] << 8) | (p.hl()[3])) + off;
                        break;
                    case FILE_OPMINUS:
                        offset = ((p.hl()[0] << 24) | (p.hl()[1] << 16) |
                                (p.hl()[2] << 8) | (p.hl()[3])) - off;
                        break;
                    case FILE_OPMULTIPLY:
                        offset = ((p.hl()[0] << 24) | (p.hl()[1] << 16) |
                                (p.hl()[2] << 8) | (p.hl()[3])) * off;
                        break;
                    case FILE_OPDIVIDE:
                        offset = ((p.hl()[0] << 24) | (p.hl()[1] << 16) |
                                (p.hl()[2] << 8) | (p.hl()[3])) / off;
                        break;
                    case FILE_OPMODULO:
                        offset = ((p.hl()[0] << 24) | (p.hl()[1] << 16) |
                                (p.hl()[2] << 8) | (p.hl()[3])) % off;
                        break;
                    }
                } else {
                    offset = (p.hl()[0] << 24) | (p.hl()[1] << 16) |
                            (p.hl()[2] << 8) | (p.hl()[3]);
                }
                if ((m.in_op & FILE_OPINVERSE) != 0) {
                    offset = ~offset;
                }
                break;
            case FILE_LELONG:
            case FILE_LEID3:
                if (nbytes < (offset + 4)) {
                    return 0;
                }
                if (off != 0) {
                    switch (m.in_op & FILE_OPS_MASK) {
                    case FILE_OPAND:
                        offset = ((p.hl()[3] << 24) | (p.hl()[2] << 16) |
                                (p.hl()[1] << 8) | (p.hl()[0])) & off;
                        break;
                    case FILE_OPOR:
                        offset = (p.hl()[3] << 24) | (p.hl()[2] << 16) |
                                (p.hl()[1] << 8) | (p.hl()[0]) | off;
                        break;
                    case FILE_OPXOR:
                        offset = ((p.hl()[3] << 24) | (p.hl()[2] << 16) |
                                (p.hl()[1] << 8) | (p.hl()[0])) ^ off;
                        break;
                    case FILE_OPADD:
                        offset = ((p.hl()[3] << 24) | (p.hl()[2] << 16) |
                                (p.hl()[1] << 8) | (p.hl()[0])) + off;
                        break;
                    case FILE_OPMINUS:
                        offset = ((p.hl()[3] << 24) | (p.hl()[2] << 16) |
                                (p.hl()[1] << 8) | (p.hl()[0])) - off;
                        break;
                    case FILE_OPMULTIPLY:
                        offset = ((p.hl()[3] << 24) | (p.hl()[2] << 16) |
                                (p.hl()[1] << 8) | (p.hl()[0])) * off;
                        break;
                    case FILE_OPDIVIDE:
                        offset = ((p.hl()[3] << 24) | (p.hl()[2] << 16) |
                                (p.hl()[1] << 8) | (p.hl()[0])) / off;
                        break;
                    case FILE_OPMODULO:
                        offset = ((p.hl()[3] << 24) | (p.hl()[2] << 16) |
                                (p.hl()[1] << 8) | (p.hl()[0])) % off;
                        break;
                    }
                } else {
                    offset = (p.hl()[3] << 24) | (p.hl()[2] << 16) |
                            (p.hl()[1] << 8) | (p.hl()[0]);
                }
                if ((m.in_op & FILE_OPINVERSE) != 0) {
                    offset = ~offset;
                }
                break;
            case FILE_MELONG:
                if (nbytes < (offset + 4)) {
                    return 0;
                }
                if (off != 0) {
                    switch (m.in_op & FILE_OPS_MASK) {
                    case FILE_OPAND:
                        offset = ((p.hl()[1] << 24) | (p.hl()[0] << 16) |
                                (p.hl()[3] << 8) | (p.hl()[2])) & off;
                        break;
                    case FILE_OPOR:
                        offset = (p.hl()[1] << 24) | (p.hl()[0] << 16) |
                                (p.hl()[3] << 8) | (p.hl()[2]) | off;
                        break;
                    case FILE_OPXOR:
                        offset = ((p.hl()[1] << 24) | (p.hl()[0] << 16) |
                                (p.hl()[3] << 8) | (p.hl()[2])) ^ off;
                        break;
                    case FILE_OPADD:
                        offset = ((p.hl()[1] << 24) | (p.hl()[0] << 16) |
                                (p.hl()[3] << 8) | (p.hl()[2])) + off;
                        break;
                    case FILE_OPMINUS:
                        offset = ((p.hl()[1] << 24) | (p.hl()[0] << 16) |
                                (p.hl()[3] << 8) | (p.hl()[2])) - off;
                        break;
                    case FILE_OPMULTIPLY:
                        offset = ((p.hl()[1] << 24) | (p.hl()[0] << 16) |
                                (p.hl()[3] << 8) | (p.hl()[2])) * off;
                        break;
                    case FILE_OPDIVIDE:
                        offset = ((p.hl()[1] << 24) | (p.hl()[0] << 16) |
                                (p.hl()[3] << 8) | (p.hl()[2])) / off;
                        break;
                    case FILE_OPMODULO:
                        offset = ((p.hl()[1] << 24) | (p.hl()[0] << 16) |
                                (p.hl()[3] << 8) | (p.hl()[2])) % off;
                        break;
                    }
                } else {
                    offset = (p.hl()[1] << 24) | (p.hl()[0] << 16) |
                            (p.hl()[3] << 8) | (p.hl()[2]);
                }
                if ((m.in_op & FILE_OPINVERSE) != 0) {
                    offset = ~offset;
                }
                break;
            case FILE_LONG:
                if (nbytes < (offset + 4)) {
                    return 0;
                }
                if (off != 0) {
                    switch (m.in_op & FILE_OPS_MASK) {
                    case FILE_OPAND:
                        offset = p.l() & off;
                        break;
                    case FILE_OPOR:
                        offset = p.l() | off;
                        break;
                    case FILE_OPXOR:
                        offset = p.l() ^ off;
                        break;
                    case FILE_OPADD:
                        offset = p.l() + off;
                        break;
                    case FILE_OPMINUS:
                        offset = p.l() - off;
                        break;
                    case FILE_OPMULTIPLY:
                        offset = p.l() * off;
                        break;
                    case FILE_OPDIVIDE:
                        offset = p.l() / off;
                        break;
                    case FILE_OPMODULO:
                        offset = p.l() % off;
                        break;
                    }
                } else {
                    offset = p.l();
                }
                if ((m.in_op & FILE_OPINVERSE) != 0) {
                    offset = ~offset;
                }
                break;
            }

            switch (m.in_type) {
            case FILE_LEID3:
            case FILE_BEID3:
                offset =
                        ((offset >> 0 & 0x7f) << 0 | (offset >> 8 & 0x7f) << 7 |
                                (offset >> 16 & 0x7f) << 14 |
                                (offset >> 24 & 0x7f) << 21) + 10;
                break;
            default:
                break;
            }

            if ((m.flag & INDIROFFADD) != 0) {
                offset += c.li.get(cont_level - 1).off;
            }
            if (mcopy(p, m.type, 0, s, offset, nbytes, count) == -1) {
                return -1;
            }
            this.offset = offset;

            if ((flags & MAGIC_DEBUG) != 0) {
                TypeTblS.mdebug(offset, p.s(), ValueType.SIZE);
                file_mdump(m);
            }
        }

        /* Verify we have enough data to match magic type */
        switch (m.type) {
        case FILE_BYTE:
            if (nbytes < (offset + 1)) /* should alway be true */ {
                return 0;
            }
            break;

        case FILE_SHORT:
        case FILE_BESHORT:
        case FILE_LESHORT:
            if (nbytes < (offset + 2)) {
                return 0;
            }
            break;

        case FILE_LONG:
        case FILE_BELONG:
        case FILE_LELONG:
        case FILE_MELONG:
        case FILE_DATE:
        case FILE_BEDATE:
        case FILE_LEDATE:
        case FILE_MEDATE:
        case FILE_LDATE:
        case FILE_BELDATE:
        case FILE_LELDATE:
        case FILE_MELDATE:
        case FILE_FLOAT:
        case FILE_BEFLOAT:
        case FILE_LEFLOAT:
            if (nbytes < (offset + 4)) {
                return 0;
            }
            break;

        case FILE_DOUBLE:
        case FILE_BEDOUBLE:
        case FILE_LEDOUBLE:
            if (nbytes < (offset + 8)) {
                return 0;
            }
            break;

        case FILE_STRING:
        case FILE_PSTRING:
        case FILE_SEARCH:
            if (nbytes < (offset + m.vallen)) {
                return 0;
            }
            break;

        case FILE_REGEX:
            if (nbytes < offset) {
                return 0;
            }
            break;

        case FILE_INDIRECT:
            if ((flags & (MAGIC_MIME | MAGIC_APPLE)) == 0 && file_printf(
                    m.desc) == -1) {
                return -1;
            }
            if (nbytes < offset) {
                return 0;
            }
            return file_softmagic(new Pointer(s, offset), nbytes - offset,
                    BINTEST);

        case FILE_DEFAULT:        /* nothing to check */
        default:
            break;
        }
        if (mconvert(m) == 0) {
            return 0;
        }
        return 1;
    }

    private int mconvert(Magic m) {
        ValueType p = ms_value;
        int rv = p.convert(m);
        if (rv == 0) {
            file_magerror(0, "invalid type %d in mconvert()", m.type);
        }
        return rv;
    }

    private int mcopy(ValueType p, byte type, int indir, Pointer s, int offset,
            int nbytes, int linecnt) {
        /*
          * Note: FILE_SEARCH and FILE_REGEX do not actually copy
          * anything, but setup pointers into the source
          */
        if (indir == 0) {
            switch (type) {
            case FILE_SEARCH:
                search.s = new Pointer(s, offset);
                search.s_len = nbytes - offset;
                search.offset = offset;
                return 0;

            case FILE_REGEX: {
                Pointer b;
                Pointer c;
                Pointer last;        /* end of search region */
                Pointer buf;        /* start of search region */
                Pointer end;
                int lines;

                if (s == null) {
                    search.s_len = 0;
                    search.s = null;
                    return 0;
                }
                buf = new Pointer(s, offset);
                end = last = new Pointer(s, nbytes);
                /* mget() guarantees buf <= last */
                for (lines = linecnt, b = buf.copy();
                     lines != 0 && ((b = memchr(c = b.copy(), '\n', end.minus(
                             b))) != null || (b = memchr(c, '\r', end.minus(
                             c))) != null);
                     lines--, b.incr()) {
                    last = b.copy();
                    if (b.get(0) == '\r' && b.get(1) == '\n') {
                        b.incr();
                    }
                }
                if (lines != 0) {
                    last = new Pointer(s, nbytes);
                }

                search.s = buf;
                search.s_len = last.minus(buf);
                search.offset = offset;
                search.rm_len = 0;
                return 0;
            }
            case FILE_BESTRING16:
            case FILE_LESTRING16: {
                Pointer src = new Pointer(s, offset);
                Pointer esrc = new Pointer(s, nbytes);
                Pointer dst = p.s();
                Pointer edst = p.s().plus(ValueType.MAXstring - 1);

                if (type == FILE_BESTRING16) {
                    src.incr();
                }

                /* check for pointer overflow */
                if (src.compareTo(new Pointer(s)) < 0) {
                    file_magerror(0, "invalid offset %u in mcopy()", offset);
                    return -1;
                }
                for (/*EMPTY*/; src.compareTo(esrc) < 0; src.incr(2), dst
                        .incr()) {
                    if (dst.compareTo(edst) < 0) {
                        dst.set(src.get());
                    } else {
                        break;
                    }
                    if (dst.get() == '\0') {
                        if (type == FILE_BESTRING16 ?
                                src.get(-1) != '\0' :
                                src.get(+1) != '\0') {
                            dst.set(' ');
                        }
                    }
                }
                edst.set('\0');
                return 0;
            }
            case FILE_STRING:        /* XXX - these two should not need */
            case FILE_PSTRING:        /* to copy anything, but do anyway. */
            default:
                break;
            }
        }

        if (offset >= nbytes) {
            p.clear();
            return 0;
        }
        if (nbytes - offset < ValueType.SIZE) {
            nbytes -= offset;
        } else {
            nbytes = ValueType.SIZE;
        }

        p.copyFrom(new Pointer(s, offset), nbytes);

        //The following seems to have no meaning, as we don't really have a union in Java
        ///*
        // * the usefulness of padding with zeroes eludes me, it
        // * might even cause problems
        // */
        //if (nbytes < ValueType.SIZE)
        //(void) memset(((char*)(void*)p)+nbytes, '\0', sizeof( * p)-nbytes);
        return 0;
    }

    private static int file_trycdf(InputStream fd, byte[] buf, int nbytes) {
        // Not done in Java, just handled with regular magic file work
        return 0;
    }

    private int file_is_tar(byte[] buf, int nbytes) {
        /*
         * Do the tar test first, because if the first file in the tar
         * archive starts with a dot, we can confuse it with an nroff file.
         */
        int tar;
        int mime = flags & MAGIC_MIME;

        if ((flags & MAGIC_APPLE) != 0) {
            return 0;
        }

        tar = is_tar(buf, nbytes);
        if (tar < 1 || tar > 3) {
            return 0;
        }

        if (file_printf("%s",
                mime != 0 ? "application/x-tar" : TarHeader.tartype[tar - 1]) ==
                -1) {
            return -1;
        }
        return 1;
    }

    private static int is_tar(byte[] buf, int nbytes) {
        TarHeader header = new TarHeader(buf);
        int i;
        int sum, recsum;
        Pointer p;

        if (nbytes < TarHeader.RECORDSIZE) {
            return 0;
        }

        recsum = from_oct(8, header.chksum());

        sum = 0;
        p = new Pointer(buf);
        for (i = TarHeader.RECORDSIZE; --i >= 0;) {
            /*
             * We cannot use unsigned char here because of old compilers,
             * e.g. V7.
             */
            sum += 0xFF & p.getIncr();
        }

        /* Adjust checksum to count the "chksum" field as blanks. */
        for (i = TarHeader.CHKSUM_SIZE; --i >= 0;) {
            sum -= 0xFF & header.chksum().get(i);
        }
        sum += ' ' * TarHeader.CHKSUM_SIZE;

        if (sum != recsum) {
            return 0;        /* Not a tar archive */
        }

        if (strcmp(header.magic(), TarHeader.GNUTMAGIC) == 0) {
            return 3;                /* GNU Unix Standard tar archive */
        }
        if (strcmp(header.magic(), TarHeader.TMAGIC) == 0) {
            return 2;                /* Unix Standard tar archive */
        }

        return 1;                        /* Old fashioned tar archive */
    }

    private int file_encoding(byte[] buf, int nbytes, char[][] ubuf, int[] ulen,
            StringBuilder code, StringBuilder code_mime, StringBuilder type) {

        int rv = 1, ucs_type;
        byte[] nbuf = null;

        ubuf[0] = new char[nbytes];

        setText(type, "text");
        if (looks_ascii(buf, nbytes, ubuf[0], ulen) != 0) {
            setText(code, "ASCII");
            setText(code_mime, "us-ascii");
        } else if (looks_utf8_with_BOM(buf, nbytes, ubuf, ulen) > 0) {
            setText(code, "UTF-8 Unicode (with BOM)");
            setText(code_mime, "utf-8");
        } else if (file_looks_utf8(new Pointer(buf, 0), nbytes, ubuf[0], ulen) >
                1) {
            setText(code, "UTF-8 Unicode");
            setText(code_mime, "utf-8");
        } else if ((ucs_type = looks_ucs16(buf, nbytes, ubuf[0], ulen)) != 0) {
            if (ucs_type == 1) {
                setText(code, "Little-endian UTF-16 Unicode");
                setText(code_mime, "utf-16le");
            } else {
                setText(code, "Big-endian UTF-16 Unicode");
                setText(code_mime, "utf-16be");
            }
        } else if (looks_latin1(buf, nbytes, ubuf[0], ulen) != 0) {
            setText(code, "ISO-8859");
            setText(code_mime, "iso-8859-1");
        } else if (looks_extended(buf, nbytes, ubuf[0], ulen) != 0) {
            setText(code, "Non-ISO extended-ASCII");
            setText(code_mime, "unknown-8bit");
        } else {
            nbuf = new byte[nbytes];
            from_ebcdic(buf, nbytes, nbuf);

            if (looks_ascii(nbuf, nbytes, ubuf[0], ulen) != 0) {
                setText(code, "EBCDIC");
                setText(code_mime, "ebcdic");
            } else if (looks_latin1(nbuf, nbytes, ubuf[0], ulen) != 0) {
                setText(code, "International EBCDIC");
                setText(code_mime, "ebcdic");
            } else { /* Doesn't look like text at all */
                rv = 0;
                setText(type, "binary");
            }
        }
        return rv;
    }

    private int file_zmagic(InputStream fd, String name, byte[] buf,
            int nbytes) {

        byte[][] newbuf = new byte[1][];
        int i, nsz;
        int rv = 0;
        int mime = flags & MAGIC_MIME;

        if ((flags & MAGIC_COMPRESS) == 0) {
            return 0;
        }

        for (i = 0; i < compr.length; i++) {
            if (nbytes < compr[i].maglen) {
                continue;
            }
            if (memcmp(new Pointer(buf), compr[i].magic, compr[i].maglen) ==
                    0 && (nsz = uncompressbuf(fd, i, buf, newbuf, nbytes)) !=
                    NODATA) {
                flags &= ~MAGIC_COMPRESS;
                rv = -1;
                if (file_buffer(null, name, newbuf[0], nsz) == -1) {
                    break;
                }

                if (mime == MAGIC_MIME || mime == 0) {
                    if (file_printf(
                            mime != 0 ? " compressed-encoding=" : " (") == -1) {
                        break;
                    }
                }

                if ((mime == 0 || (mime & MAGIC_MIME_ENCODING) != 0) &&
                        file_buffer(null, null, buf, nbytes) == -1) {
                    break;
                }

                if (mime == 0 && file_printf(")") == -1) {
                    break;
                }
                rv = 1;
                break;
            }
        }
        flags |= MAGIC_COMPRESS;
        return rv;
    }

    private int uncompressbuf(InputStream fd, int method, byte[] old,
            byte[][] newch, final int n) {

        try {
            stderr.flush();
            Runtime rt = Runtime.getRuntime();
            final Process proc = rt.exec(compr[method].argv);
            ByteArrayOutputStream out = new ByteArrayOutputStream(n) {
                int left = HOWMANY;

                @Override
                public void write(byte[] bytes, int start, int len) {
                    int toWrite = Math.min(len - start, left);
                    super.write(bytes, start, toWrite);
                    left -= toWrite;
                    if (toWrite <= 0) {
                        IOUtils.closeQuietly(this);
                        proc.destroy();
                    }
                }
            };
            if (old != null) {
                fd = new ByteArrayInputStream(old);
            }
            ThreadedCopy toProg = new ThreadedCopy("toProg", fd,
                    proc.getOutputStream());
            toProg.start();
            ThreadedCopy fromProg = new ThreadedCopy("fromProg",
                    proc.getInputStream(), out);
            fromProg.start();
            ThreadedCopy errOut = null;
            if (compr[method].silent != 0) {
                errOut = new ThreadedCopy("err", proc.getErrorStream(),
                        System.err);
                errOut.start();
            } else {
                proc.getErrorStream().close();
            }
            proc.waitFor();
            if (toProg.join() != null) {
                throw toProg.join();
            }
            fromProg.join();
            if (errOut != null) {
                errOut.join();
            }
            newch[0] = out.toByteArray();
            return newch[0].length;
        } catch (IOException e) {
            e.printStackTrace();
            return NODATA;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return NODATA;
        }
    }
}
