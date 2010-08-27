package multivalent.std;

import java.awt.Color;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Map;

import multivalent.*;
import multivalent.node.LeafText;
import multivalent.std.span.BoxSpan;
import multivalent.gui.VEntry;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;

import phelps.lang.Booleans;



/**
	Find words in tree, highlight matches within document.

	<p>TO DO:
	update to use regexp,
	maybe separate UI from functionality

	@version $Revision: 1.3 $ $Date: 2003/06/02 05:57:32 $
*/
public class Search extends Behavior {
  static final boolean DEBUG = true;

  /**
	Another semantic command, which should be given more descriptive name.
	<p><tt>"searchFor"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>expression-to-search-for</var>.
  */
  public static final String MSG_SEARCHFOR = "searchFor";

  /**
	Show previous hit.
	<p><tt>"searchPrev"</tt>.
  */
  public static final String MSG_PREV = "searchPrev";

  /**
	Show next hit.
	<p><tt>"searchNext"</tt>.
  */
  public static final String MSG_NEXT = "searchNext";

  /**
	Add/remove controls in second toolbar: on/off.
	<p><tt>"searchSetActive"</tt>: <tt>arg=</tt> {@link java.lang.String} or {@link java.lang.Boolean} or <code>null</code> to toggle.
  */
  public static final String MSG_SET_ACTIVE = "searchSetActive";

  /**
	Announces search hits, by group.
	<p><tt>"searchHits"</tt>: <tt>arg=</tt> {@link java.util.List}[] <var>hits, grouped</var>, <tt>in=</tt> {@link multivalent.Node} <var>root node of search</var>
  */
  public static final String MSG_HITS = "searchHits";

  /** Name of span for style sheet. */
  static String SEARCHHITTAG = "searchhit";


  /** Coordinate group colors among visualizations. */
  static Color[] colors = { Color.RED, Color.MAGENTA, Color.GREEN, Color.BLUE, Color.YELLOW };

  static List[] LIST0 = new List[0];

  boolean active_ = false;
  //boolean nb_ = true; => externalized to NotemarkUI
  String searchFor_ = null;
  // collect matches.  called with Search button or automatically in incremental searches
  String lastsearch="";
  List[] hits = LIST0;	// 0-length, not null
  List<SearchHit> allhits = new ArrayList<SearchHit>(100);

  VEntry entry = new VEntry("TEXT",null, null);
  // => options to move to hub+AttrUI
  //VCheckbox winc = (VCheckbox)createUI("checkbox", "Inc", null, null, null, false);
  VCheckbox wcase = (VCheckbox)createUI("checkbox", "Case", null/*"event "+MSG_SEARCH*/, null, null, false);
  VCheckbox wwhole = (VCheckbox)createUI("checkbox", "Word", null/*"event "+MSG_SEARCH*/, null, null, false);
  // "clear"?



  /** Part of Edit menu */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_EDIT==msg) {
		INode menu = (INode)se.getOut();
		VCheckbox cb = (VCheckbox)createUI("checkbox", "Search", "event "+MSG_SET_ACTIVE, menu, "Search", false);
		cb.setState(active_);

	} else if (Document.MSG_FORMATTED==msg && active_) {
System.out.println("MSG_FORMATTED on "+se.getArg());
		//search(searchFor_, di.doc);
	}

	return false;
  }


  /** When active, takes over toolbar. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg=se.getArg();

	Browser br = getBrowser();
	Document doc = br.getCurDocument();

	if (MSG_SET_ACTIVE==msg) {
		active_ = Booleans.parseBoolean(se.getArg(), !active_);
		br.setCurDocument(doc);   // to reset toolbar
		//return true;	// stop adding to toolbar from old round robin

	//} else if ("toggleSearchNB"==msg || Document.MSG_CURRENT==msg) {
		// update search hit y coords, send new event (rest handled by NotemarkUI)

	} else if (Browser.MSG_CREATE_TOOLBAR2==msg && active_) {
		INode menu = (INode)se.getOut();
		//menu.removeAllChildren();	// replace toolbar with search widgets! => own toolbar now

		// LATER: send new event: createWidget/SearchTOOLBAR
		createUI("button", "Search", "event "+MSG_SEARCHFOR, menu, null, false);
		//VEntry entry = new VEntry("TEXT",null, menu); -- keep search words available throught class
		//entry.setSizeChars(30, 1);
		menu.appendChild(entry);
		entry.bbox.setSize(50,20);
		entry.putAttr("script", "event "+MSG_SEARCHFOR);
		if (searchFor_!=null) entry.setContent(searchFor_);
		//menu.appendChild(winc);
		menu.appendChild(wcase);
		menu.appendChild(wwhole);
		createUI("button", "<img src='systemresource:/sys/images/Up16.gif'>", "event "+MSG_PREV, menu, null, false);
		createUI("button", "<img src='systemresource:/sys/images/Down16.gif'>", "event "+MSG_NEXT, menu, null, false);
		//createUI("button", "Close", "event toggleSearch", menu, null, false); // => menu only?

		//return true;	// I win!


	} else if (allhits.size()>0 && (MSG_NEXT==msg || MSG_PREV==msg)) {
		// inefficient -- see if need to make faster

		// find all, now show next
		int y=doc.getVsb().getValue(), h=doc.bbox.height, bot=y+h;
System.out.println("current window = "+y+".."+bot);
		SearchHit showme = null;

		// find first one shown
		if (MSG_PREV==msg) {
			for (Iterator<SearchHit> i=allhits.iterator(); i.hasNext(); ) {
				SearchHit hit = i.next();
				//Point pt = hit.getRelLocation(doc);
				if (hit.y < y) showme=hit;
			}
			if (showme==null) showme=allhits.get(allhits.size()-1);
		} else {
			for (Iterator<SearchHit> i=allhits.iterator(); i.hasNext(); ) {
				SearchHit hit = i.next();
				//Point pt = hit.getRelLocation(doc);
//System.out.println("\t"+hit.getName()+" @ "+y);
				if (hit.y > bot) { showme=hit; break; }
			}
			if (showme==null) showme=allhits.get(0);
		}
System.out.println(" => "+showme);
		showme.span.getStart().leaf.scrollTo();


	// Accept requests from other behaviors (whether active or not)
	// Later, accept span name too (what about setting colors according to group?).
	} else if (MSG_SEARCHFOR==msg) {
		String sf = null;
		if (arg==null) {	// take from widget
			searchFor_ = entry.getContent();
			sf = entry.getContent();
		} else if (arg instanceof String) { // can be from outside, in which case take
			//searchFor_ = (String)arg;
			//entry.setContent(searchFor_);
			// take from arg, but don't touch
			sf = (String)arg;
		}
//System.out.println("semanticEventAfter "+msg);
		search(sf, doc);
	}

	return super.semanticEventAfter(se,msg);
  }


/*  public ESISNode save() {
	// write out group
  }*/

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	searchFor_ = getAttr("searchFor");	// take from prefs?
	//if (searchFor_!=null) ...
  }


/*
  public boolean formatAfter(Node node) {
	// do this during format rather than build so have good coordinates for y positions
	/*if active* /search(searchFor_, node.getDocument());	// inner classes will make this clean
	return false;
  }*/


  /** Update last search results with small tweak. */
  void deltasearch(String searchfor) {
	// if incremental search on then fast path for difference on last character only
	int oldlen=lastsearch.length(), newlen=searchfor.length();
	int dir = newlen-oldlen;
	char lastold=' ', lastnew=' ';
	// Windows|Macintosh

	// nothing new: ignore
	if ((dir==1 && (lastnew=searchfor.charAt(newlen-1))==' ' || lastnew=='|')
		|| (dir==-1 && (lastold=lastsearch.charAt(oldlen-1))==' ' || lastold=='|')
		|| (dir==0 && lastsearch.equals(searchfor))) {
		return;
	}
	// change to big for delta
	if (/*dir<-1 || dir>1*/ dir!=1	// if deleted one character, may be new matches
		|| (dir==1 && !searchfor.startsWith(lastsearch))
		|| oldlen==0 || (dir==1 && (lastold=lastsearch.charAt(oldlen-1))==' ' || lastold=='|')
		|| (dir==-1 && !lastsearch.startsWith(searchfor))) {
		search(searchfor, getBrowser().getCurDocument()); return;
	}

	// otherwise, interrogate matches for last word to see if still match
	List<SearchHit> lasthits = (List<SearchHit>)hits[hits.length-1];
	String lastmatch = lastsearch.substring(1+Math.max(lastsearch.lastIndexOf(' '), lastsearch.lastIndexOf('|')));
	String newmatch = searchfor.substring(1+Math.max(searchfor.lastIndexOf(' '), searchfor.lastIndexOf('|')));
	for (int i=0,imax=lasthits.size(); i<imax; i++) {
		SearchHit oldhit = lasthits.get(i);
		Span oldspan = oldhit.span;
		Mark oldstart = oldspan.getStart();
		String txt = oldstart.leaf.getName();
		if (newmatch.equals("") || (txt.startsWith(lastmatch) && !txt.startsWith(newmatch))) {
			oldspan.destroy();
			lasthits.remove(i); i--; imax--;
		} else { Mark oldend=oldspan.getEnd(); oldspan.move(oldstart.leaf,oldstart.offset, oldend.leaf,oldend.offset+dir); }
	}

	lastsearch = searchfor;
  }


  public/*use semanticEvent*/ void search(String searchfor, INode root) {
	Browser br = getBrowser();

//System.out.println("entering search()");
	// clear old groups => BE INCREMENTAL
	for (int i=0,imax=hits.length; i<imax; i++) {//hits[i].clear();
//System.out.println("i="+i+", imax="+imax);
		for (int j=0,jmax=hits[i].size(); j<jmax; j++) {
			((SearchHit)hits[i].get(j)).span.destroy();
		}
	}
	hits=LIST0;

	// iterate over groups
	//if (searchfor==null) { hits=LIST0; return; }
	if (searchfor==null) return;	// should send event searchHits, repaint(), ...

	//br.eventq(Executive.MSG_SUMMARY, "ON");	// and if no such behavior, happily ignored
	br.format();  // coordinates up to date for search hits

	String delimiter = (searchfor.indexOf('|')!=-1? "|": "\t\n\r ");
	StringTokenizer st = new StringTokenizer(searchfor, delimiter);
	hits = new List[st.countTokens()];
	allhits.clear();

	for (int i=0,imax=hits.length; i<imax; i++) {
		String group = st.nextToken().intern();
//System.out.println(" group="+group+", i="+i);
/*
		Object hit = null;
		if (current!=null && (hit = current.get(group))!=null) {
			// old hits still good... assuming tree hasn't changed
		} else {
*/
			StringTokenizer gst = new StringTokenizer(group);
			String[] searchlist = new String[gst.countTokens()];
			for (int j=0; gst.hasMoreTokens(); j++) searchlist[j] = gst.nextToken();
			searchSubtree(i, searchlist, (List<SearchHit>)(hits[i]=new ArrayList<SearchHit>(10)), root);
			//System.out.println("for "+searchlist[0]+"..., found "+hits[i].getSize()+" hits");
//		}
//System.out.println(" all hits += "+hits[i].size()+", group="+group+", i="+i);
		for (int j=0,jmax=hits[i].size(); j<jmax; j++) allhits.add((SearchHit)hits[i].get(j));
	}
//System.out.println("\tdone");

	// could do spanLeaves and iterate rather than do a tree walk, but more efficient this way
	// (which could be noticable for incremental searches)
	// later, just scan text buffer and determine what nodes correspond to matches
	//System.out.println("search repaint");

	lastsearch = searchfor;

	INode docroot = br.getDocRoot();
	docroot.markDirtySubtree(true); // update all frames
	docroot.repaint();

	SemanticEvent se = new SemanticEvent(br, MSG_HITS, hits, root, null);
	br.eventq(se);
  }


  // search over leaves, store up list of nodes that match
  // search left to right over leaves
  // just walk leaves with .getNextLeaf()?
  void searchSubtree(int group, String[] searchlist, List<SearchHit> hits, INode root) {
	Layer scratchlayer = root.getDocument().getLayer(Layer.SCRATCH);

//root.dump();
//System.out.println("entering searchSubtree(), root="+root+", firstleaf="+(root!=null?root.getFirstLeaf():null));
	boolean fwhole=wwhole.getState(), fcasesen=wcase.getState();
	for (Node n=root.getFirstLeaf(),endn=root.getLastLeaf().getNextLeaf(); n!=endn; n=n.getNextLeaf()) {
		if (n instanceof LeafText) {
			// check for match
			String txt = ((LeafText)n).getText(); if (txt==null) continue;
			// want approximate match for OCR
			for (int i=0,imax=searchlist.length; i<imax; i++) {
				String sf=searchlist[i]; int sflen=sf.length();
				boolean match=false;
				if (fwhole) {
					match = (txt.length()==sflen) && (fcasesen? txt.equals(sf): txt.equalsIgnoreCase(sf));
				} else {
					match = (fcasesen? txt.startsWith(sf): txt.regionMatches(true, 0, sf,0,sflen));
				}

				if (match) {
//System.out.println("adding BoxSpan");
					BoxSpan newspan = (BoxSpan)Behavior.getInstance(SEARCHHITTAG, "multivalent.std.span.BoxSpan", null, scratchlayer);	//layer_);
					newspan.setColor(colors[group < colors.length? group: colors.length-1]);   // newspan.setGroup(group);
					//newspan.moveq(n,0, n,Math.max(searchlist[i].length(),n.size()));
					newspan.moveq((Leaf)n,0, (Leaf)n,sflen);
//					newspan.makeNotemark("SEARCHNB");
					Point pt = n.getRelLocation(root);  // let client compute this and just send around Spans
					hits.add(new SearchHit(newspan, pt.x, pt.y));
				}
			}
		}
	}
  }
}
