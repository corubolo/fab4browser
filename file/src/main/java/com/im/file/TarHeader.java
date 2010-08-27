package com.im.file;

import org.simplx.c.Pointer;

class TarHeader {
    private final byte[] buf;

    static String[] tartype = {
            "tar archive", "POSIX tar archive", "POSIX tar archive (GNU)",
    };

    TarHeader(byte[] buf) {
        this.buf = buf;
    }

    /*
    * Header block on tape.
    *
    * I'm going to use traditional DP naming conventions here.
    * A "block" is a big chunk of stuff that we do I/O on.
    * A "record" is a piece of info that we care about.
    * Typically many "record"s fit into a "block".
    */
    static final int RECORDSIZE = 512;
    static final int NAMSIZ = 100;
    static final int TUNMLEN = 32;
    static final int TGNMLEN = 32;

    /* The magic field is filled with this if uname and gname are valid. */
    /** 5 chars and a null. */
    static final Pointer TMAGIC = new Pointer("ustar");
    /** 7 chars and a null. */
    static final Pointer GNUTMAGIC = new Pointer("ustar  ");

    //char name[NAMSIZ];
    private static final int NAME_OFFSET = 0;

    Pointer name() {
        return new Pointer(buf, NAME_OFFSET);
    }

    //char mode[8];
    private static final int MODE_OFFSET = 0 + NAMSIZ;

    Pointer mode() {
        return new Pointer(buf, MODE_OFFSET);
    }

    //char uid[8];
    private static final int UID_OFFSET = MODE_OFFSET + 8;

    Pointer uid() {
        return new Pointer(buf, UID_OFFSET);
    }

    //char gid[8];
    private static final int GID_OFFSET = UID_OFFSET + 8;

    Pointer gid() {
        return new Pointer(buf, GID_OFFSET);
    }

    //char size[12];
    private static final int SIZE_OFFSET = GID_OFFSET + 8;

    Pointer size() {
        return new Pointer(buf, SIZE_OFFSET);
    }

    //char mtime[12];
    private static final int MTIME_OFFSET = SIZE_OFFSET + 12;

    Pointer mtime() {
        return new Pointer(buf, MTIME_OFFSET);
    }

    //char chksum[8];
    private static final int CHKSUM_OFFSET = MTIME_OFFSET + 12;
    static final int CHKSUM_SIZE = 8;

    Pointer chksum() {
        return new Pointer(buf, CHKSUM_OFFSET);
    }

    //char linkflag;
    private static final int LINKFLAG_OFFSET = CHKSUM_OFFSET + CHKSUM_SIZE;

    byte linkflag() {
        return buf[LINKFLAG_OFFSET];
    }

    //char linkname[NAMSIZ];
    private static final int LINKNAME_OFFSET = LINKFLAG_OFFSET + 1;

    Pointer linkname() {
        return new Pointer(buf, LINKNAME_OFFSET);
    }

    //char magic[8];
    private static final int MAGIC_OFFSET = LINKNAME_OFFSET + NAMSIZ;

    Pointer magic() {
        return new Pointer(buf, MAGIC_OFFSET);
    }

    //char uname[TUNMLEN];
    private static final int UNAME_OFFSET = MAGIC_OFFSET + 8;

    Pointer uname() {
        return new Pointer(buf, UNAME_OFFSET);
    }

    //char gname[TGNMLEN];
    private static final int GNAME_OFFSET = UNAME_OFFSET + TUNMLEN;

    Pointer gname() {
        return new Pointer(buf, GNAME_OFFSET);
    }

    //char devmajor[8];
    private static final int DEVMAJOR_OFFSET = GNAME_OFFSET + TGNMLEN;

    Pointer devmajor() {
        return new Pointer(buf, DEVMAJOR_OFFSET);
    }

    //char devminor[8];
    private static final int DEVMINOR_OFFSET = DEVMAJOR_OFFSET + 8;

    Pointer devminor() {
        return new Pointer(buf, DEVMINOR_OFFSET);
    }
}
