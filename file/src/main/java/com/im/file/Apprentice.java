package com.im.file;

import static com.im.file.MagicSet.MAGIC_CHECK;
import org.simplx.args.MainArgs;
import org.simplx.args.MissingDirAction;
import static org.simplx.c.Stdio.stderr;

import java.util.Arrays;

public class Apprentice {
    /**
     * Run this class as a program.
     *
     * @param args The command line arguments.
     *
     * @throws Exception Exception we don't recover from.
     */
    public static void main(String[] args) throws Exception {
        int ret;
        MagicSet ms;
        String progname = "apprentice";

        MainArgs line = new MainArgs(progname, args);
        String outdir = line.getDirectory("dir", null, MissingDirAction.NONE,
                "Output directory");
        String[] files = line.getOperands("file ...");

        ms = MagicSet.open(MAGIC_CHECK);
        String firstFile = files[0];
        String[] otherFiles = Arrays.copyOfRange(files, 1, files.length);

        if (files.length == 0) {
            line.usage();
            System.exit(1);
        }

        if (outdir == null) {
            ret = ms.compile(firstFile, otherFiles) ? 0 : 1;
        } else {
            ret = ms.compileTo(outdir, firstFile, otherFiles) ? 0 : 1;
        }
        if (ret == 1) {
            stderr.printf("%s: %s\n", progname, ms.error());
        }
        ms.close();
        System.exit(ret);
    }
}
