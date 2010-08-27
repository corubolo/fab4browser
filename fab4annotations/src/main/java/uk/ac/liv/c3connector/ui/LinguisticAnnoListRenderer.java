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


package uk.ac.liv.c3connector.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

import multivalent.Behavior;
import multivalent.gui.VFrame;
import multivalent.std.span.BackgroundSpan;
import multivalent.std.span.BoldSpan;
import multivalent.std.span.HyperlinkSpan;
import multivalent.std.span.ItalicSpan;
import multivalent.std.span.OverstrikeSpan;
import multivalent.std.span.UnderlineSpan;
import uk.ac.liv.c3connector.FabAnnotation;
import uk.ac.liv.c3connector.FabNote;
import uk.ac.liverpool.fab4.Fab4utils;
import uk.ac.liverpool.fab4.FabIcons;

/**
 * 
 * This class implements the renderer for the cells (elements) of the annotation list when in Linguistic mode.
 * It is focusing on features used by Linguisitc annoatations
 * 
 * @author fabio
 * 
 */
public class LinguisticAnnoListRenderer implements ListCellRenderer {

	private static final long serialVersionUID = 1L;

	JLabel title = new JLabel();

	JLabel subTitle = new JLabel();

	JPanel thePanel = new JPanel(new BorderLayout(5,5));

	Font PLAIN = title.getFont().deriveFont(Font.PLAIN);
	Font BOLD = title.getFont().deriveFont(Font.BOLD);

	public static final Color PERSONAL = new Color(153, 255, 153);// new Color(153,153,255);

	public static final Color ANON = new Color(255, 130, 130);// new Color(153,153,255);

	public static final Color KNOWN = LinguisticAnnoListRenderer.PERSONAL;

	public static final Color UNKNOWN = VFrame.grigietto;

	public static final Color TOPUBLISH = new Color(255, 153, 153);// new Color(153,153,255);

	public LinguisticAnnoListRenderer() {
		title.setOpaque(false);
		title.setIconTextGap(2);
		title.setBorder(new EmptyBorder(15,5,15,5));

		subTitle.setFont(subTitle.getFont().deriveFont(9.0f));
		title.setFont(title.getFont().deriveFont(11.0f));
		thePanel.add(title,BorderLayout.NORTH);
		thePanel.add(subTitle,BorderLayout.SOUTH);
		thePanel.setInheritsPopupMenu(true);


	}

	public static Icon getIconForAnno(Behavior bh) {
		FabIcons f = FabIcons.getIcons();

		if (bh instanceof FabNote) {
			if (((FabNote)bh).callout)
				return f.NOTE_ICO_CALL;
			return f.NOTE_ICO;
		} else if (bh instanceof ItalicSpan)
			return f.ICOITA;
		else if (bh instanceof BoldSpan)
			return f.ICOBOLD;
		else if (bh instanceof HyperlinkSpan)
			return f.ICOLINK;
		else if (bh instanceof BackgroundSpan)
			return f.ICOHIGHLIGHT;
		else if (bh instanceof OverstrikeSpan)
			return f.ICOSTR;
		else if (bh instanceof UnderlineSpan)
			return f.ICOUND;
		else
			return f.ICOGENTXT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean,
	 *      boolean)
	 */
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		FabAnnotation fa = (FabAnnotation) value;
		if (fa==null)
			return thePanel;
		String text;
		String tt = "<HTML><BODY>";


		Behavior bh = fa.getBehaviour();
		BackgroundSpan bs = null;
		if (bh instanceof BackgroundSpan)
			bs = (BackgroundSpan) bh;

		title.setIcon(getIconForAnno(bh));

		int apn = 0;
		if (fa.getAnn().getPageNumber()!=0)
			apn = fa.getAnn().getPageNumber();
		int tot = fa.totPages;
		if (apn<=tot )
			tt += "Page number: <b>" +apn+"</b><br>";


		//		if (fa.getSigner() != null) {
		//			if (fa.getVerificationStatus() == 1) {
		//				if (fa.getSigner().isItsme()) {
		//					title.setFont(BOLD);
		//				} else {
		//					title.setFont(PLAIN);
		//				}
		//				text = fa.getSigner().getName();
		//				tt += "<h2>" +
		//						"Signed annotation</h2>" + "Name: <b>"
		//				+ fa.getSigner().getName() + " </b><br>" + "Email: <b>"
		//				+ fa.getSigner().getEmail() + " </b><br>" + "Organisation: <b>"
		//				+ fa.getSigner().getOrganisation() + " </b><br><br><hr><br>";
		//
		//			} else {
		//				title.setFont(PLAIN);
		//				text = at.getUserId();
		//				}
		//		} else {
		title.setFont(PLAIN);
		text = fa.getAnn().getUserid();
		/*		}

		String verStat = (fa.getVerificationStatus() == 1 ? "Valid signature"
				: (fa.getVerificationStatus() == 0 ? "No signature" : (fa
						.getVerificationStatus() == -1 ? "INVALID SIGNATURE"
						: "Unknown user")));
		tt += "<b>" + verStat + "</b> <br>";
		 */
		if (fa.isSameUrl())
			tt += "Annotates <b>this URL</b><br>";
		if (fa.getAnn().getDocumentDigest()!=null && fa.isSameCRC())
			tt += "Annotates this <b>exact document</b><br>";
		if (fa.isSameTxtCRC())
			tt += "Annotates the <b>same exact contents</b><br>";
		if (fa.getAnn().getLexicalSignature()!=null && fa.isSameLs())
			tt += "Annotates the <b>similar contents</b><br>";
		if (fa.getAnn().getDateModified() != null)
			tt += "Mod. date: <b>"
				+ Fab4utils.DATEFTIME_SHORT.format(fa.getAnn().getDateModified()) + " at " +
				Fab4utils.DATEFTIME_SHORT.format(fa.getAnn().getDateModified())
				+"</b><br>";
		if (bs!=null){
			text = fa.getAnn().getUserid();
			text = text + "-"+bs.getColorName();

		}
		title.setText(text);

		title.setBorder(new EmptyBorder(new Insets(4, 3, 1, 1)));
		subTitle.setBorder(new EmptyBorder(new Insets(1, 3, 3, 1)));

		Color background;
		Color foreground;
		int k = 0;
		k += fa.isSameUrl() ? 0 : 10;
		k += fa.isSameTxtCRC() ? 0 : 10;
		if (fa.getAnn().getDocumentDigest()!=null)
			k = fa.isSameCRC() ? (k=0) : (k+=10);
			k *= 4;
			Color c = new Color(k, k, k);

			if (fa.getAnn()!=null && fa.getAnn()!=null && fa.getAnn().getUserid().equals("anonymous"))
				background = LinguisticAnnoListRenderer.ANON;
			else if (fa.getSigner() != null && fa.getSigner().isItsme())
				background = LinguisticAnnoListRenderer.PERSONAL;
			else if (fa.getVerificationStatus() == 1)
				background = LinguisticAnnoListRenderer.KNOWN;
			else
				background = LinguisticAnnoListRenderer.UNKNOWN;
			title.setOpaque(true);
			if (isSelected) {
				background = background.darker();
				foreground = Color.WHITE;
			} else
				foreground = c;
			if (bs!=null)
				background = bs.getColor();


			String l2t = Fab4utils.DATEFTIME_SHORT.format(fa.getAnn().getDateModified());
			if (apn<=tot)
				l2t =l2t + " p."+apn;
			String anno = fa.getAnn().getAnnotationBody();
			StringBuilder cont = new StringBuilder();
			int x = anno.indexOf('>');
			int y = anno.indexOf('<', x);
			while (x >= 0 && y >= 0) {
				String t = anno.substring(x + 1, y).trim();
				if (t.length() > 0) {
					cont.append(t);
					cont.append(' ');
				}
				x = anno.indexOf('>', y);
				y = anno.indexOf('<', x);
			}
			String body = cont.toString();


			body = body.substring(0, Math.min(body.length(),32));
			if (body.length()==0){
				if (bs!=null)
					body = bs.getAttr("content");
				else
					body = l2t;

				subTitle.setForeground(Color.DARK_GRAY);
			} else
				subTitle.setForeground(Color.BLACK);

			subTitle.setText(body);

			title.setBackground(background);
			subTitle.setBackground(background);
			thePanel.setBackground(background);
			title.setForeground(foreground);
			tt += "</HTML></BODY>";
			thePanel.setToolTipText(tt);
			Dimension preferredSize = new Dimension(title.getPreferredSize());
			preferredSize.height+=subTitle.getPreferredSize().height ;
			preferredSize.width=Math.max(subTitle.getPreferredSize().width,preferredSize.width);
			thePanel.setPreferredSize(preferredSize);
			thePanel.setMinimumSize(preferredSize);
			thePanel.setMaximumSize(preferredSize);
			return thePanel;
	}

}
