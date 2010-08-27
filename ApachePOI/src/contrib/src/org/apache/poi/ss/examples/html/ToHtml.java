package org.apache.poi.ss.examples.html;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.format.CellFormatResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.apache.poi.ss.usermodel.CellStyle.*;

public class ToHtml {
    private final Workbook wb;
    private final Appendable output;
    private boolean completeHTML;
    private Formatter out;
    private boolean gotBounds;
    private int firstColumn;
    private int endColumn;
    private HtmlHelper helper;

    private static final String DEFAULTS_CLASS = "excelDefaults";
    private static final String COL_HEAD_CLASS = "colHeader";
    private static final String ROW_HEAD_CLASS = "rowHeader";

    private static final Map<Short, String> ALIGN = mapFor(ALIGN_LEFT, "left",
            ALIGN_CENTER, "center", ALIGN_RIGHT, "right", ALIGN_FILL, "left",
            ALIGN_JUSTIFY, "left", ALIGN_CENTER_SELECTION, "center");

    private static final Map<Short, String> VERTICAL_ALIGN = mapFor(
            VERTICAL_BOTTOM, "bottom", VERTICAL_CENTER, "middle", VERTICAL_TOP,
            "top");

    private static final Map<Short, String> BORDER = mapFor(BORDER_DASH_DOT,
            "dashed 1pt", BORDER_DASH_DOT_DOT, "dashed 1pt", BORDER_DASHED,
            "dashed 1pt", BORDER_DOTTED, "dotted 1pt", BORDER_DOUBLE,
            "double 3pt", BORDER_HAIR, "solid 1px", BORDER_MEDIUM, "solid 2pt",
            BORDER_MEDIUM_DASH_DOT, "dashed 2pt", BORDER_MEDIUM_DASH_DOT_DOT,
            "dashed 2pt", BORDER_MEDIUM_DASHED, "dashed 2pt", BORDER_NONE,
            "none", BORDER_SLANTED_DASH_DOT, "dashed 2pt", BORDER_THICK,
            "solid 3pt", BORDER_THIN, "dashed 1pt");
    
    private final String css = "    .excelDefaults {\n" + 
    		"        background-color: white;\n" + 
    		"        color: black;\n" + 
    		"        text-decoration: none;\n" + 
    		"        direction: ltr;\n" + 
    		"        text-transform: none;\n" + 
    		"        text-indent: 0;\n" + 
    		"        letter-spacing: 0;\n" + 
    		"        word-spacing: 0;\n" + 
    		"        white-space: normal;\n" + 
    		"        unicode-bidi: normal;\n" + 
    		"        vertical-align: 0;\n" + 
    		"        background-image: none;\n" + 
    		"        text-shadow: none;\n" + 
    		"        list-style-image: none;\n" + 
    		"        list-style-type: none;\n" + 
    		"        padding: 0;\n" + 
    		"        margin: 0;\n" + 
    		"        border-collapse: collapse;\n" + 
    		"        white-space: pre;\n" + 
    		"        vertical-align: bottom;\n" + 
    		"        font-style: normal;\n" + 
    		"        font-family: sans-serif;\n" + 
    		"        font-variant: normal;\n" + 
    		"        font-weight: normal;\n" + 
    		"        font-size: 10pt;\n" + 
    		"        text-align: right;\n" + 
    		"}\n" + 
    		"\n" + 
    		".excelDefaults td {\n" + 
    		"        padding: 1px 5px;\n" + 
    		"        border: 1px solid silver;\n" + 
    		"}\n" + 
    		"\n" + 
    		".excelDefaults .colHeader {\n" + 
    		"        background-color: silver;\n" + 
    		"        font-weight: bold;\n" + 
    		"        border: 1px solid black;\n" + 
    		"        text-align: center;\n" + 
    		"        padding: 1px 5px;\n" + 
    		"}\n" + 
    		"\n" + 
    		".excelDefaults .rowHeader {\n" + 
    		"        background-color: silver;\n" + 
    		"        font-weight: bold;\n" + 
    		"        border: 1px solid black;\n" + 
    		"        text-align: right;\n" + 
    		"        padding: 1px 5px;\n" + 
    		"}";



    @SuppressWarnings({"unchecked"})
    private static <K, V> Map<K, V> mapFor(Object... mapping) {
        Map<K, V> map = new HashMap<K, V>();
        for (int i = 0; i < mapping.length; i += 2) {
            map.put((K) mapping[i], (V) mapping[i + 1]);
        }
        return map;
    }


    public static ToHtml create(Workbook wb, Appendable output) {
        return new ToHtml(wb, output);
    }

    public static ToHtml create(String path, Appendable output)
            throws IOException {

        InputStream in = new FileInputStream(path);
        if (path.endsWith(".xlsx"))
            return createXSSF(in, output);
        else
            return createHSSF(in, output);
    }

    public static ToHtml create(InputStream in, Appendable output)
            throws IOException {

        if (!in.markSupported())
            throw new IllegalArgumentException("!in.markSupported()");

        in.mark(1);
        boolean isXml = (in.read() == '<');
        in.reset();

        if (isXml)
            return createXSSF(in, output);
        else
            return createHSSF(in, output);
    }

    public static ToHtml createXSSF(InputStream in, Appendable output)
            throws IOException {
        // TODO: add back XML part of POI, and change back commented part
        return create(new HSSFWorkbook(in), output);
        //return create(new XSSFWorkbook(in), output);
    }

    public static ToHtml createHSSF(InputStream in, Appendable output)
            throws IOException {

        return create(new HSSFWorkbook(in), output);
    }

    private ToHtml(Workbook wb, Appendable output) {
        if (wb == null)
            throw new NullPointerException("wb");
        if (output == null)
            throw new NullPointerException("output");
        this.wb = wb;
        this.output = output;
        setupColorMap();
    }

    private void setupColorMap() {
        if (wb instanceof HSSFWorkbook)
            helper = new HSSFHtmlHelper((HSSFWorkbook) wb);
     // TODO: add back XML part of POI, and change back commented part
     //   else if (wb instanceof XSSFWorkbook)
       //     helper = new XSSFHtmlHelper((XSSFWorkbook) wb);
        else
            throw new IllegalArgumentException(
                    "unknown workbook type: " + wb.getClass().getSimpleName());
    }

    /**
     * Run this class as a program
     *
     * @param args The command line arguments.
     *
     * @throws Exception Exception we don't recover from.
     */
    public static void main(String[] args) throws Exception {
        ToHtml toHtml = create(args[0], System.out);
        toHtml.setCompleteHTML(true);
        toHtml.printStyles();

        toHtml = create(args[0], new PrintWriter(new FileWriter(
                "/tmp/k.html")));
        toHtml.setCompleteHTML(true);
        toHtml.printPage();
    }

    public void setCompleteHTML(boolean completeHTML) {
        this.completeHTML = completeHTML;
    }

    public void printPage() throws IOException {
        try {
            ensureOut();
            if (completeHTML) {
                out.format(
                        "<?xml version=\"1.0\" encoding=\"iso-8859-1\" ?>%n");
                out.format("<html>%n");
                out.format("<head>%n");
                out.format("</head>%n");
                out.format("<body>%n");
            }

            print();

            if (completeHTML) {
                out.format("</body>%n");
                out.format("</html>%n");
            }
        } finally {
            if (out != null)
                out.close();
            if (output instanceof Closeable) {
                Closeable closeable = (Closeable) output;
                closeable.close();
            }
        }
    }

    public void print() {
        printInlineStyle();
        printSheets();
    }

    private void printInlineStyle() {
        //out.format("<link href=\"excelStyle.css\" rel=\"stylesheet\" type=\"text/css\">%n");
        out.format("<style type=\"text/css\">%n");
        printStyles();
        out.format("</style>%n");
    }

    private void ensureOut() {
        if (out == null)
            out = new Formatter(output);
    }

    public void printStyles() {
        ensureOut();

        // First, copy the base css
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    getClass().getResourceAsStream("/excelStyle.css")));
            String line;
            while ((line = in.readLine()) != null) {
                out.format("%s%n", line);
            }
        } catch (Exception e) {
            // use local copy 
            in =  new BufferedReader(new StringReader(css));
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    out.format("%s%n", line);
                }
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            //throw new IllegalStateException("Reading standard css", e);
            
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //noinspection ThrowFromFinallyBlock
                    throw new IllegalStateException("Reading standard css", e);
                }
            }
        }

        // now add css for each used style
        Set<CellStyle> seen = new HashSet<CellStyle>();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            Iterator<Row> rows = sheet.rowIterator();
            while (rows.hasNext()) {
                Row row = rows.next();
                for (Cell cell : row) {
                    CellStyle style = cell.getCellStyle();
                    if (!seen.contains(style)) {
                        printStyle(style);
                        seen.add(style);
                    }
                }
            }
        }
    }

    private void printStyle(CellStyle style) {
        out.format(".%s .%s {%n", DEFAULTS_CLASS, styleName(style));
        styleContents(style);
        out.format("}%n");
    }

    private void styleContents(CellStyle style) {
        styleOut("text-align", style.getAlignment(), ALIGN);
        styleOut("vertical-align", style.getAlignment(), VERTICAL_ALIGN);
        fontStyle(style);
        borderStyles(style);
        helper.colorStyles(style, out);
    }

    private void borderStyles(CellStyle style) {
        styleOut("border-left", style.getBorderLeft(), BORDER);
        styleOut("border-right", style.getBorderRight(), BORDER);
        styleOut("border-top", style.getBorderTop(), BORDER);
        styleOut("border-bottom", style.getBorderBottom(), BORDER);
    }

    private void fontStyle(CellStyle style) {
        Font font = wb.getFontAt(style.getFontIndex());

        if (font.getBoldweight() >= HSSFFont.BOLDWEIGHT_NORMAL)
            out.format("  font-weight: bold;%n");
        if (font.getItalic())
            out.format("  font-style: italic;%n");

        int fontheight = font.getFontHeightInPoints();
        if (fontheight == 9) {
            //fix for stupid ol Windows
            fontheight = 10;
        }
        out.format("  font-size: %dpt;%n", fontheight);

        // Font color is handled with the other colors
    }

    private String styleName(CellStyle style) {
        if (style == null)
            style = wb.getCellStyleAt((short) 0);
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("style_%02x", style.getIndex());
        return fmt.toString();
    }

    private <K> void styleOut(String attr, K key, Map<K, String> mapping) {
        String value = mapping.get(key);
        if (value != null) {
            out.format("  %s: %s;%n", attr, value);
        }
    }

    private static int ultimateCellType(Cell c) {
        int type = c.getCellType();
        if (type == Cell.CELL_TYPE_FORMULA)
            type = c.getCachedFormulaResultType();
        return type;
    }

    private void printSheets() {
        ensureOut();
        Sheet sheet = wb.getSheetAt(0);
        printSheet(sheet);
    }

    public void printSheet(Sheet sheet) {
        ensureOut();
        out.format("<table class=%s>%n", DEFAULTS_CLASS);
        printCols(sheet);
        printSheetContent(sheet);
        out.format("</table>%n");
    }

    private void printCols(Sheet sheet) {
        out.format("<col/>%n");
        ensureColumnBounds(sheet);
        for (int i = firstColumn; i < endColumn; i++) {
            out.format("<col/>%n");
        }
    }

    private void ensureColumnBounds(Sheet sheet) {
        if (gotBounds)
            return;

        Iterator<Row> iter = sheet.rowIterator();
        firstColumn = (iter.hasNext() ? Integer.MAX_VALUE : 0);
        endColumn = 0;
        while (iter.hasNext()) {
            Row row = iter.next();
            short firstCell = row.getFirstCellNum();
            if (firstCell >= 0) {
                firstColumn = Math.min(firstColumn, firstCell);
                endColumn = Math.max(endColumn, row.getLastCellNum());
            }
        }
        gotBounds = true;
    }

    private void printColumnHeads() {
        out.format("<thead>%n");
        out.format("  <tr class=%s>%n", COL_HEAD_CLASS);
        out.format("    <th class=%s>&#x25CA;</th>%n", COL_HEAD_CLASS);
        //noinspection UnusedDeclaration
        StringBuilder colName = new StringBuilder();
        for (int i = firstColumn; i < endColumn; i++) {
            colName.setLength(0);
            int cnum = i;
            do {
                colName.insert(0, (char) ('A' + cnum % 26));
                cnum /= 26;
            } while (cnum > 0);
            out.format("    <th class=%s>%s</th>%n", COL_HEAD_CLASS, colName);
        }
        out.format("  </tr>%n");
        out.format("</thead>%n");
    }

    private void printSheetContent(Sheet sheet) {
        printColumnHeads();

        out.format("<tbody>%n");
        Iterator<Row> rows = sheet.rowIterator();
        while (rows.hasNext()) {
            Row row = rows.next();

            out.format("  <tr>%n");
            out.format("    <td class=%s>%d</td>%n", ROW_HEAD_CLASS,
                    row.getRowNum() + 1);
            for (int i = firstColumn; i < endColumn; i++) {
                String content = "&nbsp;";
                String attrs = "";
                CellStyle style = null;
                if (i >= row.getFirstCellNum() && i < row.getLastCellNum()) {
                    Cell cell = row.getCell(i);
                    if (cell != null) {
                        style = cell.getCellStyle();
                        attrs = tagStyle(cell, style);
                        //Set the value that is rendered for the cell
                        //also applies the format
                        CellFormat cf = CellFormat.getInstance(
                                style.getDataFormatString());
                        CellFormatResult result = cf.apply(cell);
                        content = result.text;
                        if (content.equals(""))
                            content = "&nbsp;";
                    }
                }
                out.format("    <td class=%s %s>%s</td>%n", styleName(style),
                        attrs, content);
            }
            out.format("  </tr>%n");
        }
        out.format("</tbody>%n");
    }

    private String tagStyle(Cell cell, CellStyle style) {
        if (style.getAlignment() == ALIGN_GENERAL) {
            switch (ultimateCellType(cell)) {
            case HSSFCell.CELL_TYPE_STRING:
                return "style=\"text-align: left;\"";
            case HSSFCell.CELL_TYPE_BOOLEAN:
            case HSSFCell.CELL_TYPE_ERROR:
                return "style=\"text-align: center;\"";
            case HSSFCell.CELL_TYPE_NUMERIC:
            default:
                // "right" is the default
                break;
            }
        }
        return "";
    }
}