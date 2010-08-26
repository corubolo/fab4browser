package com.im.file;

import org.simplx.c.Pointer;

class Bang {
    interface Parser {
        int parse(MagicSet ms, MagicEntry me, Pointer line);
    }
    String name;
    Parser parser;

    Bang(String name, Parser parser) {
        this.name = name;
        this.parser = parser;
    }
}