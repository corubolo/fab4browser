/*******************************************************************************
 * This Library is :
 * 
 *     Copyright © 2010 Fabio Corubolo - all rights reserved
 *     corubolo@gmail.com
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * see COPYING.LESSER.txt
 * 
 ******************************************************************************/
package uk.ac.liverpool.thumbnails;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.jws.WebMethod;
import javax.jws.WebService;


import com.im.file.FileMagic;

@WebService(targetNamespace = "http://shaman.liv.ac.uk/", name = "ThumbnailService")
public class ThumbnailService {



    private static final Set<String> allowed = new HashSet<String>();
    static {
        allowed.add("http://media.ugs.com/teamcenter/jtfiles/conrod.jt");
        allowed.add( "http://media.ugs.com/teamcenter/jtfiles/butterflyvalve.jt");
        allowed.add(  "http://media.ugs.com/teamcenter/jtfiles/bnc.jt");
        allowed.add( "http://www.schoolhistory.co.uk/year7links/1066/battlehastings.ppt");
        allowed.add( "http://www.xfront.com/REST-full.ppt");
        allowed.add( "http://java.sun.com/docs/books/jls/download/langspec-3.0.pdf");
        allowed.add(   "http://www.ctan.org/tex-archive/info/lshort/english/lshort.pdf");
        allowed.add(   "http://manuals.info.apple.com/en/iphone_user_guide.pdf");


    }

    private static GenericService[] services = new GenericService[]{
        new JTService(), new PDFService(), new PPTService(),  
    };


    public static Map<String, GenericService> map = new HashMap<String, GenericService>();
    static { 
        for (GenericService s: services) {
            map.put(s.getSupportedMime(), s);
        }
    }

    @WebMethod(exclude = true)
    public static void main(String[] args) throws MalformedURLException,
    IOException {
        System.out.println("Starting Server");
        String address = "http://localhost:8080/ThumbnailService";
        javax.xml.ws.Endpoint.publish(address,
                new ThumbnailService());

    }

    private static Map<URI, File> cacheF = new HashMap<URI, File>();

    public ThumbnailService() {
    }
    
    

    /**
     * 
     * This method will generate a thumbnail image of the object indicated by
     * objectIdentifier (Currently a simple URI). Currently supported formats
     * can be obtained by calling the {@link #getSupportedMimeTypes()}
     * 
     * 
     * @param objectIdentifier
     *            The Object identifier; currently, only URI are supported
     * @param outputWidth
     *            The with of the output image
     * @param outputHeight
     *            The height of the output image
     * @param outputFormat
     *            The output format, as indicate by
     *            {@link #getSupportedOutputType()}
     * @param outputOption
     *            Output writing options (as supported by Java ImageIO)
     * @return a byte[] of the encoded image representation.
     * @throws MalformedURLException
     * @throws IOException
     * @throws ParseException 
     */
    @WebMethod
    public byte[] generateThumbnailFromData(byte[] data, int outputWidth,
            int outputHeight, String outputFormat, String outputOption, int page)
    throws MalformedURLException, IOException {
        //URI u = resolve(objectIdentifier);
        File f = copyToTemp(data, "thservice", "dat");
        String mime = guessFormat(f);

        BufferedImage bi = null;
        GenericService service = map.get(mime);
        System.out.println(mime);
        if (service == null)
            throw new IOException("Unsupported document type");
        bi = service.generateThumb(f.toURI(),f, outputWidth, outputHeight, page);
        if (bi != null) {
            try {
                byte[] b = saveImage(bi, outputFormat, outputOption);
                return b;
            } catch (IOException e) {

                e.printStackTrace();
                return new byte[0];
            }
        } else
            return new byte[0];

    }
    
    
    
    /**
     * 
     * This method will generate a SVG representation of the object indicated by
     * objectIdentifier (Currently a simple URI). Currently supported formats
     * can be obtained by calling the {@link #getSupportedMimeTypes()}
     * 
     * 
     * @param objectIdentifier
     *            The Object identifier; currently, only URI are supported
     * @param outputWidth
     *            The with of the output image
     * @param outputHeight
     *            The height of the output image
     * @param  page Page number to convert
     * @return The String SVG representation.
     * @throws MalformedURLException
     * @throws IOException
     * @throws ParseException 
     */
    @WebMethod
    public String generateSVGThumbnailFromData(byte[] data, int outputWidth,
            int outputHeight, int page)
    throws MalformedURLException, IOException {
        File f = copyToTemp(data, "thservice", "dat");
        String mime = guessFormat(f);


        GenericService service = map.get(mime);
        if (service == null)
            throw new IOException("Unsupported document type");
        File createTempFile = File.createTempFile("tttt", ".svg");
        FileWriter fw = new FileWriter(createTempFile);
        service.generateSVG(f.toURI(),f, outputWidth, outputHeight, page, fw);
        fw.close();
        BufferedReader br = new BufferedReader(new FileReader(createTempFile));
        StringBuffer sb = new StringBuffer();
        char[] buf = new char[10000];
        int cc;
        while ((cc = br.read(buf))!=-1) {
            sb.append(buf, 0, cc);
        }
        
       return br.toString();

    }
    
    
    /**
     * 
     * This method will extract the list of fonts used in the object indicated by
     * objectIdentifier (Currently a simple URI). Currently supported formats
     * can be obtained by calling the {@link #getSupportedMimeTypes()}
     * 
     * 
     * @param objectIdentifier
     *            The Object identifier; currently, only URI are supported
     * @return FontInformation[]
     * @throws MalformedURLException
     * @throws IOException
     */
    @WebMethod
    public FontInformation[] extraxtFontInformationFromData(byte[] data)
    throws MalformedURLException, IOException {
        File f = copyToTemp(data, "thservice", "dat");
        String mime = guessFormat(f);

        GenericService service = map.get(mime);
        if (service == null)
            throw new IOException("Unsupported document type");
        return service.extractFontList(f.toURI(),f);
        
    }
    /**
     * 
     * This method will extract an XML representation of the textual contents of the object indicated by
     * objectIdentifier (Currently a simple URI). Currently supported formats
     * can be obtained by calling the {@link #getSupportedMimeTypes()}
     * 
     * 
     * @param objectIdentifier
     *            The Object identifier; currently, only URI are supported
  
     * @return a String
     * @throws MalformedURLException
     * @throws IOException
     */
    @WebMethod
    public String extraxtXmlTextFromData(byte[] data)
    throws MalformedURLException, IOException {
        File f = copyToTemp(data, "thservice", "dat");
        String mime = guessFormat(f);

        GenericService service = map.get(mime);
        if (service == null)
            throw new IOException("Unsupported document type");

        return service.extraxtXMLText(f.toURI(),f);
        
    }
    /**
     * 
     * This method will generate a thumbnail image of the object indicated by
     * objectIdentifier (Currently a simple URI). Currently supported formats
     * can be obtained by calling the {@link #getSupportedMimeTypes()}
     * 
     * 
     * @param objectIdentifier
     *            The Object identifier; currently, only URI are supported
     * @param outputWidth
     *            The with of the output image
     * @param outputHeight
     *            The height of the output image
     * @param outputFormat
     *            The output format, as indicate by
     *            {@link #getSupportedOutputType()}
     * @param outputOption
     *            Output writing options (as supported by Java ImageIO)
     * @return a byte[] of the encoded image representation.
     * @throws MalformedURLException
     * @throws IOException
     * @throws ParseException 
     */
    @WebMethod
    public byte[] generateThumbnail(String objectIdentifier, int outputWidth,
            int outputHeight, String outputFormat, String outputOption, int page)
    throws MalformedURLException, IOException {
        URI u = resolve(objectIdentifier);
        File f = cache(u);
        String mime = guessFormat(f, u);

        BufferedImage bi = null;
        GenericService service = map.get(mime);
        System.out.println(mime);
        if (service == null)
            throw new IOException("Unsupported document type");
        bi = service.generateThumb(f.toURI(),f, outputWidth, outputHeight, page);
        if (bi != null) {
            try {
                byte[] b = saveImage(bi, outputFormat, outputOption);
                return b;
            } catch (IOException e) {

                e.printStackTrace();
                return new byte[0];
            }
        } else
            return new byte[0];

    }
    
    /**
     * 
     * This method will generate a SVG representation of the object indicated by
     * objectIdentifier (Currently a simple URI). Currently supported formats
     * can be obtained by calling the {@link #getSupportedMimeTypes()}
     * 
     * 
     * @param objectIdentifier
     *            The Object identifier; currently, only URI are supported
     * @param outputWidth
     *            The with of the output image
     * @param outputHeight
     *            The height of the output image
     * @param  page Page number to convert
     * @return The String SVG representation.
     * @throws MalformedURLException
     * @throws IOException
     * @throws ParseException 
     */
    @WebMethod
    public String generateSVGThumbnail(String objectIdentifier, int outputWidth,
            int outputHeight, int page)
    throws MalformedURLException, IOException {
        URI u = resolve(objectIdentifier);
        File f = cache(u);
        String mime = guessFormat(f, u);

        GenericService service = map.get(mime);
        if (service == null)
            throw new IOException("Unsupported document type");
        File createTempFile = File.createTempFile("tttt", ".svg");
        FileWriter fw = new FileWriter(createTempFile);
        service.generateSVG(f.toURI(),f, outputWidth, outputHeight, page, fw);
        fw.close();
        BufferedReader br = new BufferedReader(new FileReader(createTempFile));
        StringBuffer sb = new StringBuffer();
        char[] buf = new char[10000];
        int cc;
        while ((cc = br.read(buf))!=-1) {
            sb.append(buf, 0, cc);
        }
        
       return br.toString();

    }
    
    
    /**
     * 
     * This method will extract the list of fonts used in the object indicated by
     * objectIdentifier (Currently a simple URI). Currently supported formats
     * can be obtained by calling the {@link #getSupportedMimeTypes()}
     * 
     * 
     * @param objectIdentifier
     *            The Object identifier; currently, only URI are supported
     * @return FontInformation[]
     * @throws MalformedURLException
     * @throws IOException
     */
    @WebMethod
    public FontInformation[] extraxtFontInformation(String objectIdentifier)
    throws MalformedURLException, IOException {
        URI u = resolve(objectIdentifier);
        File f = cache(u);
        String mime = guessFormat(f, u);

        GenericService service = map.get(mime);
        if (service == null)
            throw new IOException("Unsupported document type");
        return service.extractFontList(f.toURI(),f);
        
    }
    /**
     * 
     * This method will extract an XML representation of the textual contents of the object indicated by
     * objectIdentifier (Currently a simple URI). Currently supported formats
     * can be obtained by calling the {@link #getSupportedMimeTypes()}
     * 
     * 
     * @param objectIdentifier
     *            The Object identifier; currently, only URI are supported
  
     * @return a String
     * @throws MalformedURLException
     * @throws IOException
     */
    @WebMethod
    public String extraxtXmlText(String objectIdentifier)
    throws MalformedURLException, IOException {
        URI u = resolve(objectIdentifier);
        File f = cache(u);
        String mime = guessFormat(f, u);

        GenericService service = map.get(mime);
        if (service == null)
            throw new IOException("Unsupported document type");

        return service.extraxtXMLText(f.toURI(),f);
        
    }
    
    @WebMethod(exclude = true)
    private File cache(URI u) throws MalformedURLException, IOException {
        File f = cacheF .get(u);
        if (f == null){
         f =  copyToTemp(u.toURL().openStream(),"thum",u.toString().toLowerCase().substring(u.toString().length() - 3));
        cacheF.put(u, f);}
        return f;
    }
    @WebMethod(exclude = true)
    public static File copyToTemp(InputStream is, String st, String en)
    throws IOException {
        if (en == null || en.length()<3 || en.indexOf("/")!=-1 ||  en.indexOf("\\")!=-1)
            en = null;
        File f;
        try {
            f= File.createTempFile(st, en);
        } catch (Exception x){
            f = File.createTempFile(st, null);
        }
        f.deleteOnExit();
        FileOutputStream os = new FileOutputStream(f);
        byte[] buf = new byte[16 * 1024];
        int i;
        while ((i = is.read(buf)) != -1)
            os.write(buf, 0, i);
        is.close();
        os.close();
        return f;

    }
    
    @WebMethod(exclude = true)
    public static File copyToTemp(byte[] data, String st, String en)
    throws IOException {
        if (en == null || en.length()<3 || en.indexOf("/")!=-1 ||  en.indexOf("\\")!=-1)
            en = null;
        File f;
        try {
            f= File.createTempFile(st, en);
        } catch (Exception x){
            f = File.createTempFile(st, null);
        }
        f.deleteOnExit();
        FileOutputStream os = new FileOutputStream(f);
        os.write(data, 0, data.length);
     
        os.close();
        return f;

    }
    @WebMethod(exclude = true)
    private byte[] saveImage(BufferedImage bi, String format, String option)
    throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        ImageIO.write(bi, format, bos);

        byte[] ba = bos.toByteArray();
        return ba;
    }

    
    @WebMethod(exclude = true)
    public String guessFormat(File f) throws IOException {

        FileMagic file1 = new FileMagic("--mime-type", "-b", f.getAbsolutePath());
        StringWriter out = new StringWriter();
        file1.setOutput(out);
        file1.execute();
        String mime = out.toString().trim();
        //String us = u.toString().toLowerCase();
//        if (us.endsWith("jt"))
//            return JTService.JT_MIME;
//        if (mime.contains("application/octet-stream")){
//            if (us.endsWith("ppt"))
//                return PPTService.PPT_MIME;
//            if (us.endsWith("pdf"))
//                return PDFService.PDF_MIME;
        
        return mime;
        //        String us = u.toString().toLowerCase();
        //        if (us.endsWith("jt"))
        //            return JT_MIME;
        //        if (us.endsWith("ppt"))
        //            return PPT_MIME;
        //        if (us.endsWith("pdf"))
        //            return PDF_MIME;
        //        return null;
    }
    
    @WebMethod(exclude = true)
    public String guessFormat(File f, URI u) throws IOException {

        FileMagic file1 = new FileMagic("--mime-type", "-b", f.getAbsolutePath());
        StringWriter out = new StringWriter();
        file1.setOutput(out);
        file1.execute();
        String mime = out.toString().trim();
        String us = u.toString().toLowerCase();
        if (us.endsWith("jt"))
            return JTService.JT_MIME;
        if (mime.contains("application/octet-stream")){
            if (us.endsWith("ppt"))
                return PPTService.PPT_MIME;
            if (us.endsWith("pdf"))
                return PDFService.PDF_MIME;
        }
        return mime;
        //        String us = u.toString().toLowerCase();
        //        if (us.endsWith("jt"))
        //            return JT_MIME;
        //        if (us.endsWith("ppt"))
        //            return PPT_MIME;
        //        if (us.endsWith("pdf"))
        //            return PDF_MIME;
        //        return null;
    }

    @WebMethod
    public URI resolve(String identifier) throws IOException {
        if (!allowed.contains(identifier))
            throw new IOException("Access is allowed only to a sample URI; install a local copy that will work on any URI. ");
        try {
            return new URI(identifier);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    /**
     * Method to obtain a list of supported object formats
     * 
     * @return a list of supported object formats
     */
    @WebMethod
    public String[] getSupportedMimeTypes() {
        return map.keySet().toArray(new String[0]);
    }

    /**
     * Method to obtain a list of supported output formats, as supported by Java
     * ImageIO
     * 
     * @return list of supported output formats
     */
    @WebMethod
    public String[] getSupportedOutputType() {
        return ImageIO.getWriterFormatNames();
    }
}
