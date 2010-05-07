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

package uk.ac.liverpool.fab4.behaviors;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Context;
import multivalent.Document;
import multivalent.Layer;
import multivalent.SemanticEvent;
import uk.ac.liverpool.fab4.Fab4;

import com.pt.awt.NFont;

/**
 * 
 * A class that takes care of printing in Fab4.
 * 
 * @author fabio
 *
 */
public class Print extends Behavior implements Printable {
	/**
	 * Print document, asking for page range of multipage document.
	 * <p>
	 * <tt>"print"</tt>.
	 */
	public static final String MSG_PRINT = "print";
	public static final String MSG_PRINT_DIRECT = "print:direct";
	public static final String MSG_PRINT_PREVIEW = "print:preview";
	PrinterJob printJob = null;
	Browser br;
	Document doc;
	boolean paginated;
	/** Number of pages, zero based * */
	int numpages = Pageable.UNKNOWN_NUMBER_OF_PAGES;
	int pageHeight = 0;
	float z = 1f;
	int curpage = 0;
	JButton fp;
	JButton np;
	JButton pp;
	JButton lp;
	JLabel paget;
	JLabel splashlab;
	String labtext = "Printing ";
	boolean cancel = false;
	Thread doPrintThread = null;
	HashPrintRequestAttributeSet hs = null;
	int fromP = -1, toP = -1;

	boolean pageBorder = true;

	/**
	 * @param fromP
	 *            The fromP to set.
	 */
	public void setFromP(int fromP) {
		this.fromP = fromP;
	}

	/**
	 * @param toP
	 *            The toP to set.
	 */
	public void setToP(int toP) {
		this.toP = toP;
	}

	public int print(Graphics g, PageFormat f, final int pi)
	throws PrinterException {
		// System.out.println("Print(+" + pageIndex + "+)");
		Graphics2D g2d = (Graphics2D) g;
		if (fromP == -1) {
			fromP = 0;
			toP = numpages;
		}
		final int pageIndex = pi + fromP;
		// System.out.println("page num:"+pi+" page from"+fromP+
		// "page to"+toP+" index of the page derived(page num+page from)"+
		// pageIndex);
		if (numpages != Pageable.UNKNOWN_NUMBER_OF_PAGES && pageIndex > toP) {
			// System.out.println("END(+" + pageIndex + "+)");
			NFont.setUseBitmaps(true);
			return Printable.NO_SUCH_PAGE;
		}
		if (cancel) {
			cancel = false;
			NFont.setUseBitmaps(true);
			return Printable.NO_SUCH_PAGE;
		}
		if (pageHeight == 0 && pageIndex != 0)
			paintToImage(new Rectangle2D.Double(f.getImageableX(), f
					.getImageableY(), f.getImageableWidth(), f
					.getImageableHeight()), 0);
		//System.out.println("pAGE fORMAT "+f.getImageableX()+" "+f.getImageableY
		// ()+" "+f
		// .getImageableWidth()+" "+ f.getImageableHeight());
		// System.out.println("CB1 "+g2d.getClipBounds());
		// System.out.println("CB2 "+g2d.getDeviceConfiguration().getBounds());
		paintGeneric(
				new Rectangle2D.Double(f.getImageableX(), f.getImageableY(), f
						.getImageableWidth(), f.getImageableHeight()), g2d,
						pageIndex);
		// System.out.println("Done(+" + pageIndex + "+)");
		if (splashlab != null)
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (splashlab.getText().startsWith(labtext + "" + pi))
						splashlab.setText(splashlab.getText() + ".");
					else
						splashlab.setText(labtext + pi);

				}
			});
		return Printable.PAGE_EXISTS;
	}

	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {

		if (msg == Print.MSG_PRINT_DIRECT) {
			hs = (HashPrintRequestAttributeSet) se.getArg();
			doPrint(hs);
		} else if (msg == Print.MSG_PRINT_PREVIEW || msg == Print.MSG_PRINT) {
			if (hs == null)
				hs = new HashPrintRequestAttributeSet();
			fromP = -1;
			toP = -1;
			z = 1f;
			curpage = 0;
			pageHeight = 0;
			br = getBrowser();
			doc = (Document) br.getRoot().findBFS("content");
			printJob = PrinterJob.getPrinterJob();
			// hs = printJob.getPrintService().getAttributes();
			String testo = doc.getAttr(Document.ATTR_TITLE), a;
			if (testo.length() > 18)
				a = testo.substring(0, 18);
			else
				a = testo;
			printJob.setJobName(a);
			paginated = !(doc.getAttr(Document.ATTR_PAGE) == null);
			if (paginated) {
				curpage = doc.getAttr(Document.ATTR_PAGE) != null ? Integer
						.parseInt(doc.getAttr(Document.ATTR_PAGE)) - 1 : 0;
						numpages = doc.getAttr(Document.ATTR_PAGE) != null ? Integer
								.parseInt(doc.getAttr(Document.ATTR_PAGECOUNT)) - 1 : 0;
			}
			if (paginated) {
				Paper pa = printJob.defaultPage().getPaper();
				MediaPrintableArea mpa = new MediaPrintableArea((float) (pa
						.getImageableX() / 72),
						(float) (pa.getImageableY() / 72), (float) (pa
								.getWidth() / 72),
								(float) (pa.getHeight() / 72), MediaPrintableArea.INCH);
				hs.add(mpa);
			}

			/*
			 * PrintService[] pss = printJob.lookupPrintServices(); for
			 * (PrintService ps : pss) { System.out.println(ps.getName()); for
			 * (Attribute att : ps.getAttributes().toArray()) {
			 * System.out.println(att.getName()); if
			 * (att.getName().equals("queued-job-count"))
			 * System.out.println(((QueuedJobCount)att).getValue()); }
			 * ServiceUIFactory factory =
			 * printJob.getPrintService().getServiceUIFactory(); }
			 * PrintService[] pss = printJob.lookupPrintServices();
			 * ServiceUI.printDialog(null, 250,250, pss, pss[0], null, hs);
			 */
			boolean t;
			if (msg == Print.MSG_PRINT_PREVIEW)
				t = printJob.pageDialog(hs) != null;
			else
				t = printJob.printDialog(hs);
			if (t) {
				printJob.setPrintable(this);
				Fab4 f = Fab4.getMVFrame(br);
				f.setEnabled(false);
				f.setResizable(false);
				br.setVisible(false);

				if (msg == Print.MSG_PRINT_PREVIEW)
					buildDialog(hs);
				if (msg == Print.MSG_PRINT)
					doPrint(hs);
			} else
				end();
		}

		return super.semanticEventAfter(se, msg);

	}

	/**
	 * @param hs1
	 */
	void doPrint(final HashPrintRequestAttributeSet hs1) {

		final JFrame splash = new JFrame("Fab4 is printing...");
		JPanel p = new JPanel(new BorderLayout(1, 1));
		splashlab = new JLabel("Printing ");
		splashlab.setHorizontalAlignment(SwingConstants.CENTER);
		splashlab.setFont(splashlab.getFont().deriveFont(20.0f));
		splashlab.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		p.add(splashlab, BorderLayout.CENTER);
		JButton bu = new JButton("Cancel");
		bu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel = true;
				try {
					doPrintThread.join(2000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				printJob.cancel();
				cancel = false;
				NFont.setUseBitmaps(true);
				printJob = null;
				end();
				splash.dispose();
			}
		});
		p.add(bu, BorderLayout.SOUTH);
		splash.getContentPane().add(p);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int sw = 260;
		int sh = 100;
		int x = (d.width - sw) / 2;
		int y = (d.height - sh) / 2;
		splash.setBounds(x, y, sw, sh);
		splash.setVisible(true);
		splash.toFront();
		// splash.setAlwaysOnTop(true);
		pageHeight = 0;
		doPrintThread = new Thread(new Runnable() {
			public void run() {

				try {
					printJob.print(hs1);
					printJob = null;
				} catch (Exception ex) {
					ex.printStackTrace();
					// NFont.setUseBitmaps(true);
				}
				end();
				splash.setVisible(false);
				splash.dispose();
			}
		}, "PrintThread");
		doPrintThread.start();

	}

	/**
	 */
	private void buildDialog(final HashPrintRequestAttributeSet hs1) {
		final JDialog fe = new JDialog(Fab4.getMVFrame(br), "Print Preview");
		fe.getContentPane().setLayout(new BorderLayout(3, 3));
		fe.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				fp = null;
				np = null;
				super.windowClosing(e);
				end();
			}
		});
		final Rectangle2D size = new Rectangle2D.Double(0, 0, 100, 100);
		JPanel p2 = new JPanel(new BorderLayout(2, 2));
		final imCanvas ic = new imCanvas();
		boolean port = true;
		for (Attribute a : hs1.toArray()) {
			if (a instanceof OrientationRequested) {
				OrientationRequested or = (OrientationRequested) a;
				if (or == OrientationRequested.LANDSCAPE)
					port = false;
			}
			if (a instanceof MediaPrintableArea) {
				MediaPrintableArea mpa = (MediaPrintableArea) a;
				size.setRect(mpa.getX(MediaPrintableArea.INCH) * 72.0, mpa
						.getY(MediaPrintableArea.INCH) * 72.0, mpa
						.getWidth(MediaPrintableArea.INCH) * 72.0, mpa
						.getHeight(MediaPrintableArea.INCH) * 72.0);
			}
		}
		if (!port)
			size.setRect(size.getY(), size.getX(), size.getHeight(), size
					.getWidth());

		ic.setTim(paintToImage(size, 0));

		JPanel pright = new JPanel(new FlowLayout(FlowLayout.LEFT));
		fp = Fab4.getMVFrame(br).getButtonBorder("first");
		fp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ic.setTim(paintFirst(size));
				fe.repaint();
			}
		});
		fp.setEnabled(false);
		pright.add(fp);
		pp = Fab4.getMVFrame(br).getButtonBorder("pp");
		pp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ic.setTim(paintPrev(size));
				fe.repaint();
			}
		});
		pp.setEnabled(false);
		pright.add(pp);
		paget = new JLabel("/");
		paget.setEnabled(false);
		paget.setFont(paget.getFont().deriveFont(10.0f));
		pright.add(paget);
		np = Fab4.getMVFrame(br).getButtonBorder("np");
		np.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ic.setTim(paintNext(size));
				fe.repaint();
			}
		});
		np.setEnabled(false);
		pright.add(np);
		lp = Fab4.getMVFrame(br).getButtonBorder("last");
		lp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ic.setTim(paintLast(size));
				fe.repaint();
			}
		});
		lp.setEnabled(false);
		pright.add(lp);
		JPanel p3 = new JPanel(new FlowLayout());
		p2.add(pright, BorderLayout.WEST);
		JPanel p4 = new JPanel(new FlowLayout(FlowLayout.CENTER));
		fromP = 0;
		toP = numpages;
		JLabel pr = new JLabel("Print from");
		pr.setFont(pr.getFont().deriveFont(10.0f));
		final JTextField fromp = new JTextField(2);
		fromp.setFont(fromp.getFont().deriveFont(10.0f));
		fromp.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getButton() == java.awt.event.MouseEvent.BUTTON1)
					fromp.selectAll();

			}
		});
		fromp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					getFromPage(fromp);
			}
		});
		fromp.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				getFromPage(fromp);
			}

			public void focusGained(FocusEvent e) {
			}

		});
		fromp.setText("" + (fromP + 1));
		JLabel pr2 = new JLabel("to");
		pr2.setFont(pr2.getFont().deriveFont(10.0f));
		final JTextField top = new JTextField(2);
		top.setFont(top.getFont().deriveFont(10.0f));
		top.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getButton() == java.awt.event.MouseEvent.BUTTON1)
					top.selectAll();
			}
		});
		top.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					getToPage(top);
			}
		});
		top.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				getToPage(top);
			}

			public void focusGained(FocusEvent e) {
			}

		});
		top.setText("" + (toP + 1));
		JButton bu = new JButton("PRINT");
		bu.setFont(bu.getFont().deriveFont(10.0f));
		bu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// br.eventq(Print.MSG_PRINT_DIRECT, hs);
				getFromPage(fromp);
				getToPage(top);
				fe.setVisible(false);
				fe.dispose();
				doPrint(hs1);
			}
		});
		p3.add(bu, BorderLayout.EAST);
		bu = new JButton("Cancel");
		bu.setFont(bu.getFont().deriveFont(10.0f));
		bu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fe.setVisible(false);
				fe.dispose();
				end();
			}
		});
		p3.add(bu);
		p2.add(p3, BorderLayout.EAST);

		p4.add(pr);
		p4.add(fromp);
		p4.add(pr2);
		p4.add(top);
		p2.add(p4, BorderLayout.CENTER);
		ic.setBorder(new LineBorder(Color.DARK_GRAY, 2));
		fe.getContentPane().add(ic, BorderLayout.CENTER);
		fe.getContentPane().add(p2, BorderLayout.SOUTH);
		fe.pack();
		fe.setVisible(true);
		// fe.repaint();
	}

	BufferedImage paintNext(Rectangle2D size) {
		curpage++;
		return paintToImage(size, curpage);

	}

	BufferedImage paintPrev(Rectangle2D size) {
		curpage--;
		return paintToImage(size, curpage);

	}

	BufferedImage paintFirst(Rectangle2D size) {
		curpage = 0;
		return paintToImage(size, curpage);

	}

	BufferedImage paintLast(Rectangle2D size) {
		curpage = numpages;
		return paintToImage(size, curpage);

	}

	/**
	 * @param size
	 */
	public BufferedImage paintToImage(Rectangle2D size, int num) {
		int w1 = (int) (size.getWidth() + size.getX() * 2);
		int h1 = (int) (size.getHeight() + size.getY() * 2);
		BufferedImage im = new BufferedImage(w1, h1, BufferedImage.TYPE_INT_RGB);
		// System.out.println("***"+(int) (size.getWidth()+size.getX()));
		Graphics2D gg = (Graphics2D) im.getGraphics();
		// gg.setBackground(Color.LIGHT_GRAY);
		Color ct = gg.getColor();
		gg.setColor(new Color(248, 248, 248));
		gg.fillRect(0, 0, w1, h1);
		gg.setColor(Color.WHITE);
		gg.fill(size);
		gg.setColor(ct);
		gg.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		paintGeneric(size, gg, num);
		updateButtons(num);
		gg.dispose();
		if (pageBorder) {
			gg = (Graphics2D) im.getGraphics();
			gg.setColor(Color.DARK_GRAY);
			gg.setStroke(new BasicStroke(1f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 1f, new float[] { 8f, 8f }, 0f));
			gg.draw(size);
			// System.out.println(size);
			gg.dispose();
		}
		return im;
	}

	private void paintPaginated(Rectangle2D size, Graphics2D gg, int npage) {
		Context cx;
		if (Integer.parseInt(doc.getAttr(Document.ATTR_PAGE)) != npage + 1) {
			doc.putAttr(Document.ATTR_PAGE, Integer.toString(npage + 1));
			doc.setValid(false);
			doc.getVsb().setValue(0);
			doc.removeAllChildren();
			doc.getLayer(Layer.SCRATCH).clear();
			doc.getLayers().buildBeforeAfter(doc);
			br.setCurDocument(doc);
		}
		Rectangle pr = doc.bbox;
		Rectangle r = doc.ibbox;
		r.y = (int) (size.getY() * 0.4);
		r.x = (int) (size.getX() * 0.4);
		doc.bbox = r;
		double zx = (size.getX() * 1.4 + size.getWidth()) / r.getWidth();
		double zy = (size.getY() * 1.4 + size.getHeight()) / r.getHeight();
		double zf = Math.min(zx, zy);
		// if (zf<zy && zy<1)

		if (zf < 1 || zf > 1.04)
			gg.scale(zf, zf);
		cx = doc.getStyleSheet().getContext(gg, null);
		doc.paintBeforeAfter(size.getBounds(), cx);
		doc.bbox = pr;
		// gg.dispose();
	}

	/**
	 * @param npage
	 */
	private void updateButtons(final int npage) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (np != null) {
					if (curpage >= numpages) {
						np.setEnabled(false);
						lp.setEnabled(false);
					} else {
						np.setEnabled(true);
						lp.setEnabled(true);
					}
					if (curpage <= 0) {
						pp.setEnabled(false);
						fp.setEnabled(false);
					} else {
						pp.setEnabled(true);
						fp.setEnabled(true);
					}
					paget.setText(" " + (npage + 1) + "/" + (numpages + 1)
							+ " ");
				}
			}
		});
	}

	/**
	 * @param size
	 * @param gg
	 * @param npage
	 *            page number 0 based
	 */

	private void paintGeneric(Rectangle2D size, Graphics2D gg, int npage) {
		NFont.setUseBitmaps(false);
		// if (npage >= numpages) npage = numpages;
		// if (npage < 0) npage = 0;
		if (!paginated)
			printFlowed(size, gg, npage);
		else
			paintPaginated(size, gg, npage);
	}

	/**
	 * @param size
	 * @param gg
	 * @param npage
	 */
	private void printFlowed(Rectangle2D size, Graphics2D gg, int npage) {
		Context cx;
		Rectangle b;
		Rectangle2D b1;
		Rectangle r;
		gg.setClip(size);
		cx = doc.getStyleSheet().getContext(gg, null);
		// 1st page
		if (npage == 0 && pageHeight == 0) {
			// System.out.println("!ONCE!");
			doc.markDirtySubtree(true);
			doc.formatNode((int) size.getWidth(), (int) size.getHeight(), cx);
			b1 = size;
			b = doc.childAt(0).bbox;
			// First we look at page width, this must fit.
			// We scale down the whole page including images and else
			if (b.width > b1.getWidth())
				z = 1f * (float) b1.getWidth() / b.width - 0.01f;
			// try to fit single page for heigth (line in the 2nd page)
			// if we didn't already fit width
			if (z == 1f && b.height > b1.getHeight()) {
				int modHeigth = b.height % (int) b1.getHeight(); // how much
				// left of
				// the
				// last page
				float difh = modHeigth / (float) b1.getHeight();
				if (difh < 0.05f && difh > 0f)
					z = 1f * (float) b1.getHeight()
					/ (modHeigth + (int) b1.getHeight()) - 0.01f;
			}
			pageHeight = (int) (size.getHeight() / z);
			if (z != 1f) {
				// System.out.println("Scale:" + z);
				size = new Rectangle2D.Double(size.getX() / z, size.getY() / z,
						size.getWidth() / z, size.getHeight() / z);
				gg.scale(z, z);
				gg.setClip(size);
				cx = doc.getStyleSheet().getContext(gg, null);
				doc.markDirtySubtree(true);
				doc.formatNode((int) size.getWidth(), (int) size.getHeight(),
						cx);
			}
			b = doc.childAt(0).bbox;
			numpages = b.height / (int) size.getHeight()
			- (b.height % (int) size.getHeight() == 0 ? 1 : 0);
		} else if (z != 1f) {
			size = new Rectangle2D.Double(size.getX() / z, size.getY() / z,
					size.getWidth() / z, size.getHeight() / z);
			gg.scale(z, z);
			gg.setClip(size);
		}

		int st = pageHeight * npage;
		if (st != 0)
			// System.out.println("Scroll:" + st);
			gg.translate(size.getX(), (size.getY() - st));
		else
			gg.translate(size.getX(), size.getY());
		r = gg.getClipBounds();
		doc.childAt(0).paintBeforeAfter(r, cx);
		// gg.dispose();
	}

	public Printable getPrintable(int pageIndex)
	throws IndexOutOfBoundsException {
		return this;
	}

	/**
	 * 
	 */
	void end() {
		NFont.setUseBitmaps(true);
		br.setBounds(doc.getBbox());
		// br.eventq(Zoom.MSG_SET, Zoom.ARG_BIGGER);
		Fab4 f = Fab4.getMVFrame(br);
		f.setEnabled(true);
		f.setResizable(true);
		f.updatePagec();
		br.setVisible(true);
		printJob = null;
		if (paginated)
			hs = null;

	}

	/**
	 * @param fromp
	 */
	void getFromPage(final JTextField fromp) {
		try {
			int c = Integer.parseInt(fromp.getText()) - 1;
			if (c >= 0 && c <= toP)
				setFromP(c);
			else {
				setFromP(fromP);
				fromp.setText("" + (fromP + 1));
			}
		} catch (NumberFormatException ef) {
			setFromP(fromP);
			fromp.setText("" + (fromP + 1));
		}
	}

	/**
	 * @param top
	 */
	void getToPage(final JTextField top) {
		try {
			int c = Integer.parseInt(top.getText()) - 1;
			if (c >= fromP && c <= numpages)
				setToP(c);
			else {
				setToP(toP);
				top.setText("" + (toP + 1));
			}
		} catch (NumberFormatException ef) {
			setToP(toP);
			top.setText("" + (toP + 1));
		}
	}
}

class imCanvas extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Image tim = null;
	private Dimension size = new Dimension(300, 100);

	public imCanvas() {
		super();
		setOpaque(true);
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getPreferredSize() {
		return size;

	}

	@Override
	public void paint(Graphics g) {
		// Graphics2D gg =(Graphics2D)g;
		// gg.se

		g.drawImage(tim, 1, 1, /* this.getWidth(), this.getHeight(), */this);
		g.drawRect(0, 0, size.width - 1, size.height - 1);
		// super.paint(g);
	}

	public Image getTim() {
		return tim;
	}

	public void setTim(BufferedImage im) {
		tim = im;
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		d.setSize(d.getWidth() - 160, d.getHeight() - 160);
		int iw = im.getWidth();
		int ih = im.getHeight();
		double c = (double) d.width / (double) iw;
		double b = (double) d.height / (double) ih;
		double scale = 1;
		scale = Math.min(c, b);
		if (scale > 1)
			scale = 1;
		size.width = (int) (iw * scale) + 2;
		size.height = (int) (ih * scale) + 2;
		if (scale != 1)
			tim = im.getScaledInstance(size.width - 2, size.height - 2,
					Image.SCALE_SMOOTH);
		repaint();
	}
}