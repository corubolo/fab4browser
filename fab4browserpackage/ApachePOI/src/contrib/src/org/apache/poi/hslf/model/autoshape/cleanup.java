package org.apache.poi.hslf.model.autoshape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.math.stat.descriptive.moment.GeometricMean;
import org.apache.poi.hslf.model.ShapeTypes;

public class cleanup {

    public static class AutoShapeDefinition {
        String internalName;
        boolean shapeConcentricFill;
        String endcap;
        String jointype;
        String adjustment; 
        String geometricProperties;
        String connectorLocations;
        String path;
        String guideFormulas;
        String adjustmentValues;
        String textboxRectangle;
        String handles;

    }


    public static void printModel (PrintStream ps, Object o){
        Field[] fi = o.getClass().getDeclaredFields();
        for (Field f: fi){
            if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers()))
                ps.print(f.getName() + " = ");
            try {
                ps.println("\"" + f.get(o) + "\"");
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        ps.println();

    }

    public static void main(String[] args) throws IOException {
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
        BufferedReader r = new BufferedReader(new FileReader(args[0]));
        String line;
        boolean ingp = false;
        int k =0 ;
        while ((line = r.readLine())!=null) {
            if (line.startsWith(separator)){
                //printModel(System.out, asd);
                asd = new AutoShapeDefinition();
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
                ingp = true;
                asd.geometricProperties = line.substring(gp.length(), line.length()).trim();
                while ((line = r.readLine())!=null &&  !line.startsWith(path)  && !line.startsWith(separator)) {
                    asd.geometricProperties+=line;
                }
                if (line.startsWith(separator)){
                    //printModel(System.out, asd);
                    asd = new AutoShapeDefinition();
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




        }

        
        k = 0;
        
        BufferedReader br = new BufferedReader(new FileReader("brokken.txt"));
        String sd;
        Set<String> broke = new HashSet<String>(12);
        while ((sd =br.readLine())!=null){
            broke.add(sd);
        }
        for (AutoShapeDefinition a: as) {
            if (a.path==null){System.out.println(k++  + a.internalName + " has null path!");
            continue;

            }

            try {
                if (!broke.contains(a.internalName))
                    continue;
                //if (! (a.internalName.contains("EllipseRibbon"))) continue;
                
                //if (! (a.internalName.contains("CloudCallout"))) continue;
                //if (! (a.internalName.contains("BentArrow"))) continue;
               //if (! (a.path.contains("at") || a.path.contains("ar"))) continue;
                //if (! (a.internalName.contains("Sun")/* || a.path.contains("ar")*/)) continue;
                System.out.println(k++  + a.internalName );
                java.awt.Shape s = AutoShapeParser.parseShapeData(a.path, a.guideFormulas, a.adjustmentValues).getOutline(
                        null);
                printModel(System.out, a);
                AffineTransform at = new AffineTransform();
                at.translate(5, 5);
                at.scale(1.0f / 50, 1.0f / 50);
                final java.awt.Shape s2 = at.createTransformedShape(s);
                final JFrame f = new JFrame(a.internalName);
                final String name = a.internalName;
                JPanel fp = new JPanel() {
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
                    
                };
                f.addKeyListener(new KeyListener() {
                    
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyChar()==' ')
                            ;
                        else{   System.out.println(name);
                             
                        }
                        f.dispose();
                            
                        
                    }
                    
                    public void keyReleased(KeyEvent e) {
                        // TODO Auto-generated method stub
                        
                    }
                    
                    public void keyPressed(KeyEvent e) {
                        // TODO Auto-generated method stub
                        
                    }
                });
                f.getContentPane().add(fp);
                f.pack();
                f.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(a.path);
            }
        }
        
    
    }
    
    
}
