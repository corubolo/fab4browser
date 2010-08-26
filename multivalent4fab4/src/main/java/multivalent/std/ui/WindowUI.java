package multivalent.std.ui;

import java.util.Map;

import multivalent.*;


/**
	Creates new window or lens.
	Attributes: <tt>winclass</tt> Java class of window, <tt>menu</tt> for UI (defaults to "Lens").
	=> rename to BehaviorUI

	@see multivalent.std.ui.SpanUI for a similar behavior for Spans.

	@version $Revision: 1.2 $ $Date: 2002/02/01 04:38:33 $
*/
public class WindowUI extends Behavior {
  /**
	Another semantic command, which should be given more descriptive name.
	<p><tt>"newVWindow"</tt>: <tt>arg=</tt> {@link WindowUI} <var>this</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
  */
  public static final String MSG_NEW = "newVWindow";


  String triggerMsg_ = null;


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);
	triggerMsg_ = ("createWidget/"+(getAttr("menu", "Lens"))).intern();
  }

  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	String title=getAttr("title");
	if (super.semanticEventBefore(se,msg)) return true;
	else if (triggerMsg_==msg && title!=null) {
		INode menu = (INode)se.getOut();
		SemanticEvent nse = new SemanticEvent(getBrowser(), MSG_NEW, this);  // rather than "event newVWindow "+winclass so can use == below, and rather than ...+winclass because may have several instances all creating SignalVWindow'es
		createUI("button", title, nse, menu, "Tool", false);
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Object arg = se.getArg();
	String winclass=getAttr("winclass");
	if (MSG_NEW==msg && this==arg && winclass!=null) {
		Map<String,Object> attrs = (Map)CHashMap.getInstance(getAttr("attrs"));
		Browser br = getBrowser();
		Layer layer = (getAttr("doc")==null? br.getRoot(): br.getCurDocument()).getLayer(getAttr("layer",Layer.SCRATCH));
//System.out.println("system layer behaviors\n\t");
//		for (Iterator<> i=systemLayer.behaviorIterator(); i.hasNext(); ) System.out.println(((Behavior)i.next()).getName()+" ");
		Behavior.getInstance(winclass/*for now -- getName() not right*/,winclass,attrs, layer);
		//if (win.getTitle()==null) win.setTitle(getAttr("title"));    // share TITLE attribute with target VFrame, if it doesn't have one
		//if (lastbbox_==null) lastbbox_=win.getBounds(); else { win.setLocation(lastbbox_.x,lastbbox_.y); win.setSize(lastbbox_.width,lastbbox_.height); }
		//getBrowser().eventq(VFrame.SHOWEVENT, win); -- part of VFrame restore()
	}
//if (VFrame.SHOWEVENT==msg && this==arg) System.out.println("SHOW on "+winclass);
	return super.semanticEventAfter(se,msg);
  }
}
