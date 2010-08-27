package com.im.file;

import org.simplx.c.Pointer;

import java.io.Serializable;

class Magic implements Serializable {
    // Word 1
    int cont_level;        /* level of ">" */
    byte flag;

    byte factor;

    // Word 2
    byte reln;                /* relation (0=eq, '>'=gt, etc) */
    int vallen;                /* length of string value, if any */
    byte type;                /* comparison type (FILE_*) */
    byte in_type;        /* type of indirection */

    // Word 3
    byte in_op;                /* operator for indirection */
    byte mask_op;        /* operator for mask */
    byte cond;                /* conditional type */
    byte factor_op;

    // Word 4
    int offset;        /* offset to magic number */
    // Word 5
    int in_offset;        /* offset from indirection */
    // Word 6
    int lineno;        /* line number in magic file */
    String file;

    // Word 7,8
    long num_mask;
    int str_range;
    int str_flags;

    // Words 9-16
    final ValueType value;        /* either number or string */

    // Words 17-24
    Pointer desc;        /* description */

    // Words 25-32
    Pointer mimetype; /* MIME type */

    // Words 33-34
    Pointer apple;

    /** if '(...)' appears. */
    static final int INDIR = 0x01;
    /** if '>&' or '>...(&' appears. */
    static final int OFFADD = 0x02;
    /** if '>&(' appears. */
    static final int INDIROFFADD = 0x04;
    /** comparison is unsigned. */
    static final int UNSIGNED = 0x08;
    /** suppress space character before output. */
    static final int NOSPACE = 0x10;
    /** test is for a binary type (set only; for top-level tests) */
    static final int BINTEST = 0x20;
    /** for passing to file_softmagic. */
    static final int TEXTTEST = 0;

    static final byte FILE_INVALID = 0;
    static final byte FILE_BYTE = 1;
    static final byte FILE_SHORT = 2;
    static final byte FILE_DEFAULT = 3;
    static final byte FILE_LONG = 4;
    static final byte FILE_STRING = 5;
    static final byte FILE_DATE = 6;
    static final byte FILE_BESHORT = 7;
    static final byte FILE_BELONG = 8;
    static final byte FILE_BEDATE = 9;
    static final byte FILE_LESHORT = 10;
    static final byte FILE_LELONG = 11;
    static final byte FILE_LEDATE = 12;
    static final byte FILE_PSTRING = 13;
    static final byte FILE_LDATE = 14;
    static final byte FILE_BELDATE = 15;
    static final byte FILE_LELDATE = 16;
    static final byte FILE_REGEX = 17;
    static final byte FILE_BESTRING16 = 18;
    static final byte FILE_LESTRING16 = 19;
    static final byte FILE_SEARCH = 20;
    static final byte FILE_MEDATE = 21;
    static final byte FILE_MELDATE = 22;
    static final byte FILE_MELONG = 23;
    static final byte FILE_QUAD = 24;
    static final byte FILE_LEQUAD = 25;
    static final byte FILE_BEQUAD = 26;
    static final byte FILE_QDATE = 27;
    static final byte FILE_LEQDATE = 28;
    static final byte FILE_BEQDATE = 29;
    static final byte FILE_QLDATE = 30;
    static final byte FILE_LEQLDATE = 31;
    static final byte FILE_BEQLDATE = 32;
    static final byte FILE_FLOAT = 33;
    static final byte FILE_BEFLOAT = 34;
    static final byte FILE_LEFLOAT = 35;
    static final byte FILE_DOUBLE = 36;
    static final byte FILE_BEDOUBLE = 37;
    static final byte FILE_LEDOUBLE = 38;
    static final byte FILE_BEID3 = 39;
    static final byte FILE_LEID3 = 40;
    static final byte FILE_INDIRECT = 41;
    /** size of array to contain all names. */
    static final int FILE_NAMES_SIZE = 42;

    /** max leng of text description/MIME type. */
    static final int MAXDESC = 64;

    /** max leng of text apple type. */
    static final int MAXAPPLE = 8;

    static final int FILE_FMT_NONE = 0;
    /** "cduxXi". */
    static final int FILE_FMT_NUM = 1;
    /** "s". */
    static final int FILE_FMT_STR = 2;
    /** "ll". */
    static final int FILE_FMT_QUAD = 3;
    /** "eEfFgG". */
    static final int FILE_FMT_FLOAT = 4;
    /** "eEfFgG". */
    static final int FILE_FMT_DOUBLE = 5;

    static final char FILE_FACTOR_OP_PLUS = '+';
    static final char FILE_FACTOR_OP_MINUS = '-';
    static final char FILE_FACTOR_OP_TIMES = '*';
    static final char FILE_FACTOR_OP_DIV = '/';
    static final char FILE_FACTOR_OP_NONE = '\0';

    static final String FILE_OPS = "&|^+-*/%";
    static final int FILE_OPAND = 0;
    static final int FILE_OPOR = 1;
    static final int FILE_OPXOR = 2;
    static final int FILE_OPADD = 3;
    static final int FILE_OPMINUS = 4;
    static final int FILE_OPMULTIPLY = 5;
    static final int FILE_OPDIVIDE = 6;
    static final int FILE_OPMODULO = 7;
    /** mask for above ops. */
    static final int FILE_OPS_MASK = 0x07;
    static final int FILE_UNUSED_1 = 0x08;
    static final int FILE_UNUSED_2 = 0x10;
    static final int FILE_UNUSED_3 = 0x20;
    static final int FILE_OPINVERSE = 0x40;
    static final int FILE_OPINDIRECT = 0x80;

    static final byte COND_NONE = 0;
    static final byte COND_IF = 1;
    static final byte COND_ELIF = 2;
    static final byte COND_ELSE = 3;

    Magic(String file, int lineno) {
        this.file = file;
        this.lineno = lineno;

        value = new ValueType();
        desc = new Pointer(new byte[MAXDESC]);
        mimetype = new Pointer(new byte[MAXDESC]);
        apple = new Pointer(new byte[MAXAPPLE]);
    }

    @Override
    public String toString() {
        return file + ":" + lineno;
    }

    static boolean IS_STRING(int t) {
        return ((t) == FILE_STRING || (t) == FILE_PSTRING ||
                (t) == FILE_BESTRING16 || (t) == FILE_LESTRING16 ||
                (t) == FILE_REGEX || (t) == FILE_SEARCH || (t) == FILE_DEFAULT);
    }
}