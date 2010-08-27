package multivalent.std.ui;

import java.awt.datatransfer.*;

import multivalent.*;



/**
	Standard File operations: Undo, Redo, Cut, Copy, Paste, Clear, Select All.

	@version $Revision$ $Date$
*/
public class StandardFile extends Behavior {
  /**
	Cut selected text.
	<p><tt>"fileCut"</tt>.
  public static final String MSG_CUT = "fileCut";
  */

  /**
	Copy selected text.
	<p><tt>"fileCopy"</tt>.
  public static final String MSG_COPY = "fileCopy";
  */

  /**
	Paste clipboard at cursor.
	<p><tt>"filePaste"</tt>.
  public static final String MSG_PASTE = "filePaste";
  */

  /**
	Clear (delete) selected text.
	<p><tt>"fileQuit"</tt>.
  public static final String MSG_CLEAR = "fileQuit";
  */


  public static final String MENU_CATEGORY_OPEN = "open";

  public static final String MENU_CATEGORY_SAVE = "save";

  public static final String MENU_CATEGORY_QUIT = "quit";


  /** On {@link VMenu#MSG_CREATE_EDIT}, add cut/copy/paste/... menu items.
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_EDIT==msg) {
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();
		Span sel = br.getSelectionSpan();
		CursorMark curs = br.getCursorMark();

		createUI("button", "Cut", "event "+MSG_CUT, menu, "file", !sel.isSet());
		createUI("button", "Copy", "event "+MSG_COPY, menu, "file", !sel.isSet());
		createUI("button", "Paste", "event "+MSG_PASTE, menu, "file", !curs.isSet());
		createUI("button", "Clear", "event "+MSG_CLEAR, menu, "file", !sel.isSet());
		createUI("button", "Select All", "event "+MSG_SELECT_ALL, menu, "file", false);
		//"Undo", "Redo",

	}
	return false;
  }*/


  /** Implement MSG_CUT, MSG_COPY, ....
  public boolean semanticEventAfter(SemanticEvent se, String msg) {
	Browser br = getBrowser();
	Span sel = br.getSelectionSpan();
	Leaf startn=null,endn=null;
	int startoff=-1, endoff=-1;
	if (sel.isSet()) {
		startn=(Leaf)sel.getStart().leaf; endn=(Leaf)sel.getEnd().leaf;
		startoff=sel.getStart().offset; endoff=sel.getEnd().offset;
	}


	if (MSG_CUT==msg) {
		if (sel.isSet()) startn.cut(startoff, endn,endoff);

	} else if (MSG_COPY==msg) copy(sel);

	else if (MSG_PASTE==msg) paste();

	else if (MSG_CLEAR==msg) {}

	else if (MSG_SELECT_ALL==msg) {
		Document doc = br.getCurDocument();
		Leaf first=doc.getFirstLeaf(), last=doc.getLastLeaf();
		br.getSelectionSpan().move(first,0, last,last.size());
		//br.repaint(); -- done by moving span
		//br.clipboard();
//System.out.println("select "+first+".."+last);
	}
	return super.semanticEventAfter(se,msg);
  }
*/
}
