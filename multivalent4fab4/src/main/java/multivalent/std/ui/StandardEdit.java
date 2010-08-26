package multivalent.std.ui;

import java.awt.datatransfer.*;
import java.awt.Toolkit;
import java.io.IOException;

import multivalent.*;
import multivalent.node.LeafText;
import multivalent.gui.VMenu;



/**
	Standard Edit operations: Undo, Redo, Cut, Copy, Paste, Clear, Select All.

	@version $Revision: 1.3 $ $Date: 2002/06/25 00:26:25 $
*/
public class StandardEdit extends Behavior {
  /**
	Cut selected text.
	<p><tt>"editCut"</tt>.
  */
  public static final String MSG_CUT = "editCut";

  /**
	Copy selected text.
	<p><tt>"editCopy"</tt>.
  */
  public static final String MSG_COPY = "editCopy";

  /**
	Paste clipboard at cursor.
	<p><tt>"editPaste"</tt>.
  */
  public static final String MSG_PASTE = "editPaste";

  /**
	Clear (delete) selected text.
	<p><tt>"editClear"</tt>.
  */
  public static final String MSG_CLEAR = "editClear";

  /**
	Reverse last editing operation.
	<p><tt>"editUndo"</tt>.
  */
  public static final String MSG_UNDO = "editUndo";

  /**
	Redo last editing operation.
	<p><tt>"editRedo"</tt>.
  */
  public static final String MSG_REDO = "editRedo";

  /**
	Select all text.
	<p><tt>"editSelectAll"</tt>.
  */
  public static final String MSG_SELECT_ALL = "editSelectAll";


  public static final String MENU_CATEGORY_EDIT = "edit";



  /** On {@link VMenu#MSG_CREATE_EDIT}, add cut/copy/paste/... menu items. */
  public boolean semanticEventBefore(SemanticEvent se, String msg) {
	if (super.semanticEventBefore(se,msg)) return true;
	else if (VMenu.MSG_CREATE_EDIT==msg) {
		INode menu = (INode)se.getOut();
		Browser br = getBrowser();
		Span sel = br.getSelectionSpan();
		CursorMark curs = br.getCursorMark();

		createUI("button", "Cut", "event "+MSG_CUT, menu, MENU_CATEGORY_EDIT, /*!isEditable || */ !sel.isSet());
		createUI("button", "Copy", "event "+MSG_COPY, menu, MENU_CATEGORY_EDIT, !sel.isSet());
		createUI("button", "Paste", "event "+MSG_PASTE, menu, MENU_CATEGORY_EDIT, !curs.isSet());
		createUI("button", "Clear", "event "+MSG_CLEAR, menu, MENU_CATEGORY_EDIT, /*!isEditable ||*/ !sel.isSet());
		createUI("button", "Select All", "event "+MSG_SELECT_ALL, menu, MENU_CATEGORY_EDIT, false);
		//"Undo", "Redo",

	}
	return false;
  }


  /** Implement MSG_CUT, MSG_COPY, .... */
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

  /** Copy all text in span to clipboard. */
  /*pubic -- use sem ev*/void copy(Span span) {
	assert span!=null;
	if (!span.isSet()) return;

//	startn.copy(startoff, endn,endoff);
	Browser br = getBrowser();
	String txt = br.clipboard();
	if (txt.length() > 0) {
		StringSelection ss = new StringSelection(txt);
		br.getToolkit().getSystemClipboard().setContents(ss, ss);
	}
  }

  void paste() {
//	if (sel.isSet()) startn.paste(startoff);
	Browser br = getBrowser();
	INode scope = br.getScope();  if (scope==null) return;

	Span sel = br.getSelectionSpan();
	CursorMark curs = br.getCursorMark();
	Mark m = null;
	Leaf oselend=sel.getEnd().leaf; int oseloff=sel.getEnd().offset;     // save end because selection stretches to cover insertions at end
	if (sel.isSet()) m=sel.getEnd(); else if (curs.isSet()) m=curs.getMark();
	if (m==null) return;

	// insert at cursor / end of selection
System.out.println("PASTE into "+m.leaf.getName()+'/'+m.offset);
	Transferable xfer = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
System.out.println("Transferable = "+xfer);
	if (xfer!=null && xfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
		try {
			String txt = (String)xfer.getTransferData(DataFlavor.stringFlavor);
System.out.println("txt = "+txt);
			if (m.leaf instanceof LeafText) {
				Leaf l = m.leaf;
				l.insert(m.offset, txt, scope);
				curs.move(l,m.offset);
			}
		} catch (UnsupportedFlavorException ufe) { System.err.println(ufe);
		} catch (IOException ioe) { System.err.println(ioe); }  // really?
	}

	// delete selection (delete may zap node, so do insert first)
	if (sel.isSet()) (sel.getStart().leaf).delete(sel.getStart().offset, oselend,oseloff);
  }
}
