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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * This mapper implementation handles general field types.  It is used to handle
 * all types for which a mapping is not defined.
 *
 * @see CommandLine#isTryAllTypes()
 */
public class ObjectMapper extends MapperAdaptor {
    private static final Map<Class, Class> WRAPPER_TYPE;

    static {
        WRAPPER_TYPE = new HashMap<Class, Class>();
        WRAPPER_TYPE.put(boolean.class, Boolean.class);
        WRAPPER_TYPE.put(char.class, Character.class);
        WRAPPER_TYPE.put(byte.class, Byte.class);
        WRAPPER_TYPE.put(short.class, Short.class);
        WRAPPER_TYPE.put(int.class, Integer.class);
        WRAPPER_TYPE.put(long.class, Long.class);
        WRAPPER_TYPE.put(float.class, Float.class);
        WRAPPER_TYPE.put(double.class, Double.class);
    }

    /** Creates a new object mapper. */
    public ObjectMapper() {
    }

    /**
     * Searches the field's type for a constructor that takes a single {@link
     * String} parameter. If it finds one, it invokes it with <tt>valStr</tt> to
     * create the object and returns it.
     */
    @Override
    public Object map(CharSequence valString, Field field, Class type,
            CommandOpt anno, CommandLine line) {

        try {
            if (type.isPrimitive()) {
                type = WRAPPER_TYPE.get(type);
                assert type != null;
            }
            Constructor ctor = type.getConstructor(String.class);
            return ctor.newInstance(valString);
        } catch (NoSuchMethodException e) {
            // handle this exception to throw one that is more understandable
            throw new IllegalArgumentException(
                    "No single-string constructor for " + type, e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(
                    "Cannot construct object of " + type, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                    "Cannot access string constructor of " + type, e);
        } catch (InvocationTargetException e) {
            throw new CommandLineException(line,
                    "String constructor threw exception", e);
        }
    }
}