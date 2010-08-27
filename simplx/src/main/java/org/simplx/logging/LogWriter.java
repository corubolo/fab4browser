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

package org.simplx.logging;

import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a type of {@link Writer} that writes its output to a specified
 * logger. This can be used to hand to existing coe that puts out text to a
 * {@link Writer} and have that text appear in the log. Each line of output
 * results in a {@link Logger#log(Level,String)} call at a given level.
 *
 * @author Ken Arnold
 */
@SuppressWarnings({"NonConstantLogger"})
public class LogWriter extends Writer {
    private final Logger logger;
    private final Level level;
    private final String prefix;
    private char[] pending;
    private boolean closed;

    /**
     * Creates a new {@link LogWriter}.
     *
     * @param logger The logger that output will be sent to.
     * @param level  The level at which the logging will happen.
     * @param prefix A prefix to put in front of each line logged.
     */
    public LogWriter(Logger logger, Level level, String prefix) {
        if (logger == null)
            throw new NullPointerException("logger");
        if (level == null)
            throw new NullPointerException("level");
        this.logger = logger;
        this.level = level;
        this.prefix = prefix == null ? "" : prefix;
    }

    /**
     * Writes the given set of text to a log, breaking it up into lines. Any
     * partial line will be stored pending the next full-line write, or a {@link
     * #flush()} or {@link #close()}.
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        ensureOpen();
        if (!isLogging()) {
            pending = null;
            return;
        }

        int start = off;
        int end = off + len;
        int i;
        while ((i = ArrayUtils.indexOf(cbuf, '\n', start)) >= 0 && i < end) {
            int lineLen = i - start;
            String msg;
            if (pending == null) {
                msg = new String(cbuf, start, lineLen);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(pending);
                sb.append(cbuf, start, lineLen);
                pending = null;
                msg = sb.toString();
            }
            logger.log(level, prefix + msg);
            start = i + 1;
        }

        if (i == end) {
            pending = null;
            return;
        }

        if (pending == null)
            pending = new char[0];

        pending = ArrayUtils.addAll(pending, ArrayUtils.subarray(cbuf, start,
                end));
    }

    /**
     * Returns {@code true} if this writer is actually logging messages, based
     * on the log level and logger.
     */
    public boolean isLogging() {
        return logger.isLoggable(level);
    }

    /**
     * Flushes out any pending text. If a partial line is pending output, it is
     * logged.
     */
    @Override
    public void flush() {
        if (pending != null && pending.length > 0) {
            if (isLogging())
                logger.log(level, new String(pending));
        }
        pending = null;
    }

    private void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("writer closed");
    }

    /**
     * Closes this writer, after flushing any pending text.
     *
     * @see #flush()
     */
    @Override
    public void close() {
        if (!closed) {
            flush();
            closed = true;
        }
    }
}
