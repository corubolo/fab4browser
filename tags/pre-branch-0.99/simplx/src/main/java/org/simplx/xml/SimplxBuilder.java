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

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides methods for simplifying the creation of XML documents
 * from Java code. This is the class used in the implementation of the {@link
 * Simplx} static methods.  It is exposed here separately in order to allow you
 * to create simplx-style documents using non-standard configurations.
 *
 * @author Ken Arnold
 * @see Simplx
 */
public class SimplxBuilder {
    private final DocumentBuilder docBuilder;

    private static final ThreadLocal<Reference<Document>> curDocs =
            new ThreadLocal<Reference<Document>>();

    // This pattern allows width=1, width="1", and width='1'
    private static final Pattern ATTR_SPLIT = Pattern.compile(
            "\\b([a-zA-Z_0-9]+?)=((\"(([^\"]|\")*)\")|[^\\s]*)");

    /**
     * Creates a new simplx builder. The document builder is the one returned
     * by:
     * <pre>
     * DocumentBuilderFactory.newInstance().newDocumentBuilder()
     * </pre>
     */
    public SimplxBuilder() {
        this(getDefaultDocumentBuilder());
    }

    /**
     * Creates a new simplx builder with the given document builder.
     *
     * @param docBuilder The document builder to use for this simplx builder.
     */
    public SimplxBuilder(DocumentBuilder docBuilder) {
        this.docBuilder = docBuilder;
    }

    private static DocumentBuilder getDefaultDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e.toString(), e);
        }
    }

    /**
     * Creates a new document, and add some contents. The document is set to be
     * this builder's current document for this thread.
     *
     * @param name     The name of the top-level node.
     * @param contents The contents to add.
     */
    public Document doc(String name, Object... contents) {
        DOMImplementation domImpl = getDocBuilder().getDOMImplementation();
        Document doc = domImpl.createDocument(null, name, null);
        append(doc, contents);
        return doc;
    }

    /**
     * Creates a new document with the given node as the content. The name of
     * the node will also be the name of the document. The document is set to be
     * this builder's current document for this thread.
     *
     * @param root The root content.
     */
    public Document doc(Node root) {
        return doc(root.getNodeName(), root);
    }

    /**
     * Creates a new document from the given input. The document is set to be
     * this builder's current document for this thread.
     *
     * @param in The stream to read from.
     *
     * @throws TransformerException A problem reading the document.
     */
    public Document doc(Reader in) throws TransformerException {
        Document d = SimplxUtils.readDoc(in);
        setDoc(d);
        return d;
    }

    /**
     * Creates a new document from the given input. The document is set to be
     * this builder's current document for this thread.
     *
     * @param in The stream to read from.
     *
     * @throws TransformerException A problem reading the document.
     */
    public Document doc(InputStream in) throws TransformerException {
        Document d = SimplxUtils.readDoc(in);
        setDoc(d);
        return d;
    }

    /**
     * Appends content to a document. The contents will be appended to the
     * top-level element, not the {@code Document} object itself. The document
     * is set to be this builder's current document for this thread.
     *
     * @param doc      The document to which to add contents.
     * @param contents The contents to append.
     *
     * @return The parent element.
     */
    public Node append(Document doc, Object... contents) {
        Reference<Document> ref = new WeakReference<Document>(doc);
        curDocs.set(ref);
        return append(doc.getDocumentElement(), contents);
    }

    /**
     * Appends contents to a given element.
     *
     * @param parent   The element to which to add contents.
     * @param contents The contents to add.
     *
     * @return The parent element.
     */
    public Element append(Element parent, Object... contents) {
        for (Object content : contents) {
            appendByType(parent, content);
        }

        return parent;
    }

    /**
     * Appends a single element to a node, based on its type.
     *
     * @param parent  The element to which to add contents.
     * @param content The contents to add.
     */
    private void appendByType(Element parent, Object content) {
        // Deal with collections of nodes and other non-Node content
        if (!(content instanceof Node)) {
            if (content instanceof NodeList) {
                NodeList nodes = (NodeList) content;
                for (int i = 0; i < nodes.getLength(); i++) {
                    appendByType(parent, nodes.item(i));
                }
                return;
            }

            // syntactically this is the same as for NodeMap, but must repeat it
            if (content instanceof NamedNodeMap) {
                NamedNodeMap nodes = (NamedNodeMap) content;
                for (int i = 0; i < nodes.getLength(); i++) {
                    appendByType(parent, nodes.item(i));
                }
                return;
            }

            if (content instanceof Iterable) {
                Iterable col = (Iterable) content;
                for (Object obj : col) {
                    appendByType(parent, obj);
                }
                return;
            }

            // It's nothing we know about, so put it in as text
            content = text(parent, content);
        }

        // We now know this is a singleton Node value
        Node node = (Node) content;
        parent.getOwnerDocument().adoptNode(node);

        short type = node.getNodeType();
        switch (type) {
        case Node.ATTRIBUTE_NODE:
            parent.setAttributeNode((Attr) node);
            break;
        case Node.TEXT_NODE:
            // Merge with previous text node if present
            Node prev = parent.getLastChild();
            if (prev != null && prev.getNodeType() == Node.TEXT_NODE) {
                node = text(
                        prev.getTextContent() + "\n" + node.getTextContent());
                parent.removeChild(prev);
            }
            //noinspection fallthrough
        case Node.ELEMENT_NODE:
        case Node.CDATA_SECTION_NODE:
        case Node.COMMENT_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.ENTITY_NODE:
        case Node.ENTITY_REFERENCE_NODE:
        case Node.NOTATION_NODE:
        case Node.PROCESSING_INSTRUCTION_NODE:
            parent.appendChild(node);
            break;
        case Node.DOCUMENT_TYPE_NODE:
        case Node.DOCUMENT_NODE:
            throw new IllegalArgumentException(
                    "Invalid type to append: " + node.getNodeName() + "[type " +
                            type + "]");
        default:
            throw new IllegalArgumentException(
                    "Unknown node type " + type + ", " +
                            node.getClass().getName());
        }
    }

    /**
     * Creates a new {@link Element} with the given name. The thread's current
     * document is used as a factory.
     *
     * @param name     The name of the new element.
     * @param contents Contents to add to the element.
     */
    public Element elem(String name, Object... contents) {
        Element parent = curDoc().createElement(name);
        return append(parent, contents);
    }

    /**
     * Creates a new {@link Text} node. The contents will be the result of
     * calling {@link Object#toString()}; typically the object is a {@link
     * String}. The thread's current document is used as a factory.
     *
     * @param text The text.
     */
    public Text text(Object text) {
        return curDoc().createTextNode(text.toString());
    }

    /**
     * Creates a new {@link Comment} node. The contents will be the result of
     * calling {@link Object#toString()}; typically the object is a {@link
     * String}. The thread's current document is used as a factory.
     *
     * @param text The text.
     */
    public Comment comment(Object text) {
        String str = validContents(text, "-->", "Comment");
        return curDoc().createComment(str);
    }

    /**
     * Creates a new {@link CDATASection} node. The contents will be the result
     * of calling {@link Object#toString()}; typically the object is a {@link
     * String}. The thread's current document is used as a factory.
     *
     * @param text The text.
     */
    public CDATASection cdata(Object text) {
        String str = validContents(text, "]]>", "CDATA");
        return curDoc().createCDATASection(str);
    }

    /**
     * Creates a new {@link Text} node that is contained in the given parent.
     * The contents will be the result of calling {@link Object#toString()};
     * typically the object is a {@link String}.
     *
     * @param parent The parent.
     * @param text   The text.
     */
    public Text text(Element parent, Object text) {
        return parent.getOwnerDocument().createTextNode(text.toString());
    }

    /**
     * Creates a new {@link Comment} node that is contained in the given parent.
     * The contents will be the result of calling {@link Object#toString()};
     * typically the object is a {@link String}. The thread's current document
     * is used as a factory.
     *
     * @param parent The parent.
     * @param text   The text.
     */
    public Comment comment(Element parent, Object text) {
        String str = validContents(text, "-->", "Comment");
        return parent.getOwnerDocument().createComment(str);
    }

    /**
     * Creates a new {@link CDATASection} node that is contained in the given
     * parent. The contents will be the result of calling {@link
     * Object#toString()}; typically the object is a {@link String}. The
     * thread's current document is used as a factory.
     *
     * @param parent The parent.
     * @param text   The text.
     */
    public CDATASection cdata(Element parent, Object text) {
        String str = validContents(text, "]]>", "CDATA");
        return parent.getOwnerDocument().createCDATASection(str);
    }

    private String validContents(Object obj, String ending, String desc) {
        String str = obj.toString();
        int pos = str.lastIndexOf(ending);
        if (pos > 0) {
            if (pos != str.length() - 4) {
                throw new IllegalArgumentException(
                        "Illegal \"" + ending + "\" in " + desc + " section");
            } else {
                str = str.substring(0, str.length() - ending.length());
            }
        }
        return str;
    }

    /**
     * Creates a new {@link Attr} node. The thread's current document is used as
     * a factory.
     *
     * @param name  The name.
     * @param value The value, as returned by {@link Object#toString()}.
     */
    public Attr attr(String name, Object value) {
        Attr attr = curDoc().createAttribute(name);
        attr.setValue(value.toString());
        return attr;
    }

    /**
     * Creates a new list of {@link Attr} nodes. The thread's current document
     * is used as a factory. After the initial name/value pair, any other
     * arguments are a series of name/value pairs. For example
     * <pre>
     * attrs("x", "
     * </pre>
     * The thread's current document is used as a factory.
     *
     * @param name   The attribute name.
     * @param value  The attribute value.
     * @param others Any other name/value pairs.
     */
    public List<Attr> attrs(String name, Object value, Object... others) {
        if (others.length % 2 != 0) {
            throw new IllegalArgumentException("More names than values");
        }

        List<Attr> attrList = new ArrayList<Attr>(1 + others.length / 2);
        attrList.add(attr(name, value));
        for (int i = 0; i < others.length; i += 2) {
            attrList.add(attr((String) others[i], others[i + 1].toString()));
        }
        return attrList;
    }

    /**
     * Creates a list of {@link Attr} nodes parsed from the given string. The
     * syntax is the same as for attributes in an XML node. For example, {@code
     * "x=12 title=\"Overall\""} defines two attributes, {@code x} with a value
     * of {@code 12}, and {@code title} with a value of {@code Overall}. Quotes
     * can be either single or double quotes, and are optional if the value
     * contains no whitespace. The thread's current document is used as a
     * factory.
     *
     * @param attrList The string that defines the attributes to be created.
     */
    public List<Attr> attrs(CharSequence attrList) {
        Matcher m = ATTR_SPLIT.matcher(attrList);
        List<Attr> attrs = new ArrayList<Attr>();
        Document doc = curDoc();
        while (m.find()) {
            String name = m.group(1);
            Attr attr = doc.createAttribute(name);
            String val = m.group(2);
            if (val.length() > 2) {
                if (val.charAt(0) == '"') {
                    val = val.substring(1);
                }
                int end = val.length() - 1;
                if (val.charAt(end) == '"') {
                    val = val.substring(0, end);
                }
            }
            attr.setNodeValue(val);
            attrs.add(attr);
        }
        return attrs;
    }

    /**
     * Returns the document used as a favtory by this thread. This is set by
     * {@link #doc(String,Object...)} and {@link #append(Document,Object...)}.
     * The document is held by a {@link WeakReference}, so this does not prevent
     * garbage collection.
     * <p/>
     * New threads spawned by this thread will start with the same current
     * document, but will thereafter have their own current document.
     */
    public Document curDoc() {
        return curDocs.get().get();
    }

    /**
     * Sets the document used as a factory by this thread. Equivalent to {@code
     * append(doc)}.
     *
     * @param doc The document to use as a factory in this thread.
     */
    public void setDoc(Document doc) {
        append(doc);
    }

    /**
     * Returns a deep copy of the node. Equivalent to {@code copy(node,true)}.
     *
     * @param node The node to copy.
     * @param <T>  The node type.
     */
    public <T extends Node> T copy(T node) {
        return copy(node, true);
    }

    /**
     * Returns a copy of the node. The copy is adopted by the document used as a
     * factory by this thread, if any.
     *
     * @param node The node to copy.
     * @param deep If {@code true}, make a deep copy.
     * @param <T>  The node type.
     */
    public <T extends Node> T copy(T node, boolean deep) {
        @SuppressWarnings({"unchecked"})
        T dup = (T) node.cloneNode(deep);
        Document doc = curDoc();
        if (doc != null) {
            doc.adoptNode(dup);
        }
        return dup;
    }

    /** Returns the document builder used by this object. */
    public DocumentBuilder getDocBuilder() {
        return docBuilder;
    }
}
