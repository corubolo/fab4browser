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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.apache.poi.hslf.model.PPFont;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.freehep.graphicsio.emf.EMFViewer;

/**
 * 
 * @author Fabio Corubolo
 * 
 * Test class to extract and view images in pict, emf, and wmf formats;
 *
 */


public class ExtractFonts {

	private static final boolean doPict = false;
	private static final boolean doEmf = false;
	JFrame f;
	JButton b;
	EMFViewer v;
	HashMap<String, Integer>m = new HashMap<String, Integer>(1000);

	final Semaphore s = new Semaphore(1);

	public ExtractFonts() {
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
		SortedMap<String, Integer> map = new TreeMap<String, Integer>( new ExtractFonts().listFiles(d, recurse, 0));
		Set<Entry<String, Integer>> e = map.entrySet();
		for (Entry<String, Integer>p:e ){
			System.out.println(p.getKey() + " : "+ p.getValue());
		}
	}

	public HashMap listFiles(File d, boolean recurse, int type) {
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
					for (int i=0;i<ppt.getNumberOfFonts();i++){
						PPFont f = ppt.getFont(i);
						String fid = f.getFontName();
						int k;
						if (m.get(fid) != null){
							k= m.get(fid);
						} else k=0;
						k++;
						m.put(fid, k);
					}
				} catch (Exception e) {
					System.err.println("* On file: " + element);
					e.printStackTrace();
				}

			}
		}
		return m;

	}
	
}
