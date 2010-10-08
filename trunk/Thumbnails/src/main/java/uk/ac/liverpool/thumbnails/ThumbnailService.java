package uk.ac.liverpool.thumbnails;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;
import javax.jws.WebMethod;
import javax.jws.WebService;
  
@WebService(targetNamespace = "http://shaman.liv.ac.uk/", name = "ThumbnailService")
public class ThumbnailService {

    private static final String JT_MIME = "application/x-jt";
    private static final String PPT_MIME = "application/vnd.ms-powerpoint";

    public static String[] supported = new String[] { JT_MIME, PPT_MIME };

    @WebMethod(exclude = true)
    public static void main(String[] args) throws MalformedURLException,
            IOException {
        System.out.println("Starting Server");
        String address = "http://localhost:9090/ThumbnailClient";
        javax.xml.ws.Endpoint.publish(address,
                new ThumbnailService());

    }

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
     */
    @WebMethod
    public byte[] generateThumbnail(String objectIdentifier, int outputWidth,
            int outputHeight, String outputFormat, String outputOption)
            throws MalformedURLException, IOException {
        URI u = resolve(objectIdentifier);
        String mime = guessFormat(u);
        BufferedImage bi = null;
        if (mime == null)
            throw new IOException("Unsupported document type");
        if (mime.equals(JT_MIME)) {
            bi = JTService.generateJTThumb(u, outputWidth, outputHeight);
        } else if (mime.equals(PPT_MIME)) {
            bi = PPTService.generatePPTThumb(u, outputWidth, outputHeight);
        } else {
                throw new IOException("Unsupported document type");
        }
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

    @WebMethod(exclude = true)
    private byte[] saveImage(BufferedImage bi, String format, String option)
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        ImageIO.write(bi, format, bos);

        byte[] ba = bos.toByteArray();
        return ba;
    }

    @WebMethod
    public String guessFormat(URI u) {
        String us = u.toString().toLowerCase();
        if (us.endsWith("jt"))
            return JT_MIME;
        if (us.endsWith("ppt"))
            return PPT_MIME;
        return null;
    }

    @WebMethod
    public URI resolve(String identifier) throws IOException {
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
        return supported;
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
