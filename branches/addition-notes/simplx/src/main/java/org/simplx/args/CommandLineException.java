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

package org.simplx.args;

import org.simplx.io.StringBuilderWriter;

/**
 * Signals that the user specified an option in an improper way. The most common
 * errors are not providing an argument to an option that needed one or
 * specifying an unknown option.
 *
 * @author Ken Arnold
 */
public class CommandLineException extends RuntimeException {
    private final CommandLine line;

    private static final String USAGE_SEP = "\n" + "usage:\n";

    private static final long serialVersionUID = 4503820475450471907L;

    /**
     * Creates an exception with the given detail string.
     *
     * @param msg A descriptive message.
     */
    public CommandLineException(String msg) {
        super(msg);
        line = null;
    }

    /**
     * Creates an exception with the given detail string and cause.
     *
     * @param msg   A descriptive message.
     * @param cause The related exception.
     */
    public CommandLineException(String msg, Throwable cause) {
        super(msg, cause);
        line = null;
    }

    /**
     * Creates an exception with the given detail string.
     *
     * @param line The {@link CommandLine} object being processed.
     * @param msg  A descriptive message.
     */
    public CommandLineException(CommandLine line, String msg) {
        super(msg);
        this.line = line;
    }

    /**
     * Creates an exception with the given detail string and cause.
     *
     * @param line  The {@link CommandLine} object being processed.
     * @param msg   A descriptive message.
     * @param cause The related exception.
     */
    public CommandLineException(CommandLine line, String msg, Throwable cause) {
        super(msg, cause);
        this.line = line;
    }

    @Override
    public String getMessage() {
        return withUsage(super.getMessage());
    }

    @Override
    public String getLocalizedMessage() {
        String msg = super.getLocalizedMessage();
        if (msg.contains(USAGE_SEP))
            return msg;
        return withUsage(msg);
    }

    private String withUsage(String msg) {
        if (line == null)
            return msg;

        StringBuilder sb = new StringBuilder(msg);
        sb.append(USAGE_SEP);
        line.briefUsage(new StringBuilderWriter(sb));
        return sb.toString();
    }
}
