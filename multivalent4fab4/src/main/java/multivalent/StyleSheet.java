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

import java.awt.Graphics2D;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import phelps.io.Readers;

import com.pt.io.InputUni;



/**
	Map structural name pattern into ContextListener to add to Context's list of active behaviors.
	This is the superclass for all StyleSheets.
	It implements a simple style sheet that just uses VObject name and VObject handle
	as selectors -- no class, id, context, no nested selectors, no textual form (everything set programmatically).

<!--
	<p>LATER: handle formatting too, based on flow regions.
-->

	@see multivalent.std.adaptor.CSS

	@version $Revision: 1.3 $ $Date: 2008/09/08 09:15:29 $
 */
public class StyleSheet /*extends Behavior?*/implements Cloneable {
	public static final boolean DEBUG = false;

	//public static final int PRIORITY_ELEMENT=10, PRIORITY_CLASS=PRIORITY_ELEMENT*10, PRIORITY_ID=PRIORITY_CLASS*10;
	public static final int
	// base
	PRIORITY_INLINE = ContextListener.PRIORITY_SPAN - ContextListener.LOT * 2,  // stylesheet lower than externally applied
	PRIORITY_BLOCK = ContextListener.PRIORITY_STRUCT - ContextListener.LOT * 2,

	// deltas
	PRIORITY_ID = ContextListener.LOT,
	PRIORITY_CLASS = ContextListener.SOME,
	PRIORITY_ELEMENT = ContextListener.LITTLE
	;

	protected static final Object TERMINAL = new Object();	// singleton

	protected Context cx_ = null;

	protected String name_ = null;
	/** Link to lower priority style sheet. */
	protected StyleSheet cascade_ = null;
	public/*protected--Debug*/ /*CHash*/Map<Object,ContextListener> key2cl_ = new HashMap<Object,ContextListener>(50);
	//protected Map<> name2be = new HashMap(50);



	public StyleSheet() {
	}

	/**
	LATER
	Cache last lookup, so faster if have add immediately followed by remove on same args.
	Invalidated by put.
  protected String lastvojb_=null, lastcx_=null;
  protected ContextListener[] lastcl_ = null;
	 */
	public void setName(String name) { name_=name; }
	public String getName() { return name_; }

	public StyleSheet getCascade() { return cascade_; }
	public StyleSheet getCascade(String name) {
		for (StyleSheet ss = this; ss!=null; ss=ss.getCascade())
			if (name.equals(ss.getName())) return ss;
		return null;
	}

	public void setCascade(StyleSheet parent) { cascade_ = parent; }

	public void setCascade(Document doc) {
		if (doc!=null) {
			cascade_ = doc.getStyleSheet();
			doc.setStyleSheet(this);
		}
	}

	public StyleSheet copy() {
		StyleSheet copy = null;
		try { copy = (StyleSheet)clone(); } catch (CloneNotSupportedException canthappen) {}
		return copy;
	}

	public int size() { return key2cl_.size(); }




	/**
	Return Context matched to this StyleSheet's ContextListeners.
	This context inherits from prevailing: Graphics2D, base_, signals.
	 */
	public Context getContext(Graphics2D g, Context enclosing) {
		if (cx_==null) cx_ = createContext();

		cx_.styleSheet = this;
		//g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);	// default on for splash on OS X
		cx_.g = g;


		if (enclosing!=null) {
			cx_.signal = enclosing.signal;
			cx_.base_ = enclosing.base_;
		}

		return cx_;
	}

	// I guess this is safe as have to use full getContext() once to createContext(), else get null
	public Context getContext() { return cx_; }


	/**
	Create new Context matched to this StyleSheet's ContextListeners.
	StyleSheet subclasses should override.
	 */
	protected Context createContext() {
		return new Context();
	}

	// createGeneralContextListener?



	/**
	Add all relevant ContextListeners to Context.
	After invoking parent cascade, this simple implementation tries VObject's name and handle as selectors.
	Called directly by Leaf, indirectly by INode through paintBefore.
	 */
	public void activesAdd(List<ContextListener> actives, VObject o, Node parent) {
		if (o==null) return;	// after cascade_....?  shouldn't happen so shouldn't matter
		if (cascade_!=null) cascade_.activesAdd(actives, o, parent);

		ContextListener cl;

		String name = o.getName();
		if (name!=null && (cl = get(name, parent)) !=null) Context.priorityInsert(cl, actives); //cx.addq(cl);  // by GI

		if ((cl = get(o)) !=null) Context.priorityInsert(cl, actives);  // by handle
	}


	public void activesRemove(List<ContextListener> actives, VObject o, Node parent) {
		if (o==null) return;
		if (cascade_!=null) cascade_.activesRemove(actives, o, parent);

		ContextListener cl;

		String name = o.getName();
		if (name!=null && (cl = get(name, parent)) !=null) actives.remove(cl);  // by GI

		if ((cl = get(o)) !=null) actives.remove(cl);  // by handle
	}


	/** Low level: hash on object itself. */
	public void put(Object key, ContextListener cl) {
		//System.out.println("put "+key+" => "+cl);
		key2cl_.put(key, cl);
	}

	/** More sophisticated style sheet implementation can parse selector. */
	public void put(String selector, ContextListener cl) {
		key2cl_.put(selector.toLowerCase(), cl);
	}

	/*  public void put(Object key, List<> context, ContextListener cl) {
	put(key, cl);
  }*/



	/** Low level: hash on object itself. */
	public ContextListener get(Object key) {
		return key2cl_.get(key);
	}

	/*  public ContextListener get(String selector, List<> context) {
	return key2cl_.get(key);
  }*/

	/*  public ContextListener get(VObject o, Node context) {
	return key2cl_.get(o.getName());
  }*/

	public ContextListener get(Object key, Node context) {
		return key2cl_.get(key);
	}

	public ContextListener remove(Object key) {
		return key2cl_.remove(key);
	}

	public ContextListener remove(String selector) {
		return key2cl_.remove(selector.toLowerCase());
	}


	// methods to construct/parse
	/** Parse external style sheet. */
	public void parse(URL url) {
		try {

			String s = null;
			if (url.toString().startsWith("jar:http:")){
				Reader r = new InputStreamReader(url.openStream());
				s = Readers.toString(r);
			} else {
				InputUni iu = InputUni.getInstance(url, null, Multivalent.getInstance().getCache());	// systemresource protocol
				if (iu!=null)
					s = Readers.toString(iu.getReader());
			}
			parse(s, url);
			if (StyleSheet.DEBUG) System.out.println("StyleSheet URL = "+url+", class="+getClass().getName());
		} catch (IOException e) {
			System.out.println(e);
		}
	}


	/**
	Parse internal style sheet.
	No definition in this simple style sheet -- have to set programmatically.
	 */
	public void parse(String csstxt, URL base) {}   // not abstract because want to instantiate this class

	/**
	Parse attribute name-value pair into settings in CLGeneral.
	 */
	public void setAttr(CLGeneral gs, String name, String value) {}

	/**
	Parse attribute name-value pair into settings in CLGeneral.
	 */
	public void setAttrs(CLGeneral gs, String pairs) {
		//System.out.println("multivalentStyleSheet.setAttrs -- simple stylesheet has no textual representation and so can't parse");
	}

	/** Compute priority of passed selector. */
	public void setPriority(String selector, CLGeneral cl) {
		cl.setPriority(ContextListener.PRIORITY_STRUCT);
	}

	/*
  // can match on nested nodes
  public void put(String name, String context1, String context2, String context3, ContextListener cx) {
	List<> v = new ArrayList<>(3); v.add(context1); v.add(context2); v.add(context3); put(name, v, cx);
  }
  public void put(String name, String context1, String context2, ContextListener cx) {
	List<> v = new ArrayList<>(2); v.add(context1); v.add(context2); put(name, v, cx);
  }
	 */
	/** Context can match on nested nodes by separating with spaces, e.g., "OL OL LI"
  public void put(String name, String context, ContextListener cx) {
	List<> v = new ArrayList<>(1); v.add(context); put(name, v, cx);
  }*/

	/** Associate with styles.
  public void put(String name, ContextListener cx) {	// special case
	name = name.toLowerCase();
	Object tail = key2cl_.get(name);
	if (tail instanceof Map) ((Map)tail).put(TERMINAL, cx); else key2cl_.put(name, cx);
  } */
	/*
  public void put(String name, List<> context, ContextListener cx) {
	name = name.toLowerCase();
	for (int i=0,imax=context.size(); i<imax; i++) context.set(i, ((String)context.get(i)).toLowerCase());	 // canonicalize

	Map<Object,ContextListener> hash = key2cl_;
	Object key;
	for (int i=0,imax=context.size()-1-1; i<imax; i++) {	// all but last one
		key = context.get(i);
		Object now = hash.get(key);
		if (now==null) { hash.put(key, hash=new HashMap(10)); }
		else if (now instanceof ContextListener) {	// split
			hash.remove(key);
			Map<> split = new HashMap<>(10);
			hash.put(key, split);
			split.put(TERMINAL, now);
			split.put(key, cx);
		} else hash = (Map)now;
	}
	// finally add passed ContextListener
	key = context.get(context.size()-1);
	Object tail = hash.get(key);
	if (tail instanceof Map) ((Map)tail).put(TERMINAL, cx); else hash.put(key, cx);
  }

  // LATER unify with put so if put(name, context, null), removes in Map (maybe just subclass Map to do this)
  public void remove(String name) {
	key2cl_.remove(name);
  }
	 */
	// later: able to get for List<> of Strings
	/*
	@param parent for contextual selectors (parent node for node, start leaf for span).
  public ContextListener get(VObject o, Node parent) { // not INode--context for Span is Leaf?
//System.out.println("lookup in ss "+name);
	String name = o.getName();
	if (name==null) return null;
	String cname = name.toLowerCase();
	Object val = key2cl_.get(cname);	 // normalize to uc?
	if (val==null && cascade!=null) return cascade.get(o, parent);
	if (val instanceof Map) val = ((Map)val).get(TERMINAL);
	return (ContextListener)val;
  }

  public void put(Object key, ContextListener cx) { key2cl_.put(key, cx); }
	 */

	/**
	Get by String name or Object hash.
  public ContextListener get(Object o) {
	return key2cl_.get(o);
  }
	 */

	/** Preferred to get(String) because style sheet wants to look at context in tree, attributes and so on */
	/* don't do nested for now.  want get(Node) to return both node-specific and best match in style sheet
  public ContextListener get(Node n) {
	Object lastmatch = null;

	for (INode p = (n.isStruct()? (INode)n : n.getParentNode()); p!=null; p=p.getParentNode()) {	// names in stylesheet in uc, relying on names in tree to be uc too
		String name = p.getName(); if (name==null) continue;   // skip nonstructural
		name = name.toLowerCase();
		Object cl = key2cl_.get(name);
		if (cl==null) break;

		// end of chain: TERMINAL lookup if chain can go on, singleton ContextListener if only one
		if (cl instanceof ContextListener) { lastmatch=cl; break; }
		Object probe = ((Map)cl).get(TERMINAL);
		if (probe!=null) lastmatch = probe;
	}
	return (ContextListener)lastmatch;
//	  return key2cl_.get(name.toLowerCase());
  }
	 */

	/** Arbitrary behaviors can be associated with styles. */
	/*
  public void addBehavior(String tag, Behavior b) {
	String canonical = tag.toLowerCase();
	List<> v;
	if ((v=(List)name2be.get(canonical))==null) {
		v = new ArrayList<>(2);
		name2be.put(canonical, v);
	}
	v.add(b);
  }
  public List<> getBehavior(String tag) {
	String canonical = tag.toLowerCase();
	return (List)name2be.get(canonical);	  // null or a ArrayList
  }*/
	//	public removeBehavior(String tag, Behavior b) {} // see if need this one in practice

	public static int eatSpace(String str, int inx) {
		assert inx >= 0: inx;
		int len = str.length();
		while (inx < len && Character.isWhitespace(str.charAt(inx))) inx++;
		return inx;
	}
}
