package multivalent.gui;

import java.awt.Graphics2D;
import java.util.Map;

import multivalent.*;


/**
	Menu separator widget, whose name is used to identify menu categories.
	Formats as horizontal rule, unless if first or last in menu or follows another sepaarator, in which case it's an empty (0x0) box.

	@version $Revision: 1.2 $ $Date: 2002/01/27 02:02:51 $
*/
public class VSeparator extends Leaf {
  /** UI category name often stuffed in name. */
  public VSeparator(String name,Map<String,Object> attrs, INode parent) { super(name,attrs, parent); }

  public boolean formatNodeContent(Context cx, int start, int end) {
	INode p = getParentNode();
	if (start==0 && p!=null && this!=p.getLastChild() && this!=p.getFirstChild() && !p.childAt(childNum()+1).getName().startsWith("_")) {
		 bbox.setBounds(0,0, 10,1+8);
	}
	valid_ = true;
	return !valid_;
  }

  public boolean paintNodeContent(Context cx, int start, int end) {
	if (bbox.height>0) {
		Graphics2D g = cx.g;
		g.setColor(cx.foreground);
		int midy = bbox.height/2;
		INode p = getParentNode();
		int w = (p!=null? p.bbox.width: bbox.width);
		g.drawLine(5,midy, w-12,midy);
	}
	return false;
  }
}
