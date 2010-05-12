package org.apache.poi.hslf.model.autoshape;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.hslf.model.Shape;
import org.apache.poi.hslf.model.ShapeOutline;
import org.apache.poi.hslf.model.ShapeTypes;

/**
 * 
 * This class will parse the geometric properties of autoshapes, and use them to
 * create a Java shape.
 * 
 * @author fabio
 * 
 */
public class AutoShapeParser {

    /**
     * 
     * @param paths
     * @param formulas
     * @param adjVal
     * @return
     */

    static Map<Integer, String[]> defaultShapes = new HashMap<Integer, String[]>();
    static {
        defaultShapes.put(ShapeTypes.Parallelogram, new String[] {
                "m@0,l,21600@1,21600,21600,xe",
                "val #0 \n" + "sum width 0 #0 \n" + "prod #0 1 2 \n"
                        + "sum width 0 @2 \n" + "mid #0 width \n" +

                        "mid @1 0 \n" +

                        "prod height width #0 \n" +

                        "prod @6 1 2 \n" +

                        "sum height 0 @7 \n" +

                        "prod width 1 2 \n" +

                        "sum #0 0 @9 \n" +

                        "if @10 @8 0 \n" +

                        "if @10 @7 height" +

                        "", "5400" });

        defaultShapes
                .put(
                        ShapeTypes.Donut,
                        new String[] {
                                "m10800,qx,10800,10800,21600,21600,10800,10800,xem7340,6445qx6215,75"
                                        + "70,7340,8695,8465,7570,7340,6445xnfem14260,6445qx13135,7570,14260,86"
                                        + "95,15385,7570,14260,6445xnfem4960@0c8853@3,12747@3,16640@0nfe",
                                "val #0\n" + "sum width 0 #0\n"
                                        + "sum height 0 #0\n"
                                        + "prod @0 2929 10000\n"
                                        + "sum width 0 @3\n"
                                        + "sum height 0 @3\n", "5400" });
        defaultShapes.put(ShapeTypes.Can, new String[] {
                "m10800,qx0@1l0@2qy10800,21600,21600@2l21600@1qy10800,xem0@1qy"
                        + "10800@0,21600@1nfe",
                "val #0\n" + "prod #0 1 2\n" + "sum height 0 @1\n", "5400" });

        defaultShapes.put(ShapeTypes.Cube, new String[] {
                "m@0,l0@0,,21600@1,21600,21600@2,21600,xem0@0nfl@1@0,21600,em@"
                        + "1@0nfl@1,21600e",
                "val #0 \n" + "sum width 0 #0\n" + "sum height 0 #0\n"
                        + "mid height #0\n" + "prod @1 1 2\n" + "prod @2 1 2\n"
                        + "mid width #0\n", "5400"

        });
        defaultShapes.put(ShapeTypes.FlowChartMagneticTape, new String[] {
                "ar,,21600,21600,18685,18165,10677,21597l20990,21597r,-3432xe",
                null, null });
        defaultShapes.put(ShapeTypes.CloudCallout, new String[] {
        "ar,7165,4345,13110,1950,7185,1080,12690,475,11732,4835,17650,1080,1269" + 
        "0,2910,17640,2387,9757,10107,20300,2910,17640,8235,19545,7660,12382,1" + 
        "4412,21597,8235,19545,14280,18330,12910,11080,18695,18947,14280,1833" + 
        "0,18690,15045,14822,5862,21597,15082,18690,15045," + 
        "20895,7665,15772,259" + 
        "2,21105,9865,20895,7665,19140,2715,14330,,19187,6595,19140,2715,14910," + 
        "1170,10992,,15357,5945,14910,1170,11250,1665,6692,650,12025,7917,1125" + 
        "0,1665,7005,2580,1912,1972,8665,11162,7005,2580,1950,7185xear,7165,434" + 
        "5,13110,1080,12690,2340,130" + 
        "80nfear475,11732,4835,17650,2910,17640,346" + 
        "5,17445nfear7660,12382,14412,21597,7905,18675,8235,19545nfear7660,123" + 
        "82,14412,21597,14280,18330,14400,17370nfear12910,11080,18695,18947,18" + 
        "690,15045,17070,11475nfear15772,2592,21105,9865,20175,9015,20895,7665" + 
        "nfear14" + 
        "330,,19187,6595,19200,3345,19140,2715nfear14330,,19187,6595,149" + 
        "10,1170,14550,1980nfear10992,,15357,5945,11250,1665,11040,2340nfear191" + 
        "2,1972,8665,11162,7650,3270,7005,2580nfear1912,1972,8665,11162,1950,71" + 
        "85,2070,7890nfem@23@37qx@35@24@23@36@34@24@23@37xem@1" + 
        "6" + 
        "@33qx@31@17@16@32@30@17@16@33xem@38@29qx@27@39@38@" + 
        "28@26@39@38@29xe", "sum #0 0 10800 \n" + 

        		"sum #1 0 10800 \n" + 

        		"cosatan2 10800 @0 @1 \n" + 

        		"sinatan2 10800 @0 @1 \n" + 
        		"sum @2 10800 0 \n" + 
        		"sum @3 10800 0 \n" + 
        		"sum @4 0 #0 \n" + 
        		"sum @5 0 #1 \n" + 
        		"mod @6 @7 0 \n" + 
        		"prod 600 11 1 \n" + 
        		"sum @8 0 @9 \n" + 
        		"  \n" + 
        		"prod @10 1 3 \n" + 
        		"  \n" + 
        		"prod 600 3 1 \n" + 
        		"  \n" + 
        		"sum @11 @12 0 \n" + 
        		"  \n" + 
        		"prod @13 @6 @8 \n" + 
        		"  \n" + 
        		"prod @13 @7 @8 \n" + 
        		"  \n" + 
        		"sum @14 #0 0 \n" + 
        		"  \n" + 
        		"sum @15 #1 0 \n" + 
        		"  \n" + 
        		"prod 600 8 1 \n" + 
        		"  \n" + 
        		"prod @11 2 1 \n" + 
        		"  \n" + 
        		"sum @18 @19 0 \n" + 
        		"  \n" + 
        		"prod @20 @6 @8 \n" + 
        		"  \n" + 
        		"prod @20 @7 @8 \n" + 
        		"  \n" + 
        		"sum @21 #0 0 \n" + 
        		"  \n" + 
        		"sum @22 #1 0 \n" + 
        		"  \n" + 
        		"prod 600 2 1 \n" + 
        		"  \n" + 
        		"sum #0 600 0 \n" + 
        		"  \n" + 
        		"sum #0 0 600 \n" + 
        		"  \n" + 
        		"sum #1 600 0 \n" + 
        		"  \n" + 
        		"sum #1 0 600 \n" + 
        		"  \n" + 
        		"sum @16 @25 0 \n" + 
        		"  \n" + 
        		"sum @16 0 @25 \n" + 
        		"  \n" + 
        		"sum @17 @25 0 \n" + 
        		"  \n" + 
        		"sum @17 0 @25 \n" + 
        		"  \n" + 
        		"sum @23 @12 0 \n" + 
        		"  \n" + 
        		"sum @23 0 @12 \n" + 
        		"  \n" + 
        		"sum @24 @12 0 \n" + 
        		"  \n" + 
        		"sum @24 0 @12 \n" + 
        		"  \n" + 
        		"val #0 \n" + 
        		"  \n" + 
        		"val #1 \n" + 
        		"", "1350,25920"});
        defaultShapes.put(ShapeTypes.CurvedUpArrow, new String[] {
        "ar0@22@3@21,,0@4@21@14@22@1@21@7@21@12@2l@13@2@8,0@11@2wa0@22@3@21@10@2@16@24@14@22@1@21@16@24@14,xewr" + 
        "@14@2" + 
        "2@1@21@7@21@16@24nfe",  
        "val #0 \n" + 
        "  \n" + 
        "val #1 \n" + 
        "  \n" + 
        "val #2 \n" + 
        "  \n" + 
        "sum #0 width #1 \n" + 
        "  \n" + 
        "prod @3 1 2 \n" + 
        "  \n" + 
        "sum #1 #1 width \n" + 
        "  \n" + 
        "sum @5 #1 #0 \n" + 
        "  \n" + 
        "prod @6 1 2 \n" + 
        "  \n" + 
        "mid width #0 \n" + 
        "  \n" + 
        "ellipse #2 height @4 \n" + 
        "  \n" + 
        "sum @4 @9 0 \n" + 
        "  \n" + 
        "sum @10 #1 width \n" + 
        "  \n" + 
        "sum @7 @9 0 \n" + 
        "  \n" + 
        "sum @11 width #0 \n" + 
        "  \n" + 
        "sum @5 0 #0 \n" + 
        "  \n" + 
        "prod @14 1 2 \n" + 
        "  \n" + 
        "mid @4 @7 \n" + 
        "  \n" + 
        "sum #0 #1 width \n" + 
        "  \n" + 
        "prod @17 1 2 \n" + 
        "  \n" + 
        "sum @16 0 @18 \n" + 
        "  \n" + 
        "val width \n" + 
        "  \n" + 
        "val height \n" + 
        "  \n" + 
        "sum 0 0 height \n" + 
        "  \n" + 
        "sum @16 0 @4 \n" + 
        "  \n" + 
        "ellipse @23 @4 height \n" + 
        "  \n" + 
        "sum @8 128 0 \n" + 
        "  \n" + 
        "prod @5 1 2 \n" + 
        "  \n" + 
        "sum @5 0 128 \n" + 
        "  \n" + 
        "sum #0 @16 @11 \n" + 
        "  \n" + 
        "sum width 0 #0 \n" + 
        "  \n" + 
        "prod @29 1 2 \n" + 
        "  \n" + 
        "prod height height 1 \n" + 
        "  \n" + 
        "prod #2 #2 1 \n" + 
        "  \n" + 
        "sum @31 0 @32 \n" + 
        "  \n" + 
        "sqrt @33 \n" + 
        "  \n" + 
        "sum @34 height 0 \n" + 
        "  \n" + 
        "prod width height @35 \n" + 
        "  \n" + 
        "sum @36 64 0 \n" + 
        "  \n" + 
        "prod #0 1 2 \n" + 
        "  \n" + 
        "ellipse @30 @38 height \n" + 
        "  \n" + 
        "sum @39 0 64 \n" + 
        "  \n" + 
        "prod @4 1 2 \n" + 
        "  \n" + 
        "sum #1 0 @41 \n" + 
        "  \n" + 
        "prod height 4390 32768 \n" + 
        "  \n" + 
        "prod height 28378 32768 \n",
        "12960,19440,7200"});
        defaultShapes.put(ShapeTypes.Heart, new String[] {
        "m10860,2187c10451,1746,9529,1018,9015,730,7865,152,6685,,5415,,4175,15" + 
        "2,2995,575,1967,1305,1150,2187,575,3222,242,4220,,5410,242,6560,575,759" + 
        "7l10860,21600,20995,7597v485," + 
        "-" + 
        "1037,605," + 
        "-" + 
        "2187,485," + 
        "-" + 
        "3377c21115,3222,2042" + 
        "0,2187,19632,1305,18575,575,17425,152,16275,," + 
        "15005,,13735,152,12705,73" + 
        "0v" + 
        "-" + 
        "529,288," + 
        "-" + 
        "1451,1016," + 
        "-"  +
        "1845,1457xe", null, null});
   
    }

    public static void main(String[] args) {
        String[] p = defaultShapes.get(ShapeTypes.Heart);
        final java.awt.Shape s = parseShapeData(p[0], p[1], p[2]).getOutline(
                null);
        AffineTransform at = new AffineTransform();
        at.translate(5, 5);
        at.scale(1.0f / 50, 1.0f / 50);
        final java.awt.Shape s2 = at.createTransformedShape(s);
        JFrame f = new JFrame("path");
        f.getContentPane().add(new JPanel() {
            @Override
            public Dimension getSize() {
                // TODO Auto-generated method stub
                return new Dimension(512, 512);
            }

            public Dimension getPreferredSize() {
                return getSize();
            };

            public Dimension getMinimumSize() {
                return getSize();
            };

            public void paint(java.awt.Graphics g) {
                Graphics2D gg = (Graphics2D) g;
                gg.setColor(Color.BLACK);
                gg.draw(s2);

            };
        });
        f.pack();
        f.setVisible(true);

    }

    public static ShapeOutline parseShapeData(final String paths,
            final String formulas, final String adjVal) {

        return new ShapeOutline() {
            int[] formulaValues;
            int[] adjustmentValues;

            public java.awt.Shape getOutline(Shape shape) {

                adjustmentValues = new int[10];

                // we first parse the adjustment values, a comma separated list
                // of values.
                if (adjVal != null) {
                    LinkedList<Integer> defAdjV = getArgs(adjVal, 0,
                            adjustmentValues);
                    defAdjV.removeFirst();

                    if (shape != null)
                        for (int j = 0; j < defAdjV.size(); j++) {
                            adjustmentValues[j] = shape
                                    .getEscherProperty(
                                            (short) (EscherProperties.GEOMETRY__ADJUSTVALUE + j),
                                            defAdjV.get(j));
                        }
                    else
                        for (int j = 0; j < defAdjV.size(); j++) {
                            adjustmentValues[j] = defAdjV.get(j);
                        }
                }
                // based on them, we now compute the formula values
                formulaValues = new int[55];
                if (formulas != null)
                    parseFormulas(formulas);

                // finally we can use the relults to creathe the shape, parsing
                // the path
                GeneralPath path = parsePath(paths, formulaValues);
                // finally we return the path based on the adj values, formulas
                // and path description

                return path;
            }

            private void parseFormulas(final String formulas) {
                BufferedReader r = new BufferedReader(
                        new StringReader(formulas));
                String line;
                try {
                    // one formula per line
                    // formulas numbered from 0
                    int numFormula = 0;
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (line.length() == 0)
                            continue;
                        // space tokenised
                        StringTokenizer st = new StringTokenizer(line, " ",
                                false);
                        // interpret the 1st token
                        String t = st.nextToken();
                        int a, b, c;
                        a = b = c = 0;
                        if (t.equals("val")) {
                            t = st.nextToken();
                            if (t.startsWith("#")) {
                                int k = Integer.parseInt(t.substring(1));
                                formulaValues[numFormula] = adjustmentValues[k];
                            } else {
                                try {
                                    formulaValues[numFormula] = Integer
                                            .parseInt(t);
                                } catch (NumberFormatException e) {
                                    if (t.equals("width")) {
                                        formulaValues[numFormula] = 21600;
                                        // TODO: fixme
                                    } else if (t.equals("height")) {
                                        formulaValues[numFormula] = 21600;
                                        // TODO: fixme
                                    }
                                }

                            }
                        } else {
                            if (st.hasMoreTokens())
                                a = getValues(st.nextToken());
                            if (st.hasMoreTokens())
                                b = getValues(st.nextToken());
                            if (st.hasMoreTokens())
                                c = getValues(st.nextToken());

                            if (t.equals("sum")) {
                                formulaValues[numFormula] = a + b - c;
                            } else if (t.equals("product") || t.equals("prod")) {
                                formulaValues[numFormula] = a * b / c;
                            } else if (t.equals("mid")) {
                                formulaValues[numFormula] = (a + b) / 2;
                            } else if (t.equals("abs")) {
                                Math.abs(a);
                            } else if (t.equals("min")) {
                                formulaValues[numFormula] = Math.min(a, b);
                            } else if (t.equals("max")) {
                                formulaValues[numFormula] = Math.max(a, b);
                            } else if (t.equals("if")) {
                                if (a > 0)
                                    formulaValues[numFormula] = b;
                                else
                                    formulaValues[numFormula] = c;

                            } else if (t.equals("sqrt")) {
                                formulaValues[numFormula] = (int) Math.sqrt(a);
                            } else if (t.equals("mod")) {
                                formulaValues[numFormula] = (int) Math
                                        .sqrt((a * a) + (b * b) + (c * c));
                            } else if (t.equals("sin")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .sin(b));
                            } else if (t.equals("cos")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .cos(b));
                            } else if (t.equals("tan")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .tan(b));
                            } else if (t.equals("atan2")) {
                                formulaValues[numFormula] = (int) (Math.atan2(
                                        a, b));
                            } else if (t.equals("sinatan2")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .sin(Math.atan2(b, c)));
                            } else if (t.equals("cosatan2")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .cos(Math.atan2(b, c)));
                            } else if (t.equals("sumangle")) {
                                System.out.println("TODO: Implement sumangle ");
                            } else if (t.equals("ellipse")) {
                                formulaValues[numFormula] = (int) (c * Math
                                        .sqrt(1 - ((a / b) * (a / b))));
                            }
                        }
                        numFormula++;
                    }
                } catch (IOException e) {
                    System.out
                            .println("This should never happen, io error reading a string");
                    e.printStackTrace();
                }
            }

            int getValues(String a) {
                if (a.startsWith("@")) {
                    int k = Integer.parseInt(a.substring(1));
                    return formulaValues[k];
                } else if (a.startsWith("#")) {
                    int k = Integer.parseInt(a.substring(1));
                    return adjustmentValues[k];
                }
                try {
                    return Integer.parseInt(a);
                } catch (NumberFormatException e) {
                    if (a.equals("width")) {
                        return 21600;
                        // TODO: fixme
                    } else if (a.equals("height")) {
                        return 21600;
                        // TODO: fixme
                    }
                }
                return 0;

            }

        };
    }

    public static boolean isDigitOrMinus(char c) {
        return Character.isDigit(c) || c == '-';
    }

    /**
     * parses the list of arguments
     * 
     * @param source
     *            the source string
     * @param s
     *            the starting point in the string
     * @param vals
     *            the values to replace for variables (indicated by @X)
     * 
     * @return a list where the first member is the index of the last character
     *         of the parameters in the string (where to continue the parsing)
     *         followed by the list of arguments
     * 
     */
    static LinkedList<Integer> getArgs(String source, int s, int[] vals) {
        LinkedList<Integer> l = new LinkedList<Integer>();
        boolean newNumber = false;
        int i = s;
        for (; i < source.length(); i++) {
            char c = source.charAt(i);
            // if we have a value
            if (isDigitOrMinus(c)) {
                int start = i;
                do {
                    if (++i == source.length())
                        break;
                    c = source.charAt(i);
                } while (isDigitOrMinus(c));
                l.add(Integer.parseInt(source.substring(start, i)));
                i--;
                newNumber = true;
                // if we have a separator
            } else if (c == ' ' || c == ',') {
                if (!newNumber)
                    l.add(0);
                newNumber = false;
                // if we have a variable
            } else if (c == '@') {
                int start = ++i;
                do {
                    if (++i == source.length())
                        break;
                    c = source.charAt(i);
                } while (Character.isDigit(c));
                // we parse the number of variable, get the value
                int varValue = vals[Integer
                        .parseInt(source.substring(start, i))];
                // and add it to the list
                l.add(varValue);
                i--;
                newNumber = true;
                // if the encounter a letter, we are over the list of
                // parameters
            } else if (Character.isLetter(c)) {
                if (!newNumber)
                    l.add(0);
                break;
            }

        }
        // the first value in the list is where we got parsing
        l.addFirst(i);
        return l;
    }

    public static GeneralPath parsePath(final String paths, int[] formulaValues) {
        GeneralPath path = new GeneralPath();
        int i = 0;
        // paths have a peculiar sintax, so we parse them character by
        // character
        while (i < paths.length()) {
            char c = paths.charAt(i);
            i++;
            LinkedList<Integer> l;
            // current location for relative operations
            Point2D s = path.getCurrentPoint();
            double r1 = 0;
            double r2 = 0;
            if (s != null) {
                r1 = path.getCurrentPoint().getX();
                r2 = path.getCurrentPoint().getY();
            }
            // here we parse all the operations that costitue the path
            switch (c) {
            // move current point to
            case 'm':
            case 't':
                // m is non relative
                if (c == 'm')
                    r1 = r2 = 0;
                // get the arguments
                l = getArgs(paths, i, formulaValues);
                // the first value is the new location in the string
                i = l.removeFirst();
                if (l.size() == 0)
                    path.moveTo(0 + r1, 0 + r2);
                else if (l.size() == 1)
                    path.moveTo(l.get(0) + r1, 0 + r2);
                else if (l.size() == 2)
                    path.moveTo(l.get(0) + r1, l.get(1) + r2);
                else
                    System.out.println("move with >2 arguments!");
                break;

            case 'l':
            case 'r':
                // case for line to (or relative line to) from the
                // current point; can have multiple lines in a raw
                if (c == 'l')
                    r1 = r2 = 0;
                l = getArgs(paths, i, formulaValues);
                i = l.removeFirst();
                // l/r can specify multiple lines, by giving multiple
                // couple of parameters
                for (int k = 0; k < l.size();) {
                    int a = l.get(k++);
                    int b = 0;
                    if (k < l.size())
                        b = l.get(k++);
                    path.lineTo(a + r1, b + r2);

                }
                break;
            case 'c':
            case 'v':
                // curve to, same as for line,
                if (c == 'c')
                    r1 = r2 = 0;
                l = getArgs(paths, i, formulaValues);
                i = l.removeFirst();
                for (int k = 0; k < l.size();) {
                    path.curveTo(l.get(k++) + r1, l.get(k++) + r2, l.get(k++)
                            + r1, l.get(k++) + r2, l.get(k++) + r1, l.get(k++)
                            + r2);
                }
                break;
            case 'x':
                path.closePath();
                break;
            case 'e':
                // end current path, start a new one
                GeneralPath p2 = new GeneralPath();
                p2.append(path, false);
                path = p2;
                break;
            case 'n':
                c = paths.charAt(++i);
                if (c == 'f') {
                    // nofill
                } else if (c == 's') {
                    // nostroke
                }
                break;
            case 'a':
                c = paths.charAt(i++);
                switch (c) {
                case 'e':
                    l = getArgs(paths, i, formulaValues);
                    i = l.removeFirst();

                    for (int k = 0; k < l.size();) {
                        Arc2D.Float ar = new Arc2D.Float();
                        ar.setFrameFromCenter(l.get(k++), l.get(k++), l.get(k++), l
                                .get(k++));
                        ar.setAngleStart(l.get(k++));
                        ar.setAngleExtent(l.get(k++));
                        path.append(ar, true);
                        //if (c != 'l')
                            //path.moveTo(x, y);
                    }

                    break;
                case 't':
                case 'r':

                    l = getArgs(paths, i, formulaValues);
                    i = l.removeFirst();
                    int x,y;
                    for (int k = 0; k < l.size();) {
                        Arc2D.Float ar = new Arc2D.Float();
                        ar.setFrame(l.get(k++), l.get(k++), l.get(k++), l
                                .get(k++));
                        ar.setAngles(  x =l.get(k++),  y =l.get(k++),
                                l.get(k++), l.get(k++));
                        path.append(ar, true);
                        if (c != 'r')
                            path.moveTo(x, y);
                    }

                    break;

                default:
                    break;
                }
                break;
            case 'w':
                c = paths.charAt(i++);
                if (c == 'a' || c == 'r') {

                    l = getArgs(paths, i, formulaValues);
                    i = l.removeFirst();
                    int x,y;
                    for (int k = 0; k < l.size();) {
                        Arc2D.Float ar = new Arc2D.Float();
                        ar.setFrame(l.get(k++), l.get(k++), l.get(k++), l
                                .get(k++));
                        ar.setAngles(
                                l.get(k++), l.get(k++), x =l.get(k++),  y =l.get(k++));
                        
                        path.append(ar, true);
                        if (c != 'r')
                            path.moveTo(x, y);
                    }
                }
            case 'q':
                c = paths.charAt(i++);
                switch (c) {
                case 'x':
                    l = getArgs(paths, i, formulaValues);
                    i = l.removeFirst();
                    // l/r can specify multiple lines, by giving multiple
                    // couple of parameters
                    for (int k = 0; k < l.size();) {
                        int a = l.get(k++);
                        int b = 0;
                        if (k < l.size())
                            b = l.get(k++);
                        Arc2D.Float ar = new Arc2D.Float();
                        ar.setAngles(r1, r2, a, b);
                        ar.setAngleExtent(90);
                        path.append(ar, false);
                    }
                    break;
                case 'y':
                    l = getArgs(paths, i, formulaValues);
                    i = l.removeFirst();
                    // l/r can specify multiple lines, by giving multiple
                    // couple of parameters
                    for (int k = 0; k < l.size();) {
                        int a = l.get(k++);
                        int b = 0;
                        if (k < l.size())
                            b = l.get(k++);
                        path.lineTo(a + r1, b + r2);
                        Arc2D.Float ar = new Arc2D.Float();
                        ar.setAngles(r1, r2, a, b);
                        ar.setAngleExtent(-180);
                        path.append(ar, false);
                    }
                    break;
                case 'b':
                    System.out.println("please support q" + c);
                    break;
                default:
                    break;
                }
            default:
                break;

            }

        }
        return path;
    }

}
