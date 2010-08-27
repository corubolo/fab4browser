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

import org.simplx.args.CommandLine;
import org.simplx.args.CommandLineException;
import org.simplx.args.CommandOpt;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * This interface is for the advanced type mapper feature of {@link
 * CommandLine}.
 * <p/>
 * Unless you are using sophisticated handling for a type of field in more than
 * one application, you should probably just do it in your own code after
 * calling {@link CommandLine#apply}, rather than writing a custom mapper.  A
 * mapper lets you put that sophisticated code into a place where it can be used
 * for any fields of certain types, no matter where they appear.
 * <p/>
 * The methods take three specifying parameters: Field, type, and annotation. In
 * the normal use related to options set in a field, these will be the obvious
 * values: The field, its {@link Field#getType()}, and its {@link
 * Field#getAnnotation(Class)} for {@link CommandOpt}.
 * <p/>
 * However, this is also used for values decoded from {@link
 * CommandLineAdvanced#getValueFor}. In that case, the field value will be
 * <tt>null</tt> (because there is no field), the type and annotation will be
 * passed in by the user, and the annotation may be <tt>null</tt>.  (The type
 * cannot be <tt>null</tt>; that is checked in {@link CommandLineAdvanced#getValueFor}.)
 * <p/>
 * Be sure to handle both these cases in any mapper you write.
 */
public interface Mapper {
    /**
     * Returns the value object derived for the field from the specified string.
     * This is done first, then any checks are applied, and finally the value is
     * assigned to the field.  This method does not set the field itself.
     * <p/>
     * This method is declared to throw {@link Exception}.  An exception thrown
     * by the mapping is wrapped in a {@link CommandLineException}.  It is good
     * form to restrict the exception clause thrown in your method to those it
     * may actually throw. For example, if your class might raise an {@link
     * IOException}, your implementation of <tt>check</tt> should declare that
     * it throws {@link IOException}, not {@link Exception}.
     * <p/>
     *
     * @param valString The string to read the value from.
     * @param field     The field whose value is being set.  This may be
     *                  <tt>null</tt>.
     * @param type      The type of value.
     * @param anno      The annotation for the field.  This may be
     *                  <tt>null</tt>.
     * @param line      The command line doing the mapping.
     *
     * @return The value to store in the field, assuming all specified checks
     *         are passed.
     *
     * @throws Exception An exception thrown by the mapper, directly or
     *                   indirectly.
     */
    Object map(CharSequence valString, Field field, Class type, CommandOpt anno,
            CommandLine line) throws Exception;

    /**
     * Runs a single check from {@link CommandOpt#checks}.  If this method
     * returns <tt>true</tt> ({@link Boolean#TRUE}) or <tt>false</tt> (@link
     * Boolean#FALSE}), that signals that this function has handled the given
     * check, which has succeded (<tt>true</tt>) or failed (<tt>false</tt>). You
     * should return <tt>true</tt> for checks that are actually operations.
     * <p/>
     * If this method returns <tt>null</tt>, that means that this method did not
     * handle the check, and the default mechanism should be used. (That
     * mechanism is to find a non-static, no-argument method for the appropriate
     * class and invoke it.)
     *
     * @param checkName The name of the check.  This is without any surrounding
     *                  spaces or a leading exclamation point.
     * @param val       The value that is being assigned to the field (the value
     *                  returned by {@link #map}).
     * @param field     The field whose value is being set.  This may be
     *                  <tt>null</tt>.
     * @param type      The type of value.
     * @param anno      The annotation for the field.  This may be
     *                  <tt>null</tt>.
     * @param line      The command line doing the mapping.
     *
     * @return <tt>true</tt> or <tt>false</tt> if the implementation handles the
     *         check, or <tt>null</tt> if it hasn't.
     *
     * @throws Exception An exception thrown by the check is wrapped in a {@link
     *                   CommandLineException}.  It is good form to restrict the
     *                   exception clause thrown in your method to those it may
     *                   actually throw. For example, if your class might raise
     *                   an {@link IOException}, your implementation of
     *                   <tt>check</tt> should declare that it throws {@link
     *                   IOException}, not {@link Exception}.
     */
    Boolean check(String checkName, Object val, Field field, Class type,
            CommandOpt anno, CommandLine line) throws Exception;

    /**
     * Returns the type-implying name to be used in usage messages for this
     * type.
     *
     * @param field The field.  This may be <tt>null</tt>.
     * @param type  The type of value.
     * @param anno  The annotation for the field.  This may be <tt>null</tt>.
     * @param line  The command line doing the mapping.
     *
     * @return The value name for usage messages.
     */
    CharSequence defaultValueName(Field field, Class type, CommandOpt anno,
            CommandLine line);
}