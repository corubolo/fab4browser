package com.im.file;

import static com.im.file.Magic.*;
import com.im.util.CLib;
import org.simplx.c.Pointer;
import static org.simplx.c.Stdio.stderr;

class TypeTblS {
    String name;
    byte type;
    int format;

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

    static final TypeTblS[] type_tbl = {
            new TypeTblS("byte", FILE_BYTE, FILE_FMT_NUM), new TypeTblS("short",
                    FILE_SHORT, FILE_FMT_NUM), new TypeTblS("default",
                    FILE_DEFAULT, FILE_FMT_STR), new TypeTblS("long", FILE_LONG,
                    FILE_FMT_NUM), new TypeTblS("string", FILE_STRING,
                    FILE_FMT_STR), new TypeTblS("date", FILE_DATE,
                    FILE_FMT_STR), new TypeTblS("beshort", FILE_BESHORT,
                    FILE_FMT_NUM), new TypeTblS("belong", FILE_BELONG,
                    FILE_FMT_NUM), new TypeTblS("bedate", FILE_BEDATE,
                    FILE_FMT_STR), new TypeTblS("leshort", FILE_LESHORT,
                    FILE_FMT_NUM), new TypeTblS("lelong", FILE_LELONG,
                    FILE_FMT_NUM), new TypeTblS("ledate", FILE_LEDATE,
                    FILE_FMT_STR), new TypeTblS("pstring", FILE_PSTRING,
                    FILE_FMT_STR), new TypeTblS("ldate", FILE_LDATE,
                    FILE_FMT_STR), new TypeTblS("beldate", FILE_BELDATE,
                    FILE_FMT_STR), new TypeTblS("leldate", FILE_LELDATE,
                    FILE_FMT_STR), new TypeTblS("regex", FILE_REGEX,
                    FILE_FMT_STR), new TypeTblS("bestring16", FILE_BESTRING16,
                    FILE_FMT_STR), new TypeTblS("lestring16", FILE_LESTRING16,
                    FILE_FMT_STR), new TypeTblS("search", FILE_SEARCH,
                    FILE_FMT_STR), new TypeTblS("medate", FILE_MEDATE,
                    FILE_FMT_STR), new TypeTblS("meldate", FILE_MELDATE,
                    FILE_FMT_STR), new TypeTblS("melong", FILE_MELONG,
                    FILE_FMT_NUM), new TypeTblS("quad", FILE_QUAD,
                    FILE_FMT_QUAD), new TypeTblS("lequad", FILE_LEQUAD,
                    FILE_FMT_QUAD), new TypeTblS("bequad", FILE_BEQUAD,
                    FILE_FMT_QUAD), new TypeTblS("qdate", FILE_QDATE,
                    FILE_FMT_STR), new TypeTblS("leqdate", FILE_LEQDATE,
                    FILE_FMT_STR), new TypeTblS("beqdate", FILE_BEQDATE,
                    FILE_FMT_STR), new TypeTblS("qldate", FILE_QLDATE,
                    FILE_FMT_STR), new TypeTblS("leqldate", FILE_LEQLDATE,
                    FILE_FMT_STR), new TypeTblS("beqldate", FILE_BEQLDATE,
                    FILE_FMT_STR), new TypeTblS("float", FILE_FLOAT,
                    FILE_FMT_FLOAT), new TypeTblS("befloat", FILE_BEFLOAT,
                    FILE_FMT_FLOAT), new TypeTblS("lefloat", FILE_LEFLOAT,
                    FILE_FMT_FLOAT), new TypeTblS("double", FILE_DOUBLE,
                    FILE_FMT_DOUBLE), new TypeTblS("bedouble", FILE_BEDOUBLE,
                    FILE_FMT_DOUBLE), new TypeTblS("ledouble", FILE_LEDOUBLE,
                    FILE_FMT_DOUBLE), new TypeTblS("leid3", FILE_LEID3,
                    FILE_FMT_NUM), new TypeTblS("beid3", FILE_BEID3,
                    FILE_FMT_NUM), new TypeTblS("indirect", FILE_INDIRECT,
                    FILE_FMT_NONE), new TypeTblS(null, FILE_INVALID,
                    FILE_FMT_NONE),
    };

    TypeTblS(String name, byte type, int format) {
        this.name = name;
        this.type = type;
        this.format = format;
    }

    static byte get_type(Pointer l, Pointer[] t) {
        t[0] = l;
        TypeTblS p = null;
        String lStr = l.toString();
        for (int i = 0; i < type_tbl.length; i++) {
            p = type_tbl[i];
            if (p.name == null) {
                break;
            }
            if (CLib.strncmp(lStr, p.name, p.name.length()) == 0) {
                t[0] = l.plus(p.name.length());
                break;
            }
        }

        return p.type;
    }

    static void mdebug(int offset, Pointer str, int len) {
        stderr.printf("mget @%d: ", offset);
        InternalUtils.file_showstr(stderr, str, len);
        stderr.printf("\n");
        stderr.printf("\n");
    }
}