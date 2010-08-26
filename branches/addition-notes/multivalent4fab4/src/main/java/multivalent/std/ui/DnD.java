/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent.std.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.Layer;



/**
	Drag and drop URLs into Browser window to open page on that URL.

	@version $Revision$ $Date$
*/
public class DnD extends Behavior implements DropTargetListener {
/*  static /*final* / DataFlavor DATAFLAVOR_URL = null;
  static {
	try {
		DATAFLAVOR_URL = new DataFlavor("application/x-java-url; class = java.net.URL");    // java.net.URL.class, "URL");
	} catch (Exception e) { System.err.println("shouldn't happen "+e); }
  }
*/
  DropTarget dt_ = null;


  public void dragEnter(DropTargetDragEvent dtde) {
//System.out.println("DnD enter");
  }
  public void dragOver(DropTargetDragEvent dtde) {
//System.out.println("DnD over");
  }
  public void dropActionChanged(DropTargetDragEvent dtde) {
//System.out.println("DnD action");
  }
  public void dragExit(DropTargetEvent dte) {
//System.out.println("DnD exit");
  }

  /** Translate DnD event into Document.MSG_OPEN, <txt>. */
  public void drop(DropTargetDropEvent dtde) {
	dtde.acceptDrop(DnDConstants.ACTION_LINK);

	Transferable tr = dtde.getTransferable();
	DataFlavor[] df = tr.getTransferDataFlavors();
	boolean list = false;
	for (int i=0,imax=df.length; i<imax; i++) {
		if (df[i].isFlavorJavaFileListType())
			list=true;
		//System.out.println(df[i]);
	}

//System.out.println("DnD drop");
	try {
		Browser br = getBrowser();
		//Object o = tr.getTransferData(DATAFLAVOR_URL);
		if (list) {
			List<File> lf = (List<File>)tr.getTransferData(DataFlavor.javaFileListFlavor);
			br.eventq(Document.MSG_OPEN, lf.get(0).toURI().toString());
			dtde.dropComplete(true);
		}else {
		Object o = tr.getTransferData(DataFlavor.stringFlavor);
		System.out.println("data = "+o);
		br.eventq(Document.MSG_OPEN, o);
		dtde.dropComplete(true);
		}
	} catch (IOException ioe) {
		dtde.dropComplete(false);
//System.out.println(ioe);
	} catch (UnsupportedFlavorException ufe) {
		dtde.dropComplete(false);
//System.out.println(ufe);
	}
  }




  @Override
public void restore(ESISNode n, Map<String,Object> attr, Layer layer) {
	super.restore(n, attr, layer);

	// activate on Browser
	Browser br = getBrowser();
	dt_ = new DropTarget(br, DnDConstants.ACTION_LINK, this);
	// other actions for ACTION_MOVE / ACTION_COPY ?
  }

  @Override
public void destroy() {
	super.destroy();
	// deactive from Browser
	dt_.setComponent(null); // ?
  }
}
