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

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * This class helps port C code to Java by implementing a pointer emulation for
 * a byte buffer.  Multiple pointer objects can refer to the same byte buffer,
 * and if they do, they can be compared, subtracted, etc.  Because bytes and
 * chars are interchangable in Java, this code allows chars and bytes to be
 * overloaded. Chars are assumed to be 8-bit ASCII (ISO Latin 1).
 * <p/>
 * Pointer objects have a position relative to their buffer.  This position is
 * typically within the byte array, but this is not checked, except when you
 * access bytes form within the array.  For example, you can position a Pointer
 * object to point at the position one beyond the end of the buffer -- in other
 * words, at the position that is {@code buf.length}.
 * <p/>
 * One important difference between this and actual C pointers is that they can
 * be modified when they are passed as a parameter.  Consider the following C
 * code:
 * <pre>
 * char *p = ...;
 * &nbsp;
 * char *xpos = findChar(p, 'x');
 * &nbsp;
 * public static char *findChar(char *p, char ch) {
 *     while (*p != ch)
 *         p++;
 *     return p;
 * }
 * </pre>
 * The naive translation into Java using the {@code Pointer} class is:
 * <pre>
 * Pointer p = ...;
 * &nbsp;
 * Pointer xpos = findChar(p, 'x');
 * &nbsp;
 * public static Pointer findChar(Pointer p, char ch) {
 *     while (p.get() != ch)
 *         p.incr();
 *     return p;
 * }
 * </pre>
 * In C this kind of operation is common.  But in Java, the method will modify
 * the actual value {@code p}.  That is, in Java, after calling {@code
 * findChar}, the value of {@code p} in the calling code will be modified to
 * point at the character that was found.  Further, the local variables {@code
 * p} and {@code xpos} will refer to the same underlying object -- a
 * modification of one will modify the object the other uses as well.
 * <p/>
 * The proper way to rewrite the C code is:
 * <pre>
 * public static Pointer findChar(Pointer pOrig, char ch) {
 *     Pointer p = pOrig.copy();
 *     while (p.get() != ch)
 *         p.incr();
 *     return p;
 * }
 * </pre>
 * Now the local code is modifying a copy, and returns that copy.
 */
public class Pointer implements Comparable<Pointer>, Serializable {
    public final byte[] bytes;
    public int pos;

    private static final long serialVersionUID = -5475903906491763718L;

    /**
     * Creates a new {@link Pointer} object that points to the start of the
     * given array of bytes. Analog to:
     * <pre>
     * char bytes[] = ...;
     * p = bytes;
     * </pre>
     *
     * @param bytes The bytes that are the pointer's buffer.
     */
    public Pointer(byte[] bytes) {
        this(bytes, 0);
    }

    /**
     * Creates a new {@link Pointer} object that points to a position in the
     * given array of bytes. Analog to:
     * <pre>
     * char bytes[] = ...;
     * p = &bytes[pos];
     * </pre>
     *
     * @param bytes The bytes that are the pointer's buffer.
     * @param pos   The position within that buffer.
     */
    public Pointer(byte[] bytes, int pos) {
        this.bytes = bytes;
        this.pos = pos;
    }

    /**
     * Creates a new {@link Pointer} object that points to the start of the
     * bytes extracted from the given string. Analog to:
     * <pre>
     * char *str = ...;
     * p = str;
     * </pre>
     *
     * @param str The string whose bytes will be extacted to create the
     *            pointer's buffer.
     */
    public Pointer(String str) {
        this(bytesForString(str));
    }

    /**
     * Creates a new {@link Pointer} object that points to a position in the
     * bytes extracted from the given string. Analog to:
     * <pre>
     * char *str = ...;
     * p = &str[offset];
     * </pre>
     *
     * @param str The string whose bytes will be extacted to create the
     *            pointer's buffer.
     * @param pos The position within that buffer.
     */
    public Pointer(String str, int pos) {
        this(bytesForString(str), pos);
    }

    /**
     * Creates a new {@link Pointer} object that is a copy of the given pointer.
     * Analog to:
     * <pre>
     * p = that;
     * </pre>
     *
     * @param that The pointer to copy.
     */
    public Pointer(Pointer that) {
        this(that, 0);
    }

    /**
     * Creates a new {@link Pointer} object that is a copy of the given pointer
     * plus an offset (which can be negative). Analog to:
     * <pre>
     * p = &that[offset];
     * </pre>
     *
     * @param that   The pointer to copy.
     * @param offset The offset to add.
     */
    public Pointer(Pointer that, int offset) {
        this(that.bytes, that.pos + offset);
    }

    private static byte[] bytesForString(String str) {
        // use a null-terminated set of bytes for the string
        int size = str.length() + 1;
        byte[] bytes = new byte[size];
        byte[] strBytes = str.getBytes(Charset.forName("Latin1"));
        System.arraycopy(strBytes, 0, bytes, 0, strBytes.length);
        bytes[strBytes.length] = 0;
        return bytes;
    }

    /**
     * Increments this pointer by one. Analog to:
     * <pre>
     * (void) p++;
     * </pre>
     */
    public void incr() {
        pos++;
    }

    /**
     * Increments this pointer by the given number of bytes (can be negative,
     * which will move the pointer backwards). Analog to:
     * <pre>
     * (void) p += amount;
     * </pre>
     *
     * @param amount The number of bytes to move this pointer.
     */
    public void incr(int amount) {
        pos += amount;
    }

    /**
     * Decrements this pointer by one. Analog to:
     * <pre>
     * (void) p--;
     * </pre>
     */
    public void decr() {
        pos--;
    }

    /**
     * Decrements this pointer by the given number of bytes (can be negative,
     * which will move the pointer forwards). Analog to:
     * <pre>
     * (void) p -= amount;
     * </pre>
     *
     * @param amount The number of bytes to move this pointer.
     */
    public void decr(int amount) {
        pos -= amount;
    }

    /**
     * Returns the byte at the current position. Analog to:
     * <pre>
     * *p
     * </pre>
     *
     * @return The byte at the current position.
     */
    public byte get() {
        return bytes[pos];
    }

    /**
     * Returns the byte at the given offset from this pointer. Analog to:
     * <pre>
     * p[offset]
     * </pre>
     *
     * @param offset The offset (can be negative).
     *
     * @return The byte at the given offset from this pointer.
     */
    public byte get(int offset) {
        return bytes[pos + offset];
    }

    /**
     * Returns the byte at the current position, incrementing this pointer
     * afterwards. Analog to:
     * <pre>
     * *p++
     * </pre>
     *
     * @return The byte at the current position.
     */
    public byte getIncr() {
        try {
            return get();
        } finally {
            incr();
        }
    }

    /**
     * Increments the current position, then returns the byte at the new
     * position. Analog to:
     * <pre>
     * *++p
     * </pre>
     *
     * @return The byte at the new position.
     */
    public byte getPreIncr() {
        incr();
        return get();
    }

    /**
     * Sets the byte at the current position. Analog to:
     * <pre>
     * *p = b
     * </pre>
     *
     * @param b The new value.
     *
     * @return The new value.
     */
    public byte set(byte b) {
        return bytes[pos] = b;
    }

    /**
     * Sets the byte at the current position. Analog to:
     * <pre>
     * *p = (char) b
     * </pre>
     *
     * @param b The new value.
     *
     * @return The new value.
     */
    public byte set(int b) {
        return set((byte) b);
    }

    /**
     * Sets the byte at the current position, incrementing this pointer
     * afterwards. Analog to:
     * <pre>
     * *p++ = b
     * </pre>
     *
     * @param b The new value.
     *
     * @return The new value.
     */
    public byte setIncr(byte b) {
        try {
            return set(b);
        } finally {
            incr();
        }
    }

    /**
     * Sets the byte at the current position, incrementing this pointer
     * afterwards. Analog to:
     * <pre>
     * *p++ = (char) b
     * </pre>
     *
     * @param b The new value.
     *
     * @return The new value.
     */
    public byte setIncr(int b) {
        return setIncr((byte) b);
    }

    /**
     * Increments the current position, then sets the byte at the new position.
     * Analog to:
     * <pre>
     * *++p = b
     * </pre>
     *
     * @param b The new value.
     *
     * @return The new value.
     */
    public byte setPreIncr(byte b) {
        incr();
        return set(b);
    }

    /**
     * Increments the current position, then sets the byte at the new position.
     * Analog to:
     * <pre>
     * *++p = (char) b
     * </pre>
     *
     * @param b The new value.
     *
     * @return The new value.
     */
    public byte setPreIncr(int b) {
        return setPreIncr((byte) b);
    }

    /**
     * Sets the byte at the given offset from this pointer. Analog to:
     * <pre>
     * p[offset] = b
     * </pre>
     *
     * @param offset The offset (can be negative).
     * @param b      The new value.
     *
     * @return The new value.
     */
    public byte set(int offset, byte b) {
        return bytes[pos + offset] = b;
    }

    /**
     * Sets the byte at the given offset from this pointer. Analog to:
     * <pre>
     * p[offset] = (char) b
     * </pre>
     *
     * @param offset The offset (can be negative).
     * @param b      The new value.
     *
     * @return The new value.
     */
    public byte set(int offset, int b) {
        return set(offset, (byte) b);
    }

    /**
     * Returns a new pointer that is a copy of this pointer. Analog to:
     * <pre>
     * p
     * </pre>
     *
     * @return A copy of this pointer.
     */
    public Pointer copy() {
        return plus(0);
    }

    /**
     * Returns a new pointer that is offset from this pointer by the given
     * amount. Analog to:
     * <pre>
     * p + offset
     * </pre>
     *
     * @param offset The offset of the new pointer from this one (can be
     *               negative).
     *
     * @return A copy of this pointer.
     */
    public Pointer plus(int offset) {
        return new Pointer(this, offset);
    }

    /**
     * Returns the difference between the pointer in bytes.  If {@code that}
     * pointer references a byte before this one, the result will be negative.
     * The two pointers must refer to the same buffer. Analog to:
     * <pre>
     * p - that
     * </pre>
     *
     * @param that The pointer to subtract from this one.
     *
     * @return The difference between the two pointers.
     *
     * @throws IllegalArgumentException The two pointers refer to different
     *                                  buffers.
     */
    public int minus(Pointer that) {
        checkSameBuf(that);
        return pos - that.pos;
    }

    private void checkSameBuf(Pointer that) {
        if (bytes != that.bytes) {
            throw new IllegalArgumentException(
                    "Operation valid only on pointers to the same bytes");
        }
    }

    /**
     * Returns {@code true} if the other object is a {@code Pointer} that refers
     * to the same position in the same buffer. Analog to:
     * <pre>
     * p == that
     * </pre>
     *
     * @param o The other object.
     *
     * @return {@code true} if the other object is a {@code Pointer} that refers
     *         to the same position in the same buffer.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Pointer) {
            try {
                return compareTo((Pointer) o) == 0;
            } catch (IllegalArgumentException ignored) {
                return false;   // must be different buffers
            }
        }
        return false;
    }

    /**
     * Returns a hash code for this pointer.
     *
     * @return A hash code for this pointer.
     */
    @Override
    public int hashCode() {
        return bytes.hashCode() + pos;
    }

    /**
     * Returns a value that is less then, equal to, or greater than zero as the
     * other pointer has a position in the buffer that is less than, equal to,
     * or greater than this one's.
     *
     * @param that The other object.
     *
     * @return A comparison value.
     *
     * @throws IllegalArgumentException The two pointers refer to different
     *                                  buffers.
     */
    public int compareTo(Pointer that) {
        checkSameBuf(that);
        return pos - that.pos;
    }

    /**
     * Returns the length of the null-terminated string this pointer refers to.
     *
     * @return The length of the null-terminated string this pointer refers to.
     */
    public int strlen() {
        int len = 0;
        for (int i = pos; bytes[i] != '\0'; i++) {
            len++;
        }
        return len;
    }

    /**
     * Returns the null-terminated string this pointer refers to.
     *
     * @return The null-terminated string this pointer refers to.
     */
    @Override
    public String toString() {
        return new String(bytes, pos, strlen());
    }

    /**
     * Returns the length of the string that is made up entirely of characters
     * from {@code s2}.
     *
     * @param s2 The characters to compute the span from.
     *
     * @return The length of the string that is made up entirely of characters
     *         from {@code s2}.
     */
    public int strcspn(String s2) {
        for (int i = pos; bytes[i] != '\0'; i++) {
            if (s2.indexOf(toChar(bytes[pos])) >= 0) {
                return i - pos + 1;
            }
        }
        return 0;
    }

    /**
     * Returns a pointer to the first instance of the given character in the
     * string that is refered to by this pointer.  Returns {@code null} if the
     * charater does not exist.
     *
     * @param c The character to search for.
     *
     * @return A pointer to the first instance of the character.
     */
    public Pointer strchr(byte c) {
        for (int i = pos; bytes[i] != '\0'; i++) {
            if (bytes[i] == c) {
                return plus(i);
            }
        }
        return null;
    }

    /**
     * Returns a value that is less then, equal to, or greater than zero as the
     * null-terminated string this ponter refers to is less then, equal to, or
     * greater than that of {code s2}.  This uses simple ascii order.
     *
     * @param s2 The string to compare to.
     *
     * @return A value that can be used to sort the two strings.
     */
    public int strcmp(Pointer s2) {
        int i1 = pos;
        int i2 = s2.pos;
        while (bytes[i1] == s2.bytes[i2]) {
            if (bytes[i1] == '\0') {
                return 0;
            }
            i1++;
            i2++;
        }
        return bytes[i1] - s2.bytes[i2];
    }

    /**
     * Over a given maximum length, returns a value that is less then, equal to,
     * or greater than zero as the null-terminated string this ponter refers to
     * is less then, equal to, or greater than that of {code s2}.  This uses
     * simple ascii order.
     *
     * @param s2  The string to compare to.
     * @param len The maximum length to compare.
     *
     * @return A value that can be used to sort the two strings over the given
     *         length.
     */
    public int strncmp(Pointer s2, int len) {
        int i1 = pos;
        int i2 = s2.pos;
        for (int i = 0; i < len && bytes[i1] == s2.bytes[i2]; i++) {
            if (bytes[i1] == '\0') {
                return 0;
            }
            i1++;
            i2++;
        }
        return bytes[i1] - s2.bytes[i2];
    }

    /**
     * Copy the string from the other string to this one.
     *
     * @param from The source string.
     */
    public void strcpy(Pointer from) {
        Pointer src = from.copy();
        Pointer dest = copy();
        while (dest.setIncr(src.getIncr()) != '\0') {
            continue;
        }
    }

    /**
     * Copy {@code len} bytes from another pointer to this one.  The other
     * pointer need not be in the same buffer.  If they are, and the source and
     * destination areas overlap, the copy works properly -- that is, it works
     * as if the bytes were first copied to a temporary buffer, and then to the
     * destination.
     *
     * @param pointer The pointer to the other buffer.
     * @param len     The number of bytes to copy.
     */
    public void copyFrom(Pointer pointer, int len) {
        System.arraycopy(pointer.bytes, 0, bytes, pos, len);
    }

    /**
     * Convert a {@code byte} to a {@code char} without sign-extending the byte.
     * This is a utility function and is made public so you can use it in your
     * own code where helpful.
     *
     * @param b The byte to convert.
     *
     * @return The byte with a zero upper-byte to make it a {@code char}.
     */
    public static char toChar(byte b) {
        if (b < 0x7f) {
            return (char) b;
        }
        return (char) (((short) b) & 0xff);
    }
}