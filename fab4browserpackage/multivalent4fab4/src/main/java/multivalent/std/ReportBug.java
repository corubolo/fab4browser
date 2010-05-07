package multivalent.std;

import java.io.*;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.net.URL;
import java.net.URLClassLoader;
import java.awt.datatransfer.*;

import multivalent.*;
import multivalent.gui.VMenu;
import multivalent.gui.VAlert;

import com.pt.awt.NFont;
import com.pt.awt.font.NFontManager;



/**
	Stuff debugging information into clipboard, for pasting into email or message board post.

	@version $Revision: 1.2 $ $Date: 2002/11/09 00:30:11 $
*/
public class ReportBug extends Behavior {
  /**
	Collect information about system to include in bug report.
	<p><tt>"reportBug"</tt>: <tt>arg=</tt> {@link java.lang.StringBuffer} <var>system-information-collection (ASCII)</var>
  */
  public static final String MSG_REPORT_BUG = "reportBug";


  static String form_ = null;



  void fill(StringBuffer sb) {
	if (form_==null) try {
		StringBuffer fsb = new StringBuffer(2*1024);
		Reader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("BugReport.txt")));
		for (int ch; (ch=r.read())!=-1; ) fsb.append((char)ch);
		r.close();
		form_ = fsb.substring(0);
	} catch (IOException ioe) {}

	// 1. form from text file
	sb.insert(0, form_);


	// 2. basic information
	sb.append("Report generated ").append(new java.util.Date().toString()).append("\n");

	sb.append("Multivalent version: ").append(Multivalent.VERSION).append("\n");


	sb.append("\n\n*** JARs ***\n\n");
	URL[] urls = ((URLClassLoader)Multivalent.getInstance().getClass().getClassLoader()).getURLs();
	for (int i=0,imax=urls.length; i<imax; i++) sb.append(urls[i]).append("\n");


	sb.append("\n\n*** Current Document ***\n\n");
	Document doc = getBrowser().getCurDocument();
	// LATER: URI, tree state, ...
	sb.append("URI: ").append(doc.getURI()).append("\n");
	sb.append("layout valid? ").append(doc.isValid()).append("\n");
	for (Iterator<Map.Entry<String,Object>> i=doc.attrEntrySetIterator(); i.hasNext(); ) {
		Map.Entry<String,Object> e = i.next();
		sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");

	}
	// exception with stack trace

	// => just capture information from tool.Info

	sb.append("\n\n*** Fonts ***\n\n");
	String[] psnames = NFontManager.getDefault().getAvailableNames();
	for (int i=0,imax=psnames.length; i<imax; i++) sb.append(psnames[i]).append("\n");

	sb.append("\n\n*** AWT Fonts ***\n\n");
	String[] fam = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	for (int i=0,imax=fam.length; i<imax; i++) sb.append(fam[i]).append("\n");


	sb.append("\n\n*** Java Properties ***\n\n");     // ncludes Java VM and OS.
	Runtime rt = Runtime.getRuntime();
	sb.append("Memory use: ").append(rt.freeMemory()).append(" free of ").append(rt.totalMemory()).append("total, max=").append(rt.maxMemory()).append("\n");
	sb.append("Processors: ").append(rt.availableProcessors()).append("\n");
	Properties props = System.getProperties();
	for (Enumeration e=props.keys(); e.hasMoreElements(); ) {
		Object key=e.nextElement();
		sb.append(key).append(": ").append(props.get(key)).append("\n");
	}

	//sb.append("\n\n*** Preferences.txt ***\n\n);
	// LATER: locations and contents

  }


  /**
	"Report Bug" in Help menu.
  */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_HELP==msg) {
		SemanticEvent newse = new SemanticEvent(getBrowser(), MSG_REPORT_BUG, new StringBuffer(5000));
		createUI("button", "Report Bug", newse, (INode)se.getOut(), null, false);

	} else if (MSG_REPORT_BUG==msg && se.getArg() instanceof StringBuffer) {
		fill((StringBuffer)se.getArg());
		// let circulate for others to add...
	}

	return false;
  }

  /** Collect information, stuff in clipboard, alert box for user. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Browser br = getBrowser();

	if (MSG_REPORT_BUG==msg && se.getArg() instanceof StringBuffer) {
		// stuff in clipboard
		String txt = ((StringBuffer)se.getArg()).toString();
		StringSelection ss = new StringSelection(txt);
		br.getToolkit().getSystemClipboard().setContents(ss, ss);

		// pop up alert
		new VAlert("alert",null, getDocument(), "Debugging information copied to clipboard.  Paste into e-mail.");
	}

	return super.semanticEventAfter(se,msg);
  }
}
