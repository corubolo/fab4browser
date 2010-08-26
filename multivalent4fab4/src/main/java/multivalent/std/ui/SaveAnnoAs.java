package multivalent.std.ui;

import java.io.*;
import java.net.URI;
import javax.swing.JFileChooser;

import multivalent.*;
import multivalent.gui.VMenu;



/**
	Save annotations in a file chosen with a file chooser.
	Doesn't need to be page aware because Layer.save() is.

	@version $Revision: 1.6 $ $Date: 2002/02/01 06:34:28 $
*/
public class SaveAnnoAs extends Behavior {
  /**
	Save annotations on current document to new file.
	<p><tt>"saveAnnosAs"</tt>.
  */
  public static final String MSG_SAVE_AS = "saveAnnosAs";


  static JFileChooser jc_ = null;	// static OK because modal means can only have one at a time



  /** At {@link VMenu#MSG_CREATE_FILE}, add item to menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_FILE==msg) {
		Browser br = getBrowser();
		Document doc = br.getCurDocument();
		Layer personal = doc.getLayer(Layer.PERSONAL);    // disable if no annos -- multipage aware
//System.out.println("annos in personal = "+personal.s);
		//ESISNode e = personal.save(); -- inefficient

		INode menu = (INode)se.getOut();
		boolean fempty = personal.size()==0 && personal.auxSize()==0;
		createUI("button", "Save Annos As...", "event "+MSG_SAVE_AS, menu, StandardFile.MENU_CATEGORY_SAVE, fempty);
	}
	return false;
  }


  /** On "saveAnnosAs" semantic event, pop up dialog and save. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_SAVE_AS==msg) {
		// LATER: arg gives default, else last opened file (getPreference("HOME") for first)
		/*
		String content = typein.getContent();
		File filein = (content.length()<=1? null: new File(content));
		if (filein==null) jc.setCurrentDirectory(null);
		else if (filein.exists()) jc.setCurrentDirectory(filein.isDirectory()? filein: filein.getParentFile());
		*/

		Browser br = getBrowser();	// Browser is a Component
		Document doc = br.getCurDocument();

		if (jc_==null) jc_ = new JFileChooser();
		jc_.setDialogType(JFileChooser.SAVE_DIALOG);
		jc_.setDialogTitle("Save Personal Annotations to File");

		File dir = jc_.getCurrentDirectory();
		URI uri = doc.getURI();
		for (Node stop=getRoot(); uri==null && doc.getParentNode()!=stop; ) {    // inline Note => climb up until get base doc
			doc = doc.getParentNode().getDocument();
			uri = doc.getURI();
		}

		String file = computeFilename(uri);

		File newpath = new File(dir, file);
//System.out.println("select "+newpath+", file="+uri.getFile()+" => "+file);
		jc_.setSelectedFile(newpath);

		if (jc_.showSaveDialog(br) == JFileChooser.APPROVE_OPTION) {
			Layer personal = doc.getLayer(Layer.PERSONAL);
			ESISNode e = personal.save();

			// maybe add .mvd suffix as necessary here
			e.setGI("saved");   // improve later, to author?  "multivalent"?
			if (e.getAttr(Document.ATTR_URI)==null) e.putAttr(Document.ATTR_URI, doc.getURI().toString());

			try {
				BufferedWriter w = new BufferedWriter(new FileWriter(jc_.getSelectedFile()));
				w.write(e.writeXML());
				w.close();
			} catch (IOException ioe) {
				System.err.println("couldn't save annos: "+ioe);
				// alert box
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
		String host = uri.getAuthority();
		if (host!=null) {
			file = host;
			inx = file.lastIndexOf('.'); if (inx!=-1) file=file.substring(0,inx);
			inx = file.lastIndexOf('.'); if (inx!=-1) file=file.substring(inx+1);
		}
	}
	inx = file.lastIndexOf('.'); if (inx!=-1) file=file.substring(0,inx);   // strip suffix
	file += ".mvd";

	return file;
  }
}
