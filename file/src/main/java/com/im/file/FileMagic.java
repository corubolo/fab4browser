package com.im.file;

import static com.im.file.InternalUtils.file_mbswidth;
import static com.im.file.MagicSet.*;

import org.apache.commons.io.IOUtils;
import org.simplx.args.MainArgs;
import org.simplx.c.Stdio;

import static org.simplx.c.Stdio.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Formatter;
import java.util.Map;
import java.util.WeakHashMap;

public class FileMagic {
    static final String OPTSTRING = "bcCde:f:F:hikLm:nNprsvz0";

    private static final Opt[] nv = {new Opt("apptype", MAGIC_NO_CHECK_APPTYPE),
            new Opt("ascii", MAGIC_NO_CHECK_ASCII), new Opt("cdf",
                    MAGIC_NO_CHECK_CDF), new Opt("compress",
                    MAGIC_NO_CHECK_COMPRESS), new Opt("elf",
                    MAGIC_NO_CHECK_ELF), new Opt("encoding",
                    MAGIC_NO_CHECK_ENCODING), new Opt("soft",
                    MAGIC_NO_CHECK_SOFT), new Opt("tar", MAGIC_NO_CHECK_TAR),
            new Opt("tokens", MAGIC_NO_CHECK_TOKENS),};

    private static final String FILE_VERSION_MAJOR = "J.5";
    private static final String PATCHLEVEL = "1";
    private static final String PROGNAME = "file";

    private static final Map<String, MagicSet> magicSets =
            new WeakHashMap<String, MagicSet>();

    /* Global command-line options          */
    /* brief output format                  */
    private int bflag = 0;
    /* Don't pad output                     */
    private boolean nopad = false;
    /* Do not buffer stdout                 */
    private boolean nobuffer = false;
    /* Append '\0' to the separator         */
    private boolean nulsep = false;

    private Stdio out = stdout;

    private final int action;
    private String magicfile;
    private File magicpath;
    private int flags;
    private MagicSet magic;
    private final String[] files;
    private final String separator;

    private static class Opt {
        Opt(String name, int value) {
            this.name = name;
            this.value = value;
        }

        String name;
        int value;
    }

    public FileMagic(String... args) throws IOException {
        MainArgs opts = new MainArgs(PROGNAME, args);

        int i;
        action = 0;
        int didsomefiles = 0;
        flags = 0;
        magic = null;
        magicfile = null;

        if (opts.getBoolean("|apple", "output the Apple CREATOR/TYPE")) {
            flags |= MAGIC_APPLE;
        }
        if (opts.getBoolean("|mime-type", "output the MIME type")) {
            flags |= MAGIC_MIME_TYPE;
        }
        if (opts.getBoolean("|mime-encoding", "output the MIME encoding")) {
            flags |= MAGIC_MIME_ENCODING;
        }
        if (opts.getBoolean("v|version",
                "output version information and exit")) {
            stderr.printf("%s-%s.%s\n", PROGNAME, FILE_VERSION_MAJOR,
                    PATCHLEVEL);
            stderr.printf("magic file from %s\n", magicfile);
            System.exit(1);
        }
        if (opts.getBoolean("z|uncompress",
                "try to look inside compressed files")) {
            flags |= MAGIC_COMPRESS;
        }

        while (opts.getBoolean("b|brief",
                "do not prepend filenames to output lines")) {
            bflag++;
        }
        if (opts.getBoolean("c|checking-printout",
                "print the parsed form of the magic file, use in conjunction with -m to debug a new magic file before installing it")) {
        }
        if (opts.getBoolean("i|mime",
                "output MIME type strings (--mime-type and --mime-encoding)")) {
            flags |= MAGIC_MIME;
        }
        if (opts.getBoolean("k|keep-going", "don't stop at the first match")) {
            flags |= MAGIC_CONTINUE;
        }
        nobuffer = opts.getBoolean("n|no-buffer", "do not buffer output");
        nopad = opts.getBoolean("N|no-pad", "do not pad output");
        nulsep = opts.getBoolean("0|print0",
                "terminate filenames with ASCII NUL");
        if (opts.getBoolean("r|raw",
                "don't translate unprintable chars to \\ooo")) {
            flags |= MAGIC_RAW;
        }
        if (opts.getBoolean("s|special-files",
                "treat special (block/char devices) files as ordinary ones")) {
            flags |= MAGIC_DEVICES;
        }

        if (opts.getBoolean("C|compile", "compile file specified by -m")) {
        }
        if (opts.getBoolean("d|debug", "print debugging messages")) {
            flags |= MAGIC_DEBUG | MAGIC_CHECK;
        }

        separator = opts.getString("F|separator", ":", "STRING",
                "use string as separator instead of `:'");
        magicfile = opts.getString("m|magic-file", null, "LIST",
                "use LIST as a colon-separated list of magic number files");

        String str;
        while ((str = opts.getString("e|exclude", null, "TEST",
                "exclude TEST from the list of test to be performed for file." +
                        "Valid tests are: ascii, apptype, compress, elf, soft, tar, tokens, troff")) !=
                null) {

            for (i = 0; i < nv.length; i++) {
                if (nv[i].name.equals(str)) {
                    break;
                }
            }

            if (i == nv.length) {
            } else {
                flags |= nv[i].value;
            }
            break;
        }

        Reader sourceFiles = opts.getReader("f|files-from", (Reader) null,
                "FILE", "read the filenames to be examined from FILE");

        files = opts.getOperands("file ...");

        if (sourceFiles != null) {
            if (action != 0) {
                usage(opts);
            }
            if (magic == null) {
                if ((magic = load(magicfile, flags)) == null) {
                    System.exit(1);
                }
            }
            ++didsomefiles;
        }
        if (files.length == 0 || didsomefiles != 0) {
            usage(opts);
        }
    }

    private static synchronized MagicSet load(String magicfile, int flags) {
        String key = magicfile + "|" + flags;
        MagicSet magic = magicSets.get(key);
        if (magic != null)
            return magic;
        magic = open(flags);
        if (!magic.load(magicfile)) {
            stderr.printf("%s: %s\n", PROGNAME, magic.error());
            magic.close();
            return null;
        }
        magicSets.put(key, magic);
        return magic;
    }

    private static void usage(MainArgs opts) {
        opts.usage();
        System.exit(1);
    }

    /**
     * Run this class as a program.
     *
     * @param args The command line arguments.
     *
     * @throws Exception Exception we don't recover from.
     */
    public static void main(String[] args) throws Exception {
        new FileMagic(args).execute();
    }

    public void execute() {
        boolean c;

        switch (action) {
        case FILE_CHECK:
        case FILE_COMPILE:
            /*
             * Don't try to check/compile ~/.magic unless we explicitly
             * ask for it.
             */
            if (magicfile.equals(magicpath.toString())) {
                magicfile = null;
            }
            magic = open(flags | MAGIC_CHECK);
            c = action == FILE_CHECK ? magic.check(magicfile) : magic.compile(
                    magicfile);
            if (!c) {
                stderr.printf("%s: %s\n", PROGNAME, magic.error());
                throw new IllegalStateException(magic.error());
            }
            return;
        default:
            if (magic == null) {
                if ((magic = load(magicfile, flags)) == null) {
                    throw new IllegalStateException("Cannot load magic data");
                }
            }
            break;
        }

        int j, wid, nw;
        for (wid = 0, j = 0; j < files.length; j++) {
            nw = InternalUtils.file_mbswidth(files[j]);
            if (nw > wid) {
                wid = nw;
            }
        }

        /*
         * If bflag is only set twice, set it depending on
         * number of files [this is undocumented, and subject to change]
         */
        if (bflag == 2) {
            bflag = files.length > 1 ? 1 : 0;
        }
        int e = 0;
        for (int i = 0; i < files.length; i++) {
            e |= process(magic, files[i], wid);
        }

       //TODO: This is all wrong -- this is no way to behave in a function
        if (e != 0) {
            throw new IllegalStateException("Error in processing files");
        }
    }

    private int process(MagicSet ms, String inname, int wid) {
        String type;
        int std_in = inname.equals("-") ? 1 : 0;

        if (wid > 0 && bflag == 0) {
            out.printf("%s", (std_in) != 0 ? "/dev/stdin" : inname);
            if (nulsep) {
                out.putc('\0');
            } else {
                out.printf("%s", separator);
            }
            int spacing = nopad ? 0 : (wid - file_mbswidth(inname));
            if (spacing > 0) {
                out.printf("%" + spacing + "s", "");
            }
            out.puts(" ");
        }

        String inname1 = std_in != 0 ? null : inname;
        type = ms.file(inname1);
        if (type == null || type.length() == 0) {
            out.printf("ERROR: %s\n", ms.error());
            return 1;
        } else {
            out.printf("%s\n", type);
            return 0;
        }
    }

    int unwrap(MagicSet ms, Reader sourceFiles) throws IOException {
        int wid = 0, cwid;
        int e = 0;
        String buf;

        if (!sourceFiles.markSupported()) {
            wid = 1;
        } else {
            sourceFiles.mark(16 * 1024);
            BufferedReader in = new BufferedReader(sourceFiles);
            while ((buf = in.readLine()) != null) {
                cwid = InternalUtils.file_mbswidth(buf);
                if (cwid > wid) {
                    wid = cwid;
                }
            }
            sourceFiles.reset();
        }

        BufferedReader in = new BufferedReader(sourceFiles);
        while ((buf = in.readLine()) != null) {
            e |= process(ms, buf, wid);
            if (nobuffer) {
                out.flush();
            }
        }

        IOUtils.closeQuietly(sourceFiles);
        return e;
    }

    public void setOutput(Writer out) {
        this.out = new Stdio(new Formatter(out));
    }
}