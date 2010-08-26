package multivalent.node;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Point;
import java.util.*;
import java.util.Map;

import multivalent.INode;
import multivalent.Leaf;
import multivalent.Context;

import phelps.awt.Colors;



/**
	Block empty rectangular placeholder, for replacing advertisements, say.

	@version $Revision: 1.3 $ $Date: 2002/02/02 13:41:39 $
*/
public class LeafBlock extends Leaf {
  int w_,h_;

  public LeafBlock(String name,Map<String,Object> attr, INode parent, int w,int h) {
	super(name,attr, parent);
	w_=w; h_=h;  assert w>=0 && h>=0: getName()+" "+w+"x"+h+" "+getClass().getName();
  }

  //public void paintBeforeAfter(Rectangle docclip, Context cx) {}
  public void paintNode(Rectangle docclip, Context cx) {
	Color c = cx.foreground;
	if (c!=Context.COLOR_INVALID && c!=Colors.TRANSPARENT) {
		cx.g.setColor(c);
		cx.g.fillRect(0,0, w_,h_);
	}
  }

  public boolean formatBeforeAfter(int width, int height, Context cx) {
  //public boolean formatNode(int width, int height, Context cx) {
	bbox.setSize(w_,h_);
	valid_=true;
	return !valid_;
  }

  //public int subelementHit(Point p) { return 0; }
  //public void subelementCalc(Context cx) {}
}
