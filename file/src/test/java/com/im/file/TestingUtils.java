package com.im.file;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.util.Arrays;

public class TestingUtils {
    public static Object[][] allCombos(Object[]... params) {
        int total = 1;
        int[] strides = new int[params.length];
        for (Object[] param : params) {
            total *= param.length;
        }
        for (int i = 0; i < params.length; i++) {
            if (i == 0) {
                strides[i] = total / params[i].length;
            } else {
                strides[i] = strides[i - 1] / params[i].length;
            }
        }

        Object[][] tests = new Object[total][params.length];
        for (int p = 0; p < params.length; p++) {
            int t = 0;
            while (t < tests.length) {
                for (int i = 0; i < params[p].length; i++) {
                    Object val = params[p][i];
                    for (int j = 0; j < strides[p]; j++) {
                        tests[t][p] = val;
                        t++;
                    }
                }
            }
        }
        return tests;
    }

    public static Object[] splice(Object[]... params) {
        int total = 0;
        for (Object[] param : params) {
            total += param.length;
        }

        // Assumption -- the type of the first array is compatible with all other values in all other arrays
        Object[] all = Arrays.copyOf(params[0], total);

        int offset = 0;
        for (Object[] param : params) {
            System.arraycopy(param, 0, all, offset, param.length);
            offset += param.length;
        }
        return all;
    }

    @Test
    public void internalTestCombo() {
        Integer[] pos = {1, 2, 3};
        Integer[] neg = {-2, -1};
        Integer[] top = {8, 9};
        Integer[][] arrays = {pos, neg, top};

        Object[][] combined = allCombos(pos, neg, top);

        for (int i = 0; i < combined.length; i++) {
            System.out.printf("%2d:", i);
            for (int j = 0; j < combined[i].length; j++) {
                System.out.printf(" %2s", combined[i][j]);
            }
            System.out.printf("%n");
        }

        int[][] seen = new int[arrays.length][];
        for (int i = 0; i < arrays.length; i++) {
            seen[i] = new int[arrays[i].length];
        }

        assertEquals(combined.length, pos.length * neg.length * top.length);

        for (Object[] value : combined) {
            assertEquals(value.length, arrays.length);
            for (int i = 0; i < arrays.length; i++) {
                seen[i][Arrays.binarySearch(arrays[i], value[i])]++;
                assertSame(value[i].getClass(), Integer.class);
            }
        }

        for (int i = 0; i < arrays.length; i++) {
            for (int j = 0; j < seen[i].length; j++) {
                assertEquals(seen[i][j], combined.length / arrays[i].length,
                        "array " + i + " [" + j + "]");
            }
        }
    }

    @Test
    public void internalTestSplice() {
        Integer[] expected = {
                5, 4, 3, 2, 1
        };
        Object[] spliced = splice(new Integer[]{5}, new Integer[]{4, 3},
                new Integer[]{2, 1});
        assertTrue(Arrays.equals(expected, spliced));

        String[][] strExpected = {{"a", "b"}, {"y", "z"}};
        Object[][] strSpliced = (Object[][]) splice(new String[][]{{"a", "b"}},
                new String[][]{{"y", "z"}});
        assertTrue(Arrays.deepEquals(strExpected, strSpliced));
    }
}