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

package org.simplx.io;

import org.apache.commons.io.output.TeeOutputStream;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * This writer makes a copy of everything it sends along.  It is analogous to
 * the UNIX "tee" command.  Every operation on this writer will be sent first to
 * the "copy" writer and then to the main writer.
 * <p/>
 * (As of this writing, commons-io has a {@code TeeOutputStream}, but no {@code
 * TeeWriter} or I would have used it.)
 *
 * @see TeeOutputStream
 */
public class TeeWriter extends FilterWriter {
    private final Writer tee;   // the writer to make a copy to

    /**
     * Creates a new <tt>TeeWriter</tt>.
     *
     * @param out The main writer.
     * @param tee The writer on which a copy will be made.
     */
    public TeeWriter(Writer out, Writer tee) {
        super(out);
        this.tee = tee;
    }

    @Override
    public void close() throws IOException {
        tee.close();
        out.close();
    }

    @Override
    public void write(int c) throws IOException {
        tee.write(c);
        out.write(c);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        tee.write(cbuf, off, len);
        out.write(cbuf, off, len);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        tee.write(str, off, len);
        out.write(str, off, len);
    }

    @Override
    public void flush() throws IOException {
        tee.flush();
        out.flush();
    }
}
