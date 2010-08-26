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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * This class provides a set of utilities for simplifying the creation of XML
 * documents from Java code. For example, the following creates an XML
 * document:
 * <pre>
 * Document doc = doc("notes");
 * append(doc,
 *     elem("note",
 *         elem("to", "Tove"),
 *         elem("from", "Jani"),
 *         elem("heading", "Reminder"),
 *         elem("body", "Don't forget me this weekend!")
 *     )
 * );
 * </pre>
 * This is equivalent to
 * <pre>
 * &lt;notes&gt;
 *     &lt;note&gt;
 *         &lt;to&gt;Tove&lt;/to&gt;
 *         &lt;from&gt;Jani&lt;/from&gt;
 *         &lt;heading&gt;Reminder&lt;/heading&gt;
 *         &lt;body&gt;Don't forget me this weekend!&lt;/body&gt;
 *     &lt;/note&gt;
 * &lt;/notes&gt;
 * </pre>
 * This is provided by a simple use of variable-argument methods, and
 * generalized processing of content nodes. In the above example, the {@link
 * #append(Document,Object...) append} method is used to add one element to the
 * existing document, an element named "note". Any number of elements,
 * attributes, and/or text content, can be appended in the same call.
 * <p/>
 * You can see this working in the {@link #elem(String,Object...) elem} call
 * that creates the "note" element. It does two things: It creates an element
 * named "note", and appends a list of content to it. That content is four other
 * elements created by their own {@link #elem(String, Object...) elem} calls.
 * Each of these, in turn, is created with contents that are simple strings,
 * interpreted as {@link Text} elements.
 * <p/>
 * In this way, the Java code structure is a direct and simple corrolary to the
 * XML structure it represents.
 * <p/>
 * Contents can be of the following type: <dl> <dt> {@link Node} <dd> The node
 * will be appended to the parent. Note: Consecutive text nodes will be joined
 * into a single node, with newlines between them. <dt> {@link String} <dd> A
 * simple string will be appended as a text node.<dt> {@link NodeList}, {@link
 * NamedNodeMap} <dd> Each node of the list will be appended to the parent, in
 * order. <dt> {@link Iterable} (collections, arrays, etc.) <dd> Each element of
 * iterable object will be appended to the parent, in the order returned by the
 * iterator. <dt> {@link Object} <dd> The result of {@link Object#toString()}
 * will be appended as a text node.</dl>
 * <p/>
 * This is implemented by having a single {@link SimplxBuilder} object, and
 * invoking all the methods on that object.  The {@link SimplxBuilder} class
 * lets you define a simplx-style builder object that has specific configuration
 * settings that are not the default, as presented here.
 *
 * @author Ken Arnold
 */
public class Simplx {
    private static final SimplxBuilder instance = new SimplxBuilder();

    /**
     * Not used. This is a utility class. The constructor is left as public so
     * that  you can create subclasses that add other utility methods.
     */
    public Simplx() {
    }

    /**
     * Returns the document builder used by Simplx. If none has been provided,
     * uses the one returned by
     * <pre>
     * DocumentBuilderFactory.newInstance().newDocumentBuilder()
     * </pre>
     */
    public static DocumentBuilder getDocBuilder() {
        return instance.getDocBuilder();
    }

    /**
     * Creates a new document, and add some contents. The document is set to be
     * the current document for this thread.
     *
     * @param name     The name of the top-level node.
     * @param contents The contents to add.
     */
    public static Document doc(String name, Object... contents) {
        return instance.doc(name, contents);
    }

    /**
     * Creates a new document with the given node as the content. The name of
     * the node will also be the name of the document. The document is set to be
     * the current document for this thread.
     *
     * @param root The root content.
     */
    public static Document doc(Node root) {
        return instance.doc(root);
    }

    /**
     * Creates a new document from the given input. The document is set to be
     * the current document for this thread.
     *
     * @param in The stream to read from.
     *
     * @throws TransformerException A problem reading the document.
     */
    public static Document doc(Reader in) throws TransformerException {
        return instance.doc(in);
    }

    /**
     * Creates a new document from the given input. The document is set to be
     * the current document for this thread.
     *
     * @param in The stream to read from.
     *
     * @throws TransformerException A problem reading the document.
     */
    public static Document doc(InputStream in) throws TransformerException {
        return instance.doc(in);
    }

    /**
     * Appends content to a document. The contents will be appended to the
     * top-level element, not the {@code Document} object itself. The document
     * is set to be the current document for this thread.
     *
     * @param doc      The document to which to add contents.
     * @param contents The contents to append.
     *
     * @return The parent element.
     */
    public static Node append(Document doc, Object... contents) {
        return instance.append(doc, contents);
    }

    /**
     * Appends contents to a given element.
     *
     * @param parent   The element to which to add contents.
     * @param contents The contents to add.
     *
     * @return The parent element.
     */
    public static Element append(Element parent, Object... contents) {
        return instance.append(parent, contents);
    }

    /**
     * Creates a new {@link Element} with the given name. The thread's current
     * document is used as a factory.
     *
     * @param name     The name of the new element.
     * @param contents Contents to add to the element.
     */
    public static Element elem(String name, Object... contents) {
        return instance.elem(name, contents);
    }

    /**
     * Creates a new {@link Text} node. The contents will be the result of
     * calling {@link Object#toString()}; typically the object is a {@link
     * String}. The thread's current document is used as a factory.
     *
     * @param text The text.
     */
    public static Text text(Object text) {
        return instance.text(text);
    }

    /**
     * Creates a new {@link Comment} node. The contents will be the result of
     * calling {@link Object#toString()}; typically the object is a {@link
     * String}. The thread's current document is used as a factory.
     *
     * @param text The text.
     */
    public static Comment comment(Object text) {
        return instance.comment(text);
    }

    /**
     * Creates a new {@link CDATASection} node. The contents will be the result
     * of calling {@link Object#toString()}; typically the object is a {@link
     * String}. The thread's current document is used as a factory.
     *
     * @param text The text.
     */
    public static CDATASection cdata(Object text) {
        return instance.cdata(text);
    }

    /**
     * Creates a new {@link Text} node that is contained in the given parent.
     * The contents will be the result of calling {@link Object#toString()};
     * typically the object is a {@link String}.
     *
     * @param parent The parent.
     * @param text   The text.
     */
    public static Text text(Element parent, Object text) {
        return instance.text(parent, text);
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
    public static Comment comment(Element parent, Object text) {
        return instance.comment(parent, text);
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
    public static CDATASection cdata(Element parent, Object text) {
        return instance.cdata(parent, text);
    }

    /**
     * Creates a new {@link Attr} node. The thread's current document is used as
     * a factory.
     *
     * @param name  The name.
     * @param value The value, as returned by {@link Object#toString()}.
     */
    public static Attr attr(String name, Object value) {
        return instance.attr(name, value);
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
    public static List<Attr> attrs(String name, Object value,
            Object... others) {

        return instance.attrs(name, value, others);
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
    public static List<Attr> attrs(CharSequence attrList) {
        return instance.attrs(attrList);
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
    public static Document curDoc() {
        return instance.curDoc();
    }

    /**
     * Sets the document used as a factory by this thread. Equivalent to {@code
     * append(doc)}.
     *
     * @param doc The document to use as a factory in this thread.
     */
    public static void setDoc(Document doc) {
        instance.setDoc(doc);
    }

    /**
     * Returns a deep copy of the node. Equivalent to {@code copy(node,true)}.
     *
     * @param node The node to copy.
     * @param <T>  The node type.
     */
    public static <T extends Node> T copy(T node) {
        return instance.copy(node);
    }

    /**
     * Returns a copy of the node. The copy is adopted by the document used as a
     * factory by this thread, if any.
     *
     * @param node The node to copy.
     * @param deep If {@code true}, make a deep copy.
     * @param <T>  The node type.
     */
    public static <T extends Node> T copy(T node, boolean deep) {
        return instance.copy(node, deep);
    }

    /**
     * Returns the {@link SimplxBuilder} object on which the static methods are
     * invoked.
     */
    public static SimplxBuilder getInstance() {
        return instance;
    }
}
