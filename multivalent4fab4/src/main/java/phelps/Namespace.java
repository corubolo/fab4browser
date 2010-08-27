package phelps;

import java.util.HashMap;
import java.util.Map;



/**
	NOT IMPLEMENTED.
	A nestable Hashtable.
	Namespace names are case sensitive and should be named with mixed case (upper and lowercase)
	to prevent collisions with keys at the same level, which are normalized to all uppercase.
	Keys must be <tt>String</tt>s, but values can be any <tt>Object</tt> type.

	Separate namespaces and keys with ".",
		as in Bib.AUTHOR,
		or Http.Headers.LAST-MODIFIED

	<p>Namespaces are automatically created as something is put into them.
	For instance, within an empty global namespace, the statement
		<tt>put("Http.Headers.Last-Modified", 929387423)</tt>
	will create a namespace named "Http", and within that a namespace named "Headers" and finally
	within that a key named "LAST-MODIFIED" with the value 929387423.

	<p>A <tt>Browser</tt> uses <tt>Namespace</tt>s in both its (persistent) attributes and its (non-persistent) globals.

	@see multivalent.Browser

	@version $Revision$ $Date$
 */
public class Namespace /*extends Behavior*/ {
	Map<String,Object> hash = new HashMap<String,Object>();

	public Object get(String key) {
		int sep = key.indexOf('.');
		if (sep==-1) return hash.get(key);
		else {
			Namespace ns = (Namespace)hash.get(key.substring(0,sep));
			if (ns==null) return null;
			else return ns.get(key.substring(sep+1).toLowerCase());
		}
	}

	public Object put(String key, Object value) {
		int sep = key.indexOf('.');
		if (sep==-1) return hash.put(key.toLowerCase(), value);
		else {
			Namespace ns = (Namespace)hash.get(key.substring(0,sep));
			if (ns==null) return null;
			else return ns.get(key.substring(sep+1));
		}
	}

	public boolean remove(String key) {
		int sep = key.indexOf('.');
		if (sep==-1) return remove(key.toLowerCase());
		else {
			Namespace ns = (Namespace)hash.get(key.substring(0,sep));
			if (ns==null) return false;
			else return ns.remove(key.substring(sep+1));
		}
	}
	public boolean containsKey(String key) {
		int sep = key.indexOf('.');
		if (sep==-1) return containsKey(key.toLowerCase());
		else {
			Namespace ns = (Namespace)hash.get(key.substring(0,sep));
			if (ns==null) return false;
			else return ns.containsKey(key.substring(sep+1));
		}
	}
}
