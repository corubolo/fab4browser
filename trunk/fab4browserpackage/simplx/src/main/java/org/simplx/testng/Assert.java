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

package org.simplx.testng;

import java.util.Arrays;

/**
 * Adds more assertions to those in TestNG's {@link org.testng.Assert} class.
 * The assertions added cover primitive arrays to make them as easy to check as
 * object arrays.
 */
@SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass",
        "ClassNameSameAsAncestorName"})
public class Assert extends org.testng.Assert {
    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEquals(boolean[] actual, boolean[] expected) {
        assertEquals(actual, expected, "boolean[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown, with the given
     * message.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEquals(boolean[] actual, boolean[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else if (expected == null) {
            assertNull(actual, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            for (int i = 0; i < actual.length; i++) {
                assertEquals(actual[i], expected[i], message + " [" + i + "]");
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEqualsNoOrder(boolean[] actual,
            boolean[] expected) {
        assertEqualsNoOrder(actual, expected, "boolean[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError}, with the given message, is
     * thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEqualsNoOrder(boolean[] actual, boolean[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            int actTrues = 0;
            int expTrues = 0;
            for (int i = 0; i < actual.length; i++) {
                if (actual[i])
                    actTrues++;
                if (expected[i])
                    expTrues++;
            }
            assertEquals(actTrues, expTrues, message);
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEquals(char[] actual, char[] expected) {
        assertEquals(actual, expected, "char[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown, with the given
     * message.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEquals(char[] actual, char[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else if (expected == null) {
            assertNull(actual, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            for (int i = 0; i < actual.length; i++) {
                assertEquals(actual[i], expected[i], message + " [" + i + "]");
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEqualsNoOrder(char[] actual, char[] expected) {
        assertEqualsNoOrder(actual, expected, "char[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError}, with the given message, is
     * thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEqualsNoOrder(char[] actual, char[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            char[] sortedActual = actual.clone();
            char[] sortedExpected = expected.clone();
            Arrays.sort(sortedActual);
            Arrays.sort(sortedExpected);
            for (int i = 0; i < actual.length; i++) {
                assertEquals(sortedActual[i], sortedExpected[i], message);
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEquals(byte[] actual, byte[] expected) {
        assertEquals(actual, expected, "byte[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown, with the given
     * message.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEquals(byte[] actual, byte[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else if (expected == null) {
            assertNull(actual, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            for (int i = 0; i < actual.length; i++) {
                assertEquals(actual[i], expected[i], message + " [" + i + "]");
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEqualsNoOrder(byte[] actual, byte[] expected) {
        assertEqualsNoOrder(actual, expected, "byte[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError}, with the given message, is
     * thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEqualsNoOrder(byte[] actual, byte[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            byte[] sortedActual = actual.clone();
            byte[] sortedExpected = expected.clone();
            Arrays.sort(sortedActual);
            Arrays.sort(sortedExpected);
            for (int i = 0; i < actual.length; i++) {
                assertEquals(sortedActual[i], sortedExpected[i], message);
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEquals(short[] actual, short[] expected) {
        assertEquals(actual, expected, "short[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown, with the given
     * message.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEquals(short[] actual, short[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else if (expected == null) {
            assertNull(actual, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            for (int i = 0; i < actual.length; i++) {
                assertEquals(actual[i], expected[i], message + " [" + i + "]");
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEqualsNoOrder(short[] actual, short[] expected) {
        assertEqualsNoOrder(actual, expected, "short[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError}, with the given message, is
     * thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEqualsNoOrder(short[] actual, short[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            short[] sortedActual = actual.clone();
            short[] sortedExpected = expected.clone();
            Arrays.sort(sortedActual);
            Arrays.sort(sortedExpected);
            for (int i = 0; i < actual.length; i++) {
                assertEquals(sortedActual[i], sortedExpected[i], message);
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEquals(int[] actual, int[] expected) {
        assertEquals(actual, expected, "int[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown, with the given
     * message.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEquals(int[] actual, int[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else if (expected == null) {
            assertNull(actual, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            for (int i = 0; i < actual.length; i++) {
                assertEquals(actual[i], expected[i], message + " [" + i + "]");
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEqualsNoOrder(int[] actual, int[] expected) {
        assertEqualsNoOrder(actual, expected, "int[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError}, with the given message, is
     * thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEqualsNoOrder(int[] actual, int[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            int[] sortedActual = actual.clone();
            int[] sortedExpected = expected.clone();
            Arrays.sort(sortedActual);
            Arrays.sort(sortedExpected);
            for (int i = 0; i < actual.length; i++) {
                assertEquals(sortedActual[i], sortedExpected[i], message);
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEquals(long[] actual, long[] expected) {
        assertEquals(actual, expected, "long[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown, with the given
     * message.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEquals(long[] actual, long[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else if (expected == null) {
            assertNull(actual, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            for (int i = 0; i < actual.length; i++) {
                assertEquals(actual[i], expected[i], message + " [" + i + "]");
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEqualsNoOrder(long[] actual, long[] expected) {
        assertEqualsNoOrder(actual, expected, "long[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError}, with the given message, is
     * thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEqualsNoOrder(long[] actual, long[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            long[] sortedActual = actual.clone();
            long[] sortedExpected = expected.clone();
            Arrays.sort(sortedActual);
            Arrays.sort(sortedExpected);
            for (int i = 0; i < actual.length; i++) {
                assertEquals(sortedActual[i], sortedExpected[i], message);
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEquals(float[] actual, float[] expected) {
        assertEquals(actual, expected, "float[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown, with the given
     * message.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEquals(float[] actual, float[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else if (expected == null) {
            assertNull(actual, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            for (int i = 0; i < actual.length; i++) {
                assertEquals(actual[i], expected[i], message + " [" + i + "]");
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEqualsNoOrder(float[] actual, float[] expected) {
        assertEqualsNoOrder(actual, expected, "float[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError}, with the given message, is
     * thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEqualsNoOrder(float[] actual, float[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            float[] sortedActual = actual.clone();
            float[] sortedExpected = expected.clone();
            Arrays.sort(sortedActual);
            Arrays.sort(sortedExpected);
            for (int i = 0; i < actual.length; i++) {
                assertEquals(sortedActual[i], sortedExpected[i], message);
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEquals(double[] actual, double[] expected) {
        assertEquals(actual, expected, "double[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown, with the given
     * message.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEquals(double[] actual, double[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else if (expected == null) {
            assertNull(actual, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            for (int i = 0; i < actual.length; i++) {
                assertEquals(actual[i], expected[i], message + " [" + i + "]");
            }
        }
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError} is thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     */
    public static void assertEqualsNoOrder(double[] actual, double[] expected) {
        assertEqualsNoOrder(actual, expected, "double[] array");
    }

    /**
     * Asserts that two arrays contain the same elements in the same order. If
     * they do not, an {@link AssertionError}, with the given message, is
     * thrown.
     *
     * @param actual   The actual value.
     * @param expected The expected value.
     * @param message  The assertion error message.
     */
    public static void assertEqualsNoOrder(double[] actual, double[] expected,
            String message) {
        if (actual == null) {
            assertNull(expected, message + ": both null");
        } else {
            assertEquals(actual.length, expected.length, message + ": length");
            double[] sortedActual = actual.clone();
            double[] sortedExpected = expected.clone();
            Arrays.sort(sortedActual);
            Arrays.sort(sortedExpected);
            for (int i = 0; i < actual.length; i++) {
                assertEquals(sortedActual[i], sortedExpected[i], message);
            }
        }
    }
}
