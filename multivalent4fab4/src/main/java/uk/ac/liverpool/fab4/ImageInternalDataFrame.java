/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License,
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 * 
 *******************************************************************************/

package uk.ac.liverpool.fab4;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import multivalent.Behavior;
import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.SemanticEvent;
import multivalent.StyleSheet;
import multivalent.gui.VFrame;
import multivalent.gui.VScrollbar;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.LeafUnicode;
import multivalent.std.span.HyperlinkSpan;
import phelps.awt.Colors;

import com.pt.awt.NFont;

/**
 * An internal frane used to display image metadata.
 * 
 * 
 * @author fabio
 * 
 */
public class ImageInternalDataFrame extends Behavior {

	private static final String ATTR_TITLE = "title_attr";

	public static NFont FONT_TEXT = NFont.getInstance("Sans",
			NFont.WEIGHT_NORMAL, NFont.FLAG_SANSSERIF, 12);

	protected VFrame win_ = null;
	protected Document doc_ = null;

	private String title = "Hello";

	private Color bgcol = Color.black;

	private CLGeneral gs;

	public VFrame getFrame() {
		return win_;
	}

	/** Recurse to nested document. */
	@Override
	public void buildBefore(Document doc) {
		super.buildBefore(doc);
		doc_.getLayers().buildBefore(doc_);
	}

	/** Recurse to nested document. */
	@Override
	public void buildAfter(Document doc) {
		super.buildAfter(doc);
		doc_.getLayers().buildAfter(doc_);
	}

	@Override
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		if (VFrame.MSG_CLOSED == msg)
			if (se.getArg() == win_) {
				// System.out.println("closed");
				Document doc = (Document) getBrowser().getRoot().findBFS(
				"content");
				Layer sc = doc.getLayer(Layer.SCRATCH);
				sc.removeBehavior(this);
			}
		// System.out.println(msg);
		if (super.semanticEventBefore(se, msg))
			return true;
		return false;
	}

	public void setTransparent(boolean t) {
		win_.setTransparent(t);
	}

	public void setTitleBg(Color c) {
		if (win_ != null) {
			win_.setTitleBg(c);
			win_.repaint();
		}
	}

	public Color getTitleBg() {
		if (win_ != null)
			return win_.getTitleBg();
		return null;
	}

	@Override
	public void destroy() {
		if (win_ != null)
			win_.close();
		win_ = null;
		super.destroy();
	}

	public void setTitle(String s) {
		title = s;
		if (win_ != null)
			win_.setTitle(title);
	}

	public void setBounds (int x, int y, int w, int h) {
		win_.setBounds(x,y,w,h);
	}

	@Override
	public void restore(ESISNode n, Map<String, Object> attr, Layer layer) {
		super.restore(n, attr, layer);
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		win_ = new VFrame("Note", null, doc);
		win_.setShrinkTitle(true);
		win_.setTransparent(true);
		win_.setTitle(getAttr(ImageInternalDataFrame.ATTR_TITLE));
		win_.setPinned(true);
		win_.setBounds(20, 20, doc.bbox.width / 2 + VFrame.titleh,
				doc.bbox.height / 2);
		win_.setFree(true);
		doc_ = new Document("datadispaly", null, win_);
		// doc_.uri = doc.uri;
		String text = getAttr("text");
		String links = getAttr("links");
		boolean hasLink = links != null;
		INode ascii = new IVBox("ddoc", null, doc_);
		INode paraNode = null;
		INode lineNode = null;
		boolean startpara = true;
		BufferedReader ir = new BufferedReader(new StringReader(text));
		String line;
		try {
			while ((line = ir.readLine()) != null) {
				StringBuffer sb = new StringBuffer(line);
				boolean blankline = sb.length() == 0;
				if (blankline) {
					if (paraNode != null) {
						lineNode = new IParaBox("Line", null, paraNode); // IHBox?
						new LeafUnicode(" ", null, lineNode);
					}
					startpara = true;
					continue;
				}
				HyperlinkSpan s = null;
				if (hasLink)
					s = new HyperlinkSpan() {
					@Override
					public void go() {
						getBrowser().setCurDocument((Document) getBrowser().getRoot().findBFS("content"));
						getBrowser().event(new SemanticEvent(getBrowser(), Document.MSG_OPEN, getURI().toString()));
					}
				};
				if (startpara) {
					paraNode = new IVBox("para", null, ascii);
					startpara = false;
					if (s != null) {
						s.restore(n, attr, doc_.getLayer(Layer.SCRATCH));
						try {
							s.setTarget(new URI(links));
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						s.setMessage("Open the citation");

						s.open(paraNode);
						System.out.println("  Open!" + links);
					}
				}
				lineNode = new IParaBox("Line", null, paraNode); // IHBox?

				int start = 0;
				for (int i = 0, imax = sb.length(); i < imax; i++) {
					char wch = sb.charAt(i);
					if (wch == '\t') { // tabs get own word
						if (i > start)
							new LeafUnicode(sb.substring(start, i), null,
									lineNode);
						new LeafUnicode(sb.substring(i, i + 1), null, lineNode);
						start = i + 1;
					} else if (wch == ' ') {
						if (i > start) {
							String txt = i - start == 1 ? phelps.lang.Strings
									.valueOf(sb.charAt(start)) : sb.substring(
											start, i);
									new LeafUnicode(txt, null, lineNode);
						}
						start = i + 1;
					}
				}

				if (start < sb.length())
					new LeafUnicode(sb.substring(start), null, lineNode);

				if (s != null)
					s.close(paraNode);
			}

			ir.close();
		} catch (IOException e) {
		}
		final StyleSheet ss = doc_.getStyleSheet();
		gs = new CLGeneral();

		gs.setForeground(Colors.getColor(getAttr("foreground"), fgcol));
		gs.setBackground(Colors.getColor(getAttr("background"), bgcol));
		gs.setFont(ImageInternalDataFrame.FONT_TEXT);
		ss.put("datadispaly", gs);
		gs = new CLGeneral();
		gs.setPadding(5);
		ss.put("ddoc", gs);
		doc_.padding = INode.INSETS[3];
		doc_.setScrollbarShowPolicy(VScrollbar.SHOW_AS_NEEDED);
		win_.raise();
		return;
	}

	Color fgcol = Color.white;

}
