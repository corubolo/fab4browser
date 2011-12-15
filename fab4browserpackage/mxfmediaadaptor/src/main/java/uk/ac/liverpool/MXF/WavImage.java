/******************************************************************************
 *  
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 3 of the License, or (at your option) 
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Author     : Jerome Fuselier
 * Creation   : September 2011
 *  
 *****************************************************************************/

package uk.ac.liverpool.MXF;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class WavImage {
	

    protected static final Color BACKGROUND_COLOR = Color.gray;
    protected static final Color REFERENCE_LINE_COLOR = Color.black;
    protected static final Color WAVEFORM_COLOR = Color.yellow;
    
    
    VideoState is;
    int width;
    int height;
    
    
	public WavImage(VideoState is) {
		super();
		this.is = is;
	}


	private void drawAxis(Graphics g, float lengthTime) {
		// Draw ref
		int lineHeight = height / 2;
		g.setColor(REFERENCE_LINE_COLOR);
		g.drawLine(0, lineHeight, width, lineHeight);
		
		// Draw X legends
		
		// 1/4
		int x = width / 4;
		float value = lengthTime / 4;
		value = Math.round(value*100)/(float)100;
		String cap = String.valueOf(value);
		g.drawLine(x, lineHeight - 5, 
				   x, lineHeight + 5);
		g.drawString(cap, x-10, lineHeight + 20);		
		
		// 1/2
		x = width / 2;
		value = lengthTime / 2;
		value = Math.round(value*100)/(float)100;
		cap = String.valueOf(value);
		g.drawLine(x, lineHeight - 5, 
				   x, lineHeight + 5);
		g.drawString(cap, x-10, lineHeight + 20);
		
		// 3/4
		x = 3 * width / 4;
		value = 3 * lengthTime / 4;
		value = Math.round(value*100)/(float)100;
		cap = String.valueOf(value);
		g.drawLine(x, lineHeight - 5, 
				   x, lineHeight + 5);
		g.drawString(cap, x-10, lineHeight + 20);
		
	}
	

	private void drawWaveForm(Graphics g, float frame_ref, float duration_time) {
		ArrayList <Short> samples = is.getChannel(0); // 1st channel
		
		if (samples != null) {
			
			int sampleRate = is.get_sample_rate();
			int nbSamples = samples.size();
			
			//float lengthTime = nbSamples / (float) sampleRate;
			float lengthTime = duration_time * 48000 / (float) sampleRate;
			
			drawAxis(g, lengthTime);
			
			short sampleMax = 0;
			
			if (is.get_max_audio(0) > is.get_min_audio(0)) {
				sampleMax = is.get_max_audio(0);
	        } else {
	        	sampleMax = (short)Math.abs(is.get_min_audio(0));
	        }
	
	        g.setColor(WAVEFORM_COLOR);
	        
	
	        int increment = 60 * 48000 / width;
	        if (nbSamples > 60 * 48000) {
	        	increment = nbSamples / width;	
	        } 
			
			int oldX = (int) (width * frame_ref / lengthTime);//0;
		    int oldY = (int) (height/ 2);
		    
		    if (oldX != 0) {
		    	g.drawLine(oldX, 0, oldX, height); // draw reference start time
		    }
			
			
		    int xIndex = (int) (width * frame_ref / lengthTime);//0;
	        double yScaleFactor = height / (sampleMax * 2 * 1.2);
	
	        
			for (int t = 0 ; t < nbSamples ; t += increment) {
				double scaledSample = samples.get(t) * yScaleFactor;
	            int y = (int) ((height / 2) - (scaledSample));
	            g.drawLine(oldX, oldY, xIndex, y);
	            oldX = xIndex;
	            oldY = y;            
	            xIndex++;  
			}
			
		}
	}
	

	public BufferedImage getImage(float frame_ref, float duration_time) {
		width = is.get_width();
		height = 100;
		BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = im.getGraphics();

		// Fill Background
		g.setColor(BACKGROUND_COLOR);
		g.fillRect (0, 0, width, height);
		
		// Draw wave form
		drawWaveForm(g, frame_ref, duration_time);
		
		g.dispose();
		return im;
	}


	
	
	

}
