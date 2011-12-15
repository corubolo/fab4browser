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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import uk.ac.liv.ffmpeg.format.AVFormat;
import uk.ac.liv.ffmpeg.format.mxf.ByteWriter;

import multivalent.Browser;
import multivalent.CLGeneral;
import multivalent.INode;
import multivalent.StyleSheet;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.LeafUnicode;
import multivalent.node.FixedLeafImage;
import multivalent.gui.VButton;

import javax.imageio.ImageIO;


public class MXFDocument extends IVBox {

	 public static String getExtension(String s) {
		 String ext = null;
		 int i = s.lastIndexOf('.');

		 if (i > 0 &&  i < s.length() - 1) {
			 ext = s.substring(i+1).toLowerCase();
		 }
		 return ext;
	 }
	 
	 Thread thrPlay;
	 Thread thrDecode;

	
	VideoState	is;
    
    FixedLeafImage video;
    FixedLeafImage sound;
    
    BufferedImage current_frame;
    
    
    int vid_w= -1;
	int vid_h = -1;
	
	Image nextI;
	Image prevI;
	Image next2I;
	Image prev2I;
	Image playI;
	Image pauseI;
	
	INode body;
	FixedLeafImage playL;
	
	
	boolean isPlaying = false;
	
	
	
    
    WavImage waveIm;
    
        
    
    public MXFDocument(String name, Map<String, Object> attr, INode parent) {
		super(name, attr, parent);

		// Register all formats and codecs
		AVFormat.av_register_all();
		
		is = new VideoState(getDocument().getURI());
		
		waveIm = new WavImage(is);
		
		is.schedule_refresh(40);
		

		Runnable r = new Runnable() {
			public void run() {		
				decode_thread();
			}	
		};
	
		thrDecode = new Thread(r);
		thrDecode.start();
		
		while (!is.is_initiliazed()) {
			try {
				Thread.sleep(100); 
			} catch (Exception e) {}  		
		}
		initUI();
	}
    
    
    
	private void initIcons() {
		Browser br = getBrowser();
		try {
			nextI = ImageIO.read(br.getClass().getResource("/sys/images/next.png"));
			prevI = ImageIO.read(br.getClass().getResource("/sys/images/prev.png"));
			next2I = ImageIO.read(br.getClass().getResource("/sys/images/next2.png"));
			prev2I = ImageIO.read(br.getClass().getResource("/sys/images/prev2.png"));
			playI = ImageIO.read(br.getClass().getResource("/sys/images/play.png"));
			pauseI = ImageIO.read(br.getClass().getResource("/sys/images/pause.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	// Fast forward (more than 1 frame in the future)
    public void next_frame2() {    	
    	is.seek_delta(100);
    	is.flush_queues();    	
    	next_frame();    	
    }

    
    // Fast backward (more than 1 frame in the past)
    public void previous_frame2() {   	
    	is.seek_delta(-100);
    	is.flush_queues();    	
    	next_frame();    	
    }
    
    
    
    public void next_frame() {
    	// Video decoding
    	current_frame = is.getFirstFrame();
    	if (current_frame != null) {
	    	video.setImage(current_frame);
	    	video.repaint();
    	}
    	
    	// Audio Decoding
    	Long [] audio_buf = is.getFirstSample();
		
		int i = 0;
		for (Long nb: audio_buf) {
			short sample = nb.shortValue();
			is.addSample(i, sample);	
			
			i++;
			if (i == is.get_channels())
				i = 0;
		}
		
    	BufferedImage imA = waveIm.getImage(is.frame_to_second(is.get_seek_frame_ref()), 
    										is.frame_to_second(is.get_nb_frames()));
    	if (imA != null) {
	    	sound.setImage(imA);
	    	sound.repaint();
    	}
    	
    }
    
    public void previous_frame() { 	
    	is.seek_delta(-2);
    	is.flush_queues();    	
    	next_frame();
    }
    
    
    public void playpause() {
    	if (isPlaying()) {
    		setIsPlaying(false);
    	} else {
    		setIsPlaying(true);
			Runnable r = new Runnable() {
				public void run() {				
					while (isPlaying()) {
			    		next_frame();
					}
				}
			};
			
			thrPlay = new Thread(r);
			thrPlay.start();
    	}
    }
    
    
        
    
    synchronized private void setIsPlaying(boolean isPlaying) {
    	this.isPlaying = isPlaying;
    	if (isPlaying()) {
    		playL.setImage(pauseI);
    	} else {
    		playL.setImage(playI);
    	}
    		
    }
    
    
    synchronized private boolean isPlaying() {
    	return isPlaying;
    }
    


	public long get_nb_frames() {
		return is.get_nb_frames();	
	}

	

	public BufferedImage get_current_frame() {
		return current_frame;
	}


	private void decode_thread() {		
		is.init();	
		is.decode();
	}
		
	

	private void initUI() {
		initIcons();
        StyleSheet ss = getDocument().getStyleSheet();
        
		// Header of the document (metadata)

        INode header = new IParaBox("p", null, this);
        
        CLGeneral gs = new CLGeneral();
        gs.setForeground(Color.YELLOW);
        gs.setBackground(Color.BLACK);
        gs.setPadding(8);
        gs.setSize(28.0f);
        
        CLGeneral gs2 = new CLGeneral();
        gs2.setForeground(new Color(192, 192, 255));
        gs2.setBackground( Color.BLACK);
        //gs2.setPadding(8);
        gs2.setSize(16.0f);
        
		IParaBox h1 = new IParaBox("h1", null, header);
        new LeafUnicode(is.get_title(), null, h1);
        ss.put(h1, gs);
        
        for (String s: is.get_metadata()) {
        	IParaBox h2 = new IParaBox("h2", null, header);
        	new LeafUnicode(s, null, h2);
        	ss.put(h2, gs2);
        }
        
        
        CLGeneral gs3 = new CLGeneral();
        gs3.setPadding(8);
        
        // Body of the document (video)

		body = new IParaBox("body", null, this);
		
		INode videoB = new IParaBox("videoB", null, body);  	
		video = new FixedLeafImage("img", null, videoB, is.getFirstFrame());

    	ss.put(videoB, gs3);
		INode soundB = new IParaBox("soundB", null, body);  	
		sound = new FixedLeafImage("snd", null, soundB, 
				                   waveIm.getImage(0.0f, is.frame_to_second(is.get_nb_frames())));
    	ss.put(soundB, gs3);
        
        
        // Footer of the document (toolbar)
		INode footer = new IParaBox("footer", null, this);  	
		ss.put(footer, gs3);

        VButton prev2B = new VButton("Previous frame 2", null, footer, 
        		"event "+ MXFBehaviour.MSG_PREV2 + " button");
        VButton prevB = new VButton("Previous frame", null, footer, 
        		"event "+ MXFBehaviour.MSG_PREV + " button");
        VButton playB = new VButton("Play", null, footer, 
        		"event "+ MXFBehaviour.MSG_PLAYPAUSE + " button");
        VButton nextB = new VButton("Next frame", null, footer, 
        		"event " + MXFBehaviour.MSG_NEXT + " button");
        VButton next2B = new VButton("Next frame 2", null, footer, 
        		"event " + MXFBehaviour.MSG_NEXT2 + " button");

		new FixedLeafImage("prev2", null, prev2B, prev2I);
		new FixedLeafImage("prev", null, prevB, prevI);
		playL = new FixedLeafImage("play", null, playB, playI);
		new FixedLeafImage("next", null, nextB, nextI);
		new FixedLeafImage("next2", null, next2B, next2I);
	}
	

	private void saveFrame(BufferedImage bi, String filename) {
		try {
			File outputfile = new File(filename);
			String ext = getExtension(filename);
			if ( (!ext.equals("png")) && 
				 (!ext.equals("jpg")) && 
				 (!ext.equals("gif")) )
				ext = "png";
			ImageIO.write(bi, ext, outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	

	public void saveFrame(String path, String template, int start_frame, int end_frame, String format) {		
		int cur_frame = is.get_current_frame();
		int seek_frame_ref = is.get_seek_frame_ref();
		
		// Problem interfering with my threads ?
//		ProgressMonitor mon = new ProgressMonitor(getBrowser().getTopLevelAncestor(), 
//				"Extracting", "note", 
//				start_frame, end_frame);
		
		
		is.seek_frame(start_frame);
		
		int i = start_frame;
		while ( (i < end_frame) && (i < is.get_nb_frames()) )  {
			BufferedImage bi = is.getFirstFrame();
			is.skip_first_sample();  // Need to also read the audioq
			saveFrame(bi, path + "/" + template + "_" + i + "." + format);
			//mon.setProgress(i);
			i++;
		}		

		is.seek_frame(cur_frame);
		is.set_seek_frame_ref(seek_frame_ref);
		
	}
	


	public void save_audio_buffer(ArrayList<Long []> audioBuffers, String filename) {
		int size = 0;
		for (Long [] array: audioBuffers) {
			size += array.length * 2;
		}

		long ch = is.get_channels();
		long sampleSec = is.get_sample_rate();
		long bitsSample = is.get_bits_per_coded_sample();
		long bytesSec = ch * sampleSec * bitsSample/ 8;
		long blockAlign = ch * bitsSample / 8;
		
		// 44 = Header size
		ByteWriter writer = new ByteWriter(size + 44);
		
		writer.putString("RIFF");
		writer.putle32(size + 44 - 8);	// Size of file 
			// (not including the "RIFF" and size bytes (-8 bytes)
		writer.putString("WAVE");
		writer.putString("fmt ");
		writer.putle32(16); 			// fmt length
		writer.putle16(0x0001);			// format: WAVE_FORMAT_PCM
		writer.putle16(ch);				// number of channels
		writer.putle32(sampleSec);
		writer.putle32(bytesSec);		
		writer.putle16(blockAlign);		
		writer.putle16(bitsSample);	
		writer.putString("data");
		writer.putle32(size);	
		
		for (Long [] array: audioBuffers) {
			for (Long nb: array) {
				writer.putle16(nb);
			}
		}
		
		writer.dump(filename);
	}
	

	public void saveWAV(String path, String template, int start_frame, int end_frame) {
		
		int cur_frame = is.get_current_frame();
		int seek_frame_ref = is.get_seek_frame_ref();
		
		ArrayList<Long []> audioBuffers = new ArrayList<Long[]>();
		
		is.seek_frame(start_frame);
		
		int i = start_frame;
		while ( (i < end_frame) && (i < is.get_nb_frames()) )  {
			is.skip_first_frame();  // Need to also read the videoq
			audioBuffers.add(is.getFirstSample());
			i++;			
		}

		save_audio_buffer(audioBuffers, path + "/" + template + ".wav");
			

		is.seek_frame(cur_frame);
		is.set_seek_frame_ref(seek_frame_ref);
		
	}



	public void transcode(String path, String template, int start_frame, int end_frame,
			String format) {

		int cur_frame = is.get_current_frame();
		int seek_frame_ref = is.get_seek_frame_ref();
		

		ArrayList<Long []> audioBuffers = new ArrayList<Long[]>();
		
		is.seek_frame(start_frame);
		
		int i = start_frame;
		while ( (i < end_frame) && (i < is.get_nb_frames()) )  {
			BufferedImage bi = is.getFirstFrame();
			audioBuffers.add(is.getFirstSample());
			saveFrame(bi, path + "/" + template + "_" + i + "." + format); 
			i++;
		}		
		
		save_audio_buffer(audioBuffers, path + "/" + template + ".wav");
		

		is.seek_frame(cur_frame);
		is.set_seek_frame_ref(seek_frame_ref);
		
	}



	public void stop_thread() {
		//thrDecode.stop();
		//thrPlay.stop();
		
	}
	

}
