/******************************************************************************
 *  
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 3 of the License, or (at your option) 
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Author     : Jerome Fuselier
 * Creation   : September 2011
 *  
 *****************************************************************************/

package uk.ac.liverpool.MXF;

import java.io.File;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

public class TranscodePNGDialog extends JDialog {
    
	private static final long serialVersionUID = -840129766586830008L;
	
	private JButton b_cancel;
    private JButton b_ok;
    private JComboBox cb_format;
    private JFileChooser fc;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JPanel p_but;
    private JPanel p_main;
    private JPanel p_options;
    private JTextField tf_max;
    private JTextField tf_min;
    private JTabbedPane tp;    
	
	boolean res = false;
    
	
	public TranscodePNGDialog(JFrame parent, int minFrame, int maxFrame) {
		super(parent, "Choose an output directory", true);   
		setTitle("Choose an output directory");
		initComponents(minFrame, maxFrame);
    } 


    private void initComponents(int minFrame, int maxFrame) {
    	
    	 p_main = new JPanel();
         tp = new JTabbedPane();
         fc = new JFileChooser();
         p_options = new JPanel();
         jLabel1 = new JLabel();
         jLabel2 = new JLabel();
         jLabel3 = new JLabel();
         tf_min = new JTextField();
         tf_max = new JTextField();
         cb_format = new JComboBox();
         p_but = new JPanel();
         b_ok = new JButton();
         b_cancel = new JButton();         

         setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
         setTitle("Choose an output directory");

         p_main.setBorder(new SoftBevelBorder(BevelBorder.RAISED));

         fc.setAcceptAllFileFilterUsed(false);
         fc.setControlButtonsAreShown(false);
         fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
         fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
         tp.addTab("Output Directory", fc);
         jLabel1.setText("Start Frame:");
         jLabel2.setText("End Frame:");
         jLabel3.setText("File Format:");
         tf_min.setText(Integer.toString(minFrame));
         tf_max.setText(Integer.toString(maxFrame));

         cb_format.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PNG", "JPG", "GIF" }));

         GroupLayout p_optionsLayout = new GroupLayout(p_options);
         p_options.setLayout(p_optionsLayout);
         p_optionsLayout.setHorizontalGroup(
             p_optionsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
             .addGroup(p_optionsLayout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(p_optionsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                     .addComponent(jLabel1)
                     .addComponent(jLabel3)
                     .addComponent(jLabel2))
                 .addGap(18, 18, 18)
                 .addGroup(p_optionsLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                     .addComponent(cb_format, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(tf_max)
                     .addComponent(tf_min, GroupLayout.PREFERRED_SIZE, 129, GroupLayout.PREFERRED_SIZE))
                 .addContainerGap(300, Short.MAX_VALUE))
         );
         p_optionsLayout.setVerticalGroup(
             p_optionsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
             .addGroup(p_optionsLayout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(p_optionsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                     .addComponent(jLabel1)
                     .addComponent(tf_min, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                 .addGap(18, 18, 18)
                 .addGroup(p_optionsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                     .addComponent(tf_max, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel2))
                 .addGap(18, 18, 18)
                 .addGroup(p_optionsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                     .addComponent(jLabel3)
                     .addComponent(cb_format, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                 .addContainerGap(186, Short.MAX_VALUE))
         );

         tp.addTab("Options", p_options);

         GroupLayout p_mainLayout = new GroupLayout(p_main);
         p_main.setLayout(p_mainLayout);
         p_mainLayout.setHorizontalGroup(
             p_mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
             .addGap(0, 565, Short.MAX_VALUE)
             .addGroup(p_mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                 .addGroup(p_mainLayout.createSequentialGroup()
                     .addGap(5, 5, 5)
                     .addComponent(tp, GroupLayout.PREFERRED_SIZE, 559, GroupLayout.PREFERRED_SIZE)
                     .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
         );
         p_mainLayout.setVerticalGroup(
             p_mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
             .addGap(0, 404, Short.MAX_VALUE)
             .addGroup(p_mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                 .addGroup(p_mainLayout.createSequentialGroup()
                     .addGap(19, 19, 19)
                     .addComponent(tp, GroupLayout.PREFERRED_SIZE, 366, GroupLayout.PREFERRED_SIZE)
                     .addContainerGap(19, Short.MAX_VALUE)))
         );

         p_but.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

         b_ok.setText("Ok");
         b_ok.setMaximumSize(new java.awt.Dimension(90, 30));
         b_ok.setMinimumSize(new java.awt.Dimension(90, 30));
         b_ok.setPreferredSize(new java.awt.Dimension(90, 30));
         b_ok.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 b_okActionPerformed(evt);
             }
         });

         b_cancel.setText("Cancel");
         b_cancel.setMaximumSize(new java.awt.Dimension(90, 30));
         b_cancel.setMinimumSize(new java.awt.Dimension(90, 30));
         b_cancel.setPreferredSize(new java.awt.Dimension(90, 30));
         b_cancel.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 b_cancelActionPerformed(evt);
             }
         });

         GroupLayout p_butLayout = new GroupLayout(p_but);
         p_but.setLayout(p_butLayout);
         p_butLayout.setHorizontalGroup(
             p_butLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
             .addGroup(GroupLayout.Alignment.TRAILING, p_butLayout.createSequentialGroup()
                 .addContainerGap(355, Short.MAX_VALUE)
                 .addComponent(b_cancel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                 .addGap(18, 18, 18)
                 .addComponent(b_ok, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                 .addContainerGap())
         );
         p_butLayout.setVerticalGroup(
             p_butLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
             .addGroup(p_butLayout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(p_butLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                     .addComponent(b_ok, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                     .addComponent(b_cancel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                 .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
         );

         GroupLayout layout = new GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                     .addComponent(p_main, GroupLayout.Alignment.LEADING, 0, 571, Short.MAX_VALUE)
                     .addComponent(p_but, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                 .addContainerGap(12, Short.MAX_VALUE))
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(GroupLayout.Alignment.LEADING)
             .addGroup(layout.createSequentialGroup()
                 .addContainerGap()
                 .addComponent(p_main, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(p_but, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                 .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
         );

         pack();
    }
    
    private void b_cancelActionPerformed(java.awt.event.ActionEvent evt) {
    	res = false;
    	this.dispose();
    }

	private void b_okActionPerformed(java.awt.event.ActionEvent evt) {
		res = true;
    	this.dispose();
	}
    
    
    public boolean getRes() {
    	return res;
    }
    
    public File getDirectory() {
    	return fc.getSelectedFile();
    }
    
    public int getMin() {
    	return Integer.parseInt(tf_min.getText());
    }

    public int getMax() {
    	return Integer.parseInt(tf_max.getText());
    }
    
    public String getFileFormat() {
    	return (String)cb_format.getSelectedItem();
    }

	public static void main(String[] args) {
		TranscodePNGDialog f = new TranscodePNGDialog(new JFrame(), 0, 5/*1500*/);
		f.setVisible(true);
		
		if (f.getRes()) {
			System.out.println(f.getDirectory() + " " + f.getMax() +   " " + f.getMin() + " " + f.getFileFormat());
		}
		
		ProgressMonitor mon = new ProgressMonitor(new JFrame(), "message", "note", 0, 100);
		
		for (int i = 0 ; i < 100 ; i++) {
			try {
				Thread.sleep(100);
			}
			catch  (Exception e) {}
			mon.setProgress(i);
		}
				
	}
		
	
}