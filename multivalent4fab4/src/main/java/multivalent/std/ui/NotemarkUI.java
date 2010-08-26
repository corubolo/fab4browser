package multivalent.std.ui;

import java.util.StringTokenizer;
import java.util.Map;

import multivalent.*;
import multivalent.gui.VCheckbox;

import multivalent.std.VScript;



/**
	<i>Broken</i>
	AttrUI + style sheet hacking to make Notemarks

<!--
old

	Creates Notemark control checkbox in View menu.
	Attributes: TITLE=<i>String</i> to show,
	TAG=<i>String</i> attribute to identify span name to tweak in style sheet.
	taking SIGNAL=<i>String</i> attribute for the semantic event message that invokes Notemark (re)computation,
	and CONTEXT=<i>integer</i> for the number of words on each side of target span to show.

	<p>NB: If CONTEXT is 0 (its default), the visibility bit for the target span itself
	is set in the style sheet.	Otherwise, this behavior will search the document for spans
	of the given name and add shadow Notemark spans.
-->

	@see multivalent.std.span.Notemark

	@version $Revision$ $Date$
*/
public class NotemarkUI extends AttrUI {
  public static final String ATTR_SPANNAMES = "spannames";


  String spannames_ = null;

  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (variable_==null || spannames_==null) {
		// nothing
	} else if (Document.MSG_OPENED==msg || (AttrUI.MSG_TOGGLE_VAR==msg && variable_.equals(se.getArg()))) {
//System.out.println("NotemarkUI on "+variable_);
		Browser br = getBrowser();
		Document doc = br.getCurDocument();
		if (doc==null) return false; // BOGUS!
		boolean val = VScript.getBoolean("$"+variable_, doc, attr_, seed_);
//System.out.println("on/off = "+val+", var="+variable_+", attr_="+attr_+", seed_="+seed_);
		/*
		Multivalent control = getControl();
		boolean val = control.getPreferenceBoolean(variable_, seed_);*/

		// tweak style sheet... later add spans according to context
		StyleSheet ss = doc.getStyleSheet();

		for (StringTokenizer st=new StringTokenizer(spannames_, ","); st.hasMoreTokens(); ) {
			String sn = st.nextToken().trim(); if (sn.length()==0) continue;
			Object o = ss.get(sn);
			CLGeneral gs=null;
			if (o==null) { gs=new CLGeneral(); ss.put(sn, gs); }
			else if (o instanceof CLGeneral) gs=(CLGeneral)o;

			if (gs!=null) {
				gs.setElide(val? Context.BOOL_FALSE: Context.BOOL_INVALID);
//System.out.println("setting style sheet "+sn+" elide = "+val);
			}
		}

		// happens before document format?
		//doc.markDirtySubtree(true);
		//doc.repaint();
	}
	return super.semanticEventAfter(se, msg);
  }

  // old
	/*
  protected List<> nblist = new ArrayList<>(20);   // so can quickly remove them when need to recompute
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (msg.startsWith(VMenu.MSG_CREATE_VIEW)) {
		tag=getAttr("tag"); signal=getAttr("signal");
		context = Integers.parseInt(getAttr("context"), 0);	// has a default
		if (tag!=null && signal!=null) {
			mysignal = tag+"Notemark";	// set even if ui creation fails, as may be other ways of calling
			VCheckbox ui = (VCheckbox)createUI("checkbox", getAttr("title"), null, (INode)se.getOut(), getAttr("notemark"), false);
			if (ui!=null) ui.setState(active_);
		} else {
			if (tag==null) System.err.println("NotemarkUI: missing TAG attribute");
			if (signal==null) System.err.println("NotemarkUI: missing SIGNAL attribute");
		}
	} else if (mysignal==null) {
		// nothing -- not initialized or bad attributes
	} else if (mysignal==msg) {
		// for now assume toggle
		active_ = !active_;
	} else if (signal==msg) {
		// compute Notemarks
	}
	return false;
  }
	*/

  public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n,attr,layer);
	spannames_ = getAttr(ATTR_SPANNAMES);
  }
}
