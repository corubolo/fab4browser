package com.im.file;

import org.simplx.c.Pointer;

/*
 * Names.h - names and types used by ascmagic in file(1).
 * These tokens are here because they can appear anywhere in
 * the first HOWMANY bytes, while tokens in MAGIC must
 * appear at fixed offsets into the file. Don't make HOWMANY
 * too high unless you have a very fast CPU.
 *
 * $File: names.h,v 1.32 2008/02/11 00:19:29 rrt Exp $
 */

/*
 * XXX - how should we distinguish Java from C++?
 * The trick used in a Debian snapshot, of having "extends" or "implements"
 * as tags for Java, doesn't work very well, given that those keywords
 * are often preceded by "class", which flags it as C++.
 *
 * Perhaps we need to be able to say
 *
 *	If "class" then
 *
 *		if "extends" or "implements" then
 *			Java
 *		else
 *			C++
 *	endif
 *
 * Or should we use other keywords, such as "package" or "import"?
 * Unfortunately, Ada95 uses "package", and Modula-3 uses "import",
 * although I infer from the language spec at
 *
 *	http://www.research.digital.com/SRC/m3defn/html/m3.html
 *
 * that Modula-3 uses "IMPORT" rather than "import", i.e. it must be
 * in all caps.
 *
 * So, for now, we go with "import".  We must put it before the C++
 * stuff, so that we don't misidentify Java as C++.  Not using "package"
 * means we won't identify stuff that defines a package but imports
 * nothing; hopefully, very little Java code imports nothing (one of the
 * reasons for doing OO programming is to import as much as possible
 * and write only what you need to, right?).
 *
 * Unfortunately, "import" may cause us to misidentify English text
 * as Java, as it comes after "the" and "The".  Perhaps we need a fancier
 * heuristic to identify Java?
 */

class Names {
    /* these types are used to index the table 'types': keep em in sync! */
    /** first and foremost on UNIX. */
    public static final int L_C = 0;
    /** Bjarne's postincrement. */
    public static final int L_CC = 1;
    /** Makefiles. */
    public static final int L_MAKE = 2;
    /** PL/1. */
    public static final int L_PLI = 3;
    /** some kinda assembler. */
    public static final int L_MACH = 4;
    /** English. */
    public static final int L_ENG = 5;
    /** Pascal. */
    public static final int L_PAS = 6;
    /** Electronic mail. */
    public static final int L_MAIL = 7;
    /** Usenet Netnews. */
    public static final int L_NEWS = 8;
    /** Java code. */
    public static final int L_JAVA = 9;
    /** HTML. */
    public static final int L_HTML = 10;
    /** BCPL. */
    public static final int L_BCPL = 11;
    /** M4. */
    public static final int L_M4 = 12;
    /** PO. */
    public static final int L_PO = 13;

    public static final Names[] names = {
            /* These must be sorted by eye for optimal hit rate */
            /* Add to this list only after substantial meditation */
            new Names("msgid", L_PO), new Names("dnl", L_M4), new Names(
                    "import", L_JAVA), new Names("\"libhdr\"", L_BCPL),
            new Names("\"LIBHDR\"", L_BCPL), new Names("//", L_CC), new Names(
                    "template", L_CC), new Names("virtual", L_CC), new Names(
                    "class", L_CC), new Names("public:", L_CC), new Names(
                    "private:", L_CC), new Names("/*", L_C),
            /* must precede "The", "the", etc. */
            new Names("#include", L_C), new Names("char", L_C), new Names("The",
                    L_ENG), new Names("the", L_ENG), new Names("double", L_C),
            new Names("extern", L_C), new Names("float", L_C), new Names(
                    "struct", L_C), new Names("union", L_C), new Names("CFLAGS",
                    L_MAKE), new Names("LDFLAGS", L_MAKE), new Names("all:",
                    L_MAKE), new Names(".PRECIOUS", L_MAKE), new Names(".ascii",
                    L_MACH), new Names(".asciiz", L_MACH), new Names(".byte",
                    L_MACH), new Names(".even", L_MACH), new Names(".globl",
                    L_MACH), new Names(".text", L_MACH), new Names("clr",
                    L_MACH), new Names("(input,", L_PAS), new Names("program",
                    L_PAS), new Names("record", L_PAS), new Names("dcl", L_PLI),
            new Names("Received:", L_MAIL), new Names(">From", L_MAIL),
            new Names("Return-Path:", L_MAIL), new Names("Cc:", L_MAIL),
            new Names("Newsgroups:", L_NEWS), new Names("Path:", L_NEWS),
            new Names("Organization:", L_NEWS), new Names("href=", L_HTML),
            new Names("HREF=", L_HTML), new Names("<body", L_HTML), new Names(
                    "<BODY", L_HTML), new Names("<html", L_HTML), new Names(
                    "<HTML", L_HTML), new Names("<!--", L_HTML),
    };

    final Pointer name;
    final short type;

    public Names(String name, int type) {
        this.name = new Pointer(name);
        this.type = (short) type;
    }
}
