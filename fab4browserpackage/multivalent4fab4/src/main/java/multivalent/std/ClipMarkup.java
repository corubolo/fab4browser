package multivalent.std;

import multivalent.*;
import multivalent.gui.VMenu;
import multivalent.gui.VCheckbox;
import multivalent.node.LeafText;

import phelps.lang.Booleans;



/**
	Generate XML-format tagged text for clipboard based on structure tree,
	instead of usual text.

	<p>To do: exclude annotations by checking that span in BASE layer.

	@version $Revision: 1.3 $ $Date: 2002/02/01 04:26:24 $
*/
public class ClipMarkup extends Behavior {
  /**
	Paste as markup or not.
	<p><tt>"setMarkup"</tt>: <tt>arg=</tt> bollean or <code>null</code> to toggle.
  */
  public static final String MSG_SET = "setMarkup";


  boolean active_=false;


  /** Add entry to Clipboard menu. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_EDIT==msg) {
		INode menu = (INode)se.getOut();
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Paste as Markup", "event "+MSG_SET, menu, ClipProvenance.MENU_CATEGORY, false);
		cb.setState(active_);
	}
	return false;
  }

  /** Semantic event "setMarkup". */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_SET==msg) active_ = Booleans.parseBoolean(se.getArg(), !active_);
	return super.semanticEventAfter(se,msg);
  }


/* => just show this during selection (observer on selection?
  public void showAffected(Graphics2D g) {
	// assume affects whole document
	if (affected) {
	  g.setColor(Color.RED);
//	  Node selchunks[] = br.getSelchunks();
	  Span sel = br.getSelectionSpan();
	  Node selchunks[] = Node.spanChunky(sel.getStart(), sel.getEnd());
	  if (selchunks!=null) {
		// lasso selection chunky span
		for (int i=0; i<selchunks.length; i++) {
		  Rectangle bbox = selchunks[i].bbox;
		  g.drawRect(bbox.x,bbox.y, bbox.width,bbox.height);
		}
	  }
	}
  }
*/


  /** Add self to root to be call during clipboard tree event. */
  public void buildAfter(Document doc) {
	super.buildAfter(doc);
	doc.addObserver(this);
  }


  /*
   * SELECT - construct selected text by side-effecting passed StringBuffer
   * (easy to insert anywhere in StringBuffer)
   */
  void selectSubtree(StringBuffer sb, Node n, Span sel) {
	if (n.isStruct()) {
		sb.append("<"+n.getName()+">");
		INode parent = (INode)n;
		for (int i=0,imax=parent.size(); i<imax; i++) {
			selectSubtree(sb, parent.childAt(i), sel);
		}
		sb.append("</"+n.getName()+">\n");

	} else if (n instanceof LeafText && ((Leaf)n).sizeSticky()>0) {
		Leaf l = (LeafText)n;
		String name = l.getName();
		if (sb.length()>0) sb.append(' ');
		int start = 0;
		for (int i=0,imax=l.sizeSticky(), si; i<imax; i++, start=si) {
			Mark m = l.getSticky(i);
			si = m.offset;
			// previous text
			if (start < si) sb.append(name.substring(start,si));
			// sticky
			if (m.getOwner() instanceof Span) {
				Span span = (Span)m.getOwner();
				if (span==sel) {}
				else if (span.getStart()==m) {
//  public static void write(StringBuffer sb, int level, String name, Map attrs) {
					multivalent.std.adaptor.XML.write(span.getName(), span.getAttributes(), sb,0);
/*					sb.append("<").append(span.getName());
					// attributes
					Map map = span.getAttributes();
					if (map!=null) for (Iterator<Map.Entry<String,Object>> j=map.entrySet().iterator(); j.hasNext(); ) {
						Map.Entry<String,Object> e = j.next();
						String attrname = e.getKey();
						Object attrval = e.getValue();
						sb.append(' ').append(e.getKey());
						if (!attrval.equals(attrname)) sb.append("='").append(attrval).append("'");
					}*/
					sb.append(">");
					//span.clipboardBefore(sb, null);
				} else sb.append("</").append(span.getName()).append(">");
			}
		}
		if (start < name.length()) sb.append(name.substring(start));

	} else {
		n.clipboardBeforeAfter(sb/*, nullbehavior*/); sb.append(' ');
	}
  }

  public boolean clipboardBefore(StringBuffer sb, Node node) {
	// usually, first check bbox and shortcircuit out if not in my domain
	// (but dictionary lookup interested in whole page)
	// tree walk
	if (!active_) return false;

	Span sel = getBrowser().getSelectionSpan();
	if (sel.isSet()) {
		//br.selectOwn(this); -- don't own, just intercept in selections (have to decide about this selectOwn business)
		sb.setLength(0);
		Node list[] = Node.spanChunky(sel.getStart(), sel.getEnd());
		for (int i=0; i<list.length; i++) selectSubtree(sb, list[i], sel);

		return true;	// skip normal traversal of subtree
	}

	return false;
  }
}
