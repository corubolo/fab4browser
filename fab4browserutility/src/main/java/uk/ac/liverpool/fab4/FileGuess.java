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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * FileGuess provides the static   method guess
 * This simple method tries to guess the file mime type from the start bytes
 * of the file. Returns the myme type or null if it was unable to guess.
 * @version 1.0
 */
public class FileGuess {

	/**
	 * The magic number starts with his position in the vector of string below,
	 * so we can then sort the array by length (reversed) in order to look for
	 * the right file type starting from the longers magic number (that is more
	 * specific).
	 */
	private static byte[][] types = new byte[][] {
		{ 0, 0x25, 0x50, 0x44, 0x46, 0x2D },
		{ 1, (byte) 137, 80, 78, 71, 13, 10, 26, 10 },
		{ 2, 0x47, 0x49, 0x46, 0x38 },
		{ 3, (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1,
			(byte) 0xB1, (byte) 0x1A, (byte) 0xE1 },
			{ 4, 0x25, 0x21, 0x50, 0x53 },
			{ 5, 0x5B, 0x61, 0x3B, 0x22, 0x58, 0x44, 0x4F, 0x43, 0x2E },
			{ 6, 0x50, 0x4B, 0x03, 0x04 }, { 7, (byte) 0xF7, 0x2 },
			{ 8, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF },
			/* { 9, 0x00 , 0x01 , 0x00 , 0x00 , 0x00}, */

	};
	// 50 4B 03 04 14 00 08 00 08 00 JAR files
	// [a;"XDOC. xdoc??
	/**
	 * The myme type vector MUST be in the same order as the magic numbers above
	 * here!!
	 */
	private static String[] mimeTypes = new String[] { "application/pdf",
		"image/png", "image/gif", "office",
		"application/postscript", "application/vnd.x-doc",
		"application/zip", "application/x-dvi", "image/jpeg", /* "font/truetype" */};
	/**
	 * this contains the map (mime type) -> (list of file extensions) All of
	 * this is lowercase (taken from /etc/mime.types )
	 */
	private static HashMap<String, List<String>> mimes;
	private static List magics;
	/** minimum number of bytes needed by the Guess method */
	static public int minBytes = 0;
	/* here we sort the array by length and get the longest */

	static {
		Arrays.sort(FileGuess.types, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return ((byte[]) arg1).length - ((byte[]) arg0).length;
			}
		});
		// 1st = longest
		FileGuess.minBytes = Math.max(FileGuess.types[0].length - 1, 150);
	}

	/**
	 * This simple method tries to guess the file mime type from the start bytes
	 * of the file. Returns the myme type or null if it was unable to guess.
	 */
	public static String guess(byte[] start, String extension) {
		if (start.length < FileGuess.minBytes) {
			System.out.println("We need at least " + FileGuess.minBytes
					+ " bytes, got only " + start.length);
			return null;
		}
		String s = new String(start);
		String sl = s.toLowerCase();
		if (sl.startsWith("<?xml")) {
			if (sl.toLowerCase().indexOf("<!doctype svg") != -1)
				// System.out.println("sss");
				return "image/svg+xml";
			return "text/xml";

		}
		if (sl.indexOf("<html") > 0 || sl.indexOf("<head") > 0
				|| sl.indexOf("<title") > 0 || sl.indexOf("<!--") > 0
				|| sl.indexOf("<h1") > 0 || sl.indexOf("<!doctype html") > 0)
			return "text/html";
		if (s.startsWith("ID3"))
			return "audio/mpeg";
		if (s.startsWith("OggS"))
			return "application/ogg";
		if (s.startsWith("AT&TFORM"))
			return "image/x.djvu";
		if (s.startsWith("{\\rtf"))
			// if (s.startsWith("{\rtf"))
			return "text/rtf";
		// "%!PS-Adobe-N.n PDF-M.m" -> PDF
		if (s.startsWith("%!PS-Adobe-") && s.substring(15, 18).equals("PDF-"))
			return "application/pdf";
		if (s.startsWith("RIFF")) {
			byte[] bb = new byte[4];
			System.arraycopy(start, 8, bb, 0, 4);
			String ss = new String(bb);
			if (ss.startsWith("WAVE"))
				return "audio/x-wav";
			if (ss.startsWith("AVI"))
				return "video/x-msvideo";
		}
		if (new String(start, 0, 2).equals("PK")
				&& new String(start, 30, 8).equals("mimetype")) {
			String soo = new String(start, 38, FileGuess.minBytes - 38);
			if (soo.indexOf("PK") >= 0)
				return soo.substring(0, soo.indexOf("PK"));
		}

		// MP3 has 11 bit set to 1
		if (start.length > 3)
			if (start[0] == 0xff && start[1] == 0xff && (start[2] & 0x01)==0xFE)
				return "audio/mp3";
		for (byte[] type : FileGuess.types) {
			if (start.length < type.length - 1)
				continue;
			boolean found = true;
			// the 1st of types is the position in the string vector
			for (int k = 1; k < type.length; k++)
				if (type[k] != start[k - 1]) {
					found = false;
					break;
				}
			if (found){
				// special case for office file formats
				if (type[0]==3)
					if (extension.toLowerCase().endsWith("ppt"))
						return "application/vnd.ms-powerpoint";
					else if (extension.toLowerCase().endsWith("doc"))
						return "application/msword";
					else if (extension.toLowerCase().endsWith("xls"))
						return"application/vnd.ms-excel";

				// the 1st int is the position in the string vector
				return FileGuess.mimeTypes[type[0]];
			}
		}
		return null;
	}

	/**
	 * This simple method tries to guess the file extension from mime type,
	 * using the table normally in the file /etc/mime.types in Linux. The file
	 * mime.types must be accessible in /data/mime.types
	 */
	public static String guessExtension(String mimeType) {
		if (FileGuess.mimes == null)
			loadMimes();
		if (FileGuess.mimes == null)
			return null;
		List exts = FileGuess.mimes.get(mimeType);
		if (exts != null)
			if (exts.size() > 0)
				return (String) exts.get(0);
		return null;
	}

	/* loads the mime types table */
	private synchronized static void loadMimes() {
		if (FileGuess.mimes != null)
			return;
		InputStream is = FileGuess.mimeTypes.getClass().getResourceAsStream(
		"/data/mime.types");
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		FileGuess.mimes = new HashMap<String, List<String>>(30);
		String line;
		try {
			while ((line = r.readLine()) != null) {
				if (line.startsWith("#"))
					continue;
				StringTokenizer st = new StringTokenizer(line, " \t", false);
				if (!st.hasMoreTokens())
					continue;
				String key = st.nextToken();
				List<String> vals = new LinkedList<String>();
				if (!st.hasMoreTokens()) {
					FileGuess.mimes.put(key, vals);
					continue;
				}
				while (st.hasMoreTokens())
					vals.add(st.nextToken());
				FileGuess.mimes.put(key, vals);
			}
		} catch (IOException e) {
			e.printStackTrace();
			FileGuess.mimes = null;
		} finally {
			try {
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private synchronized static void loadFileMagic() {
		if (FileGuess.magics != null)
			return;
		InputStream is = FileGuess.mimeTypes.getClass().getResourceAsStream(
		"/data/magic.mime");
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		FileGuess.magics = new ArrayList(3000);
		String line;
		try {
			while ((line = r.readLine()) != null) {
				if (line.startsWith("#"))
					continue;
				StringTokenizer st = new StringTokenizer(line, " \t", false);
				if (!st.hasMoreTokens())
					continue;
				// System.out.println(line);
				String col1 = st.nextToken();
				String col2 = st.nextToken();
				String col3 = st.nextToken("\t");
				String col4 = "";
				try {
					col4 = st.nextToken(" \t");
				} catch (NoSuchElementException e) {
				}
				System.out.print(col1 + "\t|");
				System.out.print(col2 + "\t\t\t|");
				System.out.print(col3 + "\t\t\t|");
				System.out.print(col4 + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			FileGuess.mimes = null;
		} finally {
			try {
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) {
		loadFileMagic();
	}

	public static class Magic {
		public short offset;
		public byte type;
		public byte[] magic;
		public String mimeType;
	}
}
