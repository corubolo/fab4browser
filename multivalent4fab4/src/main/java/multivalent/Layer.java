/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import multivalent.std.ui.Multipage;
import phelps.Utility;
import phelps.lang.Booleans;
import phelps.net.URIs;
import uk.ac.liverpool.fab4.Fab4utils;

import com.pt.awt.NFont;
import com.pt.io.Cache;
import com.pt.io.InputUni;



/**
	Special behavior type that groups (1) behaviors (which can be nested Layer's) and (2) data trees of {@link ESISNode}s,
	and dispatches (round robin) protocol events over its collection.
	Every Document has at least one Layer to implement genre and document-specific functionality, and
	the Root's Layer holds all-document/pan-genre system functionality.
	Some behaviors store data in the trees; for instance, multipage documents store pagewise behaviors, and Note stores its content.

	<p>Layers are {@link #restore(ESISNode, Map, Layer) loaded from} and {@link #save() saved to} hubs,
	in which the case a constituant behavior's logical name is the tag or generic identifier (GI) and its attributes the tag's attributes;
	trees are stored as is.  Because of this, behavior logical names are normalized to all lowercase.
	Behaviors are distinguished from data by a BEHAVIOR attribute.

	<p>Dispatches round robin (non-tree) protocols (restore, build, save, semantic events, ...) by recursing over the component behaviors.
	Tree-based protocols (format, paint, low-level events, ...) are handled elsewhere, by the document tree.

	<p>
	<ul>
	<li>Operations on the collection of behaviors: {@link #addBehavior(Behavior)}, {@link #removeBehavior(Behavior)},
		{@link #clearBehaviors()}, {@link #size()},
		{@link #findBehavior(String path)}, {@link #getBehavior(int)}, {@link #getBehavior(String logicalname)}, {@link #getBehavior(String logicalname, String createname)}
	<li>Operations on data trees:
		{@link #getAux(String tag)}, {@link #getAux(String attrname, String attrval)},
		{@link #addAux(ESISNode)}, {@link #removeAux(ESISNode)}, {@link #removeAux(String attrname, String attrval)},
		{@link #auxSize()}
	</ul>


	<p>Works with {@link multivalent.std.ui.Multipage} as follows.
	Stores hub encompassing all pages in data tree keyed by page number.
	On buildBefore, if PAGE attribute on document, clears behaviors and instantiates relevant portion.
	Likewise, on "closedDocumentPage", saves current behaviors to data tree.
	Still loads full on {@link Document#MSG_OPENED} and saves everything to disk on "closedDocument".
	<!--p>If no PAGE attribute set (key assumed to be "FULLDOCUMENT"), assume applies to entire document,
	which could be a non-paginated or paginated.-->

<!--
	<p>TO DO
	<ul>
	<li>colors for annotations
	<li>editor module:
		<pre>
		change URL (< > inline in hub	< > local	< > URL [___________])
		color for annotations:	< > automatic nonconflicting  < > name (should be menu)  < > RBG #[___]
		save, remove, toggle (so infrequent can put in dialog?)
		</pre>
	</ul>
-->

	@see multivalent.std.ui.Multipage

	@version $Revision: 1.4 $ $Date: 2008/09/08 09:15:29 $
 */
public class Layer extends Behavior {
	static final boolean TRACE = false;


	/**
	Load a layer as specified in a hub document, such as genre-specific behaviors or a set of annotations.
	<p><tt>"loadLayer"</tt>: <tt>arg=</tt> {@link java.net.URI} <var>location of hub</var>
	 */
	public static final String MSG_LOAD = "loadLayer";


	/** Layer name for core behaviors, usually created by a document's media adaptor. */
	public static final String SYSTEM = "system";

	/** Layer names are given by String's and usually known by convention, but some are defined, such as PERSONAL for personal annotations. */
	public static final String PERSONAL = "personal";

	/** Standard name for layer that holds behaviors that have no semantic standing in the document. */
	public static final String SCRATCH = "<scratch>";

	/** Standard name for layer that holds media adaptor for primary document. */
	public static final String BASE = "base";

	/** Standard name for layer that holds behaviors shared among other behaviors, although any layer can be shared by asking for the right name. */
	public static final String SHARED = "<shared>";


	public static final String ATTR_ACTIVE = "active";	// make ATTR_ACTIVE available on all behaviors?


	public static final String PREFIX = "sys/hub/";

	private final String FILENAME_SFX = ".hub";


	/** Whole layer of behaviors can be deactivated. */
	boolean active_ = true;

	private final List<Behavior> vbe_ = new ArrayList<Behavior>(20);	// maybe keep as array since constantly iterating over and would save typecasts
	/** Non-behavior subtrees, as from hub.  Pagewise annos stored there, as is Note CONTENT. */
	private final List<ESISNode> vaux_ = new ArrayList<ESISNode>(10);

	/** Behaviors get hook to Layer, which has hook or Layer chain to Document, which chains to Browser. */
	Document doc_ = null;

	// handle nested event calls
	int[] nest_=new int[10];
	int nesti_=0;
	List<Behavior> killq_ = new ArrayList<Behavior>(5);
	boolean boom = false;	// destroy() during a protocol


	private Boolean JAVA_WS = null;


	/** Constructs top-most Layer of Layers in a Document. */
	public static Layer getInstance(String logicalname, Document doc) {
		//assert logicalname!=null; -- OK
		assert doc!=null;

		//System.out.println("create layer "+logicalname+" in "+doc.getName());//+" "+System.identityHashCode(doc));
		Layer l = (Layer)Behavior.getInstance(logicalname,"Layer", null, null/*topmost layer*/);
		l.doc_ = doc;
		return l;
	}

	/**
	Constructs empty layer with given name, unless one by that name already exists, in which case that existing Layer is returned.
	Construction of a new layer nests within this layer.
	New layer is populated from all hubs by that name in all JARs and the uer's home directory; if there are no hubs, the result is an empty layer.

	@see #getBehavior(String)
	 */
	public Layer getInstance(String logicalname) {
		if (logicalname.endsWith(FILENAME_SFX)) logicalname = logicalname.substring(0, logicalname.length() - FILENAME_SFX.length());

		//if (!logicalname.startsWith("<")) System.out.println(logicalname);
		//if ("system".equals(logicalname)) { new Exception().printStackTrace(); System.exit(1); }
		// 1. existing layer by that name?
		String gi = logicalname.toLowerCase();
		//System.out.println(getName()+" new child layer "+logicalname);
		for (int i=0,imax=size(); i<imax; i++) {	// faster than iterator
			Behavior be = getBehavior(i);
			//System.out.println("\t"+be.getName());
			if (gi.equals(be.getName()) && be instanceof Layer) return (Layer)be;
		}

		// 2. no, create new one
		Layer layer = (Layer)Behavior.getInstance(gi,"Layer", null, this);
		if (logicalname.startsWith("<")) return layer;
		String path = Layer.PREFIX + logicalname + FILENAME_SFX;
		//System.out.println("new "+logicalname+" => "+path+" "+getClass().getClassLoader());

		// 3. search for corresponding hubs
		// 3a. from JARs
		Map<URL,URL> seen = new HashMap<URL,URL>(13);	// get dups, I suppose from parent and local?
		try {
			for (Enumeration e = /*Multivalent.getInstance().getJARsClassLoader().*/getClass().getClassLoader().getResources(path); e.hasMoreElements(); ) {
				URL url = (URL)e.nextElement(); if (seen.get(url)!=null) continue; else seen.put(url, url);
				//System.out.println("\t"+url);
				// This is necessary for webstart as the Inputuni is not modifiable and breaks when webstarted
				if (JAVA_WS==null)
					try {
						Class.forName("javax.jnlp.BasicService");
						JAVA_WS = Boolean.TRUE;
					} catch (ClassNotFoundException ec) {
						JAVA_WS = Boolean.FALSE;
					}
					if (JAVA_WS){
						InputStream is = url.openStream();
						File f = Fab4utils.copyToTemp(is,"hub",".hub");
						f.deleteOnExit();
						URI uri = f.toURI();
						mergeHub(layer, uri);
					} else
						mergeHub(layer, url.toURI());
			}
		} catch (/*java.io.IO*/Exception ioe) { System.err.println("can't scan for hubs: "+ioe); }


		// 3b. user shadow hub -- maybe too much policy: push to behavior? recurse?
		InputUni userhub = Multivalent.getInstance().getCache().getInputUni(null, path, Cache.GROUP_PERSONAL);
		//System.out.println("checking user hub @ "+userhub+" => "+userhub.exists());
		if (userhub!=null) {
			mergeHub(layer, userhub.getURI());
			try { userhub.close(); } catch (IOException ioe) {}
		}

		return layer;
	}

	private void mergeHub(Layer layer, URI uri/*filesystem or JAR*/) {
		if (uri!=null) try {
			ESISNode xmln = multivalent.std.adaptor.XML.parseDOM(uri);
			String bename = xmln.getAttr(Behavior.ATTR_BEHAVIOR); if (bename==null) bename="Layer";	// must be Layer or subclass
			Layer l = (Layer)Behavior.getInstance(xmln.getGI()/*xmln.getAttr("name")*/,bename, xmln.attrs, this);
			//System.out.println("hub root attrs = "+xmln.attrs);
			l.restoreChildren(xmln, layer/*merge!?*/, URIs.toURL(uri));
			assert l.size() == 0: l.vbe_;
			l.destroy();

		} catch (Exception e) {
			System.out.println("Error reading layer "+e);
			e.printStackTrace();
		}
	}



	// MOVE OUT of Layer, if only into attributes
	static final NFont FONT_DEFAULT = NFont.getInstance("Times", NFont.WEIGHT_NORMAL, NFont.FLAG_SERIF, 12f);
	Color annoColor_ = Color.RED;//Color.CYAN;
	NFont annoFont_ = Layer.FONT_DEFAULT;
	public Color getAnnoColor() { return annoColor_; }
	public NFont getAnnoFont() { return annoFont_; }
	// END move out of Layer


	/** End of Layer chain. */
	@Override
	public Browser getBrowser() { Document doc=getDocument(); return doc!=null? doc.getBrowser(): null; }

	/** Often end of Layer chain. */
	@Override
	public Document getDocument() { Layer l=getLayer(); return l!=null? l.getDocument(): doc_; }	//if (l!=null//return (doc_!=null? doc_: getLayer().getDocument()); }

	/** For now, a layer is editable if its name is "personal". */
	@Override
	public boolean isEditable() { return Layer.PERSONAL.equals(getName()); }


	/**
	If a Layer is not active, all its component behaviors are still instantiated but not invoked during protocols.
	Layer directly controls build and semanticEvent, and without build there should be no attachments to the document tree for format, paint, ....
	Useful to disable Debugging layer, various annotation layers.
	 */
	public void setActive(boolean active) { active_ = active; }

	public boolean isActive() { return active_; }


	/*
	Behavior management
	 */

	/**
	A Behavior can only be in one layer at a time, so if it's already in a layer, it's removed from that layer first.
	Behaviors added to layer during a protocol are ignored until the protocol (both before and after phases) cpmpletes in this Layer.
	 */
	public void addBehavior(Behavior be) {
		assert be!=null && !vbe_.contains(be);

		if (!vbe_.contains(be)) {
			Layer oldlayer = be.getLayer();
			if (oldlayer!=null) oldlayer.removeBehavior(be);

			vbe_.add(be);	// don't need to put on a queue even if nested, as added behaviors ignored on current protocol traversal
			be.layer_ = this;	// "protected" not very secure as half the classes are behaviors
		}
		if (Layer.TRACE) System.out.println("adding Behavior "+be.getName()+" to Layer "+getName());
	}

	/** Removes behavior from Layer as soon as pending semanticEvent roundrobins completed; if none, then remove immediately. */
	public void removeBehavior(Behavior be) {
		assert be!=null && vbe_.contains(be) && nesti_>=0;

		if (Layer.TRACE) System.out.println("removing Behavior "+be.getName()+" from Layer "+getName());
		if (nesti_==0) vbe_.remove(be);
		else killq_.add(be);	// queue drained in semanticEventAfter
	}

	/** Removes all active behaviors from Layer, destroy()'ing in the process.  Also used in Anno/Wipe. */
	public void clearBehaviors() {
		//System.out.println("clearBehaviors, nesti_="+nesti_);
		for (int i=vbe_.size()-1; i>=0; i--) {
			Behavior be = vbe_.get(i);
			be.destroy();	// removes from layer too
			//vbe_.get(i).destroy();
			assert vbe_.size() == i: "no super.destroy() in "+be.getName();
		}
		//vbe_.clear();	// redundant as Behavior.destory() removes
		assert vbe_.size() == 0;
	}

	/** Returns the numbers behaviors in the layer. */
	public int size() { return vbe_.size(); }

	/** Removes behaviors that were pending (don't allow removeBehavior during roundrobin because could (would probably) change indexes). */
	void drain() {
		if (nesti_>0) return;

		for (int i=0,imax=killq_.size(); i<imax; i++)
			removeBehavior(killq_.get(i));	// add behaviors during roundrobin no problem
		//System.out.print(" "+(killq_.get(i).getName());
		//System.out.println();
		killq_.clear();

		if (boom) destroy();
	}


	/** Given a layer pathname, finds nested layer. */
	public Behavior findBehavior(String path) {
		assert path!=null && path.length()>0;
		if (path==null) return null;

		if (path.startsWith("/")) return getDocument().getLayers().findBehavior(path.substring(1));

		String now=path, next=null;
		int inx=path.indexOf('/');
		if (inx!=-1) { now=path.substring(0,inx); next=path.substring(inx+1); }

		//System.out.println("enter layer "+getName()+" for "+now.toLowerCase());
		if (next!=null) {	// find Layer
			now = now.toLowerCase();
			for (int i=0,imax=size(); i<imax; i++) {
				Behavior be = getBehavior(i);
				//System.out.println("checking layer "+be.getName());
				if (now.equals(be.getName()) && be instanceof Layer) return next==null || next.length()==0? (Layer)be: ((Layer)be).findBehavior(next);
			}
		} else
			for (int i=0,imax=size(); i<imax; i++) {
				Behavior be = getBehavior(i);
				//System.out.println("checking be "+be.getAttr(ATTR_BEHAVIOR));
				if (now.equals(be.getAttr(Behavior.ATTR_BEHAVIOR))) return be;
			}

		return null;
	}


	//public ListIterator<Behavior> behaviorIterator() { return vbe_.listIterator(); }
	public Behavior getBehavior(int index) {
		assert index>=0 && index<size();	// let List<> exception pass through
		return vbe_.get(index);
	}

	/** Get behavior of given logical name, null if doesn't exist. */
	public Behavior getBehavior(String logicalname) {
		//assert /*logicalname!=null--ok/*;

		return getBehavior(logicalname, null);
	}

	/*
	Behaviors needing more sophisticated cooperation can share a manager.
	Managers aren't listed in any hub, but rather are requested by name here, and
	if said manager doesn't exist, it is spontaneously created here.
	Useful for time-based media, lenses, and could have been used for the selection and elsewhere.
	(Could putGlobal on the HashMap if wanted to be more minimal but/and less structured.)
	 */
	/**
	Get (first) behavior of given logical name.  If one doesn't exist, create with passed class name.
	To create a behavior regardless if one by that name already existes, use {@link Behavior#getInstance(String, String, java.util.Map, Layer)}.
	To get all behaviors of given logical name, iterate over the list yourself from 0 .. size()-1 with <code>getBehavior(int)</code>.
	 */
	public Behavior getBehavior(String logicalname, String classname) {
		assert logicalname!=null && classname!=null: logicalname+", "+classname;
		// if (logicalname==null) return null; -- want to go boom

		String oname = logicalname;
		logicalname = logicalname.toLowerCase();	// intern() ?
		if (!oname.equals(logicalname)) System.out.println("getBehavior() uppercase logical: "+oname);	// warn of this inefficiency

		Behavior found = null;
		for (int i=0,imax=size(); i<imax; i++) {	// faster than iterator
			Behavior be = getBehavior(i);
			if (logicalname.equals(be.getName())) { found=be; break; }
		}
		//System.out.println((found!=null?"":"NOT ")+"found "+logicalname);
		if (found==null && classname!=null) found = Behavior.getInstance(logicalname,classname, null, this);
		//System.out.println(logicalname+" => "+found.getName()+", size="+sin+"=>"+vbe_.size());
		return found;
	}



	/*
	Auxiliary-tree management
	 */

	/** Returns tree of aux tree data with passed tag. */
	public ESISNode getAux(String tag) {
		assert tag!=null;

		tag = tag.toLowerCase();
		for (int i=0,imax=vaux_.size(); i<imax; i++) {
			ESISNode pe = vaux_.get(i);
			//System.out.println("\t"+pe.getGI()+"/"+pe.getAttr(MediaAdaptor.PAGE));
			if (tag.equals(pe.getGI())) return pe;
		}
		//System.out.println("not found"); //pe.dump();
		return null;
	}

	/** Returns tree of aux tree data with attribute name = val.  If <code>attrval == null</code>, anything matches. */
	public ESISNode getAux(String attrname, String attrval) {
		assert attrname!=null /*&& attrval!=null--ok*/: attrname;
		//if (attr==null || val==null) return null; -- go boom

		//System.out.println("searching for annos on page "+page);
		for (int i=0,imax=vaux_.size(); i<imax; i++) {
			ESISNode pe = vaux_.get(i);
			//System.out.println("\t"+pe.getGI()+"/"+pe.getAttr(MediaAdaptor.PAGE));
			Object val = pe.getAttr(attrname);
			if (val!=null && (attrval==null || attrval.equals(val))) return pe;
		}
		//System.out.println("not found"); //pe.dump();
		return null;
	}

	/** Add ESISNode tree to aux trees, if not already there (not a multilist). */
	protected void addAux(ESISNode e) {
		assert e!=null && vaux_.indexOf(e)==-1;

		if (vaux_.indexOf(e)==-1) vaux_.add(e);
	}

	protected void removeAux(ESISNode e) {
		assert e!=null && vaux_.indexOf(e)!=-1;

		vaux_.remove(e);
	}

	protected void removeAux(String attrname, String attrval) {
		ESISNode e = getAux(attrname, attrval);
		if (e!=null) removeAux(e);
	}

	/** Number of auxiliary trees (often 0). */
	public int auxSize() { return vaux_.size(); }



	/** Clears everything, behaviors and aux. */
	public void clear() {
		if (nesti_>0) { boom=true; return; }

		//destroy() first?
		clearBehaviors();
		//System.out.println("*** clearing aux");
		vaux_.clear();
		//	pi_=-1;
	}



	/**
	Iterate over auxiliary non-behavior trees, then component Behaviors highest priority to lowest.
	Trees go first as they may have information needed by behaviors to follow.
	If document is multipage, as indicated by a MediaAdapator.PAGE attribute in Document, then stuff aux.
	 */
	@Override
	public ESISNode save() {
		ESISNode e = super.save();
		if (e==null) return null;

		// behaviors
		String page = getDocument().getAttr(Document.ATTR_PAGE);
		if (page != null) {
			//System.out.println(msg+", page="+page);
			//System.out.println(msg+", page="+page+", vaux.size()="+vaux.size()+", doc="+doc);
			removeAux(Document.ATTR_PAGE, page);

			//System.out.println("save page "+page+", # annos = "+e.size());
			ESISNode pagee = new ESISNode(Document.ATTR_PAGE);
			pagee.putAttr(Document.ATTR_PAGE, page);
			for (Behavior behavior : vbe_) {
				ESISNode ce = behavior.save();
				if (ce!=null) pagee.appendChild(ce);
			}

			if (pagee.size()>0)
				vaux_.add(pagee);
			//System.out.println(getName()+" saving page "+page);//\n"+e.writeXML());

		} else
			for (Behavior behavior : vbe_) {
				ESISNode ce = behavior.save();
				if (ce!=null) e.appendChild(ce);
			}

		// auxiliary subtrees
		for (int i=0,imax=vaux_.size(); i<imax; i++) e.appendChild(vaux_.get(i));

		return e.size()>0? e: null;	// empty layer no good?
	}



	@Override
	public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
		super.restore(n,attr,layer);
		//String name = getAttr("name"); if (name!=null) setName(name);	// else taken from GI, which isn't case sensitive
		setActive(Booleans.parseBoolean(getAttr(Layer.ATTR_ACTIVE), true));
		restoreChildren(n, this);
	}


	/** Override definition in Behavior to keep immediate subtrees that aren't behaviors. */
	@Override
	public void restoreChildren(ESISNode n, Layer layer) { restoreChildren(n, layer, null); }
	void restoreChildren(ESISNode n, Layer layer, URL urlin) {
		if (n!=null) for (int i=0; i<n.size()/*can change as we go*/; i++) {
			Object child = n.childAt(i);
			//if (child instanceof ESISNode) System.out.println("child = "+((ESISNode)child).getGI());
			if (child instanceof ESISNode) {
				ESISNode m = (ESISNode)child;
				String bname = m.getAttr(Behavior.ATTR_BEHAVIOR);
				if (m.ptype=='!') {
				} else if (m.ptype=='?') {
					if ("import".equals(m.getGI()))
						//System.out.println("importing "+m.getAttr("href"));
						//getBrowser().loadLayer(new URL(urlin, m.getAttr("href")), layer.getAnnoColor(), false, this);
						layer.getInstance(m.getAttr("href"));
					else if ("delete".equals(m.getGI())) {


						//} else if ("override".equals(m.getGI())) {

					} //else ignore

				} else if (bname!=null)
					/*Behavior be =*/ Behavior.getInstance(m.getGI(),bname, m,m.attrs, layer);
				else
					vaux_.add(m);

				// most behaviors with internal structure call restoreChildren first and then (rest of) self
			}
		}
		//if (vaux_.size()>0) System.out.println(getName()+" vaux.size()="+vaux_.size());
	}



	/*
	 * ROUND ROBIN protocols - just pass on to collected behaviors
	 */

	public void buildBeforeAfter(Document doc) {
		//int nesti = nesti_;
		buildBefore(doc);
		//assert nesti == nesti_;
		buildAfter(doc);
	}


	/**
	Iterates over component Behaviors (Layer is a subclass of Behavior), highest priority to lowest, if layer is active.
	If in multipage document, first clear current behaviors and instantiate those for new page, if any, regardless of active state.
	 */
	@Override
	public void buildBefore(Document doc) {
		String page = doc.getAttr(Document.ATTR_PAGE);
		//System.out.println(getName()+" buildBA "+page+", vaux_.size()="+vaux_.size());
		if (page!=null && vaux_.size()>0) {
			//System.out.println("clear");
			clearBehaviors();	// saved on Multipage.MSG_CLOSEPAGE

			ESISNode pe = getAux(Document.ATTR_PAGE, page);
			if (pe!=null) restoreChildren(pe, this);
			//if (pe!=null) System.out.println(getName()+" restore page "+page);
		}

		//System.out.println("layer "+getName()+" buildBefore");
		// can't add behaviors to same layer as build it -- may want allow (simple to do so), but not now
		if (isActive()) for (int i=0,imax=vbe_.size(); i<imax/*vbe_.size()?*/; i++)
			try {
				//Behavior be = vbe_.get(i);
				//if (false) System.out.println("buildBefore "+be.getName()+" / "+be.getClass().getName()+" / "+be.getLayer().getName());
				vbe_.get(i).buildBefore(doc);
			} catch (Exception e) {
				e.printStackTrace();
				Utility.warning(vbe_.get(i)+" buildBefore(): "+e);	// not Utility.error => always keep on truckin'
			}
	}

	/** Iterates over component Behaviors (Layer is a subclass of Behavior), lowest priority to highest. */
	@Override
	public void buildAfter(Document doc) {
		if (isActive()) for (int i=vbe_.size()-1; i>=0; i--)
			try {
				//System.out.println("Layer.buildAfter on "+vbe_.get(i));
				vbe_.get(i).buildAfter(doc);
			} catch (Exception e) {
				e.printStackTrace();
				Utility.warning(vbe_.get(i)+" buildAfter(): "+e);
			}
	}


	/**
	Iterates over component Behaviors (including nested Layer's), highest priority to lowest.
	On {@link Multipage#MSG_CLOSEPAGE} for isEditable() documents, first saves active behaviors for that page to aux in memory
	(behaviors are moved from aux to active in buildBefore).
	(Low-level eventBefore/After is a tree-based protocol, so it's not seen by Layer.)
	 */
	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		// bump nesti_ at start so nested event/*no q*/(SemantiEvent) have right index...  have to bump before loop so that removeBehavior()'s within loop @ 0 are delayed
		int nc = nesti_;
		nesti_++;	// always, to maintain invariant, even if super short-circuits

		if (super.semanticEventBefore(se,msg)) { nest_[nc]=0; return true; }

		if (Multipage.MSG_CLOSEPAGE==msg && isEditable()) save();	// even if not active  -- isEditable() checks parent layer
		//if (Multipage.MSG_CLOSEPAGE==msg) System.out.println("close, ed="+isEditable());

		int imax = isActive()? vbe_.size(): 0;
		nest_[nc] = imax;	// assume no shortcuits

		boolean shortcircuit = false;
		for (int i=0; i<imax; i++) {	// high priority to low
			if (vbe_.get(i).semanticEventBefore(se,msg)) {
				shortcircuit = true;
				nest_[nc] = i+1;	// invariant: nest_[nc] = last behavior + 1, so in After can always -1
				break;	// we can break here where we can't in After because stopping doesn't leave behind unfinshed business -- the business was never started
			}
			//if (dump) System.out.println("semanticEventBefore, after child call, now @"+pi_);
			if (Layer.TRACE && shortcircuit) System.out.println("Layer sEB short-circuit @ "+vbe_.get(i));
		}
		return shortcircuit;	// shortcircuits in Before cause no problem: jump to After at same point
	}


	/** Iterates over component Behaviors, lowest priority to highest. */
	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		assert nesti_>0;

		int nc = nesti_-1;

		//if (!isActive()) return false; -- don't recognize change in state during current roundrobin

		//System.out.println("<= semanticEventAfter "+getName()+", "+msg+" @ nest_["+nesti_+"]="+nest_[nesti_]+", "+getDocument().getURI());
		boolean shortcircuit = false;
		//if (nest_[nc]-1 >= size()) System.out.println("lost behaviors between semEvBe and Af");
		//System.out.println("after "+(nest_[nc]-1));
		for (int i=Math.min(nest_[nc],size())-1; i>=0; i--)
			//Behavior be = vbe_.get(i);
			//System.out.println("semEvAf "+be.getName()+" / "+be.getClass().getName()+" / "+be.getLayer().getName());
			if (vbe_.get(i).semanticEventAfter(se,msg)) {
				if (Layer.TRACE && shortcircuit) System.out.println("semanticEventAfter SHORT-CIRCUIT "+msg+" @ "+vbe_.get(i));
				shortcircuit=true;
				//se.consume(); -- protected method
				//se=null; -- may be useful yet, but se.getMessage() invalid
				msg=null;	// indicate shortcircuit!
				//break; ... but complete popping out of nestings
			}

		nesti_--;
		drain();	// regardless of shortcircuit...

		return shortcircuit || super.semanticEventAfter(se,msg);
	}


	/** Iterates over component Behaviors (including nested Layer's), highest priority to lowest. */
	@Override
	public void destroy() {
		clear();
		super.destroy();
	}



	@Override
	public boolean checkRep() {
		assert super.checkRep();

		assert nesti_>=0;

		assert vbe_.size()>=0;	// empty OK
		for (int i=0,imax=vbe_.size(); i<imax; i++) {
			Behavior be = vbe_.get(i);
			assert be.getLayer() == this;
			assert be.checkRep();	// other layers or behavior.  Span also checked from Leaf
		}

		// any checks on vaux_?

		return true;
	}



	public void dump() { dump(0); }
	public void dump(int level) {
		for (int i=0; i<level; i++) System.out.print("   ");
		System.out.print(getName()+":");
		for (int i=0,imax=vbe_.size(); i<imax; i++) { Behavior be = vbe_.get(i); if (!(be instanceof Layer)) System.out.print("  "+be.getName()); }
		System.out.println();
		for (int i=0,imax=vbe_.size(); i<imax; i++) { Behavior be = vbe_.get(i); if (be instanceof Layer) ((Layer)be).dump(level+1);	}
	}
}
