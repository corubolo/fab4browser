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
import java.awt.geom.Ellipse2D;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;


/**
    Radiobox widget: only on in associated Radiogroup can be selected at the same time.
    Usable as button and menu item.

<!--
    <p>To do: replace VRadiogroup with key into Document globals so other behaviors can play too.
-->

    @version $Revision$ $Date$
*/
public class VRadiobox extends VButton {
  /** Length of side of radiobox diamond. */
  static final int LENGTH=10;
  static final int GAP = 5;
  static final Insets ZEROGAP = new Insets(0,5,0,0);  // top,left,bottom,right
  //static int[] xpts={LENGTH/2,LENGTH,LENGTH/2,0}, ypts={0,LENGTH/2,LENGTH,LENGTH/2};    // LENGTH/2,0, LENGTH,LENGTH/2, LENGTH/2,LENGTH, 0,LENGTH/2,
  //static Polygon diamond_ = new Polygon(xpts, ypts, xpts.length);
  static final Ellipse2D.Double diamond_ = new Ellipse2D.Double(0,0, LENGTH,LENGTH);
  static final Ellipse2D.Double diamond2_ = new Ellipse2D.Double(2,2, LENGTH-4,LENGTH-4);
  static Insets lastold_=INSETS_ZERO, lastnew_=ZEROGAP;

  VRadiogroup group_;


  public VRadiobox(String name,Map<String,Object> attr, INode parent, VRadiogroup group) {
    super(name,attr, parent);
    setRadiogroup(group);
  }

  public boolean getState() { return (group_!=null && group_.getActive()==this); }
  public void setState(boolean b) { if (b!=getState() && group_!=null) group_.setActive(b?this:null); }

  public VRadiogroup getRadiogroup() { return group_; }
  public void setRadiogroup(VRadiogroup group) { setState(false); group_=group; }

  /** Ensure that padding on left is large enough for checkbox. */
  @Override
public boolean formatNode(int width,int height, Context cx) {
    int lp=padding.left, dp=0;
    if (lp<GAP) {
        if (padding==INSETS_ZERO) padding=ZEROGAP;
        else if (padding==lastold_) padding=lastnew_;
        else {
            lastold_=padding;
            padding = lastnew_ = new Insets(padding.top,GAP,padding.bottom,padding.right);
            
        }
        dp = lp-GAP;
    }

    super.formatNode(width+dp,height, cx);

    //if (!cx.elide && bbox.height<LENGTH) { baseline += LENGTH - bbox.height; bbox.height = LENGTH; }
    if (!cx.elide && bbox.height<LENGTH) bbox.height = baseline = LENGTH;

    return false;
  }


  /** Draw radiobox too. */
  @Override
public void paintNode(Rectangle docclip, Context cx) {
      int GAPO = GAP, tapo=2;
      
      if ((parent_ instanceof VMenu)) {
          super.paintNode(docclip,cx);
          GAPO= 13;
          tapo=0;
          }
      else
          super.paintNode(docclip,cx);
    int dx=(GAPO-LENGTH)/2-GAPO, dy=(bbox.height-padding.top-padding.bottom-border.top-border.bottom - LENGTH)/2+tapo;   // not: baseline - LENGTH -2
    
    //dy= (bbox.height - LENGTH + 1)/2;
    //System.out.println("new radio dy = "+dy);
    Graphics2D g = cx.g;
    g.translate(dx,dy);
    //g.setColor(cx.foreground); if (getState()) g.fillPolygon(diamond_); else g.drawPolygon(diamond_);
    g.setColor(cx.foreground); 
    if (inside_) {
        g.setColor(darkGray); 
        g.fill(diamond_);
        g.setColor(cx.foreground); 
        }
    g.draw(diamond_);
    if (getState()) g.fill(diamond2_); 
    else {
        g.setColor(gray);
        g.fill(diamond2_);
        } 
    //g.setColor(cx.foreground); if (getState()) g.fillOval(x,y, LENGTH,LENGTH); else g.drawOval(x,y,LENGTH,LENGTH);    // looks bad in outline at small sizes
    g.translate(-dx,-dy);
  }

  /** Change state, then call associated VScript, if any. */
  @Override
public void invoke() {
    setState(true);
    super.invoke();
  }
}
