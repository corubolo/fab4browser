package multivalent.std.ui;

import multivalent.*;
//import multivalent.std.ui.DocumentPopup;

import phelps.lang.Strings;



/**
	For some text that's implicitly a link, dynamically treat it as one if text matches some pattern:
	URL, manual page, email, ....

	<p>To do: use java.util.regex

	@see multivalent.std.LinkMarkup

	@version $Revision: 1.2 $ $Date: 2002/02/01 07:19:43 $
*/
public class LinkMe extends Behavior {

  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==getBrowser().getSelectionSpan()) {

		Node n = getBrowser().getCurNode();
		String name; int len, inx, si=0, ei;
		if (n==null || (name=n.getName())==null || (len=ei=name.length())<=0) return false;

		// determine type
		String target=null, type="URI";
		if ((inx=name.indexOf("://"))>0 && inx+len>=5) {
			target=name;	  // ftp:x
			//if (name.lastIndexOf(')')>inx+1) ...
		} else if (name.startsWith("file:/") && len>=7) target=name;
		else if (name.startsWith("www.") && len>=9) target="http://"+name; // www.x.com
		else if (name.startsWith("ftp.") && len>=9) target="ftp://"+name; // ftp.x.com
		else if ((inx=name.indexOf("@"))>0 && inx+len>=7 && name.indexOf('.',inx+1)!=-1) { target="mailto:"+name; type="e-mail"; }	// x@y.com
		else if ((inx=name.indexOf('('))>0 && name.indexOf(')',inx+1)>0) { target="manpage:"+name; type="Manual Page"; }
//		else if ("ManualPage".equals(getBrowser().getCurDocument().getAttr(Document.ATTR_GENRE))) { target="manpage:"+name; type="Manual Page"; } -- done by man page media adaptor
		else { target="http://www."+Strings.trimPunct(name)+".com/"; type=target; }

		// should trim off punctuation

		createUI("button", "Open as "+type, "event "+Document.MSG_OPEN+" "+target, (INode)se.getOut(), "NAVIGATE", false);
		// also Aux Window
	}

	return false;
  }
}
