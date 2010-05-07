package org.simplx.xml;

import static org.simplx.xml.Simplx.*;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerException;

@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class TestSimplx {
    @Test
    public void attrMethods() throws TransformerException {
        Document doc = doc("gui");
        append(doc, elem("widget", attrs("x=12 y=\"a\"c\" z=qwerty")));

        Document expected = doc("gui");
        append(expected, elem("widget", attrs("x", "12", "y", "a\"c"), attr("z",
                "qwerty")));

        dumpDocs(doc, expected);

        assertTrue(new CompareXML().match(expected, doc, "doc"));
    }

    private void dumpDocs(Node doc, Node expected) throws TransformerException {
        System.out.println("... doc");
        SimplxUtils.write(doc, System.out);

        System.out.println("... expected");
        SimplxUtils.write(expected, System.out);
    }

    @Test
    public void noteDoc() throws TransformerException {

        Document doc = doc("notes");
        append(doc, elem("note", elem("to", "Tove"), elem("from", "Jani"), elem(
                "heading", "Reminder"), elem("body",
                "Don't forget me this weekend!")));

        Document expected = SimplxUtils.readDoc(getClass().getResourceAsStream(
                "note.xml"));

        dumpDocs(doc, expected);

        assertTrue(new CompareXML().match(expected, doc, "doc"));
    }

    @Test
    public void catalogDoc() throws TransformerException {

        Document doc = doc("catalog");
        append(doc, elem("book", attr("id", "bk101"), elem("author",
                "Gambardella, Matthew"), elem("title", "XML Developer's Guide"),
                elem("genre", "Computer"), elem("price", "44.95"), elem(
                        "publish_date", "2000-10-01"), elem("description",
                        "An in-depth look at creating applications",
                        "with XML.")), elem("book", attr("id", "bk102"), elem(
                "author", "Ralls, Kim"), elem("title", "Midnight Rain"), elem(
                "genre", "Fantasy"), elem("price", "5.95"), elem("publish_date",
                "2000-12-16"), elem("description",
                "A former architect battles corporate zombies,",
                "an evil sorceress, and her own childhood to become queen",
                "of the world.")), elem("book", attr("id", "bk103"), elem(
                "author", "Corets, Eva"), elem("title", "Maeve Ascendant"),
                elem("genre", "Fantasy"), elem("price", "5.95"), elem(
                        "publish_date", "2000-11-17"), elem("description",
                        "After the collapse of a nanotechnology",
                        "society in England, the young survivors lay the",
                        "foundation for a new society.")), elem("book", attr(
                "id", "bk104"), elem("author", "Corets, Eva"), elem("title",
                "Oberon's Legacy"), elem("genre", "Fantasy"), elem("price",
                "5.95"), elem("publish_date", "2001-03-10"), elem("description",
                "In post-apocalypse England, the mysterious",
                "agent known only as Oberon helps to create a new life",
                "for the inhabitants of London. Sequel to Maeve",
                "Ascendant.")), elem("book", attr("id", "bk105"), elem("author",
                "Corets, Eva"), elem("title", "The Sundered Grail"), elem(
                "genre", "Fantasy"), elem("price", "5.95"), elem("publish_date",
                "2001-09-10"), elem("description",
                "The two daughters of Maeve, half-sisters,",
                "battle one another for control of England. Sequel to",
                "Oberon's Legacy.")), elem("book", attr("id", "bk106"), elem(
                "author", "Randall, Cynthia"), elem("title", "Lover Birds"),
                elem("genre", "Romance"), elem("price", "4.95"), elem(
                        "publish_date", "2000-09-02"), elem("description",
                        "When Carla meets Paul at an ornithology",
                        "conference, tempers fly as feathers get ruffled.")),
                elem("book", attr("id", "bk107"), elem("author",
                        "Thurman, Paula"), elem("title", "Splish Splash"), elem(
                        "genre", "Romance"), elem("price", "4.95"), elem(
                        "publish_date", "2000-11-02"), elem("description",
                        "A deep sea diver finds true love twenty",
                        "thousand leagues beneath the sea.")), elem("book",
                        attr("id", "bk108"), elem("author", "Knorr, Stefan"),
                        elem("title", "Creepy Crawlies"), elem("genre",
                                "Horror"), elem("price", "4.95"), elem(
                                "publish_date", "2000-12-06"), elem(
                                "description",
                                "An anthology of horror stories about roaches,",
                                "centipedes, scorpions  and other insects.")),
                elem("book", attr("id", "bk109"), elem("author",
                        "Kress, Peter"), elem("title", "Paradox Lost"), elem(
                        "genre", "Science Fiction"), elem("price", "6.95"),
                        elem("publish_date", "2000-11-02"), elem("description",
                                "After an inadvertant trip through a Heisenberg",
                                "Uncertainty Device, James Salway discovers the problems",
                                "of being quantum.")), elem("book", attr("id",
                        "bk110"), elem("author", "O'Brien, Tim"), elem("title",
                        "Microsoft .NET: The Programming Bible"), elem("genre",
                        "Computer"), elem("price", "36.95"), elem(
                        "publish_date", "2000-12-09"), elem("description",
                        "Microsoft's .NET initiative is explored in",
                        "detail in this deep programmer's reference.")), elem(
                        "book", attr("id", "bk111"), elem("author",
                                "O'Brien, Tim"), elem("title",
                                "MSXML3: A Comprehensive Guide"), elem("genre",
                                "Computer"), elem("price", "36.95"), elem(
                                "publish_date", "2000-12-01"), elem(
                                "description",
                                "The Microsoft MSXML3 parser is covered in",
                                "detail, with attention to XML DOM interfaces, XSLT processing,",
                                "SAX and more.")), elem("book", attr("id",
                        "bk112"), elem("author", "Galos, Mike"), elem("title",
                        "Visual Studio 7: A Comprehensive Guide"), elem("genre",
                        "Computer"), elem("price", "49.95"), elem(
                        "publish_date", "2001-04-16"), elem("description",
                        "Microsoft Visual Studio 7 is explored in depth,",
                        "looking at how Visual Basic, Visual C++, C#, and ASP+ are",
                        "integrated into a comprehensive development",
                        "environment.")));

        Document expected = SimplxUtils.readDoc(getClass().getResourceAsStream(
                "catalog.xml"));
        assertTrue(new CompareXML().match(expected, doc, "doc"));
    }
}
