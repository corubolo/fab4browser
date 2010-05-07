/*
 * Copyright (c) 2009, 2010, Ken Arnold All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the myself nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @SimplxCopyright
 */

package org.simplx.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * General XML utility methods.
 *
 * @author Ken Arnold
 */
@SuppressWarnings({"TypeMayBeWeakened"})
public class SimplxUtils {
    private static TransformerFactory transformerFactory = null;

    private static DriverFactory driverFactory = new DriverFactory() {
        @Override
        public HierarchicalStreamDriver getDriver() {
            return new DomDriver();
        }
    };

    private static final Collection<Converter> converters =
            new ArrayList<Converter>();

    /** A factory for creating {@link HierarchicalStreamDriver} objects. */
    public interface DriverFactory {
        /** Returns a {@link HierarchicalStreamDriver}. */
        HierarchicalStreamDriver getDriver();
    }

    /**
     * Not used. This is a utility class. The constructor is left as public so
     * that  you can create subclasses that add other utility methods.
     */
    public SimplxUtils() {
    }

    /**
     * Returns the first direct child whose type is {@link Element}.
     *
     * @param parent The parent.
     */
    public static Element firstElement(Element parent) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element)
                return (Element) n;
        return null;
    }

    /**
     * Returns a document read from a file.
     *
     * @param xmlFile The file to read.
     *
     * @throws IOException          An I/O problem.
     * @throws TransformerException A parsing problem.
     */
    public static Document readDoc(File xmlFile)
            throws IOException, TransformerException {

        InputStream in = new BufferedInputStream(new FileInputStream(xmlFile));
        return doReadDoc(in, new StreamSource(in));
    }

    /**
     * Returns a document read from an {@link InputStream}.
     *
     * @param in The stream to read.
     *
     * @throws TransformerException A parsing problem.
     */
    public static Document readDoc(InputStream in) throws TransformerException {
        return doReadDoc(in, new StreamSource(in));
    }

    /**
     * Returns a document read from an {@link Reader}.
     *
     * @param in The stream to read.
     *
     * @throws TransformerException A parsing problem.
     */
    public static Document readDoc(Reader in) throws TransformerException {
        return doReadDoc(in, new StreamSource(in));
    }

    private static Document doReadDoc(Closeable in, StreamSource src)
            throws TransformerException {
        try {
            DOMResult res = new DOMResult();
            getTransformer().transform(src, res);
            return (Document) res.getNode();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Writes a document to a {@link Writer}.
     *
     * @param node  The node to write.
     * @param out   The stream to write to.
     * @param props A list of zero or more option/value parameters, from {@link
     *              OutputKeys}.
     *
     * @throws TransformerException The transformation failed.
     */
    public static void write(Node node, Writer out, String... props)
            throws TransformerException {

        StreamResult streamResult = new StreamResult(out);
        write(node, streamResult, props);
    }

    /**
     * Writes a node to an {@link OutputStream}.
     *
     * @param node  The node to write.
     * @param out   The stream to write to.
     * @param props A list of zero or more option/value parameters, from {@link
     *              OutputKeys}.
     *
     * @throws TransformerException The transformation failed.
     */
    public static void write(Node node, OutputStream out, String... props)
            throws TransformerException {

        StreamResult streamResult = new StreamResult(out);
        write(node, streamResult, props);
    }

    private static void write(Node node, StreamResult streamResult,
            String... props) throws TransformerException {

        if (props.length % 2 != 0)
            throw new IllegalArgumentException(
                    "properties must be name/value pairs of strings");

        DOMSource domSource = new DOMSource(node);

        Transformer xmlOut = getTransformer();
        if (props.length == 0 || props[0].equals("+")) {
            xmlOut.setOutputProperty(OutputKeys.INDENT, "yes");
            xmlOut.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        }
        for (int i = 0; i < props.length; i += 2)
            xmlOut.setOutputProperty(props[i], props[i + 1]);
        xmlOut.transform(domSource, streamResult);
    }

    private static Transformer getTransformer()
            throws TransformerConfigurationException {
        return getTransformerFactory().newTransformer();
    }

    private static synchronized TransformerFactory getTransformerFactory() {
        if (transformerFactory == null)
            transformerFactory = TransformerFactory.newInstance();
        return transformerFactory;
    }

    /**
     * Returns the string that is the content of a child from this node. If
     * there is none, return {@code null}.
     *
     * @param parent  The parent.
     * @param kidName The name of the child node, which must be unique within
     *                the parent.
     */
    public static String getChildText(Element parent, String kidName) {
        Node elem = getUniqueChild(parent, kidName);
        if (elem == null)
            return null;
        elem.normalize();
        return elem.getTextContent();
    }

    /**
     * Returns the child of a parent with the given name. If there is none,
     * returns {@code null}. If there is more than one, throws {@link
     * IllegalArgumentException}.
     *
     * @param parent The parent.
     * @param name   The name of the child node.
     */
    public static Element getUniqueChild(Element parent, String name) {
        Element child = null;
        for (Node n = parent.getFirstChild();
             n != null;
             n = n.getNextSibling()) {
            if (n.getNodeName().equals(name) && n instanceof Element) {
                if (child == null)
                    child = (Element) n;
                else
                    throw new IllegalArgumentException(
                            "more than one <" + name + "> tag inside <" +
                                    parent.getNodeName() + ">");
            }
        }
        return child;
    }

    /**
     * Returns all the direct children of the parent that are {@link Element}
     * nodes.
     *
     * @param parent The parent.
     * @param name   Only elements with this name will be included, or if "*",
     *               includes all child elements.
     */
    public static List<Element> getChildElements(Element parent, String name) {
        List<Element> kids = new ArrayList<Element>();
        for (Node n = parent.getFirstChild();
             n != null;
             n = n.getNextSibling()) {
            if ((name.equals("*") || n.getNodeName().equals(name)) &&
                    n instanceof Element) {
                kids.add((Element) n);
            }
        }
        return kids;
    }

    /**
     * Get an attribute value from a node, returning a default value if it not
     * present.
     *
     * @param elem   The element that has attributes.
     * @param name   The name of the attribute.
     * @param defVal The value to return if the attribute is not defined.
     */
    public static String getAttrValue(Element elem, String name,
            Object defVal) {
        String value = elem.getAttribute(name);
        if (value == null || value.length() == 0)
            value = defVal.toString();
        return value;
    }

    /**
     * Writes an object to a file as XML, using Xstream.
     *
     * @param xmlFile  The file for the XML.
     * @param contents The object to write to the file.
     */
    public static void writeObj(File xmlFile, Object contents) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(xmlFile));
            writeObj(out, contents);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write XML file " + xmlFile,
                    e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Writes an object to a stream as XML, using Xstream.
     *
     * @param out      The stream for the XML.
     * @param contents The object to write to the stream.
     */
    public static void writeObj(Writer out, Object contents) {
        try {
            Document doc = createDoc(contents);
            write(doc, out);
        } catch (TransformerException e) {
            throw new IllegalStateException("Cannot write XML", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Writes an object to a stream as XML, using Xstream.
     *
     * @param out      The stream for the XML.
     * @param contents The object to write to the stream.
     */
    public static void writeObj(OutputStream out, Object contents) {
        try {
            Document doc = createDoc(contents);
            write(doc, out);
        } catch (TransformerException e) {
            throw new IllegalStateException("Cannot write XML", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Returns an XML string for a node.
     *
     * @param node The node.
     */
    public static String toString(Node node) {
        try {
            StringWriter sw = new StringWriter();
            write(node, sw);
            IOUtils.closeQuietly(sw);
            return sw.toString();
        } catch (TransformerException e) {
            return e.getMessage();
        }
    }

    private static Document createDoc(Object contents)
            throws TransformerException {

        XStream xstream = createXstream(contents);
        String str = xstream.toXML(contents);
        return readDoc(new StringReader(str));
    }

    private static XStream createXstream(Object contents) {
        XStream xstream = new XStream(driverFactory.getDriver());
        xstream.processAnnotations(contents.getClass());
        for (Converter converter : converters) {
            xstream.registerConverter(converter);
        }
        return xstream;
    }

    /**
     * Reads an object from an XML file using Xstream.
     *
     * @param xml    The XML file to read from.
     * @param result The object into which to read the result.
     *
     * @throws IOException A problem happend reading the XML.
     */
    public static void readObj(File xml, Object result) throws IOException {
        Reader in = new BufferedReader(new FileReader(xml));
        readObj(in, result);
    }

    /**
     * Reads an object from an XML stream using Xstream.
     *
     * @param in     The stream to read from.
     * @param result The object into which to read the result.
     */
    public static void readObj(Reader in, Object result) {
        try {
            XStream xStream = createXstream(result);
            xStream.fromXML(in, result);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /** Returns the driver factory being used for Xstream. */
    public static DriverFactory getDriverFactory() {
        return driverFactory;
    }

    /**
     * Sets the driver factory to be used for Xstream.
     *
     * @param driverFactory The driver factory to be used for Xstream.
     */
    public static void setDriverFactory(DriverFactory driverFactory) {
        if (driverFactory == null)
            throw new NullPointerException("driverFactory");
        SimplxUtils.driverFactory = driverFactory;
    }

    /**
     * Adds a convert to the list of known converters that will be used for
     * reading and writing XML objects via Xstream.
     *
     * @param converter The converter to add.
     */
    public static void addConverter(Converter converter) {
        converters.add(converter);
    }
}
