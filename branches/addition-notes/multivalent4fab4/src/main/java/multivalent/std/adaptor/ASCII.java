/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent.std.adaptor;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import multivalent.*;
import multivalent.IDInfo.Confidence;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.LeafUnicode;
import uk.ac.liverpool.fab4.CharsetIdentifier;



/**
 * Media adaptor for plain old ASCII files (.txt => doc tree) groups lines into heuristically determined paragraphs (two styles of paragraph: blank line between and indended) MUST RETAIN SPACES, WHICH
 * THIS PRESENTLY DOESN'T.
 * 
 * @version $Revision$ $Date$
 */
public class ASCII extends MediaAdaptor {
    // static final boolean DEBUG = false;

    private static final Map<String, String> TEXT_SUFFIXES;

    static {
        TEXT_SUFFIXES = new HashMap<String, String>();
        TEXT_SUFFIXES.put("txt", "text/plain");
    }

    /**
     * Fabio: this assumes that the identification of text is already happening
     * elsewhere (file) and focuses on detecting the codepage and confidence.
     *
     * @param min
     * @param max
     *
     * @return IDInfo object with mime type = text/plain; charset=detected
     *         codepage
     */
    @Override
    public SortedSet<IDInfo> getTypeInfo(Confidence min, Confidence max,
            String path, boolean complete) throws IOException {

        SortedSet<IDInfo> infos = validateParams(min, max);
        if (inRange(min, Confidence.HEURISTIC, max)) {
            String ct = getInputUni().getContentType();
            InputStream is = getInputUni().getInputStreamRaw();
            if (is == null)
                is = getInputUni().getInputStream();
            CodepageDetector.Match m = CodepageDetector.detectCodepage(is, ct,
                    false);

            // it is not really simple to identify binary files, as different multibyte encoding use contol characters as well.
            // For now, this is based on the confidence in the codepage identification;
            // as a second possiblity, one could use the result of the file command; and see it's a text mime time
            // see also http://stackoverflow.com/questions/277521/how-to-identify-the-file-content-is-in-ascii-or-binary
            if (m != null && m.confidence > 0.20) {
                IDInfo i = new IDInfo(Confidence.HEURISTIC, this,
                        "text/plain; charset=" + m.encoding);
                infos.add(i);
            }
        } else if (inRange(min, Confidence.SUFFIX, max)) {
            String type = lookupSuffix(path, TEXT_SUFFIXES);
            if (type != null)
                infos.add(new IDInfo(Confidence.SUFFIX, this, type));
        }
        return infos;
    }

	
	@Override
	public Object parse(INode parent) throws Exception {
		Browser br = getBrowser();
		Document doc = parent.getDocument();
		doc.getStyleSheet().getContext(null, null).zoom = getZoom();

		INode ascii = new IVBox("ascii", null, parent); // always return this, maybe with no children

		INode paraNode = null;
		INode lineNode = null;
		LeafUnicode wordLeaf = null;
		boolean startpara = true;
		int prcnt = 0;
		Reader r  = null;
		try {
			 String ct = getInputUni().getContentType();
		      InputStream is = getInputUni().getInputStreamRaw();
		      if (is == null)
		        is = getInputUni().getInputStream();
		      r = CharsetIdentifier.getReader(is, ct, false);
		} catch (Exception e){ 
			e.printStackTrace();
			 r = new InputStreamReader(getInputUni().getInputStreamRaw(), "UTF-8");
		}
		BufferedReader ir = new BufferedReader(r);
		String line;
		while ((line = ir.readLine()) != null) {
			StringBuffer sb = new StringBuffer(line);
			boolean blankline = (sb.length() == 0);
			if (blankline) {
				if (paraNode != null) {
					lineNode = new IParaBox("Line", null, paraNode); // IHBox?
					new LeafUnicode(" ", null, lineNode);
				}
				startpara = true;
				continue;
			}

			if (startpara) {
				paraNode = new IVBox("para", null, ascii);
				startpara = false;
			}
			lineNode = new IParaBox("Line", null, paraNode); // IHBox?
			int start = 0;
			for (int i = 0, imax = sb.length(); i < imax; i++) {
				char wch = sb.charAt(i);
				if (wch == '\t') { // tabs get own word
					if (i > start)
						wordLeaf = new LeafUnicode(sb.substring(start, i), null, lineNode);
					wordLeaf = new LeafUnicode(sb.substring(i, i + 1), null, lineNode);
					start = i + 1;
				} else if (wch == ' ') {
					if (i > start) {
						String txt = (i - start == 1 ? phelps.lang.Strings.valueOf(sb.charAt(start)) : sb.substring(start, i));
						wordLeaf = new LeafUnicode(txt, null, lineNode);
					}
					start = i + 1;
				}
//				else if (!Character.isDefined(wch)) {
//					sb.deleteCharAt(i);
//					imax--;
//				}
			}
			if (start < sb.length()) {
				wordLeaf = new LeafUnicode(sb.substring(start), null, lineNode);
			}
			if (++prcnt > 100 && br != null) {
				br.paintImmediately(br.getBounds());
				prcnt = 0;
			}
		}
		ir.close();

		return ascii;
	}
}
