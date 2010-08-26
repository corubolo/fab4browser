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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TimerTask;

import multivalent.Browser;
import multivalent.Context;
import multivalent.EventListener;
import multivalent.INode;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.node.IParaBox;
import multivalent.node.LeafUnicode;
import multivalent.std.VScript;



/**
    Menu button widget that pops up associated {@link VMenu} when mouse pressed.
    Scripts can be associated with menu items (which are arbitrary child {@link Node}s--not subclasses--of VMenu).
    If a script is associated with {@link VMenuButton} itself, then it functions as
    a <i>button when clicked</i>, executing that script and not popping up the menu;
    and a <i>menubutton when pressed</i>.
    If parent is {@link VMenu}, operates as cascade; else as pulldown.
    For a popupmenu, simply invoke {@link #post()} in the VMenu.

    <p>{@link VMenu}s can either be supplied as children (as in HTML), or can be {@link #setDynamic(String) made <i>dynamic</i>}.
    Dynamic menus are constructed afresh each time they are posted,
    by instantiating an empty {@link VMenu} and passing around a semantic event with the message
    <tt>createWidget/<var>dynamic-name</var><tt>.
    Common menus -- which may or may not actually exist in a given Browser -- are given in {@link VMenu#MSG_CREATE_FILE VMenu}.

    <p>Can be given both a menu (either as a child or dynamically) and a script.
    In this case, a quick click executes the script, and a mouse press pops up the menu.

<!--
Notes:
    <p>Unify this and VButton?  No, everything overridden, and event different
-->

    @version $Revision$ $Date$
*/
public class VMenuButton extends IParaBox implements EventListener {
  /** For buttons that can function with both a SCRIPT and a menu, delay before menu function supercedes SCRIPT. */
  static int DELAY = 1000/2;     // half-second -- take from Preferences (at each popup)?

  /**
    Document use of attribute in hub document (convention: "ATTR_" prefix).
  */
  /*public*/ static final String ATTR_GENERATE = "generate";



  VMenu menu_=null;         // associated menu, taken from children; else dynamically generated
  String generate_ = null;  // if set, always dynamically generated menu with given semantic name.  Event is "createMenu/<generate>".
  boolean button_=false;

  TimerTask tt = null;



  public VMenuButton(String name,Map<String,Object> attr, INode parent) { this(name,attr, parent, null); }
  public VMenuButton(String name,Map<String,Object> attr, INode parent, String script) {
    super(name,attr, parent);
    if (script!=null) putAttr(ATTR_SCRIPT, script);
  }

  /**
    Menus can be dynamically generated.
    Each time such a menu is to be posted, it is created from scratch by
    sending an empty VMenu around in the semantic event <tt>createMenu/<i>name</i></tt>,
    at which time behaviors build its content.
  */
  public void setDynamic(String name) {
    generate_=name;
    if (name!=null) menu_=null;
  }


  /** Takes witdh and height from max dimensions of associated VMenu's children. */
  @Override
public boolean formatNode(int width,int height, Context cx) {
    // assuming have VMenu as child, my content is largest bbox of *its* children
    //menu_ = null; -- menu is regularly removed and put into different layer while in use
    int mx=0, my=0;
    if (menu_==null) for (int i=0,imax=size(); i<imax; i++) {
        Node child = childAt(i);
        if (child instanceof VMenu) {
            menu_=(VMenu)child;
            break;
        }
    }

    if (menu_!=null) { mx=menu_.bbox.x; my=menu_.bbox.y; }      // don't affect position of menu

    boolean ret = super.formatNode(width,height, cx);

//  bbox.setSize(0,0);

    // if can function as button, first child gives content to display in place of active menu item
    boolean button = false; //getAttr(ATTR_SCRIPT)!=null;
    // => better to just setMenu() and keep in field rather than messing with searching for, attaching and removing a child
    if (size()>0) {
        Node child = childAt(0);
        if (button = !(child instanceof VMenu)) {
            //child.formatBeforeAfter(width,height, cx); -- already formatted
            bbox.setSize(child.bbox.width, child.bbox.height);
        }
    }

    //bbox.setSize(menu_.bbox.width, menu_.getMaxItemHeight());    // LATER: need to add border
    // if take title from some child, take max height of children
    if (!button && menu_!=null) {
        int hmax=0;
        for (int j=0,jmax=menu_.size(); j<jmax; j++) hmax = Math.max(hmax, menu_.childAt(j).bbox.height);
        bbox.setSize(Math.min(menu_.bbox.width,300), hmax);
//System.out.println("menu child, size="+menu_.size()+", "+bbox);
    }

    //assert menu_!=null || button: getName()+" must either display as a button or have a menu attached at build";

    if (menu_!=null) { menu_.bbox.x=mx; menu_.bbox.y=my; }
    if (getName()=="select") {
    bbox.width=bbox.width+13;
    }
    baseline = bbox.height;// + border.top+border.bottom+padding.top+padding.bottom;
    return ret;
  }

  @Override
public boolean breakBefore() { return false; }
  @Override
public boolean breakAfter() { return false; }


  /** Shows active child by drawing it directly. */
  @Override
public void paintNode(Rectangle docclip, Context cx) {
    Node paintitem = childAt(0);
    //if (getAttr(ATTR_SCRIPT)!=null) paintitem=childAt(0);
    if (paintitem==null || paintitem instanceof VMenu) paintitem=menu_.getSelected();
    //else if (menu_!=null) paintitem=menu_.getSelected();
//System.out.println("painting "+paintitem);

    //if (menu_!=null && (activeitem=menu_.getSelected())!=null) {
    if (paintitem!=null) {
        Graphics2D g = cx.g;
        //g.translate(3, 0);
//        bbox.x=-3;
//        g.translate(3,0);
        paintitem.paintNode/*paintBeforeAfter*/(docclip, cx);    // no coordinate transformations as draw directly
//        g.translate(-3,0);
//        bbox.x=0;
//        bbox.y=-4;
//            g.fillRect(bbox.x+bbox.width-12, bbox.y, 12, 12);
        // if in VMenu, it's a cascade, so draw arrow indicating this
        //g.translate(-6, -4);
//      System.out.println("painting "+paintitem);//+" @ "+dx+","+dy);
              if (getName()=="select") {
                  
                  //g.setColor(Color.BLACK);
                  //g.drawRect(bbox.x-3, bbox.y-4, bbox.width,bbox.height-1);
                  g.setColor(VButton.gray);
                  //g.setColor(new Color(150,230,150));
                  g.fillRoundRect(bbox.width-22, bbox.y, 16, bbox.height-bbox.y-8, 3, 3);
                  g.setColor(Color.GRAY);
                  int [] xs = new int[] {bbox.width-20,bbox.width-8,bbox.width-14};
                  int [] ys = new int[] {bbox.y+2,bbox.y+2,bbox.height-9};
                  g.fillPolygon(xs, ys, 3);
                  
                  
                  //System.out.println(g.getBackground());
                  //System.out.println(g.getColor());
                  
              }

        
        if (getParentNode() instanceof VMenu) {
            g.setColor(cx.foreground);
            int w=bbox.width, h=bbox.height-1, r=w-padding.left-border.left-border.right/*-padding.right*/-1, l=r-5, m=h/2, t=m-3, b=m+3;
            g.drawLine(r,m, l,t); g.drawLine(r,m, l,b); g.drawLine(l,t, l,b);  // simple arrowhead
        }
        //g.translate(3, 4);

    }
  }


  /**
    Post associated menu on screen, under root.
  */
  public void post() { post(null); }

  void post(VMenu prevm) {
    Browser br=getBrowser();
    VMenu menu = menu_;
    Point pt = null;

    if (generate_!=null) {
        pt = getAbsLocation();    // aka rel to root

        if (prevm!=null && generate_.equals(prevm.getAttr(ATTR_GENERATE))) {
            menu = prevm;
            menu.setParentNode(null);   // not set to null on remove()
            menu.post(menu.bbox.x, menu.bbox.y, br);
//System.out.println("reusing "+generate_);
            return;

        } else {
//System.out.println("new "+generate_);
            //menu_ = new VMenu("MENU",null, this);   // => don't want generated menu attached back!
            //menu_ = new VMenu("MENU",null, null);   // not attached back, but cached so menu scanning faster => more bookkeeping, maybe LATER
            menu = new VMenu("menu"/*for style sheet*/,null, null);
            menu.putAttr(ATTR_GENERATE, generate_);
            br.event/*no q!*/(new SemanticEvent(br, "createWidget/"+generate_, null, null, menu));
            //menu.dump();
            if (menu.size()==0) new LeafUnicode("(empty for '"+generate_+"')",null, menu);
        }
    } else {
        pt = new Point(0,0);
    }

//System.out.println("menubutton post @ "+pt.x+","+pt.y+"+"+bbox.height);

//System.out.println("parent "+getParentNode()+", class="+getParentNode().getClass().getName());
    if (getParentNode() instanceof VMenu) { // nested in menu: draw to right as cascade
        INode p = getParentNode();
        pt.x = p.bbox.x + p.bbox.width - p.padding.right - p.border.right;     // parent in absolute already, -1 => no double bordersvv
        //pt.x = p.bbox.x + p.bbox.width/2;     // overlap so don't have to drag over so far right but still drag-scan down, parent in absolute already
        //pt.x = br.getCurScrn().x + 10;     // easy to select, but can still drag-scan
        //pt.x += bbox.width /*- padding.leftrepaint( - border.left*/ - border.right-padding.right;

    } else pt.y += bbox.height /*- padding.bottom*/ - border.bottom;   // pulldown, so draw menu below button

/*
System.out.println("posting menu @ "+pt.x+","+pt.y+"+"+bbox.height+"="+(pt.y+bbox.height));
System.out.println(bbox);
*/
    /*if (menu.size()>0) -- */ menu.post(pt.x, pt.y, br);
  }


  /**
    Just post associated menu and get out of the way.
    If has script and click, treat as button; if no script or press and has menu, treat as menubutton.
  */
  @Override
public boolean eventNode(AWTEvent e, Point rel) {
    int eid = e.getID();
    Browser br = getBrowser();

    if (eid==MouseEvent.MOUSE_PRESSED) {
        String script = getAttr(ATTR_SCRIPT);
        if (script==null) post();

        else if (menu_==null) VScript.eval(script, getDocument(), getAttributes(), this);   // if so, should have just used VButton

        else {  // could be either button or menubutton
            button_=true;
            tt = new TimerTask() {
                @Override
				public void run() {
                    tt = null;
                    button_ = false;
                    getBrowser().releaseGrab(VMenuButton.this);
                    post();
                }
            };
            getGlobal().getTimer().schedule(tt, DELAY);
            br.setGrab(this);
        }
    }
    br.setCurNode(this,-1);

    return false;
  }


  public void event(AWTEvent e) {
    int eid=e.getID();
    Browser br = getBrowser();

/*  if (eid==TreeEvent.HEARTBEAT && button_ && System.currentTimeMillis() > lastPing_+DELAY) {
        button_=false;
        br.releaseGrab(this);
        tt.cancel();
        post();

    } else*/ if (eid==MouseEvent.MOUSE_RELEASED && button_) {
        button_=false; br.releaseGrab(this); tt.cancel();  //getGlobal().getTimer().deleteObserver(this);
        VScript.eval(getAttr(ATTR_SCRIPT), getDocument(), getAttributes(), this);
    }
  }

  /* Called programmatically, post associated menu, if any; else invoke script, if any. */
  //public void invoke() {
  //}
}
