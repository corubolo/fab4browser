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

import java.lang.reflect.Array;
import java.util.Formattable;
import java.util.Formatter;

class PositionRecorder implements Formattable {
    private final Object array;
    private final String modifier;

    PositionRecorder(Object array, String modifier) {
        this.array = array;
        this.modifier = modifier;
    }

    @Override
    public String toString() {
        return array.toString();
    }

    public void formatTo(Formatter formatter, int flags, int width,
            int precision) {

        int pos = formatter.toString().length();

        //noinspection OverlyBroadCatchBlock
        try {
            if (modifier.length() == 1) {
                switch (modifier.charAt(0)) {
                case 'h':
                    Array.setShort(array, 0, (short) pos);
                    break;
                case 'l':
                case 'j':
                case 't':
                case 'z':
                    Array.setInt(array, 0, pos);
                    break;
                case 'q':
                    Array.setLong(array, 0, pos);
                    break;
                }
            } else if (modifier.equals("hh")) {
                Array.setByte(array, 0, (byte) pos);
            } else if (modifier.equals("ll")) {
                Array.setLong(array, 0, pos);
            } else {
                Array.setInt(array, 0, pos);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    "Argument for '%n' must be array with at least one element",
                    e);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Argument for '%n' must be array compatible with the specification",
                    e);
        }

        // All width, precision, flags, etc. are ignored in C printf
        // %n is always a zero-width output
        // That is, to format this is to add nothing to the output at all
    }
}