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

package org.simplx.time;

/**
 * This interface defines constants useful in being explicit about times in
 * calculations.
 *
 * @author Ken Arnold
 */
public interface TimeConstants {
    /**
     * The number of milliseconds per tick of {@link System#currentTimeMillis()}.
     * In other words, the number of millseconds per millsecond, or more simply,
     * 1. This is present for clarity in code that uses the other constants,
     * allowing it to be clear that an oepration is in milliseconds when other
     * operionts use {@link #SECONDS}, {@link #MINUTES}, etc.
     */
    int MILLISECONDS = 1;

    /** The number of millseconds per second. */
    int SECONDS = 1000;
    /** The number of millseconds per minute. */
    int MINUTES = 60 * SECONDS;
    /** The number of millseconds per hour. */
    int HOURS = 60 * MINUTES;
    /** The number of millseconds per day. */
    int DAYS = 24 * HOURS;
    /** The number of millseconds per week. */
    int WEEK = 7 * DAYS;
}
