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
package uk.ac.liverpool.ODMA.ODT;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import multivalent.*;
import multivalent.IDInfo.Confidence;
import multivalent.node.LeafUnicode;

import org.xml.sax.SAXException;

import uk.ac.liverpool.ODMA.EncrypredPackageException;
import uk.ac.liverpool.ODMA.MetaHandler;
import uk.ac.liverpool.ODMA.ODFpackage;
import uk.ac.liverpool.ODMA.PasswordBehaviour;
import uk.ac.liverpool.ODMA.Nodes.LazyLeafImage;
import uk.ac.liverpool.ODMA.styleTags.FontDeclaration;
import uk.ac.liverpool.ODMA.styleTags.Style;
import uk.ac.liverpool.ODMA.styleTags.TextStylesHandler;
import uk.ac.liverpool.ODMA.textTags.TextContentHandler;

import com.pt.io.InputUni;

import static multivalent.IDInfo.Confidence.*;


/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 *
 * ODT
 *
 * Media adaptor for the Open Document format
 */
public class ODT extends MediaAdaptor {

        public static String MIME_TYPE = "application/vnd.oasis.opendocument.text";

    public static final String[][] ODF_MIMES;
    private static final Map<String, String> ODF_SUFFIXES;

    static {
        ODF_MIMES = new String[][]{
                {"application/vnd.oasis.opendocument.text", "odt"}, {
                        "application/vnd.oasis.opendocument.text-template", "ott"}, {
                        "application/vnd.oasis.opendocument.graphics", "odg"}, {
                        "application/vnd.oasis.opendocument.graphics-template", "otg"}, {
                        "application/vnd.oasis.opendocument.presentation", "odp"}, {
                        "application/vnd.oasis.opendocument.presentation-template", "otp"}, {
                        "application/vnd.oasis.opendocument.spreadsheet", "ods"}, {
                        "application/vnd.oasis.opendocument.spreadsheet-template", "ots"}, {
                        "application/vnd.oasis.opendocument.chart", "odc"}, {
                        "application/vnd.oasis.opendocument.chart-template", "otc"}, {
                        "application/vnd.oasis.opendocument.image", "odi"}, {
                        "application/vnd.oasis.opendocument.image-template", "oti"}, {
                        "application/vnd.oasis.opendocument.formula", "odf"}, {
                        "application/vnd.oasis.opendocument.formula-template", "otf"}, {
                        "application/vnd.oasis.opendocument.text-master", "odm"}, {
                        "application/vnd.oasis.opendocument.text-web", "oth"},
        };
        ODF_SUFFIXES = new HashMap<String, String>();
        for (String[] odfMime : ODF_MIMES) {
            ODF_SUFFIXES.put(odfMime[0], odfMime[1]);
        }
    }

    static final String CONTENT_ID = "content.xml";

        static final String STYLES_ID = "styles.xml";

        static final String META_ID = "meta.xml";

        static final String SETTINGS_ID = "settings.xml";

        static int ID_L = 250;

        static HashMap<URI, byte[]> passes = new HashMap<URI, byte[]>(10);

        private long before;



    /**
     * Identifies Open Document Format files.
     *
     * @param min
     *@param max
     * @param path          The path for the bytes. This can be a file path, a
     *                      URL, or <tt>null</tt>.
     *@param complete   @return
     * @throws IOException
     */
    @Override

    public SortedSet<IDInfo> getTypeInfo(Confidence min,
            Confidence max, String path, boolean complete)
            throws IOException {

        SortedSet<IDInfo> infos = validateParams(min, max);

        if (inRange(min, PARSE, max)) {
            Document doc = new Document("doc", null, null);
            try {
                parse(doc);
                infos.add(new IDInfo(PARSE, this,
                        "application/vnd.oasis.opendocument.text"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (inRange(min, HEURISTIC, max)) {
            InputUni uni = getInputUni();
            fast_parse(uni, infos);
        } else if (inRange(min, SUFFIX, max)) {
            if (path != null) {
                String type = lookupSuffix(path, ODF_SUFFIXES);
                if (type != null)
                    infos.add(new IDInfo(SUFFIX, this, type));
            }
        }

        return infos;
    }

    private void fast_parse(InputUni uni, SortedSet<IDInfo> infos) throws IOException {
        BufferedInputStream in;

        // We first check what type of document we have: package (ZIP) or single
        // XML and
        // the mime type (text, spreadsheet etc.)
        // This can be useful in the future when having spreasheet etc. media
        // adapters
        in = new BufferedInputStream(uni.getInputStreamRaw(), ODT.ID_L);
        byte[] ba = new byte[ODT.ID_L];
        in.mark(ODT.ID_L);
        in.read(ba);
        in.reset();
        String mime = null;
        // in this case we have a ZIP file, as per ODF spec.
        if (ba[0] == 'P' && ba[1] == 'K'
                && new String(ba, 30, 8).equals("mimetype")) {
            mime = new String(ba, 38, ODT.ID_L - 38);
        }
        // we have an XML file; not found specs (as ODF is a series of XML documents)
        else {

        }
        if (mime!=null){
           for (String[] suffix : ODF_MIMES) {
                if (mime.startsWith(suffix[0]))
                    infos.add(new IDInfo(SUFFIX, this, suffix[0]));
           }
        }
    }


    @Override
        public Object parse(INode parent) throws Exception {


                Document doc;
                /** the styles */
                Map<String, Style> styles;

                Map<String, Style> masterStyles;

                Map<String, FontDeclaration> fontDecls;

                SAXParserFactory spf = null;

                ODFpackage odfPkg = null;

                BufferedInputStream in;
                InputUni uni = getInputUni();
                String mime = null;
                doc = parent.getDocument();
                spf = SAXParserFactory.newInstance();
                // set it to be fast, we are trursing people :)
                spf.setNamespaceAware(false);
                /* TODO: Change to non validating after testing!!! */
                spf.setValidating(false);
                styles = new HashMap<String, Style>();
                masterStyles = new HashMap<String, Style>();
                fontDecls = new HashMap<String, FontDeclaration>();
                // We first check what type of document we have: package (ZIP) or single
                // XML and
                // the mime type (text, spreadsheet etc.)
                // This can be useful in the future when having spreasheet etc. media
                // adapters
                in = new BufferedInputStream(uni.getInputStreamRaw(), ODT.ID_L);
                byte[] ba = new byte[ODT.ID_L];
                in.mark(ODT.ID_L);
                in.read(ba);
                in.reset();
                // in this case we have a ZIP file, as per ODF spec.
                if (new String(ba, 0, 2).equals("PK")
                                && new String(ba, 30, 8).equals("mimetype")) {
                        String soo = new String(ba, 38, ODT.ID_L - 38);
                        if (soo.indexOf("PK") >= 0)
                                mime = soo.substring(0, soo.indexOf("PK"));
                        if (getURI().getScheme().equals("file"))
                                odfPkg = new ODFpackage(new File(getURI()));
                        else
                                odfPkg = new ODFpackage(in);

                }
                byte[] pass = ODT.passes.get(getURI());
                if (pass!=null)
                        odfPkg.setPassword(pass);
                // or Single XML document ---
                /*
                 * FIXME: get the right mime type from the doc content and manage the
                 * situation
                 */
                else {
                        String sa = new String(ba, 0, ODT.ID_L);
                        if (sa.startsWith("<?xml"))
                                // quick and dirty
                                if (sa.indexOf(ODT.MIME_TYPE) >= 0)
                                        mime = ODT.MIME_TYPE;
                }
                // If we were unable to identify the mime type
                // TODO: better checks?
                // If we have a different format (not text)
                if (mime == null || !mime.equalsIgnoreCase(ODT.MIME_TYPE))
                        throw new ParseException("Invalid file type");
                try {
                        before = System.currentTimeMillis();
                        parseSettings(doc,odfPkg, styles, masterStyles, fontDecls, spf);
                        parseMeta(doc, odfPkg, spf);
                        parseStyles(doc, odfPkg, styles, masterStyles, fontDecls, spf);
                        parseContent(doc, odfPkg, styles, masterStyles, fontDecls, spf);
                        System.out.println("done parsing in "
                                        + (System.currentTimeMillis() - before) + "ms");
                        before = System.currentTimeMillis();
                } catch (EncrypredPackageException e) {
                        getBrowser().eventq(PasswordBehaviour.MSG_ASK_PASS, null);
                        new LeafUnicode("      Encrypted document!",null, doc);
                } catch (ZipException ex) {
                        if (ODT.passes.get(getURI())!=null) {
                                new LeafUnicode("      Wrong password!  You can try to reload and give another password...",null, doc);
                                ODT.passes.remove(getURI());
                        } else
                                throw ex;


                }
                //              LazyLeafImage.stopLoading();
                //System.out.println(doc.getFirstChild());
                System.gc();
                System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1000+"|"+Runtime.getRuntime().maxMemory()/1000);
                //System.out.println("****"+pass);
                return doc.getFirstChild();

        }
        /* (non-Javadoc)
         * @see multivalent.Behavior#semanticEventBefore(multivalent.SemanticEvent, java.lang.String)
         */
        @Override
        public boolean semanticEventAfter(SemanticEvent se, String msg) {
                if (msg == PasswordBehaviour.MSG_RET_PASS) {
                        ///INode parent  = (Document)se.getArg();


                        //parse(parent);
                        //System.out.println(getBrowser().getCurDocument().getURI());
                        ODT.passes.put (getBrowser().getCurDocument().getURI(), (byte[])se.getArg());
                        getBrowser().eventq(new SemanticEvent(getBrowser(), Document.MSG_RELOAD , null));
                        //System.out.println("****************");

                        //doc.putAttr("pass", pass);
                        //getBrowser().eventq(new SemanticEvent(getBrowser(), Document.MSG_RELOAD , doc));
                        //getBrowser().getCurDocument().getLayers().buildBeforeAfter(getBrowser().getCurDocument());

                }else if (msg == Document.MSG_FORMATTED) {
                        if (LazyLeafImage.hasQueue()) {
                                LazyLeafImage.startLoading();
                                LazyLeafImage.stopLoading();
                        }
                        System.out.println("done formatting in "
                                        + (System.currentTimeMillis() - before) + "ms");
                }

                return super.semanticEventBefore(se, msg);
        }
        /**
         * @throws SAXException
         * @throws ParserConfigurationException
         * @throws EncrypredPackageException
         * @throws IOException
         */
        private void parseContent(multivalent.Document doc,     ODFpackage zipFile,
                        Map<String, Style> styles, Map<String, Style> masterStyles,
                        Map<String, FontDeclaration> fontDecls, SAXParserFactory spf)
        throws ParserConfigurationException, SAXException, IOException,
        EncrypredPackageException {
                SAXParser parser = spf.newSAXParser();
                //
                // ToHtmlContentHandler contentHandler = new ToHtmlContentHandler(doc);
                // df.setEscapeTextTags(false);
                // df.setSpaceEating(false);
                // DefaultHandler contentHandler = new ContentHandler(doc);
                // DefaultHandler contentHandler = new DefaultHandler();
                TextContentHandler contentHandler = new TextContentHandler(doc, styles,
                                masterStyles, fontDecls, zipFile);

                parser.parse(zipFile.getFileIS(ODT.CONTENT_ID), contentHandler);

                // parseHelper(contentHandler.outputBuffer.toString(), "HTML",
                // getLayer(), doc);
                // Iterator<String> i = contentHandler.styles.keySet().iterator();
                /*
                 * int ii = 1;
                 *
                 * for (Entry <String, Style>e:contentHandler.styles.entrySet()) {
                 * System.out.println(ii + ")" + e.getValue().family + " " +
                 * e.getValue().name + " " + e.getValue().parent); ii++;
                 */
                // while (i.hasNext()) {
                // String k = i.next();
                // Style s = contentHandler.styles.get(k);
                /*
                 * if (s.textProperties != null) { printMap(s.textProperties); }
                 */
                // }

                contentHandler = null;
                // System.gc();

        }

        public static void printMap(Map<?, ?> m) {
                if (m == null)
                        return;
                // Iterator<E>m.entrySet().iterator()
                for (Entry en : m.entrySet())
                        System.out.println(en.getKey() + "=" + en.getValue());
                System.out.println("***");
        }

        /**
         * @throws SAXException
         * @throws ParserConfigurationException
         * @throws EncrypredPackageException
         * @throws IOException
         */
        private void parseStyles(multivalent.Document doc,      ODFpackage zipFile,
                        Map<String, Style> styles, Map<String, Style> masterStyles,
                        Map<String, FontDeclaration> fontDecls, SAXParserFactory spf)
        throws ParserConfigurationException, SAXException, IOException,
        EncrypredPackageException {
                SAXParser parser = spf.newSAXParser();
                TextStylesHandler stylesHandler = new TextStylesHandler(doc, styles,
                                masterStyles, fontDecls);
                long before1 = System.currentTimeMillis();
                parser.parse(zipFile.getFileIS(ODT.STYLES_ID), stylesHandler);
                System.out.println("done parsing styles in "
                                + (System.currentTimeMillis() - before1) + "ms");
                // parseHelper(stylesHandler.outputBuffer.toString(), "HTML",
                // getLayer(), doc);
                stylesHandler = null;

        }

        /**
         * Empty method / placeholder. Should parse the settings. Most probably we
         * don't need to.
         *
         */
        private void parseSettings(multivalent.Document doc,    ODFpackage zipFile,
                        Map<String, Style> styles, Map<String, Style> masterStyles,
                        Map<String, FontDeclaration> fontDecls, SAXParserFactory spf) {
                // TODO Seems like we don't really need to, maybe useful in the future

        }

        /**
         * Parses the MetaData information and stores it in the appropriate Document
         *
         *
         * @throws IOException
         * @throws SAXException
         * @throws ParserConfigurationException
         * @throws EncrypredPackageException
         * @throws SAXException
         * @throws ParserConfigurationException
         *
         */
        private void parseMeta(multivalent.Document doc,        ODFpackage zipFile,
                        SAXParserFactory spf) throws IOException,
                        EncrypredPackageException, ParserConfigurationException,
                        SAXException {
                if (zipFile != null) {
                        SAXParser parser = spf.newSAXParser();
                        parser.parse(zipFile.getFileIS(ODT.META_ID), new MetaHandler(doc));
                }

        }



}