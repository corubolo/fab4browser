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
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfviewer.PageDrawer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.svg.SVGDocument;


/** 
 * 
 * PDF image generatiomn, text extraction based on PDFBOX.
 * @author fabio
 *
 */

public class PDFService implements GenericService {



    public static String[] standard14 = new String[]{"Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic", "Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Helvetica-BoldOblique", "Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique", "Symbol", "ZapfDingbats"};
    static {
        Arrays.sort(standard14);
    }


    public BufferedImage generateThumb(URI u, File f, int w, int h, int pn) throws IOException { 
        PDDocument document = getPages(u,f);
        List pages = document.getDocumentCatalog().getAllPages();
        int pagen = document.getNumberOfPages();
        int i=0;
        if (pn < pages.size())
            i = pn;
        PDPage page = (PDPage)pages.get( i);
        PDRectangle mBox = page.findMediaBox();
        float widthPt = mBox.getWidth();
        float heightPt = mBox.getHeight();
        float sx = widthPt / (float) w;
        float sy = heightPt/ (float) h;
        BufferedImage bi = page.convertToImage(BufferedImage.TYPE_INT_ARGB, Math.round(72/ Math.max(sx, sy)));

        return bi;



    }


    private PDDocument getPages(URI u, File f) throws IOException,
    MalformedURLException {
        if (f == null)
            f = ThumbnailService.copyToTemp(u.toURL().openStream(), "thumb", u.toString().toLowerCase().substring(u.toString().length() - 3));
        PDDocument document = PDDocument.load(f);


        return document;
    }

    static final String PDF_MIME = "application/pdf";
    @Override
    public String getSupportedMime() {

        return PDF_MIME;
    }


    public static void main(String[] args) throws HeadlessException, IOException, URISyntaxException {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.WARN);
        URI u = new URI("file:///Users/fabio/Desktop/altro%20sw%20papo/OPEN%20TYPE%20VARI/Adobe%20OpenType/Adobe%20Fonts/Adobe%20Open%20Type%20Collection/Kepler%20Bl%20Ext%20It%20Cap-012719OPN/KeplerStdReadme.pdf");
        //u = new URI("file:///Users/fabio/Downloads/Planets_PC3-D23A_TheConceptOfSignificantProperties.pdf");
        //u = new URI("file:///Users/fabio/Downloads/ModuloApertura.pdf");

        // new PDFService().generateSVG(u,null,  800, 800, 1,new FileWriter("test.svg"));
       // ImageIO.write(new PDFService().generateThumb(u,null,  800, 800, 0), "png",new File("test.png"));
        FontInformation[] fi = new PDFService().extractFontList(u,null);
       
        int i=1;
        for (FontInformation f: fi)     {
            System.out.println(i++);
            printModel(System.out, f);
        }
        //        JSVGCanvas canvas = new JSVGCanvas();
        //        JFrame f = new JFrame();
        //        f.getContentPane().add(canvas);
        //        canvas.setSVGDocument(document);
        //        f.pack();
        //        f.setVisible(true);

    }

    public static void printModel (PrintStream ps, Object o){
        Field[] fi = o.getClass().getDeclaredFields();
        for (Field f: fi){
            if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers()))
                ps.print(f.getName() + " = ");
            try {
                ps.println(f.get(o));
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        ps.println();

    }


    @Override
    public FontInformation[] extractFontList(URI u, File fff)
    throws MalformedURLException, IOException {

        SortedSet<FontInformation> ret = new  TreeSet<FontInformation>();
        PDDocument document = getPages(u, fff);
        List pages = document.getDocumentCatalog().getAllPages();
        int i = 0;
        // The code down here is easier as it gets all the fonts used in the document. Still, this would inlcude unused fonts, so we get the fonts page by page and add them to a Hash table.
        for (COSObject c: document.getDocument().getObjectsByType(COSName.FONT)) {
            if (c == null || !(c.getObject() instanceof COSDictionary))
                continue;
            //System.out.println(c.getObject());
         
            COSDictionary fontDictionary = (COSDictionary) c.getObject();
            // System.out.println(dic.getNameAsString(COSName.BASE_FONT));
            //            }
            //        }
            //        int pagen = document.getNumberOfPages();
            //        i=0;
            //        for (int p=0;p<pagen;p++){
            //            PDPage page = (PDPage)pages.get(p);
            //            PDResources res = page.findResources();
            //            //for each page resources
            //            if (res==null) continue; 
            //            // get the font dictionary
            //            COSDictionary fonts = (COSDictionary) res.getCOSDictionary().getDictionaryObject( COSName.FONT );
            //            for( COSName fontName : fonts.keySet() ) {
            //                COSObject font = (COSObject) fonts.getItem( fontName );
            //                // if the font has already been visited we ingore it
            //                long objectId = font.getObjectNumber().longValue();
            //                if (ret.get(objectId)!=null)
            //                    continue;
            //                if( font==null ||  ! (font.getObject() instanceof COSDictionary) )
            //                    continue;
            //                COSDictionary fontDictionary = (COSDictionary)font.getObject();

            // Type MUSt be font
            if (!fontDictionary.getNameAsString(COSName.TYPE).equals("Font"))
                continue;
            // get the variables
            FontInformation fi = new FontInformation();
            fi.fontType= fontDictionary.getNameAsString(COSName.SUBTYPE);          
         
            String baseFont= fontDictionary.getNameAsString(COSName.BASE_FONT);
            if (Arrays.binarySearch(standard14, baseFont)>=0)
                continue;
            COSDictionary fontDescriptor = (COSDictionary) fontDictionary.getDictionaryObject(COSName.FONT_DESC);
            COSBase enc = fontDictionary.getItem(COSName.ENCODING);
            COSBase uni = fontDictionary.getItem(COSName.TO_UNICODE);
            int firstChar  = fontDictionary.getInt(COSName.FIRST_CHAR);
            int lastChar  = fontDictionary.getInt(COSName.LAST_CHAR);
            String encoding;
            boolean toUnicode = uni!=null;
            if (enc == null) {
                encoding = "standard14";
            }
            if (enc instanceof COSString) {
                encoding = ((COSString)enc).getString();
            } else {
                encoding = "table";
            }
            fi.isSubset  = false;
            boolean t=true;
            // Type one and TT can have subsets defineing the basename see 5.5.3 pdfref 1.6
            //  if (fi.fontType.lastIndexOf(COSName.TYPE1.getName())!=-1 || fi.fontType.equals(COSName.TRUE_TYPE.getName()) )
            if (baseFont!=null) {
                if (baseFont.length() > 6) {
                    for (int k=0;k<6;k++)
                        if (!Character.isUpperCase(baseFont.charAt(k)))
                            t = false;
                    if (baseFont.charAt(6) != '+')
                        t = false;
                } else t = false;
                fi.isSubset  = t;
                if (fi.isSubset )
                    baseFont = baseFont.substring(7);
            } 
            fi.fontFlags  = 0;
            if (fi.fontType.equals(COSName.TYPE0) || fi.fontType.equals(COSName.TYPE3) )
                fi.isEnbedded = true;
            
            if (fontDescriptor != null) {
                // in Type1 charset indicates font is subsetted
                if (fontDescriptor.getItem(COSName.CHAR_SET)!=null)
                    fi.isSubset  = true;
                if (fontDescriptor.getItem(COSName.FONT_FILE) != null ||fontDescriptor.getItem(COSName.FONT_FILE3) != null || fontDescriptor.getItem(COSName.FONT_FILE2) != null )
                    fi.isEnbedded = true;
                fi.fontFlags = fontDescriptor.getInt(COSName.getPDFName("Flags"));
                fi.fontFamily  = fontDescriptor.getString(COSName.FONT_FAMILY);
                fi.fontStretch = fontDescriptor.getString(COSName.FONT_STRETCH);
            }
            fi.charset =encoding;
            fi.fontName = baseFont;
            fi.isToUnicode = toUnicode;


            ret.add(fi);

        } // for all fonts 

//    } // for all pages
        Iterator<FontInformation> it = ret.iterator();
        FontInformation prev = null;
        LinkedList<FontInformation> toDelete = new LinkedList<FontInformation>();
        while (it.hasNext()) {
            FontInformation current = it.next();
            
            if (prev!= null && prev.fontName.equals(current.fontName) && prev.fontType.startsWith("CIDFontType"))
                toDelete.add(current);
            prev = current;
        }
        ret.removeAll(toDelete);
        FontInformation[] retArray =ret.toArray(new FontInformation[0]);

    return retArray;
}


@Override
public String extraxtXMLText(URI u, File f) throws MalformedURLException,
IOException {
    // TODO Auto-generated method stub
    return null;
}

private static SVGDocument document;

@Override
public void generateSVG(URI u, File f, int w, int h, int pn , Writer out)
throws MalformedURLException, IOException {
    PDDocument doc = getPages(u, f);
    List pages = doc.getDocumentCatalog().getAllPages();
    int pagen = doc.getNumberOfPages();
    int i=0;
    if (pn < pages.size())
        i = pn;
    PDPage page = (PDPage)pages.get( i);
    PDRectangle mBox = page.findMediaBox();
    float widthPt = mBox.getWidth();
    float heightPt = mBox.getHeight();
    float sx = widthPt / (float) w;
    float sy = heightPt/ (float) h;


    // Get a DOMImplementation
    DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
    // Create an instance of org.w3c.dom.Document
    //   String svgNS = "http://www.w3.org/2000/svg";
    //        org.w3c.dom.Document document = domImpl.createDocument(svgNS, "svg",
    //                null);
    DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
    String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
    document= (SVGDocument) impl.createDocument(svgNS, "svg", null);

    // Create an instance of the SVG Generator
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
    svgGenerator.getGeneratorContext().setComment("Test");
    svgGenerator.getGeneratorContext().setEmbeddedFontsOn(true);

    // Ask the test to render into the SVG Graphics2D implementation

    Dimension pageDimension = new Dimension( (int)widthPt, (int)heightPt );


    svgGenerator.setBackground( new Color( 255, 255, 255, 0 ) );
    svgGenerator.scale( sx, sy );
    svgGenerator.setSVGCanvasSize(pageDimension);
    PageDrawer drawer = new PageDrawer();
    drawer.drawPage( svgGenerator, page, pageDimension );

    //        Element root = document.getDocumentElement();
    //        svgGenerator.getRoot(root);

    // Finally, stream out SVG to the standard output using UTF-8
    // character to byte encoding
    boolean useCSS = true;              // we want to use CSS style attribute
    svgGenerator.stream(out, useCSS, false);

    return;
}
}
