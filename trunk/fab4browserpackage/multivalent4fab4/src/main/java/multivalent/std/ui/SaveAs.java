/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent.std.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.swing.JFileChooser;

import com.pt.io.InputUni;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Document;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.std.MediaLoader;



/**
	Save current document to path chosen with a file chooser.

<!--
	Doesn't need to be page aware because Layer.save() is.
-->

	@version $Revision$ $Date$
*/
public class SaveAs extends Behavior {
  /**
	Save current document to new file.
	<p><tt>"savePageAs"</tt>.
  */
  public static final String MSG_SAVE_AS = "savePageAs";


  static public JFileChooser jc_ = null;	// static OK because modal means can only have one at a time

  /** Disabled if viewing directory. */
  @Override
public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	return false;
  }


  /** On "saveAnnosAs" semantic event, pop up dialog and save. */
  @Override
public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_SAVE_AS==msg) {
		Browser br = getBrowser();	// Browser is a Component
		Document doc = br.getCurDocument();
		
		if (jc_==null) jc_ = new JFileChooser();
		jc_.setDialogType(JFileChooser.SAVE_DIALOG);
		jc_.setDialogTitle("Save Document to File");

		File dir = jc_.getCurrentDirectory();
		URI uri = doc.getURI();
		for (Node stop=getRoot(); uri==null && doc.getParentNode()!=stop; ) {    // inline Note => climb up until get base doc
			doc = doc.getParentNode().getDocument();
			uri = doc.getURI();
		}

		String file = computeFilename(uri);

		File newpath = new File(dir, file);
		jc_.setSelectedFile(newpath);
		if (jc_.showSaveDialog(br) == JFileChooser.APPROVE_OPTION) {
			File dest = jc_.getSelectedFile();
			try {
				File source = MediaLoader.FileCache.get(uri);
				if (source!=null) {
					FileInputStream in = new FileInputStream(source);
					FileOutputStream out = new FileOutputStream(dest);
					byte[] b = new byte[1024];
					int a;
					while ((a=in.read(b))!=-1) {
						out.write(b,0,a);
					}
					in.close();
					out.close();
				}
				else {
					System.out.println("no source " +dest);
					InputUni iu =  InputUni.getInstance(uri, null, getGlobal().getCache());
					InputStream in = iu.getInputStream();
					FileOutputStream out = new FileOutputStream(dest);
					byte[] b = new byte[1024];
					int a;
					while ((a=in.read(b))!=-1) {
						out.write(b,0,a);
					}
					in.close();
					iu.close();
					out.close();
				
				}
			} catch (IOException ioe) {
				System.err.println("couldn't save: "+ioe);
			}
		}
		return true;
	 }
	 return super.semanticEventAfter(se,msg);
  }

  String computeFilename(URI uri) {
	String file = uri.getPath();
	int inx = file.lastIndexOf('/'); if (inx!=-1) file=file.substring(inx+1);   // chop tail file from path
	if (file.length()==0) {
		file="index.html";    // should set to site name
		String host = uri.getHost();
		if (host!=null) {
			file = host;
			inx = file.lastIndexOf('.'); if (inx!=-1) file=file.substring(0,inx);
			inx = file.lastIndexOf('.'); if (inx!=-1) file=file.substring(inx+1);
			file += ".html";
		}
	}
	//inx = file.lastIndexOf('.'); if (inx!=-1) file=file.substring(0,inx);   // strip suffix
	//file += ".mvd";
	return file;
  }
}
