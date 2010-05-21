package org.apache.poi.hslf.model.autoshape;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.IllegalPathStateException;
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

    static Map<Integer, String[]> defaultShapes = new HashMap<Integer, String[]>();
    private static boolean nostroke;

    public static void main(String[] args) throws IOException {
        cleanup.main(new String[] { "shapeDescriptions.txt" });
        if (false) {
            String[] p = defaultShapes.get(ShapeTypes.Donut);
            final java.awt.Shape s = parseShapeData(p[0], p[1], p[2])
                    .getOutline(null);
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
                formulaValues = new int[80];
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
                        double a, b, c;
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
                                formulaValues[numFormula] = (int) (a + b - c);
                            } else if (t.equals("product") || t.equals("prod")) {
                                formulaValues[numFormula] = (int) (a * b / c);
                            } else if (t.equals("mid")) {
                                formulaValues[numFormula] = (int) ((a + b) / 2);
                            } else if (t.equals("abs")) {
                                Math.abs(a);
                            } else if (t.equals("min")) {
                                formulaValues[numFormula] = (int) Math
                                        .min(a, b);
                            } else if (t.equals("max")) {
                                formulaValues[numFormula] = (int) Math
                                        .max(a, b);
                            } else if (t.equals("if")) {
                                if (a > 0)
                                    formulaValues[numFormula] = (int) b;
                                else
                                    formulaValues[numFormula] = (int) c;

                            } else if (t.equals("sqrt")) {
                                formulaValues[numFormula] = (int) Math.sqrt(a);
                            } else if (t.equals("mod")) {
                                formulaValues[numFormula] = (int) Math
                                        .sqrt((a * a) + (b * b) + (c * c));
                            } else if (t.equals("sin")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .sin(b*(Math.PI/180)));
                            } else if (t.equals("cos")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .cos(b*(Math.PI/180)));
                            } else if (t.equals("tan")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .tan(b));
                            } else if (t.equals("atan2")) {
                                formulaValues[numFormula] = (int) (Math.atan2(
                                        a, b)/(Math.PI/180));
                            } else if (t.equals("sinatan2")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .sin(Math.atan2(b, c)));
                            } else if (t.equals("cosatan2")) {
                                formulaValues[numFormula] = (int) (a * Math
                                        .cos(Math.atan2(b, c)));
                            } else if (t.equals("sumangle")) {
                                formulaValues[numFormula] = (int) (a
                                        + (b * 65536) - (c * 65536));
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
        // System.out.println(source.substring(s));
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
        // paths have a peculiar syntax, so we parse them character by
        // character
        while (i < paths.length()) {
            char c = paths.charAt(i);
            i++;
            LinkedList<Integer> l;
            // current location for relative operations
            Point2D s = path.getCurrentPoint();
            double rx = 0;
            double ry = 0;

            if (s != null) {
                rx = path.getCurrentPoint().getX();
                ry = path.getCurrentPoint().getY();
            }
            // here we parse all the operations that costitue the path
            switch (c) {
            /* MOVETO */
            case 'm':
            case 't':
                // m is non relative
                if (c == 'm')
                    rx = ry = 0;
                // get the arguments
                l = getArgs(paths, i, formulaValues);
                // the first value is the new location in the string
                i = l.removeFirst();
                if (l.size() == 0)
                    path.moveTo(0 + rx, 0 + ry);
                else if (l.size() == 1)
                    path.moveTo(l.get(0) + rx, 0 + ry);
                else if (l.size() == 2)
                    path.moveTo(l.get(0) + rx, l.get(1) + ry);
                else
                    System.out.println("move with >2 arguments!");
                break;
                /* LINETO */
            case 'l':
            case 'r':
                // case for line to (or relative line to) from the
                // current point; can have multiple lines in a raw
                if (c == 'l')
                    rx = ry = 0;
                l = getArgs(paths, i, formulaValues);
                i = l.removeFirst();
                // l/r can specify multiple lines, by giving multiple
                // couple of parameters
                for (int k = 0; k < l.size();) {
                    int a = l.get(k++);
                    int b = 0;
                    if (k < l.size())
                        b = l.get(k++);
                    try {
                        path.lineTo(a + rx, b + ry);
                    } catch (IllegalPathStateException e) {
                        path.moveTo(rx, ry);
                        path.lineTo(a + rx, b + ry);
                    }
                    // for some reason, it's relative to the starting point
                    // only, even in a sequence
                    // if (c != 'l')
                    // rx = a + rx;ry = a + ry;
                }
                break;
                /* CURVETO */
            case 'c':
            case 'v':
                // curve to, same as for line,
                if (c == 'c')
                    rx = ry = 0;
                l = getArgs(paths, i, formulaValues);
                i = l.removeFirst();
                for (int k = 0; k < l.size();) {
                    double a, b;
                    path.curveTo(l.get(k++) + rx, l.get(k++) + ry, l.get(k++)
                            + rx, l.get(k++) + ry, (a = (l.get(k++) + rx)),
                            (b = (l.get(k++) + ry)));
                    // if (c != 'c')
                    // rx = a + rx;ry = b + ry;
                }
                break;
                /* CLOSE PATH */
            case 'x':
                if (!nostroke)
                    path.closePath();
                break;

                /* END PATH */
            case 'e':
                // end current path, start a new one
                nostroke = false;
                GeneralPath p2 = new GeneralPath();
                p2.append(path, false);
                path = p2;
                break;
                /* NOSTROKE NOFILL */
            case 'n':
                c = paths.charAt(++i);
                if (c == 'f') {
                    // nofill
                } else if (c == 's') {
                    nostroke = true;
                    // nostroke
                }
                break;
                /* Angle or Arc */
            case 'a':
                c = paths.charAt(i++);
                /* AngleEllipseTo */
                switch (c) {
                case 'e':
                case 'l':
                    l = getArgs(paths, i, formulaValues);
                    i = l.removeFirst();
                    Arc2D.Double ar = new Arc2D.Double(Arc2D.OPEN);
                    for (int k = 0; k < l.size();) {

                        double cx = l.get(k++), cy = l.get(k++), sx = l
                        .get(k++), sy = l.get(k++), sa = l.get(k++), ea = l
                        .get(k++);
                        sa /= 65536;
                        ea /= 65536;

                        sa = Math.round(sa);
                        ea = Math.round(ea);

                        //
                        System.out.println(sa + " " + ea);
                        ar.setArcByCenter(cx, cy, sx, sa, ea, Arc2D.OPEN);
                        path.append(ar, true);

                    }
                    if (c != 'l')
                        path.moveTo(ar.getStartPoint().getX(), ar
                                .getStartPoint().getY());

                    break;
                    /* ARCTO */
                case 't':
                case 'r':
                    l = getArgs(paths, i, formulaValues);
                    i = l.removeFirst();
                    // int x,y;
                    double px1=0,
                    px2 = 0,
                    py1=0,
                    py2 = 0;
                    for (int k = 0; k < l.size();) {
                        double left = l.get(k++), top = l.get(k++), right = l
                        .get(k++), bottom = l.get(k++);
                        px1 = l.get(k++);
                        py1 = l.get(k++);
                        px2 = l.get(k++);
                        py2 = l.get(k++);
                        // ar = getArc(xx1, y1, xx2, y2, px1, py1,
                        // px2, py2, true);
                        ar = new Arc2D.Double();
                        ar.setFrame(left, top, right - left, bottom - top);
                        ar.setAngles(px1, py1, px2, py2);
//                        double ae = ar.getAngleExtent();
//                        ar.setFrame(left, top, right - left, bottom - top);
//                        ar.setAngleStart(new Point2D.Double(px1,py1));
//                        ar.setAngleExtent(ae); 
                        if (c == 'r')
                            path.append(ar, false);
                        else 
                            path.append(ar, true);
                    }
                    //if (c != 'r') 
                    //    path.moveTo(px1, py1);

                    break;
                default:
                    break;
                }
                break;

            case 'w':
                c = paths.charAt(i++);

                /* clockwise arc to */
               
                if (c == 'a' || c == 'r') {
                    l = getArgs(paths, i, formulaValues);
                    i = l.removeFirst();
                    // int x,y;
                    double px1=0, px2 = 0, py1=0, py2 = 0;
                    for (int k = 0; k < l.size();) {
                        double left = l.get(k++), top = l.get(k++), right = l
                        .get(k++), bottom = l.get(k++);
                        px1 = l.get(k++);
                        py1 = l.get(k++);
                        px2 = l.get(k++);
                        py2 = l.get(k++);
                        // Arc2D.Double ar = getArc(xx1, y1, xx2, y2, px1, py1,
                        // px2, py2, false);
                        Arc2D.Double ar = new Arc2D.Double();

                        ar.setFrame(left, top, right - left, bottom - top);
                        ar.setAngles(px2, py2, px1, py1);
                        double ae = ar.getAngleExtent();
                        ar.setFrame(left, top, right - left, bottom - top);
                        ar.setAngleStart(new Point2D.Double(px1,py1));
                        ar.setAngleExtent(-ae); 
                        if (c == 'r')
                            path.append(ar, false);
                        else 
                            path.append(ar, true);
                       
                    }
                   // if (c != 'r') 
                 //       path.moveTo(px1, py1);
                }
                break;
            case 'q':
                c = paths.charAt(i++);
                boolean tanX = false;
                switch (c) {
                case 'x':
                    tanX = true;
                case 'y':
                    l = getArgs(paths, i, formulaValues);
                    i = l.removeFirst();
                    boolean start = tanX;

                    for (int k = 0; k < l.size();) {

                        int ax = l.get(k++);
                        int ay = 0;
                        if (k < l.size())
                            ay = l.get(k++);
                        // double dx=ax-rx, dy = ay-ry;
                        double dx, dy;

                        dx = ax - rx;
                        dy = ay - ry;

                        System.out.println("Q"+(start?"x":"y")+" ax - rx =" + (dx) + " ay - ry ="
                                + (dy));
                        System.out.println(new Point2D.Double(rx, ry));
                        System.out.println(new Point2D.Double(ax, ay));
                        System.out.println(tanX);
                        Arc2D.Double ar = new Arc2D.Double();

                        double sx = Math.abs(dx);
                        double sy = Math.abs(dy);
                        double cx, cy = 0;
                        double max = cx = Math.max(ax, rx);
                        double mix = cx = Math.min(ax, rx);
                        double may = Math.max(ay, ry);
                        double miy = Math.min(ay, ry);
                        boolean ccw = true;
                        // - -
                        if (dx < 0 && dy < 0) {
                            if (tanX) {
                                cx = max;
                                cy = miy;
                            } else {
                                cx = mix;
                                cy = may;
                                ccw = false;
                            }

                            // + -
                        } else if (dx > 0 && dy < 0) {
                            if (tanX) {
                                cx = mix;
                                cy = miy;
                                ccw = false;
                            } else {
                                cx = max;
                                cy = may;

                            }

                        }
                        // + +
                        else if (dx > 0 && dy > 0) {
                            if (tanX) {
                                cx = mix;
                                cy = may;

                            } else {
                                cx = max;
                                cy = miy;
                                ccw = false;
                            }

                        }
                        // - +
                        else if (dx < 0 && dy > 0) {
                            if (tanX) {
                                cx = max;
                                cy = may;
                                ccw = false;
                            } else {
                                cx = mix;
                                cy = miy;
                            }
                        } else if (dx == 0 && dy == 0)
                            System.err.println("Flat ellipse!");
                        double px = cx - sx;
                        double py = cy - sy;

                        ar.setFrame(px, py, sx * 2, sy * 2);
                        ar.setAngleStart(new Point2D.Double(rx,ry));

                        // ar.se
                        ar.setAngleExtent(ccw ? -90 : 90);
                        //ar.setAngles( rx, rx,ax, ay);

                        path.append(ar, true);
                        // ar.setFrame(px-22, py -28,sx*2,sy*2);
                        // path.append(ar, false);
                        //                        
                        // path.moveTo(ax,ay);
                        rx = ax;
                        ry = ay;
                        tanX = !tanX;
                        // path.quadTo(rx, ay, ax, ay);//curveTo(rx, ny, nx, ay,
                        // ax, ay);
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

    static Arc2D.Double getArc(double left, double top, double right,
            double bottom, double xstart, double ystart, double xend,
            double yend, boolean ccw) {

        double cx = left + (right - left) / 2;
        double cy = top + (bottom - top) / 2;
        double startAngle = -Math.toDegrees(Math
                .atan2(ystart - cy, xstart - cx));
        double endAngle = -Math.toDegrees(Math.atan2(yend - cy, xend - cx));

        if (!ccw) {
            double t = startAngle;
            startAngle = endAngle;
            endAngle = t;
        }
        double extentAngle = endAngle - startAngle;

        if (extentAngle < 0)
            extentAngle += 360;
        if (startAngle < 0)
            startAngle += 360;

        return new Arc2D.Double(left, top, right - left, bottom - top,
                startAngle, extentAngle, Arc2D.OPEN);

    }

    //
    // /**
    // * Adds an elliptical arc, defined by two radii, an angle from the
    // * x-axis, a flag to choose the large arc or not, a flag to
    // * indicate if we increase or decrease the angles and the final
    // * point of the arc.
    // *
    // * @param rx the x radius of the ellipse
    // * @param ry the y radius of the ellipse
    // *
    // * @param angle the angle from the x-axis of the current
    // * coordinate system to the x-axis of the ellipse in degrees.
    // *
    // * @param largeArcFlag the large arc flag. If true the arc
    // * spanning less than or equal to 180 degrees is chosen, otherwise
    // * the arc spanning greater than 180 degrees is chosen
    // *
    // * @param sweepFlag the sweep flag. If true the line joining
    // * center to arc sweeps through decreasing angles otherwise it
    // * sweeps through increasing angles
    // *
    // * @param x the absolute x coordinate of the final point of the arc.
    // * @param y the absolute y coordinate of the final point of the arc.
    // */
    // public synchronized void arcTo(float rx, float ry,
    // float angle,
    // boolean largeArcFlag,
    // boolean sweepFlag,
    // float x, float y) {
    //
    // // Ensure radii are valid
    // if (rx == 0 || ry == 0) {
    // lineTo(x, y);
    // return;
    // }
    //
    // checkMoveTo(); // check if prev command was moveto
    //
    // // Get the current (x, y) coordinates of the path
    // double x0 = cx;
    // double y0 = cy;
    // if (x0 == x && y0 == y) {
    // // If the endpoints (x, y) and (x0, y0) are identical, then this
    // // is equivalent to omitting the elliptical arc segment entirely.
    // return;
    // }
    //
    // Arc2D arc = computeArc(x0, y0, rx, ry, angle,
    // largeArcFlag, sweepFlag, x, y);
    // if (arc == null) return;
    //
    // AffineTransform t = AffineTransform.getRotateInstance
    // (Math.toRadians(angle), arc.getCenterX(), arc.getCenterY());
    // Shape s = t.createTransformedShape(arc);
    // path.append(s, true);
    //
    // makeRoom(7);
    // types [numSeg++] = ExtendedPathIterator.SEG_ARCTO;
    // values[numVals++] = rx;
    // values[numVals++] = ry;
    // values[numVals++] = angle;
    // values[numVals++] = largeArcFlag?1:0;
    // values[numVals++] = sweepFlag?1:0;
    // cx = values[numVals++] = x;
    // cy = values[numVals++] = y;
    // }

    /**
     * This constructs an unrotated Arc2D from the SVG specification of an
     * Elliptical arc. To get the final arc you need to apply a rotation
     * transform such as:
     * 
     * AffineTransform.getRotateInstance (angle, arc.getX()+arc.getWidth()/2,
     * arc.getY()+arc.getHeight()/2);
     */
    public static Arc2D computeArc(double x0, double y0, double rx, double ry,
            double angle, boolean largeArcFlag, boolean sweepFlag, double x,
            double y) {
        //
        // Elliptical arc implementation based on the SVG specification notes
        //

        // Compute the half distance between the current and the final point
        double dx2 = (x0 - x) / 2.0;
        double dy2 = (y0 - y) / 2.0;
        // Convert angle from degrees to radians
        angle = Math.toRadians(angle % 360.0);
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);

        //
        // Step 1 : Compute (x1, y1)
        //
        double x1 = (cosAngle * dx2 + sinAngle * dy2);
        double y1 = (-sinAngle * dx2 + cosAngle * dy2);
        // Ensure radii are large enough
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double Prx = rx * rx;
        double Pry = ry * ry;
        double Px1 = x1 * x1;
        double Py1 = y1 * y1;
        // check that radii are large enough
        double radiiCheck = Px1 / Prx + Py1 / Pry;
        if (radiiCheck > 1) {
            rx = Math.sqrt(radiiCheck) * rx;
            ry = Math.sqrt(radiiCheck) * ry;
            Prx = rx * rx;
            Pry = ry * ry;
        }

        //
        // Step 2 : Compute (cx1, cy1)
        //
        double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
        double sq = ((Prx * Pry) - (Prx * Py1) - (Pry * Px1))
                / ((Prx * Py1) + (Pry * Px1));
        sq = (sq < 0) ? 0 : sq;
        double coef = (sign * Math.sqrt(sq));
        double cx1 = coef * ((rx * y1) / ry);
        double cy1 = coef * -((ry * x1) / rx);

        //
        // Step 3 : Compute (cx, cy) from (cx1, cy1)
        //
        double sx2 = (x0 + x) / 2.0;
        double sy2 = (y0 + y) / 2.0;
        double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
        double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

        //
        // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
        //
        double ux = (x1 - cx1) / rx;
        double uy = (y1 - cy1) / ry;
        double vx = (-x1 - cx1) / rx;
        double vy = (-y1 - cy1) / ry;
        double p, n;
        // Compute the angle start
        n = Math.sqrt((ux * ux) + (uy * uy));
        p = ux; // (1 * ux) + (0 * uy)
        sign = (uy < 0) ? -1.0 : 1.0;
        double angleStart = Math.toDegrees(sign * Math.acos(p / n));

        // Compute the angle extent
        n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        p = ux * vx + uy * vy;
        sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
        double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
        if (!sweepFlag && angleExtent > 0) {
            angleExtent -= 360f;
        } else if (sweepFlag && angleExtent < 0) {
            angleExtent += 360f;
        }
        angleExtent %= 360f;
        angleStart %= 360f;

        //
        // We can now build the resulting Arc2D in double precision
        //
        Arc2D.Double arc = new Arc2D.Double();
        arc.x = cx - rx;
        arc.y = cy - ry;
        arc.width = rx * 2.0;
        arc.height = ry * 2.0;
        arc.start = -angleStart;
        arc.extent = -angleExtent;

        return arc;
    }

    // /**
    // * Implements {@link
    // *
    // org.apache.batik.parser.PathHandler#arcAbs(float,float,float,boolean,boolean,float,float)}.
    // */
    // public void arcAbs(float rx, float ry,
    // float xAxisRotation,
    // boolean largeArcFlag, boolean sweepFlag,
    // float x, float y) throws ParseException {
    //
    // // Ensure radii are valid
    // if (rx == 0 || ry == 0) {
    // linetoAbs(x, y);
    // return;
    // }
    //
    // // Get the current (x, y) coordinates of the path
    // double x0 = lastAbs.getX();
    // double y0 = lastAbs.getY();
    // if (x0 == x && y0 == y) {
    // // If the endpoints (x, y) and (x0, y0) are identical, then this
    // // is equivalent to omitting the elliptical arc segment entirely.
    // return;
    // }
    //
    // Arc2D arc = ExtendedGeneralPath.computeArc(x0, y0, rx, ry, xAxisRotation,
    // largeArcFlag, sweepFlag, x, y);
    // if (arc == null) return;
    //
    // AffineTransform t = AffineTransform.getRotateInstance
    // (Math.toRadians(xAxisRotation), arc.getCenterX(), arc.getCenterY());
    // Shape s = t.createTransformedShape(arc);
    //
    // PathIterator pi = s.getPathIterator(new AffineTransform());
    // float[] d = {0,0,0,0,0,0};
    // int i = -1;
    //
    // while (!pi.isDone()) {
    // i = pi.currentSegment(d);
    //
    // switch (i) {
    // case PathIterator.SEG_CUBICTO:
    // curvetoCubicAbs(d[0],d[1],d[2],d[3],d[4],d[5]);
    // break;
    // }
    // pi.next();
    // }
    // lastAbs.setPathSegType(SVGPathSeg.PATHSEG_ARC_ABS);
    // }
    // }
    //    
    //    
}
