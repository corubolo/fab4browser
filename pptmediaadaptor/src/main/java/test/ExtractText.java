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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.util.concurrent.Semaphore;

import uk.ac.liverpool.MSOffice.MSPowerpoint;
import uk.ac.liverpool.MSOffice.MSPowerpoint.ExtractionFeature;

/**
 * 
 * @author Fabio Corubolo
 * 
 * Test class to extract and view images in pict, emf, and wmf formats;
 *
 */


public class ExtractText {

	private static final boolean doPict = false;
	private static final boolean doEmf = false;
	private static final String ssem = ".meta";
	final Semaphore s = new Semaphore(1);

	public ExtractText() {

	}

	/* Utility to extract all text from ppt files */
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
		ExtractText.listFiles(d, recurse, 0);
		
	}

	public static void listFiles(File d, boolean recurse, int type) {
		MSPowerpoint pp = new MSPowerpoint();
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
					String xml = pp.extractFeature(fis, ExtractionFeature.METADATA);
					Writer w = new FileWriter(new File(
							element.getAbsoluteFile() +ssem));
					try {
						w.write(xml);
					} catch (Exception e) {
						System.err.println("On file: " + element);
						e.printStackTrace();
					}
					w.close();

				} catch (Exception e) {
					System.err.println("* On file: " + element);
					e.printStackTrace();
				

				}
			}
		
		}


	}

}
