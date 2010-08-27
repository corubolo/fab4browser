package multivalent;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import multivalent.IDInfo.Confidence;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static multivalent.IDInfo.Confidence.*;
import static multivalent.MediaAdaptor.*;
import static org.testng.Assert.*;

public class TestIdentify {
    @DataProvider(name = "files")
    public Object[][] files() {
        return new Object[][]{  // tests
                {"jimarnold.jpg", SUFFIX, SUFFIX, new String[]{
                        "IDInfo:confidence=SUFFIX,mimeType=image/jpeg,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.RawImage"}},
                {"jimarnold.jpg", SUFFIX, HEURISTIC, new String[]{
                        "IDInfo:confidence=MAGIC,mimeType=image/jpeg,formatVersion=null,description=JPEG,sourceType=class multivalent.std.adaptor.RawImage",
                        "IDInfo:confidence=MAGIC,mimeType=image/jpeg; charset=binary,formatVersion=null,description=JPEG image data, EXIF standard, comment: \"\",sourceType=class com.im.file.FileMagic"}},
                {"jimarnold.jpg", SUFFIX, MAXIMUM, new String[]{
                        "IDInfo:confidence=PARSE,mimeType=image/jpeg,formatVersion=null,description=JPEG,sourceType=class multivalent.std.adaptor.RawImage",
                        "IDInfo:confidence=MAGIC,mimeType=image/jpeg; charset=binary,formatVersion=null,description=JPEG image data, EXIF standard, comment: \"\",sourceType=class com.im.file.FileMagic"}},
                {"FloorPlans.pdf", SUFFIX, SUFFIX, new String[]{
                        "IDInfo:confidence=SUFFIX,mimeType=application/pdf,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.pdf.PDF"}},
                {"FloorPlans.pdf", SUFFIX, HEURISTIC, new String[]{
                        "IDInfo:confidence=HEURISTIC,mimeType=pdf,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.pdf.PDF",
                        "IDInfo:confidence=MAGIC,mimeType=application/pdf; charset=binary,formatVersion=null,description=PDF document, version 1.4,sourceType=class com.im.file.FileMagic"}},
                {"FloorPlans.pdf", SUFFIX, MAXIMUM, new String[]{
                        "IDInfo:confidence=PARSE,mimeType=application/pdf,formatVersion=1.4,description=pdf,sourceType=class multivalent.std.adaptor.pdf.PDF",
                        "IDInfo:confidence=MAGIC,mimeType=application/pdf; charset=binary,formatVersion=null,description=PDF document, version 1.4,sourceType=class com.im.file.FileMagic"}},
                {"broken_pdf.pdf", SUFFIX, SUFFIX, new String[]{
                        "IDInfo:confidence=SUFFIX,mimeType=application/pdf,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.pdf.PDF"}},
                {"broken_pdf.pdf", SUFFIX, HEURISTIC, new String[]{
                        "IDInfo:confidence=HEURISTIC,mimeType=pdf,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.pdf.PDF",
                        "IDInfo:confidence=MAGIC,mimeType=application/pdf; charset=binary,formatVersion=null,description=PDF document, version 1.4,sourceType=class com.im.file.FileMagic"}},
                {"broken_pdf.pdf", SUFFIX, MAXIMUM, new String[]{
                        "IDInfo:confidence=MAGIC,mimeType=application/pdf; charset=binary,formatVersion=null,description=PDF document, version 1.4,sourceType=class com.im.file.FileMagic"}},
                {"index.html", SUFFIX, SUFFIX, new String[]{
                        "IDInfo:confidence=SUFFIX,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML"}},
                {"index.html", SUFFIX, MAGIC, new String[]{
                        "IDInfo:confidence=MAGIC,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML",
                        "IDInfo:confidence=MAGIC,mimeType=text/html; charset=iso-8859-1,formatVersion=null,description=HTML document text,sourceType=class com.im.file.FileMagic"}},
                {"index.html", SUFFIX, TYPED, new String[]{
                        "IDInfo:confidence=MAGIC,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML",
                        "IDInfo:confidence=MAGIC,mimeType=text/html; charset=iso-8859-1,formatVersion=null,description=HTML document text,sourceType=class com.im.file.FileMagic"}},
                {"index.html", SUFFIX, HEURISTIC, new String[]{
                        "IDInfo:confidence=HEURISTIC,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML",
                        "IDInfo:confidence=HEURISTIC,mimeType=text/plain; charset=ISO-8859-1,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.ASCII",
                        "IDInfo:confidence=MAGIC,mimeType=text/html; charset=iso-8859-1,formatVersion=null,description=HTML document text,sourceType=class com.im.file.FileMagic"}},
                {"index.html", SUFFIX, PARSE, new String[]{
                        "IDInfo:confidence=HEURISTIC,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML",
                        "IDInfo:confidence=HEURISTIC,mimeType=text/plain; charset=ISO-8859-1,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.ASCII",
                        "IDInfo:confidence=MAGIC,mimeType=text/html; charset=iso-8859-1,formatVersion=null,description=HTML document text,sourceType=class com.im.file.FileMagic"}},
                {"index.html", SUFFIX, PROCESS, new String[]{
                        "IDInfo:confidence=HEURISTIC,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML",
                        "IDInfo:confidence=HEURISTIC,mimeType=text/plain; charset=ISO-8859-1,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.ASCII",
                        "IDInfo:confidence=MAGIC,mimeType=text/html; charset=iso-8859-1,formatVersion=null,description=HTML document text,sourceType=class com.im.file.FileMagic"}},
                {"index.html", SUFFIX, MAXIMUM, new String[]{
                        "IDInfo:confidence=HEURISTIC,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML",
                        "IDInfo:confidence=HEURISTIC,mimeType=text/plain; charset=ISO-8859-1,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.ASCII",
                        "IDInfo:confidence=MAGIC,mimeType=text/html; charset=iso-8859-1,formatVersion=null,description=HTML document text,sourceType=class com.im.file.FileMagic"}},
                {"index.html", MAGIC, MAGIC, new String[]{
                        "IDInfo:confidence=MAGIC,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML",
                        "IDInfo:confidence=MAGIC,mimeType=text/html; charset=iso-8859-1,formatVersion=null,description=HTML document text,sourceType=class com.im.file.FileMagic"}},
                {"index.html", HEURISTIC, MAXIMUM, new String[]{
                        "IDInfo:confidence=HEURISTIC,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML",
                        "IDInfo:confidence=HEURISTIC,mimeType=text/plain; charset=ISO-8859-1,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.ASCII"}},
                {"elegance.html", SUFFIX, MAXIMUM, new String[]{
                        "IDInfo:confidence=HEURISTIC,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML",
                        "IDInfo:confidence=HEURISTIC,mimeType=text/plain; charset=ISO-8859-1,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.ASCII",
                        "IDInfo:confidence=MAGIC,mimeType=text/plain; charset=us-ascii,formatVersion=null,description=ASCII English text,sourceType=class com.im.file.FileMagic"}},
                {"elegance.html", SUFFIX, MAGIC, new String[]{
                        "IDInfo:confidence=MAGIC,mimeType=text/plain; charset=us-ascii,formatVersion=null,description=ASCII English text,sourceType=class com.im.file.FileMagic"}},
                {"elegance.html", SUFFIX, SUFFIX, new String[]{
                        "IDInfo:confidence=SUFFIX,mimeType=text/html,formatVersion=null,description=null,sourceType=class multivalent.std.adaptor.HTML"}},
                //end of tests
        };
    }

    @Test
    public void confidences() {
        assertFalse(inRange(MAGIC, SUFFIX, MAGIC));
        assertFalse(inRange(MAGIC, SUFFIX, MAXIMUM));
        assertTrue(inRange(MAGIC, MAGIC, MAGIC));
        assertTrue(inRange(MAGIC, MAGIC, PARSE));
        assertTrue(inRange(MAGIC, HEURISTIC, PARSE));
        assertTrue(inRange(MAGIC, PARSE, PARSE));
        assertFalse(inRange(MAGIC, PARSE, MAGIC));
        assertFalse(inRange(MAGIC, PARSE, HEURISTIC));
    }

    @Test(dataProvider = "files")
    public void testFiles(String file, Confidence min, Confidence max,
            String[] out) throws IOException {

        File top = new File("src/test/resources/multivalent");
        System.out.println("top: " + top.getAbsolutePath());
        assertTrue(top.exists() && top.isDirectory());

        String path = new File(top, file).getCanonicalPath();
        IDInfo[] ids = Identify.identify(min, max, path);
        System.out.printf("%s: (%d responses)%n", path, ids.length);
        for (int i = 0; i < ids.length; i++) {
            IDInfo id = ids[i];
            System.out.printf("%5d: %s%n", i, id);
        }

        assertEquals(ids.length, out.length, file + ": number of responses");
        for (int i = 0; i < ids.length; i++) {
            IDInfo id = ids[i];
            assertEquals(id.toString().trim(), out[i],
                    file + ": response " + i);
        }
    }
}