/*******************************************************************************
 * This Library is :
 * 
 *     Copyright © 2010 Fabio Corubolo - all rights reserved
 *     corubolo@gmail.com
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * see COPYING.LESSER.txt
 * 
 ******************************************************************************/
package uk.ac.liverpool.thumbnails;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.LinkedList;

import org.apache.poi.hslf.HSLFSlideShow;
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

public class PPTService implements GenericService {
    
    
    public FontInformation[] extractFontList(URI u, File fff) throws MalformedURLException, IOException {
        
        HSLFSlideShow hppt = getHPPT(u,fff);
        SlideShow ppt = new SlideShow(hppt);
        LinkedList<FontInformation> ll = new LinkedList<FontInformation>();
        for (int i = 0; i < ppt.getNumberOfFonts(); i++) {
            PPFont f = ppt.getFont(i);
            FontInformation ff = new FontInformation();
            ff.charset = ""+ f.getCharSet();
           ff.fontFlags = f.getFontFlags();
           ff.fontName = f.getFontName();
           ff.fontType = ""+ f.getFontType();
           ff.pitchAndFamily = ""+ f.getPitchAndFamily();

        }
        return ll.toArray(new FontInformation[0]);
    }
    
    public  String extractXMLText(URI u, File f) throws MalformedURLException,
            IOException {
       

        HSLFSlideShow hppt = getHPPT(u, f);
        SlideShow ppt = new SlideShow(hppt);
        Element root = new Element("Slideshow");
        root.setAttribute("URI", u.toString());
        Slide[] slides = ppt.getSlides();
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
                comment.setAttribute("authorInitials",
                        comments[j].getAuthorInitials());
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

    public BufferedImage generateThumb(URI u, File f, int w, int h, int pn)
            throws MalformedURLException, IOException {
        // retrieve the slides from the docuemnt attributes
        HSLFSlideShow hppt = null;

        // the slides are saved to a document attribute

        SlideShow ppt;

        hppt = getHPPT(u, f);
        ppt = new SlideShow(hppt);

        // and memorise it as attribute to the document
        Slide[] slide = null;
        slide = ppt.getSlides();

        int i = 0;
        if (pn < slide.length)
            i = pn;
        String title = slide[i].getTitle();
        System.out.println("Rendering slide " + slide[i].getSlideNumber()
                + (title == null ? "" : ": " + title));
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        // create the slide object and add it to the document
        Graphics2D graphics = (Graphics2D) bi.getGraphics();
        Dimension pgsize = slide[i].getSlideShow().getPageSize();
        double zoom = ((double)w / (double)pgsize.width);
        graphics.setPaint(Color.white);
        graphics.fill(new Rectangle2D.Float(0, 0, w, h));
        graphics.scale(zoom, zoom);
        
        FontRenderContext frx = graphics.getFontRenderContext();
        
        try {
            slide[i].draw(graphics);
        } catch (Exception e) {
            e.printStackTrace();
        }
        graphics.dispose();
        return bi;

    }

    private static HSLFSlideShow getHPPT(URI u, File f) throws IOException,
            MalformedURLException {
        HSLFSlideShow hppt;
        if (f!=null) {
            hppt = new HSLFSlideShow(new FileInputStream(f));
        } else 
        hppt = new HSLFSlideShow(u.toURL().openStream());
        return hppt;
    }

    @Override
    public String[] getSupportedMimes() {
        return new String[]{PPT_MIME};
    }


    static final String PPT_MIME = "application/vnd.ms-powerpoint";


    @Override
    public void generateSVG(URI u, File f, int w, int h, int page, Writer out)
            throws MalformedURLException, IOException {
        // TODO Auto-generated method stub
        return 
;
    }
    
}
