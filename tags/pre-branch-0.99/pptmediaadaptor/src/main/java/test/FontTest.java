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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.AttributedString;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;

public class FontTest {

	
	public static void main(String[] args) throws IOException {
		Font f = new Font("Wingdings",Font.PLAIN, 18);
		final BufferedImage i = new BufferedImage(1500, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = i.createGraphics();
		//g.setBackground(Color.white);
		//g.setColor(Color.black);
		for (String s:GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
			System.out.println(s);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		System.out.println(f.canDisplay('w'));
		FontRenderContext fr = g.getFontRenderContext();
		System.out.println(fr.getTransform());
		
		System.out.println(f.getBaselineFor('w'));
		System.out.println(System.getProperty("java.version"));
		System.out.println(System.getProperty("java.vendor"));
		System.out.println(f.getNumGlyphs());
		System.out.println(f.getFamily());
		System.out.println(f.getFontName());
		System.out.println(f.getPSName());
	
		int[] gl = new int[f.getNumGlyphs()];
		for (int k = 0; k <gl.length;k++){
			gl[k] = k;
		}
		GlyphVector v = f.createGlyphVector(fr, gl);
		System.out.println(v.getGlyphCharIndex('w'));
		System.out.println(v.getGlyphCode('w'));
		g.drawGlyphVector(v, 0, 70);
		AttributedString bat = new AttributedString(Character.toString((char)((0xF000 + 'w'))));
		bat.addAttribute(TextAttribute.FONT, f);
		g.drawString(bat.getIterator(), 3, 15);
		ImageIO.write(i, "JPEG", new File("font.jpg"));
		JFrame frame = new JFrame();
		JButton b = new JButton(new Icon() {

			public void paintIcon(Component c, Graphics g, int x, int y) {
				g.drawImage(i, x, y, null);
			}

			public int getIconWidth() {
				// TODO Auto-generated method stub
				return i.getWidth();
			}

			public int getIconHeight() {
				// TODO Auto-generated method stub
				return i.getHeight();
			}
		});
	frame.getContentPane().add(b);
		frame.pack();
		frame.setVisible(true);
	}
}
