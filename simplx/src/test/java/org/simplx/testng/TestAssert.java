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

import org.testng.annotations.Test;

import static org.simplx.testng.Assert.*;

@SuppressWarnings({"ErrorNotRethrown"})
public class TestAssert {
    @Test
    public void testBooleanArray() {
        boolean[] exp = null;
        boolean[] act = null;
        assertEquals(exp, act);
        try {
            assertEquals(exp, new boolean[0]);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        try {
            assertEquals(new boolean[0], null);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        exp = new boolean[]{true, true, false};
        act = new boolean[]{true, true, false};
        assertEquals(exp, act);
        act = new boolean[]{true, false, true};
        try {
            assertEquals(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
        assertEqualsNoOrder(exp, act);
        act = new boolean[]{false, false, true};
        try {
            assertEquals(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
        try {
            assertEqualsNoOrder(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
    }

    @Test
    public void testCharArray() {
        char[] exp = null;
        char[] act = null;
        assertEquals(exp, act);
        try {
            assertEquals(exp, new char[0]);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        try {
            assertEquals(new char[0], null);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        exp = new char[]{1, 2, 3};
        act = new char[]{1, 2, 3};
        assertEquals(exp, act);
        act = new char[]{3, 2, 1};
        try {
            assertEquals(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
        assertEqualsNoOrder(exp, act);
    }

    @Test
    public void testByteArray() {
        byte[] exp = null;
        byte[] act = null;
        assertEquals(exp, act);
        try {
            assertEquals(exp, new byte[0]);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        try {
            assertEquals(new byte[0], null);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        exp = new byte[]{1, 2, 3};
        act = new byte[]{1, 2, 3};
        assertEquals(exp, act);
        act = new byte[]{3, 2, 1};
        try {
            assertEquals(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
        assertEqualsNoOrder(exp, act);
    }

    @Test
    public void testShortArray() {
        short[] exp = null;
        short[] act = null;
        assertEquals(exp, act);
        try {
            assertEquals(exp, new short[0]);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        try {
            assertEquals(new short[0], null);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        exp = new short[]{1, 2, 3};
        act = new short[]{1, 2, 3};
        assertEquals(exp, act);
        act = new short[]{3, 2, 1};
        try {
            assertEquals(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
        assertEqualsNoOrder(exp, act);
    }

    @Test
    public void testIntArray() {
        int[] exp = null;
        int[] act = null;
        assertEquals(exp, act);
        try {
            assertEquals(exp, new int[0]);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        try {
            assertEquals(new int[0], null);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        exp = new int[]{1, 2, 3};
        act = new int[]{1, 2, 3};
        assertEquals(exp, act);
        act = new int[]{3, 2, 1};
        try {
            assertEquals(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
        assertEqualsNoOrder(exp, act);
    }

    @Test
    public void testLongArray() {
        long[] exp = null;
        long[] act = null;
        assertEquals(exp, act);
        try {
            assertEquals(exp, new long[0]);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        try {
            assertEquals(new long[0], null);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        exp = new long[]{1, 2, 3};
        act = new long[]{1, 2, 3};
        assertEquals(exp, act);
        act = new long[]{3, 2, 1};
        try {
            assertEquals(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
        assertEqualsNoOrder(exp, act);
    }

    @Test
    public void testFlaotArray() {
        float[] exp = null;
        float[] act = null;
        assertEquals(exp, act);
        try {
            assertEquals(exp, new float[0]);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        try {
            assertEquals(new float[0], null);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        exp = new float[]{1, 2, 3};
        act = new float[]{1, 2, 3};
        assertEquals(exp, act);
        act = new float[]{3, 2, 1};
        try {
            assertEquals(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
        assertEqualsNoOrder(exp, act);
    }

    @Test
    public void testDoubleArray() {
        double[] exp = null;
        double[] act = null;
        assertEquals(exp, act);
        try {
            assertEquals(exp, new double[0]);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        try {
            assertEquals(new double[0], null);
            fail("allowed null vs. not null");
        } catch (AssertionError ignored) {
        }
        exp = new double[]{1, 2, 3};
        act = new double[]{1, 2, 3};
        assertEquals(exp, act);
        act = new double[]{3, 2, 1};
        try {
            assertEquals(exp, act);
            fail("allowed differently ordered");
        } catch (AssertionError ignored) {
        }
        assertEqualsNoOrder(exp, act);
    }
}
