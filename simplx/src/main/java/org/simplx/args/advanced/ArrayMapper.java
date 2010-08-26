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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

/**
 * This class implements the standard mapping for array options. If you are
 * writing a custom mapper, you may find it useful to extend this; see {@link
 * MapperAdaptor} for an overview of why you would or would not write custom
 * mappers.
 */
public class ArrayMapper extends MapperAdaptor {
    /** Creates a new array mapper. */
    public ArrayMapper() {
    }

    /**
     * Adds the values in the strong to the array in the field, creating it if
     * need be.  The string is spilt into values using {@link
     * CommandLineAdvanced#getValueSplitter()}. If the result is a single empty
     * string, then the value string is interepreted as a zero-length array.
     * Otherwise, each element in the split-up string is passed to the mapper
     * for the field array's component type.
     * <p/>
     * Nested arrays are not supported.
     */
    @Override
    public Object map(CharSequence valStr, Field field, Class type,
            CommandOpt anno, CommandLine line) throws IllegalAccessException {

        CommandLineAdvanced adv = line.getAdvancedKnobs();

        Class compType = type.getComponentType();
        if (compType.isArray()) {
            throw new IllegalArgumentException(
                    "Multi-dimensional array fields not supported");
        }

        Object curArray = field.get(line.getHolder());

        Pattern p;
        String pat = anno.mode();
        if (pat.length() > 0)
            p = Pattern.compile(pat);
        else
            p = adv.getValueSplitter();

        String[] vals = p.split(valStr);

        // Special case: "" means "empty array"
        if (vals.length == 1 && vals[0].equals(""))
            vals = new String[0];

        int curLength;
        if (curArray == null) {
            curLength = 0;
        } else {
            curLength = Array.getLength(curArray);
        }

        Object newArray = Array.newInstance(compType, curLength + vals.length);
        if (curLength > 0) {
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(curArray, 0, newArray, 0, curLength);
        }

        for (int i = 0; i < vals.length; i++) {
            String val = vals[i];
            Object valObj = adv.getValueFor(val, compType, anno);
            Array.set(newArray, curLength + i, valObj);
        }

        return newArray;
    }

    /**
     * Returns the {@link Mapper#defaultValueName)} for the component type,
     * followed by the value splitter display string, followed by
     * <tt>"..."</tt>
     */
    @Override
    public CharSequence defaultValueName(Field field, Class type,
            CommandOpt anno, CommandLine line) {

        CommandLineAdvanced adv = line.getAdvancedKnobs();
        Mapper mapper = adv.getMapperFor(type.getComponentType());
        return mapper.defaultValueName(null, type, null, line) +
                adv.getValueSplitterDisplay() + "...";
    }
}
