package com.im.file;

/*
        modified by Chris Lowth - 9 April 2000
        to add mime type strings to the types table.
*/
class MimeName {
    static final MimeName[] types = {
            new MimeName("C program", "text/x-c"), new MimeName("C++ program",
                    "text/x-c++"), new MimeName("make commands",
                    "text/x-makefile"), new MimeName("PL/1 program",
                    "text/x-pl1"), new MimeName("assembler program",
                    "text/x-asm"), new MimeName("English", "text/plain"),
            new MimeName("Pascal program", "text/x-pascal"), new MimeName(
                    "mail", "text/x-mail"), new MimeName("news", "text/x-news"),
            new MimeName("Java program", "text/x-java"), new MimeName(
                    "HTML document", "text/html"), new MimeName("BCPL program",
                    "text/x-bcpl"), new MimeName(
                    "M4 macro language pre-processor", "text/x-m4"),
            new MimeName("PO (gettext message catalogue)", "text/x-po"),
            new MimeName("cannot happen error on names.h/types",
                    "error/x-error")
    };

    final String human;
    final String mime;

    MimeName(String human, String mime) {
        this.human = human;
        this.mime = mime;
    }
}
