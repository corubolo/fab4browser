package org.apache.poi.ss.format;

import junit.framework.TestCase;
import org.apache.poi.ss.format.CellFormatCondition;

public class CellFormatConditionTest extends TestCase {
    public void testSVConditions() {
        CellFormatCondition lt = CellFormatCondition.getInstance("<", "1.5");
        assertTrue(lt.pass(1.4));
        assertFalse(lt.pass(1.5));
        assertFalse(lt.pass(1.6));

        CellFormatCondition le = CellFormatCondition.getInstance("<=", "1.5");
        assertTrue(le.pass(1.4));
        assertTrue(le.pass(1.5));
        assertFalse(le.pass(1.6));

        CellFormatCondition gt = CellFormatCondition.getInstance(">", "1.5");
        assertFalse(gt.pass(1.4));
        assertFalse(gt.pass(1.5));
        assertTrue(gt.pass(1.6));

        CellFormatCondition ge = CellFormatCondition.getInstance(">=", "1.5");
        assertFalse(ge.pass(1.4));
        assertTrue(ge.pass(1.5));
        assertTrue(ge.pass(1.6));

        CellFormatCondition eqs = CellFormatCondition.getInstance("=", "1.5");
        assertFalse(eqs.pass(1.4));
        assertTrue(eqs.pass(1.5));
        assertFalse(eqs.pass(1.6));

        CellFormatCondition eql = CellFormatCondition.getInstance("==", "1.5");
        assertFalse(eql.pass(1.4));
        assertTrue(eql.pass(1.5));
        assertFalse(eql.pass(1.6));

        CellFormatCondition neo = CellFormatCondition.getInstance("<>", "1.5");
        assertTrue(neo.pass(1.4));
        assertFalse(neo.pass(1.5));
        assertTrue(neo.pass(1.6));

        CellFormatCondition nen = CellFormatCondition.getInstance("!=", "1.5");
        assertTrue(nen.pass(1.4));
        assertFalse(nen.pass(1.5));
        assertTrue(nen.pass(1.6));
    }
}