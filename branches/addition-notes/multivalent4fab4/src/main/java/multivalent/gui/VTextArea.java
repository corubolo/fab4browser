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

import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Map;

import multivalent.Browser;
import multivalent.Context;
import multivalent.INode;
import multivalent.IScrollPane;
import multivalent.Leaf;
import multivalent.Mark;
import multivalent.Node;
import multivalent.node.IParaBox;
import multivalent.node.LeafText;



/**
    Editable area (not necessarily all text).
    Depends on other behaviors, such as {@link multivalent.std.ui.BindingsDefault} and {@link multivalent.std.ui.BindingsEmacs}, for key bindings,
    and other nodes to display content.

    <p>Content kept as subtree, available as string from getContent().

<!--
    <p>TO DO: Maybe integrate this functionality (all or part) into IScrollPane.
-->

    @version $Revision$ $Date$
*/
public class VTextArea extends IScrollPane {
  /** Amount of context to show around cursor when content too big to fit into box. */
  static final int FUZZ=30;

  INode layout_ = null;
//  LeafAscii dummy_ = new LeafAscii("",null, null);

  public VTextArea(String name,Map<String,Object> attr, INode parent) { this(name,attr, parent, null); }

  /**
    @param layout INode, defaults to IParaBox.  Content should be added to this node,
    which should be the single child of VTextArea.
  */
  public VTextArea(String name,Map<String,Object> attr, INode parent, INode layout) {
    super(name,attr, parent);
    if (layout==null) layout=new IParaBox("layoutT"/*null*/,null, null);
    layout_ = layout;
    /*super.*/appendChild(layout);
    //children_[childcnt_++] = layout; layout.parent_=this;
//System.out.println("layout="+layout+", layout parent = "+layout.getParentNode()+", children_[0]="+children_[0]+", childcnt_="+childcnt_);
    //if (size()==0) appendChild(dummy_);
    fixContent();
//System.out.println("layout parent = "+layout.getParentNode()+", children[0]="+children_[0]);
    setScrollbarShowPolicy(VScrollbar.SHOW_ALWAYS);
    setSizeChars(60,5); // default

    editable = true;
//System.exit(0);
  }

  /** appendChild() adds to formatting node, which is VTextArea's first and only child, instead. */
/*
  public void appendChild(Node child) {
//    if (layout_.childAt(0) == dummy_) layout_.removeChildAt(0);
    //if (size()==1 &&
    while (size()>0 && "".equals(childAt(0).getName())) layout_.removeChildAt(0);
System.out.println("adding "+child.getName()+" to "+layout_.getName());
    layout_.appendChild(child);
  }
  public Node removeChildAt(int num) {
    Node zap = layout_.removeChildAt(num);
    fixContent();
    //if (layout_.size()==0) layout_.appendChild(dummy_);
    return zap;
  }
  public void insertChildAt(Node child, int num) { layout_.insertChildAt(child, num); }
  public void removeAllChildren() {
    layout_.removeAllChildren();
    fixContent();
    //layout_.appendChild(dummy_);
  }
  public Node childAt(int num) { return layout_.childAt(num); }
  public int size() { return layout_.size(); }
  public void setChildAt(Node child, int num) { layout_.setChildAt(child, num); }
*/
  protected void fixContent() {
//    if (size()==0) appendChild(dummy_);
//create and edits should be careful:   if (layout_.size()==0) new LeafAscii("",null, layout_);
    // fix up cursor...
  }

  @Override
public boolean formatNode(int width,int height, Context cx) {
    fixContent();
    //INode format = (INode)childAt(0);
    //if (layout_.size()==0) new LeafAscii("",null, layout_);
    int b = 0; if (border.top==0 && border.bottom==0 && border.left==0 && border.right==0) { b = 1; border=INSETS[b]; }
//System.out.println("border = "+b+" ("+border.top+")");
//System.out.print("VTextArea formatNode "+bbox.width+"x"+bbox.height+" in="+width+"x"+height);
    super.formatNode(width-b-b,height-b-b, cx);
    //layout_.formatBeforeAfter(width-b-b,height-b-b,cx);   //-- duplicates work(?)... no, for some reason
    baseline = bbox.height; // - cx.getFontMetrics().getDescent() + border.top + padding.top;// - padding.bottom - border.bottom;
//System.out.println(", new dim="+bbox.width+"x"+bbox.height);
    return false;
  }

  @Override
public boolean breakBefore() { return false; }    // return layout.breakBefore()?
  @Override
public boolean breakAfter() { return false; }


  @Override
public void paintNode(Rectangle docclip, Context cx) {
/*
    // adjust xoff_ as necessary to show cursor
    CursorMark cur = getBrowser().getCursorMark();
    if (cur.isSet()) {
        int cxoff=cur.getXOffset(), cyoff=cur.getStart().node.baseline;
//System.out.println("leaf cxoff="+cxoff);
        for (Node n=cur.getStart().node; n!=null; n=n.getParentNode()) {
            if (n==this) {
                if (cxoff<xoff_) xoff_=Math.max(0,cxoff);   // show some preceeding content
                else if (cxoff-bbox.width>xoff_) xoff_=cxoff-bbox.width+FUZZ;   // show following content -- no Math.min as want to see some empty space at end
//System.out.println("rel cxoff="+cxoff+", xoff="+xoff_+", bbox.width="+bbox.width);

                if (cyoff<yoff_) yoff_=Math.max(0,cyoff);
                else if (cyoff-bbox.width>yoff_) yoff_=cyoff-bbox.width+FUZZ;
                // set scrollbars
                break;
            } else { cxoff += n.bbox.x; cyoff += n.bbox.y; }
        }
    }
*/
      //cx.g.
      /*
      System.out.println(border);//getBbox()
      System.out.println(padding);
      System.out.println(bbox);
      System.out.println(layout_.bbox);
      */
    super.paintNode(docclip, cx);
    //cx.g.drawRect(margin.x, margin.top);
    //cx.g.draw(bbox);
    //g.setColor(cx.foreground); g.drawRect(0-padding.left,0-padding.top, bbox.width,bbox.height);
  }

  /** If no content, route mouse to 0-length leaf, so can click and type. */
  @Override
public boolean eventNode(AWTEvent e, Point rel) {
    int eid = e.getID();
/*
    if (id==MouseEvent.MOUSE_PRESSED) {
        Node content = getFirstLeaf();
        if (content!=null && content.getName().equals("")) {
//           Point p = content.getRelLocation(this);
//System.out.println("only child "+content+" rel "+p+", margins="+padding.left+","+padding.top);
           //rel.setLocation(0,0);
           rel.setLocation(padding.left, padding.top);
        }
    }*/
    //int dx=BORDER-xoff_, dy=BORDER-yoff_;
//System.out.print("event "+rel.x+","+rel.y);
    //rel.translate(-dx,-dy);
//System.out.println(" => "+rel.x+","+rel.y+" ");
//System.out.println("child bbox = "+childAt(0).bbox);

    // set editing (should check some local flag too).  Set before chlidren so they can narrow.
    Browser br = getBrowser();
//  if (id==MouseEvent.MOUSE_PRESSED) cursor.scope = this;
//System.out.println("scope = "+this);}

    boolean ret = super.eventNode(e, rel);  // pass to children, set cursor on leaf if hits one

    if (eid==MouseEvent.MOUSE_PRESSED && br.getCurNode()==this) {
        //Leaf l = getFirstLeaf();  // => last leaf on same line
        Mark closest = findNearestLeaf(rel);
        getBrowser().getCursorMark().move(closest);
        br.setCurNode(closest);
    }

    // grabs cursor on MOUSE_PRESSED
    // cursor set in BindingsDefault, so not set yet here
    /*if (/*id==TreeEvent.FIND_NODE ||* / id==MouseEvent.MOUSE_PRESSED && !contains(curs.getMark().node)) {
        //if (size()==0) new LeafAscii("", null, this); -- done during format
System.err.println("curs node = "+curs.getMark().node);

        //Node n = null;
        Leaf l = null;
        //if (layout_.size()==1 && dummy_==layout_.childAt(0)) br.setCurNode(dummy_,0);
        if (layout_.size()==1 && (l=getFirstLeaf())==getLastLeaf() && "".equals(l.getName())) br.setCurNode(l,0);
        else br.setCurNode(getLastLeaf(), getLastLeaf().size());
    }*/
    //rel.translate(dx,dy);
    // ensure cursor is visible
    //Mark cur = getBrowser().getCursorMark();
    return ret;// || (id==MouseEvent.MOUSE_PRESSED);
  }

  /**
  */
  Mark findNearestLeaf(Point rel) {
    Mark closest = new Mark();
    int distance = Integer.MAX_VALUE;
    int x0=rel.x, y0=rel.y;
    for (Leaf l=getFirstLeaf(), lend=getLastLeaf().getNextLeaf(); l!=lend; l=l.getNextLeaf()) {
        Point p = l.getRelLocation(this);   // can be slow?
        int d = (p.x-x0)*(p.x-x0) + (p.y-y0)*(p.y-y0);
        if (d<distance) {
            closest.leaf = l;
            closest.offset = (x0<p.x? 0: l.size());
        }
    }
    return closest;
  }


  /** Collect up textual leaves into a space-separated String. */
  public String getContent() {
    // set up vars for possible use by script
    StringBuilder sb = new StringBuilder(100);
    for (Node n=getFirstLeaf(), e=getLastLeaf().getNextLeaf(); n!=e; n=n.getNextLeaf()) {
        if (n instanceof LeafText) sb.append(n.getName()).append(' ');
    }
    if (sb.length()>0) sb.setLength(sb.length()-1); // chop trailing space
//System.out.println(getFirstLeaf()+".."+getLastLeaf().getNextLeaf()+" => "+sb.substring(0));
//    System.out.println("++"+sb.substring(0));
    return sb.substring(0);
  }


/*
  public void dump(int level, int maxlevel) {
    for (int i=0; i<level; i++) System.out.print("  ");
    System.out.println("textarea/"+getName()+" "+bbox);
    layout_.dump(level+1, maxlevel);
  }*/
}
