package multivalent.std.span;

import java.util.Map;

import multivalent.*;


/**
	A copy editor mark: bold/italics/under suggestion.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:26 $
*/
public class BIUSpan extends ActionSpan {
  public static final String ATTR_TYPE = "type";

  static final int BOLD=0, ITALIC=1, UNDERLINE=2;	   // overstrike, ...
  static final String[] biuString_ = { "Boldface", "Italicize", "Underline" };

  int biu_;
  LabelSpan label_=null;


  public void setType(char biu) {
	assert 0<=biu && biu<=2;
	biu_=char2off(biu);
  }

  int char2off(char biu) {
	switch (biu) {
		case 'b': case 'B': return BOLD;
		case 'i': case 'I': return ITALIC;
		case 'u': case 'U': return UNDERLINE;
		default: return BOLD;
	}
  }

  public boolean appearance(Context cx, boolean all) {
	cx.underline=getLayer().getAnnoColor();
 /*
	switch (biu_) {
	case BOLD: cx.style=NFont.WEIGHT_BOLD; break;
	case ITALIC: cx.style=NFont.FLAG_ITALIC; break;
	case UNDERLINE: cx.underline = layer_.getAnnoColor(); break;
	}*/
	return false;
  }


  public void moveq(Leaf ln,int lo, Leaf rn,int ro) { super.moveq(ln,lo, rn,ro); label_.moveq(ln,lo, ln,lo+1); }

  public void destroy() {
	if (label_!=null) label_.destroy(); label_=null;
	super.destroy();
  }


  public boolean action() {
	// if have permission to change base document...

	// want to make this part of base document
	String spanname;
	switch (biu_) {
	case BOLD: spanname="BoldSpan"; break;
	case ITALIC: spanname="ItalicSpan"; break;
	case UNDERLINE: spanname="UnderlineSpan"; break;
	default: spanname=null;
	}
	if (spanname!=null) {
		Span newspan = (Span)Behavior.getInstance(getName(), spanname,null, getLayer()/*should be baselayer*/);
		newspan.moveq(this);
	}
	destroy();

	return true;
  }

  //public String getEditInfo() { return " | TYPE | B"; }
  public ESISNode save() {
	putAttr(ATTR_TYPE, biuString_[biu_]);
//System.out.println("*** SAVING BIUSpan");
	return super.save();
  }

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	String type = getAttr(ATTR_TYPE);
	if (type==null) biu_=BOLD; else biu_=char2off(type.charAt(0));	// just check first character
	if (label_==null) label_=(LabelSpan)Behavior.getInstance(getName(), "LabelSpan", null,  layer.getDocument().getLayer(Layer.SCRATCH));
	label_.setLabel(biuString_[biu_]+" region");
  }
}
