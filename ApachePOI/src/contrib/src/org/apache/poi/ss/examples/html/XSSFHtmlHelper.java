package org.apache.poi.ss.examples.html;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
//import org.apache.poi.xssf.usermodel.XSSFCellStyle;
//import org.apache.poi.xssf.usermodel.XSSFColor;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Formatter;
import java.util.Hashtable;

public class XSSFHtmlHelper implements HtmlHelper {
    //private final XSSFWorkbook wb;

    private static final Hashtable colors = HSSFColor.getIndexHash();

   // // TODO: add back XML part of POI, and change back commented part
    public void colorStyles(CellStyle style, Formatter out) {
        // TODO Auto-generated method stub
        
    }

//    public XSSFHtmlHelper(XSSFWorkbook wb) {
//        //this.wb = wb;
//    }

//    public void colorStyles(CellStyle style, Formatter out) {
//       // XSSFCellStyle cs = (XSSFCellStyle) style;
//        styleColor(out, "background-color", cs.getFillForegroundXSSFColor());
//        styleColor(out, "text-color", cs.getFont().getXSSFColor());
//    }
//
//    private void styleColor(Formatter out, String attr, XSSFColor color) {
//        if (color == null || color.isAuto())
//            return;
//
//        byte[] rgb = color.getRgb();
//        if (rgb == null) {
//            return;
//        }
//
//        // This is done twice -- rgba is new with CSS 3, and browser that don't
//        // support it will ignore the rgba specification and stick with the
//        // solid color, which is declared first
//        out.format("  %s: #%02x%02x%02x;%n", attr, rgb[0], rgb[1], rgb[2]);
//        out.format("  %s: rgba(0x%02x, 0x%02x, 0x%02x, 0x%02x);%n", attr,
//                rgb[0], rgb[1], rgb[2], rgb[3]);
//    }
}