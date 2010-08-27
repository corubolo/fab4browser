package edu.berkeley.biblio;

import java.io.*;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.*;

import multivalent.*;
import multivalent.node.IVBox;


// => obsolete.  replaced by clone of whatever existing parent
// later generalize this by passing formatter about tree
// actually, nothing special about this: just a regrouper, which should be a standard class
/*
class IBiblio extends INode {
  IBiblio(String name, Map attr, Behavior owner) {	super(name, attr, owner); }

  // in order to work in both fluid and fixed environments, do whatever the prevailing
  // method is unless you know better

  public boolean formatNode(int width, int height, Context cx) {
	return getParentNode().formatNode(width, height, cx);	// but that operates on parent's children!
  }


  public boolean validate(String phase, List<> errors) {
	boolean valid = super.validate(phase, errors);
	// nothing for now, but this one most needs it
	return valid;
  }
}
*/


/**
	<i>Broken</i>
	Biblio - just build bib entries here, extensible set of concrete selects dynamically loaded

	LATER: update to work with both fixed and fluid layout
		process locations and convert to BIB
		use formatter of parent or one passed in
		then scan whole tree for nodes of type BIB

	@version $Revision$ $Date$
*/
public class Biblio extends Behavior {
  private static final boolean DEBUG = true;

  int active;
  boolean affected=false;
  List<Node> /*of unattached doc tree*/ bibentry;	// holds START, END ESIS Locations; bbox
  static final String attrsBib_[] = {
	"uid", "type", "title", "author", "journal", "month", "year", "volume", "pagestart", "pageend", "comment"
  };



  public Biblio() {
	active = 0;
  }

  /* *************************
   * ACCESSORS
   **************************/

  public List<Node> getBibs() { return (List<Node>)((ArrayList)bibentry).clone(); }
//	public String[] getAttributes() { return attrsBib_; }


  protected static String titles[] = { "OCR" };
/*
  public int countItems() { return 1+titles.length; }
  public String getCategory(int id) { return (id==titles.length? "Affected": "BiblioSelect"); }
  public Object getTitle(int id) { return (id==titles.length? "Biblio regions": "Biblio: "+titles[id]); }
  public int getType(int id) { return (id==titles.length? Valence.CHECKBOX: Valence.RADIOBOX); }
  public String getStatusHelp(int id) {
	if (id==titles.length) return "Show bibliographic regions";
	else return "Return selections in "+titles[id]+" format";
  }*/
  public void command(int id, Object arg) {
	Browser br = getBrowser();
	if (id==titles.length) {
		affected = !affected;
		br.repaint();
	} else {
		active = id;
		br.clipboard();
	}
  }

/* make just another function of behavior
  public void showAffected(Graphics2D g) {
	if (affected) {
		g.setColor(Color.RED);
		for (int j=0,jmax=bibentry.size(); j<jmax; j++) {
			Rectangle bbox = ((Node)bibentry.get(j)).bbox;
			g.drawRect(bbox.x, bbox.y, bbox.width, bbox.height);
		}
	}
  }
*/

/*
  public boolean checkDependencies(List<> bv) {
	// maybe centralize this more -- need to do so for type graph at server
	return true;	// true=ok
  }
*/

/*  public ESISNode save() {
	// write out bibs with recomputed Locations
	return null;
  }*/

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	// if behavior with internal structure, usually restore children first, but not in this case
	//restoreChildren(n);

	//assert n.size()>0: "no bibliographic entries";

	//if ((bibentry=(List)cache.get(n)) == null) {
	  bibentry = makeBibI(n);
	//	cache.put(n, bibentry);
	//}	//else return true;
  }


  List<Node> makeBibI(ESISNode n) {
	// validate and normalize ESIS tree, make unattached doc tree with START, END attributes

	//System.out.println("makeBibI");
	List<Node> bibentry = new ArrayList<Node>(10);
	//System.out.println("trimming "+n);
	ESISNode.trimTree(n, ESISNode.TRIM_ALLCONTENT);	// children are bib entries

	boolean gotstart=false, gotend=false;
	for (int i=0,imax=n.size(); i<imax; i++) {
		ESISNode head = (ESISNode)n.childAt(i);
		ESISNode.trimTree(head, ESISNode.TRIM_ALLCONTENT);	// children are bib entry fields

		//RESTRIEVE THIS: IVBox e = new IVBox("BIB", head.getAttributes(), this);	//-- just clone existing parent
		IVBox e=null;
		e.putAttr("type", head.getGI());
		bibentry.add(e);
		//System.out.println("bib entry #"+i); head.dump();

		// convert to doc tree nodes, but some advantages to keeping as ESIS
		for (int j=0; j<head.size(); j++) {
			ESISNode iattr = (ESISNode)head.childAt(j);
			String gi = iattr.getGI().toLowerCase();	// toUpperCase necessary?
			if (gi.equals("START") || (gi.equals("END"))) {
				e.putAttr(gi, iattr/*iattr.childAt(0) => children are multiple redundant paths*/);
			} else if (gi.equals("PAGES")) {
				String pagestart=null, pageend=null;
				String pages = (String)iattr.childAt(0);
				int split = pages.indexOf(' ');
				if (split>=-1) e.putAttr("pageend", pages.substring(split+1)); else split=pages.length();
				e.putAttr("pagestart", pages.substring(0,split));
			} else {
				String content = (String)iattr.childAt(0);
				// expand Month, Year
				/*
				if (gi.equals("MONTH")) {
				} else if (gi.equals("YEAR")) {
				} else {
				*/
				/*
				IVBox label = new IVBox(gi, null, this);
				label.appendChild(new LeafText(content, null, this));
				e.appendChild(label);
				*/
				e.putAttr(gi, content);
			}
		}
		//if (DEBUG) e.dump();
	}
	//if (!gotstart || !gotend) error("need START and END");

	//assert bibentry.size()>0: "no bibliographic entries!";

	return bibentry;
  }


  /*
   * BUILD - hack the doc tree.  return possibly new root
   */

  List<Leaf> spanlist;
  // hack marks before...
  public void buildBefore(Document doc) {
	spanlist = new ArrayList<Leaf>(bibentry.size() * 2);

	// resolve locations to nodes -- resolve all locations before hacking tree
	for (int i=0,imax=bibentry.size(); i<imax; i++) {
	  //System.out.println("resolving #"+i+" of "+bibentry.size());
	  // make fresh copy: just name, attr, owner
	  IVBox bib = (IVBox)bibentry.get(i);
//	  bib.remove();	// remove from old tree
//	  bib.removeAllChildren();	// zap old children

	  // just make start, end sticky pointers and automatically relocated
	  //System.out.println("\tRobustLocation.attach");
/* update this now!!!
	  Node start = RobustLocation.attach((ESISNode)bib.getAttr("start"), root).node;
	  Node end = RobustLocation.attach((ESISNode)bib.getAttr("end"), root).node;
*/
	  //System.out.println("\tattached");

/*JB
	  if (start!=null && end!=null) {
		spanlist.add(start);
		spanlist.add(end);
	  } else System.err.println("couldn't attach "+bib);
*/
	}
  }


	// ...hack tree after
  public void buildAfter(Document doc) {
/* rewrite now that no more cloning
	// now to the hacking
	for (int i=0,imax=bibentry.size(); i<imax; i++) {
	  //System.out.println("hacking #"+i);
	  INode bib = (INode)bibentry.get(i);
	  // always use fresh copy so don't accumulate children
	  //bib = new INode(bib.name, bib.attr, bib.owner);

	  Leaf start = spanlist.get(i*2);
	  Leaf end = spanlist.get(i*2+1);

	  INode newtree = (INode)start.getParentNode().clone();
//RECOVER	   newtree.putAttr((Map)bib.getAttributes());
	  newtree.setName("BIB");
	  newtree.removeAllChildren();
	  List<Behavior> bibobs = bib.getObservers();
	  for (int j=0,jmax=(bibobs==null?0:bibobs.size()); j<jmax; j++) newtree.addObserver(bibobs.get(j));

	  Node leaf[] = Node.spanLeaves(start,0, end,0);

	  // replace first leaf in span with new BIB structural node
	  // guaranteed to have at least one leaf in space or else couldn't have found attachment
	  Node child0 = leaf[0];
	  INode parent = child0.getParentNode();
	  int childnum = child0.childNum();
	  newtree.appendChild(child0);
	  parent.setChildAt(newtree, childnum);

	  // remaining leaves are removed from old parent, added to BIB
	  for (int j=1; j<leaf.length; j++) {
		leaf[j].remove();
		newtree.appendChild(leaf[j]);	// addChild sets child's parent pointer
	  }
	}*/
  }


  // SGML
  // HTML
  // inline reference
}
