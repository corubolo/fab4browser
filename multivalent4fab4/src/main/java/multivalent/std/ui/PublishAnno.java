package multivalent.std.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
//import java.net.URLConnection;

import multivalent.*;
import multivalent.gui.VMenu;

import phelps.net.URIs;



/**
	Write annotations to server.
	(As opposed to ordinary "saving", which automatically writes locally in directory tree parallel to cache tree--may want annos and extras and rely on robust locations for reassoc with pages.)

	@see multivalent.std.ui.SaveAnnoAs

	@version $Revision$ $Date$
*/
public class PublishAnno extends Behavior {
  /**
	Write annotations to a server.
	<p><tt>"publishAnnos"</tt>.
  */
  public static final String MSG_PUBLISH = "publishAnnos";

  /** Hub attribute that sets URI of server to which to write annos. */
  public static final String ATTR_URI = "uri";

  /** Hub attribute that gives text to show in menu item. */
  public static final String ATTR_TITLE = "title";


  URI publishTo_=null;



  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);
	String surl = getAttr(ATTR_URI);
	try {
		publishTo_ = new URI(surl);
	} catch (URISyntaxException bad) {
		System.err.println("Bad URI to publish to: "+surl+" ("+bad+")");
	}
  }


  /** At {@link VMenu#MSG_CREATE_FILE}, add "Publish Annos to <server>" to menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_FILE==msg) {
		createUI("button", "Publish Annos To "+getAttr(ATTR_TITLE)+"...", "event "+MSG_PUBLISH, (INode)se.getOut(), StandardFile.MENU_CATEGORY_SAVE, false);
		//if (url==null) url=;
	}
	return false;
  }

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_PUBLISH==msg && /*arg!=null &&*/ publishTo_!=null) {
		String name = "xxx";   // ask user
		Browser br = getBrowser();
		String txt = "";	//br.save();
		if (txt.length()>0) {
//System.out.println("ANNO: |"+txt+"|");
			DocInfo di = new DocInfo(publishTo_);
			di.method="POST";
			di.attrs.put("POST", "name=mvd/"+URIs.encode(name) + "&contents="+URIs.encode(txt));
			br.eventq(Document.MSG_OPEN, di);
		}
		return true;
	}
	return super.semanticEventAfter(se,msg);
  }
}
