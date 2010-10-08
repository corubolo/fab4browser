package uk.ac.liverpool.thumbnails;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import org.apache.poi.hslf.HSLFSlideShow;
import org.apache.poi.hslf.model.Comment;
import org.apache.poi.hslf.model.HeadersFooters;
import org.apache.poi.hslf.model.Notes;
import org.apache.poi.hslf.model.Slide; 
import org.apache.poi.hslf.model.TextRun;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.jdom.Element;
import org.jdom.IllegalDataException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class PPTService {

    public static String extraxtXMLText(URI u) throws MalformedURLException,
            IOException {
        SlideShow ppt;

        HSLFSlideShow hppt = new HSLFSlideShow(u.toURL().openStream());
        ppt = new SlideShow(hppt);
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

    public static BufferedImage generatePPTThumb(URI u, int w, int h)
            throws MalformedURLException, IOException {
        // retrieve the slides from the docuemnt attributes
        HSLFSlideShow hppt = null;
        Slide[] slide = null;
        // the slides are saved to a document attribute

        SlideShow ppt;

        hppt = new HSLFSlideShow(u.toURL().openStream());
        ppt = new SlideShow(hppt);

        // and memorise it as attribute to the document

        slide = ppt.getSlides();

        int i = 0;
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

}
