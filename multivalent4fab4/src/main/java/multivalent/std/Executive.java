package multivalent.std;

import java.awt.Point;
import java.awt.AWTEvent;
import java.awt.event.MouseEvent;

import multivalent.*;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;
import multivalent.std.ui.DocumentPopup;



/**
	Collapse document, showing just Notemarks and structure.

	@version $Revision: 1.3 $ $Date: 2002/02/01 04:16:30 $
*/
public class Executive extends Behavior {
  /**
	Turn on or off or toggle.
	<p><tt>"executiveSummary"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>on/off/toggle</var>.
  */
  public static final String MSG_SUMMARY = "executiveSummary";


  /** High priority span: crunch margins. */
  static class ExecSpanHigh extends Span {
	public boolean appearance(Context cx, boolean all) {
		cx.margintop = cx.marginbottom = 0;	 // pack content, which is the point
		return false;
	}
	public int getPriority() { return ContextListener.PRIORITY_SPAN+2*ContextListener.LOT; }
  }

  /** Low priority span: elide < Notemark, catch click after outline has expanded. */
  static class ExecSpanLow extends Span {
	public boolean appearance(Context cx, boolean all) {
		cx.size=10f; cx.elide=true;
		return false;
	}
	/** Lower priority than style sheet-based settings so some things show through. */
	public int getPriority() { return ContextListener.PRIORITY_STRUCT-2*ContextListener.LOT; }

	/** When click, resolve click to leaf, undo elision, scroll to leaf. */
	public boolean eventAfter(AWTEvent e, Point scrn, Node obsn) {
//System.out.println(e.getID()+" @ "+getBrowser().getCurMark());
		// elaborate/filter
		if (e.getID()==MouseEvent.MOUSE_CLICKED && ((((MouseEvent)e).getModifiers())&MouseEvent.BUTTON1_MASK)!=0) {
			Browser br = getBrowser();
			br.eventq(MSG_SUMMARY, "OFF");
			br.eventq(IScrollPane.MSG_SCROLL_TO, br.getCurNode());    // after formatting, I hope -- in Low so other scrolling happens first
//System.out.println("eventq scrollTo "+br.getCurNode());
		}
		return false;
	}
	/** @return true to stop execution of hyperlink, which executes in its eventAfter.
	public boolean eventAfter(AWTEvent e, Point scrn, Node obsn) {
		return true;    // don't execute hyperlink! -- need to be in hi for that
	}*/
  }


  Span elideSpanHigh_=null;
  Span elideSpanLow_=null;
  boolean active_=false;



  /** Put checkbox in View menu and document popup.  => Move to user hub */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_VIEW==msg || (DocumentPopup.MSG_CREATE_DOCPOPUP==msg  && se.getIn()==null)) {//!=getBrowser().getSelectionSpan())) {
		INode menu = (INode)se.getOut();
		VCheckbox ui = (VCheckbox)createUI("checkbox", "Executive Summary", "event "+MSG_SUMMARY+" toggle", menu, "VIEW", false);
		ui.setState(active_);
	}
	return false;
  }

  /** On "executiveSummary", mark tree dirty and repaint (which invokes reformat). */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_SUMMARY!=msg) return false;

	Browser br = getBrowser();
	Document doc = br.getCurDocument();

	boolean oldactive=active_;
	Object arg=se.getArg();
	if ("toggle".equalsIgnoreCase((String)arg)) { br.eventq(new SemanticEvent(br, msg, (active_?"off":"on"))); return false; }     // rebroadcast so other behaviors know setting
	else if ("off".equalsIgnoreCase((String)arg)) active_=false;
	else active_=true;
	if (oldactive==active_) return false;	// nothing to do


	//Span elideSpanHigh_=(Span)doc.getGlobal("elideSpanHigh"), elideSpanLow_=(Span)doc.getGlobal("elideSpanLow");
	//if (elideSpanHigh_==null) { elideSpanHigh_=(Span)Behavior.getInstance("ElideSpan",null, doc.getLayer(Layer.SCRATCH)); doc.putGlobal("elideSpan", elideSpanHigh_); }
	if (elideSpanHigh_==null) {
		elideSpanHigh_=new ExecSpanHigh(); elideSpanHigh_.restore(null,null,doc.getLayer(Layer.SCRATCH)); elideSpanHigh_.setName("exechi"); //doc.putGlobal("elideSpanHigh", elideSpanHigh_);
		elideSpanLow_=new ExecSpanLow(); elideSpanLow_.restore(null,null,doc.getLayer(Layer.SCRATCH)); elideSpanLow_.setName("execlow"); //doc.putGlobal("elideSpanLow", elideSpanLow_);
	}

	if (active_) {
		Leaf ln=doc.getFirstLeaf(), rn=doc.getLastLeaf();
		elideSpanHigh_.moveq(ln,0, rn,rn.size()); elideSpanLow_.moveq(ln,0, rn,rn.size());
		doc.getVsb().setValue(0);
//System.out.println("adding elide span  "+ln+".."+rn); //-- not getting picked up during format
	} else {
//System.out.println("removing elide span");
		//elideSpanHigh_.remove(); elideSpanLow_.remove();    // remove so that not prevailing span for docpopup when not active
		elideSpanHigh_.moveq(null); elideSpanLow_.moveq(null);    // remove so that not prevailing span for docpopup when not active
	}

	doc.markDirtySubtree(true);     // done by span if using span, if use move() -- moveq() doesn't
	doc.repaint();

	return super.semanticEventAfter(se,msg);
  }
}
