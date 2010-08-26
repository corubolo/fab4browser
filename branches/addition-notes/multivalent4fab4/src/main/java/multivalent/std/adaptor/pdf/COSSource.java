package multivalent.std.adaptor.pdf;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import phelps.util.Version;

import static multivalent.std.adaptor.pdf.COS.*;



/**
	Abstract superclass for classes that can produce PDF COS objects.

	<ul>
	<li>Object access: {@link #getObject(Object)}, {@link #getObjCnt()} {@link #getTrailer()}
	<li>Algorithms: {@link #connected(Object)}
	<li>Object inspection: {@link #getDecodeParms(Dict, String)}, 
	<li>Versioning: {@link #getVersion()}}
	</ul>

	@version $Revision: 1.3 $ $Date: 2004/05/09 07:04:26 $
*/
public abstract class COSSource {
  // OBJECT ACCESS

  public abstract Object getObject(Object ref) throws IOException;
  public abstract int getObjCnt();
  public abstract Dict getCatalog() throws IOException;
  public abstract Dict getTrailer();
  /** Convenience method for <code>((Number)getObject(<var>ref</var>)).intValue()</code>. */
  public int getObjInt(Object ref) throws IOException { return ((Number)getObject(ref)).intValue(); }
  /** Convenience method for <code>(Dict)getObject(<var>ref</var>))</code>. 
  public Dict getDict(Object ref) throws IOException { return (Dict)getObject(ref); }*/
  /** Convenience method for <code>((Number)getObject(<var>ref</var>)).intValue()</code>. 
  public double getReal(Object ref) throws IOException { return ((Number)getObject(ref)).intValue(); }*/


  // OBJECT CREATION
  // getColorSpace, getImage, getFont, ...


  // ENCRYPTION



  // ALGORITHMS

  /**
	Returns list of {@link IRef}s to objects reachable from object <var>obj</var> connected by nested data structures or indirect references.
  */
  public List<IRef> connected(Object obj) throws IOException {
	List<IRef> l = new ArrayList<IRef>(20);
	boolean[] seen = new boolean[getObjCnt()];	// boolean[] so fast even if connected(getTrailer()), so can use for fault()

	connected(obj, l, seen);	// top object may or may not be IRef
	for (int i=0; i<l.size(); i++) {	// i<l.size() since list grows
//System.out.println(l.get(i)+" = "+getObject(l.get(i)));
		connected(getObject(l.get(i)), l, seen);
	}

	return l;
  }

  private void connected(Object o, List<IRef> l, boolean[] seen) throws IOException {
	if (o==null) return;

	Class cl = o.getClass();
	if (CLASS_IREF==cl) {
		IRef iref = (IRef)o; int id = iref.id;
		if (0 < id&&id < seen.length && !seen[id]) { l.add(iref); seen[id]=true; }	// only IRefs in list, which may or may not include initiating object

	} else if (CLASS_ARRAY==cl) {
//System.out.println(Arrays.asList((Object[])o));
		for (Object oa: (Object[])o) connected(oa, l, seen);

	} else if (CLASS_DICTIONARY==cl) {
		Dict dict = (Dict)o;
//System.out.println(dict);
		for (Iterator<Object> i = dict.values().iterator(); i.hasNext(); ) connected(i.next(), l, seen);
	}
  }



  // VERSIONING
  /** Returns the major version of PDF used; for example, for PDF 1.4. */
  public abstract Version getVersion();


  /** Returns DecodeParms associated with <var>filter</var>; or if no filter by that name returns null. */
  public Object getDecodeParms(Dict stream, String filter) throws IOException {
	assert stream!=null && filter!=null;

	Object dp = null;
	Object fo = getObject(stream.get("Filter")), dpo = getObject(stream.get("DecodeParms"));
	if (fo==null) {
	} else if (CLASS_NAME==fo.getClass()) {
		if (fo.equals(filter)) dp = dpo!=null? dpo: OBJECT_NULL;
	} else { assert CLASS_ARRAY==fo.getClass();
		Object[] foa = (Object[])fo;
		for (int i=0,imax=foa.length; i<imax; i++) {
			if (filter.equals(foa[i])) {
				dp = dpo!=null? getObject(((Object[])dpo)[i]): OBJECT_NULL;
				break;
			}
		}	
	}
	return dp;
  }
}
