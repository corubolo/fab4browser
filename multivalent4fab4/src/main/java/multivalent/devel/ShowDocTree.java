package multivalent.devel;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.Map;

import phelps.lang.Booleans;

import com.pt.awt.NFont;

import multivalent.*;
import multivalent.EventListener;
import multivalent.gui.VCheckbox;
import multivalent.gui.VScrollbar;
import multivalent.node.Fixed;
import multivalent.node.IVBox;
import multivalent.node.Root;
import multivalent.node.LeafUnicode;
import multivalent.node.LeafText;



/**
	Replace view current document with infomration on data structure: indented to show nesting, with name/GI, bbox, attributes, stickies, ....
	Lets the original document paint so it formats properly (including images which load asynchronously).
	Can scroll to see all of tree vs lame Windoze DOS window.
	Make a selection in the formatted document to jump to that point on the tree view.
	If line has red background, the node failed its self test.
	Click button-3 on node-line for validation details, list of actives.

	Turn on by going to Help menu and turning on the Debug switch.  Then go to the new Debug menu and choose "Show Doc Tree" or "Show Doc Root".

	@see multivalent.devel.lens.Bounds - show bbox bounds in context of document

	@version $Revision: 1.6 $ $Date: 2003/06/02 05:15:11 $
*/
public class ShowDocTree extends Behavior implements EventListener {
  /**
	Request display of ducment tree.
	<p><tt>"showDocTree"</tt>.
  */
  public static final String MSG_SHOW = "showDocTree";

  /**
	Request display of ducment tree, rooted at uber-Root.
	<p><tt>"showDocTree/Root"</tt>.
  */
  public static final String MSG_SHOWROOT = "showDocTree/Root";

  /**
	Toggle whether to show leaves or not.
	<p><tt>"showDocTree/setShowLeaf"</tt>.
  */
  public static final String MSG_SET_SHOWLEAF = "showDocTree/setShowLeaf";


  static Color guideColor = new Color(0xe0, 0xe0, 0xe0);    // off white
  static NFont FONT_PLAIN = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 10f), 
	  FONT_BOLD = NFont.getInstance(FONT_PLAIN.getFamily(), NFont.WEIGHT_BOLD, FONT_PLAIN.getFlags(), FONT_PLAIN.getSize());
  static int displayH_ = (int)Math.ceil(FONT_PLAIN.getSize());	//getHeight();
  static INode popup_ = new IVBox("menu"/*"detail"*/,null, null);   // only one active at a time so can be static


  Node[] nodes_ = null;
  boolean[] nvalid_ = null;   // from validate(), not Node.isValid()
  Document obsdoc_=null, showdoc_=null;
  int oldvsbmin_, oldvsbmax_, oldvsbval_;
  boolean showLeaf_ = true;
  Node selnode_= null;

  // various statistics -- split off into own behavior so can constantly monitor as browse without switching into data structure view
  int lcnt_,icnt_,iscnt_, txtcnt_,txtlen_,txtmax_;
  List<String> stats_ = new ArrayList<String>(10);


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (Debug.MSG_CREATE_DEBUG==msg) {
		//String cat = "Debug";
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();
		Document doc = br.getCurDocument();
		if (doc!=null) {
			// radiobox?  shift-select to choose root?
			VCheckbox cbd = (VCheckbox)createUI("checkbox", "Show Doc Tree", new SemanticEvent(br, MSG_SHOW, doc), menu, null, false);
			VCheckbox cbr = (VCheckbox)createUI("checkbox", "Show Root Tree", new SemanticEvent(br, MSG_SHOWROOT, doc), menu, null, false);
			if (showdoc_!=null) { boolean r=(showdoc_==getRoot()); cbd.setState(!r); cbr.setState(r); }
			VCheckbox cbl = (VCheckbox)createUI("checkbox", "Show Leaves in Tree Dumps", "event "+MSG_SET_SHOWLEAF, menu, null, false);
			cbl.setState(showLeaf_);
		}
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Browser br = getBrowser();

	if (Document.MSG_CLOSE==msg /*this doc*/ && obsdoc_!=null) {
		obsdoc_.deleteObserver(this); obsdoc_=showdoc_=null;

	} else if (MSG_SET_SHOWLEAF==msg) {
		showLeaf_ = Booleans.parseBoolean(se.getArg(), !showLeaf_);
		if (showdoc_!=null) {
			br.eventq((showdoc_==getRoot()? MSG_SHOWROOT: MSG_SHOW), obsdoc_);
			showdoc_=null;
		}
	} else if (MSG_SHOW==msg || MSG_SHOWROOT==msg) {
		// clean up old
		if (obsdoc_!=null) {
			obsdoc_.deleteObserver(this);
			VScrollbar vsb = obsdoc_.getVsb();
			vsb.setMinMax(oldvsbmin_, oldvsbmax_); vsb.setValue(oldvsbval_);
		}
		nodes_=null; nvalid_=null;	// no dangling pointers

		Document doc=(Document)se.getArg(), showdoc=(MSG_SHOW==msg? doc: getRoot());
		if (showdoc==showdoc_) { // toggle on same=>remove
			obsdoc_=showdoc_=null;
			selnode_=null;
			doc.deleteObserver(this);

		} else {	// move to new doc
			obsdoc_=doc; showdoc_=showdoc;
			lcnt_=icnt_=iscnt_ = txtcnt_=txtlen_=txtmax_ = 0;
			List<Node> nodes = new ArrayList<Node>(1000);
			listNodes(showdoc_, nodes);
			int ncnt = nodes.size();
			if (ncnt>0) {
				nodes_ = new Node[ncnt];
				nvalid_ = new boolean[ncnt];
				for (int i=0; i<ncnt; i++) {
					Node n = nodes.get(i);
					nodes_[i] = n;
					//nvalid_[i] = n.validate(Browser.PROTOCOL_PAINT,null);
					nvalid_[i] = true;
				}
			}

			stats_.clear();
			stats_.add((icnt_+lcnt_)+" total Nodes");
			if (icnt_>0) stats_.add(icnt_+" INode, avg "+(iscnt_/icnt_)+" children");
			if (lcnt_>0) stats_.add(lcnt_+" Leaf");
			if (txtcnt_>0) stats_.add(txtcnt_+" LeafText, avg="+(txtlen_/txtcnt_)+", max="+txtmax_);
//System.out.println("collected "+ncnt+" nodes");

			// reset scrollbar min, max
			VScrollbar vsb=doc.getVsb(), hsb=doc.getHsb();
			oldvsbmin_=vsb.getMin(); oldvsbmax_=vsb.getMax(); oldvsbval_=vsb.getValue();
			vsb.setMinMax(0, displayH_ * ncnt);
			vsb.setValid(false); vsb.formatBeforeAfter(doc.bbox.width, doc.bbox.height, doc.getStyleSheet().getContext());   // need to format or else won't appear if didn't on source doc
			hsb.setValue(hsb.getMin());

			// if cursor or selection, scroll there
			CursorMark curs=br.getCursorMark(); Span sel=br.getSelectionSpan();
			if (curs.isSet()) selnode_=curs.getMark().leaf; else if (sel.isSet()) selnode_=sel.getStart().leaf; else selnode_=null;
//System.out.println("sel start="+selnode_);
			if (selnode_!=null) {
				if (!showLeaf_) selnode_=selnode_.getParentNode();
				for (int i=0; i<ncnt; i++) {
					if (selnode_ == nodes_[i]) { vsb.setValue(displayH_ * Math.max(i-10/*lines of previous context*/,0)); break; } // back context of ten nodes
				}
//System.out.println("initial scroll @ "+nodes_.indexOf(selnode_)+"/"+selnode_);
			}

			doc.addObserver(this);
		}

		br.repaint(100);
	}
	return super.semanticEventAfter(se,msg);
  }


  /** Compute liinearized tree of nodes. */
  public void listNodes(Node n, List<Node> l) {
	if (n.isStruct()) {
		l.add(n);
		INode p = (INode)n;
		for (int i=0,imax=p.size(); i<imax; i++) listNodes(p.childAt(i), l);
		icnt_++; iscnt_+=p.size();
	} else {
		if (showLeaf_) l.add(n);
		lcnt_++;
		if (n instanceof LeafText) { txtcnt_++; txtlen_+=n.size(); txtmax_=Math.max(txtmax_,n.size()); }
	}
  }


  // have to let reach scrollbar
  public boolean eventBefore(AWTEvent e, Point rel, Node obsn) {
	//int eid=e.getID();
	if (rel!=null && rel.x > obsdoc_.bbox.width-VScrollbar.SIZE && e.getID()==MouseEvent.MOUSE_PRESSED) return false;
	// on button 3, pop up with node details
	return true;    // no bleed through
  }

  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	int eid=e.getID();
	if (eid==MouseEvent.MOUSE_PRESSED) {
		MouseEvent me = (MouseEvent)e;
		if (me.getModifiers()==MouseEvent.BUTTON3_MASK) {
			// cursor=>line=>node
			Node n = nodes_[rel.y / displayH_];
			popup_.removeAllChildren();

			// collect info
			List<Object> detail = new ArrayList<Object>(10);
			INode p = n.getParentNode();
			detail.add("parent = "+(p!=null? p.getName(): "(null)")+", class="+n.getClass().getName());
			int dheadi = detail.size();

			//n.validate("Detail", detail);
			if (detail.size()>dheadi) detail.add(dheadi, "*** Warnings ***");
			// stats...
			// actives
			List<ContextListener> actives = n.getActivesAt(-1);
			if (actives.size()>0) detail.add("*** Actives ***");
			detail.addAll(actives);
/*			for (int i=0,imax=actives.size(); i<imax; i++) {
				ContextListener cl = actives(i);
				detail.add(cl.getName());
			}*/

			if (detail.size()==0) detail.add("(No details)");
			for (int i=0,imax=detail.size(); i<imax; i++) new LeafUnicode(detail.get(i).toString(),null, popup_);

			// display in popup_ => put in some style sheet
			Browser br = getBrowser();
			Root root = getRoot();
			CLGeneral gs = new CLGeneral();
			gs.setBackground(Color.WHITE);
			//root.getStyleSheet().put(popup_.getName(), gs);

			//popup_.bbox.setLocation(me.getX()+10,me.getY()+10); // +10=>get out of way of cursor
			popup_.bbox.setLocation(me.getX()+displayH_/*next line*/,me.getY()+10); // constant position no good because have to look away from cursor for small box
			root.appendChild(popup_);
//System.out.println("rel.y="+rel.y+", node="+n+" @"+me.getX()+","+me.getY()+", list size="+detail.size());
			br.repaint();
			br.setGrab(this);

			//return true;    // no bleed through -- if in before
		}
	}
	return false;
  }

  public void event(AWTEvent e) {
	int eid=e.getID();
	if (eid==MouseEvent.MOUSE_RELEASED || eid==MouseEvent.MOUSE_DRAGGED) {
		// put region repaint on queue while still attached, but remove so doesn't display
		//popup_.repaint(100);
		Browser br = getBrowser();
		br.releaseGrab(this);
		popup_.remove();

		if (eid==MouseEvent.MOUSE_DRAGGED) {
			MouseEvent me = (MouseEvent)e;
			MouseEvent newme = new MouseEvent(me.getComponent(), MouseEvent.MOUSE_PRESSED, me.getWhen(), me.getModifiers(), me.getX(), me.getY(), me.getClickCount(), me.isPopupTrigger());
			br.eventq(newme);
		} else {
			br.repaint(100);
		}

/*		} else if (e.getID()==MouseEvent.MOUSE_DRAGGED) {
			return eventAfter(e, ???);
*/
		}
  }


  protected int drawStringWidth(Graphics2D g, String str, int x, int y, Color bkgnd, NFont f) {
	int w = (int)f.stringAdvance(str).getX();
	if (bkgnd!=null) { g.setColor(bkgnd); g.fillRect(x,y-displayH_+3, w,displayH_); g.setColor(Color.BLACK); }
	f.drawString(g, str, x, y);
	return w;
  }

  /* would be faster to skip document content, but less correct
  public boolean paintAfter(Context cx, Node ignore) {
	return true;
  }*/

  /** Entire document constructed dynamically as painted. */
  public boolean paintAfter(Context cx, Node ignore) {
	//Document odoc=obsdoc_, sdoc=showdoc_;
	Rectangle bbox = obsdoc_.bbox;
	int dx=obsdoc_.getHsb().getValue(), dy=obsdoc_.getVsb().getValue();

	// could be more efficient if knew clip
	Graphics2D g = cx.g;
	g.setColor(Color.WHITE); g.fillRect(dx,dy, bbox.width-VScrollbar.SIZE,bbox.height);
	g.setColor(guideColor);	for (int x=0; x<150; x+=7 + 7/*every other tabstop*/) g.drawLine(dx+x,dy, dx+x,dy+bbox.height);

	NFont f = FONT_PLAIN;
	int ppl = (int)(f.getSize()*1.2f/*getHeight()*/), ascent = (int)f.getAscent();
	int linecnt = nodes_.length;
	int first = dy/ppl, last = (dy+bbox.height)/ppl + 1;
	g.setColor(Color.BLACK);
	StringBuffer sb = new StringBuffer(200);
	if (first < linecnt) {
		Node n = nodes_[first];
		int level=0; for ( ; n!=showdoc_; n=n.getParentNode()) level++;

		Node prevn = null;
		for (int i=first,imax=Math.min(last,linecnt); i<imax; i++, prevn=n) {
			n = nodes_[i];
			boolean isstr = n.isStruct();
			INode in = (isstr? (INode)n: null);


			// draw data

			// adjust level
			if (n.getParentNode()==prevn) level++;
			//else if (n.getParentNode() == prevn.getParentNode()) level=level;
			//else if (n == prevn.getParentNode()) level--;  -- doesn't happen: parents come before children
			else if (prevn!=null) for (Node m=prevn; m!=showdoc_ && m==m.getParentNode().getLastChild(); m=m.getParentNode()) level--;

			int x=level*7, y=i*ppl + ascent;
			// selection start, passed self tests?
			Color bkgnd = null;
			if (!nvalid_[i]) bkgnd=Color.RED;
			else if (n==selnode_) bkgnd=Color.LIGHT_GRAY;
			if (bkgnd!=null) { g.setColor(bkgnd); g.fillRect(dx,y-ppl+3, bbox.width,15); g.setColor(Color.BLACK); }

			// indent, name/GI, class
			sb.setLength(0);
			f = isstr? FONT_BOLD: FONT_PLAIN;
			String txt=n.getName(); if (txt!=null) sb.append(txt.length()<40? txt: txt.substring(0,40));
			sb.append(" / ");	// want slash to indicate following is class name when GI is null
			String cname = n.getClass().getName(); int inx=cname.lastIndexOf('.');
			sb.append(inx!=-1? cname.substring(inx+1): cname);
			/*if (isstr) */sb.append(" / ").append(n.size()); // size: # children / medium-specific size
			sb.append("  #").append(n.childNum());
			x += drawStringWidth(g, sb.substring(0), x,y, null, f);

			// bbox, margins, padding
			bkgnd=null; sb.setLength(0); x = (x<200? 200: x+10); // different than Math.max(200,x+10)
			f = FONT_PLAIN;
			Rectangle cbbox = n.bbox;
			sb.append(cbbox.width).append("x").append(cbbox.height).append("@(").append(cbbox.x).append(",").append(cbbox.y).append(") + ").append(n.baseline);
			if (n instanceof Fixed) {
				Rectangle ibbox = ((Fixed)n).getIbbox();
				sb.append(", F");
				if (cbbox.width!=ibbox.width) sb.append("  w!="+ibbox.width);
				if (cbbox.height!=ibbox.height) sb.append("  h!="+ibbox.height);
				//if (cbbox.x!=ibbox.x) sb.append("  x!="+ibbox.x); -- always true since makde relative
				//if (cbbox.y!=ibbox.y) sb.append("  y!="+ibbox.y);
			}
			bkgnd = (cbbox.x>=0 && cbbox.y>=0 && cbbox.x+cbbox.width<=bbox.width && cbbox.y+cbbox.height<=bbox.height? null: Color.RED);
			x += drawStringWidth(g, sb.substring(0), x,y, bkgnd, f) + 20;


			bkgnd=null; sb.setLength(0);
			if (isstr) {
				Insets mar=in.margin;
				if (mar==INode.INSETS_ZERO) {}
				else if (mar.top==mar.bottom && mar.left==mar.right && mar.top==mar.left) sb.append("mar ").append(mar.left);	// if margins==0 here, bold because should have been INSETS_ZERO (unless client override, and even then)
				else sb.append("mar x:").append(mar.left).append(',').append(mar.right).append(", y:").append(mar.top).append(',').append(mar.bottom);

				Insets pad=in.padding;
				if (pad==INode.INSETS_ZERO) {}
				else if (pad.top==pad.bottom && pad.left==pad.right && pad.top==pad.left) sb.append(sb.length()>0?" / ":"").append("pad ").append(pad.left);
				else sb.append(sb.length()>0?" / ":"").append("pad x:").append(pad.left).append(',').append(pad.right).append(", y:").append(pad.top).append(',').append(pad.bottom);
			}
			if (sb.length()>0) x += drawStringWidth(g, sb.substring(0), x,y, bkgnd, f) + 20;


			// valid bit
			if (!n.isValid()) x += drawStringWidth(g, "INVALID", x,y, null, f) + 20;	// ok to be invalid this way


			if (x<325) x=325;

			// stickies
			sb.setLength(0);
			int slen = n.sizeSticky();
			if (slen>/*=*/0) {
				// orange for 0-length list--don't have this information anymore, green for stickies on structural
				bkgnd = (slen==0? Color.ORANGE/*warning?*/: (isstr? Color.GREEN: null));
				x += drawStringWidth(g, "Stickies: ", x,y, bkgnd, f);
			}
			for (int j=0; j<slen; j++) {
				sb.setLength(0);
				if (j>0) x += drawStringWidth(g, " / ", x,y, bkgnd, f);
				bkgnd=null;
				Mark m = n.getSticky(j);
				Object owner = m.getOwner();
				if (isstr) { sb.append('|'); }
				else {
					sb.append(m.offset);
					if (owner instanceof Span) {
						Span span = (Span)owner;
						sb.append(span.getStart()==m? '<': '/');//.append(span.getName());
					}
				}
				if (owner!=null) {
					if (owner instanceof Behavior) {
						sb.append(((VObject)/*...*/owner).getName());
						//if (!((Behavior)owner).validate(Browser.PROTOCOL_PAINT,null)) bkgnd=Color.RED;
					} else sb.append(owner);	// .toString()
				}
				x += drawStringWidth(g, sb.substring(0), x,y, bkgnd, f);
				//String owner=m.getOwner().getClass().getName(); if (owner.lastIndexOf('.')!=-1) owner=owner.substring(owner.lastIndexOf('.')+1);
			}
			if (slen>0) x += 20;


			// observers
			bkgnd=null; sb.setLength(0);
			List<Behavior> obs = n.getObservers(); int olen=(obs==null? 0: obs.size());
			if (olen>0) sb.append("Observers: ");
			for (int j=0; j<olen; j++) sb.append(obs.get(j).getName()).append(" / ");
			//if (olen>0) sb.setLength(sb.length()-3);
			//bkgnd = (isstr? Color.GREEN: null); // observer on leaf unusual--
			bkgnd = (n!=showdoc_? Color.GREEN: null);	// observer on non-Document unusual
			if (olen>0) x += drawStringWidth(g, sb.substring(0,sb.length()-3), x,y, bkgnd, f) + 20;


			// attributes
			bkgnd=null; sb.setLength(0);
			/*Hash*/Map<String,Object> attrs = n.getAttributes(); int alen=(attrs==null? 0: attrs.size());
			//if (olen>0) sb.append("Attributes: "); -- self evident from <name>=<val>
			if (alen>0) {
				for (Iterator<Map.Entry<String,Object>> enu=attrs.entrySet().iterator(); enu.hasNext(); ) {
					Map.Entry<String,Object> e = enu.next();
					sb.append(e.getKey()).append('=').append(e.getValue()).append(", ");    // menuitems can have SemanticEvent in getValue()
				}
			}
			//if (alen>0) sb.setLength(sb.length()-2);
			if (alen>0) x += drawStringWidth(g, sb.substring(0,sb.length()-2), x,y, bkgnd, f) + 20;


			//if (sb.length()>2) { sb.setLength(sb.length()-2); g.drawString(sb.substring(0), 350,y); }
		}

		int y = dy+100;
		for (int i=0,imax=stats_.size(); i<imax; i++) f.drawString(g, stats_.get(i), bbox.width-200+dx, y+i*ppl);
		//f.drawString(g, linecnt+" Nodes", bbox.width-100+dx, y);
	}

	return false;
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);
  }
}
