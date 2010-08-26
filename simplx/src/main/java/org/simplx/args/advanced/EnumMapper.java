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

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.regex.Pattern;

/**
 * This class implements the standard mapping for <tt>enum</tt> options. If you
 * are writing a custom mapper, you may find it useful to extend this; see
 * {@link MapperAdaptor} for an overview of why you would or would not write
 * custom mappers.
 */
public class EnumMapper extends MapperAdaptor {
    private static final Pattern OPTIONAL = Pattern.compile("_");

    /** Creates a new enum mapper. */
    public EnumMapper() {
    }

    /**
     * Parses values for an <tt>enum</tt> field as described in the overal
     * documentation for {@link CommandLine}.
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public Object map(CharSequence valString, Field field, Class type,
            CommandOpt anno, CommandLine line) {

        Class<? extends Enum> enumClass = (Class<? extends Enum>) type;
        Enum[] possibles = enumClass.getEnumConstants();
        EnumSet matches = EnumSet.noneOf(enumClass);

        String str = valString.toString();
        for (Enum e : possibles) {
            String name = e.toString();
            if (str.equalsIgnoreCase(name))
                return e;
            String strStripped = OPTIONAL.matcher(str).replaceAll("");
            String nameStripped = OPTIONAL.matcher(name).replaceAll("");
            if (strStripped.equalsIgnoreCase(nameStripped))
                return e;

            if (abbreviation(str, name) || abbreviation(strStripped,
                    nameStripped)) {
                matches.add(e);
            }
        }

        // No exact match, see if it is an abbreviation
        if (matches.size() == 0) {
            throw new CommandLineException(line,
                    '"' + str + "\": unknown value");
        } else if (matches.size() > 1) {
            throw new CommandLineException(line,
                    '"' + str + "\": ambiguous; could be " + matches);
        }

        return matches.iterator().next();
    }

    private static boolean abbreviation(String str, String name) {
        return str.regionMatches(true, 0, name, 0, str.length());
    }
}
