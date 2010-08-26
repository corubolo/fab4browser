package multivalent.std.ui;

import java.util.ArrayList;
import java.util.List;

import multivalent.*;
import multivalent.std.span.FamilySpan;



/**
	Set selected text to given font face.

	@version $Revision: 1.2 $ $Date: 2002/02/01 07:29:46 $
*/
public class FontFaceMenu extends Behavior {
  /**
	Set selected text to given font face.
	<p><tt>"setFontFace"</tt>: <tt>arg=</tt> {@link java.lang.String} <var>name-of-font-face</var>.
  */
  public static final String MSG_SETFACE = "setFontFace";

  /**
	Construct Font Face menu by passing around to behaviors and letting them add (or delete) entiries.
	<p><tt>"createWidget/FontFace"</tt>: <tt>out=</tt> {@link multivalent.gui.VMenu} <var>instance-under-construction</var>.
  */
  public static final String MSG_CREATE_FONTFACE = "createWidget/FontFace";



  static final int MAX_RECENT = 5;

  List<String> seen_ = new ArrayList<String>(MAX_RECENT);



  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	Browser br = getBrowser();
	if (super.semanticEventBefore(se,msg)) return true;
	else if (MSG_CREATE_FONTFACE==msg) {
		INode menu = (INode)se.getOut();
		boolean fdisable = !br.getSelectionSpan().isSet();

		// recently used
		if (seen_.size() > 0) {
			for (int i=seen_.size()-1; i>=0; i--) {
				String fam = seen_.get(i);
				createUI("button", fam, new SemanticEvent(br, MSG_SETFACE, fam), menu, null, fdisable);
			}
			createUI("separator", null, null, menu, null, fdisable);
		}

//System.out.println(allfam.length+ "fonts");
		String[] families = com.pt.awt.font.NFontManager.getDefault().getAvailableFamilies();
		for (int i=0,imax=families.length; i<imax; i++) {
			String fam = families[i];

			/* filter out TeX fonts, which aren't general purpose => don't appear in general list
			if (fam.startsWith("cm") || fam.startsWith("eu") || fam.startsWith("l") || fam.startsWith("ms")) {
				if (Character.isDigit(fam.charAt(fam.length()-1))) continue;
			}*/

			// how to distinguish dingbats from text?
			//String title = "<font face='"+fam+"'>"+fam+"</font>"; => way too time consuming
			String title = fam;
			createUI("button", title, new SemanticEvent(br, MSG_SETFACE, fam), menu, null, fdisable);
		}
	}
	return false;
  }


  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_SETFACE==msg) {
		Browser br = getBrowser();
		Span sel = br.getSelectionSpan();
		Object o = se.getArg();
		if (sel.isSet() && o instanceof String) {
			String family = (String)o;
			seen_.remove(family); if (seen_.size()>=MAX_RECENT) seen_.remove(0); seen_.add(family);    // LRU

			//Document doc = sel.getStart().node.getDocument();     // better?
			Document doc = br.getCurDocument();
			Layer personalLayer = doc.getLayer(Layer.PERSONAL);
			FamilySpan span = (FamilySpan)Behavior.getInstance("face","multivalent.std.span.FamilySpan",null, personalLayer);
			span.setFamily((String)o);
			span.move(sel);
			sel.move(null);
		}
	}
	return super.semanticEventAfter(se,msg);
  }
}
