package ca.ualberta.cs.corallook;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JLabel;

import uk.ac.liverpool.fab4.Fab4;

public class ListenerForFab4FrameChange implements ComponentListener{

	CoRALFrame coralFrame;
	
	
	public ListenerForFab4FrameChange(CoRALFrame internalFrame){
		coralFrame = internalFrame;		
	}
	
	public void componentHidden(ComponentEvent e) {
		coralFrame.setVisible(false);
	}

	public void componentMoved(ComponentEvent event) {
		Component outerFrame = event.getComponent();
		
		coralFrame.resize(outerFrame.getX(), outerFrame.getY(),outerFrame.getWidth(), outerFrame.getHeight());
//		coralLabel.setPreferredSize(new Dimension(outerFrame.getWidth(), coralLabel.getHeight()));
//		coralLabel.setBounds(outerFrame.getX(), outerFrame.getY(), outerFrame.getWidth(), (int)(outerFrame.getHeight()*0.2));
//		coralLabel.setSize(outerFrame.getWidth()/*+CoRALFrame.REL_WIDTH*/, (int)(outerFrame.getHeight()*0.2));
		coralFrame.validate();
		outerFrame.validate();
	}

	public void componentResized(ComponentEvent event) {
		componentMoved(event);
//		Component outerFrame = event.getComponent();
		
//		coralFrame.resize(/*outerFrame.getX(), outerFrame.getY(),*/outerFrame.getWidth(), outerFrame.getHeight());
		
//		coralLabel.setPreferredSize(new Dimension(outerFrame.getWidth(), coralLabel.getHeight()));
//		coralLabel.setBounds(outerFrame.getX()/*+CoRALFrame.REL_LOC_X*/, outerFrame.getY()/*+CoRALFrame.REL_LOC_Y*/, outerFrame.getWidth()/*+CoRALFrame.REL_WIDTH*/, (int)(outerFrame.getHeight()*0.2));
		
		
//		coralLabel.setSize(outerFrame.getWidth()/*+CoRALFrame.REL_WIDTH*/, (int)(outerFrame.getHeight()*0.2));
//		coralFrame.validate();
//		outerFrame.validate();
	}

	public void componentShown(ComponentEvent e) {
		coralFrame.setVisible(true);		
	}
	

}
