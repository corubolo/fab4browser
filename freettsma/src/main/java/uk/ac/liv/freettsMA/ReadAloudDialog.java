/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms 
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, 
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  * 
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 *  
 *******************************************************************************/
/**
 * Author: Fabio Corubolo - f.corubolo@liv.ac.uk
 * (c) 2007 University of Liverpool
 */
package uk.ac.liv.freettsMA;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author fabio
 * 
 */
public class ReadAloudDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private JPanel jContentPane = null;

    private JPanel jPanel1 = null;

    private JPanel jPanel2 = null;

    private JPanel jPanel3 = null;

    private JButton jButton = null;

    private JButton jButton1 = null;

    private JButton jButton2 = null;

    private JLabel jLabel = null;

    private JSlider jSlider = null;

    private JLabel jLabel1 = null;

    private JSlider jSlider1 = null;

    FreettsReader rt;

    boolean pause = false;

    String text;

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Window#dispose()
     */
    @Override
    public void dispose() {
        // TODO Auto-generated method stub
        stop();
        super.dispose();
    }

    public void pause() {
        pause = !pause;
        if (rt != null) {
            if (pause) {
                rt.pause();
            } else {
                rt.resume();
            }
        }
    }

    public void stop() {

        if (rt != null) {
            rt.stop();
        }
    }

    public void restart() {

        if (rt != null) {
            rt.stop();
            rt.readText(text);
        }
    }

    /**
     * @param owner
     */
    public ReadAloudDialog(Frame owner, String text) {
        super(owner);
        rt = new FreettsReader();
        rt.readText(text);
        this.text = text;
        initialize();

    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setSize(218, 136);
        setTitle("Read out loud"); // Generated
        setName("Read out loud"); // Generated
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // Generated
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                rt = null;
                System.gc();
                super.windowClosed(e);
            }
        });
        setContentPane(getJContentPane());
        pack();
        setVisible(true); // Generated

    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            GridLayout gridLayout = new GridLayout();
            gridLayout.setRows(3); // Generated
            gridLayout.setHgap(4); // Generated
            gridLayout.setVgap(3); // Generated
            gridLayout.setColumns(1); // Generated
            jContentPane = new JPanel();
            jContentPane.setLayout(gridLayout); // Generated
            jContentPane.add(getJPanel1(), null); // Generated
            jContentPane.add(getJPanel2(), null); // Generated
            jContentPane.add(getJPanel3(), null); // Generated

        }
        return jContentPane;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel1() {
        if (jPanel1 == null) {
            try {
                jPanel1 = new JPanel();
                jPanel1.setLayout(new FlowLayout()); // Generated
                jPanel1.add(getJButton(), null); // Generated
                jPanel1.add(getJButton1(), null); // Generated
                jPanel1.add(getJButton2(), null); // Generated
            } catch (java.lang.Throwable e) {
                // TODO: Something
            }
        }
        return jPanel1;
    }

    /**
     * This method initializes jPanel2
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel2() {
        if (jPanel2 == null) {
            try {
                jLabel = new JLabel();
                jLabel.setText("Volume"); // Generated
                jLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Generated
                jLabel.setDisplayedMnemonic(KeyEvent.VK_UNDEFINED); // Generated
                jPanel2 = new JPanel();
                jPanel2
                        .setLayout(new BoxLayout(getJPanel2(), BoxLayout.X_AXIS)); // Generated
                jPanel2.setVisible(false); // Generated
                jPanel2.add(jLabel, null); // Generated
                jPanel2.add(getJSlider(), null); // Generated
            } catch (java.lang.Throwable e) {
                // TODO: Something
            }
        }
        return jPanel2;
    }

    /**
     * This method initializes jPanel3
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel3() {
        if (jPanel3 == null) {
            try {
                jLabel1 = new JLabel();
                jLabel1.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Generated
                jLabel1.setText("Rate"); // Generated
                jLabel1.setPreferredSize(new Dimension(57, 15)); // Generated
                jLabel1
                        .setToolTipText("The rate is in words per minute. You must restart to apply"); // Generated
                jLabel1.setDisplayedMnemonic(KeyEvent.VK_UNDEFINED); // Generated
                jPanel3 = new JPanel();
                jPanel3.setLayout(new BorderLayout()); // Generated
                jPanel3.add(jLabel1, BorderLayout.WEST); // Generated
                jPanel3.add(getJSlider1(), BorderLayout.CENTER); // Generated
            } catch (java.lang.Throwable e) {
                // TODO: Something
            }
        }
        return jPanel3;
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButton() {
        if (jButton == null) {
            try {
                jButton = new JButton();
                jButton.setText("Pause"); // Generated
                jButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        pause();
                        if (!pause) {
                            jButton.setText("Pause");
                        } else {
                            jButton.setText("Resume");
                        }

                    }

                });
            } catch (java.lang.Throwable e) {
                // TODO: Something
            }
        }
        return jButton;
    }

    /**
     * This method initializes jButton1
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButton1() {
        if (jButton1 == null) {
            try {
                jButton1 = new JButton();
                jButton1.setText("Stop"); // Generated
                jButton1.setVisible(false); // Generated
                jButton1.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        stop();

                    }

                });
            } catch (java.lang.Throwable e) {
                // TODO: Something
            }
        }
        return jButton1;
    }

    /**
     * This method initializes jButton2
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButton2() {
        if (jButton2 == null) {
            try {
                jButton2 = new JButton();
                jButton2.setText("Restart"); // Generated
                jButton2.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        restart();
                    }

                });
            } catch (java.lang.Throwable e) {
                // TODO: Something
            }
        }
        return jButton2;
    }

    /**
     * This method initializes jSlider
     * 
     * @return javax.swing.JSlider
     */
    private JSlider getJSlider() {
        if (jSlider == null) {
            try {
                jSlider = new JSlider();
                jSlider.setMaximum(100);
                jSlider.setMinimum(0);
                jSlider.setValue((int) (rt.getVolume() * 100));
                jSlider.setVisible(false); // Generated
                jSlider.addChangeListener(new ChangeListener() {

                    public void stateChanged(ChangeEvent e) {
                        float t = jSlider.getValue() / 100f;
                        rt.setVolume(t);
                        System.out.println(t);
                    }

                });

            } catch (java.lang.Throwable e) {
                // TODO: Something
            }
        }
        return jSlider;
    }

    /**
     * This method initializes jSlider1
     * 
     * @return javax.swing.JSlider
     */
    private JSlider getJSlider1() {
        if (jSlider1 == null) {
            try {
                jSlider1 = new JSlider();
                jSlider1.setMaximum(300);
                jSlider1.setMinimum(0);
                jSlider1.setPaintLabels(true); // Generated
                jSlider1.setPaintTicks(true); // Generated
                jSlider1.setMajorTickSpacing(100); // Generated
                jSlider1.setMinorTickSpacing(25); // Generated
                jSlider1.setFont(new Font("Dialog", Font.PLAIN, 10)); // Generated
                jSlider1
                        .setToolTipText("Words per minutes. You must restart to apply"); // Generated
                jSlider1.setValue((int) rt.getRate());
                jSlider1.addChangeListener(new ChangeListener() {

                    public void stateChanged(ChangeEvent e) {
                        float t = jSlider1.getValue();
                        rt.setRate(t);
                        System.out.println(t);
                    }

                });

            } catch (java.lang.Throwable e) {
                // TODO: Something
            }
        }
        return jSlider1;
    }

} // @jve:decl-index=0:visual-constraint="108,54"
