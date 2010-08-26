package multivalent.std.span;

import java.util.Map;

import multivalent.*;


/**
	Change case of region by clicking anywhere in span.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:26 $
*/
public class CapSpan extends ActionSpan {
  /** String to show when hovering over link. */
  public static final String ATTR_CAPTYPE = "captype";


  LabelSpan label_=null;


  public void setCaptype(String captype) {
	if (captype==null) captype="CAP";

	putAttr(ATTR_CAPTYPE, captype);

	String title = (captype.endsWith("CAP")? "Capitalize" : "Lower case") + " region";
	putAttr(ATTR_TITLE, title);

	if (label_==null) label_=(LabelSpan)Behavior.getInstance(getName(), "LabelSpan", null,  getDocument().getLayer(Layer.SCRATCH));
	label_.setLabel(title);
  }

  String zap(String me,int offset, boolean cap,boolean first) {
//	  if (me.length()==0) {
		// just leave it alone
//	  } else
	if (first) {
		if (offset==0) me = (cap? me.substring(0,1).toUpperCase(): me.substring(0,1).toLowerCase()) + me.substring(1);
	} else {
		if (cap) me=me.toUpperCase(); else me=me.toLowerCase();
	}
	return me;
  }

// if (first) maybe force span to first letter
  public boolean action() {
//	  if (!isSet()) return false; -- shouldn't be called if not set
	String captype = getAttr(ATTR_CAPTYPE, "CAP");
	boolean cap = captype.endsWith("CAP");
	boolean first = captype.startsWith("I");

	Node startn=getStart().leaf, endn=getEnd().leaf;
	int startoff=getStart().offset, endoff=getEnd().offset;
	if (endoff==0) { endn=endn.getPrevLeaf(); endoff=endn.size(); }
	if (startn==endn) {
		String name = startn.getName();
		startn.setName(name.substring(0,startoff) + zap(name.substring(startoff,endoff),startoff,cap,first) + name.substring(endoff));
	} else {
		String name = startn.getName();
		startn.setName(name.substring(0,startoff) + zap(name.substring(startoff),startoff,cap,first));
		name = endn.getName();
		endn.setName(zap(name.substring(0,endoff),0,cap,first) + name.substring(endoff));
		for (Node n=startn.getNextLeaf(); n!=endn; n=n.getNextLeaf()) n.setName(zap(n.getName(),0,cap,first));
	}

	markDirty();
	getBrowser().repaint(250);
	destroy();

	return false;
  }


  public boolean appearance(Context cx, boolean all) { cx.underline = getLayer().getAnnoColor(); return false; }


  public void moveq(Leaf ln,int lo, Leaf rn,int ro) { super.moveq(ln,lo, rn,ro); label_.moveq(ln,lo, ln,lo+1); }
  public void destroy() {
	super.destroy();
	if (label_!=null) { label_.destroy(); label_=null; }
  }


  //public String getEditInfo() { return " | CAPTYPE | CAP"; }


  // for save and restore, CAPTYPE always kept as attribute, but correct bad input
  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	// correct bad input later
	setCaptype(getAttr(ATTR_CAPTYPE, "CAP"));
  }
}
