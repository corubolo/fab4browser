package com.im.file;

import static com.im.file.MagicSet.*;
import com.im.util.CLib;
import org.simplx.c.Pointer;
import org.simplx.c.Stdio;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

class InternalUtils {
    static final byte[] ebcdic_to_ascii = {
            (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 156, (byte) 9,
            (byte) 134, (byte) 127, (byte) 151, (byte) 141, (byte) 142,
            (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16,
            (byte) 17, (byte) 18, (byte) 19, (byte) 157, (byte) 133, (byte) 8,
            (byte) 135, (byte) 24, (byte) 25, (byte) 146, (byte) 143, (byte) 28,
            (byte) 29, (byte) 30, (byte) 31, (byte) 128, (byte) 129, (byte) 130,
            (byte) 131, (byte) 132, (byte) 10, (byte) 23, (byte) 27, (byte) 136,
            (byte) 137, (byte) 138, (byte) 139, (byte) 140, (byte) 5, (byte) 6,
            (byte) 7, (byte) 144, (byte) 145, (byte) 22, (byte) 147, (byte) 148,
            (byte) 149, (byte) 150, (byte) 4, (byte) 152, (byte) 153,
            (byte) 154, (byte) 155, (byte) 20, (byte) 21, (byte) 158, (byte) 26,
            (byte) ' ', (byte) ' ', (byte) '¡', (byte) '¢', (byte) '£',
            (byte) '?', (byte) '¥', (byte) '?', (byte) '§', (byte) '¨',
            (byte) 'Õ', (byte) '.', (byte) '<', (byte) '(', (byte) '+',
            (byte) '|', (byte) '&', (byte) '©', (byte) 'ª', (byte) '«',
            (byte) '¬', (byte) '?', (byte) '®', (byte) '¯', (byte) '°',
            (byte) '±', (byte) '!', (byte) '$', (byte) '*', (byte) ')',
            (byte) ';', (byte) '~', (byte) '-', (byte) '/', (byte) '?',
            (byte) '?', (byte) '´', (byte) 'µ', (byte) '¶', (byte) '·',
            (byte) '¸', (byte) '?', (byte) 'Ë', (byte) ',', (byte) '%',
            (byte) '_', (byte) '>', (byte) '?', (byte) 'º', (byte) '»',
            (byte) '?', (byte) '?', (byte) '?', (byte) '¿', (byte) 'À',
            (byte) 'Á', (byte) 'Â', (byte) '`', (byte) ':', (byte) '#',
            (byte) '@', (byte) '\'', (byte) '=', (byte) '"', (byte) 'Ã',
            (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e',
            (byte) 'f', (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'Ä',
            (byte) 'Å', (byte) 'Æ', (byte) 'Ç', (byte) 'È', (byte) 'É',
            (byte) 'Ê', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm',
            (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r',
            (byte) '^', (byte) 'Ì', (byte) 'Í', (byte) 'Î', (byte) 'Ï',
            (byte) '?', (byte) 'Ñ', (byte) 'å', (byte) 's', (byte) 't',
            (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y',
            (byte) 'z', (byte) 'Ò', (byte) 'Ó', (byte) 'Ô', (byte) '[',
            (byte) 'Ö', (byte) '?', (byte) 'Ø', (byte) 'Ù', (byte) 'Ú',
            (byte) 'Û', (byte) 'Ü', (byte) '?', (byte) '?', (byte) 'ß',
            (byte) 'à', (byte) 'á', (byte) 'â', (byte) 'ã', (byte) 'ä',
            (byte) ']', (byte) 'æ', (byte) 'ç', (byte) '{', (byte) 'A',
            (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F',
            (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'è', (byte) 'é',
            (byte) 'ê', (byte) 'ë', (byte) 'ì', (byte) 'í', (byte) '}',
            (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
            (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'î',
            (byte) 'ï', (byte) '?', (byte) 'ñ', (byte) 'ò', (byte) 'ó',
            (byte) '\\', (byte) 159, (byte) 'S', (byte) 'T', (byte) 'U',
            (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z',
            (byte) 'ô', (byte) 'õ', (byte) 'ö', (byte) '÷', (byte) 'ø',
            (byte) 'ù', (byte) '0', (byte) '1', (byte) '2', (byte) '3',
            (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8',
            (byte) '9', (byte) 'ú', (byte) 'û', (byte) 'ü', (byte) '?',
            (byte) '?', (byte) 'ÿ',
    };

    //    static {
//        System.out.print("{");
//        for (int i = 0; i < ebcdic_to_ascii.length; i++) {
//            if (i % 16 == 0)
//            System.out.println();
//            int v = ebcdic_to_ascii[i];
//            char cv = (char) v;
//            System.out.print("(byte) ");
//            if (!isprint(cv)) {
//                System.out.print(v);
//            } else if (cv == '\'' || cv == '\\') {
//                System.out.printf("'\\%c'", cv);
//            } else {
//                System.out.print("'" + cv + "'");
//            }
//            System.out.print(", ");
//        }
    //        System.out.println("\n};");
    //    }

    /** character never appears in text. */
    static final byte F = 0;
    /** character appears in plain ASCII text. */
    static final byte T = 1;
    /** character appears in ISO-8859 text. */
    static final byte I = 2;
    /** character appears in non-ISO extended ASCII (Mac, IBM PC). */
    static final byte X = 3;
    static final byte[] text_chars = {
            /*                  BEL BS HT LF    FF CR    */
            F, F, F, F, F, F, F, T, T, T, T, F, T, T, F, F,  /* 0x0X */
            /*                              ESC          */
            F, F, F, F, F, F, F, F, F, F, F, T, F, F, F, F,  /* 0x1X */
            T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x2X */
            T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x3X */
            T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x4X */
            T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x5X */
            T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T,  /* 0x6X */
            T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, F,  /* 0x7X */
            /*            NEL                            */
            X, X, X, X, X, T, X, X, X, X, X, X, X, X, X, X,  /* 0x8X */
            X, X, X, X, X, X, X, X, X, X, X, X, X, X, X, X,  /* 0x9X */
            I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xaX */
            I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xbX */
            I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xcX */
            I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xdX */
            I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I,  /* 0xeX */
            I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I   /* 0xfX */
    };

    private static final Pattern FMT_PATTERN = Pattern.compile("%[-0-9\\.]*s");

    static int file_mbswidth(String file) {
        // In the original, this tries to take into account that some characters
        // take more than one character width on the screen.  Java has no easy
        // way to do this.
        return file.length();
    }

    static int file_strncmp(String s1, String s2, int len, int flags) {
        int v;

        /*
          * What we want here is v = strncmp(s1, s2, len),
          * but ignoring any nulls.
          */
        v = 0;
        if (0L == flags) { /* normal string: do it fast */
            for (int i = 0; i < len; i++) {
                if ((v = s1.charAt(i) - s2.charAt(i)) != 0) {
                    break;
                }
            }
        } else { /* combine the others */
            int i1 = 0;
            int i2 = 0;
            while (len-- > 0) {
                if ((flags & STRING_IGNORE_LOWERCASE) != 0 && CLib.islower(
                        s1.charAt(i1))) {
                    if ((v = CLib.tolower(s2.charAt(i2++)) - s1.charAt(i1++)) !=
                            '\0') {
                        break;
                    }
                } else if ((flags & STRING_IGNORE_UPPERCASE) != 0 &&
                        CLib.isupper(s1.charAt(i1))) {
                    if ((v = CLib.toupper(s2.charAt(i2++)) - s1.charAt(i1++)) !=
                            '\0') {
                        break;
                    }
                } else if ((flags & STRING_COMPACT_BLANK) != 0 && CLib.isspace(
                        s1.charAt(i1))) {
                    i1++;
                    if (CLib.isspace(s2.charAt(i2++))) {
                        while (CLib.isspace(s2.charAt(i2))) {
                            i2++;
                        }
                    } else {
                        v = 1;
                        break;
                    }
                } else if ((flags & STRING_COMPACT_OPTIONAL_BLANK) != 0 &&
                        CLib.isspace(s1.charAt(i1))) {
                    i1++;
                    while (CLib.isspace(s2.charAt(i2))) {
                        i2++;
                    }
                } else {
                    if ((v = s2.charAt(i2++) - s1.charAt(i1++)) != 0) {
                        break;
                    }
                }
            }
        }
        return v;
    }

    static int file_strncmp(Pointer s1, Pointer s2, int len, int flags) {
        /*
          * Convert the source args to unsigned here so that (1) the
          * compare will be unsigned as it is in strncmp() and (2) so
          * the ctype functions will work correctly without extra
          * casting.
          */
        Pointer a = s1.copy();
        Pointer b = s2.copy();
        int v;

        /*
          * What we want here is v = strncmp(s1, s2, len),
          * but ignoring any nulls.
          */
        v = 0;
        if (0L == flags) { /* normal string: do it fast */
            while (len-- > 0) {
                if ((v = b.getIncr() - a.getIncr()) != '\0') {
                    break;
                }
            }
        } else { /* combine the others */
            while (len-- > 0) {
                if ((flags & STRING_IGNORE_LOWERCASE) != 0 && CLib.islower(
                        a.get())) {
                    if ((v = CLib.tolower(b.getIncr()) - a.getIncr()) != '\0') {
                        break;
                    }
                } else if (((flags & STRING_IGNORE_UPPERCASE)) != 0 &&
                        CLib.isupper(a.get())) {
                    if ((v = CLib.toupper(b.getIncr()) - a.getIncr()) != '\0') {
                        break;
                    }
                } else if ((flags & STRING_COMPACT_BLANK) != 0 && CLib.isspace(
                        a.get())) {
                    a.incr();
                    if (CLib.isspace(b.getIncr())) {
                        while (CLib.isspace(b.get())) {
                            b.incr();
                        }
                    } else {
                        v = 1;
                        break;
                    }
                } else if ((flags & STRING_COMPACT_OPTIONAL_BLANK) != 0 &&
                        CLib.isspace(a.get())) {
                    a.incr();
                    while (CLib.isspace(b.get())) {
                        b.incr();
                    }
                } else {
                    if ((v = b.getIncr() - a.getIncr()) != '\0') {
                        break;
                    }
                }
            }
        }
        return v;
    }

    static String file_fmttime(int v, int local) {
        Calendar cal;
        if (local != 0) {
            cal = Calendar.getInstance();
        } else {
            cal = Calendar.getInstance(CLib.GMT);
        }
        cal.setTimeInMillis(v * 1000);
        Date d = cal.getTime();
        return d.toString();
    }

    static void file_showstr(Stdio fp, Pointer sOrig, int len) {
        byte c;

        Pointer s = sOrig.copy();
        for (; ;) {
            if (len == ~0) {
                c = s.getIncr();
                if (c == '\0') {
                    break;
                }
            } else {
                if (len-- == 0) {
                    break;
                }
                c = s.getIncr();
            }
            if (c >= 040 && c <= 0176)      /* TODO isprint && !iscntrl */ {
                fp.putc((char) c);
            } else {
                fp.putc('\\');
                switch (c) {
                case '\u0007':
                    // alert (bell)
                    fp.putc('a');
                    break;

                case '\b':
                    fp.putc('b');
                    break;

                case '\f':
                    fp.putc('f');
                    break;

                case '\n':
                    fp.putc('n');
                    break;

                case '\r':
                    fp.putc('r');
                    break;

                case '\t':
                    fp.putc('t');
                    break;

                case '\u000b':
                    // vertical tab
                    fp.putc('v');
                    break;

                default:
                    fp.printf("%03o", c & 0xff);
                    break;
                }
            }
        }
    }

    static void from_ebcdic(byte[] buf, int nbytes, byte[] out) {
        int i;

        for (i = 0; i < nbytes; i++) {
            out[i] = ebcdic_to_ascii[buf[i] & 0xff];
        }
    }

    static int looks_extended(byte[] buf, int nbytes, char[] ubuf, int[] ulen) {
        int i;

        ulen[0] = 0;

        for (i = 0; i < nbytes; i++) {
            int t = text_chars[buf[i] & 0xff];

            if (t != T && t != I && t != X) {
                return 0;
            }

            ubuf[ulen[0]++] = CLib.toChar(buf[i]);
        }

        return 1;
    }

    static int looks_ucs16(byte[] buf, int nbytes, char[] ubuf, int[] ulen) {
        int bigend;
        int i;

        if (nbytes < 2) {
            return 0;
        }

        if (buf[0] == 0xff && buf[1] == 0xfe) {
            bigend = 0;
        } else if (buf[0] == 0xfe && buf[1] == 0xff) {
            bigend = 1;
        } else {
            return 0;
        }

        ulen[0] = 0;

        for (i = 2; i + 1 < nbytes; i += 2) {
            /* XXX fix to properly handle chars > 65536 */
            if ((bigend) != 0) {
                ubuf[ulen[0]++] = (char) (buf[i + 1] + 256 * buf[i]);
            } else {
                ubuf[ulen[0]++] = (char) (buf[i] + 256 * buf[i + 1]);
            }

            if (ubuf[ulen[0] - 1] == 0xfffe) {
                return 0;
            }
            if (ubuf[ulen[0] - 1] < 128 && text_chars[ubuf[ulen[0] - 1]] != T) {
                return 0;
            }
        }

        return 1 + bigend;
    }

    static int looks_ascii(byte[] buf, int nbytes, char[] ubuf, int[] ulen) {
        int i;

        ulen[0] = 0;

        for (i = 0; i < nbytes; i++) {
            int t = text_chars[buf[i] & 0xff];

            if (t != T) {
                return 0;
            }

            ubuf[ulen[0]++] = CLib.toChar(buf[i]);
        }

        return 1;
    }

    static int looks_latin1(byte[] buf, int nbytes, char[] ubuf, int[] ulen) {
        int i;

        ulen[0] = 0;

        for (i = 0; i < nbytes; i++) {
            int t = text_chars[buf[i] & 0xff];

            if (t != T && t != I) {
                return 0;
            }

            ubuf[ulen[0]++] = CLib.toChar(buf[i]);
        }

        return 1;
    }

    static int file_strncmp16(String a, String b, int len, int flags) {
        /*
          * TODO - The 16-bit string compare probably needs to be done
          * differently, especially if the flags are to be supported.
          * At the moment, I am unsure.
          */
        flags = 0;
        return file_strncmp(a, b, len, flags);
    }

    static int file_strncmp16(Pointer a, Pointer b, int len, int flags) {
        /*
          * TODO - The 16-bit string compare probably needs to be done
          * differently, especially if the flags are to be supported.
          * At the moment, I am unsure.
          */
        flags = 0;
        return file_strncmp(a, b, len, flags);
    }

    static int BIT(int i) {
        return 1 << i;
    }

    static void OCTALIFY(char ch, StringBuilder pbuf) {
        pbuf.append('\\');
        if (ch <= 07) {
            pbuf.append("00");
        } else if (ch <= 077) {
            pbuf.append("0");
        }
        pbuf.append(Integer.toOctalString(ch));
    }

    static void setText(StringBuilder sb, String text) {
        sb.setLength(0);
        sb.append(text);
    }

    static int check_fmt(Magic m) {
        if (CLib.strchr(m.desc, '%') == null) {
            return 0;
        }
        return FMT_PATTERN.matcher(m.desc.toString()).matches() ? 1 : 0;
    }

    static int from_oct(int digs, Pointer whereOrig) {
        Pointer where = whereOrig.copy();
        int value;

        while (CLib.isspace(where.get())) {        /* Skip spaces */
            where.incr();
            if (--digs <= 0) {
                return -1;                /* All blank field */
            }
        }
        value = 0;
        while (digs > 0 && CLib.isodigit(
                where.get())) {      /* Scan til nonoctal */
            value = (value << 3) | (where.getIncr() - '0');
            --digs;
        }

        if (digs > 0 && where.get() != '\0' && !CLib.isspace(where.get())) {
            return -1;                        /* Ended on non-space/nul */
        }

        return value;
    }

    static int trim_nuls(byte[] buf, int nbytes) {
        while (nbytes > 1 && buf[nbytes - 1] == '\0') {
            nbytes--;
        }

        return nbytes;
    }

    static boolean ISSPC(char x) {
        return ((x) == ' ' || (x) == '\t' || (x) == '\r' || (x) == '\n' ||
                (x) == 0x85 || (x) == '\f');
    }

    static int ascmatch(Pointer s, char[] us, int offset, int ulen) {
        int i;

        for (i = 0; i < ulen; i++) {
            if (s.get(i) != us[i + offset]) {
                return 0;
            }
        }

        if (s.get(i) != 0) {
            return 0;
        } else {
            return 1;
        }
    }

    static int file_looks_utf8(Pointer buf, int nbytes, char[] ubuf,
            int[] ulen) {

        int i;
        int n;
        char c;
        int gotone = 0, ctrl = 0;

        if (ubuf != null) {
            ulen[0] = 0;
        }

outer:
        for (i = 0; i < nbytes; i++) {
            if (CLib.isascii(buf.get(i))) {
                /* 0xxxxxxx is plain ASCII */
                /*
                 * Even if the whole file is valid UTF-8 sequences,
                 * still reject it if it uses weird control characters.
                 */

                if (text_chars[buf.get(i) & 0xff] != T) {
                    ctrl = 1;
                }

                if (ubuf != null) {
                    ubuf[ulen[0]++] = (char) buf.get(i);
                }
            } else if ((buf.get(i) & 0x40) == 0) { /* 10xxxxxx never 1st byte */
                return -1;
            } else {                           /* 11xxxxxx begins UTF-8 */
                int following;

                if ((buf.get(i) & 0x20) == 0) {                /* 110xxxxx */
                    c = (char) (buf.get(i) & 0x1f);
                    following = 1;
                } else if ((buf.get(i) & 0x10) == 0) {        /* 1110xxxx */
                    c = (char) (0x0f & buf.get(i));
                    following = 2;
                } else if ((buf.get(i) & 0x08) == 0) {        /* 11110xxx */
                    c = (char) (buf.get(i) & 0x07);
                    following = 3;
                } else if ((buf.get(i) & 0x04) == 0) {        /* 111110xx */
                    c = (char) (buf.get(i) & 0x03);
                    following = 4;
                } else if ((buf.get(i) & 0x02) == 0) {        /* 1111110x */
                    c = (char) (buf.get(i) & 0x01);
                    following = 5;
                } else {
                    return -1;
                }

                for (n = 0; n < following; n++) {
                    i++;
                    if (i >= nbytes) {
                        break outer;
                    }

                    if ((buf.get(i) & 0x80) == 0 || ((buf.get(i) & 0x40)) !=
                            0) {
                        return -1;
                    }

                    c = (char) ((c << 6) + (buf.get(i) & 0x3f));
                }

                if (ubuf != null) {
                    ubuf[ulen[0]++] = c;
                }
                gotone = 1;
            }
        }
        return ctrl != 0 ? 0 : gotone != 0 ? 2 : 1;
    }

    static int looks_utf8_with_BOM(byte[] buf, int nbytes, char[][] ubuf,
            int[] ulen) {

        if (nbytes > 3 && buf[0] == 0xef && buf[1] == 0xbb && buf[2] == 0xbf) {
            return file_looks_utf8(new Pointer(buf, 3), nbytes - 3, ubuf[0],
                    ulen);
        } else {
            return -1;
        }
    }

    static String ifNull(String str, String strIfNull) {
        return (str != null ? str : strIfNull);
    }

    static Pointer eatsize(Pointer l) {
        if (CLib.tolower(l.get()) == 'u') {
            l.incr();
        }

        switch (CLib.tolower(l.get())) {
        case 'l':    /* long */
        case 's':    /* short */
        case 'h':    /* short */
        case 'b':    /* char/byte */
        case 'c':    /* char/byte */
            l.incr();
            //noinspection fallthrough
        default:
            break;
        }

        return l;
    }

    static Pointer eatab(Pointer l) {
        while (l.get() != '\0' && CLib.isspace(l.get())) {
            l.incr();
        }
        return l;
    }
}
