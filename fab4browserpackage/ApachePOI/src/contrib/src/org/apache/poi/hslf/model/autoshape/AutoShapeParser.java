package org.apache.poi.hslf.model.autoshape;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
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
                // based on them, we now compute the formula values
                formulaValues = new int[25];
                BufferedReader r = new BufferedReader(
                        new StringReader(formulas));
                String line;
                try {
                    // one formula per line
                    // formulas numbered from 0
                    int numFormula = 0;
                    while ((line = r.readLine()) != null) {
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

                // finally we can use the relults to creathe the shape, parsing
                // the path
                GeneralPath path = parsePath(paths, formulaValues);
                // finally we return the path based on the adj values, formulas
                // and path description

                return path;
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
                c = paths.charAt(++i);
                switch (c) {
                case 'e':
                    System.out.println("please support a" + c);
                    break;
                case 'l':
                    System.out.println("please support a" + c);
                    break;
                case 't':
                    System.out.println("please support a" + c);
                    break;
                case 'r':
                    System.out.println("please support a" + c);
                    break;
                default:
                    break;
                }
            case 'w':
                c = paths.charAt(++i);
                if (c == 'a') {
                    System.out.println("please support w" + c);
                } else if (c == 'r') {
                    System.out.println("please support w" + c);
                }
            case 'q':
                c = paths.charAt(++i);
                switch (c) {
                case 'x':
                    System.out.println("please support q" + c);
                    break;
                case 'y':
                    System.out.println("please support q" + c);
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

    public static void main(String[] args) {
        String[] p = defaultShapes.get(ShapeTypes.Parallelogram);
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

}
