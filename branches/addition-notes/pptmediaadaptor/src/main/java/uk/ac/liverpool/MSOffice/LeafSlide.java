/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms 
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, 
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  * 
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 *  
 *******************************************************************************/
package uk.ac.liverpool.MSOffice;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import multivalent.Context;
import multivalent.INode;
import multivalent.Leaf;

import org.apache.poi.hslf.model.Slide;

/**
 * Simple class that draws a complete slide (using the APACHE POI library) in Mulitvalent, using Graphics2d.
 * Caching added to the POI library allow constant repaint of the slide, as opposed to the painting of a rendered image of the slide as previously. 
 * @author fabio
 *
 */

public class LeafSlide extends Leaf {
        private Slide slide;
		private int width;
		private int height;
		private double zoom;
		
		/**
		 * This constructor takes, apart from teh usual attributes, the POI slide, and the zoom factor
		 * 
		 * @param name 
		 * @param attr
		 * @param parent
		 * @param s
		 * @param zoomFactor
		 */

		public LeafSlide(String name, Map<String, Object> attr, INode parent,Slide s, double zoomFactor) {
        super(name, attr, parent);
        slide = s;
        zoom= zoomFactor;
        Dimension pgsize = slide.getSlideShow().getPageSize();
    	width = (int) (pgsize.width * zoom);
		height = (int) (pgsize.height * zoom);

    }

    public int getWidth() {
		return width;
	}
    public int getHeight() {
		return height;
	}
    @Override
	public boolean formatNodeContent(Context cx, int start, int end) {
    	bbox.setSize(width, height);
        return !valid_;
    }


    @Override
	public boolean paintNodeContent(Context cx, int start, int end) {
        Graphics2D graphics = cx.g;
    	graphics.setPaint(Color.white);
		graphics.fill(new Rectangle2D.Float(0, 0, width, height));
		graphics.scale(zoom, zoom);
		try {
		slide.draw(graphics);
		} catch (Exception e){
			e.printStackTrace();
		}
		//graphics.dispose();
			
    	return false;
    }



}
