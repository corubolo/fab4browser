package multivalent.std.adaptor.pdf;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

import phelps.lang.Integers;
import phelps.lang.Strings;
import phelps.util.Arrayss;

import static multivalent.std.adaptor.pdf.COS.*;



/**
	Manipulate interactive forms: export, import.
	Forms can be extracted from FDF or PDF, modified, and written to a new FDF or PDF.

	<ul>
	<li>The contents of the form can be {@link #export(PDFReader) read}, from a PDF or FDF file,
	or {@link #exportDefaults(PDFReader) read defaults values} from a PDF.
	Keys may already be known, or all keys can be iterated with {@link Map#iterator()}.

	<li>New form values can be set with {@link Map#put(Object,Object)}.  Data can come from FDF or XML or directly from a database or computed or elsewhere.
	Note that new form fields <em>cannot</em> be created simply by adding a new key to the Map.

	<li>A new PDF can be created with {@link PDFWriter}, new or overwrite.
	The contents of the form can {@link #set(Map,PDFWriter) replace} or {@link #merge(Map,PDFWriter) merged} with existing PDF.
	before {@link PDFWriter#writePDF() writing}.

	<li>If want to make form read-only, can {@link #flatten(PDFWriter)}.
	</ul>

	<p>Keys of the Map are fully qualified export field names (concatenated <code>TM</code> dictionary keys),
	Values are form <code>V</code> settings.

	@version $Revision: 1.5 $ $Date: 2004/05/24 00:09:42 $
*/
public class Forms {
  private static String CMD_EXPORT="ex", CMD_DEFAULTS="def", CMD_SET="set", CMD_MERGE="merge";

  private static int FF_READONLY = 1<<0;
  private static int FF_REQUIRED = 1<<1;
  private static int FF_NOEXPORT = 1<<2;
  private static int FF_MULTILINE = 1<<12;
  private static int FF_PASSWORD = 1<<13;
  private static int FF_NOTOGGLETOOFF = 1<<14;
  private static int FF_RADIO = 1<<15;
  private static int FF_PUSHBUTTON = 1<<16;
  private static int FF_COMBO = 1<<17;
  private static int FF_EDIT = 1<<18;
  private static int FF_SORT = 1<<19;
  private static int FF_FILESELECT = 1<<20;
  private static int FF_MULTISELECT = 1<<21;
  private static int FF_DONOTSPELLCHECK = 1<<22;
  private static int FF_DONOTSCROLL = 1<<23;
  private static int FF_COMB = 1<<24;
  private static int FF_RADIOSINUNISON = 1<<25;
  private static int FF_COMMITONSELCHANGE = 1<<26;
  private static int FF_RICHTEXT = 1<<27;
  //private static int FF_ = 1<<;


  private Forms() {}


  public static Map<String,Object> export(PDFReader pdfr) throws IOException {
	return process(CMD_EXPORT, new LinkedHashMap<String,Object>(20), pdfr, null);
  }

  /**
	Returns a {@link java.util.Map} of the FDF's or PDF's interactive form data, 
	or an empty Map if there is no form (there is at most one form, with fields on possibly many pages).
  */
  public static Map<String,Object> exportDefaults(PDFReader pdfr) throws IOException {
	return process(CMD_DEFAULTS, new LinkedHashMap<String,Object>(20), pdfr, null);
  }

  /**
	Set the values of a map as a new FDF/PDF's form.
	If a key is not specified in the passed <var>form</var>, it is set to null.
  */
  public static void set(Map<String,Object> map, PDFWriter pdfw) throws IOException {
	process(CMD_SET, map, pdfw, pdfw);
  }

  /**
	Merge the values of a map into a new FDF/PDF's form.
	If a key is not specified in the passed <var>form</var>, it is set retains its existing value.
  */
  public static void merge(Map<String,Object> map, PDFWriter pdfw) throws IOException {
	process(CMD_MERGE, map, pdfw, pdfw);
  }


  private static Map<String,Object> process(String cmd, Map<String,Object> map,  COSSource cos, PDFWriter pdfw) throws IOException {
	Dict cat = cos.getCatalog();
	Object o = cat.get("FDF"); if (o==null) o = cat.get("AcroForm");
	o = cos.getObject(o);
	if (o!=null && CLASS_DICTIONARY==o.getClass()) {
		Dict af = (Dict)o;
		Object[] fields = (Object[])cos.getObject(af.get("Fields"));
		for (Object f: fields) process(cmd,  cos,pdfw,  f, map,  null, null, 0, null);
	}
	return map;
  }

  private static void process(String cmd,  COSSource cos, PDFWriter pdfw,  final Object ref,  Map<String,Object> map,  String FT/*inherited*/, Object V/*inherited*/, int Ff/*inherited*/, String T) throws IOException {
	Dict dict = (Dict)cos.getObject(ref);
	//if (FT!=null && dict.get("Parent")==null) return;	// Widget appearance => can be merged into field

	// 1. collect attributes
	Object o = cos.getObject(dict.get("T")); if (T==null) T=o.toString(); else if (o!=null) T = T + "." + o.toString();
	o = dict.get("TM"); String TM = o!=null? cos.getObject(o).toString(): T;	// TM overrides

	o = dict.get("FT"); if (o!=null) FT = cos.getObject(o).toString();	// intermediate can have FT and V for inheriting by kids
	o = dict.get("V"); if (o!=null) V = cos.getObject(o);
	o = dict.get("DV"); Object DV = o!=null? cos.getObject(o): null;	// not inherited?
	o = dict.get("Ff"); if (o!=null) Ff = ((Number)cos.getObject(o)).intValue();
	o = cos.getObject(dict.get("Kids")); Object[] kids = o!=null && CLASS_ARRAY==o.getClass()? (Object[])o: null/*Objects.ARRAY0?*/;

	// 2. process command
	if (dict.get("T")==null) {	// Widget only (not merged with field)
	} else if (CMD_EXPORT==cmd) {
		// add if terminal, not intermediate... how to tell? "T"? => has /Subtype/Widget or kid with /Subtype/Widget and no /T?
		/*if ((FF_NOEXPORT&Ff)==0) ?*/ map.put(TM, V);

	} else if (CMD_DEFAULTS==cmd) {
		if (DV!=null /*&& if (FF_NOEXPORT&Ff)==0*/) map.put(TM, DV);

	//else if (CMD_CLEAR==cmd) pdfw.getReader().setObject(((IRef)ref).id, null);
	} else { assert CMD_SET==cmd || CMD_MERGE==cmd;
		Object newV = map.get(TM);
		boolean fchange = !(newV==V || (newV!=null && newV.equals(V)));	// or newV!=V && (newV==null || !newV.equals(V))		
		if (!fchange) {}
		else if (newV!=null) {
			// conversion and type coercion
			if ("Tx".equals(FT) || "Ch".equals(FT)) {
				if (CLASS_STRING!=newV.getClass()) newV = new StringBuffer(newV.toString());
			} else if ("Btn".equals(FT) && (FF_RADIO&Ff)!=0) {	// translate value to index in /Opt, if exists
				String key = newV.toString(); Object[] opts = (Object[])pdfw.getObject(dict.get("Opt"));
//if (opts!=null) System.out.println("\t"+key+" in "+java.util.Arrays.asList(opts));
				if (opts!=null) for (int i=0,imax=opts.length; i<imax; i++) if (key.equals(pdfw.getObject(opts[i]).toString())) { newV = Integers.toString(i); break; }
			}
			dict.put("V", newV);
		} else if (CMD_SET==cmd && dict.get(TM)!=null) dict.remove("V");

//if (fchange) System.out.println(ref+": ^"+dict.get("Parent")+", "+TM+" = "+newV/*+", "+V.getClass().getName()*/+", pdfr="+pdfw.getReader());
		if (fchange) {
			PDFReader pdfr = pdfw.getReader();
			if (CLASS_IREF==ref.getClass() && pdfr!=null) pdfr.setObject(((IRef)ref).id, null);	// mutated, so reread object for next PDFWriter on same PDFReader

			// set appearance (AP and AS) by state and widget type
			setAppearance(ref, FT,newV,Ff, pdfr,pdfw);	// /Widget merged with field
			if (kids!=null) for (Object kid: kids) setAppearance(kid, FT,newV,Ff, pdfr,pdfw);	// not merged
		}
	}

	// 3. recurse
	if (kids!=null) for (Object kid: kids) process(cmd,  cos, pdfw,  kid,  map,  FT, V, Ff, T);
  }

  private static void setAppearance(Object ref,  String FT, Object V, int Ff,  PDFReader pdfr, PDFWriter pdfw) throws IOException {
	Object o = pdfw.getObject(ref); if (CLASS_DICTIONARY!=o.getClass()) return;
	Dict dict = (Dict)o; if (!"Widget".equals(pdfw.getObject(dict.get("Subtype")))) return;
//System.out.println(ref+" "+FT+" "+((Dict)o).get("Subtype")+" "+Integer.toBinaryString(Ff));

	boolean fchange = false;
	Dict AP = (Dict)pdfw.getObject(dict.get("AP"));
	if ("Tx".equals(FT)) {
		if (V==null) dict.remove("AP");
		else {
			Dict af = ((Dict)pdfw.getObject(pdfw.getCatalog().get("AcroForm")));
			AP = new Dict(); dict.put("AP", AP);
			Dict N = new Dict(); IRef Nref = pdfw.addObject(N); AP.put("N", Nref); N.put("Subtype", "Form");
			Object[] oa = (Object[])pdfw.getObject(dict.get("Rect"));
			float w = Math.abs(((Number)oa[2]).floatValue() - ((Number)oa[0]).floatValue()), h = Math.abs(((Number)oa[3]).floatValue() - ((Number)oa[1]).floatValue());
			N.put("BBox", new Object[] { Integers.ZERO, Integers.ZERO, Integers.getInteger((int)w), Integers.getInteger((int)h) });
			Object DR = af.get("DR"); if (DR!=null) N.put("Resources", DR);

			StringBuffer sb = new StringBuffer();
			sb.append("q BT 0 0 Td\n");
			Object DA = getInherited(dict, "DA", pdfw); if (DA==null) DA = af.get("DA");
			if (DA!=null) {
				String da = DA.toString();
				int inx = da.indexOf(" 0 Tf"); if (inx!=-1) {	// "A zero value for size means that the font is to be autosized
					float size = 12f; // FIX: compute "as a function of the height of the annotation rectangle"
					DA = new StringBuffer(da.substring(0,inx+1) + size + da.substring(inx+" 0".length()));
				}
				sb.append("  ").append(pdfw.getObject(DA)).append("\n");
			}
			sb.append("  (").append(Strings.escape(V.toString(), "()", '\\')).append(") Tj\n");
			sb.append("ET Q\n");
			N.put(STREAM_DATA, Strings.getBytes8(sb.toString()));
//System.out.println("text |"+V+"| => "+sb);
		}
		fchange = true;

	} else if ("Btn".equals(FT)) {
		if ((FF_PUSHBUTTON&Ff)==0) {
			Object val = V==null? "Off": ((o=pdfw.getObject(AP.get("N")))!=null && ((Dict)o).get(V)!=null) || ((o=pdfw.getObject(AP.get("R")))!=null && ((Dict)o).get(V)!=null) || ((o=pdfw.getObject(AP.get("D")))!=null && ((Dict)o).get(V)!=null)? V: "Off";
//Object as0 = dict.get("AS");
			if (!val.equals(dict.get("AS"))) { dict.put("AS", val); fchange = true; }
//if (fchange) System.out.println("not push "+as0+" => "+V+"/"+val);
		}

	} else if ("Ch".equals(FT)) {
		// OK as is

	} else if ("Sig".equals(FT)) {

	} // else others added in future


	if (fchange && CLASS_IREF==ref.getClass() && pdfr!=null) pdfr.setObject(((IRef)ref).id, null);
  }

  /** Climbs up forms tree to find value of inherited attribute. */
  private static Object getInherited(Dict dict, String key, COSSource cos) throws IOException {
	Object val = null;
	for (Dict p = dict; p!=null; p=(Dict)cos.getObject(p.get("Parent"))) if ((val=p.get(key))!=null) break;
	return cos.getObject(val);
  }


  /**
	Flattens forms by fusing appearance onto pages.
	This makes forms uneditable because in fact there is no form as a distinct entity, just its appearance as lines and text.
	Incidentally reduces file size.
	If changed values, first {@link #merge(Map, PDFWriter) merge} or {@link #set(Map, PDFWriter) set}, then flatten.
	Recommended {@link PDFWriter#refcntRemove()} throw out objects no longer used.
  */
  public static void flatten(PDFWriter pdfw) throws IOException {
	PDFReader pdfr = pdfw.getReader();
	List<Object> mu = new ArrayList<Object>(100);	// list of mutated objects, so clear in PDFReader for possible reuse
	Object o = pdfw.getTrailer().get("Root"); mu.add(o);
	Dict cat = pdfw.getCatalog();
	o = pdfw.getObject(cat.remove("AcroForm")); if (o==null || CLASS_DICTIONARY!=o.getClass()) return; Dict af = (Dict)o;

	// for all pages...
	List<Object> q = new LinkedList<Object>(); q.add(cat.get("Pages"));
	while (q.size()>0) {
		Object pref = q.remove(0); Dict page = (Dict)pdfw.getObject(pref);
		Object type = pdfw.getObject(page.get("Type"));
		if ("Pages".equals(type)) {
			for (Object kid: (Object[])pdfw.getObject(page.get("Kids"))) q.add(kid);

		} else { assert "Page".equals(type);
			o = pdfw.getObject(page.get("Annots")); if (o==null || OBJECT_NULL==o) continue; Object[] annots = (Object[])o;
			Object[] newannots = new Object[annots.length]; int newlen=0;
			Object presref = page.get("Resources"); Dict pres = (Dict)pdfw.getObject(presref);
			Object cref = page.get("Contents"); Dict contents = null;
			ByteArrayOutputStream bout = null;

			// for all annotations...
			for (int i=0,imax=annots.length; i<imax; i++) {
				// if /Subtype /Widget, append right appearance stream (and Resources)
				Object aref = annots[i]; o=pdfw.getObject(aref); if (OBJECT_NULL==o) continue; Dict annot = (Dict)o;
				if (!"Widget".equals(pdfw.getObject(annot.get("Subtype"))) /*&& belongs to Form -- all do?*/) { newannots[newlen++] = aref; continue; }
				if (bout==null) {	// create on demand because most pages don't have form fields (though if going to write, have to read anyhow)
//System.out.println("page = "+page);
					contents = (Dict)pdfw.getObject(cref);
					o = contents.get(STREAM_DATA); byte[] data = o!=null? (byte[])o: new byte[0];
					bout = new ByteArrayOutputStream(data.length + 1000); bout.write(data);
				}
				Dict AP = (Dict)pdfw.getObject(annot.get("AP")); Object FT = getInherited(annot,"FT",pdfw);
				Object fuse = null;
				if (AP==null) {
				} else if ("Tx".equals(FT) || "Ch".equals(FT) /* || "Sig".equals(FT)?*/) {
					fuse = AP.get("N");
				} else if ("Btn".equals(FT)) {
					Dict N = (Dict)pdfw.getObject(AP.get("N"));
					Object AS = pdfw.getObject(annot.get("AS"));
					fuse = N.get(AS!=null? AS: "Off");
				}

				if (fuse != null) {
					Dict xobj = (Dict)pdfw.getObject(fuse);
//System.out.println("fuse "+FT+": "+fuse+" => "+xobj);
					if (xobj.get(STREAM_DATA)!=null) {
						StringBuffer sb = new StringBuffer(50);
						//sb.append("% form widget: ").append(fuse).append("\n");
						sb.append("\nq\n");	// q..Q implicit for forms
						Object[] Rect = (Object[])pdfw.getObject(annot.get("Rect"));	// position according to Widget (not XObject) Rect
						sb.append("1 0 0 1 ").append(Rect[0]).append(" ").append(Rect[1]).append(" cm\n");
						Object[] Matrix = (Object[])xobj.get("Matrix");
						if (Matrix!=null) sb.append(Matrix[0]).append(" ").append(Matrix[1]).append(" ").append(Matrix[2]).append(" ").append(Matrix[3]).append(" ").append(Matrix[4]).append(" ").append(Matrix[5]).append(" cm");
						// color and other attributes from Widget /MK
						Dict MK = (Dict)pdfw.getObject(annot.get("MK"));
						//Object[] BC = (Object[])pdfw.getObject(MK.get("BC"));
						//Object[] BG = (Object[])pdfw.getObject(MK.get("BG"));
						bout.write(Strings.getBytes8(sb.toString()));
						bout.write((byte[])xobj.get(STREAM_DATA));	// OR "/form Do".  May or may not save space: share text, but compressed out and have /XObject overhead
						bout.write(Strings.getBytes8("\nQ\n"));

						// merge Resources from XObject into page
						Object wresref = xobj.get("Resources"); Dict wres = (Dict)pdfw.getObject(wresref); wres.remove("ProcSet");
						if (wres==null || wres.size()==0) {}
						else if (pres==null) page.put("Resources", wresref);
						else for (Iterator wresi = wres.keySet().iterator(); wresi.hasNext(); ) {
							Object key = wresi.next(); Object wsubref = wres.get(key); Dict wsub = (Dict)pdfw.getObject(wsubref);
							o = pres.get(key);
							if (o==null) pres.put(key, wsubref);
							else {	// merge sub-resources
								mu.add(o); Dict psub = (Dict)pdfw.getObject(o);
								for (Iterator wsubi = wsub.keySet().iterator(); wsubi.hasNext(); ) {
									key = wsubi.next();
									if (psub.get(key)==null) psub.put(key, wsub.get(key));	// if already exists in /Page, defer to that value
								}
							}
						}
					}
					// lots of objects no longer used, but possibly still wanted, so keep.  User can invoke big cleanup with refcntRemove()
				}
			}

			if (bout!=null) {	// mutated, so reconstruct in PDFReader
				contents.put(STREAM_DATA, bout.toByteArray()); mu.add(cref);
				if (newlen==0) page.remove("Annots"); else page.put("Annots", Arrayss.resize(newannots,newlen));
				mu.add(pref);
				if (pdfr!=null) for (int i=0,imax=mu.size(); i<imax; i++) {
					o = mu.get(i);
					if (CLASS_IREF==o.getClass()) pdfr.setObject(((IRef)o).id, null);	// page content, new /Annots[]
				}
				//X pdfw.refcntRemove(); => recommended but sometimes not wanted
			}
		}
	}
  }
}
