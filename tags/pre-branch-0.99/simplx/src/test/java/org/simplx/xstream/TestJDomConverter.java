package org.simplx.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.io.xml.JDomDriver;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import static org.junit.Assert.*;
import org.junit.Test;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * Tests for the JDom converter.
 *
 * @author Ken Arnold
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "JUnitTestNG"})
public class TestJDomConverter {
    private static final String STARTING_XML =
            "<rawHolder xmlns=\"\">" + "<name>rawName</name>" +
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
        assertEquals("doc", elem.getName());
        List kids = elem.getContent();
        assertEquals(3, kids.size());
        assertEquals("Some_", ((Text) kids.get(0)).getText());
        assertEquals("b", ((Element) kids.get(1)).getName());
        assertEquals("bold", ((Element) kids.get(1)).getText());
        assertEquals("_text", ((Text) kids.get(2)).getText());
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
        XStream xstream = new XStream(new JDomDriver());
        xstream.registerConverter(new JDomConverter());
        return xstream;
    }

    @Test
    public void testWriteDOM() throws TransformerException {
        String written = doWrite();

        // We remove the class attribute, which is ignored on reading
        String base = written.replaceFirst(" class[^>]*", "");
        System.out.println(base);
        String fullXML =
                "<?xmlversion=\"1.0\"encoding=\"UTF-8\"?>" + STARTING_XML;
        assertEquals(fullXML.replaceAll("\\s", ""), base.replaceAll("\\s", ""));
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
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
                        "<doc>Some_<b>bold</b>_text</doc>");
        Document doc = readDoc(in);

        RawHolder rh = new RawHolder();
        rh.name = "rawName";
        rh.doc = doc.getRootElement();
        rh.value = 13;
        return rh;
    }

    private static Document readDoc(Reader in) {
        StreamSource src = new StreamSource(in);
        JDOMResult res = new JDOMResult();
        transform(src, res);
        return res.getDocument();
    }

    private void writeDoc(Writer sw, Document doc) {
        StreamResult streamResult = new StreamResult(sw);
        JDOMSource domSource = new JDOMSource(doc);
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