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
 * This class exists as convenience for creating your own custom {@link Mapper}}
 * type.
 */
public abstract class MapperAdaptor implements Mapper {
    /** Creates a new basic mapper. */
    public MapperAdaptor() {
    }

    /** {@inheritDoc} */
    @Override
    public abstract Object map(CharSequence valString, Field field, Class type,
            CommandOpt anno, CommandLine line) throws Exception;

    /** Returns <tt>null</tt>; this type has no added checks. */
    @Override
    public Boolean check(String checkName, Object val, Field field, Class type,
            CommandOpt anno, CommandLine line) throws Exception {

        return null;
    }

    /**
     * Returns the type name of the field type, with the first letter changed to
     * lowercase.
     */
    @Override
    public CharSequence defaultValueName(Field field, Class type,
            CommandOpt anno, CommandLine line) {

        StringBuilder sb = new StringBuilder(type.getSimpleName());
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb;
    }
}