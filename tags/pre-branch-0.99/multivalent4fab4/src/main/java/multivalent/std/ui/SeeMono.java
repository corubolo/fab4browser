package multivalent.std.ui;

import multivalent.*;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;
import multivalent.std.span.FamilySpan;



/**
	Usually better to see ASCII in proportionally spaced fonts,
	but sometimes have ASCII art -- though some media adaptors don't preserve spaces.
	Maybe setting a preference.

	@version $Revision: 1.2 $ $Date: 2002/02/01 06:15:39 $
*/
public class SeeMono extends Behavior {
  /**
	Toggle mono/proportional.
	<p><tt>"seeMono"</tt>.
  */
  public final String MSG_MONO = "seeMono";


  FamilySpan monoSpan=null;



  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se, msg)) return true;    // respect superclass short-circuit
	else if (VMenu.MSG_CREATE_VIEW==msg) {
		VCheckbox check = (VCheckbox)createUI("checkbox", "See Text in Fixed-width Font", "event "+MSG_MONO, (INode)se.getOut(), null, false);
		check.setState(monoSpan!=null);
	}

	return false;
  }

  /** Choose between short and fielded displays. */
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	if (MSG_MONO==msg) {
		if (monoSpan!=null) {
			monoSpan.destroy();
			monoSpan=null;

		} else {
			Document doc = getDocument();

			monoSpan = (FamilySpan)Behavior.getInstance("mono", "multivalent.std.span.FamilySpan", null, doc.getLayer(Layer.SCRATCH));
			monoSpan.setFamily("monospace");

			Leaf lastl=doc.getLastLeaf();
			monoSpan.move(doc.getFirstLeaf(),0, lastl,lastl.size());
		}
	}

	return super.semanticEventAfter(se, msg);
  }
}
