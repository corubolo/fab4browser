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

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A logger that can be used in places where a logger is required, but no log
 * messages are desired.
 *
 * @author Ken Arnold
 */
public class NullLogger extends Logger {
    /** A filter that filters out all log messages. */
    public static final Filter NULL_FILTER = new Filter() {
        /**
         * Returns {@code false}.
         */
        @Override
        public boolean isLoggable(LogRecord record) {
            return false;
        }
    };

    /** A logger that never logs anything. */
    public static final Logger NULL_LOGGER = new NullLogger();

    /**
     * Creates a new {@link NullLogger}. The name and bundle name will be {@code
     * "org.simplx.log.NullLogger"}.
     */
    private NullLogger() {
        super(NullLogger.class.getName(), null);
        super.setLevel(Level.OFF);
        super.setFilter(NULL_FILTER);
        super.setUseParentHandlers(false);
    }

    /**
     * This call does nothing, in order to prevent changing the level from
     * {@link Level#OFF}.
     */
    @Override
    public void setLevel(Level newLevel) throws SecurityException {
    }

    /**
     * This call does nothing, in order to prevent changing the filter from
     * {@link #NULL_FILTER}.
     */
    @Override
    public void setFilter(Filter newFilter) throws SecurityException {
    }

    /**
     * This call does nothing, in order to prevent changing the value from
     * {@code false}.
     */
    @Override
    public void setUseParentHandlers(boolean useParentHandlers) {
    }
}
