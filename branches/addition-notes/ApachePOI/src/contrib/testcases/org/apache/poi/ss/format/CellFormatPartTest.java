package org.apache.poi.ss.format;

import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.format.CellFormatPart;
import org.apache.poi.ss.usermodel.Cell;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Test the individual CellFormatPart types. */
public class CellFormatPartTest extends CellFormatTestBase {
    private static final Pattern NUMBER_EXTRACT_FMT = Pattern.compile(
            "([-+]?[0-9]+)(\\.[0-9]+)?.*(?:(e).*?([+-]?[0-9]+))",
            Pattern.CASE_INSENSITIVE);

    public void testGeneralFormat() throws Exception {
        runFormatTests("GeneralFormatTests.xlsx", new CellValue() {
            public Object getValue(Cell cell) {
                int type = CellFormat.ultimateType(cell);
                if (type == Cell.CELL_TYPE_BOOLEAN)
                    return cell.getBooleanCellValue() ? "TRUE" : "FALSE";
                else if (type == Cell.CELL_TYPE_NUMERIC)
                    return cell.getNumericCellValue();
                else
                    return cell.getStringCellValue();
            }
        });
    }

    public void testNumberFormat() throws Exception {
        runFormatTests("NumberFormatTests.xlsx", new CellValue() {
            public Object getValue(Cell cell) {
                return cell.getNumericCellValue();
            }
        });
    }

    public void testNumberApproxFormat() throws Exception {
        runFormatTests("NumberFormatApproxTests.xlsx", new CellValue() {
            public Object getValue(Cell cell) {
                return cell.getNumericCellValue();
            }

            @Override
            void equivalent(String expected, String actual,
                    CellFormatPart format) {
                double expectedVal = extractNumber(expected);
                double actualVal = extractNumber(actual);
                // equal within 1%
                double delta = expectedVal / 100;
                assertEquals("format \"" + format + "\"," + expected + " ~= " +
                        actual, expectedVal, actualVal, delta);
            }
        });
    }

    public void testDateFormat() throws Exception {
        runFormatTests("DateFormatTests.xlsx", new CellValue() {
            public Object getValue(Cell cell) {
                return cell.getDateCellValue();
            }
        });
    }

    public void testElapsedFormat() throws Exception {
        runFormatTests("ElapsedFormatTests.xlsx", new CellValue() {
            public Object getValue(Cell cell) {
                return cell.getNumericCellValue();
            }
        });
    }

    public void testTextFormat() throws Exception {
        runFormatTests("TextFormatTests.xlsx", new CellValue() {
            public Object getValue(Cell cell) {
                if (CellFormat.ultimateType(cell) == Cell.CELL_TYPE_BOOLEAN)
                    return cell.getBooleanCellValue() ? "TRUE" : "FALSE";
                else
                    return cell.getStringCellValue();
            }
        });
    }

    public void testConditions() throws Exception {
        runFormatTests("FormatConditionTests.xlsx", new CellValue() {
            Object getValue(Cell cell) {
                return cell.getNumericCellValue();
            }
        });
    }

    private double extractNumber(String str) {
        Matcher m = NUMBER_EXTRACT_FMT.matcher(str);
        if (!m.find())
            throw new IllegalArgumentException(
                    "Cannot find numer in \"" + str + "\"");

        StringBuffer sb = new StringBuffer();
        // The groups in the pattern are the parts of the number
        for (int i = 1; i <= m.groupCount(); i++) {
            String part = m.group(i);
            if (part != null)
                sb.append(part);
        }
        return Double.valueOf(sb.toString());
    }
}