package multivalent.std.ui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import multivalent.*;
import multivalent.gui.VMenu;
import multivalent.gui.VSeparator;



/**
	Initiates document popup menu (semantic event createWidget/DOCPOPUP, with VMenu in out field).
	Default categories; SPECIFIC, NAVIGATE, VIEW, CREATE, SAVE, EDIT.

	@version $Revision: 1.5 $ $Date: 2003/06/02 05:56:14 $
*/
public class DocumentPopup extends Behavior {
  /**
	Request to show document popup menu.
	<p><tt>"createDocPopup"</tt>: <tt>in=</tt> {@link multivalent.Behavior} <var>requestor</var>.
  */
  public static final String MSG_CREATE = "createDocPopup";

  /**
	Collect components for document context-aware popup menu.
	<p><tt>"createWidget/DOCPOPUP"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.Behavior} or {@link java.util.List} <var>active behavior(s)</var>, <tt>out=</tt> {@link multivalent.INode} <var>menu-to-modify</var>.
	where <var>attributes</var> holds key-value pairs "node" - active node, "actives" - all active behaviors at point.
  */
  public static final String MSG_CREATE_DOCPOPUP = "createWidget/DOCPOPUP";

  public static final String ATTR_CATEGORIES = "categories";

  // public static final String for most common categories...


  /*static?*/ String[] popcats_ = null;    // initialized in restore


  public void buildAfter(Document doc) { doc.getRoot().addObserver(this); }

  /**
	Button 3 creates the document popup menu by sending semantic event,
	with CHashMap as arg with following attributes, as applicable: NODE=current node, ACTIVES=active ContextListeners, ACTIVESPAN=first active span, MENU=popup menu (seeded with categories SPECIFIC, NAVIGATE, VIEW, CREATE, SAVE).
  */
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	if (e.getID()!=MouseEvent.MOUSE_PRESSED) return false;
	MouseEvent me = (MouseEvent)e;
//System.out.println("docpop, button = "+me.getModifiers()+" vs "+MouseEvent.BUTTON3_MASK);
	if ((me.getModifiers() & MouseEvent.BUTTON3_MASK) ==0) return false;

	Map<String,Object> popattrs = new CHashMap<Object>();
	//popattrs.put("MENU", docpopup);

	Browser br = getBrowser();
	//Mark curm = br.getCurMark();
	Node curn=br.getCurNode(); //int curoff=br.getCurOffset();
	Span sel = br.getSelectionSpan();
	Object active = null;
	if (curn!=null && curn.isLeaf()) {
		//Leaf curl = (Leaf)curn;
		// current node, if any
		popattrs.put("node", curn);

		//if (curn.isLeaf()) {}
		List<ContextListener> actives = curn.getActivesAt(br.getCurOffset());
		List<Behavior> belist = new ArrayList<Behavior>(actives.size());
		if (actives.size()>0) popattrs.put("actives", actives);
//System.out.println("actives @ "+curn.getName()+"/"+curoff+" = "+actives);

		// find first editable span -- maybe have to traverse in opposite order
		//Layer curlayer = br.getCurrentLayer();
		Behavior ed=null;//, noed=null;
		for (Iterator<ContextListener> i=actives.iterator(); i.hasNext(); ) {
			Object o = i.next();
//if (o instanceof Span) System.out.println("active span = "+o.getClass().getName());
//			if (o instanceof Span /*&& curlayer==((Span)o).getLayer()*/) {
//				Span span = (Span)o;
			if (o instanceof Behavior /*&& curlayer==((Span)o).getLayer()*/) {
				//Span span = (Span)o;
				Behavior be = (Behavior)o;
				belist.add(be);
				if (be==sel) { ed=sel; break; }     // selecion emphatic: set very recently
				if (be.isEditable()) ed=be; //else noed=span; -- unfortunately last is highest priority
				//activespan = o;
				//popattrs.put("ACTIVESPAN", activespan);

//System.out.println("active span = "+activespan+" / "+activespan.getClass().getName()+", priority="+((Span)activespan).getPriority());
				//break; -- highest priority is last -- probably wrong
			}
		}
//System.out.println("ed="+ed+", noed="+noed+", node="+curm.leaf);
		//active = (ed!=null? ed: noed);
		active = (ed!=null? (Object)ed: (Object)belist/*actives*/);
		//active = belist;
	}
	if (active==null) active=curn;

//System.out.println("*** createDocPopup from DocumentPopup "+active);
	br.eventq(new SemanticEvent(br, MSG_CREATE, popattrs, active, null));

	return false;
  }


  /** Recognizes "createDocPopup <attrs> <source>", sends "createWidget/DOCPOPUP <attrs> <source> <menu>". */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_CREATE==msg) {
		VMenu docpopup = new VMenu("docpopup",null, null);//br.getRoot());
		for (int i=0,imax=popcats_.length; i<imax; i++) new VSeparator("_"+popcats_[i],null, docpopup);

		Browser br = getBrowser();
		Object popattrs = se.getArg();
		Object in = se.getIn();
		if (in==null) {}
		else if (in instanceof List) {

/*			List<> list = (List)in;
			for (int i=0,imax=list.size(); i<imax; i++) {
				Behavior be = (Behavior)list.get(i);
				br.event/*no q* /(new SemanticEvent(br, MSG_CREATE_DOCPOPUP, popattrs, be, docpopup));
			}*/

			//br.event/*no q*/(new SemanticEvent(br, MSG_CREATE_DOCPOPUP, popattrs, null, docpopup));
			List<Behavior> list = (List<Behavior>)in;
			for (int i=0,imax=list.size(); i<imax; i++) {
				Behavior be = list.get(i);
				if (br.event(new SemanticEvent(br, MSG_CREATE_DOCPOPUP, popattrs, be, docpopup))) break;
			}
/*			int i=0;
			for (int imax=list.size(); i<imax; i++) {
				Behavior be = list.get(i);
				if (be.semanticEventBefore(new SemanticEvent(br, MSG_CREATE_DOCPOPUP, popattrs, be, docpopup), MSG_CREATE_DOCPOPUP)) { i++; break; }
			}
			for (i--; i>=0; i--) {
				Behavior be = list.get(i);
				if (be.semanticEventAfter(new SemanticEvent(br, MSG_CREATE_DOCPOPUP, popattrs, be, docpopup), MSG_CREATE_DOCPOPUP)) break;
			}*/
		} else {
			//br.event/*no q*/(new SemanticEvent(br, MSG_CREATE_DOCPOPUP, popattrs, null, docpopup));
			br.event/*no q*/(new SemanticEvent(br, MSG_CREATE_DOCPOPUP, popattrs, in, docpopup));
		}

		// no takers for something more specific, try null
		if (docpopup.size() == popcats_.length) br.event/*no q*/(new SemanticEvent(br, MSG_CREATE_DOCPOPUP, popattrs, null, docpopup));

		// could position menu so cursor over first item for click-boom!
		Point curscrn = br.getCurScrn();
//System.out.println("SIZE = "+docpopup.size()); docpopup.dump();
		if (docpopup.size() > popcats_.length) docpopup.post(curscrn.x+1/*so no intersect so stays posted*/, curscrn.y, br);
	}

	return super.semanticEventAfter(se,msg);
  }


  /**
	Take name of menu categories, in order, from comma-separated list in {@link #ATTR_CATEGORIES} attribute.
  */
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);
	popcats_ = getAttr(ATTR_CATEGORIES, "specific, navigate, view, create, save, edit").toLowerCase().split("\\s*,\\s*");     // trim?
  }
}
