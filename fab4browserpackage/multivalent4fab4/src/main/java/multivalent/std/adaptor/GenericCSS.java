package multivalent.std.adaptor;

//import java.awt.Font;
//import java.awt.Insets;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.Iterator;



/**
	<i>Placeholder</i>
	Generic context modifier, tuned to CSS properties.

	@version $Revision$ $Date$
*/
public class GenericCSS /*extends CLGeneral -- NO */ {

/*
  public CLGeneral() { invalidate(); }

  void invalidate() {
	super.invalidate();
  }
*/
  /** All attributes or just inherited ones? */
/*
  public boolean appearance(Context cx, boolean all) {

  }

  public boolean equals(Object o) {
  }
  public int hashCode() {
	return super.hashCode();
  }
*/

  /* * CssListener directly implements some attributes, like font and color,
	  sets attributes in Context for the rest.
	  => merge with GenericContext?  override to add setAttr?  replace GenericContext?
	  (CLGeneral as overhead, and CLGeneral inherits from it--but don't have so many style sheet settings per document)
  */
  /*
  class CssListener implements ContextListener {
	String family=null;
	int size=-1,style=-1;
	Color fg=null, bg=null;
	Map attrs=null;	// might be all special cases

	public void setAttr(String name, String value) {

		if ("color".equals(name)) {
			fg = Colors.getColor(value);
System.out.println("set color to "+value+"/"+fg);

		} else if ("backgrond-color".equals(name)) {	// "background" is more general--can be an image
			bg = Colors.getColor(value);
System.out.println("set color to "+value+"/"+bg);

		} else if ("font-family".equals(name)) {	// "font" is more general--can be an image
		  // generic names: 'serif' (e.g. Times), 'sans-serif' (e.g. Helvetica), 'cursive' (e.g. Zapf-Chancery), 'fantasy' (e.g. Western), 'monospace' (e.g. Courier)
		  // Java guaranteed: "Dialog", "DialogInput", "Monospaced", "Serif", "SansSerif", "Symbol"
		  StringTokenizer st = new StringTokenizer(value, ", ");	// comma-separated list of possible font names
		  String fam=null;
		  while (st.hasMoreTokens()) {
			fam = st.nextToken();
			if ("sans-serif".equalsIgnoreCase(fam)) fam="SansSerif";
			if ("cursive".equalsIgnoreCase(fam)) fam="ZapfChancery";
			//if ("fantasy".equalsIgnoreCase(fam)) fam="???";
			if ("monospace".equalsIgnoreCase(fam)) fam="Monospaced";

			int inx = Arrays.binarySearch(families, fam, String.CASE_INSENSITIVE_ORDER);
			if (inx>=0) { family=families[inx]; System.out.println("fam=>"+families[inx]); break; }
		  }

		} else if ("font-style".equals(name)) {
			// normal | italic | oblique
			if ("italic".equals(value)) NFont.FLAG_ITALIC;

		} else if ("font-weight".equals(name)) {
			// normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900
			if (!"normal".equals(value)) style=NFont.WEIGHT_BOLD;

		} else if ("font-size".equals(name)) {
			// <absolute-size> | <relative-size> | <length> | <percentage>
			if (value.endsWith("pt")) {
				try { size = Integer.parseInt(value.substring(0,value.length()-2)); } catch (NumberFormatException nfe) {}
			}

		} else {
			if (attrs==null) attrs=new HashMap(5);
			attrs.put(name, value);
		}
	}


	public boolean appearance(Context gs, boolean all) {
//System.out.println("CSS appearance");
		if (family!=null) gs.family=family;
		if (size!=-1) gs.size=size;
		if (style!=-1) gs.style=style;
		if (fg!=null) gs.foreground=fg;
		if (bg!=null) gs.background=bg;

		// set rest of attributes here
		//if (attrs!=null) ...

		return false;
	}
	public boolean paintBefore(Context gs, Node n) { return false; }
	public boolean paintAfter(Context gs, Node n) { return false; }
	public int getPriority() { return ContextListener.PRIORITY_STRUCT; }
  }
	*/
}
