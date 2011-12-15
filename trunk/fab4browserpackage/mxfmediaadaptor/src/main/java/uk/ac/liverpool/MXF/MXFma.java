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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;

import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.StyleSheet;
import phelps.awt.Colors;
import uk.ac.liv.util.LogFactory;
import uk.ac.liverpool.fab4.Fab4;



public class MXFma extends MediaAdaptor implements ActionListener {
	
	 Document doc;
	 MXFDocument mxfdoc; 
	 JMenu fileMenu = null;
	 JMenuItem itemTranscodePNG = null;
	 JMenuItem itemTranscodeWAV = null;
	 JMenuItem itemTranscode = null;
	 boolean menuModified = false; 	 
	 
	 private static String transcodePngCmd  = "Extract frames";
	 private static String transcodeWaveCmd = "Extract Audio";
	 private static String transcodeCmd     = "Transcode";
	 

	 
	 
    public Object parse(INode parent) throws Exception {
    	LogFactory.useLogFile = false;
    	
        doc = parent.getDocument();
        if (doc.getFirstChild() != null) {
            doc.clear();
        }
        final StyleSheet ss = doc.getStyleSheet();
        CLGeneral gs = new CLGeneral();
        gs.setForeground(Colors.getColor(getAttr("foreground"), Color.WHITE));
        gs.setBackground(Colors.getColor(getAttr("background"), Color.BLACK));
        gs.setPadding(8);
        ss.put(doc.getName(), gs);
        

     
        
        
        
        
        Map<String, Object> attr = new HashMap<String, Object>(1);
        attr.put("resize", false);
        attr.put("embedded", false);
        attr.put("uri", getURI().toString());
        doc.uri = getURI();
        
        
        if (getURI() == null) {
            throw new IOException("File not found");
        }
        
        mxfdoc = new MXFDocument("mxf", attr, doc);
        
        doc.putAttr("mxfdoc", mxfdoc);

        
        Layer ll = doc.getLayer(Layer.PERSONAL);
        if (ll != null) {
            ll.destroy();
        }
        
        if (!menuModified) {
        	insertMenu(Fab4.getMVFrame(getBrowser()));
        }

        return parent;
    }
	 
	 
    public void insertMenu(JFrame frame) {
        JMenuBar menuBar = frame.getJMenuBar();
        
        for (MenuElement me : menuBar.getSubElements()) {
        	JMenu m = (JMenu) me;
        	
        	if (m.getText() == "File") {
        		fileMenu = m;        		

				for ( int i = 0 ; i < m.getItemCount() ; i++ ) {
					JMenuItem mi = m.getItem(i);
					if (mi != null) {
	            		if (mi.getText() == "Save as") {	
	            			menuModified = true;
	            			Icon saveIcon = mi.getIcon();
							itemTranscodePNG = new JMenuItem(transcodePngCmd, saveIcon);
							itemTranscodeWAV = new JMenuItem(transcodeWaveCmd, saveIcon);
							itemTranscode = new JMenuItem(transcodeCmd, saveIcon);
							itemTranscodePNG.addActionListener(this);
							itemTranscodeWAV.addActionListener(this);
							itemTranscode.addActionListener(this);
							fileMenu.add(itemTranscodePNG, i+1);
							fileMenu.add(itemTranscodeWAV, i+2);
							fileMenu.add(itemTranscode, i+3);
							break;
	            		}           	
					}
            	}        		
        	}
        }
        
        
    }
    
    
    
	public void close() throws IOException {
		
		//mxfdoc.stop_thread();

		Fab4 f = Fab4.getMVFrame(getBrowser());
		if (f != null) {
			menuModified = false;
			if (fileMenu != null) {
				fileMenu.remove(itemTranscodePNG);
				fileMenu.remove(itemTranscodeWAV);
				fileMenu.remove(itemTranscode);
			}		        
		}
		super.close();
	}
	
	
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == transcodeCmd) {
			transcode();	
		} else if (e.getActionCommand() == transcodePngCmd) {
			saveFrame();	
		} else if (e.getActionCommand() == transcodeWaveCmd) {
			saveWAV();
		}
		
	}


	private void saveWAV() {
		TranscodeWAVDialog f = new TranscodeWAVDialog(new JFrame(), 0, (int) mxfdoc.get_nb_frames());
		f.setVisible(true);
		
		if (f.getRes()) {
			saveWAV(f.getDirectory().getPath(),
					  f.getMin(),
					  f.getMax());
		}
	}
	

	private void saveWAV(String path, int min, int max) {
		String template = "output";	
		mxfdoc.saveWAV(path, template, min, max);
	}
	
	
	private void transcode() {
		TranscodePNGDialog f = new TranscodePNGDialog(new JFrame(), 0, (int) mxfdoc.get_nb_frames());
		f.setVisible(true);
		
		if (f.getRes()) {
			transcode(f.getDirectory().getPath(),
					  f.getMin(),
					  f.getMax(),
					  f.getFileFormat());
		}
	}

	private void transcode(String path, int min, int max, String format) {
		String template = "output";	
		mxfdoc.transcode(path, template, min, max, format.toLowerCase());
	}

	private void saveFrame() {
		TranscodePNGDialog f = new TranscodePNGDialog(new JFrame(), 0, (int) mxfdoc.get_nb_frames());
		f.setVisible(true);
		
		if ( (f.getRes()) && (f.getDirectory() != null) ) {
			saveFrame(f.getDirectory().getPath(),
					  f.getMin(),
					  f.getMax(),
					  f.getFileFormat());
		}
	}


	private void saveFrame(String path, int min, int max, String format) {
		String template = "output";	
		mxfdoc.saveFrame(path, template, min, max, format.toLowerCase());
	}

	

}
