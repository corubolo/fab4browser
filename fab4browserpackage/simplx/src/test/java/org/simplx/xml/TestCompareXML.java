package org.simplx.xml;

import org.simplx.xml.CompareXML.IgnoreTester;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.transform.TransformerException;
import java.io.IOException;

@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class TestCompareXML {
    private Document basicDoc;

    @Test
    public void testSimpleComparison()
            throws TransformerException, IOException {

        Document doc = getBasicDoc();
        CompareXML compare = new CompareXML();

        expectMatch(compare, doc, doc, "self");
        IgnoreTester ignoreTester = compare.getIgnoreTester();
        assertSame(ignoreTester, CompareXML.IGNORE_NOTHING, "default tester");

        Document mod = (Document) doc.cloneNode(true);
        expectMatch(compare, doc, mod, "copy");
        assertSame(compare.getIgnoreTester(), CompareXML.IGNORE_NOTHING,
                "default tester");
    }

    private void expectMatch(CompareXML compare, Node n1, Node n2,
            String name) {
        assertTrue(compare.match(n1, n2, "matches self"),
                "doc matches " + name);
        assertNull(compare.getMismatch(), "no mistmach exception");

        assertTrue(compare.equals(n1, n2), "doc equals " + name);
        assertNull(compare.getMismatch(), "no mistmach exception");
    }

    @Test
    public void testIgnoring() throws TransformerException {
        Document doc = getBasicDoc();

        Document mod = (Document) doc.cloneNode(true);
        Simplx.append(mod, "newChild");

        CompareXML compare = new CompareXML();

        // with nothing ingored, this is a mismatch
        assertFalse(compare.equals(doc, mod),
                "doc doesn't match modified copy");
        XMLMismatchException mismatchException = compare.getMismatch();
        assertNotNull(mismatchException.getClass());
        assertSame(compare.getIgnoreTester(), CompareXML.IGNORE_NOTHING,
                "default tester");

        try {
            compare.match(doc, mod, "modified copy");
            fail("expected mismatch not thrown");
        } catch (XMLMismatchException e) {
            assertSame(e, compare.getMismatch());
        }

        compare.setIgnoreTester(new IgnoreTester() {
            public boolean ignore(Node node) {
                return node instanceof Text &&
                        node.getTextContent().trim().equals("newChild");
            }
        });

        expectMatch(compare, doc, mod, "copy");
    }

    private synchronized Document getBasicDoc() throws TransformerException {
        if (basicDoc == null) {
            basicDoc = SimplxUtils.readDoc(getClass().getResourceAsStream(
                    "catalog.xml"));
        }
        return basicDoc;
    }
}