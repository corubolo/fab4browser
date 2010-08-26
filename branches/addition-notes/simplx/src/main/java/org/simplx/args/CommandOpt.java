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

package org.simplx.args;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This attribute is used to mark and define a single field that is a command
 * line option, to be interpreted by {@link CommandLine}.
 *
 * @see CommandLine
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CommandOpt {
    /**
     * A constant used to mark that an option has no default value.  More
     * precisely, it means that no default will be set by {@link CommandLine}.
     * This is the default value for {@link #deflt()}; you will probably never
     * need to use it explicitly.
     */
    String NO_DEFAULT = "[org.simplx.args.NO_DEFAULT]";

    /**
     * A constant used to mark that an option has no multi-character value. By
     * default, each option has a multi-character version, even if it only one
     * character long (such as for a field named "x".  Setting this for the
     * value for {@link #multi()} means that only the single character can be
     * used for the option.
     */
    String NO_MULTI = "[org.simplx.args.NO_MULTI]";

    /**
     * The single character option for this field.  By default this is
     * <tt>'\0'</tt>, meaning this field has no single character option.
     */
    char single() default '\0';

    /**
     * An alternate multi-character name for this field.  The default
     * multi-character name is the name of the field itself.  This allows you to
     * override that default.
     */
    String multi() default "";

    /**
     * The default value for this field.  By default, this is {@link
     * #NO_DEFAULT}, meaning that the field's value will be left alone.  If this
     * is set to anything else, and the option is not specified on the command
     * line, the field will be initialized as if this was the value for the
     * option on the command line.  This will be done after all options have
     * been applied, and before {@link CommandLine#apply(String...)} returns.
     */
    String deflt() default NO_DEFAULT;

    /**
     * A mode parameter than can be interpreted when the option value is set.
     * For example, when used for a {@link RandomAccessFile} field, this can be
     * <tt>"r"</tt> or <tt>"rw"</tt> for the <tt>mode</tt> parameter to the
     * constructor.
     */
    String mode() default "";

    /**
     * Checks that will be applied to the value when it is set.  These are
     * typically boolean checks from the class (such as using
     * <tt>"!isEmpty"</tt> to invoke {@link String#isEmpty()}), or simple
     * actions, such as {@link File#mkdirs}.
     */
    String checks() default "";

    /**
     * The name of the value paramter for usage messages.  This is ignored for
     * boolean options.
     *
     * @see CommandLine#fullUsage(Writer)
     */
    String valueName() default "";

    /**
     * A description of the option for usage messages.
     *
     * @see CommandLine#fullUsage(Writer)
     */
    String desc() default "";
}