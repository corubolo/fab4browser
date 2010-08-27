package multivalent.gui;

import java.net.URI;
import java.util.Map;
import java.util.HashMap;

import multivalent.*;
//import multivalent.EventListener;



/**
	A {@link VFrame} that returns an event to its parent Document.
	Automatically sizes to fit content Document.
	For example, content can be HTML page associated with behavior, and include a FORM for setting attributes.
	User closes with close box or perhaps close button in the document (HTML FORM or scripted).
	Submit button should send XXX event to close.

<!--
HyperlinkSpan wants URL, Preferences want many, Manual Page wants MANPATH.
=> VFrame with HTML FORM
	set initial values of fields
<= map of attrs, tagged with msg in SemanticEvent (targeted to calling Document)

=>=> behavior observer on new VFrame.
	on windowClosed (close box), or specialized (e.g., hyperlinkData),
	kill VFrame and return map to caller (semantic event: arg=map)
-->

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:17:53 $
*/
public class VDialog extends VFrame /*implements EventListener*/ {
  //protected String trigger_ = null;
  Behavior seIn_ = null;

  class Dialog extends Behavior {
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
//System.out.println("**** after |"+msg+"|");
		if (SystemEvents.MSG_FORM_DATA==msg || VFrame.MSG_CLOSED==msg) {
			Browser br = getBrowser();

			Object arg=se.getArg(), out=se.getOut();
			if (VFrame.MSG_CLOSED==msg) { arg=new HashMap(); out=null; }
			else close();

			if (br!=null) {
				//Document pdoc = getDocument().getParentNode().getDocument();	// parent
				//br.setCurDocument(pdoc);
				if (seIn_!=null) br.setCurDocument(seIn_.getDocument());
				br.eventq(new SemanticEvent(br, SystemEvents.MSG_FORM_DATA, arg, seIn_, out));
			}

			//return true;	-- getting away from semanticEventAfter shortcuiting
		}
		return super.semanticEventAfter(se, msg);
	}
  }

  public VDialog(String name,Map<String,Object> attr, INode parent, URI docuri) {
	super(name,attr, parent, docuri);
	setBounds(100,100, 300,10); // height taken from content
	setPinned(false);
	resizable = false;
  }

  public VDialog(String name,Map<String,Object> attr, INode parent, URI docuri, Map vals, Behavior in) {
	this(name,attr, parent, docuri);
	//setTrigger(trigger);
	setIn(in);

	Node child = childAt(0);
	if (child instanceof Document) {
		Document doc = (Document)child;
		Browser br = getBrowser();
		//br.setCurDocument(doc);
//Document curdoc = br.getCurDocument();
//System.out.println("cur doc =? doc => "+(curdoc==doc)); -- true

		doc.setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);

		if (vals!=null) {
			Node form = doc.findBFS("form");
//System.out.println("sending populate with "+vals+" to "+br.getCurDocument());
			br.eventq(new SemanticEvent(br, "populateForm", form, vals, null));
		}

		Behavior be = new Dialog();
		//Layer l = doc.getLayer(Layer.SCRATCH);
		//be.setName("catch");
		be.restore(null,null, doc.getLayer(Layer.SCRATCH));
		//doc.addObserver(be); -- just for formatting and painting (tree protocols)


//System.exit(0);
	}
  }

  /** SemanticEvent Message that triggers closing of window and rethrowing in parent (i.e. caller) Docoument. */
  //public void setTrigger(String msg) { trigger_ = (msg!=null? msg.intern(): null); }

  // why not broadcast?
  /** Set Behavior that's interested in what happens when dialog is closed. */
  public void setIn(Behavior in) { seIn_=in; }


  public boolean formatNode(int width, int height, Context cx) {
	//if (valid_) return !valid_;

	boolean ret = super.formatNode(width,height, cx);
	// size to fit (keep width) -- maybe make option of IScrollPane

	Node child = childAt(0);
	if (child instanceof IScrollPane) {
		IScrollPane sp = (IScrollPane)child;
		//sp.bbox.height = sp.ibbox.height;
		//sp.bbox.height = -1;
		int ih = sp.ibbox.height;
		// because of way IScrollPane does split pane, have to reformat, rather than sneaky-quick elongation
		int eh = sp.bbox.height;
		int dh = sp.border.top+sp.border.bottom + sp.padding.top+sp.padding.bottom;
		if (eh-dh != ih && ih<400/*guard against runaway*/) {
//System.out.println("resetting height "+eh+"/"+dh+"=>"+ih+"/"+sp.margin.top+"+"+sp.margin.bottom+", "+this.bbox); //-- HTML BODY takes full WxH
			sp.bbox.height = ih;
			bbox.height = ih + titleh;
			markDirtySubtree(false);
			formatNode(width,height, cx);
//getParentNode().getParentNode().dump();
		}

		if (title_==null) title_=getAttr(Document.ATTR_TITLE);
	}

	return ret;
  }

/*	  boolean flag=true;
  public void paintNode(Rectangle docclip, Context cx) {
	 super.paintNode(docclip, cx);
	 if (flag) getParentNode().dump(); flag=false;
  }
  public void paintBeforeAfter(Rectangle docclip, Context cx) {
	System.out.println("paintNode "+(docclip.intersects(bbox))+", docclip.y="+docclip.y+", bbox.y="+bbox.y);
	super.paintBeforeAfter(docclip, cx);
  }*/
}
