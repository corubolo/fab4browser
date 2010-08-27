/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent.gui;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;



/**
    Checkbox widget: toggle ON and OFF.
    Usable as button and menu item.

    @version $Revision$ $Date$
*/
public class VCheckbox extends VButton {
  /** Length of side of checkbox square. */
  static final int LENGTH=10;
  static final int GAP = LENGTH+6;
  static final Insets ZEROGAP = new Insets(0,GAP,0,0);  // top,left,bottom,right

  static Insets lastold_=INSETS_ZERO, lastnew_=ZEROGAP;

  boolean state_=false;

  public VCheckbox(String name,Map<String,Object> attr, INode parent) { super(name,attr, parent); }

  public boolean getState() { return state_; }

  public void setState(boolean b) { if (state_!=b) { state_=b; repaint(); } }


  /** Ensure that padding on left is large enough for checkbox. */
  @Override
public boolean formatNode(int width,int height, Context cx) {
    int lp=padding.left, dp=0;
    if (lp<GAP) {
        if (padding==INSETS_ZERO) padding=ZEROGAP;
        else if (padding==lastold_) padding=lastnew_;   // recycling probably not very effective
        else {
            lastold_ = padding;
            padding = lastnew_ = new Insets(padding.top,GAP,padding.bottom,padding.right);
//System.out.println("new check padding = "+padding);
        }
        dp = lp-GAP;    // subtracted off padding before entry, so add back, but take away new
    }
    super.formatNode(width+dp, height, cx);

    //if (!cx.elide && bbox.height<LENGTH) { baseline += LENGTH - bbox.height; bbox.height = LENGTH; }
    if (!cx.elide && bbox.height<LENGTH) bbox.height = baseline = LENGTH;

    return false;
  }

  /** Draw checkbox too. */
  @Override
public void paintNode(Rectangle docclip, Context cx) {
      if ((parent_ instanceof VMenu))
          super.paintNode(docclip,cx);

    //int x=LENGTH/2+(inside_?1:0), y=bbox.height/2-LENGTH/2+(inside_?1:0);
    int x=(GAP-LENGTH)/2-GAP, y=(bbox.height-padding.top-padding.bottom-border.top-border.bottom - LENGTH)/2+2;   // not: baseline - LENGTH -2
    //x = -11;
    //x = bbox.width-(LENGTH/2+LENGTH/*+2*/);   // checkbox on right
    Graphics2D g = cx.g;
    
    Object prevAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    if (inside_) {
        g.setColor(darkGray);
        g.fillRect(x,y, LENGTH,LENGTH);
    }
    g.setColor(cx.foreground);
    g.drawRect(x,y, LENGTH,LENGTH);
    g.setColor(darkGray);
    g.drawRect(x+1,y+1, LENGTH-2,LENGTH-2);
    g.setColor(cx.foreground);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
    
    if (getState()) { //g.fillRect(x+2,y+2, LENGTH-4,LENGTH-4);
        g.drawPolyline(new int[] {x+3,x+LENGTH/2-2, x+LENGTH+2}, new int[] {y+3,y+LENGTH-4,y-1},3);
        g.drawPolyline(new int[] {x+2,x+LENGTH/2-1, x+LENGTH+3}, new int[] {y+2,y+LENGTH-3,y-2},3);
    }
    
  }


  /** Change state, then call associated VScript, if any. */
  @Override
public void invoke() {
    setState(!getState());
    super.invoke();     // any script gets updated value (need before and after?)
  }
}

