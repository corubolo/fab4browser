package com.im.file;

class CondTblS {
    String name;
    byte cond;
    static final CondTblS[] cond_tbl = {
            new CondTblS("if", Magic.COND_IF), new CondTblS("elif",
                    Magic.COND_ELIF), new CondTblS("else",
                    Magic.COND_ELSE), new CondTblS("",
                    Magic.COND_NONE),
    };

    CondTblS(String name, byte cond) {
        this.name = name;
        this.cond = cond;
    }
}
