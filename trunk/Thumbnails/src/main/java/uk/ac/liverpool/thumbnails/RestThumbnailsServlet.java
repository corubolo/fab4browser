package uk.ac.liverpool.thumbnails;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class RestThumbnailsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String u = request.getParameter("jt_uri");
        String f = request.getParameter("f");
        int w = getInt(request, "w", 700);
        int h = getInt(request, "h", 800);
        URI uri = null;
        try {
                uri = new URI(u);
        } catch (Exception e) {
                String str = u + "seems to be an invalid URI.";
                writeImage(str, response);
                return;
        }

        if (f != null) {
                Iterator writers = javax.imageio.ImageIO.getImageWritersByFormatName(f);
                if (!writers.hasNext()) {
                        String[] formatNames = ImageIO.getWriterFormatNames();
                        formatNames = unique(formatNames);
                        String str = "Supported formats:";
                        for (int i = 0; i < formatNames.length; i++) {
                                String n = formatNames[i];
                                str += "["+n +"], ";
                        }
                        writeImage(str, response);
                        return;
                }
        } else { // f != null
                f = "png";
        }
        try {
                BufferedImage image = JTService.generateJTThumb(uri, w, h);
                writeImage(f, image, response);
        } catch (Exception e) {
                String str = e.getMessage();
                writeImage(str, response);
                return;
        }
 }       

 private void writeImage(String f, BufferedImage image, HttpServletResponse response) throws IOException {
        response.setContentType("image/"+f);
        OutputStream outputStream = response.getOutputStream();
        ImageIO.write(image, f, outputStream);
        outputStream.close();
 }

 private void writeImage(String str, HttpServletResponse response) throws IOException {
        String f = "png";
        BufferedImage image = image(str);
        writeImage(f, image, response);
 }
 
 
 private BufferedImage image(String str) {
    int w = 500;
        int h = 300;
    BufferedImage buffer =
            new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics g = buffer.createGraphics();
        g.setColor(Color.orange);
        g.fillRect(0,0,w,h);
        g.setColor(Color.black);
        g.drawString(str, 00, 100);
        return buffer;
 }
 
 private int getInt(HttpServletRequest request, String name, int theDefault) {
        String s = request.getParameter(name);
        if (s == null)
                return theDefault;
        try {
           return Integer.parseInt(s);
        } catch (NumberFormatException e) {
                return theDefault;
        }
 }
 
private String[] unique(String[] strings) {
    Set set = new HashSet();
    for (int i=0; i<strings.length; i++) {
        String name = strings[i].toLowerCase();
        set.add(name);
    }
    return (String[])set.toArray(new String[0]);
}       
}
