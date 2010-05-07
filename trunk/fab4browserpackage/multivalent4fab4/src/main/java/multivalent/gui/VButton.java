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

package multivalent.gui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Map;

import multivalent.Browser;
import multivalent.Context;
import multivalent.EventListener;
import multivalent.INode;
import multivalent.Node;
import multivalent.TreeEvent;
import multivalent.node.IParaBox;
import multivalent.std.VScript;

/**
 * Button widget: click to invoke the associated script (as given by SCRIPT
 * attribute). Usable as button and menu item. <!-- for comparison: AWT button
 * Public Methods X addActionListener(ActionListener l) Adds the specified
 * action listener to receive action events from this button. SCRIPT
 * getActionCommand() Returns the command name of the action event fired by this
 * button. SCRIPT removeActionListener(ActionListener l) Removes the specified
 * action listener so that it no longer receives action events from this button.
 * SCRIPT setActionCommand(String command) Sets the command name for the action
 * event fired by this button. -->
 * 
 * @version $Revision$ $Date$
 */
public class VButton extends IParaBox implements EventListener {
	/** Cursor inside button, so that if release, execute. */
	protected boolean inside_ = false;
	protected static final Color gray = new Color(230, 230, 230);
	protected static final Color darkGray = new Color(200, 200, 200);

	public VButton(String name, Map<String, Object> attr, INode parent) {
		this(name, attr, parent, null);
	}

	public VButton(String name, Map<String, Object> attr, INode parent,
			String script) {
		super(name, attr, parent);
		if (script != null)
			putAttr("script", script);
	}

	@Override
	public boolean formatNode(int width, int height, Context cx) {
		if (!(parent_ instanceof VMenu))
			padding = new Insets(padding.top + 1, padding.left + 4,
					padding.bottom + 2, padding.right + 5);
		super.formatNode(width/*
							 * Integer.MAX_VALUE--embedded table needs non-probe
							 * width
							 */, 0/* 0 to fake out HTML BODY. height */, cx);
		// tree nodes should have border built in, handled by
		// Node.formatBeforeAfter()/paint()
		// bbox.setSize(bbox.width, bbox.height);
		baseline = bbox.height;// +
								// border.top+border.bottom+padding.top+padding.
								// bottom; //
		// not baseline of children, as box in line should not descend
		// below baseline

		valid_ = true;
		return false;
	}

	@Override
	public boolean breakBefore() {
		return false;
	}

	@Override
	public boolean breakAfter() {
		return false;
	}

	@Override
	public int dx() {
		return super.dx() + (inside_ ? 1 : 0);
	}

	@Override
	public int dy() {
		return super.dy() + (inside_ ? 1 : 0);
	}

	@Override
	public void paintNode(Rectangle docclip, Context cx) {
		if (!(getParentNode() instanceof VMenu) && !(this instanceof VRadiobox)) { // =
																					// >
																					// nested
																					// selector
																					// in
																					// stylesheet
			Graphics2D g = cx.g;
			g.setColor(gray);// !inside_
			// g.setColor(cx.foreground);//!inside_
			g.fill3DRect(0 - border.left - padding.left, 0 - border.top
					- padding.top, bbox.width - 1, bbox.height - 1, !inside_);
			/*
			 * if (inside_)
			 * g.drawRoundRect(1-border.left-padding.left,1-border.top
			 * -padding.top, bbox.width-2,bbox.height-2,8,8); else
			 * g.drawRoundRect
			 * (1-border.left-padding.left,1-border.top-padding.top,
			 * bbox.width-2,bbox.height-2,1,1);
			 */
		}
		super.paintNode(docclip, cx);

	}

	@Override
	public boolean eventNode(AWTEvent e, Point rel) {
		int eid = e.getID();
		Browser br = getBrowser();

		if (eid == MouseEvent.MOUSE_PRESSED) {
			br.setGrab(this);
			inside_ = true;
			repaint();
		}

		// return super.eventNode(e, rel); -- NO, opaque to children: function
		// as a leaf
		br.setCurNode(this, -1); // but take this from super.eventNode(e,rel)
		//System.out.println("VButton "+getFirstLeaf().getName()+(eid!=TreeEvent
		// .FIND_NODE));
		// return (eid!=TreeEvent.FIND_NODE); // opaque to content
		return false;
		// return true;
	}

	public void event(AWTEvent e) {
		int eid = e.getID();
		Browser br = getBrowser();

		if (eid == MouseEvent.MOUSE_DRAGGED) {
			// could just compute location of button in screen cordinates in
			// eventNode() and
			// just compare bboxes here
			// => don't do it that way because may be inside lens

			br.getRoot().eventBeforeAfter(
					new TreeEvent(this, TreeEvent.FIND_NODE), br.getCurScrn()); // bypass
																				// grabs
																				// on
																				// Browser
			Node now = br.getCurNode();

			if (now != null && contains(now) != inside_) {
				inside_ = !inside_;
				repaint();
			}

		} else if (eid == MouseEvent.MOUSE_RELEASED) {
			br.releaseGrab(this);

			if (inside_)
				invoke();
			inside_ = false;

			repaint(); // redraw after performed associated command
		}
	}

	/**
	 * Execute associated VScript, if any. VCheckbox and VRadiobox override to
	 * change state first.
	 */
	public void invoke() {
		//System.out.println(getName()+" invoking SCRIPT = "+getAttr(ATTR_SCRIPT
		// )); //+",
		// listeners="+listeners_);
		VScript.eval(getValue(ATTR_SCRIPT), getDocument(), getAttributes(),
				this);
	}
}
