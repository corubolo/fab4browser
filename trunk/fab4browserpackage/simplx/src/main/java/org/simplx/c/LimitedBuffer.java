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

package org.simplx.c;

import java.io.IOException;

/** This appends, silently ignoring any characters beyond the max. */
class LimitedBuffer implements Appendable {
    private final Appendable sb;
    private final int max;
    private int added;

    LimitedBuffer(Appendable sb, int max) {
        this.sb = sb;
        this.max = max;
        added = 0;
    }

    public Appendable append(char c) throws IOException {
        if (added < max) {
            sb.append(c);
        }
        added++;
        return this;
    }

    public Appendable append(CharSequence csq) throws IOException {
        return append(csq, 0, csq.length());
    }

    public Appendable append(CharSequence csq, int start, int end)
            throws IOException {

        int remain = max - added;
        int requested = end - start;
        int toWrite = Math.min(requested, remain);
        sb.append(csq, start, start + toWrite);
        added += requested;
        return this;
    }

    int ignored() {
        if (added <= max) {
            return 0;
        } else {
            return added - max;
        }
    }
}