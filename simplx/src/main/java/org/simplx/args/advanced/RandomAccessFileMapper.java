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

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;

/**
 * This class implements the standard mapping for {@link RandomAccessFile}.
 * options.  If you are writing a custom mapper, you may find it useful to
 * extend this; see {@link MapperAdaptor} for an overview of why you would or
 * would not write custom mappers.
 */
public class RandomAccessFileMapper extends MapperAdaptor {
    /** Creates a new random access file mapper. */
    public RandomAccessFileMapper() {
    }

    /**
     * Returns a {@link RandomAccessFile} constructed with the value string. If
     * the {@link CommandOpt#mode} is specified, that will be the mode string
     * for the constructor.  Otherwise the mode will be <tt>"r"</tt>.
     *
     * @see RandomAccessFile#RandomAccessFile(String,String)
     */
    @Override
    public Object map(CharSequence valString, Field field, Class type,
            CommandOpt anno, CommandLine line) throws FileNotFoundException {

        String mode = anno == null ? "" : anno.mode().trim();
        if (mode.length() == 0)
            mode = "r";
        return new RandomAccessFile(valString.toString(), mode);
    }

    /** Returns <tt>"file"</tt>. */
    @Override
    public CharSequence defaultValueName(Field field, Class type,
            CommandOpt anno, CommandLine line) {

        return "file";
    }
}
