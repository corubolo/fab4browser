package multivalent.node;

import java.awt.Rectangle;
import java.util.Map;

import multivalent.Node;
import multivalent.INode;
import multivalent.Context;


/**
	Place children in horizontal line, no line breaking.

	@see IParaBox for line breaking
	@version $Revision: 1.3 $ $Date: 2002/02/02 13:41:39 $
*/
public class IHBox extends IParaBox/*handle click between words*/ {
  public IHBox(String name, Map<String,Object> attr, INode parent) { super(name,attr, parent); }

/*  public boolean formatNodeOld(int width,int height, Context cx) {
	boolean ret = super.formatNode(PROBEWIDTH/2/*Integer.MAX_VALUE/*how related to PROBEWIDTH?* /,height, cx);
	valid_ = (width>=0 && width<PROBEWIDTH/2);  // Integer.MAX_VALUE throws off IParaBox
	return ret;
  }*/

  /**
	Format children in horizontal row, ignoring ALIGN setting.
	<!--, with all ALIGN=left to the left and all ALIGN=right to the right-->
  */
  public boolean formatNode(int width,int height, Context cx) {
	boolean shortcircuit = false;

	// 1. format children and determine common baseline
	int hmax=0;
	for (int i=0,imax=size(); i<imax && !shortcircuit; i++) {
		Node child = childAt(i);
		if (!child.isValid()) {
			if (!cx.valid) cx.reset(child,-1);
			shortcircuit = child.formatBeforeAfter(width,height, cx);
		} else if (child.sizeSticky()>0) cx.valid=false;

		hmax = Math.max(hmax, child.baseline);    // baseline
	}


	// 2. place children and set own dimensions
	int x = 0, ymax=0;
	if (!shortcircuit) for (int i=0,imax=size(); i<imax; i++) {
		Node child = childAt(i);
		Rectangle cbbox = child.bbox;

		int y = hmax - child.baseline;
		cbbox.setLocation(x, y);

		x += cbbox.width + 2;
		if ((cbbox.width!=0 || cbbox.height!=0) && child instanceof LeafText) x += cx.getFont().charAdvance(' ').getX();	// bad bad bad
		ymax = Math.max(y+cbbox.height, ymax);
	}
	//	if (child.floats==LEFT || child.floats==RIGHT) {

	bbox.setSize(x, ymax);

	valid_ = !shortcircuit;
	return shortcircuit;
  }
}
