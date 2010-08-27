package org.apache.poi.ss.format;

import org.apache.poi.ss.format.CellFormat;

import javax.swing.*;

public class CellFormatTest extends CellFormatTestBase {
    public void testSome() {
        JLabel l = new JLabel();
        CellFormat fmt = CellFormat.getInstance(
                "\"$\"#,##0.00_);[Red]\\(\"$\"#,##0.00\\)");
        fmt.apply(l, 1.1);
    }
}