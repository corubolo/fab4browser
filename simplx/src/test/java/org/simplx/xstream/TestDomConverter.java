package org.simplx.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.io.xml.DomDriver;
import static org.junit.Assert.*;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Tests for the DOM converter.
 *
 * @author Ken Arnold
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "JUnitTestNG"})
public class TestDomConverter {
    private static final String STARTING_XML =
            "<rawHolder>" + "<name>rawName</name>" +
                    "<doc>Some_<b>bold</b>_text</doc>" + "<value>13</value>" +
                    "</rawHolder>";

    @XStreamAlias("rawHolder")
    public static class RawHolder {
        public String name;
        public Element doc;
        public long value;
    }

    @Test
    public void testReadDOM() {
        RawHolder rh = doRead(STARTING_XML);
        examineRawHolder(rh);
    }

    private void examineRawHolder(RawHolder rh) {
        assertEquals("rawName", rh.name);
        assertEquals(13, rh.value);
        assertNotNull(rh.doc);
        Element elem = rh.doc;
        assertTrue(Element.class.isAssignableFrom(elem.getClass()));
        assertEquals("doc", elem.getNodeName());
        NodeList kids = elem.getChildNodes();
        assertEquals(3, kids.getLength());
        assertEquals("Some_", kids.item(0).getTextContent());
        assertEquals("b", kids.item(1).getNodeName());
        assertEquals("bold", kids.item(1).getFirstChild().getTextContent());
        assertEquals("_text", kids.item(2).getTextContent());
    }

    private RawHolder doRead(String docStr) {
        RawHolder rh = new RawHolder();
        // Remove the class annotation, which might be incompatible
        docStr = docStr.replaceFirst(" class=\"[^\"]*\"", "");
        Reader in = new StringReader(docStr);
        XStream xstream = createXStream();
        xstream.processAnnotations(rh.getClass());
        xstream.fromXML(in, rh);
        return rh;
    }

    private XStream createXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.registerConverter(new DomConverter());
        return xstream;
    }

    @Test
    public void testWriteDOM() throws TransformerException {
        String written = doWrite();

        // We remove the class attribute, which is ignored on reading
        String base = written.replaceFirst(" class[^>]*", "");
        System.out.println(base);
        String fullXML =
                "<?xmlversion=\"1.0\"encoding=\"UTF-8\"standalone=\"no\"?>" +
                        STARTING_XML;
        assertEquals(fullXML, base.replaceAll("\\s", ""));
    }

    private String doWrite() {
        RawHolder rh = startingRawHolder();

        Writer sw = new StringWriter();
        XStream xstream = createXStream();
        xstream.processAnnotations(rh.getClass());
        String str = xstream.toXML(rh);
        Document doc = readDoc(new StringReader(str));
        writeDoc(sw, doc);
        return sw.toString();
    }

    private RawHolder startingRawHolder() {
        StringReader in = new StringReader(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + "<xyzzy>" +
                        "<doc>Some_<b>bold</b>_text</doc>" + "</xyzzy>");
        Document doc = readDoc(in);

        RawHolder rh = new RawHolder();
        rh.name = "rawName";
        rh.doc = (Element) doc.getDocumentElement().getFirstChild();
        rh.value = 13;
        return rh;
    }

    private static Document readDoc(Reader in) {
        StreamSource src = new StreamSource(in);
        DOMResult res = new DOMResult();
        transform(src, res);
        return (Document) res.getNode();
    }

    private void writeDoc(Writer sw, Document doc) {
        StreamResult streamResult = new StreamResult(sw);
        DOMSource domSource = new DOMSource(doc);
        transform(domSource, streamResult);
    }

    private static void transform(Source src, Result res) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.newTransformer().transform(src, res);
        } catch (TransformerException e) {
            e.printStackTrace();  //To change, use Options | File Templates
            fail(e.getMessage());
        }
    }

    @Test
    public void testRoundtripDOM() throws TransformerException {
        String written = doWrite();
        RawHolder r1 = doRead(written);
        RawHolder r2 = startingRawHolder();
        examineRawHolder(r1);
        examineRawHolder(r2);
    }
}