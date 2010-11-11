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
        String u = request.getParameter("file_uri");
        String f = request.getParameter("f");
        int w = getInt(request, "w", 700);
        int h = getInt(request, "h", 800);
        int page = getInt(request, "p", 1);
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
                byte[] image = new ThumbnailService().generateThumbnail(u.toString(), w, h,f,"", page - 1);
                writeImage(f, image, response);
        } catch (Exception e) {
                String str = e.getMessage();
                writeImage(str, response);
                return;
        }
 }       

 private void writeImage(String f, byte[] image, HttpServletResponse response) throws IOException {
        response.setContentType("image/"+f);
        OutputStream outputStream = response.getOutputStream();
        //ImageIO.write(image, f, outputStream);\
        outputStream.write(image);
        outputStream.close();
 }

 private void writeImage(String str, HttpServletResponse response) throws IOException {
        String f = "png";
        BufferedImage image = image(str);
        OutputStream outputStream = response.getOutputStream();
        ImageIO.write(image, f, outputStream);
        outputStream.close();
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
