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

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import multivalent.Behavior;
import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.IDInfo;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.MediaAdaptor;
import multivalent.StyleSheet;
import multivalent.IDInfo.Confidence;
import multivalent.node.LeafText;

import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hpsf.wellknown.PropertyIDMap;
import org.apache.poi.hslf.HSLFSlideShow;
import org.apache.poi.hslf.blip.BitmapPainter;
import org.apache.poi.hslf.blip.VectorPainter;
import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.apache.poi.hslf.model.Comment;
import org.apache.poi.hslf.model.HeadersFooters;
import org.apache.poi.hslf.model.Notes;
import org.apache.poi.hslf.model.PPFont;
import org.apache.poi.hslf.model.Slide;
import org.apache.poi.hslf.model.TextRun;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.jdom.Element;
import org.jdom.IllegalDataException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import phelps.awt.Colors;
import uk.ac.liverpool.fab4.ImageInternalDataFrame;

import com.pt.io.InputUni;

/**
 * The MS Powerpoint media engine. Based on the Apache POI library, it adds
 * features to improve the rendering of the slides. Supports Powerpoint 97-2003.
 * Adds support for PICT, EMF and WMF using external libraries.
 * 
 * @author Fabio Corubolo
 * 
 */

public class MSPowerpoint extends MediaAdaptor {

    private static final String MIME = "application/vnd.ms-powerpoint";

    public enum ExtractionFeature {
        XMLTEXT, SIMPLETEXT, SIMPLENOTES, SVGGRAPHICS, METADATA
    }

    private float prevZoom;

    @Override
    /**
     * 
     */
    public Object parse(INode parent) throws Exception {
        // Set up the context for drawing
        Document doc = parent.getDocument();
        final StyleSheet ss = doc.getStyleSheet();
        CLGeneral gs = new CLGeneral();
        gs.setForeground(Colors.getColor(getAttr("foreground"), Color.BLACK));
        gs.setBackground(Colors.getColor(getAttr("background"),
                Color.LIGHT_GRAY));
        gs.setPadding(8);
        ss.put(doc.getName(), gs);

        // parse the slideshow
        InputUni uni = getInputUni();
        // the slideshow could be already parsed as this method is called page
        // by page.
        // slideshow object is savetd to an attribute to avoid re parsing
        SlideShow ppt = (SlideShow) doc.getValue("ppt");
        HSLFSlideShow hppt = (HSLFSlideShow) doc.getValue("hppt");
        Slide[] slide = null;
        // the slides are saved to a document attribute
        if (doc.getValue("slides") != null) {
            slide = (Slide[]) doc.getValue("slides");
        }
        if (doc.getValue("enc") != null)
            return new LeafText("The presentation is password protected", null,
                    doc);
        // if the slideshow has not been parsed already,we parse it here
        if (ppt == null) {
            try {
                hppt = new HSLFSlideShow(uni.getInputStreamRaw());
                ppt = new SlideShow(hppt);
            } catch (org.apache.poi.hslf.exceptions.EncryptedPowerPointFileException e) {
                System.out.println("Encrypted presentation!");
                doc.putAttr("enc", Boolean.TRUE);
                return new LeafText("The presentation is password protected",
                        null, doc);
            }
            // and memorise it as attribute to the document
            doc.putAttr("ppt", ppt);
            doc.putAttr("hppt", hppt);
            slide = ppt.getSlides();
            doc.putAttr("slides", slide);

            // experiments
            for (int i = 0; i < ppt.getNumberOfFonts(); i++) {
                PPFont f = ppt.getFont(i);
                System.out.println("++++ " + f.getCharSet() + " - "
                        + f.getFontFlags() + " - " + f.getFontName() + " - "
                        + f.getFontType() + " - " + f.getPitchAndFamily());

            }

        }

        // save also the number of pages
        doc.putAttr(Document.ATTR_PAGECOUNT, "" + slide.length);
        // System.out.println(extractFeature(ExtractionFeature.XMLTEXT));
        // Draw the requested page (or 1 by default
        return drawPage(doc.getAttr(Document.ATTR_PAGE, "1"), doc);

    }

    public String extractFeature(InputStream is, ExtractionFeature f)
            throws IOException {
        HSLFSlideShow ppth = new HSLFSlideShow(is);
        SlideShow ppt = new SlideShow(ppth);
        return extractFeature(ppt, ppth, f);

    }

    public String extractFeature(ExtractionFeature f) throws IOException {
        HSLFSlideShow ppth = (HSLFSlideShow) getDocument().getValue("hppt");
        SlideShow ppt = (SlideShow) getDocument().getValue("ppt");

        return extractFeature(ppt, ppth, f);
    }

    private String extractFeature(SlideShow s, HSLFSlideShow hppt,
            ExtractionFeature f) throws IOException {
        PowerPointExtractor p;
        switch (f) {

        case SIMPLETEXT:
            p = new PowerPointExtractor(hppt);
            return p.getText(true, false);
        case SIMPLENOTES:
            p = new PowerPointExtractor(hppt);
            return p.getText(false, true);
            // break;
        case SVGGRAPHICS:
            throw new IOException("Unimplemented");
        case XMLTEXT:
            return extraxtXMLText(s);
            // break;
        case METADATA:
            p = new PowerPointExtractor(hppt);
            return p.getMetadataTextExtractor().getText();
        default:
            break;
        }
        return null;
    }

    private String extraxtXMLText(SlideShow ppt) {
        Slide[] slides = ppt.getSlides();
        Element root = new Element("Slideshow");
        if (getDocument() != null)
            if (getDocument().getURI() != null)
                root.setAttribute("URI", getDocument().getURI().toString());
        for (Slide s : slides) {
            Element slide = new Element("Slide");
            if (s.getTitle() != null)
                slide.setAttribute("title", s.getTitle());
            slide.setAttribute("slideNumber", "" + s.getSlideNumber());

            HeadersFooters hf = s.getHeadersFooters();
            if (hf != null && hf.isHeaderVisible()
                    && hf.getHeaderText() != null) {
                Element header = new Element("Header");
                header.addContent(hf.getHeaderText());
                slide.addContent(header);
            }

            TextRun[] runs = s.getTextRuns();
            StringBuilder ret = new StringBuilder();
            for (int j = 0; j < runs.length; j++) {
                TextRun run = runs[j];
                if (run != null) {
                    String text = run.getText();
                    ret.append(text);
                    if (!text.endsWith("\n")) {
                        ret.append("\n");
                    }
                }
            }
            try {
                slide.addContent(ret.toString());
            } catch (IllegalDataException x) {
                for (int i = 0; i < ret.length(); i++) {
                    char c = ret.charAt(i);
                    if (c < 0x20) {
                        if (c != 0x9 && c != 0xA && c != 0xD)
                            ret.deleteCharAt(i);
                    }
                }

            }
            slide.addContent(ret.toString());
            if (hf != null && hf.isFooterVisible()
                    && hf.getFooterText() != null) {
                Element footer = new Element("Footer");
                footer.addContent(hf.getFooterText());
                slide.addContent(footer);
            }
            Comment[] comments = s.getComments();
            for (int j = 0; j < comments.length; j++) {
                Element comment = new Element("Comment");
                comment.setAttribute("author", comments[j].getAuthor());
                comment.setAttribute("authorInitials", comments[j]
                        .getAuthorInitials());
                comment.addContent(comments[j].getText());
                slide.addContent(comment);
            }

            StringBuilder sb = new StringBuilder();
            Notes nn = s.getNotesSheet();
            if (hf != null && hf.isHeaderVisible()
                    && hf.getHeaderText() != null) {
                sb.append(hf.getHeaderText() + "\n");
            }
            if (nn != null) {
                TextRun[] ts = nn.getTextRuns();

                for (TextRun t : ts) {
                    sb.append(t.getText());

                }
            }
            if (hf != null && hf.isFooterVisible()
                    && hf.getFooterText() != null) {
                sb.append(hf.getFooterText() + "\n");
            }
            if (sb != null && sb.length() > 0) {
                Element note = new Element("PresenterNote");
                note.addContent(sb.toString());
                slide.addContent(note);
            }
            root.addContent(slide);
        }
        org.jdom.Document d = new org.jdom.Document(root);
        XMLOutputter p = new XMLOutputter(Format.getPrettyFormat());
        return p.outputString(d);
    }

    /**
     * Draws a specific page in the Powerpoint document. Also takes care of the
     * display of the notes from the slideshow.
     * 
     * @param attr
     * @param doc
     * @return
     */
    private Leaf drawPage(String attr, Document doc) {
        // retrieve the slides from the docuemnt attributes
        SlideShow ppt = (SlideShow) doc.getValue("ppt");
        Slide[] slide = ppt.getSlides();
        // clean the doc
        doc.removeAllChildren();
        // page number
        int i = Integer.parseInt(attr) - 1;
        String title = slide[i].getTitle();
        System.out.println("Rendering slide " + slide[i].getSlideNumber()
                + (title == null ? "" : ": " + title));

        // create the slide object and add it to the document
        LeafSlide l = new LeafSlide("slide", null, doc, slide[i], getZoom());
        if (getZoom() != prevZoom) {
            prevZoom = getZoom();
        }
        HeadersFooters hf = ppt.getNotesHeadersFooters();

        // Interpret the notes
        StringBuilder sb = new StringBuilder();
        Notes nn = slide[i].getNotesSheet();
        if (hf != null && hf.isHeaderVisible() && hf.getHeaderText() != null) {
            sb.append(hf.getHeaderText() + "\n");
        }

        if (nn != null) {
            TextRun[] ts = nn.getTextRuns();

            for (TextRun t : ts) {
                sb.append(t.getText());
            }
        }
        if (hf != null && hf.isFooterVisible() && hf.getFooterText() != null) {
            sb.append(hf.getFooterText() + "\n");
        }
        // Display a preesnter's notes pop up window
        if (sb != null && sb.length() > 0) {
            Layer sc = doc.getLayer(Layer.SCRATCH);
            sc.clearBehaviors();
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("text", sb.toString());
            ImageInternalDataFrame ls = (ImageInternalDataFrame) Behavior
                    .getInstance("ImageInternalDataFrame",
                            "uk.ac.liverpool.fab4.ImageInternalDataFrame",
                            null, m, sc);
            ls.setTitle("Presenter's Notes");
            ls.setTransparent(true);
            ls.setBounds(10, l.getHeight() + 10, l.getWidth() - 20,
                    doc.bbox.height - l.getHeight() - 30);
        }
        return l;
    }

    @Override
    /** 
     * Clean up image caches and resources on close as PPT are parsed in memory.
     */
    public void close() throws IOException {
        super.close();
        getDocument().clearAttributes();
        VectorPainter.clearCache();
        BitmapPainter.clearCache();
        System.gc();

    }

    @Override
    public SortedSet<IDInfo> getTypeInfo(Confidence min, Confidence max,
            String path, boolean complete) throws IOException {

        SortedSet<IDInfo> infos = validateParams(min, max);

        if (inRange(min,Confidence.SUFFIX, max)) {
            if (path != null && path.toLowerCase().endsWith("ppt")) {
                infos.add(new IDInfo(Confidence.SUFFIX, this, MIME));
            }
        } else if (inRange(min,Confidence.PARSE, max)) {
            InputUni uni = getInputUni();
            try {
                HSLFSlideShow hppt = new HSLFSlideShow(uni.getInputStream());
                SummaryInformation s = hppt.getSummaryInformation();
                SlideShow ppt = new SlideShow(hppt);
                ppt.getSlides();
                infos.add(new IDInfo(Confidence.PARSE, this, MIME, s
                        .getApplicationName(), s.getApplicationName()));
            } catch (Exception x) {
                x.printStackTrace();
            }

        }

        return infos;
    }

}
