package org.apache.poi.hslf.model.autoshape;

import java.awt.Font;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.IllegalPathStateException;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.hslf.model.Shape;
import org.apache.poi.hslf.model.ShapeOutline;

/**
 * 
 * This class will parse the geometric properties of autoshapes, and use them to
 * create a Java shape.
 * 
 * @author fabio
 * 
 */
public class AutoShapeParser {


    private static boolean nostroke;

    public static List<AutoShapeDefinition> parseShapes(Reader rr)
    throws FileNotFoundException, IOException {
        List <AutoShapeDefinition> as = new LinkedList<AutoShapeDefinition>();

        String separator = "Internal Name: ";
        String scf = "Shaped Concentric Fill :";
        String joins = "Joins: ";
        String endcaps = "Endcaps: ";
        String adj = "Adjustments: ";
        String gp = "Geometric properties: ";
        String path = "Path ";
        String gf = "Guide Formulas ";
        String adjv = "Adjustment Values ";
        String connloc = "Connector Locations ";
        String handles = "Handles ";
        String textbox = "Text Box ";
        String connAng = "Connector Angles ";
        AutoShapeDefinition asd;
        asd = null;
        BufferedReader r = new BufferedReader(rr);
        String line;
        String lastLine = "";
        while ((line = r.readLine())!=null) {
            if (line.startsWith(separator)){
                //printModel(System.out, asd);
                asd = new AutoShapeDefinition();
                asd.name = lastLine.trim().replaceAll(" " ,"").replace(":", "").replaceAll("/","").replaceAll("-","");
                asd.internalName = line.substring(separator.length(), line.length()-1);
                //System.out.println( asd.internalName + k++);
                as.add(asd);
            } else if (line.startsWith(scf)) {
                asd.shapeConcentricFill = line.substring(scf.length()).contains("Yes");
            }  else if (line.startsWith(joins)) {
                asd.jointype = line.substring(joins.length(), line.length()-2);
            }  else if (line.startsWith(endcaps)) {
                asd.endcap = line.substring(endcaps.length(), line.length()-2);
            }  else if (line.startsWith(adj)) {
                asd.adjustment = line.substring(adj.length(), line.length());
                while ((line = r.readLine())!=null && !line.startsWith(gp) )
                    asd.adjustment+=line;
            } if (line.startsWith(gp)) {

                asd.geometricProperties = line.substring(gp.length(), line.length()).trim();
                while ((line = r.readLine())!=null &&  !line.startsWith(path)  && !line.startsWith(separator)) {
                    asd.geometricProperties+=line;
                    lastLine = line;
                }
                if (line.startsWith(separator)){
                    //printModel(System.out, asd);
                    asd = new AutoShapeDefinition();
                    asd.name = lastLine.trim().replaceAll(" " ,"").replace(":", "").replaceAll("/","").replaceAll("-","");
                    asd.internalName = line.substring(separator.length(), line.length()).trim();
                    //System.out.println( asd.internalName + k++);
                    as.add(asd);
                }
                if (line.startsWith(path)) {
                    asd.path = line.substring(path.length(), line.length()-1);
                    while ((line = r.readLine())!=null && !line.startsWith(gf) && !line.startsWith(connloc)&& !line.startsWith(connAng)&& !line.startsWith(textbox)&& !line.startsWith(handles)&& !line.startsWith(adjv))
                        asd.path+=line.trim();
                    if (line.startsWith(gf)){ 
                        asd.guideFormulas = line.substring(gf.length(), line.length()); 
                        asd.guideFormulas+="\n";
                        while ((line = r.readLine())!=null && !line.startsWith(adjv))
                            asd.guideFormulas+=line.substring(0,line.length()) + "\n";
                    } //else System.out.println(line); 
                    if (line.startsWith(adjv)) {
                        asd.adjustmentValues = line.substring(adjv.length(), line.length()-1);
                        //System.out.println(asd.adjustmentValues);
                    } //else System.out.println(line);
                }
                // else System.out.println(line);

            }

            lastLine = line;


        }
        return as;
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
                    path.setWindingRule(path.WIND_NON_ZERO);
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
                        //System.out.println(sa + " " + ea);
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

                        //                        System.out.println("Q"+(start?"x":"y")+" ax - rx =" + (dx) + " ay - ry ="
                        //                                + (dy));
                        //                        System.out.println(new Point2D.Double(rx, ry));
                        //                        System.out.println(new Point2D.Double(ax, ay));
                        //                        System.out.println(tanX);
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


}
