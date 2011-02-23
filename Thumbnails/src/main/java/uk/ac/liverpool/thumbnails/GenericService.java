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
    
    public String[] getSupportedMimes();
    
    public  FontInformation[] extractFontList(URI u, File fff) throws MalformedURLException, IOException;
    
    public  String extractXMLText(URI u, File f) throws MalformedURLException,
    IOException ;
    
    public void generateSVG(URI u, File f, int w, int h, int page, Writer out) throws MalformedURLException, IOException;

}
