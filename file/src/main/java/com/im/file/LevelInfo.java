package com.im.file;

class LevelInfo {
    int off;
    int got_match;
    int last_match;
    byte last_cond;

    public LevelInfo(int got_match, int last_match, byte last_cond) {
        this.got_match = got_match;
        this.last_match = last_match;
        this.last_cond = last_cond;
    }
}