package multivalent.std.ui;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

import multivalent.*;
import multivalent.node.LeafText;
//import multivalent.std.ui.DocumentPopup;

import phelps.net.URIs;
import phelps.lang.Strings;



/**
	Lookup selection or current URI at scripted destination.

<!--
	<p>LATER
	display definitions in named browser instance
	put in popup menu on word
	support Project Gutenberg public domain dictionary (default?)
-->

	@version $Revision$ $Date$
*/
public class SelectionUI extends Behavior {
  /** Hub attribute that gives text to show in menu. */
  public static final String ATTR_TITLE = "title";

  /** Hub attribute that gives text to show in menu. */
  public static final String ATTR_SELURI = "seluri";

  /** Hub attribute that gives text to show in menu. */
  public static final String ATTR_FULLPAGEURI = "fullpageuri";

  /** Hub attribute that gives text to show in menu. */
  public static final String ATTR_URI = "uri";



  public boolean semanticEventBefore(SemanticEvent se, String msg) {
//if (getAttr("parent")!=null) System.out.println("*** semanticUI "+getAttr("parent")+" vs "+msg);
	if (super.semanticEventBefore(se,msg)) return true;
	else if (DocumentPopup.MSG_CREATE_DOCPOPUP==msg && se.getIn()==getBrowser().getSelectionSpan() /*&& getAttr("docpopup")!=null*/) {
//	if (pop && getAttr("docpopup")==null) return false;
//		Browser br = getBrowser();
//		boolean fsel = se.getIn()==br.getSelectionSpan();
		String uri = getAttr(/*fsel?*/ ATTR_SELURI/*: "URI"*/); if (uri==null) return false;
//System.out.println("SelectionUI w/"+msg+", uri="+uri);

		INode menu = (INode)se.getOut();
//if (getAttr("docpopup")!=null) System.out.println("docpopup: menu="+menu.getName()+", uri="+uri+", title="+getAttr("title")+", ==? "+(se.getIn()==br.getSelectionSpan()));

//		if (pop || (se.getIn()==br.getSelectionSpan() /*&& br.getCurNode() instanceof LeafText*/)) {
//System.out.println("added "+getAttr("title"));
			createUI("button", getAttr(ATTR_TITLE), "event "+uri/*lookupSelection"*/, menu, /*"VIEW"*/"LOOKUP", false);
//		}
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Browser br = getBrowser();
	Span sel = br.getSelectionSpan();	// or wherever cursor is

	String uri=null;  // add trailing slash?  doesn't necessarily have one
	if (null==msg) {
	} else if (/*sel.isSet() &&*/ msg.equals(getAttr(ATTR_SELURI))) {
//		String txt = br.getSelection();
		String txt=null;
		if (sel.isSet()) txt = br.clipboard();
		else {
			Node n = br.getCurNode();
			if (n instanceof LeafText) txt = n.getName();
		}
//System.out.println("selected text = |"+txt+"|");
		//String word = sel.getStart().node.getName().trim();	 // collect full selection?
		if (txt!=null) uri = msg + URIs.encode(Strings.trimPunct(txt));

	} else if (msg.equals(getAttr(ATTR_FULLPAGEURI))) {
		URI cururi = br.getCurDocument().getURI();
		if (cururi.getScheme().equals("http"))	  // no sense sending URI network service can't access
			uri = msg + URIs.encode(cururi.toString());

	} else if (msg.equals(getAttr("uri"))) {
		uri=msg;
	}

	if (uri!=null) {
		try {
			DocInfo di = new DocInfo(new URI(uri));
			di.window = getAttr("window");	// a number of lookup could decide socially to share same name
			br.eventq(Document.MSG_OPEN, di);
		} catch (URISyntaxException e) {}
	}
	return super.semanticEventAfter(se,msg);
  }
}
