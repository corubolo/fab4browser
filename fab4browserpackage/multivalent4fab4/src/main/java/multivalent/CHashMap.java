package multivalent;

import java.util.HashMap;
import java.util.Map;



/**
	{@link java.util.Map} that canonicalizes keys that are of type String to all lowercase.

<!--
	toLowerCase and expensive operation, actually, unfortunately.
	Should call this "Attributes".

	deprecated by Namespace, which can be nested and saved
	see multivalent.Namespace
-->

	@version $Revision: 1.4 $ $Date: 2003/06/02 04:58:14 $
 */
public class CHashMap<V> extends HashMap<String,V> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Unmodifiable, empty instance. */
	public static final /*CHashMap<Object>*/Map EMPTY = java.util.Collections.unmodifiableMap(new CHashMap<Object>());
	/*
	new CHashMap<Object>() {
	  // don't silently fail
	  public Object put(String key, Object value) { throw new UnsupportedOperationException(); }
	  // { return key; }
	  //public void putAll(Map<String,Object> map) { throw new UnsupportedOperationException(); }
	  // get, remove, clear, containsKey, ... operate on empty
	  };*/


	//public static final String DEFINED = new String();

	// don't inherit constructors...
	public CHashMap(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
	public CHashMap(int initialCapacity) { super(initialCapacity); }
	public CHashMap() { super(); }

	/**
	Conviently populate with name-value pairs given in a String:
	<i>name1</i><tt>=</tt><i>value1</i><tt>;</tt><i>name2</i><tt>=</tt>...
	Values can be quoted, with single or double quotes.
	 */
	public static CHashMap<String> getInstance(String attrs) {
		CHashMap<String> map = new CHashMap<String>();

		int len; char c0;
		if (attrs!=null) for (String nv: attrs.split("\\s*;\\s*")) {
			String name, value;   //=null, value=DEFINED;
			int inx = nv.indexOf('=');
			if (inx!=-1) { name=nv.substring(0,inx).trim(); value=nv.substring(inx+1).trim(); } else name = value = nv;
			len=name.length(); if (len>=2 && ((c0=name.charAt(0))=='"' || c0=='\'') && name.charAt(len-1)==c0) name=name.substring(1,len-1);	// quoted name???
			len=value.length(); if (len>=2 && ((c0=value.charAt(0))=='"' || c0=='\'') && value.charAt(len-1)==c0) value=value.substring(1,len-1);
			//System.out.println("CHashMap:  "+name+"="+value);
			//assert value!=null: "null value for "+name+" in "+attrs;
			map.put(name, value);
		}

		return map;
	}

	@Override
	public V put(String key, V value) { return super.put(key!=null? key.toLowerCase(): null, value); }
	/*public void putAll(Map<String,V> map) {	// compiler troubles, so hope that implemented in terms of put()
	for (Iterator<Map.Entry<String,V>> i=map.entrySet().iterator(); i.hasNext(); ) {
		Map.Entry<String,V> e = i.next();
		put(e.getKey(), e.getValue());
	}
	}*/
	public V get(String key) { return super.get(key!=null? key.toLowerCase(): null); }
	public V remove(String key) { return super.remove(key!=null? key.toLowerCase(): null); }
	public boolean containsKey(String key) { return super.containsKey(key!=null? key.toLowerCase(): null); }
}
