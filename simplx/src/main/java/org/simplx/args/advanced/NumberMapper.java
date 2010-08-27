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

import java.lang.reflect.Field;

/**
 * This class implements the standard mapping for numeric options. If you are
 * writing a custom mapper, you may find it useful to extend this; see {@link
 * MapperAdaptor} for an overview of why you would or would not write custom
 * mappers.
 */
public class NumberMapper extends ObjectMapper {
    /** Creates a new number mapper. */
    public NumberMapper() {
    }

    /**
     * Returns the appropriately-typed {@link Number} object for the string.
     * This handles a leading plus-sign; otherwise it just uses the appropriate
     * type's constructor.
     */
    @Override
    public Object map(CharSequence valString, Field field, Class type,
            CommandOpt anno, CommandLine line) {
        // ignore leading plus (some number parsers balk at it)
        if (valString.length() > 0 && valString.charAt(0) == '+') {
            valString = valString.toString().substring(1);
        }
        return super.map(valString, field, type, anno, line);
    }

    /** Returns <tt>"num"</tt>. */
    @Override
    public CharSequence defaultValueName(Field field, Class type,
            CommandOpt anno, CommandLine line) {

        return "num";
    }
}
