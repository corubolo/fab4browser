package multivalent.std.ui;

import java.awt.FileDialog;	// native better than generic javax.swing.JFileChooser
import java.awt.Frame;
import java.awt.Component;
import java.io.File;

import multivalent.*;



/**
	Browse a file on the local file system, as chosen by a {@link java.awt.FileDialog}.

	@version $Revision: 1.4 $ $Date: 2004/01/02 04:25:09 $
*/
public class OpenFile extends Behavior {
  /**
	Request opening file from local file system.
	<p><tt>"openFile"</tt>.
  */
  public static final String MSG_OPEN = "openFile";


  static FileDialog Dialog_ = null;	// static OK because modal -- create on demand


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_OPEN==msg) {
		// LATER: arg gives default, else last opened file (getPreference("HOME") for first)
		/*
		String content = typein.getContent();
		File filein = (content.length()<=1? null: new File(content));
		if (filein==null) Dialog_.setCurrentDirectory(null);
		else if (filein.exists()) Dialog_.setCurrentDirectory(filein.isDirectory()? filein: filein.getParentFile());
		*/

		Browser br = getBrowser();	// Browser is a Component
		
		Component p = br; while (p.getParent()!=null) p=p.getParent();	// at top must be a Frame
		if (Dialog_==null) Dialog_ = new FileDialog((Frame)p, "Open Document", FileDialog.LOAD);

		Dialog_.show();
		String file = Dialog_.getFile();
		if (file!=null) {
			// set type to initial file
//System.out.println("setting typein content to "+Dialog_.getSelectedFile());
			//if (typein!=null) typein.setContent(Dialog_.getSelectedFile().toString());
			File f = new File(new File(Dialog_.getDirectory()), Dialog_.getFile());
			br.eventq(Document.MSG_OPEN, f.toURI());
		}
		return true;
	}
	return false;
  }
}
