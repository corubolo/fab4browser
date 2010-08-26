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
import org.simplx.args.CommandOpt;

import java.util.regex.Pattern;

/**
 * This interface defines methods that are hooks for using advanced features of
 * {@link CommandLine}.  This is not needed by most users.
 */
public interface CommandLineAdvanced {
    /**
     * Adds a mapping to be used for this {@link CommandLine} object.  Mappers
     * are an advanced feature described in the interface {@link Mapper}.
     * Mappings added with this method will be consulted before the global
     * default mappings, in the order they were added.
     *
     * @param mapper The mapper to add.
     * @param types  The list of types that this mapper will handle.  When
     *               searching for a mapper, the first mapper will be used whose
     *               list of types has one that is the same type, or a
     *               supertype, of the field being considered.
     */
    void addMapping(Mapper mapper, Class... types);

    /**
     * Returns the mapper that will be used for a given type.
     *
     * @param type The type for which you want the mapper.
     */
    Mapper getMapperFor(Class<?> type);

    /**
     * Returns the value for a string, parsed as if it was a field marked with a
     * {@link CommandOpt} annotation.  This is used in {@link ArrayMapper} and
     * may be useful in other similar contexts, where you don't have an actual
     * field into which to store the values directly.
     *
     * @param valueString The value as it would be on the command line.
     * @param type        The type of the value, as if it was the field type.
     * @param anno        The annotation for the field, or <tt>null</tt> to use
     *                    default values.  If you want to create your own, you
     *                    will probably find the class {@link CommandOptValues}
     *                    useful.
     *
     * @return The value as if was decoded from the string for an option field.
     */
    Object getValueFor(String valueString, Class type, CommandOpt anno);

    /**
     * Returns the pattern used for splitting up values for array fields.
     *
     * @see #setValueSplitter(Pattern, String)
     */
    Pattern getValueSplitter();

    /**
     * Returns the string used as a value separator for showing usage array
     * fields in usage messages.
     *
     * @see #setValueSplitter(Pattern, String)
     */
    String getValueSplitterDisplay();

    /**
     * Sets the pattern used for splitting up values for array fields.
     *
     * @param splitterPattern The pattern to use for splitting up values for
     *                        array fields.
     * @param splitterDisplay The string that will be shown as a value separator
     *                        in usage messages for array fields.  This should
     *                        be a simple form of the pattern.
     */
    void setValueSplitter(Pattern splitterPattern, String splitterDisplay);
}