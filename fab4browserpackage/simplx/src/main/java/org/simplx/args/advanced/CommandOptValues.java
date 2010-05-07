/*
 * Copyright (c) 2009, 2010, Ken Arnold All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the myself nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @SimplxCopyright
 */

package org.simplx.args.advanced;

import org.simplx.args.CommandOpt;

import java.lang.annotation.Annotation;

/**
 * This class exists so you can create an annotation to pass in to {@link
 * CommandLineAdvanced#getValueFor(String,Class,CommandOpt)}, primarily to set
 * any name (using {@link #multi() multi}), {@link #mode() mode} or {@link
 * #checks() checks}.
 */
@SuppressWarnings({"ClassExplicitlyAnnotation"})
public class CommandOptValues implements CommandOpt {
    private final char single;
    private final String multi;
    private final String deflt;
    private final String mode;
    private final String checks;
    private final String valueName;
    private final String desc;

    /**
     * Creates a new {@link CommandOpt} annotation object with all default
     * values.
     */
    public CommandOptValues() {
        this("", "", "");
    }

    /**
     * Creates a new {@link CommandOpt} annotation with some values set.
     *
     * @param multi  The value for {@link #multi() multi}.
     * @param mode   The value for {@link #mode() mode}.
     * @param checks The value for {@link #checks() checks}.
     */
    public CommandOptValues(String multi, String mode, String checks) {
        this('\0', multi, NO_DEFAULT, mode, checks, "", "");
    }

    /**
     * Creates a new {@link CommandOpt} annotation with all values set.
     *
     * @param single    The value for {@link #single() single}.
     * @param multi     The value for {@link #multi() multi}.
     * @param deflt     The value for {@link #deflt() deflt}.
     * @param mode      The value for {@link #mode() mode}.
     * @param checks    The value for {@link #checks() checks}.
     * @param valueName The value for {@link #valueName() valueName}.
     * @param desc      The value for {@link #desc() desc}.
     */
    public CommandOptValues(char single, String multi, String deflt,
            String mode, String checks, String valueName, String desc) {

        this.single = single;
        this.multi = multi;
        this.deflt = deflt;
        this.mode = mode;
        this.checks = checks;
        this.valueName = valueName;
        this.desc = desc;
    }

    /** See {@link CommandOpt#single()}. */
    @Override
    public char single() {
        return single;
    }

    /** See {@link CommandOpt#multi()}. */
    @Override
    public String multi() {
        return multi;
    }

    /** See {@link CommandOpt#deflt()}. */
    @Override
    public String deflt() {
        return deflt;
    }

    /** See {@link CommandOpt#mode()}. */
    @Override
    public String mode() {
        return mode;
    }

    /** See {@link CommandOpt#checks().} */
    @Override
    public String checks() {
        return checks;
    }

    /** See {@link CommandOpt#valueName()}. */
    @Override
    public String valueName() {
        return valueName;
    }

    /** See {@link CommandOpt#desc()}. */
    @Override
    public String desc() {
        return desc;
    }

    /** Returns <tt>CommandOpt.class</tt>. */
    @Override
    public Class<? extends Annotation> annotationType() {
        return CommandOpt.class;
    }
}
