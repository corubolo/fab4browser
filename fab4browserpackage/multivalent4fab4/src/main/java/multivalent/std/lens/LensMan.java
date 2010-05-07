package multivalent.std.lens;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.awt.*;

import multivalent.*;



/**
	Manager to coordinate lens painting.

<!--
	<p>To do
	handle pinned lenses: keep track of scroll position.  if see in paintBefore that changed by less than a screenful, short-circuit out from drawing tree, the repaint full (dx=dy=0 by then)
-->
<!--
  Algorithm

  <pre>
	pre passes as before
		clip against clipping region
		group into sets of overlapping
	sort top, bottom, left, right (n log n * 4/number of sides)
	topy = first top
	nexttop = second top, nextbottom = first bottom
	actives = null
	regions = null	[Rectangles]
	sweep down
		boty = min(nexttop, nextbottom)

		leftx = first left in actives set
		xactives = shape corresponding to leftx
		nextleft = second left in actives set
		nextright = first right in actives set
		sweep across actives |active cnt * 2 -3|
			rightx = min(nextleft,nextright)
			current region (leftx,topy, rightx-leftx,boty-topy)

			regionposn = bit sum of intersecting shapes
			if (already have region at regionposn) region[regionposn] = sum of regions
			else region[regionposn] = new region
			(actually array of size 2^n points to packed array of actual intersections)

			if (nextleft<nextright) {
				add to xactives
				if more in actives set, nextleft = next left in actives set
				else set final region and break
			} else {
				remove from xactives
				nextright = next right in actives set
			}
			leftx = rightx


		if (nexttop<nextbottom) {
			add region corresponding to nexttop to actives
		} else {
			remove region corresponding to nextbottom to actives
		}
		nextbottom = first bottom in actives set
		topy=boty

	iterate over array to draw


	<p>LATER: New algorithm due to XXX, pointed out by Franklin Cho, modified.  Just works for rectangles
-->

<!--
	NOTES
	optimization if single lens only? -- no intersection computation
-->

	@see multivalent.std.ui.WindowUI

	@version $Revision: 1.6 $ $Date: 2002/11/18 05:11:41 $
*/
public class LensMan extends Behavior {
  static final boolean DEBUG = false;

  // higher priority gets higher bit position number
  /** Maximum number of <em>intersecting</em> lenses; can have more total. */
  public static final int LENS_MAX = 6;	// not very limiting

  /** Minimum overlap needed to show that lens. */
  static final int INVISIBLE = 10;

  /** For given index number, value is bit position of highest 1. */
  private static final int[] bitvalhigh = new int[1<<LENS_MAX];

  /** For given index number, value is array of bit positions of 1 bits. */
  private static final int[][] bitposn = new int[1<<LENS_MAX][];

  static {	// compute bitvalhigh and bitposn tables (vs table: can increase LENS_MAX easily and know no errors in table)
	int[] bs = new int[LENS_MAX];
	for (int i=0,imax=bitvalhigh.length; i<imax; i++) {
		int high1=0, cnt1=0;
		for (int j=0; j<LENS_MAX; j++) if (((i>>j) & 1) == 1) { high1=1<<j; bs[cnt1++]=j; }

		bitvalhigh[i] = high1;
		int[] b=new int[cnt1]; System.arraycopy(bs,0, b,0, cnt1); bitposn[i]=b;
	}

	checkTables();
  }



  /** As elsewhere, highest priority lens is last in stacking order: drawn last. */
  private List<Lens> vlens_ = new ArrayList<Lens>(LENS_MAX);

  // determine groups of overlapping lenses (exponential expense within groups)
  // keep these data structures around so can be incremental when just moving/resizing top one
  // (also need to cache everything but top lens)

  private boolean inLens_ = false;



  public void addLens(Lens lens) {
	assert vlens_.indexOf(lens)==-1;	// assert or if?
	vlens_.add(lens);
  }

  public void deleteLens(Lens lens) {
	assert vlens_.indexOf(lens)!=-1;
	vlens_.remove(lens);
  }

  /** Raise passed lens to top of stacking order, adding to stack if necessary. */
  public void raiseLens(Lens lens) {
	vlens_.remove(lens); vlens_.add(lens);
  }
/* add if needed
  public boolean containsLens(Lens lens) { return vlens_.contains(lens); }
  public int lensCount() { return vlens_.size(); }
  public Lens lensAt(int i) { return (Lens)vlens_.get(i); }
*/


  /** Return INode under which all Lens VFrame's are stored. */
  public INode getVisualLayer() {
	return getBrowser().getRoot().getVisualLayer("lens", "multivalent.node.IRootAbs");
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	//getBrowser().getRoot().addObserver(this);	// if so, affects menus (and should draw in paintAfter) and window resizing events
	getVisualLayer().addObserver(this);	//-- not always drawn
  }
/*	public void buildAfter(Document doc) {
	doc.addObserver(this);
  }*/


  /**
	Redraws portions of document covered by lenses, combining effects where lenses intersect.
	Lenses kept in same visual layer, on Browser's root, visually above everything except menus.
	Paint by drawing contents (including intersections) here in paintBefore,
	before window apparatus has a chance to clutter visuals,
	then follow normal control flow to draw empty VFrames,
	then nothing to do in paintAfter.
	Document sans lenses has been painted; now redraw parts under lenses.
  */
  public boolean paintBefore(Context cx, Node notused) {
  //public boolean paintAfter(Context cx, Node notused) { -- draw window resize on top
	if (inLens_ || vlens_.size()==0) return true/*false?*/;	// don't recurse on self
	inLens_=true;

	INode root = getBrowser().getRoot();

	// background has been painted.  divide up lenses and repaint
/*
g.setColor(Color.RED);
g.drawLine(0,0, 200,200);
g.drawString("lenscnt="+vlens_.size(), 200,200);
*/

	Graphics2D g = cx.g;
	Rectangle cliprect = g.getClipBounds();	// corresponds to offscreen image... in absolute coordinates


	// priorities: (0) filters, (1) lenses, (2) Spans (ad hoc overrides), (3) structural
	// so before filter, lens, range, struct ... after struct, range, lens(?), filter(?)
	// lens.paintBefore sets context, lens.paintAfter draws window apparatus
	// later, lenses make own calls after setting Context and clipping region


	// 1. Draw all lenses unintersected, low to high
	//    Could be doing useless work if one lens dominated by other or combination of others
	for (int i=0,imax=vlens_.size(); i<imax; i++) {
		Lens lensnow = vlens_.get(i);

		Rectangle paintrect = lensnow.getContentBounds();
//System.out.println("singles, bbox="+paintrect+" vs cliprect="+cliprect+", intersects=?"+paintrect.intersects(cliprect));
		if (!paintrect.intersects(cliprect)) continue;

		Graphics2D lensg = (Graphics2D)g.create();	// new guy that can be trashed
		lensg.clip(paintrect);
		//lensg.setClip(paintrect.x, paintrect.y, paintrect.width, paintrect.height);	// if any overlap paint everything, since weird coordinate transformations
		//lensg.setClip(paintrect.x, paintrect.y, paintrect.x+paintrect.width, paintrect.y+paintrect.height);	// if any overlap paint everything, since weird coordinate transformations
//System.out.println(Rectangles2D.pretty(paintrect)+" vs "+Rectangles2D.pretty(this.getVisualLayer().bbox)+" => "+Rectangles2D.pretty(lensg.getClipBounds()));
//lensg.setColor(Color.RED); lensg.fill(paintrect);
		cx.g = lensg;	//cx.pagebackground=null;

		cx.addBase(lensnow);
		boolean shortcircuit = lensnow.paintBefore(cx, null);
//System.out.println("ss = "+shortcircuit);
//System.out.println("singleton on "+lensnow.getClass().getName());
		if (!shortcircuit) {	// opaque? => then not a lens!
//System.out.println("singleton on "+lensnow.getClass().getName()+", clip="+lensg.getClipBounds());
			cx.valid=false;
			root.paintBeforeAfter(lensg.getClipBounds(), cx);
		} else System.err.println("shortcircuit -- skipping doc content -- opaque lens shouldn't be lens");	// lens that doesn't compose?  (If opaque, then shouldn't be lens.)
		cx.deleteBase(lensnow);

//lensg.setColor(Color.BLUE); lensg.drawLine(paintrect.x, paintrect.y, paintrect.x+paintrect.width, paintrect.y+paintrect.height);
		lensg.dispose();

		// after trying a variety of places, drawing here seems to work best:
		//cx.g=g; lensnow.paintAfter(cx, null);
		cx.valid=false;
	}



	// if (vlens_.size()==1) ... skip to #3

	// 2. Compose lenses.
	//    Need to be as efficient as possible here due to exponential explosion in intersections
	int vlcnt=0;

	// 2A. COLLECT GROUPS of overlapping lenses, to reduce exponential intersection drawing.
	// Every lens in a group overlaps with at least one other, but not necessarily every other lens.
	List vlg[] = new ArrayList[LENS_MAX];	// lens Behaviors (LENS_MAX applies to number of lenses in group, not number of groups)
	Rectangle[] lgbbox = new Rectangle[LENS_MAX];	// bbox enclosing entire group.  max # groups = max # lenses, as each lens can be own group.
	for (int i=0,imax=vlens_.size(); i<imax; i++) {
		Lens lensnow = vlens_.get(i);
		Rectangle rectnow = lensnow.getContentBounds();
		if (!rectnow.intersects(cliprect)) continue;

		// intersect with an existing group?
		boolean connected=false;
//		if (!lensnow.paintBefore(null, opaque, null))	// opaque lenses don't intersect with lower priority
		for (int j=0; j<vlcnt; j++) {
			if (rectnow.intersects(lgbbox[j])) {
			// tangible overlap to count (Rectangle's intersects expression with fuzz factor) -- doesn't make much difference
/*			&& (!(rectnow.x + rectnow.width + INVISIBLE <= lgbbox[j].x
				|| rectnow.y + rectnow.height + INVISIBLE <= lgbbox[j].y
				|| rectnow.x >= lgbbox[j].x + lgbbox[j].width + INVISIBLE
				|| rectnow.y >= lgbbox[j].y + lgbbox[j].height + INVISIBLE)) */

				// if have too many overlapping lenses, kick out lowest priority (graceful degradation)
				if (vlg[j].size() >= LENS_MAX) vlg[j].remove(0);	// could recompute bbox, but should be rare

				lgbbox[j].add(rectnow);
				vlg[j].add(lensnow);

				connected = true;
				//break; -- might have multiple intersections
				// might encompass higher groups now, but prefer more groups of smaller size as intersection is exponential, vs collapsing groups now found to have common member
			}
		}

		if (!connected) {	// not part of any existing group: create new group
			lgbbox[vlcnt] = rectnow;
			//vlg[vlcnt].clear();
			vlg[vlcnt] = new ArrayList<Lens>(LENS_MAX);
			vlg[vlcnt].add(lensnow);
			vlcnt++;
		}
	}

	// 2B. now take INTERSECTIONS to get paintrects and paint intersections
	for (int i=0; i<vlcnt; i++) {
		List<Lens> lg = (List<Lens>)vlg[i];
		int lgsize = lg.size();
		if (lgsize<=1) continue;	// single lenses already drawn

		// exponential intersections within connected component!
		Rectangle[] combos = new Rectangle[1<<LENS_MAX];	// for lens combinations given by bit positions, indexed starting at 1
		for (int j=0; j<lgsize; j++) combos[1<<j] = ((Lens)lg.get(j)).getContentBounds();
// if no intersections at given power of 2, can stop early.
// for lenses, unlikely to be many in given set of connected components, so it doesn't matter much

		// dynamic programming to build up intersections: take intersection of <same bits except high> with <high only>
		for (int j=3,jmax=(1<<lgsize); j<jmax; j++) {	// case 0=no lenses, cases 1&2 singleton lenses that are handled above
			int bvh = bitvalhigh[j];
			if (j==bvh) continue;	// singleton lenses handled above

			Rectangle combo = null;
			Rectangle rlower = combos[j-bvh];
			if (rlower!=null) {
				Rectangle inter = rlower.intersection(combos[bvh]);	// combos[bvh] never null as set above
				if (inter.width>=INVISIBLE && inter.height>=INVISIBLE) {	// empty rectangle if no intersection
					combo = inter;

					// If dominates lower intersection, zap lower because if further lenses contains lower it contains current as well
					// Propagates up to that intersection's intersections too.
					// Doesn't happen very often -- worth saving a small tree traversal or two by taxing every intersection?
					if (rlower.width - inter.width < INVISIBLE &&/*not ||*/ rlower.height - inter.height < INVISIBLE) combos[j-bvh] = null;
				}
			}
			combos[j] = combo;
		}


		// paint lenses inline rather than building up and tearning down a data structure for each connected group
		// each non-null intersection

		// 3. draw intersections low to high (intersections of more lenses on top of intersections with fewer)
		// don't fold this into loop above, because wouldn't gain much, deep nesting, more work in case of dominated lower intersection
		for (int j=3,jmax=(1<<lgsize); j<jmax; j++) {
			if (combos[j]==null || j==bitvalhigh[j]) continue;	// 0 or 1 lens in group
//System.out.println("group size = "+bitposn[j].length);

			Rectangle paintrect = combos[j];
//	  Graphics2D lensg = offset.create(xoff+paintrect.x,yoff+paintrect.y, paintrect.width,paintrect.height);
			Graphics2D lensg = (Graphics2D)g.create(/*br.xoff+*/paintrect.x,/*br.yoff+*/paintrect.y, paintrect.width,paintrect.height);
			//Graphics lensg = offset.create(paintrect.x,paintrect.y, paintrect.width,paintrect.height);
			lensg.translate(-(/*br.xoff+*/paintrect.x), -(/*br.yoff+*/paintrect.y));
			cx.g = lensg; //cx.pagebackground=null;
			//lensg.translate(-paintrect.x, -paintrect.y);
//			  offset.setColor(Color.RED); offset.drawRect(0,0, paintrect.width-1,paintrect.height-1);	// draw intersections

			// lenses outside of structure: they're purely geometric (filters==full screen lenses => regular, though high priority, behaviors)
			// shortcut in lens mean it handles all drawing in that lens.  (Note does this)
			boolean shortcircuit = false;

			// draw lenses low to high so high can overwrite
			//int k;
			//lensbase.hardreset();
			//for (int k=bitposn[j].length-1; !shortcircuit && k>=0; k--) { -- priority direction reversed
			for (int k=0,kmax=bitposn[j].length; k<kmax; k++) {
				Lens lensnow = (Lens)lg.get(bitposn[j][k]);
//System.out.println("add lens "+lensnow.getAttr("title")+", priority="+lensnow.getPriority());
				cx.addBase(lensnow);
				shortcircuit = lensnow.paintBefore(cx, null);
				//lensbase.slipStruct(lensnow);
//				lensbase.addStruct(lensnow);
			}
			// at long last, an intersection to draw!
			//cx.reset(null,-1); cx.valid=false;

			if (!shortcircuit) {	// lenses can be opaque
				// before range, before struct, after struct, after range in context of tree walk
				cx.valid=false;
				//try {
				root.paintBeforeAfter(lensg.getClipBounds(), cx);
				//} catch (Exception e) { e.printStackTrace(System.out); System.out.println("@ intersection "+j+" "+lensg.getClipBounds()); }
			}
			lensg.dispose();
			cx.clearBase();	// WRONG => can be other things besides lenses
		}
	}

	// 4. draw lens apparatus, low to high
	//	  always see titles and regions, even if wholly dominated
//	  for (i=vlens_.size()-1; i>=0; i--) vlens_.get(i).paintAfter(offset, base, null);
	//cx.g = (Graphics2D)g.create();	// want normal Graphics2D (e.g., magnified throws out right and bottom borders)
	cx.g = g;	// no weird transformations in paintAfter
	for (int i=0,imax=vlens_.size(); i<imax; i++) vlens_.get(i).paintAfter(cx, null);
	//cx.g.dispose();

	cx.g = g;
	inLens_=false;

	return false;
  }


  /** While painting lenses, shortcircuit higher ups until done. */
  public boolean paintAfter(Context cx, Node root) {
// LATER, if dominate document, don't draw w/o lenses
/*
	// for now, just check against top lens
	Rectangle toplensBbox = (vlens_.size()>0? vlens_.get(0).getBounds(): null);
	boolean domX = (toplensBbox!=null) && (cliprect.x>=toplensBbox.x && cliprect.x+cliprect.width <= toplensBbox.x+toplensBbox.width);
	boolean domY = (toplensBbox!=null) && (cliprect.y>=toplensBbox.y && cliprect.y+cliprect.height <= toplensBbox.y+toplensBbox.height);
	if (true/*!inDrag* /) {
		if (!domX || !domY) root_.paintBeforeAfter(offset, cliprect, lensbase); -- back in Browser.java
		//else System.out.println("dominated");
		// NOT part of paintAfter: showing affected is independent of active state of behavior

	} else /*inDrag* / {
		if (!domX || !domY) g.drawImage(offImage, 0,0, this);	// speed up with Java 1.1
		offset = g.create(); offset.clipRect(cliprect.x,cliprect.y, cliprect.width,cliprect.height);
		offset.translate(-xoff-rx, -yoff-ry); cliprect = offset.getClipBounds();
	}
*/

	//return false;	// always draw document below
	return inLens_;
  }


  // also events: on eventBefore, if intersects lens, short-circuit
//* interferes with Magnify resize, so disable for now
/*
  private int essi = -1;
  public boolean eventBefore(AWTEvent e, Point rel, Node obsn) {
	if (super.eventBefore(e,rel,obsn) || getBrowser().getGrab()!=null) return true;

	essi = -1;
	for (int i=vlens_.size()-1; i>=0; i--) {
		Lens lens = vlens_.get(i);
		if (lens.getContentBounds().contains(rel)) {	// don't mess with window apparatus
			if (lens.eventBefore(e, rel, null)) { essi=i-1; return true; }
		}
	}
	return false;
  }
*/
  public boolean eventAfter(AWTEvent e, Point rel, Node obsn) {
	//for (int i=essi+1,imax=vlens_.size(); i<imax; i++) {
	for (int i=0/*essi+1*/,imax=vlens_.size(); i<imax; i++) {
		Lens lens = vlens_.get(i);
		if (lens.getContentBounds().contains(rel)) {	// don't mess with window apparatus
			//if (lens.eventAfter(e, rel, null)) return true;
			return true;	// treat lenses as opaque vis-a-vis content
		}
	}

	return super.eventAfter(e, rel, obsn);
  }
//*/

  /** LensMan is spontaneously generated -- nothing to save. */
  public ESISNode save() { return null; }


  /**
	Verify computations against hand-computed tables.
	Computed tables are scalable to arbitrary lens nesting.
  */
  private static void checkTables() { if (DEBUG) {
	//System.out.println("LENS_MAX="+LENS_MAX+", "+(1<<LENS_MAX));
	//System.out.println("bitvalhigh.length="+bitvalhigh.length+", bitposn.length="+bitposn.length);

	int[] high = { 0,
		1,
		2, 2,
		4, 4, 4, 4,
		8, 8, 8, 8, 8, 8, 8, 8,
		16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
		32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32
	};

	int[][] bp = { {},
		{0},
		{1}, {0,1},
		{2}, {0,2}, {1,2}, {0,1,2},
		{3}, {0,3}, {1,3}, {0,1,3}, {2,3}, {0,2,3}, {1,2,3}, {0,1,2,3},
		{4}, {0,4}, {1,4}, {0,1,4}, {2,4}, {0,2,4}, {1,2,4}, {0,1,2,4}, {3,4}, {0,3,4}, {1,3,4}, {0,1,3,4}, {2,3,4}, {0,2,3,4}, {1,2,3,4}, {0,1,2,3,4},
		{5}, {0,5}, {1,5}, {0,1,5}, {2,5}, {0,2,5}, {1,2,5}, {0,1,2,5}, {3,5}, {0,3,5}, {1,3,5}, {0,1,3,5}, {2,3,5}, {0,2,3,5}, {1,2,3,5}, {0,1,2,3,5},
		 {4,5}, {0,4,5}, {1,4,5}, {0,1,4,5}, {2,4,5}, {0,2,4,5}, {1,2,4,5}, {0,1,2,4,5}, {3,4,5}, {0,3,4,5}, {1,3,4,5}, {0,1,3,4,5}, {2,3,4,5}, {0,2,3,4,5}, {1,2,3,4,5}, {0,1,2,3,4,5},
	};

	// verify computation of computed tables with old tables
	for (int i=0,imax=Math.min(high.length, bitvalhigh.length); i<imax; i++) {
		//System.out.print(i+" ");
		assert high[i] == bitvalhigh[i]: "high bit "+high[i]+" vs "+bitvalhigh[i];

		int[] b0=bp[i], b1=bitposn[i];
		assert b0.length==b1.length: "size mismatch for "+i+": "+b0.length+" vs "+b1.length;
		for (int j=0,jmax=b0.length; j<jmax; j++) assert /*Array.equals(b0,b1)*/b0[j]==b1[j]: "1 array @ "+j+" for "+i+": "+b0[j]+" vs "+b1[j];
	}
  }}
}
