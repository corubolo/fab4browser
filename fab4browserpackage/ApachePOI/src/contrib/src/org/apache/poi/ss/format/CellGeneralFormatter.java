package org.apache.poi.ss.format;

import java.util.Formatter;

public class CellGeneralFormatter extends CellFormatter {
    public CellGeneralFormatter() {
        super("General");
    }

    public void formatValue(StringBuffer toAppendTo, Object value) {
        if (value instanceof Number) {
            double val = ((Number) value).doubleValue();
            if (val == 0) {
                toAppendTo.append('0');
                return;
            }

            String fmt;
            double exp = Math.log10(Math.abs(val));
            boolean stripZeros = true;
            if (exp > 10 || exp < -9)
                fmt = "%1.5E";
            else if ((long) val != val)
                fmt = "%1.9f";
            else {
                fmt = "%1.0f";
                stripZeros = false;
            }

            Formatter formatter = new Formatter(toAppendTo);
            formatter.format(LOCALE, fmt, value);
            if (stripZeros) {
                // strip off trailing zeros
                int removeFrom;
                if (fmt.endsWith("E"))
                    removeFrom = toAppendTo.lastIndexOf("E") - 1;
                else
                    removeFrom = toAppendTo.length() - 1;
                while (toAppendTo.charAt(removeFrom) == '0') {
                    toAppendTo.deleteCharAt(removeFrom--);
                }
                if (toAppendTo.charAt(removeFrom) == '.') {
                    toAppendTo.deleteCharAt(removeFrom--);
                }
            }
        } else {
            toAppendTo.append(value.toString());
        }
    }

    public void simpleValue(StringBuffer toAppendTo, Object value) {
        formatValue(toAppendTo, value);
    }
}