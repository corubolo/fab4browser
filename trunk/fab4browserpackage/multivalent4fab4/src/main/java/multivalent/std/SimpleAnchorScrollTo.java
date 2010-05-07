package multivalent.std;

import multivalent.*;
import phelps.doc.RobustLocation;



/**
	Scroll to simple anchor, both HTML NAME/ID attr and AnchorSpan annotations.
	XPointer support will be implemented in same way.

	@version $Revision: 1.6 $ $Date: 2002/03/24 01:55:33 $
*/
public class SimpleAnchorScrollTo extends Behavior {

  /** Scroll in Before so other scrollers, such as forward/backward menu or XPointer, can override (by scrolling again). */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	Object arg = se.getArg();
	Browser br = getBrowser();
//if (Document.MSG_OPEN==msg) System.out.println("open on SimpleAnchor "+arg.getClass().getName()+": "+arg);
	if (super.semanticEventBefore(se,msg)) return true;

	// newly loaded document or anchor within current document
	else if (Document.MSG_FORMATTED==msg && arg instanceof Document) {   // same document
		Document doc = (Document)arg;
//System.out.println("sast => "+doc.getURI().getFragment());
		String frag = (doc.getURI()!=null? doc.getURI().getFragment(): null);
		if (frag!=null) br.event/*no q*/(new SemanticEvent(br, IScrollPane.MSG_SCROLL_TO, frag, doc, null));

	} else if ((/*Document.MSG_FORMATTED==msg ||*/ Document.MSG_OPEN==msg) && arg instanceof DocInfo) {   // if same document don't format
		DocInfo di = (DocInfo)arg;
		Document doc = di.doc; if (doc==null) doc=getBrowser().getCurDocument();
		String frag = (di.uri!=null? di.uri.getFragment(): null);

//System.out.println("new URI="+di.uri+" vs old URI="+doc.getURI());
		// getSchemeSpecificPart() exclusive of #fragment
		if (frag!=null && doc!=null && doc.getURI()!=null && doc.getURI().getScheme().equals(di.uri.getScheme()) && doc.getURI().getSchemeSpecificPart().equals(di.uri.getSchemeSpecificPart())) {
			//scrollTo(doc, frag);
			br.event/*no q*/(new SemanticEvent(br, IScrollPane.MSG_SCROLL_TO, frag, doc, null));
		}
//System.out.println("simple anchor scroll to "+frag+" in "+di.doc);  //+", anchor set ="+di.doc.getVar(Document.VAR_ANCHORS));
		//else doc.scrollTo(0,0); -- document built so defaults to this, can be modified by foward/back buttons

	} else if (IScrollPane.MSG_SCROLL_TO==msg && arg instanceof String) {
		IScrollPane doc = (se.getIn() instanceof Node? ((Node)se.getIn()).getIScrollPane(): getBrowser().getCurDocument());
		scrollTo(doc, (String)arg);
	}

	return false;
  }


  /** Scroll to anchor/id/ref within Document. */
  public void scrollTo(IScrollPane isp, String ref) {
//System.out.println("isp="+isp+", ref="+ref);
	//assert isp!=null && ref!=null;
	if (isp==null || ref==null) return;
	//Object refo = ((Map)doc.getVar(Document.VAR_ANCHORS)).get(ref);
	Object refo = RobustLocation.attachId(ref, Node.ATTR_ID, isp);
//System.out.println("*** ref = "+ref+", refo="+refo);

	if (refo!=null) getBrowser().eventq(IScrollPane.MSG_SCROLL_TO, refo);
  }

}
