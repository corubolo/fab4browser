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
package test;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Semaphore;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;

import org.apache.batik.transcoder.wmf.tosvg.WMFPainter;
import org.apache.batik.transcoder.wmf.tosvg.WMFRecordStore;
import org.apache.poi.hslf.usermodel.PictureData;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.freehep.graphicsio.emf.EMFViewer;

import fi.faidon.jvg.PICTReader;

/**
 * 
 * @author Fabio Corubolo
 * 
 * Test class to extract and view images in pict, emf, and wmf formats;
 *
 */


public class ExtractAllImages {

	private static final boolean doPict = false;
	private static final boolean doEmf = true;
	private static final boolean doWMF = false;
	JFrame f;
	JButton b;
	EMFViewer v;

	final Semaphore s = new Semaphore(1);

	public ExtractAllImages() {
		v = new EMFViewer();
		//if (doPict){
		f = new JFrame();
		b = new JButton();
		b.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				s.release();

			}
		});
		f.getContentPane().add(b);
		f.setVisible(true);
		//}
}

	/* Utility to extract all images from ppt files */
	/* input: signle ppt file or dir */
	/* option -r to recurse */
	public static void main(String[] args) {
		boolean recurse = false;
		File d = null;
		if (args.length < 1)
			return;
		if (args[0].startsWith("-r")) {
			recurse = true;
			if (args.length < 2)
				return;
			d = new File(args[1]);
		} else
			d = new File(args[0]);
		new ExtractAllImages().listFiles(d, recurse, 0);


	}

	public void listFiles(File d, boolean recurse, int type) {
		if (d.isDirectory() && d.canRead() && d.exists()) {

			File[] dirList = d.listFiles();
			for (File element : dirList) {
				System.gc();
				if (!element.canRead()) {
					System.out.println("Can't read " + element);
				}
				if (!element.exists()) {
					System.out.println("Does not exist: " + element);
				}
				if (element.isDirectory()) {
					if (recurse)
						listFiles(element, recurse, type);
					// loop again
					continue;
				}
				try {
					if (!element.getAbsolutePath().toLowerCase()
							.endsWith("ppt")) {
						//System.out.println("Refused file: " + element);
						continue;
					}

					FileInputStream fis = new FileInputStream(element);
					SlideShow ppt = new SlideShow(fis);
					PictureData[] pd = ppt.getPictureData();
					int i = 0;
					for (PictureData p : pd) {

						if (type != 0 && p.getType() != type)
							continue;


						byte[] data;
						String ssem = (i++) + ".";
						switch (p.getType()) {
						case 2:
							ssem += "EMF";
							if (doEmf){
								System.out.println(element.getAbsoluteFile() + ssem);

								data = p.getData();
								try {
									s.acquire();
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								//v.show(new ByteArrayInputStream(data), element.getAbsoluteFile() + ssem, s);
								//Process pr = Runtime.getRuntime().exec("open -a Preview "+ element.getName() +s +"",null, element.getParentFile());
							}
							break;
						case 3:
							ssem += "WMF";
							if (doWMF) {
							System.out.println("++"+element.getAbsoluteFile() + ssem);
							data = p.getData();
							displayImagea(data);
							f.setTitle(element.getAbsoluteFile() + ssem);
							FileOutputStream outd = new FileOutputStream(new File(
									element.getAbsoluteFile() + ssem));
							try {
								outd.write(data);
							} catch (Exception e) {
								System.err.println("On file: " + element);
								e.printStackTrace();
							}
							outd.close();
							}
							break;
						case 4:
							ssem += "PICT";
							if (doPict){
								System.out.println(element.getAbsoluteFile() + ssem);
								data = p.getData();
								displayImage(data);
								FileOutputStream out = new FileOutputStream(new File(
										element.getAbsoluteFile() + ssem));
								try {
									out.write(data);
								} catch (Exception e) {
									System.err.println("On file: " + element);
									e.printStackTrace();
								}
								out.close();
								f.setTitle(element.getAbsoluteFile() + ssem);

								Process pr = Runtime.getRuntime().exec("open -a Preview "+ element.getName() +ssem +"",null, element.getParentFile());
							}
							//							BufferedReader br = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
							//							String line;
							//							while ((line= br.readLine()) != null)
							//								System.out.println(line);
							break;
						case 5:
							ssem += "JPG";
							break;
						case 6:
							ssem += "PNG";
							break;
						case 7:
							ssem += "DIB";
							break;

						default:
							ssem += "SUR";
							break;
						}


					}
				} catch (Exception e) {
					System.err.println("* On file: " + element);
					e.printStackTrace();
				}

			}
		}

		System.out.println("OUT OF MAIN!!!!");
	}

	private void displayImagea(byte[] data) {

		try {
			try {
				s.acquire();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			WMFRecordStore currentStore = new WMFRecordStore();
			currentStore.read(new DataInputStream(new ByteArrayInputStream(data)));
			// Build a painter for the RecordStore
			float r = 1;
			Rectangle2D o = currentStore.getRectanglePixel();
			if (currentStore.getVpW()>1000){
				r= 1000f / ((float)o.getWidth());
			}

			WMFPainter painter = new WMFPainter(currentStore,(int)(o.getX()/r),(int)(o.getY()/r),1);
			System.out.println(currentStore.getRectanglePixel());

			final BufferedImage img = new BufferedImage((int) (o.getWidth()*r), (int)(o.getHeight()*r), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = ((BufferedImage)img).createGraphics();
			g.scale(r, r);
			RenderingHints rh = g.getRenderingHints();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			painter.paint(g);
			g.dispose();


			Icon i = new Icon() {

				public void paintIcon(Component c, Graphics g, int x, int y) {
					g.drawImage(img, x, y, null);
				}

				public int getIconWidth() {
					// TODO Auto-generated method stub
					return img.getWidth();
				}

				public int getIconHeight() {
					// TODO Auto-generated method stub
					return img.getHeight();
				}
			};
			b.setIcon(i);
			f.pack();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void displayImage(byte[] data) {
		PICTReader r;
		try {
			try {
				s.acquire();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			r = new PICTReader(new ByteArrayInputStream(data));
			r.setVerbose(false);
			final BufferedImage img = new BufferedImage(r.getWidth(), r
					.getHeight(), BufferedImage.TYPE_INT_ARGB);
			r.playIt(img.createGraphics());

			Icon i = new Icon() {

				public void paintIcon(Component c, Graphics g, int x, int y) {
					g.drawImage(img, x, y, null);
				}

				public int getIconWidth() {
					// TODO Auto-generated method stub
					return img.getWidth();
				}

				public int getIconHeight() {
					// TODO Auto-generated method stub
					return img.getHeight();
				}
			};
			b.setIcon(i);
			f.pack();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
