package multivalent.gui;

import java.awt.Rectangle;
import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.Color;
//import java.awt.AlphaComposite;
//import java.awt.Composite;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import multivalent.*;
import multivalent.node.Root;
import multivalent.node.LeafShadow;
import multivalent.std.VScript;



/**
	Pure Multivalent menu widget.  Like IVBox + IScrollPane.
	Connect to VMenuButton for pulldown or popup,
	invoke post()/unpost() externally for popup anywhere,
	or embed in document for List-type widget.

	<p>As children you can have any kind of node:
	VButton/VCheckButton/VRadioButton,
	IHBox for HTML OPTIONs,
	or VMenuButton for cascaded menus.
	In other words, there is no need for a separate group of MenuItem widgets.
	As mentioned on the package-wide description, since Multivalent widgets can have
	arbitrary content, having, say, a menu item with a image and text is easy.

<!--   <p>LATER: To have a menu with shortcut keys listed alongside the right, just
		use a table with one column for the labels and a second for the keys.
-->

	<p>If selected item has VScript in "script" attribute, it's executed upon selection.

	@version $Revision: 1.13 $ $Date: 2003/06/02 05:19:13 $
*/
public class VMenu extends /*Document*/IScrollPane implements EventListener {
  /**
	Construct File menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/File"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_FILE = "createWidget/File";

  /**
	Construct Edit menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Edit"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_EDIT = "createWidget/Edit";

  /**
	Construct Go menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Go"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_GO = "createWidget/Go";

  /**
	Construct Bookmark menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Bookmark"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_BOOKMARK = "createWidget/Bookmark";

  /**
	Construct Lens menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Lens"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_LENS = "createWidget/Lens";

  /**
	Construct Clipboard menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Clipboard"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  public static final String MSG_CREATE_CLIPBOARD = "createWidget/Clipboard";
  */

  /**
	Construct Style menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Style"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_STYLE = "createWidget/Style";

  /**
	Construct Anno menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Anno"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_ANNO = "createWidget/Anno";

  /**
	Construct View menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/View"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_VIEW = "createWidget/View";

  /**
	Construct Help menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Help"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_HELP = "createWidget/Help";

  /** Menu category (<code>medium</code>) for medium-specific options (where medium is PDF, HTML, DirectoryLocal, ...). */
  public static final String CATEGORY_MEDIUM = "medium";



  /** Key into Root's globals to get ordered List<> of menus currently posted. */
  public static final String VAR_MENUSTACK = "MENUSTACK";

  /** Maximum number of items to show at once (more uses scrolling). */
  public static int Maxsize=20;	// Netscape default is 20

  //protected static AlphaComposite ac_ = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f);	// Aqua


  /** Permit selection of multiple items? */
  boolean multiple_=false;

  private Node activeitem_ = null;
  private ActiveSpan activespan_=null;	// don't co-opt SelectionSpan as selection must survive menu selections.  not static because can have cascaded menus.  visual only--activeitem_.
  private Node activenew_=null;	// communication between drag/move and release
  //INode oldparent_ = null;	// needed when popup
  private Node shadow_ = null;	// if part of content tree as in HTML, need shadow
  private boolean posted_=false;
  private boolean tickled_=false;	// after posting, tickled anymenu items?  if not, leave posted on MOUSE_RELEASED -- could probably be static since only applies to latest one shown



  static class ActiveSpan extends Span {	// maybe set CLASS or something
	ActiveSpan() { name_="active"; }

	/** Inverts colors, but could set font, underline, or whatever. */
	public boolean appearance(Context cx, boolean all) {
		Color tmp=cx.foreground; cx.foreground=cx.background; cx.background=tmp;
		return false;
	}
	/** May have rich content with own spans, so set priority to override them. */
	public int getPriority() { return ContextListener.PRIORITY_SPAN+ContextListener.LOT; }
  }



  // for HTML, name==null (SELECT==VMenuButton, this==null, children==OPTIONs)
  public VMenu(String name,Map<String,Object> attr, INode parent) { super(name,attr, parent); }


  /**
	Immediate subtrees of menu are <i>categories</i>:
	named <tt>VSeparator</tt>'s used to cluster items from disparate behaviors.
	If category doesn't exist, it's created.
	If category is <tt>null</tt>, item is appended to end of menu.
  */
  public Node addCategory(Node n, String category) {
	if (category==null) appendChild(n);
	else {
		if (!category.startsWith("_")) category="_"+category;
		Node cat = findBFS(category);	// don't want to recurse
		if (cat==null) cat=new VSeparator(category,null, this);
		int inx = cat.childNum() + 1;
		// find next category (or end of menu)
		for (int imax=size(); inx<imax; inx++) {
			String cname = childAt(inx).getName();
//System.out.println("cat @ "+inx+" ? "+cname);
			if (cname==null || cname.startsWith("_")) break;
		}
		// add child to end of category, after category marker child
//System.out.println("adding "+n.getName()+" @ "+inx);
		insertChildAt(n, inx);
	}
	return n;	// maybe true/false reporting success/failure
  }


  public Node getSelected() { return activeitem_; }

  /** Use <tt>invoke()</tt> to execute any associated script. */
  public void setSelected(Node n) {
	if (n!=activeitem_) {
		if (n==null) { activeitem_=null; /*activespan_.moveq(null);*/ }
		else if (n.getParentNode() == this) {
			activeitem_=n;
			//if (activespan_!=null) activespan_.moveq(n.getFirstLeaf(),0, n.getLastLeaf(),n.getLastLeaf().size());
		}
		//repaint(); -- handled by span, for now
	}
  }

/* public void setSelected(String text) {
	// search for matching text (regexp in Java 1.4)
  }*/


  /** Like a IVBox, except is max of children (which are menitems). */
  public boolean formatNode(int width,int height, Context cx) {
	// like VBox: stack children vertically
	Root root = getRoot();

	int wmax=0, hmax=0, smax=Integer.MAX_VALUE, y=0;
//System.out.println("VMenu layout, root bbox ="+root.bbox.width);
//System.out.println("VMenu layout, ss="+cx.styleSheet+", ss.get('b')="+cx.styleSheet.get("b"));
	for (int i=0,imax=size(); i<imax; i++) {
		Node child = childAt(i);
		if (!child.isValid()) child.formatBeforeAfter(root.bbox.width,height, cx);
//System.out.println("layout "+child.getName()+" "+root.bbox.width);
		Rectangle cbbox = child.bbox;
		cbbox.setLocation(0,y);
		y += cbbox.height;
		if (i==Maxsize) smax=y;
		wmax=Math.max(wmax,cbbox.width); //hmax=Math.max(hmax,cbbox.height);
	}

	// postpass to max out width of items
	for (int i=0,imax=size(); i<imax; i++) {
		Node child = childAt(i);
		child.bbox.width = wmax;
	}
//dump();

//System.out.println("smax="+smax);
//	if (size()>maxsize) {
	int maxh = root.bbox.height;
	if (y > maxh) {
		// add width for scrollbar
		for (int i=size()-1; i>=1; i--) {	// since different heights anyway, don't bother with getting whole child
			int top = childAt(i).bbox.y;
			if (top <= maxh) { bbox.setSize(wmax+VScrollbar.SIZE, top-1/*smax*/); break; }
		}
		getVsb().setShowPolicy(VScrollbar.SHOW_AS_NEEDED/*or ALWAYS*/); getHsb().setShowPolicy(VScrollbar.SHOW_NEVER);	// LATER: update IScrollPane to free up space when not needed
	} else {
		bbox.setSize(wmax,y);
		setScrollbarShowPolicy(VScrollbar.SHOW_NEVER);
	}
	baseline = bbox.height;

	return super.formatNode(width,height, cx);	// wrap in scrollbar
  }


  public void paintNode(Rectangle docclip, Context cx) {
//System.out.println("menu "+getFirstLeaf().getName+" paint at "+bbox.x+","+bbox.y);
	// if doesn't fit on screen, move up so that it does
	// ok if overflow right edge?
//System.out.println("paint menu");
	Root root = getRoot();	// not doc bottom
	int rootbot = root.bbox.y + root.bbox.height;
//if (bbox.y == 0 && bbox.y + bbox.height > rootbot) System.out.println("menu too tall");
	if (bbox.y > 0 && bbox.y + bbox.height > rootbot) {
		bbox.y = Math.max(rootbot - bbox.height, 0);
		repaint();	// throw back to get accurate clip -- don't need br.repaint() because new position covers old
		return;
	}

	// translucent menus, like Aqua
/*
	Composite cin = g.getComposite();
	g.setComposite(ac_);
	g.setColor(cx.background); g.fillRect(0,0, bbox.width,bbox.height);
	g.setComposite(cin);
*/

//System.out.println("*** PAINTING MENU, bbox="+bbox);
	super.paintNode(docclip, cx);	// I think this draws the background too, which we don't want for transparency
  }



  /**
	Show on screen by disconnecting from (old) parent and adding to root (which is in absolutely positioned space).
	Independent of mouse clicks.
	Menu is not necessarily posted, as when used a list widget embedded in content space.
	Subclasses can dynamically construct menu contents by overriding with construction
	code and calling super.post(int, int, Browser); the forward/backward buttons and bookmarks do this.

	@see multivalent.std.ui.ForwardBack
	@see multivalent.std.ui.Bookmark
  */
  public void post(int x, int y, Browser br) {
	// post() while already posted
	if (posted_) { /*shadow_.*/repaint(); return; }


	Root root = br.getRoot();
	INode p = getParentNode();

	if (p==null || p==root) {
		shadow_ = this;
	} else {	// as for HTML FORM menu
		shadow_ = new LeafShadow("menu",null, null, this);
		//oldparent_=null;	// HACK!   if (oldparent_!=null && !oldparent_.contains(this)) oldparent_=null;
		//Document doc = oldparent_.getDocument(); if (doc!=null) setStyleSheet(doc.getStyleSheet());
	}

	bbox.setLocation(x,y);
	//bbox.setLocation(0,0); shadow_.bbox.setLocation(x,y); => no, LeafShadow formats according to shadowed node

	//Browser br = getBrowser(); -- may not have parent (as document popup doesn't), hence no Browser
	br.setCurNode(null);
	br.setGrab(this);
	// take style sheet from root, as silly for each menu to have own stylesheet, and parent document may not be HTML

	// add to menu stack
	List<VMenu> ms = (List<VMenu>)root.getVar(VAR_MENUSTACK);
	if (ms==null) { ms=new ArrayList<VMenu>(10); root.putVar(VAR_MENUSTACK, ms); }
	ms.add(this);

//System.out.println("*** post, "+getFirstLeaf().getName()+", bbox="+bbox);//+", doc="+doc);
//shadow_.dump();
//dump();
	root.appendChild(shadow_);
//System.out.println(root+" / valid="+root.isValid()+", curdoc="+br.getCurDocument()+"/valid="+br.getCurDocument().isValid());
//root.dump(5);

	// not in restore or constructor because may not show menu at all, as in HTML FORM
	activespan_=new ActiveSpan(); activespan_.restore(null,null, root/*== getDocument()*/.getLayer(Layer.SCRATCH));

	posted_=true; tickled_=false;
	//br.repaint(/*10*/);	// vs this.repaint() on post only, to format dynamically constructed menus, as 0x0 otherwise
//System.out.println("bbox = "+Rectangles2D.pretty(bbox));
	if (/*!isValid()*/bbox.width <= 0/*generating -- they're all invalid as switching parents*/) {
		if (p!=null) p.markDirty();
//System.out.println(p+", valid="+root.isValid()+", curdoc="+br.getCurDocument()+"/valid="+br.getCurDocument().isValid());
		/*Document doc = (p==null? root: p.getDocument());
		StyleSheet ss = doc.getStyleSheet();
		Context cx = ss.getContext(null);
		formatBeforeAfter(Integer.MAX_VALUE, Integer.MAX_VALUE, cx);*/
		br.repaint(/*10*/);	// doc.repaint ok to format, but br.repaint to paint whole area
	} else repaint(10);
  }


  /**
	Unshow by removing from root and reconnecting to old parent (if any).
	Independent of mouse clicks.
  */
  public void unpost() {
	if (!posted_) return;
//System.out.println("*** unpost, back to "+oldparent_);
	posted_=false;

	Browser br = getBrowser();
	br.releaseGrab(this);
	//activespan_.destroy(); activespan_=null;
	activespan_.moveq(null);

	Root root = br.getRoot();
//System.out.println("unpost");
	repaint(50);	// put request on queue while still in tree

	// move from absolute space back to parent, repaint
//System.out.println("oldparent_ = "+oldparent_);
	//if (oldparent_!=null) { oldparent_.appendChild(this); oldparent_.repaint(); }
	//else remove();
	shadow_.repaint();	// should be unnecessary
	shadow_.remove(); shadow_=null;

	List<VMenu> ms = (List<VMenu>)root.getVar(VAR_MENUSTACK);	// known to exist as put it there in post
	int inx = ms.indexOf(this);
	if (inx!=-1) {
		for (int i=ms.size()-1; i>inx; i--) ms.get(i).unpost();	// unpost later menus
		ms.remove(inx);	// ArrayList<> has removeRange
	} // else error...
//System.out.println("unpost "+getFirstLeaf().getName());
	//br.repaint(100);	// since we're gone from view now
  }


  /** Lets events pass through so disable behavior works. */
  public boolean eventBeforeAfter(AWTEvent e, Point rel) {
	if (super.eventBeforeAfter(e, rel)) return true;	// posted by somebody else
//System.out.println("eBA "+getBrowser().getCurNode());
//System.out.println("rel="+rel);
	return (rel!=null && contains(rel));	// opaque -- have to check because can be called by self when mouse moves outside
  }

  /** Posted with grab, */
  public void event(AWTEvent e) {
	Browser br = getBrowser();
	Point scrn = br.getCurScrn();

	Point rel = new Point(scrn.x-shadow_.bbox.x+bbox.x, scrn.y-shadow_.bbox.y+bbox.y);
	if (super.eventBeforeAfter(e, rel)) return;	// needed for scrolling

	int eid=e.getID();
	List<VMenu> ms = (List<VMenu>)br.getRoot().getVar(VAR_MENUSTACK);	// known to exist as put it there in post
	// LATER: keyboard control with up/down/return too

	if (eid==MouseEvent.MOUSE_PRESSED) {
		// posting done elsewhere, and not necessarily even posted
		// but eat event so nobody else does and sets a grab
		if (posted_) tickled_=true;	// retains grab, but have to wait until MOUSE_RELEASED to close

	} else if (eid==MouseEvent.MOUSE_DRAGGED || eid==MouseEvent.MOUSE_MOVED) {
		//MouseEvent me = (MouseEvent)e;
//System.out.println("shadow="+shadow_.bbox+" vs "+scrn);
		if (shadow_.bbox.contains(scrn)) {	// direct checking works because posted in screen coordinates
//System.out.println("same menu");
			// find current item
			TreeEvent finde = new TreeEvent(br, TreeEvent.FIND_NODE);
			br.setCurNode(null);
			shadow_/*in abs*/.eventBeforeAfter(finde, scrn);	// should send
			//eventBeforeAfter(e, scrn);

			//Mark curm = br.getCurMark();
			//Node child = (curm==null? null: curm.leaf);	// can be null if disabled
			Node child = br.getCurNode();
			if (child!=this && contains(child/*null ok*/)) while (child.getParentNode()!=this) child=child.getParentNode();
//System.out.println("in VMenu @ "+scrn+", child="+child+"/"+child.childNum());

//if (activenew_!=null && activenew_!=activeitem_) System.out.println("inside "+activenew_.getFirstLeaf().getName());
			if (activenew_!=child && child!=this) {
				//for (int i=ms.size()-1, inx=ms.indexOf(this); i>inx; i--) ms.get(i).unpost();	// unpost sibling menus

				//if (activenew_!=null) activenew_.repaint(50);
				if (activenew_!=null) { Rectangle bbox=activenew_.bbox; /*shadow_.*/repaint(50, bbox.x,bbox.y, bbox.width,bbox.height); }
				activenew_=child;
//if (activenew_!=null) { System.out.println("inside "+activenew_.getFirstLeaf().getName()); }
				if (child==null) activespan_.moveq(null);
				else {
					activespan_.moveq(child.getFirstLeaf(),0, child.getLastLeaf(),child.getLastLeaf().size());
					//child.repaint(50);
					Rectangle bbox=child.bbox; /*shadow_.*/repaint(50, bbox.x,bbox.y,bbox.width,bbox.height);
//System.out.println("rel x,y = "+child.getRelLocation());

					if (child instanceof VMenuButton) {	// cascade menu
						br.releaseGrab(this);	// => keep grab until move into other menu
						((VMenuButton)child).post();
//System.out.println("in menubutton post "+child.getFirstLeaf().getName());
					}
				}
				//activeitem_ = activenew_; -- NO!
				//repaint();	//-- handled by span, for now => using moveq, so not
				//repaint();	// whole menu, but fast enough -- just old and new items, above
			}
			tickled_=true;

		} else {	// drag/move outside of current menu
			// gone back to a parent menu, or sweeping over menubar?
			// kludgy for now: can be confused by any VMenuButton, ...
			activenew_ = null;	// but leave activespan
			Root root = br.getRoot();
			br.setCurNode(null);
			root.eventBeforeAfter(new TreeEvent(br,TreeEvent.FIND_NODE), scrn);

			if (br.getCurNode()!=null) {
//System.out.println("over "+br.getCurNode().getFirstLeaf());
				for (Node n=br.getCurNode(); n!=null; n=n.getParentNode()) {
					if (n instanceof VMenuButton) {
//System.out.println("menubutton: "+n.getFirstLeaf().getNextLeaf());
						br.releaseGrab(this);
						VMenu m = null;
						if (n.getParentNode() instanceof VMenu) {
							m = (VMenu)n.getParentNode();
							//m.setSelected(n);	// WRONG!  Want:  m.activenew_ = n;
							//m.event(e);
							//m.repaint(100);
							int inx = ms.indexOf(m);
							m = ms.get(inx+1 < ms.size()? inx+1: 0);
						} else {
							m = ms.get(0);	// should have me at least
						}
						//if (m!=null)
						m.unpost();	// clear nested menus
						//if (n!=oldparent_)
						//n.getParentNode().repaint(100);
						((VMenuButton)n).post(m);	// left to menubutton to re-post
						//br.eventq(e);
//System.out.println("out menubutton post "+m.getFirstLeaf().getName());
						break;

					} else if (n instanceof VMenu) {
						br.releaseGrab(this);
						VMenu m = (VMenu)n;
//System.out.println("menu: "+n.getFirstLeaf().getNextLeaf()+" this? "+(n==this));
/*						int mei=ms.indexOf(this), otheri=ms.indexOf(m);
System.out.println("mei="+mei+", otheri="+otheri);
						if (mei+1 == otheri) {
							br.releaseGrab(this); br.setGrab(m);
						} else {*/
						if (m!=this && ms.indexOf(m) != ms.size()-1) {
							int x=m.bbox.x, y=m.bbox.y;
							m.unpost();	// clear nested menus
							m.post(x,y, br);	// but keep this one, and get grab again
						}
						//br.eventq(e);
						//}

//System.out.println("other menu "+m.getFirstLeaf().getName()+" vs "+getFirstLeaf().getName()+": unpost/post");
						break;

					} //else System.out.println("not in menu or menubutton");
				}
			}
//System.out.println("over "+br.getCurMark()+", parent="+getParentNode());
		}

	} else if (eid==MouseEvent.MOUSE_RELEASED) {
		if (posted_ && tickled_) ms.get(0).unpost();	// tear down everybody
		br.repaint(100);	// everybody, since VMenuButton may be showing item from menu
		if (activenew_!=null && activenew_!=activeitem_) {
			setSelected(activenew_);
			invoke();
		}
	}
  }

  /** Execute active item's script, if any. */
  public void invoke() {
	if (activeitem_!=null) VScript.eval(activeitem_.getValue("script"), getDocument(), activeitem_.getAttributes(), activeitem_);
  }


  public void repaint(long ms, int x, int y, int w, int h) {
	//System.out.println(getName()+", h="+h+" vs "+bbox.height);
	if (shadow_!=null && shadow_!=this) shadow_.repaint(ms, x+dx(), y+dy(), w, h);
	else super.repaint(ms, x, y, w, h);
  }
//	if (p!=null) p.repaint(ms, x+dx(),y+dy(), w, h);


/*  public boolean checkRep() {
	// if content dynamically generated, ok not to have children
	if (size() > 0) assert super.checkRep();

	return true;
  }*/
}
