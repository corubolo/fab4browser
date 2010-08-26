package com.im.file;

import org.simplx.c.Pointer;

class ComprDesc {
    Pointer magic;
    int maglen;
    int silent;
    String[] argv;

    static final ComprDesc[] compr = {
            /* compressed */
            new ComprDesc("\037\235", 2, 1, "gzip", "-cdq"),

            /* Uncompress can get stuck; so use gzip first if we have it
                  * Idea from Damien Clark, thanks! */
            /* compressed */
            new ComprDesc("\037\235", 2, 1, "uncompress", "-c"),

            /* gzipped */
            new ComprDesc("\037\213", 2, 1, "gzip", "-cdq"),

            /* frozen */
            new ComprDesc("\037\236", 2, 1, "gzip", "-cdq"),

            /* SCO LZH */
            new ComprDesc("\037\240", 2, 1, "gzip", "-cdq"),

            /* the standard pack utilities do not accept standard input */
            /* packed */
            new ComprDesc("\037\036", 2, 0, "gzip", "-cdq"),

            /* pkzipped, */
            new ComprDesc("PK\3\4", 4, 1, "gzip", "-cdq"),

            /* ...only first file examined */
            /* bzip2-ed */
            new ComprDesc("BZh", 3, 1, "bzip2", "-cd"),

            new ComprDesc("LZIP", 4, 1, "lzip", "-cdq"),

            /* XZ Utils */
            new ComprDesc("\3757zXZ\0", 6, 1, "xz", "-cd"),
    };

    public ComprDesc(String magic, int maglen, int silent, String... argv) {
        this.magic = new Pointer(magic);
        this.maglen = maglen;
        this.argv = argv;
        this.silent = silent;
    }
}
