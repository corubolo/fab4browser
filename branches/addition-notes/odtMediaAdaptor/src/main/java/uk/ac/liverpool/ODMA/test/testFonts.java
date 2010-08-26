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
/**
 * Author: Fabio Corubolo - f.corubolo@liv.ac.uk
 * (c) 2005 University of Liverpool
 */
package uk.ac.liverpool.ODMA.test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.pt.awt.font.NFontManager;

/**
 * @author fabio
 *
 */
public class testFonts {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		NFontManager f = NFontManager.getDefault();
		String[] families = f.getAvailableFamilies();
		String[] names = f.getAvailableNames();
		for (int i=0;i<families.length;i++)
			System.out.println(""+i+families[i]);
		for (int i=0;i<names.length;i++)
			System.out.println(""+i+names[i]);
		System.out.println("---------------------------------");
		GraphicsEnvironment ge = GraphicsEnvironment.
		getLocalGraphicsEnvironment();
		Font[] fonts= ge.getAllFonts();
		for (int i=0;i<fonts.length;i++)
			System.out.println(""+i+fonts[i]);

		System.out.println(Toolkit.getDefaultToolkit().getScreenResolution());
		System.out.println(Toolkit.getDefaultToolkit().getScreenSize());
		System.out.println(Toolkit.getDefaultToolkit().getScreenInsets(ge.getDefaultScreenDevice().getDefaultConfiguration()));
		System.out.println(ge.getCenterPoint());
		System.out.println(ge.getMaximumWindowBounds());
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		System.out.println(gd.getAvailableAcceleratedMemory());
		System.out.println(gd.getIDstring());
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		System.out.println(gc.getBounds());
		System.out.println(gc.getColorModel());
		System.out.println(gc.getDefaultTransform());
		System.out.println(gc.getNormalizingTransform());
		AffineTransform at = gc.getNormalizingTransform();
		System.out.println(at.getScaleX()*72.0);
		System.out.println(at.getScaleY()*72.0);
		BufferedImage i = new BufferedImage(800,600,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = i.createGraphics();
		//		at = g.getDeviceConfiguration().getNormalizingTransform();
		//		System.out.println(at.getScaleX()*72.0);
		//		System.out.println(at.getScaleY()*72.0);
		g.setColor(Color.yellow);
		g.fillRect(0, 0, 800, 600);
		g.setColor(Color.BLACK);
		g.draw(new Rectangle2D.Float(0,0,199,199));
		g.setTransform(g.getDeviceConfiguration().getDefaultTransform());
		g.transform(at);
		g.setBackground(Color.WHITE);
		g.setColor(Color.red);
		g.draw(new Rectangle2D.Float(0,0,199,199));
		g.draw(new Line2D.Float(100,100,100,172));
		g.draw(new Line2D.Float(100,100,172,100));
		//g.setColor(Color.blue);
		//g.drawRect(0,0,199,199);
		ImageIO.write(i, "png", new File("test.png"));
		//System.out.println(gc.);
		//MouseInfo.getPointerInfo().getDevice()

		System.out.println(Locale.getDefault());

	}

}
