package multivalent.std.adaptor.pdf;



/**
	PDF dictionary, which is different from PostScript dictionary.
	Keys are always a PDF /name (Java java.lang.String), and value can be any PDF COS object.

	@version $Revision: 1.2 $ $Date: 2003/08/29 03:58:23 $
*/
public class Dict extends java.util.HashMap</*String*/Object,Object> {
  public Dict() { super(); }
  public Dict(int initialCapacity) { super(initialCapacity); }
  //public Dict(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
  public Dict(Dict m) { super(m); }

  // in PostScript, any object can be a key, but strings are canonicalized to names
  // in PDF, all keys are names
  public Object put(Object key, Object value) { return super.put(key, value); }
  public Object get(String key) { return super.get(key); }
  public Object remove(String key) { return super.remove(key); }
  public boolean containsKey(String key) { return super.containsKey(key); }
}
