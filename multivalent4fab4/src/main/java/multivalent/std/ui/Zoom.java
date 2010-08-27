package multivalent.std.ui;

import java.util.Map;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import multivalent.*;
import multivalent.gui.VRadiogroup;
import multivalent.gui.VRadiobox;
import multivalent.gui.VMenuButton;
import multivalent.gui.VMenu;

import phelps.lang.Integers;



/**
	Zoom controls.

	@version $Revision: 1.6 $ $Date: 2005/05/06 10:47:07 $
*/
public class Zoom extends Behavior {
  /**
	Reports current zoom factor.
	<p><tt>"getZoom"</tt>
	Current zoom is returned as {@link java.lang.Float} in event argument.
  */
  public static final String MSG_GET = "zoom:get";

  /**
	Sets zoom factor from <var>arg</var>.
	<p><tt>"setZoom"</tt>: <tt>arg=</tt> {@link java.lang.Float} or {@link java.lang.String} <var>zoom-factor</var>, where 1.0 is 100%
  */
  public static final String MSG_SET = "zoom:set";


  public static final String ARG_BIGGER = "Bigger";

  public static final String ARG_SMALLER = "Smaller";

  /** Argument to {@link #MSG_SET} to zoom page to width of window<!--, <code>Fit Width</code>-->. */
  public static final String ARG_FIT_WIDTH = "Fit_Width";

  /** Argument to {@link #MSG_SET} to set zoom to fit entire page. */
  public static final String ARG_FIT_PAGE = "Fit_Page";


  /**
	Construct Zoom menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/Zoom"</tt>: <tt>out=</tt> {@link VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_ZOOM = "createWidget/Zoom";


  /** Attribute in hub for list of zoom factors. */
  public static final String ATTR_ZOOMS = "zooms";


  private Matcher FACTOR = Pattern.compile("([0-9.]+)").matcher("");
  private Matcher PERCENTAGE = Pattern.compile("([0-9.]+)\\s*%").matcher("");
  /*private Matcher BIGGER = Pattern.compile("(?i)\\+|bigger|larger").matcher("");
  private Matcher SMALLER = Pattern.compile("(?i)\\+|smaller").matcher("");*/
  private String[] zooms_ = null;
  private VRadiogroup rg_ = new VRadiogroup();


  /** Returns zoom for current document. */
  private float getCurZoom() {
	return getBrowser().getCurDocument().getMediaAdaptor().getZoom();
  }

  private float parseZoom(String str, boolean direct) {
	float z0 = getCurZoom(), z = 0f;
	if (str==null) {}
	else if (FACTOR.reset(str).find()) {
		try { z = Float.parseFloat(FACTOR.group(0)) / 100f; } catch (NumberFormatException nfe) {}

	} else if (PERCENTAGE.reset(str).find()) {
		try { z = Float.parseFloat(PERCENTAGE.group(0)); } catch (NumberFormatException nfe) {}

	} else if (direct) {


	} else if (ARG_FIT_WIDTH.equals(str)) {
		Document doc = getBrowser().getCurDocument();
		z = z0 * doc.bbox.width / doc.childAt(0).bbox.width - 0.01f/* -1% to be safely inside*/;

	} else if (ARG_FIT_PAGE.equals(str)) {
		Document doc = getBrowser().getCurDocument();
		float w = z0 * doc.bbox.width / doc.childAt(0).bbox.width - 0.01f;
		float h = z0 * doc.bbox.height / doc.childAt(0).bbox.height - 0.01f;
		z = Math.min(w,h);

	} else if (ARG_BIGGER.equals(str) || ARG_SMALLER.equals(str)) {
		// find closest existing
		int inx = -1; float diff = Integer.MAX_VALUE;
		for (int i=0,imax=zooms_.length; i<imax; i++) {
			try {
				float d = parseZoom(zooms_[i], true) - z0;
				if (Math.abs(d) < Math.abs(diff)) { inx=i; diff=d; }
			} catch (NumberFormatException nfe) {}
		}
//System.out.println("bs vis-a-vis "+zooms_[inx]);

		String s;
		if (ARG_BIGGER.equals(str)) {
			s = diff>=0 && inx+1<zooms_.length? zooms_[inx+1]: diff<0? zooms_[inx]: null;
		} else { assert ARG_SMALLER.equals(str);
			s = diff<=0 && inx>0? zooms_[inx-1]: diff>0? zooms_[inx]: null;
		}
		z = parseZoom(s, true);
	}

//System.out.println("\tzoom "+str+" = "+z);
	return z;
  }


  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		VMenuButton mb = (VMenuButton)createUI("menubutton", "Zoom", "event "+MSG_CREATE_ZOOM, (INode)se.getOut(), "View", false);
		mb.setDynamic("Zoom");

	} else if (MSG_CREATE_ZOOM==msg) {
		rg_.clear();
		float z0 = getCurZoom();
		INode menu = (INode)se.getOut();
		for (String zoom: zooms_) {
			float z = parseZoom(zoom, true);
			Node ui = createUI((z!=0f? "radiobox": "button"), zoom, "event "+MSG_SET+" "+zoom, menu, "Zoom", false);
			if (z!=0f) {
				VRadiobox radio = (VRadiobox)ui;
				radio.setRadiogroup(rg_);
				if (rg_.getActive()==null &&  Math.abs(z - z0) < 0.02f) rg_.setActive(radio);
			}
		}
//System.out.println("zoom now "+z0);
		//if (rg_.getActive()==null) ... add separator and menu element with that setting
	}

	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
//System.out.println(msg+" => "+se.getArg());
	if (MSG_GET==msg) {
		se.setArg(getCurZoom());

	} else if (MSG_SET==msg) {
		float z0 = getCurZoom(), z = z0;
		Document doc = getBrowser().getCurDocument();

		Object arg = se.getArg();
//System.out.println("zoom, now "+z0+", "+" => "+bbox.width);
		if (arg instanceof String) z = parseZoom((String)arg, false);
		else if (arg instanceof Number) z = ((Number)arg).floatValue();

		if (z != z0) {
//System.out.println("zoom "+z0+" => "+z);
			doc.getMediaAdaptor().setZoom(z);

			if (getCurZoom() != z0) {	// new zoom accepted by media adaptor
				String genre = doc.getAttr(Document.ATTR_GENRE);
				if (genre!=null) Multivalent.getInstance().putPreference(genre+"-zoom", Float.toString(z));
				//br.eventq(Document.MSG_RELOAD, null);
				//br.eventq(Multipage.MSG_OPENPAGE, getDocument().getAttr(Document.PAGE));
				getBrowser().eventq(doc.getAttr(Document.ATTR_PAGE)==null? Document.MSG_RELOAD: Multipage.MSG_RELOADPAGE, null);
			}
		}

	}

	return super.semanticEventAfter(se,msg);
  }


  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr, layer);

	zooms_ = getAttr(ATTR_ZOOMS, ARG_BIGGER+","+ARG_SMALLER+",50%,75%,90%,100%,110%,125%,150%,175%,200%,400%,"+ARG_FIT_WIDTH+","+ARG_FIT_PAGE).split("\\s*,\\s*");
  }
}
