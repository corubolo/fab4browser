package org.apache.poi.hslf.model.autoshape;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class TestAutoshapes {



    
    public static void main(String[] args) throws IOException {
        int k;
        List<AutoShapeDefinition> as = AutoShapeParser.parseShapes(new InputStreamReader(AutoShapeParser.class.getResourceAsStream("/shapeDescriptions.txt")));


        k = 1;

        BufferedReader br = new BufferedReader(new InputStreamReader(AutoShapeParser.class.getResourceAsStream("/broken.txt")));
        String sd;
        Set<String> broke = new HashSet<String>(12);
        while ((sd =br.readLine())!=null){
            broke.add(sd);
        }
        for (AutoShapeDefinition a: as) {
            System.out.println(k++ + " -- " + a.internalName
                     + " - " + a.name);

            if (a.path==null){
                System.out.println(a.internalName + " has null path!");
                
                continue;

            }

            try {
                if (!broke.contains(a.internalName))
                    continue;

                java.awt.Shape s = AutoShapeParser.parseShapeData(a.path, a.guideFormulas, a.adjustmentValues).getOutline(
                        null);
                AffineTransform at = new AffineTransform();
                at.translate(5, 5);
                at.scale(1.0f / 50, 1.0f / 50);
                final java.awt.Shape s2 = at.createTransformedShape(s);
                final JFrame f = new JFrame(a.internalName);
                final String name = a.internalName;
                JPanel fp = new JPanel() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -4784371590751985202L;

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
