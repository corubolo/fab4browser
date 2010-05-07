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

import java.io.Writer;

import static org.simplx.io.StringBuilderWriter.Mode.APPEND;
import static org.simplx.io.StringBuilderWriter.Mode.INSERT;

/**
 * This class allows you to write to a target {@link StringBuffer} or {@link
 * StringBuilder} object.  This is helpful when you are building up a string and
 * want to add the result of some operation that writes to streams.
 * <p/>
 * Writing can happen in one of three modes: <ul> <li>{@link Mode#OVERWRITE
 * Mode.OVERWRITE}: Contents of the target will be overwritten starting at the
 * initial position. If the end of the target contents are reached, the
 * characters will be appended. <li>{@link Mode#APPEND Mode.APPEND}: All writes
 * will add to the end of the target.  The initial position will be the end of
 * the target; any position passed to the constructor will be ignored.
 * <li>{@link Mode#INSERT Mode.INSERT}: All writes will be inserted starting at
 * the initial position; successive writes will insert where the previous write
 * stopped.  This means that a <tt>write('1')</tt> followed by a
 * <tt>write('2')</tt> will insert <tt>"12"</tt> into the string, rather than
 * "21"} (which would result if the second insert also started that the initial
 * position. </ul>
 * <p/>
 * (Note that this would all be easier if {@link StringBuffer} and {@link
 * StringBuilder} shared a common base type that had all the commonly named
 * methods, but it does not. The closest is {@link Appendable}, which only has
 * the <tt>append</tt> methods, not <tt>insert</tt> or <tt>replace</tt>.)
 */
public class StringBuilderWriter extends Writer {
    /**
     * The modes for {@link StringBuilderWriter}.
     *
     * @see StringBuilderWriter
     */
    public enum Mode {
        /** Overwrite the target, appending to the end if need be. */
        OVERWRITE,
        /** Append to the end of the target. */
        APPEND,
        /** Insert into the target. */
        INSERT
    }

    private final StringBuilder build;
    private final StringBuffer buffr;
    private final Mode mode;
    private int pos;

    /**
     * Creates a writer that will append to a string buffer. Equivalent to
     * <pre>
     * StringBuilder(buffer, APPEND, 0)
     * </pre>
     *
     * @param buffer The string buffer.
     */
    public StringBuilderWriter(StringBuffer buffer) {
        this(buffer, APPEND, 0);
    }

    /**
     * Creates a writer that will write to a string buffer with a given mode and
     * starting position.
     *
     * @param buffer The string buffer.
     * @param mode   The mode with which to write.
     * @param pos    The initial position (ignored if mode is <tt>APPEND</tt>).
     */
    public StringBuilderWriter(StringBuffer buffer, Mode mode, int pos) {
        if (buffer == null)
            throw new NullPointerException("buffer");
        buffr = buffer;
        build = null;
        this.pos = mode == APPEND ? buffer.length() : pos;
        this.mode = mode;
    }

    /**
     * Creates a writer that will append to a string builder. Equivalent to
     * <pre>
     * StringBuilder(builder, APPEND, 0)
     * </pre>
     *
     * @param builder The string Builder.
     */
    public StringBuilderWriter(StringBuilder builder) {
        this(builder, APPEND, 0);
    }

    /**
     * Creates a writer that will write to a string builder with a given mode
     * and starting position.
     *
     * @param builder The string builder.
     * @param mode    The mode with which to write.
     * @param pos     The initial position (ignored if mode is
     *                <tt>APPEND</tt>).
     */
    public StringBuilderWriter(StringBuilder builder, Mode mode, int pos) {
        if (builder == null)
            throw new NullPointerException("builder");
        build = builder;
        buffr = null;
        this.pos = mode == APPEND ? builder.length() : pos;
        this.mode = mode;
    }

    /**
     * Returns the current position.  This is always the same number of
     * characters from the intial position as have been written to this stream.
     */
    public int getPos() {
        return pos;
    }

    /**
     * Returns the {@link StringBuffer} passed to the constructor, or
     * <tt>null</tt> if the target is a {@link StringBuilder}.
     */
    public StringBuffer getStringBuffer() {
        return buffr;
    }

    /**
     * Returns the {@link StringBuilder} passed to the constructor, or
     * <tt>null</tt> if the target is a {@link StringBuilder}.
     */
    public StringBuilder getStringBuilder() {
        return build;
    }

    /**
     * Returns the target, either {@link StringBuilder} or {@link
     * StringBuffer}.
     */
    public Appendable getTarget() {
        return (build == null ? buffr : build);
    }

    /**
     * This returns the result of calling <tt>toString</tt> on the target.
     *
     * @return The current contents of the target.
     */
    @Override
    public String toString() {
        return buffr != null ? buffr.toString() : build.toString();
    }

    /** Do nothing. */
    @Override
    public void flush() {
    }

    /** Do nothing. */
    @Override
    public void close() {
    }

    /** {@inheritDoc} */
    @Override
    public Writer append(char c) {
        write(c);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Writer append(CharSequence csq) {
        write(csq.toString());
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Writer append(CharSequence csq, int start, int end) {
        write(csq.toString(), start, end);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void write(int c) {
        char ch = (char) c;
        if (mode == INSERT) {
            if (buffr != null)
                buffr.insert(pos, ch);
            else
                build.insert(pos, ch);
        } else {
            if (buffr != null) {
                if (pos >= buffr.length())
                    buffr.append(ch);
                else
                    buffr.setCharAt(pos, ch);
            } else {
                if (pos >= build.length())
                    build.append(ch);
                else
                    build.setCharAt(pos, ch);
            }
        }
        pos++;
    }

    /** {@inheritDoc} */
    @Override
    public void write(String str) {
        write(str, 0, str.length());
    }

    /** {@inheritDoc} */
    @Override
    public void write(char[] cbuf) {
        write(cbuf, 0, cbuf.length);
    }

    /** {@inheritDoc} */
    @Override
    public void write(String str, int off, int len) {
        if (len == 0)
            return;
        if (len < 0)
            throw new IllegalArgumentException("len < 0");
        if (mode == INSERT) {
            if (buffr != null)
                buffr.insert(pos, str, off, off + len);
            else
                build.insert(pos, str, off, off + len);
        } else {
            if (buffr != null) {
                if (pos >= buffr.length())
                    buffr.append(str, off, off + len);
                else
                    buffr.replace(pos, pos + len, str);
            } else {
                if (pos >= build.length())
                    build.append(str, off, off + len);
                else
                    build.replace(pos, pos + len, str);
            }
        }
        pos += len;
    }

    /** {@inheritDoc} */
    @Override
    public void write(char[] cbuf, int off, int len) {
        if (len == 0)
            return;
        if (len < 0)
            throw new IllegalArgumentException("len < 0");
        if (mode == INSERT) {
            if (buffr != null)
                buffr.insert(pos, cbuf, off, len);
            else
                build.insert(pos, cbuf, off, len);
        } else {
            if (buffr != null) {
                if (pos >= buffr.length())
                    buffr.append(cbuf, off, len);
                else
                    buffr.replace(pos, len, new String(cbuf, off, len));
            } else {
                if (pos >= build.length())
                    build.append(cbuf, off, len);
                else
                    build.replace(pos, len, new String(cbuf, off, len));
            }
        }
        pos += len;
    }
}