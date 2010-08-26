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
 * This class implements the renderer for the cells of the annotation list.
 * @author fabio
 * 
 */
public class FabAnnoListRenderer implements ListCellRenderer {

	private static final long serialVersionUID = 1L;

	JLabel theLabel = new JLabel();

	JLabel theLabel2 = new JLabel();

	JPanel thePanel = new JPanel(new BorderLayout(5,5));

	public static final Color PERSONAL = new Color(153, 255, 153);// new Color(153,153,255);

	public static final Color ANON = new Color(255, 130, 130);// new Color(153,153,255);

	public static final Color KNOWN = FabAnnoListRenderer.PERSONAL;

	public static final Color UNKNOWN = new Color(220, 220, 220);

	public static final Color TOPUBLISH = new Color(255, 153, 153);// new Color(153,153,255);

	public FabAnnoListRenderer() {
		theLabel.setOpaque(false);
		theLabel.setIconTextGap(2);
		theLabel.setBorder(new EmptyBorder(15,5,15,5));
		thePanel.add(theLabel,BorderLayout.NORTH);
		thePanel.add(theLabel2,BorderLayout.SOUTH);
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

		Behavior bh = fa.getBehaviour();
		theLabel.setIcon(getIconForAnno(bh));
		int apn = 0;

		if (fa.getAnn().getPageNumber()!=0)
			apn = fa.getAnn().getPageNumber();
		String text;
		String tt = "<HTML><BODY>";
		if (fa.getSigner() != null) {
			if (fa.getVerificationStatus() == 1) {
				if (fa.getSigner().isItsme())
					theLabel.setFont(theLabel.getFont().deriveFont(Font.BOLD));
				else
					theLabel.setFont(theLabel.getFont().deriveFont(Font.PLAIN));
				text = fa.getSigner().getName();
				tt += "<h2>" +
				"Signed annotation</h2>" + "Name: <b>"
				+ fa.getSigner().getName() + " </b><br>" + "Email: <b>"
				+ fa.getSigner().getEmail() + " </b><br>" + "Organisation: <b>"
				+ fa.getSigner().getOrganisation() + " </b><br><br><hr><br>";

			} else {
				theLabel.setFont(theLabel.getFont().deriveFont(Font.PLAIN));
				text = fa.getAnn().getUserid();
			}
		} else {
			theLabel.setFont(theLabel.getFont().deriveFont(Font.PLAIN));
			text = fa.getAnn().getUserid();
		}

		String verStat = fa.getVerificationStatus() == 1 ? "Valid signature"
				: fa.getVerificationStatus() == 0 ? "No signature" : fa
						.getVerificationStatus() == -1 ? "INVALID SIGNATURE"
								: "Unknown user";
		tt += "<b>" + verStat + "</b> <br>";
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
		int tot = fa.totPages;
		if (apn<=tot )
			tt += "Page number: <b>" +apn+"</b><br>";

		theLabel.setText(text);
		theLabel.setBorder(new EmptyBorder(new Insets(4, 3, 1, 1)));
		theLabel2.setBorder(new EmptyBorder(new Insets(1, 3, 3, 1)));
		Color background;
		Color foreground;
		int k = 0;
		k += fa.isSameUrl() ? 0 : 10;
		k += fa.isSameTxtCRC() ? 0 : 10;
		if (fa.getAnn().getDocumentDigest()!=null)
			k = fa.isSameCRC() ? (k=0) : (k+=10);
			k *= 4;
			Color c = new Color(k, k, k);
			//System.out.println(c);
			if (fa.getAnn()!=null && fa.getAnn()!=null && fa.getAnn().getUserid().equals("anonymous"))
				background = FabAnnoListRenderer.ANON;
			else if (fa.getSigner() != null && fa.getSigner().isItsme())
				background = FabAnnoListRenderer.PERSONAL;
			else if (fa.getVerificationStatus() == 1)
				background = FabAnnoListRenderer.KNOWN;
			else
				background = FabAnnoListRenderer.UNKNOWN;
			theLabel.setOpaque(true);
			if (isSelected) {
				background = background.darker();
				foreground = Color.WHITE;
			} else
				foreground = c;
			theLabel2.setFont(theLabel2.getFont().deriveFont(9.0f));
			String l2t = Fab4utils.DATEFTIME_SHORT.format(fa.getAnn().getDateModified());
			if (apn<=tot)
				l2t =l2t + " p."+apn;
			String anno = fa.getAnn().getAnnotationBody();
			String body = Fab4utils.stripXml(anno);
			body = body.substring(0, Math.min(body.length(),32));
			if (body.length()==0){
				body = l2t;
				theLabel2.setForeground(Color.DARK_GRAY);
			} else
				theLabel2.setForeground(Color.BLACK);

			theLabel2.setText(body);
			theLabel.setBackground(background);
			theLabel2.setBackground(background);
			thePanel.setBackground(background);
			theLabel.setForeground(foreground);
			tt += "</HTML></BODY>";
			thePanel.setToolTipText(tt);
			Dimension preferredSize = new Dimension(theLabel.getPreferredSize());
			preferredSize.height+=theLabel2.getPreferredSize().height ;
			preferredSize.width=Math.max(theLabel2.getPreferredSize().width,preferredSize.width);
			thePanel.setPreferredSize(preferredSize);
			thePanel.setMinimumSize(preferredSize);
			thePanel.setMaximumSize(preferredSize);
			return thePanel;
	}

}
