package ca.ualberta.cs.corallook;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.event.ChangeListener;

public class CoRALFrame extends JInternalFrame{ //implements ComponentListener{

	public static final int REL_LOC_X = 0;
	public static final int REL_LOC_Y = 0;//+10;
	public static final int REL_WIDTH = -5;
	public static final int REL_HEIGHT = -80;
	
	JLabel coralLabel;
	
	public CoRALFrame(JLabel logo){
		//super("internal", true, true, true);
		coralLabel = logo;
		setBackground(Color.black);
		
		//setTitle("Internal");		
		//setIconifiable(true);
		
	}
	
	/*public void setSize(Dimension size){
		setSize(size.width-1000, size.height-100);
		setLocation(5, 5);
	}*/
	
	/*public void resize(int width, int height){
		setPreferredSize(new Dimension(width+CoRALFrame.REL_WIDTH, height+CoRALFrame.REL_HEIGHT));        
//        internal.setSize(s.width-50, s.height-50);
        
//        if (p!=null)
//            internal.setBounds(p.x+CoRALFrame.REL_LOC_X, p.y+CoRALFrame.REL_LOC_Y, s.width+CoRALFrame.REL_WIDTH, s.height+CoRALFrame.REL_HEIGHT);
//        else
//    	setLocation(CoRALFrame.REL_LOC_X, CoRALFrame.REL_LOC_Y);
	}*/
	
	/**
	 * change location and size of the internal frame (coralframe) based on given coordinates and size of the parent frame (fab4)
	 * @param x location of fab4 frame
	 * @param y location of fab4 frame
	 * @param width of fab4 frame
	 * @param height of fab4 frame
	 */
	public void resize(int x, int y, int width, int height){
		setPreferredSize(new Dimension(width+CoRALFrame.REL_WIDTH, height+CoRALFrame.REL_HEIGHT));
//		coralLabel.setPreferredSize(new Dimension(width, (int)((height)*0.05)));
//		coralLabel.revalidate();
//        coralLabel.setPreferredSize(new Dimension(s.width, coralLabel.getHeight()));
        
//        if (p!=null)
//        setBounds(x+CoRALFrame.REL_LOC_X, y+CoRALFrame.REL_LOC_Y, width+CoRALFrame.REL_WIDTH, height+CoRALFrame.REL_HEIGHT);
        setBounds(CoRALFrame.REL_LOC_X, CoRALFrame.REL_LOC_Y, width+CoRALFrame.REL_WIDTH, height+CoRALFrame.REL_HEIGHT);
//        coralLabel.setBounds(x+CoRALFrame.REL_LOC_X, y, width, (int)((height)*0.05));
//        coralLabel.revalidate();
    
	}
	
	@Override
	public void setVisible(boolean aFlag) {
		super.setVisible(aFlag);
		if(coralLabel!=null)
			coralLabel.setVisible(aFlag);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//initializes
		
		//JFrame innerFrame = new JIn

	}

}
