package multivalent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import multivalent.std.adaptor.XML;



/**
	Simple tree node for use in building parse tree:
	attributes, children, write linearized tree to string.
	Like java.awt.Rectangle, all fields public.
	Can't be directly used in document tree.

<!--
	<p>Obsolete by INode?  General enough?
	+ can learn one set of tree manipulation methods, develop one set
	- extra state
	- need to save that state for rebuilds (maybe)
	- possibly corrupted state BUT + want to be able to mutate state
-->

	@version $Revision: 1.7 $ $Date: 2003/06/02 05:06:01 $
 */
public class ESISNode {
	//static final boolean DEBUG = true;
	//public static Map EMPTYATTRS = new CHashMap(1);
	static String nl_ = System.getProperties().getProperty("line.separator");

	String gi_;	// tag names are case sensitive


	public Map<String,Object> attrs = null;	// name-value pairs
	List<Object> children_ = null;  // replace by firstsibling, rightsibling pointers?
	/*ESISNode parent;*/
	public boolean empty=false;
	//public boolean open=true;    // open or close tag?
	public char ptype='X';    // '/'=close, '!', '?', ...


	//public ESISNode(ESISNode t) { this(t.getGI(), t.getAttributes()); }	// or clone()?
	public ESISNode(String gi, Map<String,Object> attr, ESISNode parent) {
		setGI(gi);
		attrs = attr;
		if (parent!=null) parent.appendChild(this);
	}
	public ESISNode(String gi, Map<String,Object> attr) { this(gi, attr, null); }
	public ESISNode(String gi) { this(gi, null); }


	/* *************************
	 * ACCESSORS
	 **************************/

	public String getGI() { return gi_; }
	public void setGI(String gi) { gi_ = gi.toLowerCase(); }

	public int size() { return children_==null? 0 : children_.size(); }
	//  public List<> getChildren() { return (List)(children_==null? null: ((ArrayList)children_).clone()); }
	public void appendChild(Object n) {	// can add String or ESISNode
		if (children_==null) children_ = new ArrayList<Object>(10); // need post hoc system measurements
		//assert n!=null: "can't add null child to "+gi_;
		//System.out.println(gi+": add "+n);
		children_.add(n);
	}
	public void removeChildAt(int posn) {
		assert children_!=null && posn < children_.size();
		children_.remove(posn);
	}
	// maybe make everything an ESISNode so can get rid of Objects
	public Object childAt(int n) {
		assert children_!=null && n>=0 && n<children_.size();
		return children_.get(n);
	}

	// SGML stores attribute values as Strings,
	// but application may replace this with actual object, e.g., URL attr => URL class
	public ESISNode putAttr(String key, Object value) {
		if (attrs==null) attrs = new CHashMap<Object>(5);
		// check for duplicate attribute names.  in production--when things should just work--just overwrite
		//assert attr.get(key.toLowerCase())==null: "duplicate keys in "+gi+": "+key;
		attrs.put(key, value);
		return this;    // chaining: putAttr(a,b).putAttr(c,d)...
	}

	public String getAttr(String key) {
		if (attrs==null) return null;
		else return (String)attrs.get(key);
	}

	public void removeAttr(String key) {
		if (attrs!=null) attrs.remove(key);
	}
	//public Map getAttributes() { return (CHashMap)(attrs==null? null : attrs.clone()); }
	//public void setAttrs(Map attr) { attrs=attr; }


	// iterator that skips {all, empty} content nodes
	public static final int TRIM_ALLCONTENT=1, TRIM_NULLCONTENT=2;

	public static void trimTree(ESISNode tree) { trimTree(tree, ESISNode.TRIM_NULLCONTENT); }
	public static void trimTree(ESISNode tree, int flags) {
		assert flags>=ESISNode.TRIM_ALLCONTENT && flags<=ESISNode.TRIM_NULLCONTENT;

		for (int i=tree.size()-1; i>=0; i--) {
			Object child = tree.childAt(i);
			if (child==null && (flags&ESISNode.TRIM_NULLCONTENT)!=0
					|| !(child instanceof ESISNode) && (flags&ESISNode.TRIM_ALLCONTENT)!=0)
				tree.children_.remove(i);
		}
	}


	/**
	Writes ESIS tree as XML.
	 */
	public String writeXML() {
		StringBuffer sb = new StringBuffer(8 * 1024);
		writeXML(sb, 0);
		return sb.substring(0);
	}

	/** Appends linearized tree to existing StringBuffer starting with given indentation level. */
	String writeXML(StringBuffer sb, int level) {
		XML.write(getGI(), attrs, sb,level);
		/* => moved to XML, where it belongs
	sb.append("<").append(getGI()); //o.print(" Behavior="+instance.getClass().getName()+source of class on network);

	int cnt=0;
	if (attrs!=null) {	// when interating through behaviors, have at least "Behavior" attribute, but for general purpose...
		// special attributes go first
		for (int i=0,imax=specialattr.length; i<imax; i++) {	// can do this in general as don't guarantee an order
			String attrname = specialattrlc[i], attrval = getAttr(attrname);
			if (attrval!=null) {
				sb.append("  ").append(specialattr[i]);     // keep case
				if (!attrval.equals(attrname)) { sb.append('='); encode(sb, attrval, true); }
			}
		}
		if (cnt>0) { sb.append(nl_); cnt=0; }

		// other attributes
		for (Iterator<Map.Entry<String,Object>> i=attrs.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<String,Object> e = i.next();
			String attrname = e.getKey();

			boolean special=false;	// put specials into Hashtable?
			for (int j=0; j<specialattr.length; j++) if (specialattrlc[j].equals(attrname)) { special=true; break; }
			if (special) continue;

			if (cnt>0 && cnt%3==0) { sb.append(nl_); indent(sb,level); }    // 0,3,6,9,..., so three on every line

			sb.append("  ").append(attrname);

			String attrval = (String)e.getValue();	// always non-null -- can't Hashtable.put("name", null);
			if (!attrval.equals(attrname)) { sb.append('='); encode(sb, attrval, true); }
			cnt++;
		}
	}
		 */

		if (size()==0)
			sb.append(" />").append(ESISNode.nl_);
		else {    // recurse to children
			sb.append(">").append(ESISNode.nl_);
			for (int i=0,imax=size(); i<imax; i++) {
				Object o = childAt(i);
				if (o instanceof ESISNode) ((ESISNode)o).writeXML(sb, level+1);
				else XML.encode(o.toString(), sb, false);  // content
			}
			XML.indent(level,sb); sb.append("</").append(getGI()).append(">").append(ESISNode.nl_);
		}

		//if (cnt>0) o.println();
		return null;
	}

	//void decode(StringBuffer sb, String o) { => done by XML

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(gi_.length() + (attrs!=null? attrs.size(): 0) * 10 * 2);
		sb.append(gi_);

		if (attrs!=null)
			for (Entry<String, Object> e : attrs.entrySet())
				sb.append("  ").append(e.getKey()).append("=\"").append(e.getValue()).append('"');

		return sb.toString();
	}


	public void dump() {
		dump("");
	}

	public void dump(String indent) {
		System.out.println(indent+gi_);
		for (int i=0,imax=size(); i<imax; i++) {
			Object n = childAt(i);
			assert n!=null: "child #"+i+" of "+gi_+" is null";
			if (n instanceof ESISNode)
				((ESISNode)n).dump(indent+"  ");
			else
				System.out.println(indent+"  "+(String)n);
		}
	}
}
