package multivalent.std.adaptor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import multivalent.*;
import multivalent.node.LeafUnicode;



/**
	Multivalent hub document adaptor.
	Just reads self to extract URI, then throws events to load that and then self as a layer on top.

	@version $Revision: 1.6 $ $Date: 2002/10/14 16:56:49 $
*/
public class MultivalentAdaptor extends MediaAdaptor {
  protected URI refURI_ = null;

  public Object parse(INode parent) throws Exception {
	String msg = null;

	try {
		ESISNode proot = XML.parseDOM(getURI());  // wasteful to parse whole file just to pluck out URL, so switch to SAX

		// check that it's a Multivalent document description
		// ...

		String suri = proot.getAttr(Document.ATTR_URI);
		if (suri==null) {
			suri = proot.getAttr("url");    // backwards compatibility
			if (suri!=null) proot.putAttr(Document.ATTR_URI, suri);    // update
		}
		proot.removeAttr("url");

		if (suri!=null) {
			msg = "Loading "+suri;
			try { refURI_ = new URI(suri); } catch (URISyntaxException male) { msg="Bad URI in hub: "+suri; }
		} else msg = "no URI attribute set in hub!";

	} catch (IOException ioe) {
	} catch (Exception e) {
		System.out.println(e);
		e.printStackTrace();
		return null;
	}

	return new LeafUnicode(msg,null, parent);
  }

  /** When things settle down to Document.MSG_FORMATTED, load real document and load layer on top. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
//System.out.println("MA: "+msg);
	if (Document.MSG_OPENED/*Document.MSG_FORMATTED*/==msg && refURI_!=null) {
//System.out.println("MA: openedDoc");
		Browser br = getBrowser();
		Document doc = getDocument();
		DocInfo di = new DocInfo(refURI_);
		di.doc = doc;
		br.eventq(Document.MSG_OPEN, di);
		br.eventq(new SemanticEvent(br, Layer.MSG_LOAD, getURI(), null, doc));
		br.eventq(Document.MSG_REDIRECTED, null);
	}
	return super.semanticEventAfter(se,msg);
  }
}
