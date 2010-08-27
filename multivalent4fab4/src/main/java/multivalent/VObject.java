package multivalent;

import java.util.Iterator;
import java.util.Map;



/**
	{@link java.lang.Object}s with a name and attributes.  Superclass of {@link Behavior} and {@link Node}.
	Attributes are generally {@link java.lang.String}s,
	but {@link VObject}s that are not to be written to a hub can store any {@link java.lang.Object} as the {@link #getValue(String) value}.
	Most getters optionally take a second value, a default that's returned if the attribute does not exist or cannot be interpreted as the requested type,
	rather than throwing an exception in that case.

	<p>Also validate, statistics.

	@version $Revision: 1.6 $ $Date: 2003/06/02 05:11:39 $
 */
public abstract class VObject extends Object {
	public abstract String getName();
	//public abstract void setName(String name);	// can subclass to enforce lc or whatever

	/** Convenience method to global state in class Multivalent. */
	public /*static--if so just use Multivalent's*/ Multivalent getGlobal() { return Multivalent.getInstance(); }


	/**
	All Behaviors and Nodes have attributes, that is, a set (possibly empty) of key, value pairs.
	Attributes keys must be Strings, and are canonicalized to all lowercase.
	Values must be Strings.
	<!--Values can be any Object type.	Otherwise attributes are like HashMap's. -->
	By default, attributes are kept in a CHashMap, which ignores that case of the key;
	if a {@link java.util.HashMap} is desired, create the object with one.
	 */
	protected Map<String,Object> attr_ = null;

	/** Needed for scripting, testing and moving from ESISNode to real node, but not for general use. */
	public final Map<String,Object> getAttributes() { return attr_; }
	public final void setAttributes(Map<String,Object> attrs) { /*if (attr_==null)?*/ attr_ = attrs; }
	//public final Map<String,Object> getAttributes() { return (attr_!=null? (Map)attr_.clone(): null); }	 // passed to VScript as local attribute source
	//public void putAttr(Map<String,Object> h) { attr_=h/*.clone()*/; }

	/** DOM2. */
	public boolean hasAttributes() { return attr_!=null && attr_.size()>0; }


	/** Return attribute value that might not be of type String. */
	public final Object getValue(String key) { return attr_!=null? attr_.get(key): null; }

	public final String getAttr(String key) { return attr_!=null? (String)attr_.get(key): null; }

	/** Same as {@link #getAttr(String)}, except if no such key, then return <var>default_</var>. */
	public final String getAttr(String key, String default_) {
		String val = getAttr(key);
		return val!=null? val: default_;
	}

	/* => putAttr(key, Utility.xxxx(color))
  public void putAttrColor(String key, Color color) {
	// convert to string...
	String name=Colors.color2Name(color);
	if (name!=null) putAttr(key, name);
  }
	 */

	/** If <var>val</var> == <code>null</code>, remove attribute. */
	public final void putAttr(String key, Object val) {
		if (val!=null) {
			if (attr_==null) attr_ = new CHashMap<Object>(5);
			attr_.put(key, val);
		} else removeAttr(key); // probably bad
	}

	/**
	Returns old value of <var>key</var>.
	 */
	public final Object removeAttr(String key) {
		Object oldval = null;
		if (attr_!=null) {
			oldval = attr_.remove(key);
			if (attr_.size()==0) { clearAttributes();
			System.out.println("nulling "+getName());}
		}
		return oldval;
	}

	public final void clearAttributes() { attr_=null; }       // may have to make HashMap again, but don't remove attributes very often

	public final Iterator<String> attrKeysIterator() { return attr_!=null? attr_.keySet().iterator(): null; }
	public final Iterator<Map.Entry<String,Object>> attrEntrySetIterator() { return attr_!=null? attr_.entrySet().iterator(): null; }



	/**
	Checks "representation invariant" (see <a href='http://ocw.mit.edu/6/6.170/f01/lecture-notes/index.html'>MIT SE</a>),
	and returns true iff object is valid, which should be always.
	<!-- Would like to force subclasses to define, but many classes don't have anything interesting to add over superclass. -->
	 */
	public boolean checkRep() { return true; }


	/** Types of statistics to dump during stats(). */
	//static final String ALL="all";
	/** Statistics of performance measurement, memory usage, and interesting facts. */
	//public abstract void stats(String type);
}
