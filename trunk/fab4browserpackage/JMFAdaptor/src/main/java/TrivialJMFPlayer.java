import javax.media.*;

import uk.ac.liverpool.media.JMFInitializer;

import java.io.File;
import java.awt.*;
	 
public class TrivialJMFPlayer extends Frame {
	
	public static void main (String[] args) {
		try {
		    JMFInitializer.initJMF();
	            Frame f = new TrivialJMFPlayer();
	            f.pack();
	            f.setVisible (true);
	    } catch (Exception e) {
	            e.printStackTrace();
	    }
	}
	public TrivialJMFPlayer() 
	        throws java.io.IOException,
	               java.net.MalformedURLException,
	               javax.media.MediaException {
	    FileDialog fd = new FileDialog
	            (this, "TrivialJMFPlayer", FileDialog.LOAD);
	    fd.setVisible(true);
	    File f = new File (fd.getDirectory(), fd.getFile());
	    Player p = Manager.createRealizedPlayer
	        (f.toURI().toURL());
	    Component c = p.getVisualComponent();
	    add (c);
	    p.start();
	}
}


