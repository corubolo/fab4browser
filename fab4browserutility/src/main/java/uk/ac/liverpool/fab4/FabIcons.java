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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Convenience class containing all the icons (resources) used by Fab4.
 * @author fabio
 *
 */
public class FabIcons {
	public ImageIcon ICOGENTXT;

	public ImageIcon ICOITA;

	public ImageIcon ICOBOLD;

	public ImageIcon ICODOWN;

	public ImageIcon ICODEL;

	public ImageIcon ICOPUB;

	public ImageIcon ICOSHO;

	public ImageIcon ICOICO;

	public ImageIcon ICOHID;

	public ImageIcon ICOHIGLIGHT;

	public ImageIcon BICO1;

	public ImageIcon BICO2;

	public ImageIcon ICOURL;

	public ImageIcon ICOHIGHLIGHT;

	public ImageIcon ICOPDF;

	public ImageIcon ICOTXT;

	public ImageIcon ICOLIGHT;

	public ImageIcon ICOUSER;

	public ImageIcon ICOODT;

	public ImageIcon ICOBMK;

	public Image FAB4ICO;

	public ImageIcon HAVENOTES_ICO;

	public ImageIcon NOTE_ICO;

	public ImageIcon NOTE_ICO_CALL;

	public Icon ICOLINK;

	public Icon ICOIMA;

	public ImageIcon ICOSTR;

	public ImageIcon ICOUND;

	public ImageIcon ICOCOM;
	public ImageIcon ICOCUT;
	public ImageIcon ICOPASTE;

	public Cursor SelectCursor;

	public ImageIcon ICOSAVE;

	private static FabIcons theInstance = null;

	private FabIcons() {
	}

	public static FabIcons getIcons() {
		if (FabIcons.theInstance == null) {
			FabIcons.theInstance = new FabIcons();
			FabIcons.theInstance.loadIcons();
		}
		return FabIcons.theInstance;
	}
	/**
	 * Convienence method to scale t a max w and h
	 * @param nMaxWidth max width
	 * @param nMaxHeight max height
	 * @param imgSrc
	 * @return
	 */
	public static BufferedImage scaleToSize(int nMaxWidth, int nMaxHeight,
			BufferedImage imgSrc) {
		int nHeight = imgSrc.getHeight();
		int nWidth = imgSrc.getWidth();
		double scaleX = (double) nMaxWidth / (double) nWidth;
		double scaleY = (double) nMaxHeight / (double) nHeight;
		return scale(scaleX, scaleY, imgSrc);
	}

	/**
	 * Convenience method for scaling a BufferedImage
	 * @param scalex fraction
	 * @param scaley fraction
	 * @param srcImg
	 * @return
	 */
	public static BufferedImage scale(double scalex, double scaley,
			BufferedImage srcImg) {
		if (scalex == scaley && scaley == 1.0d)
			return srcImg;
		AffineTransformOp op = new AffineTransformOp(AffineTransform
				.getScaleInstance(scalex, scaley), null);
		return op.filter(srcImg, null);
	}
	private void loadIcons() {
		//System.out.println("load icons start");
		try {
			BufferedImage bi = ImageIO.read(getClass().getResource(
			"/res/select.png"));

			Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(
					bi.getWidth(), bi.getHeight());
			int maxc = Toolkit.getDefaultToolkit().getMaximumCursorColors();
			if (maxc <= 256 || d.width == 0)
				SelectCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
			else {
				double scaleX = (double) d.width / (double) bi.getWidth();
				double scaleY = (double) d.height / (double) bi.getHeight();
				bi = scale(scaleX, scaleY, bi);

				SelectCursor = Toolkit.getDefaultToolkit()
				.createCustomCursor(
						bi,
						new Point((int) (3.0d * scaleX),
								(int) (14.0d * scaleY)),
								"Fab4highlight");
			}
		} catch (Exception e) {
			SelectCursor = null;
		}
		Class c = getClass();

		try {
			ICODOWN = new ImageIcon(c.getResource("/res/down12.png"));
		} catch (Exception e) {
			ICODOWN = null;
		}
		try {
			ICOCUT = new ImageIcon(c.getResource("/res/cut.png"));
		} catch (Exception e) {
			ICOCUT = null;
		}
		try {
			ICOCOM = new ImageIcon(c.getResource("/res/comment.png"));
		} catch (Exception e) {
			ICOCOM = null;
		}

		try {
			ICOLIGHT = new ImageIcon(c.getResource("/res/find.png"));
		} catch (Exception e) {
			ICOLIGHT = null;
		}
		try {
			ICOPASTE = new ImageIcon(c.getResource("/res/paste.png"));
		} catch (Exception e) {
			ICOPASTE = null;
		}

		try {
			ICOUSER = new ImageIcon(c.getResource("/res/user.png"));
		} catch (Exception e) {
			ICOUSER = null;
		}

		try {
			ICOURL = new ImageIcon(c.getResource("/res/url1.png"));
		} catch (Exception e) {
			ICOURL = null;
		}
		try {
			ICOBMK = new ImageIcon(c.getResource("/res/book_add.png"));
		} catch (Exception e) {
			ICOBMK = null;
		}
		try {
			ICOHIGLIGHT = new ImageIcon(c
					.getResource("/res/mark_occurrences.gif"));
		} catch (Exception e) {
			ICOHIGLIGHT = null;
		}

		try {
			BICO1 = new ImageIcon(c
					.getResource("/res/application_side_contract.png"));
		} catch (Exception e) {
			BICO1 = null;
		}
		try {
			BICO2 = new ImageIcon(c
					.getResource("/res/application_side_expand.png"));
		} catch (Exception e) {
			BICO2 = null;
		}

		try {
			NOTE_ICO = new ImageIcon(c.getResource("/res/note_add.png")); ///res
			// /
			// comment_add
			// .
			// png
		} catch (Exception e) {
			NOTE_ICO = null;
		}
		try {
			ICODEL = new ImageIcon(c.getResource("/res/delete.png"));
			// ICODEL = new ImageIcon(c.getResource("/res/comment_delete.png"));
		} catch (Exception e) {
			ICODEL = null;
		}
		try {
			ICOPUB = new ImageIcon(c.getResource("/res/note_edit.png"));
		} catch (Exception e) {
			ICOPUB = null;
		}
		try {
			ICOTXT = new ImageIcon(c.getResource("/res/txt.png"));
		} catch (Exception e) {
			ICOTXT = null;
		}
		try {
			ICOSHO = new ImageIcon(c
					.getResource("/res/application_view_tile.png"));
		} catch (Exception e) {
			ICOSHO = null;
		}
		try {
			ICOICO = new ImageIcon(c
					.getResource("/res/application_view_icons.png"));
		} catch (Exception e) {
			ICOICO = null;
		}
		try {
			ICOHID = new ImageIcon(c.getResource("/res/application_xp.png"));
		} catch (Exception e) {
			ICOHID = null;
		}

		try {
			ICOPDF = new ImageIcon(c.getResource("/res/pdf.png"));
		} catch (Exception e) {
			ICOPDF = null;
		}
		try {
			FAB4ICO = ImageIO.read(c.getResource("/res/fab4.png"));
		} catch (Exception e) {
			FAB4ICO = null;
		}
		try {
			NOTE_ICO_CALL = new ImageIcon(c.getResource("/res/comment_add.png"));
		} catch (Exception e) {
			NOTE_ICO_CALL = null;
		}
		try {
			ICOODT = new ImageIcon(c.getResource("/res/odt.gif"));
		} catch (Exception e) {
			ICOODT = null;
		}
		try {
			HAVENOTES_ICO = new ImageIcon(c.getResource("/res/havenotes.png"));
		} catch (Exception e) {
			HAVENOTES_ICO = null;
		}
		try {
			ICOITA = new ImageIcon(c.getResource("/res/text_italic.png"));
		} catch (Exception e) {
			ICOITA = null;
		}
		try {
			ICOBOLD = new ImageIcon(c.getResource("/res/text_bold.png"));
		} catch (Exception e) {
			ICOBOLD = null;
		}
		try {
			ICOGENTXT = new ImageIcon(c.getResource("/res/text_signature.png"));
		} catch (Exception e) {
			ICOGENTXT = null;
		}
		try {
			ICOHIGHLIGHT = new ImageIcon(c
					.getResource("/res/mark_occurrences.gif"));
		} catch (Exception e) {
			ICOHIGHLIGHT = null;
		}
		try {
			ICOIMA = new ImageIcon(c.getResource("/res/image.png"));
		} catch (Exception e) {
			ICOIMA = null;
		}
		try {
			ICOSTR = new ImageIcon(c.getResource("/res/text_strikethrough.png"));
		} catch (Exception e) {
			ICOSTR = null;
		}
		try {
			ICOUND = new ImageIcon(c.getResource("/res/text_underline.png"));
		} catch (Exception e) {
			ICOUND = null;
		}

		try {
			ICOLINK = new ImageIcon(c.getResource("/res/link.png"));
		} catch (Exception e) {
			ICOLINK = null;
		}
		try {
			ICOSAVE = new ImageIcon(c.getResource("/res/disk.png"));
		} catch (Exception e) {
			ICOSAVE = null;
		}
		//System.out.println("load icons stop");
	}

}
