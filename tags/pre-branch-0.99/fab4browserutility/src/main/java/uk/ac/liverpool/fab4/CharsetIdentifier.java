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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class CharsetIdentifier {

	public static Reader getReader(InputStream is, String ct, boolean isMarkup) throws IOException {

		if (ct!=null){
			ct = getCharset(ct);
		}
		CharsetDetector cd = new CharsetDetector();
		cd.enableInputFilter(isMarkup);
		if (is== null) return null;
		BufferedInputStream p = new BufferedInputStream(is, 8500);
		if (ct == null) {
			byte[] b = new byte[8000];
			p.mark(8500);
			p.read(b);
			p.reset();
			String s = new String (b);
			ct = getCharset(s);
		}
		if (ct!=null){
			cd.setDeclaredEncoding(ct);
			System.out.println("Declared encoding: " + ct);
			return new InputStreamReader(p, ct);
		}
		cd.setText(p);
		CharsetMatch m = cd.detect();
		Reader r = m.getReader();
		if (r == null)
			r = new InputStreamReader(p, "UTF-8");
		System.out.println("Detected encoding " + m.getName());
		return r;
	}


	private static String getCharset(String ct) {
		Pattern p = Pattern.compile("charset\\s*=\\s*([\"'])?([^ \"'>]*)");
		Matcher m = p.matcher(ct);
		if (m.find()) {
		  ct = m.group(2);
		} else
			ct = null;


//		int c = ct.toLowerCase().indexOf("charset");
//		if (c != -1){
//			c = ct.indexOf("=", c);
//			c++;
//			if (c!=-1){
//				int cc = ct.indexOf(';',c);
//				if (cc==-1)
//					cc = ct.indexOf('"',c);
//				if (cc==-1)
//					cc = ct.indexOf('>',c);
//				if (cc==-1)
//					cc = ct.indexOf(' ',c);
//				if (cc==-1)
//					cc=ct.length();
//				ct = ct.substring(c+1, cc).replaceAll("\"", "");
//			}}
//		else ct = null;
		return ct;
	}

}
