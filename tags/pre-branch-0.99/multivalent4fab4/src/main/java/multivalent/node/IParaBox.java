package multivalent.node;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Point;
import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;
import multivalent.Node;
import multivalent.Leaf;
import multivalent.TreeEvent;
import multivalent.Browser;



/**
	Like a paragraph, layout children in a row, wrapping lines as necessary.
	Supports margins and floats.  The most heavily used node type.

	<p>LATER: maybe introduce (and remove) a nonstructural nodes for each line to balance tree better.

	@version $Revision: 1.4 $ $Date: 2002/02/02 13:41:39 $
*/
public class IParaBox extends INode {
  static final Leaf SPACE = new Leaf(" ", null, null);


  public IParaBox(String name, Map<String,Object> attr, INode parent) { super(name,attr, parent); }

  // have to be really careful about marks when splitting and recombining screen lines
  public boolean formatNode(int width,int height, Context cx) {
//System.out.println("entering IParaBox "+this+" format "+width);//+" - "+xoff+" = "+mwidth);
	boolean shortcircuit = false;
	boolean probe = (width<=0 || width>PROBEWIDTH/2);	 // MAX_VALUE/2 because gets shrunk by borders, margins, ...

	int x=0, y=0;
	//y += cx.flowFloats(y, width);	// flushing floats can return a height>0
	//int xoff=cx.getFloatWidth(LEFT), floatrightw=cx.getFloatWidth(RIGHT);
	//int mwidth = width - xoff - floatrightw;
	int xoff=0, floatrightw=0;	// -- initialized in body
	int mwidth = width;// - xoff - cx.getFloatWidth(RIGHT);
	int maxbaseline=0;
	int firstonline=0;
	int maxw=0, wordcnt=0;
	int maxmt=0, /*ml=0,*/ maxmb=0, mr=0, prevmb=0;	  // margins: collapse left&right among siblings on same line, top&bottom between lines
	Node child, prevchild=null; //,lbchild=null;
	Rectangle cbbox;

	if (size()>0)	// don't flush phantom final child + 1 if empty (which happens)
	for (int i=0,imax=size(); i<=imax && !shortcircuit; i++, prevchild=child) {	// i<=imax because final pass to flush partial line
		boolean ffloat = false;
		// 1. FORMAT CURRENT CHILD
		if (i<imax /* or child!=null*/) {
			child = childAt(i);
			cbbox=child.bbox;
			if (!child.isValid()) {
				if (!cx.valid) cx.reset(child,-1);
				shortcircuit = child.formatBeforeAfter(width/*not mwidth--INodes subtract on their own*/,height, cx);
			} else if (child.sizeSticky()>0) cx.valid=false;

			if (child.floats==LEFT || child.floats==RIGHT) {
//System.out.println("IParaBox: queueing child #"+i+"  "+child.getName()+" "+child.getAttr("src"));
				ffloat = true;
				cx.queueFloat(child);
				//continue; -- no, might get flowed right away
			}
		} else {	// final pass to flush last, partial line
			child=null; cbbox=null;
		}
//if (child!=null) System.out.println(child.getName()+", xoff="+xoff+", x="+x+", wordcnt="+wordcnt);


		// text a very special case => just ask children to try to lay themselves out within available remaining width.  I suppose some could scale, but for now it's just text that does something special, namely splitting the node
/*
		if (x>0 && <would cause linebreak && n instanceof LeafText) {
			// chop up text at (after) spaces, first to last, until doesn't fit anymore
			// insert new node at current point,
			// redistribute spans (directly move start node, offset -- since new node shares same parent as old doesn't affect summary information in tree)
		}
*/
		// 2. LINEBREAK: set screen line
		// special cases: 0-width BR, floats only (just queue), not first time through loop,
		// even floats can force a linebreak, if we wouldn't have enough room to place it
		if (wordcnt>0 && //i>firstonline || i==imax) &&///*i>0 && */(x>0 /*-- 0-width BRi>firstonline ||*/ i==imax/*sole item is 0-width, as for BR*/) && // non-empty line (in particular, doesn't happen first time through loop, so get chance to flow floats before setting first line)
			(cbbox==null || prevchild.breakAfter() || (!ffloat && (child.breakBefore() || x + cbbox.width > mwidth)))) {
			//if (y==0) System.out.println("top of "+getName()+" with "+wordcnt+" words, margin="+maxmt);
			wordcnt=0;
			Node lbchild = childAt(firstonline);
			// item too big to fit between floats, but enough width overall: scan down until enough floats are done
			if (firstonline+1 == i && (lbchild=childAt(firstonline)).bbox.width > mwidth && lbchild.bbox.width < width) {
				// LATER
			}

			if (y==0) prevmb=maxmt;
			int ydelta = Math.max(prevmb, maxmt);

			// line justification: LEFT, RIGHT, FILL, CENTER
			int xdelta=0; double justinter=0.0;
			/*if (cx.justify==RIGHT) xdelta=mwidth-x;
			else if (cx.justify==CENTER) xdelta=/*0;* /(mwidth-x)/2;
			else if (i-firstonline>1 && cx.justify==FILL) justinter=(mwidth-x)/(double)(i-firstonline-1);*/
// if (align==RIGHT) System.out.println("probe="+probe+", width="+width);
			int diff=mwidth-x;
			if (probe || diff<0) {} // if don't have enough horizontal space diff<0
			else if (align==RIGHT) xdelta=diff;
			else if (align==CENTER || (firstonline+1==i && prevchild.floats==CENTER)) { xdelta=diff/2; maxw=Math.max(maxw, mwidth); }
			else if (align==FILL && (diff<20 || (i-firstonline>1 && i/*+1*/<imax && !(childAt(i).breakBefore() || (childAt(i).bbox.width==0/*not formatted yet*/ && childAt(i).breakAfter()))))) justinter=diff/(double)(i-firstonline-1);	 // don't fill last line
			//else if (align==LEFT || align==NONE) no adjustments
//if (align==CENTER) System.out.println("IParaBox align = "+align+" on "+getName());

			// alignment and common baseline
			int lh=0, ih=0;		// height due to children which are leaves, internal
			for (int j=firstonline; j<i; j++) {
				lbchild = childAt(j);
				Rectangle lbbox = lbchild.bbox;
				boolean isLeaf = lbchild.isLeaf();
				String lbname = lbchild.getName();

				// skip floats
				if (lbchild.floats==LEFT || lbchild.floats==RIGHT) continue;
//System.out.println("skipping "+lbchild.getName());

				// translate child to justification, common baseline, margin
				lbbox.translate(xdelta+(int)((j-firstonline)*justinter), maxbaseline - lbchild.baseline + ydelta);	// baseline itself still ok: it's relative to y

				// bump leaves and rectangular blocks over by amount of left floats
				int lbeffh = lbbox.y + lbbox.height - y;	//+ lydelta;
				if (isLeaf || "pre".equals(lbname) || "table".equals(lbname)) {	 // special case non-flows
					lbbox.translate(xoff,0); // only translate leaves!
					lh = Math.max(lh,lbeffh);
				} else {
					ih = Math.max(ih,lbeffh);
//System.out.println("internal: "+lbchild.getName());
				}
			}
//if (lh>ih) System.out.println("eating "+(lh-ih)+" @ "+prevchild.getName());
			if (lh>ih) cx.eatHeight(lh-ih, prevchild,prevchild.size()); //lbchild,lbchild.size());
//if (lh>ih) { System.out.println("eating "+(lh-ih)+", LEFT to go="+cx.getFloatHeightToGo(LEFT)); }
			// may have exhausted one or more floats--reset width on next round

			//maxw = Math.max(maxw, x+xoff);
			Rectangle lbbox = prevchild.bbox;	// prevchild is last child on current line
			//if (i<imax || cx.justify!=LEFT) maxw=width; // if filled entire line and more to go, max out to passed width -- maybe
//System.out.println("max leaf x = "+(lbbox.x+lbbox.width)+" @ "+prevchild.getName());
			maxw = Math.max(maxw, lbbox.x + lbbox.width /*+ floatrightw*/); // + floatrightw so include floats on right
			//y = lbbox.y + lbbox.height;	-- NO!
			y += Math.max(ih, lh);
			//y += maxh; height -= maxh;

			// reset for next line
			x=0;
			prevmb=maxmb; maxmt = maxmb = mr = 0;
			maxbaseline=0; firstonline=i;
		}


		// 3. FLOW FLOATS at start of new line
		if (x==0) {// && i<imax/*wordcnt==0?*/ /*&& i<imax?*/) {
//if (cx.floatStackSize()>0) System.out.println("flow floats @ #"+i+", x="+x+", wordcnt="+wordcnt+", xoff="+xoff+",  mwidth="+mwidth);
//int y0=y;
//System.out.print("*** flushing "+cx.flush+" @ "+prevchild+(prevchild!=null? " clear="+prevchild.getAttr("clear"): ""));
//System.out.print("*** flushing "+cx.flush+" @ "+prevchild+(prevchild!=null? " clear="+prevchild.getAttr("clear"): ""));
			//y += cx.flushFloats(cx.flush); -- can have pending floats that should be flushed, so flow then flush

			cx.flowFloats(y, width);	// flushing floats can return a height>0
			y += cx.flushFloats(cx.flush);
//System.out.print(", dy="+(y-y0));

			// LATER: add floats to this.bbox
			xoff=cx.getFloatWidth(LEFT); floatrightw=cx.getFloatWidth(RIGHT);
//System.out.println(", new xoff = "+xoff);
			mwidth = width - xoff - floatrightw;
		}


		// 4. SET CURRENT CHILD, pending common baseline, alignment, top margin
		//if (child==null) {}
		//else if (ffloat) cx.queueFloat(child);
		//else {
		if (!ffloat && child!=null) {
			// FLOAT -- postpone until new line
/*			if (child.floats==LEFT || child.floats==RIGHT) {
//System.out.println("queueing "+child.getName());
				cx.queueFloat(child);
				continue;
			} else */
			wordcnt++;

			if (child.isStruct()) {
				// HACK for floats, which don't know extent of tree to mark dirty.	helps but doesn't account for preceding floats and has other problems probably
				if (child.bbox.width > xoff+mwidth /*child.bbox.x != x+xoff*/) { // not right because INodes contain floats, so could >mwidth and stlil be valid
					child.markDirtySubtree(false);
					shortcircuit = child.formatBeforeAfter(width,height, cx);
				}

				// margins: collapse left&right, running max for top and bottom on line
				Insets mar = ((INode)child).margin;
//if (mar.top > 50) System.out.println(getName()+"/"+child.getName()+", mar.top="+mar.top);
				maxmt = Math.max(maxmt, mar.top);	// collapsed with previous bottom when set line
				maxmb = Math.max(maxmb, mar.bottom);
				x += Math.max(mr, mar.left);	// collapse with previous
				mr = mar.right;

//System.out.println("zapping "+child.getName());
			} else {	// leaf
				mr=0;
			}

			if (cbbox.width>PROBEWIDTH/2) {	// X this shouldn't happen now that tables don't send out MAX_VALUE for width
System.out.println(child+" has INFINITE WIDTH!");	  // this can happen during table probes -- still?
//child.dump();
				cbbox.width=mwidth;
			}
//System.out.println(child.getName()+"	"+x+","+y+", "+cbbox.width+"x"+cbbox.height+", mwidth="+mwidth);

			cbbox.setLocation(x,y);

			// tabs -- don't do this here
			if ("\t".equals(child.getName())) {
				int sw = (int)cx.getFont().charAdvance(' ').getX();
				cbbox.width = sw*8 - x%sw - sw/*implicit following space*/;
			}
			x += cbbox.width;
			if ((cbbox.width!=0 || cbbox.height!=0) && child instanceof LeafText) x += cx.getFont().charAdvance(' ').getX() + 1/*rounding*/ + 1/*antialiasing or bitmap fuzz*/;
			if (child.baseline > maxbaseline) maxbaseline=child.baseline;
		}
	}
	//y += prevmb;//maxmb; //-- parent deals with this -- WRONG

	//if (!probe && align!=LEFT && align!=NONE) maxw = Math.max(maxw, width);
	bbox.setSize(maxw, y+prevmb);	// usually right, but weird case (e.g., www.cs.berkeley.edu/~wilensky/) where float within one struct flows through to next struct, so bboxes overap and sum of widths wrong
//System.out.println("parabox width = "+maxw);
	baseline = y;

	if (mwidth < width) {	// propagate dirty when float active -- not so great because does extra work sometimes and not enough work other times
		INode p = getParentNode();
		int rsib = childNum()+1;
		if (rsib < p.size()) p.childAt(rsib).markDirtySubtreeDown(false);
	}

	valid_ = !(shortcircuit || probe);
	return shortcircuit;
  }



  /** <i>Needs refurbishing</i>. */
  public void reformat(Node dirty) {
  // full format if would spill into next line as that's infrequent enough during typing
//	  Context cx = new Context(); cx.styleSheet = getDocument().getStyleSheet(); cx.reset(dirty);
/* move this to format or Context
	Point absloc = getAbsLocation();
	DocumentAbs abslayer = (DocumentAbs)getDocument().getVisualLayer("FLOAT");
	List<Node> floats = null;
	if (abslayer!=null) {
		floats = findNodesClip(new Rectangle(absloc.x,absloc.y, bbox.x,bbox.y));	// finds shadow nodes
		// clear out nested floats, which are already accounted for by parent
		for (int i=floats.size()-1; i>=0; i--) {
			INode p = floats.get(i).getParentNode();
			String align = p.getAttr("align");
			if (align!=null && ("LEFT".equals(align) || "RIGHT".equals(align))) {
				floats.remove(i);
				break;
			}
		}
	}

	// fact that using shadow nodes and reverse order on right don't matter as just using to summaize previous state
	// actually only care about right margin...
	for (int i=0,imax=floats.size(); i<imax; i++) {
		Node n = (Node)floats.get(i);
		int align = ("LEFT".equalsIgnoreCase(n.getAttr("align"))? LEFT: RIGHT);
		int heighttogo = n.bbox.y - absloc.y;	// n in absolute layer
		cx.addFloat(n, heighttogo);
	}
*/
/* dirty already reformatted
	// it change still fits in current line, handle it here; else abort
	int owidth=dirty.bbox.width, oheight=dirty.bbox.height, obaseline=dirty.baseline;
	dirty.formatBeforeAfter(Integer.MAX_VALUE, owidth,oheight/*Integer.MAX_VALUE* /, cx);

	int dw = dirty.bbox.width - owidth;
	if (dw==0) return;
*/
//retrieve this:	if (dirty.bbox.height!=oheight || dirty.baseline!=baseline) { super.reformat(dirty); return; }

	// find last node on line
	Node lastn=dirty, nextline=null;
	int dirtyx=dirty.bbox.x, leftx=dirtyx, rightx = leftx + dirty.bbox.width, baseline=dirty.baseline;
	int miny=dirty.bbox.y, maxy=dirty.bbox.y+dirty.bbox.height;
	int oldrightx = rightx;
	for (int i=dirty.childNum()+1,imax=size(); i<imax; i++) {
		Node nextn = childAt(i);
		if (nextn.floats==LEFT || nextn.floats==RIGHT) continue;
		if (nextn.bbox.x > dirtyx /*nextn.baseline == baseline*/ /*nextn.bbox.x > x*/) {
			lastn=nextn;
			Rectangle cbbox = nextn.bbox;
			oldrightx = cbbox.x+cbbox.width;
			cbbox.x = leftx = rightx + (cbbox.height>20?7:3); rightx = leftx + cbbox.width;
			miny = Math.min(miny,cbbox.y); maxy = Math.max(maxy, cbbox.y+cbbox.height);
//System.out.println("@ "+nextn.getName()+" maxy="+maxy);
		} else { nextline=nextn; break; }
	}
//System.out.println("last on line: "+lastn.getName());
	// given change in size of dirty, is last still last (no flow to next, no flow from next line)?
	// for now, ignore possibility of right floats
	int lm = bbox.width/*- cx.getFloatWidth(RIGHT)*/;
	if (rightx <= lm && nextline!=null && rightx+nextline.bbox.width > lm) {
//System.out.println("within line, bbox="+new Rectangle(dirty.bbox.x,miny, rightx-dirty.bbox.x,maxy-miny));
		repaint(10, dirty.bbox.x,miny, Math.max(rightx,oldrightx)-dirty.bbox.x,maxy-miny);
	} else { /*System.out.println("*** para reformat");*/ super.reformat(this); }
  }



  /**
	Paint (nonexistent) spaces between words, as in background for selection and highlights, underline for hyperlinks.
	<!-- Moved here from INode => put into HBox and IHBox too, well HBox extends IParaBox can does format.super(width=MAX_INTEGER). -->
  */
  public void paintNode(Rectangle docclip, Context cx) {
//System.out.println("painting "+getName()+", bbox="+bbox+", fg="+cx.foreground+", bg="+cx.background+", valid="+valid_);
	Graphics2D g = cx.g;
	Color bgin = cx.pagebackground;

	int curbaseline=-1, prevbaseline=-1;
	///int stopy = docclip.y + docclip.height;	   // doesn't have much effect since docclip not reduced in height
	int starty=docclip.y, stopy = starty + docclip.height;    // ok
	//Rectangle r = g.getClipBounds();
	//int starty=0, stopy = r.height;
	//Node prev=null, child=null;
	//boolean first=true;
	for (int i=0,imax=size(); i<imax /*&& curbaseline<stopy*/; i++) {//, prev=child) {
		Node child = childAt(i);
		Rectangle cbbox = child.bbox;
		// later take into consideration prevailing scale in Context (with inlined, specialized version of intersects)

		// buggy, unfortunately.  skipping probably doesn't save much as have to iterate through all whether ask node itself or not
		//if (cbbox.y + cbbox.height < starty) { cx.valid=false; /*System.out.println("skip "+child.getName()+" @ "+cbbox.y);*/ continue; }
		//else//*/ //if (cbbox.y > stopy) { cx.valid=false; /*System.out.println("stop @ "+cbbox.y);*/ break; }
		if (prevbaseline > stopy) { cx.valid=false; /*System.out.println("stop @ "+child.getName()+" / y="+cbbox.y);*/ break; }
		//if (first) { if (i>0) System.out.println("first=#"+i+"/"+child.getName()); first=false; }

		// Handle space, underline, whatever between this and previous leaf
		// Leaf nodes draw own background but no space leaf convering interword space.
		// implemented as drawing a dummy space node => put real spaces into nodes? harder to fill justify
		// should just set bbox and call space.paintBeforeAfter()
		// if cx not valid, then didn't draw previous node, so don't draw interstitial space
		int bl = child.baseline + cbbox.y;
		if (i>0 && cx.valid && curbaseline == bl) {
			int x=(int)cx.x, y=cbbox.y, w=cbbox.x-x+1, h=cbbox.height;
			SPACE.bbox.setBounds(x,y, w,h);
			//Rectangle pbbox = prev.bbox;
			//SPACE.bbox.setBounds(cx.x,pbbox.y, cbbox.x-cx.x+1,pbbox.height);	  // height of previous bbox
			SPACE.baseline = curbaseline;//child.baseline; -- don't do g.transform() with paintBeforeAfter
			//SPACE.baseline = child.baseline;
//System.out.println("space = "+SPACE);

			if (cx.background!=null && !cx.background.equals(bgin)) { g.setColor(cx.background); g.fillRect(x,y, w,h); }
			//if (cx.background!=cx.pagebackground && cx.background!=null) { g.setColor(cx.background); g.fill(SPACE.bbox); }
			//SPACE.paintBeforeAfter(g, docclip, cx);
			cx.paintBefore(cx, SPACE);
			cx.x = cbbox.x;
			cx.paintAfter(cx, SPACE);
			//g.setColor(java.awt.Color.GREEN); g.draw(SPACE.bbox);

			// later handle vertical space too

		} else if (child.floats==NONE) {
			prevbaseline = curbaseline;
			curbaseline = bl;
		}
		//cx.x = cbbox.x;

		cx.pagebackground = bgin;
		child.paintBeforeAfter(docclip, cx);
		cx.x = cbbox.x+cbbox.width;
		//cx.x = cbbox.width;
//System.out.println();
	}
  }


  /**
	Stops at first node where bbox.y > rel.y.
	Can "hit" in between nodes to pick up prevailing spans (cur node set to first node following, with offset -1).
  */
  public boolean eventNode(AWTEvent e, Point rel) {
	boolean hitchild=false;
//System.out.println(getName()+" p "+rel);
	boolean shortcircuit=false;

	// faster version of super.eventNode(e, rel);
	//for (int i=size()-1; i>=0 && !shortcircuit; i--) {
	int i=0;	// used for backtracking on !hitchild
	int prevbaseline=-1, curbaseline=-1;
	for (int imax=size(); i<imax && !shortcircuit && !hitchild; i++) {	// for stop to work
		Node child=childAt(i); Rectangle cbbox=child.bbox;
		int bl = child.baseline + cbbox.y;
		if (bl!=curbaseline && child.floats==NONE) { prevbaseline=curbaseline; curbaseline=bl; }

		if (rel==null) {}
		else if (cbbox.contains(rel)) {
			//System.out.println("hit "+child.getName());
			hitchild=true;  // not Node.contains(Point)
		} else if (rel.y < prevbaseline /*&& child.floats==NONE/*cbbox.y > rel.y--words of different heights*/) {//break;   // stop looking: no hits possible on subsequent nodes
			//System.out.println("stopping at "+child.getName()+": "+cbbox.y+" < "+prevbaseline);
			break;
		}

		shortcircuit = child.eventBeforeAfter(e,rel);	// regardless of bbox or rel==null!
//if (shortcircuit) System.out.println("IParaBox ss @ "+child.getClass().getName()+"   "+child.getName()+"/"+child.getFirstLeaf().getName());
	}


	Browser br = getBrowser();
	Node curnode = br.getCurNode(); // => !hitchild
//if (curnode!=null && curnode.isLeaf()) System.out.println("curnode = "+curnode);
	//if (!shortcircuit && (curnode==null || (curnode.isStruct() && curnode.bbox.width * curnode.bbox.height > bbox.width*bbox.height))/*!hitchild*/) {
	if (!shortcircuit && !hitchild && curnode==null /*|| curnode.isStruct())*/) {
		// if between *leaf nodes*, fake cur node so that pick up prevailing spans
		int eid=e.getID();
		//if (eid==MouseEvent.MOUSE_MOVED || eid==MouseEvent.MOUSE_DRAGGED || eid==TreeEvent.FIND_NODE) {
		if ((MouseEvent.MOUSE_FIRST<=eid && eid<=MouseEvent.MOUSE_LAST) || eid==TreeEvent.FIND_NODE) {
		//if (eid==MouseEvent.MOUSE_MOVED || eid==MouseEvent.MOUSE_DRAGGED || eid==TreeEvent.FIND_NODE) {
			for (Node child=null,prev=null; --i >= 0; prev=child) {
				child=childAt(i); Rectangle cbbox=child.bbox;
				if (cbbox.x < rel.x) {	// stop on this regardless
					if (cbbox.y <= rel.y && cbbox.y+cbbox.width >= rel.y	// y ok
						&& prev!=null	// else off right edge
						&& prev.isLeaf() && child.isLeaf()) {	// consecutive leaf nodes only
//System.out.println("between "+child.getName()+" and "+prev.getName());
						//br.setCurNode(prev,0/*-1*/);	// -1 == fake offset
						// no tickling here => tickling done by leaf, in eventBeforeAfter
//System.out.println("tween tickle "+prev);
						//br.tickleActives(e, rel, prev);
						prev.eventBeforeAfter(e, new Point(prev.bbox.x, prev.bbox.y));  // end of previous
						hitchild=true;
					}
					break;
				}
			}
			if (!hitchild && curnode==null) br.setCurNode(this,0);	// set to lowest enclosing node -- or Leaf only?
		}
	}

//	Node curn = br.getCurNode();
//System.out.println("curn="+curn+", leaf? "+(curn!=null? curn.isLeaf(): false));
//	if (curn!=null && curn.isLeaf() /*&& curm.offset>=0--no*/) br.tickleActives(e, rel, curn);

	return shortcircuit;
  }
}
