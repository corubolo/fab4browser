package phelps.doc;

import java.util.StringTokenizer;
import java.util.Map;
import java.awt.Color;

import multivalent.*;       // move into multivalent since requires multivalent.Node's?
//import org.w3c.dom.*;     // no more likely to match user's tree either

import phelps.net.URIs;
import phelps.lang.Integers;



// anonymous inner classes
class FuzzSpan extends Span {   // => named span, with properties set in style sheet
  Color color_;
  FuzzSpan(Node node, int offset, Layer layer, Color color) {
//	super(node,offset, node,offset+(offset<node.size()?1:-1), layer);
//System.out.println("attach to "+offset+".."+(offset+(offset<node.size()?1:-1)));
	color_ = color;
  }
  public boolean appearance(Context cx, boolean all) { cx.background = color_; return false; }
}


/**
	System support for robust locations.
	Kinds of locations: id, absolute position in text (sticky pointers), tree walk, perhaps later query/search.
	See <a href='http://www9.org/w9cdrom/312/312.html'>Robust Locations paper</a>.

<!--
	<p>To do
	<ul>
	<li>fix locations at end of structural unit
	</ul>
-->

	@author T.A. Phelps
	@version $Revision: 1.6 $ $Date: 2003/06/01 08:07:10 $
*/
public class RobustLocation {
  static final boolean DEBUG=false;

  public static final String ATTR_CONTEXT = "context";
  public static final String ATTR_TREE = "tree";
  public static final String ATTR_ID = "id";
  public static final String ATTR_LENGTH = "length";


  static final int PENALTY_SIBLING=1, PENALTY_LEVEL=25;	 // later tweak based on empirical experiments
  static final int PENALTY_THRESHOLD = PENALTY_LEVEL/2 + PENALTY_SIBLING*10;
  static final int DEPTHCREDIT = PENALTY_LEVEL + PENALTY_SIBLING*10;
  static final int SCORE_CONTENT=3, SCORE_CONTEXT=2, PERFECTCC=SCORE_CONTENT+2*SCORE_CONTEXT;


  static class Report {
	public Node node;
	public int fuzz, depth;
	public boolean complete;
	public Report() { reset(); }
/*	  public Report(Node node, int fuzz, int depth) {
		this.node=node; this.fuzz=fuzz; this.depth=depth;
	}*/
	public void reset() { node=null; fuzz=0; depth=0; complete=false; }
	public void reset(Node node, int fuzz, int depth, boolean complete) {
		this.node=node; this.fuzz=fuzz; this.depth=depth; this.complete=complete;
	}
	/*
	boolean updateBetter(Node n, int fuzz, int newdepth, int bfuzz,int bdepth) {
		// update: add self to sub.  if leaf or subtree a loser, set Report to self
		if (fuzz==Integer.MAX_VALUE) reset(n,fuzz,newdepth, false);
		// better
		return (bfuzz==Integer.MAX_VALUE || fuzz-(depth-bdepth)* DEPTHCREDIT<bfuzz);
	}*/
  }


  private RobustLocation() {}

  // caller is responsible for passing right root -- maybe go up to lowest Document
  //public static void descriptorFor(Mark keepme, Map map) { descriptorFor(keepme.node, keepme.offset, map); }
  /**
	Given node, write multiple, redundant location descriptors into passed Map.
	Result can be mapped back to tree by {@link #attach(Map, INode)}.
  */
  public static void descriptorFor(Node keepme, int offset, Node docroot, Map<String,Object> map) {
//System.out.println("map class = "+map.getClass().getName());
	map.put(Behavior.ATTR_BEHAVIOR, "Location");

	// never make descriptorFor root -- root is sentinal and special in other ways too
	INode parent = keepme.getParentNode();
	while (parent!=null && parent.getName()==null) parent=parent.getParentNode();	// bump up to structural
	int childNum = keepme.structChildNum();

	// absolute position, with context -- if want more than one word of context on each side--as for example Patricia tree minimal unique string--have to determine delimiters
	StringBuffer sb = new StringBuffer(250);
	sb.append(URIs.encode(keepme.getName())); // don't want spaces!
	sb.append(' '); // always have delimiters
	// apparently structChildAt is broken
//	if (childNum>0) System.out.println("me="+childNum+", "+parent.structChildAt(childNum-1));
	if (childNum>0	/*BOGUS=>*/&& parent.structChildAt(childNum-1)!=null) sb.append(URIs.encode(parent.structChildAt(childNum-1).getName()));
	sb.append(' ');
//	if (childNum<Integer.MAX_VALUE) System.out.println("cnt="+parent.structsize()+", "+parent.structChildAt(childNum+1));
	if (childNum+1<parent.structsize() /*BOGUS=>*/&& parent.structChildAt(childNum+1)!=null) sb.append(URIs.encode(parent.structChildAt(childNum+1).getName()));
	map.put(ATTR_CONTEXT, sb.substring(0));


	// climb up tree for path, keeping names and position of nodes
	// also include name of node
	sb.setLength(0);
	sb.append(offset).append(' ');
	for (Node n=keepme; n!=docroot; n=n.getParentNode()) {	  // exclude root
		String name = n.getName();
		if (name!=null) sb.append(' ').append(n.structChildNum()).append('/').append(URIs.encode(name));
	}
	map.put(ATTR_TREE, sb.substring(0));


	// absolute position, with context -- tree subsumes and gives better scoping
	/*
	sb.append("<Absolute>");
	sb.append(new Mark(keepme,0).abs());
	sb.append("</Absolute>\n");
	*/
  }



  /** Given Hastable of descriptors, return corresponding node/offset, robust to change
	  Returns null if couldn't attach (may throw exception in the future)
  */
  public static Mark attach(Map<String,Object> attachme, INode toroot) {
if (DEBUG) System.out.println("toroot="+toroot+", attrs="+attachme);
	if (attachme==null || toroot==null) return null;
	String ref;
	INode subroot=null;
	int intra=0;	// set by TREE, if it exists.  this should be a medium-specific token
	Document doc = toroot.getDocument();
	Layer layer = doc.getLayer(Layer.SCRATCH);	//CurrentLayer();
//	boolean showfuzz = (br.getGlobal("STATS")!=null);
	boolean showfuzz=false;


	// id/name
	if ((ref = (String)attachme.get("idref"))!=null) {
if (DEBUG) System.out.println("IDREF = "+ref);
		// easy, but later
		// getGlobal from table of ID=>(node,offset) mappings
	}


	Report best = new Report();

	// tree
	if ((ref = (String)attachme.get(ATTR_TREE))!=null) {
if (DEBUG) System.out.println("TREE = "+ref+", with root="+toroot.getName());

		// collect path, traverse backwards.  fallback strategies for when things change
		StringTokenizer st = new StringTokenizer(ref, " ");
		intra = Integer.parseInt(st.nextToken());
		int len = st.countTokens();

		// construct (typed) arrays so can efficiently pass as parameters
		// may fail early in path and make full construction useless, but usually successful, in which case would do this anyhow
		String[] names = new String[len];
		int[] posns = new int[len];
		for (int i=0; i<len; i++) {
			String desc = st.nextToken();
			int chop = desc.indexOf('/');
			posns[i] = Integer.parseInt(desc.substring(0,chop));
			names[i] = URIs.decode(desc.substring(chop+1));
		}

		attachTree(toroot, names,posns,len-1, 0,0, best);  // offset field==-1 iff best guess, ==0 iff exact, >1 iff fuzzy
		int fuzz = best.fuzz;	// abusing Mark but so what?
		if (fuzz>0) updateStat("TREEROBUST", +1, doc);
		if (showfuzz && fuzz>0) new FuzzSpan(best.node, intra, layer, (fuzz<PENALTY_THRESHOLD? Color.GREEN: Color.YELLOW));
//		  if (fuzz>0) // update statistic for successfully repositioned location.  if fuzz big, have user verify
		if (best.complete) { best.fuzz=intra; return new Mark((Leaf)best.node,best.fuzz); }
		else subroot = (INode)best.node;	 // make farthest match along tree available to content+context to use for scoping
	}
//System.out.println("TREE FAILURE, best="+best.node+"/"+(best.node==null?"":best.node.getFirstLeaf().toString())+" w/fuzz="+best.fuzz+" at depth="+best.depth);


	// content+context
	if ((ref = (String)attachme.get(ATTR_CONTEXT))!=null) {
if (DEBUG) System.out.println("CONTEXT = "+ref);

		if (subroot==null) subroot=toroot;
//System.out.println("trying CONTEXT search starting at "+subroot);
		int fuzz=0;
		int /*len=ref.length(),*/ chop1=ref.indexOf(' '), chop2=ref.lastIndexOf(' ');	  // even if content is empty, have spaces.  this will change when leaves!=words
//System.out.println("for |"+ref+"|, chop1="+chop1+", chop2="+chop2);
//		  String content = ref.substring(0,chop1), pre=ref.substring(chop1+1,chop2), post=ref.substring(chop2+1);
		String content, pre, post;
		if (chop1<chop2 /*&& chop2!=-1*/) { content = ref.substring(0,chop1); pre=ref.substring(chop1+1,chop2); post=ref.substring(chop2+1); }
		else if (chop1==-1) { content = ref; pre=post=""; }
		else /*if (chop1==chop2)*/ { content = ref.substring(0,chop1); pre=post=""; }
		content=URIs.decode(content); pre=URIs.decode(pre); post=URIs.decode(post);

		Mark match = null; //attachContent(subroot, content,pre,post);

		// find best among siblings, if any
		int score,bestscore=0;
		//Node/*Leaf*/ bestmatch=null;
		Mark bestmatch=null;
		//INode root = subroot.getRoot();
		// may want to limit number of levels up we go (missed hits vs false hits tradeoff, as in information retrieval)
		// check for p!=null too as node may not be attached to tree
		// return best match in subtree
		/*if (match==null)*/ for (INode p=subroot,lastp=null,endp=toroot.getParentNode(); /*fuzz<PENALTY_THRESHOLD &&*/ p!=null && p!=endp; fuzz = (fuzz+p.size())*2, lastp=p, p=p.getParentNode()) {
//System.out.println("scanning "+p+"/"+p.getFirstLeaf()+", child cnt="+p.size());
			for (int i=0,imax=p.size(); i<imax; i++) {
				Node n = p.childAt(i);
				if (n==lastp) continue;
				match = attachContent(n, content,pre,post);
				if (match!=null) { score=match.offset; if (score>bestscore) { bestscore=score; bestmatch=match; }; if (score==PERFECTCC) break; }
			}
//if (bestmatch!=null) System.out.println("c+c match on "+bestmatch);
			if ((match=bestmatch)!=null) break;
		}
//System.out.println("context MATCH on "+bestmatch.node+" w/fuzz="+bestmatch.offset);
		if (match!=null) {
			score = match.offset;
			fuzz += PERFECTCC-score;
			if (score==PERFECTCC || fuzz<=PENALTY_THRESHOLD) {	// weed out some but not all false positives
				if (fuzz>0) updateStat("CCROBUST", +1, doc);
				if (showfuzz && fuzz>0) new FuzzSpan(match.leaf, intra, layer, (fuzz<PENALTY_THRESHOLD? Color.ORANGE: Color.RED));
				match.offset=intra; return match;
			} // else drop down to next strategy
		}
	}


	// other strategies?

	return null;	// failure that clients must handle
	// report failure to user too so user can reattach
  }


  // score all matches in range, track best score, if best>threshold got match else try with larger tree
  public static Mark attachContent(Node subroot, String content, String pre, String post) {
	Leaf now=subroot.getFirstLeaf();
//System.out.println("attachContent on "+subroot+", leaves= "+now+".."+subroot.getLastLeaf()+"/"+subroot.getLastLeaf().getNextLeaf());
	if (now==null) return null;
//	  if (((INode)subroot).size()==0) return null;

	int score=-1,best=0;
	Leaf bestleaf=null;

	// +2 for context, +3 for content, threshold>=2 (content or both context)
	Leaf prev=null, /*now=subroot.getFirstLeaf(),*/ next=now.getNextLeaf(), last=subroot.getLastLeaf().getNextLeaf();
	String prevname=null, nowname=(now!=null?now.getName():null), nextname=(next!=null?next.getName():null);
//System.out.println("scanning "+subroot.getName()+": "+nowname+".."+(subroot.getLastLeaf()!=null?subroot.getLastLeaf().getName():""));
	for (; now!=null /*<= I'm not sure how that happens but it does*/&& now!=last; prev=now,now=next,next=(next!=null?now.getNextLeaf():null), prevname=nowname,nowname=nextname,nextname=(next!=null?next.getName():null)) {
//System.out.println(now+" => "+nextname);
		score = 0;
		if (content.equals(nowname)) score+=SCORE_CONTENT;
		if (pre.equals(prevname)) score+=SCORE_CONTEXT;
		if (post.equals(nextname)) score+=SCORE_CONTEXT;

		if (score>best) {
			best=score; bestleaf=now;
			//System.out.println("hit "+now+", score="+score);
			if (score==PERFECTCC) break;
		}
	}

//if (best>=2*SCORE_CONTEXT) { System.out.println("returning hit on "+bestleaf+", score="+best); }
//return new Mark(bestleaf, score);   // both sides of context or better
	if (best>=2*SCORE_CONTEXT) return new Mark(bestleaf, best);	 // both sides of context or better
	return null;	// failure
  }


  /**
	Search tree rooted at <var>root</var> for ID attribute matching <var>id</var> (stored under key <var>idkey</var>, which is usually "id" or "idref").
  */
  public static Node attachId(String id, String idkey, Node root) {
	// first search spans
	for (Leaf l=root.getFirstLeaf(), endl=root.getLastLeaf(); l!=null && l!=endl; l=l.getNextLeaf()) {
		for (int i=0,imax=l.sizeSticky(); i<imax; i++) {
			Mark sticky = l.getSticky(i);
			Object owner = sticky.getOwner();
			if (owner instanceof VObject && id.equals(((VObject)owner).getAttr(idkey))) return l;
		}
	}

	// if not in spans, try on nodes
	return root.findDFS(null, Node.ATTR_ID, id);
  }


  // should maybe keep count of fuzziness and fail if too high
  // returns "best match", balancing fuzziness with depth of match
  public static void attachTree(Node n, String[] names,int[] posns,int ni, int fuzz,int depth, Report sub) {
	if (n.getName()==null && n.isStruct() && ((INode)n).size()==1) n=((INode)n).childAt(0);
/* skip over null-names -- but which child to pick?
	if (n.getName()==null) {
		attachTree();
		return;
	}*/
	if (fuzz>PENALTY_THRESHOLD) return;
//System.out.println("attachTree based at "+n.getName());
//	  if (ni<0 || !(n.isStruct())) return n; // end recursion. (match on leaf but still have path is never going to happen)
//if (fuzz>0 && n.isLeaf() && ni<0) System.out.println("repositioned location with fuzz = "+fuzz);
	//if (ni<0) return bestsofar;  // => make this check before calling attachTree!
	//if (n.isLeaf()) { bestsofar.complete=true; return bestsofar; }
	boolean nisLeaf = (n.isLeaf());
	if (ni<0 || nisLeaf) { sub.reset(n,fuzz,depth, nisLeaf/*true I hope*/);
	//System.out.println("leaf: "+n);
	return; }
	INode p = (INode)n;

	// try to attach at posns[ni], else search siblings in order 0,+1,-1,+2,-2,+3,-3,...
	int ccnt = p.structsize();
	int absposn = Math.min(posns[ni],ccnt);  // translate structural position to runtime position
	//for (int i=0; i<absposn && i<ccnt; i++) if (p.childAt(i).getName()==null) absposn++; -- see if can get away with this for now, use getStructChild later

	// best node seen so far
	Node bestn=null;
	int bestfuzz=Integer.MAX_VALUE, bestdepth=0;

	Node exNode=null;
	String exgi;	// just needed to dump debugging info

	// SIBLINGS
	boolean poststruct=true;
//System.out.println("siblings for "+names[ni]+"@"+posns[ni]+"/"+absposn);
	for (int i=0,j=i/2,side=1,newfuzz=fuzz; /*newfuzz<subbest.fuzz &&*/ (poststruct || absposn-j>=0); i++,j=i/2,side=side*-1, newfuzz+=PENALTY_SIBLING) {  // don't know end of struct nodes, not /*,imax=Math.max(absposn,ccnt-absposn)+1*/ .. /*i<imax*/
if (DEBUG) System.out.println("siblings i="+i+", poststruct="+poststruct+", absposn-i="+(absposn-i));
		exNode=null;
		if (side==1) { if (poststruct) poststruct=(exNode=p.structChildAt(absposn+j))!=null;
		} else { if (i>0 && absposn-j>=0) exNode=p.structChildAt(absposn-j);	// previous context
		}

		if (exNode!=null) {
			exgi = exNode.getName();
if (DEBUG) System.out.println("=> MATCH @ "+exgi);
			if (exgi.equals(names[ni])) {
				attachTree(exNode, names,posns,ni-1, newfuzz,depth+1, sub);
				if (sub.fuzz==Integer.MAX_VALUE) sub.reset(exNode,newfuzz,depth,false);
//System.out.println(/*"side="+side+*/"sibling adding "+exNode);
				if (bestfuzz==Integer.MAX_VALUE || sub.fuzz-(sub.depth-bestdepth)*DEPTHCREDIT<bestfuzz) {
					if (sub.complete) return;	// optional
					else { bestn=sub.node; bestfuzz=sub.fuzz; bestdepth=sub.depth; }
				}
			}
		}
	}


	// retain best node seen so far (which in internal) as fail through to path/hierarchy tricks

	// PATH/HIERARCHY
	// don't want too many of these in a row
	//	 won't every have a sibling then a skip, as sibling has to match on what would be skipped
	poststruct=true;
	//boolean prim=true;	// prim=true because start search at primary *for that subtree*
	// have to match on leaf so ni>0, and can't both be sibling and skip level
	if (/*primary &&*/ ni>0) for (int i=0,j=i/2,side=1,newfuzz=fuzz+/*(getName()!=null?*/ PENALTY_LEVEL; /*newfuzz<subbest.fuzz &&*/ (poststruct || absposn-j>=0); i++,j=i/2,side*=-1,newfuzz+=PENALTY_SIBLING/*,prim=false*/) {
//if (DEBUG) System.out.println("assume new level of hierarchy: "+exNode.getName()+" for "+names[ni]);
//System.out.println("siblings i="+i+", poststruct="+poststruct+", absposn-i="+(absposn-i));
		exNode=null;
		if (side==1) { if (poststruct) poststruct=(exNode=p.structChildAt(absposn+j))!=null;
		} else { if (i>0 && absposn-j>=0) exNode=p.structChildAt(absposn-j);	// previous context
		}

		if (exNode!=null) { for (int k=0; k<=1; k++) {
			// k=0: assume additional level of hierarchy, so skip that node and see if match subtree
			// k=1: assume lost level of hierarchy, so toss out that part of path and see if match remainder
			Node subroot = (k==0?exNode:p);
			int pathtrim = (k==0?0:1);	// just happens to match k

//System.out.println("\tpost cf "+exgi);
			//if (/*exgi.equals(names[ni]) && -- assuming match here*/
			attachTree(subroot, names,posns,ni-pathtrim, newfuzz,depth+1/*not sure should get credit for depth*/, sub);
			//if (sub.fuzz==Integer.MAX_VALUE) sub.reset(exNode,newfuzz,depth,false); <=don't anchor on a skip!   else sub.fuzz+=newfuzz;
			if (sub.fuzz<Integer.MAX_VALUE) {
//System.out.println(/*"side="+side+*/"skip "+(k==0?"hierarchy":"path")+" adding "+exNode);
				if (bestfuzz==Integer.MAX_VALUE || sub.fuzz-(sub.depth-bestdepth)*DEPTHCREDIT<bestfuzz) {
					if (sub.complete) return;
					else { bestn=sub.node; bestfuzz=sub.fuzz; bestdepth=sub.depth; }
				}
			}
		}}
	}

	// no full matches to a leaf (which return out directly), pass up best internal node
	sub.reset(bestn, bestfuzz, bestdepth, false);
  }


  static void updateStat(String statname, int inc, Document doc) {
	if (doc==null) return;

	//Browser br = getDocument().getBrowser()
	int newval = Integers.parseInt(doc.getAttr(statname),0) + inc;
	//String val = br.getAttr(statname);
	//int newval=0;
	//if (val!=null) try { newval=Integer.parseInt(val); } catch (NumberFormatException nfe) {}
	//newval += inc;
	doc.putAttr(statname, Integer.toString(newval));	// or String.valueOf(newval);
  }
}
