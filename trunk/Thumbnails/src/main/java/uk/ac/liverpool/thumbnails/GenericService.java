package uk.ac.liverpool.thumbnails;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;



public interface GenericService {
    /**
     * Generates a thumb
     * @param u Uri 
     * @param f File cache if existing (can be null)
     * @param w width
     * @param h height 
     * @param page page number
     * @return a BufferedImage for the desired page
     * @throws MalformedURLException
     * @throws IOException
     */
    public BufferedImage generateThumb(URI u, File f, int w, int h, int page) throws MalformedURLException, IOException;
    
    public String getSupportedMime();
    
    public  FontInformation[] extractFontList(URI u, File fff) throws MalformedURLException, IOException;
    
    public  String extraxtXMLText(URI u, File f) throws MalformedURLException,
    IOException ;
    
    public void generateSVG(URI u, File f, int w, int h, int page, Writer out) throws MalformedURLException, IOException;

}