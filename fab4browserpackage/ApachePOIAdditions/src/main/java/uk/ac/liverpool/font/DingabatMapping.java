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
package uk.ac.liverpool.font;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class DingabatMapping {

	
	public static void main(String[] args) throws IOException {
		
		Font f = new Font("Wingdings",Font.PLAIN, 18);
		Font f2 = new Font("DejaVu Sans",Font.PLAIN, 17);
		final BufferedImage i = new BufferedImage(1500, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = i.createGraphics();
		StringBuilder b = new StringBuilder();
		StringBuilder b2 = new StringBuilder();
		for (char[] d : wding){
			b.append((char)(0xf000 + d[0]));
			b2.append(d[1]);
		}
		g.setFont(f);
		g.drawString(b.toString(), 10, 30);
		g.setFont(f2);
		g.drawString(b2.toString(), 10, 60);
		ImageIO.write(i, "JPEG", new File("font.jpg"));
		
	}
	public enum DingbatFont{WINDING, SORTS};
	
	
	public static char[][] wding = new char[][] {
		{33, 9999},{34,9986 },{35, 9985},{40,9742 },{41, 9990},
		{42,9993 },{54,8987 },{55,9000 },{62, 9991},{63,9997 },
		{65,9996 },{69,9756 },{70,9758 },{71,9757 },{72, 9759},
		{74, 9786},{76, 9785},{78, 9760},{79, 9872},{81,9992 },
		{82,9788 },{84,10052 },{86, 10014},{108,9679 },{109,10061 },
		{110, 9632},{116,10731 },{111,9633 },{117,9670 },{158,183 },
		{159,8226 },{160,9642 },{161, 9675},{167,9642 },{168,9723 },
		{178, 10209},
	};
	public static char[][] monosorts = new char[][] {
		{108,9679 },{109,10061 },
		{110, 9632},{116,10731 },{111,9633 },{117,9670 },
	};
	/**
	 * Shortcut to obtain the Unicode font (DejaVu Sans) that supports all the Dingbats commonly found. 
	 * @return
	 */
	static Font deja = null;
	public static Font getUnicodeFont() {

		if (deja == null)
			try {
				deja = Font.createFont(Font.TRUETYPE_FONT, Thread. currentThread(). getContextClassLoader().getResourceAsStream("DejaVuSans.ttf"));
			} catch (Exception e) {
				deja = new Font("DejaVu Sans",Font.PLAIN, 17);
				e.printStackTrace();
			}
		return deja;
	}
	
	public static char toUnicode(char c,String s){
		if (c>0XF000)
			c-=0xF000;
		DingbatFont d = DingbatFont.WINDING;
		if (s.toLowerCase().equals("wingding"))
			d = DingbatFont.WINDING;
		if (s.toLowerCase().equals("monotype sorts"))
			d = DingbatFont.SORTS;
		switch (d) {
		case WINDING:
			for (char[]w : wding){
				if (c == w[0]) return w[1];
			}
			return 0x25a0;


		default:
			
			return 0x25a0;

		}
		
	}
	
}
