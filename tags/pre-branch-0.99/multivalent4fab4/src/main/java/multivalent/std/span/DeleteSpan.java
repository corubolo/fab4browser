package multivalent.std.span;

import multivalent.*;


/**
	Executable copy editor mark to suggest span should be deleted.

	@version $Revision: 1.2 $ $Date: 2002/02/02 13:16:26 $
*/
public class DeleteSpan extends ActionSpan {
  public boolean action() {
	Leaf startn=getStart().leaf, endn=getEnd().leaf;
	int startoff=getStart().offset, endoff=getEnd().offset;
	destroy();	// remove first as may delete nodes (though this doesn't cause a probably now as memory isn't freed and path up parents still ok)

	startn.delete(startoff, endn,endoff);

//	  startn.markDirtyTo(endn); -- no such method?
//	  repaint();
	return true;
  }

  public boolean appearance(Context cx, boolean all) { cx.overstrike = getLayer().getAnnoColor();  return false; }
}
