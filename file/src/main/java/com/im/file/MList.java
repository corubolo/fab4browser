package com.im.file;

class MList {
    Magic[] magic;                /* array of magic entries */
    int nmagic;                        /* number of entries in array */
    int mapped;  /* allocation type: 0 => apprentice_file
		  *                  1 => apprentice_map + malloc
		  *                  2 => apprentice_map + mmap */
    MList next;
    MList prev;
}