package multivalent;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import multivalent.node.LeafShadow;

import com.pt.awt.NFont;



/**
	Graphics context object passed from node to node.
	Holds graphics attributes, list of prevailing ContextListeners, random "signals" (name-value pairs),

<!--
	Augments state in Graphics2D object, which has
		The Component to draw on,
		affine transformation (translation, scaling, rotation),
		clipping region,
		color,
		font,
		logical pixel operation function (XOR or Paint),
		XOR alternation color (see setXORMode).

	Context adds
-->
<!--
	<p>To do
	Add signals to achieve parity among span, context, lens.
	Make into Behavior and subsume getActivesAt, so can update for different document types.
	Add affine transformation, so can can build up--just as possible on Graphics2D?
	Add scale, generalized coordinates (for GIS, et cetera).
-->
	@version $Revision: 1.14 $ $Date: 2004/04/30 19:05:37 $
 */
public class Context {
	/** Use to set primitive-based properties to invalid. */
	public static final int INT_INVALID = Integer.MIN_VALUE;
	public static final float FLOAT_INVALID = Float.MIN_VALUE;
	public static final float[] FLOATARRAY_INVALID = new float[0];

	/** Use to set Object-based properties to invalid, as <tt>null</tt> is valid for some properties. */
	public static final Object OBJECT_INVALID = new Object();
	public static final String STRING_INVALID = new String();

	/** Special color.  Use <code>==</code>, not <code>equals()</code>. */
	public static final Color COLOR_INHERIT = new Color(0,0,0, 0x10/*random value so .equals(BLACK) false*/);	// "just happens to be black"
	/** Special color.  Use <code>==</code>, not <code>equals()</code>. */
	public static final Color COLOR_INVALID = new Color(0,0,0, 0x11);

	public static final byte BOOL_FALSE=0, BOOL_TRUE=1, BOOL_INHERIT=2, BOOL_INVALID=3;

	public static final BasicStroke STROKE_DEFAULT = new BasicStroke(1f);
	// same with gradient and texture?

	private static final AffineTransform TRANSFORM_IDENTITY = new AffineTransform();
	private static final int DEPTH_MAX = 100;	// max depth of document tree (not extensible for now)


	// PROPERTIES
	/**
	Valid means all property settings are current for current Node in tree.
	Needs to be current on all Nodes because leaves need font, color, ... and INodes need colors to draw background and border.
	When ContextListners added to or removed from active set, the current property settings are recomputed automatically.
	Mark valid if skip nodes during tree walk (that have non-empty summary list)
	and recompute with reset(Node, offset) before use if valid==false.
	 */
	public boolean valid = false;

	// change to managed array so don't have to cast all the time add or remove one
	/*protected*/public List<ContextListener> vactive_ = new ArrayList<ContextListener>(30);	// keep list of active tags, for performance -- public so Debug class can reach in
	//protected List<> vactivenode_ = new ArrayList<>(vactive_.size());
	//*protected*/public List<> vactive_ = new LinkedList<>();	// keep list of active tags, for performance -- public so Debug class can reach in
	// much faster as typed array, and always adjusting actives so context settings are up to date

	public Graphics2D g = null;

	List<ContextListener> base_ = new ArrayList<ContextListener>(10);

	public StyleSheet styleSheet = null;

	// STANDARD ATTRIBUTES that every leaf should understand
	// if extend attributes, update CLGeneral too!

	/** or pagewise? */
	public float zoom = 1f;
	/** current point a la PostScript (OUT). */
	public float x, y;
	public int baseline;
	/** {sub,super}scripts, opening gaps for spliced in text. */
	public float xdelta, ydelta;
	/** Doesn't seem very useful, though does affect images. */
	public Color xor;
	/** Foreground/fill color. */
	public Color foreground;
	public Color background;
	public Color strokeColor;

	//** Special case: background color of a large area, which should be set upon entering area and the previous value restored upon exiting. */
	public Color pagebackground;// = COLOR_INVALID;	// INodes can do this internally, but Leaf nodes need to know parent's color

	public float linewidth;
	public int linecap;
	public int linejoin;
	public float miterlimit;
	public float[] dasharray;
	public float dashphase;

	// gradient
	// texture

	/** Font properties set independently of one another. */
	public String family;
	/** Size in pixels. */
	public float size;	// need to add relative setting, units
	/** More than just {@link java.awt.Font#ITALIC}, {@link NFont#FLAG_NONE flags}. */
	public int flags;
	/** More than just {@link java.awt.Font#BOLD}, {@link NFont#WEIGHT_THIN weights}. */
	public int weight;
	public AffineTransform textAT;
	/** Text {@link NFont#MODE_FILL fill mode}. */
	public int mode;

	public String texttransform;

	public String display;

	public Color underline, underline2, overline;
	public Color overstrike;
	public boolean elide;	// for Notemarks (blink makes set foreground color to background)
	/** Corresponds to CSS text-align property.  Set to LEFT, RIGHT, CENTER, FILL. */
	public int justify;
	public int spaceabove, spacebelow;	// for use during spans to open up space for annotations

	public byte align, valign;
	public byte floats;

	/** Pending flush at next flowFloats()?  Set to Node.LEFT/RIGHT/BOTH? */
	public byte flush = Node.NONE;

	//public List<Behavior> be_everywhere = new ArrayList<Behavior>(10);	// behaviors active on every node

	/**
	A spot font overrides font properties.
	A spot font can't generally be computed from family, weight, et cetera and then selected from OS fonts,
	because it is dynamically load and thus not available in the OS, or has special treatment such as {@link NFont#setWidths(int[])}.
	For example, fonts embedded in PDF are set as spot fonts.
	 */
	public NFont spot;


	//int leading;
	// tabs
	// initial space
	//public Color lasso;	// for searches
	// ... Tk other text things


	// effective in STRUCTURAL use only, in effect just for current node
	public int marginleft;
	public int marginright;
	public int margintop;
	public int marginbottom;

	public int paddingleft;
	public int paddingright;
	public int paddingtop;
	public int paddingbottom;

	public int borderleft;
	public int borderright;
	public int bordertop;
	public int borderbottom;


	// ODD SIGNALS from lenses: "XDOC" => "OCR", "image"; ...
	// if signal.get(...) != null, signal on, perhaps with value
	public Map<Object,Object> signal = new HashMap<Object,Object>(10);	// KEY NOT NORMALIZED!	Key not necessarily a string!
	// signals different than attributes, such as those used by CSS?
	//public Map attrs = new HashMap(10); -- extend with new fields


	// MANAGEMENT
	/** used as enter/leave flow regions */
	List<FloatContext> fcstack = new ArrayList<FloatContext>();
	FloatContext fc = new FloatContext();

	private NFont font_;

	private BasicStroke stroke_;

	private List[] nodeCLs_ = new List[Context.DEPTH_MAX];
	private int depth_ = 0;



	/** Returns active font, which is the spot font if set, otherwise computed from family, weight, flags, and size. */
	public NFont getFont() {
		NFont f;
		if (spot != null) f = spot;
		else if (font_!=null) f = font_;	// still valid
		else f = font_ = NFont.getInstance(family, weight, flags, size * zoom);
		return f;
	}

	/* maybe, just to bundle attributes
  public void setFont(NFont font) {
	this.font = font;
	family = font.getFamily();
	size = font.getSize();
	weight = font.getWeight();
	flags = font.getFlags();
	textAT = font.getTransform();
  }*/

	public Stroke getStroke() {	// create on demand
		if (stroke_==null) stroke_ = new BasicStroke(linewidth, linecap, linejoin, miterlimit, dasharray, dashphase);
		return stroke_;
	}

	/**
	Usually use <tt>getStyleSheet().getContext()</tt> instead.  (The stylesheet can be obtained from <tt>Document.getStyleSheet()</tt>.)
	Stylesheets use this constructor to create their associated context object.
	<!--
	Fuse StyleSheet with Context?  No, because cascade StyleSheet's and pass single Context through them.
	-->
	 */
	public Context(/*Graphics g, StyleSheet ss -- probably want this too*/) {
		//styleSheet = ss;
		//	hardreset();
		clear();
		valid=false;
	}

	// methods - reset Context, compute Context given point in tree (take tage into consideration),
	// update when tag(s) goes away, update when new tag(s) (with associated priorities) arrive
	/*
  public void hardresetZ() {
	vactive_.clear();
//	  vactive_.add(override);	// always highest priority
	// NO  vactive_ = (List)base__.vactive_.clone();
	//fc.reset();	// I think.
	reset();
//	  if (override!=null)/*startup cirularity* /override.invalidate();
  }*/

	// softreset doesn't reset list of actives
	public void clear() {
		// set defaults
		//zoom = 1f;
		xdelta = ydelta = 0;
		foreground = Color.BLACK;	// lenses to hack colors (darker, inverse) without adding fields here
		//background = Color.WHITE; //null;  //Color.WHITE; -- background a special case whose management is shared by Root
		background = null;
		//pagebackground = Color.WHITE; => not reset

		spot = null;
		font_ = null;	// create on demand
		family = "Times"; size=12f; weight=NFont.WEIGHT_NORMAL; flags=NFont.FLAG_SERIF;
		texttransform = Context.STRING_INVALID;
		//be_everywhere.clear();	// HACK!
		underline = underline2 = overline = Context.COLOR_INVALID;
		overstrike = Context.COLOR_INVALID;
		textAT = Context.TRANSFORM_IDENTITY;
		mode = NFont.MODE_FILL;

		stroke_ = null;	// create on demand
		// defaults as of JDK1.4 are 1f 0=CAP_SQUARE 2=JOIN_MITER 10f null 0f
		linewidth = Context.STROKE_DEFAULT.getLineWidth();
		linecap = Context.STROKE_DEFAULT.getEndCap();
		linejoin = Context.STROKE_DEFAULT.getLineJoin();
		miterlimit = Context.STROKE_DEFAULT.getMiterLimit();
		dasharray = Context.STROKE_DEFAULT.getDashArray();
		dashphase = Context.STROKE_DEFAULT.getDashPhase();

		elide = false;
		justify = Node.LEFT;
		spaceabove = spacebelow = 0;
		xor = null;
		signal.clear();
		//attrs.clear();

		//clearNonInherited();
	}

	public void clearNonInherited() {	// bad name
		marginleft = marginright = margintop = marginbottom = 0;
		borderleft = borderright = bordertop = borderbottom = 0;
		paddingleft = paddingright = paddingtop = paddingbottom = 0;

		//background = COLOR_INVALID;

		align = valign = floats = Node.NONE;
	}

	public void reset() {
		clear();
		//base_.clear() -- NO!
		//for (Iterator<> i=base_.iterator(); i.hasNext(); ) ((ContextListener)i.next()).appearance(this);
		//be_everywhere.clear();

		//	  signal = (Map)base__.signal.clone();
		// NO	vactive_ = (List)base__.vactive_.clone();
		//	 }

		// overrides, from context, spans, lens
		//int lastpri = Integer.MIN_VALUE;
		for (int i=0,imax=vactive_==null?0:vactive_.size(); i<imax; i++)
			vactive_.get(i).appearance(this, false);
		/*ContextListener cl = vactive_.get(i);
int pri = cl.getPriority();
if (pri < lastpri) System.out.println(pri+" < "+lastpri);
lastpri = pri;*/

		/* => create on demand, as only used by LeafShape
	if (linewidth!=stroke.getLineWidth() || linecap!=stroke.getEndCap() || linejoin!=stroke.getLineJoin() || miterlimit!=stroke.getMiterLimit()) {
		stroke = new BasicStroke(linewidth, linecap, linejoin, miterlimit, dasharray, dashphase);
	}
		 */
		//System.out.println("context reset() -- valid");
		valid = true;
	}


	public void addBase(ContextListener cl) {
		if (base_.indexOf(cl)==-1) priorityInsert(cl, base_);
		/*
		// add in priority order
		int pri=cl.getPriority();
		for (int i=0,imax=base_.size(); i<=imax; i++) {
			if (i==imax || pri < ((ContextListener)base_.get(i)).getPriority()) { base_.add(i, cl); break; }
		}
	}*/
		valid = false;
	}

	public void deleteBase(ContextListener cl) {
		base_.remove(cl);
		valid = false;
	}
	public void clearBase() {
		base_.clear();
		valid = false;
	}


	// go back to default and build back up
	public void reset(Node n) { reset(n, -1); }
	public void reset(Mark m) { reset(m.leaf, m.offset); }
	public void reset(Node n, int offset) {
		// integrate base's vactive_
		//for (int i=0, imax=base__.vactive_.size(); i<imax; i++) priorityInsert((ContextListener)base__.vactive_.get(i), vactive_);
		//	  vactive_.add(override);	// always highest priority

		//fc.reset();
		if (n!=null) {
			// collect up all tags up to but not including
			//		vactive_ = n/*ContextListener=>Node*/.getActivesAt(offset, null, false);	// make sure sorted
			//System.out.println("Context base = "+base_);
			//System.out.println("reset @ "+n.getName()+", base="+base_);
			vactive_ = n/*ContextListener=>Node*/.getActivesAt(offset, base_, false);	// make sure sorted
			x=n.bbox.x; baseline=n.baseline;//+n.bbox.y;
		}
		//System.out.println("node reset @ "+n.getName()+", id="+n.getAttr("id"));
		reset();	// reset AFTER collecting actives!
	}


	static class FloatContext {
		/** Pending floats ({@link #queueFloat(Node)}, wating to be made active. */
		List<Node> q;
		/** Active floats, {@link #addFloat(Node)}. */
		Node[] act;
		/** Height to go before have enough to fit float. */
		int[] htg;
		//int[] align;
		int size=0;
		/** Current width occupied by left/right floats. */
		int leftw, rightw;

		int fcnulls=0;	// most times don't have floats (as in most table cells), so be more efficient by just keeping track of push stack nulls

		FloatContext() { super(); reset(); }

		void reset() {
			q = new ArrayList<Node>(10);
			act = new Node[10];
			//align = new int[10];
			htg = new int[10];
			size=0;
			leftw=rightw=0;
		}
	}

	// LATER: generalize float support from HTML specifics
	public int floatStackSize() { return fc.q.size(); }
	public void pushFloat() {
		//System.out.println("pushing float @ "+fcstack.size()+", width="+getFloatWidth(Node.LEFT));
		fcstack.add(fc);
		fc = new FloatContext();
	}

	public void popFloat() {
		fc = fcstack.remove(fcstack.size()-1);
		//System.out.println("popping float @ "+fcstack.size()+", width="+getFloatWidth(Node.LEFT));
	}


	//public void addFloat(Node n, int align) { addFloat(n,align,n.bbox.height); }
	//public void addFloat(Node n, int align, int heighttogo) {
	void addFloat(Node n) { //addFloat(n,n.bbox.height); }
		//protected void addFloat(Node n, int heighttogo) {
		assert n.floats!=Node.LEFT || n.floats!=Node.RIGHT: floats;
		int heighttogo = n.bbox.height;

		// enlarge storage, if necessary
		if (fc.size==fc.act.length) {
			int newlen = fc.act.length + 5;
			Node[] newact=new Node[newlen]; System.arraycopy(fc.act,0, newact,0, fc.act.length); fc.act=newact;
			int[] newhtg = new int[newlen]; System.arraycopy(fc.htg,0, newhtg,0, fc.htg.length); fc.htg=newhtg;
			//int[] newalign=new int[newlen]; System.arraycopy(fc.align,0, newalign,0, fc.align.length); fc.align=newalign;
		}

		// make a shadow, if necessary
		// if (!covered in parent)
		Document doc = n.getDocument();
		INode pabs = doc.getVisualLayer("floats", "multivalent.node.IRootAbs");
		// place right after contents
		if (pabs.childNum()!=1) { pabs.remove(); pabs.getParentNode().insertChildAt(pabs, 1); }
		boolean found=false;
		for (int i=0,imax=pabs.size(); i<imax; i++) {
			Node child = pabs.childAt(i);
			if (child instanceof LeafShadow && ((LeafShadow)child).getShadowed() == n) { found=true; break; }
		}
		if (!found)
			//System.out.println("adding shadow "+n.getName()+"/"+n.getAttr("src")+" @ "+n.bbox);
			/*LeafShadow shadow =*/ new LeafShadow(n.getName(),null, pabs, n);
		//shadow.shadow = n;

		// update active state
		fc.act[fc.size] = n;
		fc.htg[fc.size] = heighttogo;
		//fc.align[fc.size] = align;	// use integer vs boolean left/right to accommodate CENTER in the future
		if (n.floats==Node.LEFT) fc.leftw+=n.bbox.width; else fc.rightw+=n.bbox.width;
		fc.size++;
	}


	/** Encountered float, queue up for next time have chance to position. */
	/* => put queueFloat and flowFloats in ParaBox */
	public void queueFloat(Node n) {
		//System.out.println("queuing "+n.getAttr("src")+", height="+n.bbox.height);
		fc.q.add(n);
		//n.dump();
	}

	/*
			// all ALIGN=(left|right) get stub in ALIGN visual layer's absolute coordinates for painting
			byte align = Node.NONE;
			if (newn!=null) align = newn.align = getAlign(newn.getAttr("align"));
//if (newn!=null) System.out.println(newn.getName()+" => align="+align);
			// should normalize to all caps here
			//if ("LEFT".equalsIgnoreCase(align) || "RIGHT".equalsIgnoreCase(align)) {
			if ((align==Node.LEFT || align==Node.RIGHT) && false) {
System.out.println("creating bridge for "+newn.getName());
				if (absvis==null) absvis = doc.getVisualLayer("FLOAT","multivalent.node.IRootAbs");	//=> after BODY so drawn on top!
				HTMLFLOAT floatn = new HTMLFLOAT(newn, absvis);
				// when n reformats, shadow positions itself at absolute coordinate version
				// (actually we hook onto parent, because parents position children)
				floatbridge.add(p, floatn);
			}*/

	/**
	Position queued floats.
	If the <tt>flush</tt> field is set to Node.LEFT/RIGHT/BOTH, floats on that side will be flushed, and the additional vertical space required returned, which should be accounted for in calling flow region.
	 */
	public void flowFloats(int y, int width) {
		// LATER: check if left floats overlap right floats
		// update active state
		for (int i=0,imax=fc.q.size(); i<imax; i++) {
			Node n = fc.q.get(i);
			//System.out.println("flowing "+i+" "+n.getFirstLeaf()+"  src="+n.getFirstLeaf().getAttr("src"));
			//int align = ("left".equalsIgnoreCase(n.getAttr("align"))? LEFT: RIGHT);	// LATER: push out
			//n.bbox.setLocation((n.align==Node.LEFT? fc.leftw: width-fc.rightw-n.bbox.width), y);
			n.bbox.setLocation((n.floats==Node.LEFT? fc.leftw: width-fc.rightw-n.bbox.width), y);
			//n.getParentNode().bbox.add(n.bbox); -- float in absolute coordinate visual layer
			//System.out.println("flowing "+n.getAttr("src")+" ("+n.bbox.width+"x"+n.bbox.height+")"+", x="+n.bbox.x+", y="+y+", width="+width);
			//System.out.println("flowing "+n.getName()+"="+n.getAttr("src")+" @ "+n.bbox.x+","+n.bbox.y);
			addFloat(n);
		}
		fc.q.clear();
	}


	public int flushFloats(int side) {
		int h = 0;
		if (flush!=Node.NONE) {
			h=getFloatHeightToGo(flush);
			///*if (h>0)*/ System.out.println("flushing side "+flush+", height = "+h);
			eatHeight(h, null,-1);
			flush=Node.NONE;
		}
		return h;
	}


	/** Returns total width of active left floats */
	public int getFloatWidth(int side) {
		//System.out.println("getFloatWidth "+side+", fc.leftw="+fc.leftw);
		if (side==Node.LEFT) return fc.leftw;
		else if (side==Node.RIGHT) return fc.rightw;
		else if (side==Node.BOTH) return fc.leftw+fc.rightw;
		else return -1;
		//return (side==Node.LEFT? fc.leftw: fc.rightw);
	}

	/** Returns max height of left height-to-go's.  Used by BR clear=XXX */
	public int getFloatHeightToGo(int side) {
		int max=0;
		for (int i=0,imax=fc.size; i<imax; i++)
			if (side==Node.BOTH || side==fc.act[i].floats/*align[i]*/) max=Math.max(max,fc.htg[i]);
		//if (max<=5 && side==Node.LEFT) { System.out.println("close @ "+max); }
		return max;
	}

	/* subtract passed int from height-to-go (min h-t-g=0),
	 remove floats as possible (from right, as long as h-t-g==0),
	 if remove set end of associated span to end of previous formatted node */
	public void eatHeight(int h, Node lastn, int lastnoff) {
		//int fill=0;
		//if (fc.size>0) System.out.println("eatHeight "+h+" @ "+lastn);//.getName());
		// update height-to-go for everybody
		for (int i=0,imax=fc.size; i<imax; i++)
			//System.out.print(fc.htg[i]+" => ");
			if (fc.htg[i]>0) fc.htg[i] -= h;
		//System.out.println(fc.htg[i]);

		//if (fc.size>0) System.out.println("fc.size="+fc.size+"=>");
		boolean killleft=true, killright=true;
		for (int i=fc.size-1; i>=0 && (killleft || killright); i--) {
			Node n = fc.act[i];
			int floats = n.floats;//fc.align[i];
			boolean kill = fc.htg[i]<=0;

			if (floats==Node.LEFT) {
				if (kill && killleft) fc.leftw -= n.bbox.width; else killleft=kill=false;
			} else { assert floats==Node.RIGHT: floats;
			if (kill && killright) fc.rightw -= n.bbox.width; else killright=kill=false;
			}

			if (kill /*&& (killleft || killright)*/) {
				//System.out.println("removing "+n.getAttr("src")+"/"+i+", width="+n.bbox.width+", height="+n.bbox.height);

				// if flows outside of structural parent, make parallel float item in virt coord space layer

				System.arraycopy(fc.act,i+1, fc.act,i, fc.size-i-1);
				System.arraycopy(fc.htg,i+1, fc.htg,i, fc.size-i-1);
				//System.arraycopy(fc.align,i+1, fc.align,i, fc.size-i-1);
				// LATER: reset end of associated span: ((Span)n.sticky_.get(0)).move(n,0, lastn,lastn.lastnoff);
				/*			for (int j=i; j<fc.size; j++) {
				fc.act[j]=fc.act[j+1];
				fc.htg[j]=fc.htg[j+1];
				fc.align[j]=fc.align[j+1];
			}*/
				//System.out.println("killing "+n.getFirstLeaf().getAttr("src"));
				fc.size--;
			}
		}
		//if (fc.size>0) System.out.println("fc.size="+fc.size+"=>");
	}

	/**
	Flows all pending floats at end of passed bbox and extends bbox as needed to cover the flushed floats.
	NB: floats still active and flow region, such as an IParaBox, does eatHeight.
	Used by HTML BR clear, HR (who knew), BODY.
	 */
	/*
  public void flushFloats(Node n, int width, int side) {
	if (side==Node.LEFT || side==Node.RIGHT || side==Node.BOTH) {
		Rectangle r = n.bbox;
		cx.flowFloats(h/*wrong* /, width);	// pending floats -- don't know right y until line is set!
		int h = getFloatHeightToGo(side);
		//eatHeight(h);	// duplicates flow region's cx.eatHeight, but that OK since queue should be empty
		bbox.height += h;
	}

		/*
		while ((eat = cx.getFloatHeightToGo(align)) > 0) {
			cx.flowFloats(h/*bbox.height* //*FIX: wrong y* /, width - cx.getFloatWidth(BOTH));
			cx.eatHeight(eat, this,size());
			h += eat;
if (align==Node.BOTH) System.out.println("eating "+eat+", h=>"+h);
		}* /
//System.out.println("h="+h+", to go = "+cx.getFloatHeightToGo(align));
		h = Math.max(h, cx.getFloatHeightToGo(align));

	// subsequent flow will eat again!
	// because BR is a leaf, its parent will count its height twice in cx.eatHeight, but that's usually OK, as when clear=both not a factor and when clear=left exclusive-or clear=right rare

  }*/


	/** Order by priority, low to high so high wins; within equal priority, latest set added last so it wins. */
	public static final int priorityInsert(ContextListener r, List<ContextListener> v) {
		int priority = r.getPriority();
		int insertat = v.size();
		// reject if already have a copy?
		// could do binary search
		while (insertat>0 && priority < v.get(insertat-1).getPriority()) insertat--;
		//if (priority==ContextListener.PRIORITY_LENS) System.out.println("insert "+((Behavior)r).getAttr("title")+" @ "+insertat);
		v.add(insertat, r);
		return insertat;
	}


	// LATER pass Graphics so ContextListener can do specialized drawing at start and end

	/** Add ContextListener to active set, bring Context values up to date. */
	public final void add(ContextListener r) {
		assert r!=null;

		int inx = priorityInsert(r, vactive_);
		if (inx+1==vactive_.size()) r.appearance(this, false);	// fast path: if highest priority, just grok to make context valid
		else reset();
	}

	/** Remove ContextListener to active set, bring Context values up to date. */
	public final void remove(ContextListener r) {
		int inx = vactive_.indexOf(r);
		if (inx!=-1) {
			vactive_.remove(inx);
			reset();
		}
	}

	/**
	Add ContextListener, leave Context in need of reset().
	Good for adding more than one ContextListener before needing a valid Context (which can be obtained by <code>reset()</code>).
	<!--@return false if context is still valid because added ContextListener had highest priority (and we executed appearance()).-->
	 */
	public final void addq(ContextListener r) {
		//int inx =
		priorityInsert(r, vactive_); valid=false;
		//if (inx+1==vactive_.size()) { r.appearance(this, false); return false; }	// fast path: if highest priority, just grok to make context valid
		//return true;
	}

	/** Remove ContextListener, leave Context in need of reset().  Good for removing more than one ContextListener before needing a valid Context. */
	public final void removeq(ContextListener r) {
		//assert vactive_.indexOf(r)!=-1;

		vactive_.remove(r); valid=false;
	}


	/**
	Upon entering node,
	Draw background, border, ...

<!--
	Context gets passed around to all Nodes -- uber-observer

	<p>Used to be done in INode, but
	<ul>
	<li>give subclasses more power to introduce new attributes
	<li>lighten memory use of INode and extend to Leaf
	</ul>
-->
	 */
	public boolean paintBefore(Context cx, INode node) {
		//if (pbgin!=Color.WHITE) System.out.println("INode "+pbgin);
		//System.out.println("node="+getName()+", valid="+isValid()+", # children="+size()+", bbox="+bbox);

		// X can't move this into Context because have to set before other observers too, which get same coordinate space and same Context
		//boolean fstyle=false; //StyleSheet ss=cx.styleSheet;
		List<ContextListener> sact = nodeCLs_[depth_];
		//if (depth_>DEPTH_MAX) { System.out.println("tree depth "+depth_+" > "+DEPTH_MAX); node.getDocument().dump(); System.exit(1); }
		if (sact==null) nodeCLs_[depth_] = sact = new ArrayList<ContextListener>(5);
		else sact.clear();

		Color newbg = Context.COLOR_INVALID;//, pbgin = pagebackground;
		if (node.name_!=null/* && ss!=null*/) {
			styleSheet.activesAdd(sact, node/*this*/, node.getParentNode());

			//System.out.println("paint "+getName()+"/"+getAttr("class")+"/"+getAttr("id")+": def="+defstructcx+", tweak="+structcx);
			if (sact.size() > 0) {
				clearNonInherited();
				background = Context.COLOR_INVALID;
				for (int i=0, imax=sact.size(); i<imax; i++) {
					ContextListener cl = sact.get(i);
					addq(cl);
					cl.appearance(cx, true);	// execute non-inherited ones too
				}

				newbg = background;	// special case: both inherited and non-inherited
				//valid = false;
				reset();	//=> leave non-inherited for subclasses; reset() done in INode
				//fstyle = true;
			}
		}



		Rectangle bbox = node.bbox;
		int x=bbox.x, y=bbox.y, w=bbox.width, h=bbox.height;	// we're in parent's coordinate space


		//if (cx.background!=null && !cx.background.equals(bgin)/*not from span* / && !cx.background.equals(pbgin)) {	// this is not right but usually right so leave for now
		//if ((newbg==null && bgin!=null && pbgin!=null) || (newbg!=null && !newbg.equals(bgin)/*not from span*/ && !newbg.equals(pbgin))) {	// this is not right but usually right so leave for now
		if (newbg!=Context.COLOR_INVALID) {	// it's set in this node.  could be same as prevailing background, in which case do extra work, but should be rare
			//&& (newbg==null && pbgin!=null) || (newbg!=null && pbgin==null) || (pbgin!=null && !pbgin.equals(newbg))
			if (newbg!=null) {
				//Graphics2D g = cx.g;
				// margin already handled: transparent + incorporated in location (x,y)
				// let border overwrite below
				g.setColor(newbg); g.fillRect(x,y, w,h);
				//bgcnt_++;
				//System.out.print(" - bg "+newbg+" "+w+"x"+h);//+" over "+pbgin);
			}
			//*if (newbg!=Color.WHITE)* / System.out.println("new pagebg = "+newbg);
			pagebackground = newbg;	// pick up null (transparent) background
		}


		// border -- used to draw after content, but want node to be able to paint over -- and also Context may be invalid then and don't want to reset() just to pick up border color
		Insets border = node.border;
		if (border != INode.INSETS_ZERO) {
			//System.out.println("drawing border in "+cx.foreground+": "+border.left+" => "+new Rectangle(x,y, border.left,h));
			// also, border-color and border-style
			//cx.reset();
			//Graphics2D g = cx.g;
			//g.setColor(cx.foreground);	// black around black text looks bad
			g.setColor(foreground==Color.BLACK && background!=Color.LIGHT_GRAY? Color.LIGHT_GRAY: foreground);
			// handles both side and width
			if (border.left>0) g.fillRect(x,y, border.left,h);
			if (border.right>0) g.fillRect(x+w-border.right,y, border.right,h);
			if (border.top>0) g.fillRect(x,y, w,border.top);
			if (border.bottom>0) g.fillRect(x,y+h-border.bottom, w,border.bottom);
		}


		//activesAdd(actives, node, parent);
		depth_++;
		return false;
	}


	/**

	 */
	public boolean paintAfter(Context cx, INode node) {
		depth_--;

		List<ContextListener> sact = nodeCLs_[depth_];
		if (sact.size() > 0/*fstyle*/) {
			List<ContextListener> actives = vactive_;	// cx.vactive_ handle may have received brand new list between start of method and here if some child called cx.reset(node,off) which does getActvesAt()
			//ss.activesRemove(actives, this, getParentNode());
			for (int i=sact.size()-1; i>=0; i--) actives.remove(sact.get(i));
			//putList(sact);
			if (valid) reset(); else valid=false;	// if not valid, can't make it so just by removing your cl's and exiting!!!
		}

		return false;
	}


	private float lastx=-1;	// pass between paintBefore and paintAfter
	public void paintBefore(Context cx, Leaf n) {
		// possible to just set color and modes when something changes?
		lastx = cx.x;
		//baseline = n.baseline;
		//if (g==null) return; -- ?
		if (xor==null) g.setPaintMode(); else g.setXORMode(xor);
		/* can't do this paint background here as don't know correct width on partial leaf contents
	g.setColor(cx.background);
	Rectangle bbox = n.bbox;
	g.fillRect(bbox.x,bbox.y, bbox.width,bbox.height);
		 */
		// (bbox not needed here but may be needed in subclasses)
	}

	// paint usual context
	public void paintAfter(Context cx, Leaf n) {
		if (xor!=null) g.setPaintMode();	// break this out into non-per-node piece
		//g.setXORMode(Color.BLACK);
		baseline = n.baseline;

		//if (underlin==COLOR_INVALID) System.out.println("underline INVALID");
		if (underline!=Context.COLOR_INVALID) { g.setStroke(Context.STROKE_DEFAULT); g.setColor(underline==Context.COLOR_INHERIT? foreground: underline); g.drawLine((int)lastx,baseline+1, (int)x,baseline+1);
		//System.out.println("underline="+(underline==COLOR_INVALID? "invalid": underline==COLOR_INHERIT? "inherit": underline.toString())+", ("+lastx+","+(baseline+1)+") .. ("+x+","+(baseline+1)+")");
		}
		if (underline2!=Context.COLOR_INVALID) { g.setStroke(Context.STROKE_DEFAULT); g.setColor(underline2==Context.COLOR_INHERIT? foreground: underline2); g.drawLine((int)lastx,baseline+3, (int)x,baseline+3); }
		if (overline!=Context.COLOR_INVALID) { g.setStroke(Context.STROKE_DEFAULT); g.setColor(overline==Context.COLOR_INHERIT? foreground: overline); g.drawLine((int)lastx,0/*n.bbox.y*/, (int)x,0/*n.bbox.y*/); }
		//	  if (overline!=COLOR_INVALID) { g.setColor(overline); g.drawLine((int)lastx,y, (int)x,y); }
		if (overstrike!=Context.COLOR_INVALID) {
			int ymid = baseline - 4;    //fm.getHeight()/2;
			g.setStroke(Context.STROKE_DEFAULT);
			g.setColor(overstrike==Context.COLOR_INHERIT? foreground: overstrike);
			g.drawLine((int)lastx,ymid, (int)x,ymid);
			//System.out.println("overstrike line "+lastx+","+ymid+" to "+x+","+ymid);
			//System.out.println("baseline="+baseline);
		}
		//	if (lasso!=null) { g.setColor(lasso); g.drawRect(lastx,bbox.y, lastx-1,baseline+1); }	// how to coalese this with an immediately preceding box?
	}


	@Override
	public String toString() {
		//return "zoom="+zoom+", fg="+foreground+", bg="+background+", underline="+underline;
		//return "font="+font_;
		//return "elide = "+elide;
		StringBuffer sb = new StringBuffer(40);
		sb.append(family).append(':').append(size).append(", ").append(Integer.toBinaryString(flags)).append( "@ ").append(weight).append(" / ").append(spot);
		//if (underline!=...
		return sb.toString();
		//return vactive_.toString();
	}
}
