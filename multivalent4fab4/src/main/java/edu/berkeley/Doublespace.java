package edu.berkeley;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Document;
import multivalent.INode;
import multivalent.node.Root;



/**
	BROKEN -- replaced by fine-grained reformatting.
	Doublespace text in entire document.
	Sets spaceabove on root structural context.

	LATER: three kinds of spacing: absolute, additive, max of settings.

	@version $Revision$ $Date$
 */
public class Doublespace extends Behavior {
	private int spacing_=0;
	private int active_;


	/**************************
	 * PROTOCOLS
	 **************************/

	public Doublespace() { active_ = 0; }

	static String spaceTitle[] = {
		"Singlespace", "Space and a half", "Doublespace", "Triplespace"
	};
	/*
  public int countItems() { return spaceTitle.length; }
  public String getCategory(int id) { return "View"; }
  public Object getTitle(int id) { return spaceTitle[id]; }
  public int getType(int id) { return Valence.RADIOBOX; }
  public String getStatusHelp(int id) { return "Set linespaceing"; }
	 */
	public void setActive(int id) {
		Browser br = getBrowser();
		active_=id;
		//	  if (xdoc!=null) xdoc.aswordscnt += (active_==-1? -1: 1);

		spacing_=0;

		switch (id) {
		case 0: spacing_=0; break;
		case 1: spacing_=6; break;
		case 2: spacing_=12; break;
		case 3: spacing_=24; break;
		default: assert false: id;
		}
		Root root = br.getRoot();
		space(root);
		root.markDirtySubtree(false);
		br.repaint();
	}


	// not updated on a rebuild
	void space(INode root) {
		getBrowser();
	}

	@Override
	public void buildAfter(Document doc) {
		space(doc);
	}


	/*
	public boolean checkDependencies(Vector vbe) {
	  // find Xdoc behavior, if any, for ugly hack
		for (int i=0; i<vbe.size(); i++) {
			if (vbe.elementAt(i) instanceof Xdoc) { xdoc=(Xdoc)vbe.elementAt(i); break; }
		}
		return true;
	}
	 */
}
