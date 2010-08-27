package org.simplx.xml;

import static org.simplx.xml.Simplx.*;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Iterator;

public class TestSimplxUtils {
    @Test
    public void testFirstElem() {
        Document d = doc("testDoc");
        Element top = d.getDocumentElement();
        Element e;
        append(d, "Some text", cdata("cdata"), e = elem("first"), elem("last"));

        assertSame(e, SimplxUtils.firstElement(top));
        assertNull(SimplxUtils.firstElement(e));
    }

    @Test
    public void testTextContent() {
        Document d = doc("testDoc");
        Element top = d.getDocumentElement();
        append(d, elem("first", "Some", "text\n", cdata("cdata")), elem("mid"),
                elem("mid"), elem("last"));

        assertEquals(SimplxUtils.getChildText(top, "first"),
                "Some\ntext\n" + "cdata");
        assertEquals(SimplxUtils.getChildText(top, "last"), "");
        assertNull(SimplxUtils.getChildText(top, "none"));
        try {
            String str = SimplxUtils.getChildText(top, "mid");
            fail("Got string str: " + str);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("<mid>"));
        }
    }

    @Test
    public void testGetChildElements() {
        Document d = doc("testDoc");
        Element top = d.getDocumentElement();
        Element f;
        Element l;
        append(d, f = elem("first", "Some", "text\n", cdata("cdata")), elem(
                "mid"), elem("mid"), l = elem("last"));

        @SuppressWarnings({"TooBroadScope"}) Iterator<Element> iter;

        iter = SimplxUtils.getChildElements(top, "*").iterator();
        assertEquals(iter.next().getNodeName(), "first");
        assertEquals(iter.next().getNodeName(), "mid");
        assertEquals(iter.next().getNodeName(), "mid");
        assertEquals(iter.next().getNodeName(), "last");
        assertFalse(iter.hasNext());

        iter = SimplxUtils.getChildElements(top, "first").iterator();
        assertEquals(iter.next().getNodeName(), "first");
        assertFalse(iter.hasNext());

        iter = SimplxUtils.getChildElements(top, "mid").iterator();
        assertEquals(iter.next().getNodeName(), "mid");
        assertEquals(iter.next().getNodeName(), "mid");
        assertFalse(iter.hasNext());

        iter = SimplxUtils.getChildElements(top, "none").iterator();
        assertFalse(iter.hasNext());

        // No element children
        assertTrue(SimplxUtils.getChildElements(f, "*").isEmpty());
        assertTrue(SimplxUtils.getChildElements(f, "none").isEmpty());

        // No children at all
        assertTrue(SimplxUtils.getChildElements(l, "*").isEmpty());
        assertTrue(SimplxUtils.getChildElements(l, "none").isEmpty());
    }

    @Test
    public void testAttrValue() {
        Document d = doc("testDoc");
        Element top = d.getDocumentElement();
        append(top, attr("attr", "value"));

        assertEquals(SimplxUtils.getAttrValue(top, "attr", "default"), "value");
        assertEquals(SimplxUtils.getAttrValue(top, "noAttr", "default"),
                "default");
    }
}