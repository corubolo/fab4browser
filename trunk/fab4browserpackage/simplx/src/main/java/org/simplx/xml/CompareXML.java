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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a way to compare two XML trees or subtrees. There are two
 * kinds of comparison that this provides: The {@code equals} style returns
 * boolean values, and the {@code match} style throws an exception detailing the
 * difference that is detected.
 * <p/>
 * When comparing, certain differences may be unimportant. You can use {@link
 * IgnoreTester} objects to specify parts of the XML that could be ignored.
 *
 * @author Ken Arnold
 * @see XMLMismatchException
 */
public class CompareXML {
    private IgnoreTester ignoreTester = IGNORE_NOTHING;
    private XMLMismatchException mismatch;

    private static final Method EQUALS;
    private static final Method TO_STRING;

    static {
        try {
            EQUALS = Object.class.getMethod("equals", Object.class);
            TO_STRING = Object.class.getMethod("toString");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Pattern CANON_SPACE = Pattern.compile("\\s+");

    /** An tester that adds no new things to ignore. */
    public static final IgnoreTester IGNORE_NOTHING = new IgnoreTester() {
        @Override
        public boolean ignore(Node node) {
            return false;
        }
    };

    /**
     * Implement this interface to make your own tester for whether a node
     * should be involved in the comparison or not. For example, you could
     * ignore all "notes" nodes with the following tester:
     * <pre>
     * new IgnoreTester() {
     *     public boolean ignore(Node node) {
     *        return node.getNodeName().equals("notes") &&
     *               node instanceof Element;
     *    }
     * };
     * </pre>
     */
    public interface IgnoreTester {
        /**
         * Returns {@code true} if the XML comparision should ignore this node.
         *
         * @param node The node to test.
         */
        boolean ignore(Node node);
    }

    class WrappedNode implements InvocationHandler {
        private final Node node;

        WrappedNode(Node node) {
            this.node = node;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {

            if (method.equals(EQUALS)) {
                Object obj = args[0];
                if (!(obj instanceof Node))
                    return false;
                Node that = (Node) obj;
                if (Proxy.isProxyClass(that.getClass())) {
                    InvocationHandler h = Proxy.getInvocationHandler(that);
                    if (h instanceof WrappedNode) {
                        that = ((WrappedNode) h).node;
                    }
                }
                return match(node, that, "equals");
            } else if (method.equals(TO_STRING)) {
                return "Wrapper{" + node + "}";
            } else {
                return method.invoke(node, args);
            }
        }
    }

    /** Creates a new XML comparison object. */
    public CompareXML() {
        this(IGNORE_NOTHING);
    }

    /**
     * Creates a new XML comparison object with the specificied node ignore
     * tester.
     *
     * @param ignoreTester The ignore tester to use during comparision.
     */
    public CompareXML(IgnoreTester ignoreTester) {
        this.ignoreTester = ignoreTester;
    }

    /** Returns the node ignore tester. This is never {@code null}. */
    public IgnoreTester getIgnoreTester() {
        return ignoreTester;
    }

    /**
     * Sets the node ignore tester. A value of {@code null} will be replaced
     * with {@link #IGNORE_NOTHING}.
     *
     * @param ignoreTester The ignore tester to use during comparision.
     */
    public void setIgnoreTester(IgnoreTester ignoreTester) {
        if (ignoreTester == null)
            ignoreTester = IGNORE_NOTHING;
        this.ignoreTester = ignoreTester;
    }

    /**
     * Returns the exception that caused the most recent comparision to fail.
     * Returns {@code null} if the most recent comparison did not fail.
     */
    public XMLMismatchException getMismatch() {
        return mismatch;
    }

    /**
     * Returns {@code true} if this is an ignorable node. A node is ignorable
     * for testing purposes if it is a text node that has no content except
     * (possibly) whitespace.
     *
     * @param node The node to test.
     */
    public boolean ignorable(Node node) {
        if (node.getNodeType() == Node.TEXT_NODE &&
                node.getTextContent().trim().length() == 0)
            return true;
        return ignoreTester.ignore(node);
    }

    /**
     * Returns {@code true} if the two nodes are equal, otherwise {@code
     * false}.
     *
     * @param n1 One node.
     * @param n2 Another node.
     *
     * @see #match(Node,Node,String)
     */
    public boolean equals(Node n1, Node n2) {
        try {
            return match(n1, n2, "");
        } catch (XMLMismatchException ignored) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the two nodes match, otherwise throws {@link
     * XMLMismatchException}.  Two nodes match if they have the same type,
     * namespace URI, and attributes attributes, and all child nodes also match,
     * in the order they appear. If either node is {@code null}, the other node
     * must also be {@code null} or they will not match.
     *
     * @param n1   One node.
     * @param n2   Another node.
     * @param desc A description string included in the exception.
     *
     * @see #match(Node,Node,String)
     */
    public boolean match(Node n1, Node n2, String desc) {
        mismatch = null;
        if ((n1 == null) != (n2 == null))
            mismatch(desc + ": " + n1 + " vs. " + n2);

        if (n1 == null) // both are null
            return true;

        assert n2 != null;
        if (!match(n1.getNodeType(), n2.getNodeType(), "node type"))
            return false;

        switch (n1.getNodeType()) {
        case Node.TEXT_NODE:
            Matcher m1 = CANON_SPACE.matcher(n1.getTextContent().trim());
            Matcher m2 = CANON_SPACE.matcher(n2.getTextContent().trim());
            return m1.replaceAll(" ").equals(m2.replaceAll(" "));
        case Node.ATTRIBUTE_NODE:
            return match(n1.getNodeName(), n2.getNodeName(),
                    desc + " attr name") && match(n1.getTextContent(),
                    n2.getTextContent(), desc + " attr val");
        }

        return match(n1.getNodeName(), n2.getNodeName(), "node name") && match(
                n1.getNamespaceURI(), n2.getNamespaceURI(), "namespace") &&
                match(n1.getAttributes(), n2.getAttributes(), "attributes") &&
                match(n1.getChildNodes(), n2.getChildNodes(), "children");
    }

    /**
     * Returns {@code true} if the two node lists are equal, otherwise {@code
     * false}.
     *
     * @param l1 One node list.
     * @param l2 Another node list.
     *
     * @see #match(NodeList,NodeList,String)
     */
    public boolean equals(NodeList l1, NodeList l2) {
        try {
            return match(l1, l2, "");
        } catch (XMLMismatchException ignored) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the two node lists match, otherwise throws {@link
     * XMLMismatchException}. Two lists match if they have the same number of
     * elements, and if each node matches the corresponding node in the other,
     * in order. If either list is {@code null}, the other must also be null or
     * there is a mismatch.
     *
     * @param l1   One node list.
     * @param l2   Another node list.
     * @param desc A description string included in the exception.
     *
     * @see #match(Node,Node,String)
     */
    public boolean match(NodeList l1, NodeList l2, String desc) {
        mismatch = null;
        if ((l1 == null) != (l2 == null))
            mismatch(desc + ": " + l1 + " vs. " + l2);

        if (l1 == null) // both are null
            return true;

        List<Node> list1 = listFor(l1);
        List<Node> list2 = listFor(l2);
        if (list1.size() != list2.size()) {
            return mismatch(desc + ": length " + list1.size() + " vs. length " +
                    list2.size());
        }
        if (!list1.equals(list2))
            return mismatch(desc + ": " + list1 + " vs. " + list2);
        return true;
    }

    /**
     * Returns a list of non-ignorable child nodes.
     *
     * @param nl The node list.
     *
     * @see #ignorable(Node)
     */
    public List<Node> listFor(NodeList nl) {
        List<Node> lst1 = new ArrayList<Node>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (!ignorable(n))
                lst1.add(wrap(n));
        }
        return lst1;
    }

    private Node wrap(Node n) {
        Class[] ifaces = interfaces(n.getClass());
        return (Node) Proxy.newProxyInstance(n.getClass().getClassLoader(),
                ifaces, new WrappedNode(n));
    }

    private Class[] interfaces(Class<?> type) {
        Set<Class> ifaces = new HashSet<Class>();
        ifaces = interfaces(type, ifaces);
        return ifaces.toArray(new Class[ifaces.size()]);
    }

    private Set<Class> interfaces(Class<?> type, Set<Class> ifaces) {
        if (type == null)
            return ifaces;

        Class[] ifTypes = type.getInterfaces();
        ifaces.addAll(Arrays.asList(ifTypes));
        ifaces = interfaces(type.getSuperclass(), ifaces);
        for (Class ifType : ifTypes) {
            ifaces = interfaces(ifType, ifaces);
        }
        return ifaces;
    }

    /**
     * Returns {@code true} if the two named node maps are equal, otherwise
     * {@code false}.
     *
     * @param m1 One named node map.
     * @param m2 Another named node map.
     *
     * @see #match(NamedNodeMap,NamedNodeMap,String)
     */
    public boolean equals(NamedNodeMap m1, NamedNodeMap m2) {
        try {
            return match(m1, m2, "");
        } catch (XMLMismatchException ignored) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the two named node maps match, otherwise throws
     * {@link XMLMismatchException}. Two maps match if they have the same number
     * of elements, and if each node matches has an entry with the same node in
     * the other map, and those two nodes match. If either map is {@code null},
     * the other must also be null or there is a mismatch.
     *
     * @param m1   One named node map.
     * @param m2   Another named node map.
     * @param desc A description string included in the exception.
     *
     * @see #match(Node,Node,String)
     */
    public boolean match(NamedNodeMap m1, NamedNodeMap m2, String desc) {
        mismatch = null;
        if ((m1 == null) != (m2 == null))
            mismatch(desc + ": " + m1 + " vs. " + m2);

        if (m1 == null) // both are null
            return true;

        assert m2 != null;
        if (m1.getLength() != m2.getLength()) {
            return mismatch(
                    desc + ": length " + m1.getLength() + " vs. length " +
                            m2.getLength());
        }

        Map<String, Node> map1 = mapFor(m1);
        Map<String, Node> map2 = mapFor(m2);
        if (!map1.equals(map2))
            mismatch(desc + ": " + m1 + " vs. " + m2);
        return true;
    }

    private boolean mismatch(String s) {
        mismatch = new XMLMismatchException(s);
        throw mismatch;
    }

    /**
     * Returns a map of node names to nodes from a given named node map.
     *
     * @param nnm The source named node map.
     */
    public Map<String, Node> mapFor(NamedNodeMap nnm) {
        Map<String, Node> map = new TreeMap<String, Node>();
        for (int i = 0; i < nnm.getLength(); i++) {
            Node node = nnm.item(i);
            map.put(node.getNodeName(), wrap(node));
        }
        return map;
    }

    /**
     * Returns {@code true} if the two values match, otherwise throws {@link
     * XMLMismatchException}.
     *
     * @param v1   One value.
     * @param v2   Another value.
     * @param desc A description string included in the exception.
     */
    private boolean match(int v1, int v2, String desc) {
        if (v1 != v2)
            mismatch(desc + ": " + v1 + " vs. " + v2);
        return true;
    }

    /**
     * Returns {@code true} if the two strings match, otherwise throws {@link
     * XMLMismatchException}. Two strings match if {@link String#equals(Object)}
     * return {@code true}. If either string is {@code null}, the other must
     * also be null or there is a mismatch.
     *
     * @param s1   One string.
     * @param s2   Another string.
     * @param desc A description string included in the exception.
     */
    private boolean match(String s1, String s2, String desc) {
        if ((s1 == null) != (s2 == null))
            return mismatch(desc + ": " + s1 + " vs. " + s2);

        if (s1 == null) // both are null
            return true;

        if (!s1.equals(s2))
            return mismatch(desc + ": " + s1 + " vs. " + s2);
        return true;
    }
}
