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

import org.simplx.io.StringBuilderWriter;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * This class provides a log formatter that shows the time stamp as elapsed
 * time, and optimizes the optout for brevity; for example, it prints only the
 * final component of the logger name rather than the entire one, which is
 * usually unique enough.
 */
public class BasicFormatter extends Formatter {
    private static final long RAW_START = System.currentTimeMillis();
    private static final long START =
            RAW_START + TimeZone.getDefault().getOffset(RAW_START);

    private static final String TIME_FMT = "HH:mm:ss.SSS";
    private static final DateFormat ELAPSE_FMT = new SimpleDateFormat(TIME_FMT,
            Locale.getDefault());

    /** This is the base amount for the string buffer size. */
    private static final int BASE_FMT_LEN = TIME_FMT.length() + // the time
            2 +     // colon after the time
            2 +     // colon after the logger name
            6 +     // log level
            2 +     // colon after the log level
            100;    // room for message to grow by formatting

    /**
     * We never actually care about this, but it is required to be present if we
     * want to use the version of format() that appends to a StringBuffer, so we
     * just use the same one everywhere.
     */
    private static final FieldPosition DUMMY = new FieldPosition(
            DateFormat.YEAR_FIELD);

    /** {@inheritDoc} */
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Override
    public synchronized String format(LogRecord record) {
        String loggerName = record.getLoggerName();
        int loggerNameLength = 0;
        if (loggerName != null) {
            int dot = loggerName.lastIndexOf('.');
            if (dot >= 0)
                loggerName = loggerName.substring(dot + 1);
            loggerNameLength = loggerName.length();
        }

        StringBuffer sb = new StringBuffer(
                BASE_FMT_LEN + loggerNameLength + record.getMessage().length());

        long elapsed = System.currentTimeMillis() - START;
        ELAPSE_FMT.format(elapsed, sb, DUMMY);
        sb.append(": ");
        if (loggerName != null) {
            sb.append(loggerName);
            sb.append(": ");
        }
        String message = formatMessage(record);
        String levelName = record.getLevel().getLocalizedName();
        sb.append(levelName);
        sb.append(": ");
        for (int i = levelName.length(); i < 6; i++)
            sb.append(' ');
        sb.append(message);
        sb.append('\n');
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            Writer sw = new StringBuilderWriter(sb);
            PrintWriter pw = new PrintWriter(sw);
            thrown.printStackTrace(pw);
            pw.close();
        }
        return sb.toString();
    }
}