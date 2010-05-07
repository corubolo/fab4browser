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
package uk.ac.liverpool.MSOffice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;

import multivalent.Behavior;
import multivalent.Document;
import multivalent.IDInfo;
import multivalent.INode;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.Node;
import multivalent.IDInfo.Confidence;
import multivalent.node.LeafUnicode;
import multivalent.std.adaptor.HTML;

import org.apache.poi.hslf.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.pt.io.InputUni;

public class MSWord extends MediaAdaptor{

    private static final String MIME = "application/msword";

    @Override
    public SortedSet<IDInfo> getTypeInfo(Confidence min, Confidence max,
            String path, boolean complete) throws IOException {

        SortedSet<IDInfo> infos = validateParams(min, max);
        System.out.println(path);
        if (inRange(min,Confidence.SUFFIX, max)) {
            if (path != null && path.toLowerCase().endsWith("doc")) {
                infos.add(new IDInfo(Confidence.SUFFIX, this, MIME));
            }
        } else if (inRange(min,Confidence.PARSE, max)) {
            InputUni uni = getInputUni();
            try {
                POIFSFileSystem pfs = new POIFSFileSystem(uni.getInputStream());
                new HWPFDocument(pfs);
                infos.add(new IDInfo(Confidence.PARSE, this, MIME));
            } catch (Exception x) {
                x.printStackTrace();
            }

        }

        return infos;
    }

	@Override
	public Object parse(INode parent) throws Exception {

		Document doc = parent.getDocument();
//		final StyleSheet ss = doc.getStyleSheet();
//		CLGeneral gs = new CLGeneral();
//		gs.setForeground(Colors.getColor(getAttr("foreground"), Color.BLACK));
//		gs.setBackground(Colors.getColor(getAttr("background"), Color.LIGHT_GRAY));
//		gs.setPadding(8);
//		ss.put(doc.getName(), gs);
		InputUni uni = getInputUni();
		HWPFDocument wor = (HWPFDocument) doc.getValue("worddoc");
		if (wor == null ){
			System.out.println("new word extractor");
			POIFSFileSystem pfs = new POIFSFileSystem(uni.getInputStreamRaw());
			wor = new HWPFDocument(pfs);
			doc.putAttr("worddoc", wor);
		}

		return parseHelper(toHTML(parent), "HTML", getLayer(), parent);
	}

	public static Object parseHelper(String txt, String adaptor, Layer layer,
            INode parent) {
		HTML helper = (HTML) Behavior.getInstance("helper",
                adaptor, null, layer);
        Node root = null;
        try {
            helper.setInput(txt);
            root = (Node) helper.parse(parent);
        } catch (Exception e) {
            new LeafUnicode("ERROR " + e, null, parent);
            e.printStackTrace();
        } finally {
            try {
                helper.close();
            } catch (IOException ioe) {
            }
        }

        return root;
    }

	
	
	private String toHTML(INode parent) {
		HWPFDocument wor = (HWPFDocument) parent.getDocument().getValue("worddoc");
		WordExtractor wx = new WordExtractor(wor);
		StringBuilder b = new StringBuilder();

		b.append("<html><head>" +
				"<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
				"<style type=\"text/css\">\n" + 
				"body {\n" + 
				"	color: black; background-color: white;\n" + 
				"	font-size: 14pts;\n" + 
				"	padding: 10px;}\n" +
				"\n" + 
				"a:link { color: blue; }\n" + 
				"a:visited { color: magenta; }\n" + 
				"a:hover { color: red; }\n" + 
				"a:active { color: red; }\n" + 
				"\n" + 
				"a:link, a:visited, \n" + 
				"a:active, a:hover {\n" + 
				"	text-decoration: underline;\n" + 
				"}\n" + 
				"\n" + 
				"p {\n" + 
				"	margin-top: 10px;\n" + 
				"}\n" +
				"text { padding: 5px; }\n" + 
				"\n" + 
				"pre { font-family: monospace; }\n" + 
				"\n\n" + 
				"h1 { font-size: 24pt; font-weight: bold; margin: 10px 0px; }\n" + 
				"h2 { font-size: 18pt; font-weight: bold; margin: 9px 0px; }\n" + 
				"h3 { font-size: 14pt; font-weight: bold; margin: 7px 0px; }\n" + 
				"h4 { font-size: 12pt; font-weight: bold; margin: 6px 0px; }\n" + 
				"h5 { font-size: 10pt; font-weight: bold; margin: 5px 0px; }\n" + 
				"h6 { font-size:  9pt; font-weight: bold; margin: 5px 0px; }\n" + 
				"" + 
				"" + 
		"</style>");
		b.append("<title>").append("Text extracion contents of the word document (APACHE POI):").append("</title>");
		b.append("</head>\n");
		b.append("<body>\n");
		b.append("<p>").append(wx.getHeaderText()).append("</p>\n");
		ArrayList<String> text = new ArrayList<String>();
		text.addAll(Arrays.asList(wx.getParagraphText()));
		text.addAll(Arrays.asList(wx.getFootnoteText()));
		text.addAll(Arrays.asList(wx.getEndnoteText()));

		for(String p : text) {
			b.append("<p>").append(p).append("</p>\n");
		}
		b.append("</body></html>");
		return b.toString();
	}


}
