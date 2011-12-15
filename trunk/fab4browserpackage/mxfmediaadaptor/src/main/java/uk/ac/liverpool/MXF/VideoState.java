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

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;

import uk.ac.liv.ffmpeg.AVPacket;
import uk.ac.liv.ffmpeg.AVPacketList;
import uk.ac.liv.ffmpeg.AVStream;
import uk.ac.liv.ffmpeg.FfmpegConstants.AVMediaType;
import uk.ac.liv.ffmpeg.codecs.AVCodec;
import uk.ac.liv.ffmpeg.codecs.AVCodecContext;
import uk.ac.liv.ffmpeg.format.AVFormat;
import uk.ac.liv.ffmpeg.format.AVFormatContext;
import uk.ac.liv.ffmpeg.util.AVRational;


public class VideoState {

	public static int MAX_AUDIOQ_SIZE = 10;
	public static int MAX_VIDEOQ_SIZE = 10;
	
	boolean initialized = false;
	
	URI uri;

    AVFormatContext formatCtx;
    AVCodecContext vCodecCtx;
    AVCodec vCodec;
    AVCodecContext aCodecCtx;
    AVCodec aCodec;
    

	int video_index;
	int audio_index;

	
	AVStream audio_stream;
	AVPacketList audioq;
	
	AVStream video_stream;		
	AVPacketList videoq;

	int video_delay;
	int current_frame;
	int seek_frame_ref; // the last seek frame (if we seek at the middle we keep that reference)
	int nb_frames;
	
	// Video parameters
	int width;
	int height;
	
	// Audio parameters
	int sample_rate;
	int channels;	
	ArrayList <ArrayList<Short>> channelsBuffers;
	ArrayList<Short> maxAudio;
	ArrayList<Short> minAudio;
	ArrayList<Long []> audioBuffers;
	
	double video_current_pts;
	double video_current_pts_time; 
	
	
	public VideoState(URI uri) {
		super();
		this.uri = uri;

		this.audioq = new AVPacketList();
		this.videoq = new AVPacketList();
		
		channelsBuffers = new ArrayList<ArrayList<Short>> ();
		maxAudio = new ArrayList<Short>();
		minAudio = new ArrayList<Short>();
		audioBuffers = new ArrayList<Long[]>();		
	}
	
	public boolean is_initiliazed() {
		return initialized;
	}

	public void set_seek_frame_ref(int seek_frame_ref) {
		this.seek_frame_ref = seek_frame_ref;
	}


	public synchronized int get_current_frame() {
		return current_frame;
	}
	
	public synchronized void set_current_frame(int current_frame) {
		this.current_frame = current_frame;
	}
	

	public int get_width() {
		return width;
	}


	public int get_height() {
		return height;
	}


	public AVFormatContext getFormatCtx() {
		return formatCtx;
	}


	public void setFormatCtx(AVFormatContext formatCtx) {
		this.formatCtx = formatCtx;
	}


	public int getVideoStream() {
		return video_index;
	}


	public void setVideoStream(int videoStream) {
		this.video_index = videoStream;
	}


	public int getAudioStream() {
		return audio_index;
	}


	public void setAudioStream(int audioStream) {
		this.audio_index = audioStream;
	}


	public AVStream getAudio_st() {
		return audio_stream;
	}


	public void setAudio_st(AVStream audio_st) {
		this.audio_stream = audio_st;
	}


	public AVPacketList getAudioq() {
		return audioq;
	}


	public void setAudioq(AVPacketList audioq) {
		this.audioq = audioq;
	}


	public AVStream getVideo_st() {
		return video_stream;
	}


	public void setVideo_st(AVStream video_st) {
		this.video_stream = video_st;
	}


	public AVPacketList getVideoq() {
		return videoq;
	}


	public void setVideoq(AVPacketList videoq) {
		this.videoq = videoq;
	}

	
	// schedule a video refresh in 'delay' ms
	public void schedule_refresh(int delay) {
		this.video_delay = delay;
		
	}


	public void init() {
		video_index = -1;
		audio_index = -1;
		
		// Open video file
		formatCtx = AVFormat.av_open_input_file(uri, null, null);
		
		for (int i = 0 ; i < formatCtx.nb_streams() ; i++) {
			
			// Find the first video stream
			if ( (formatCtx.get_stream(i).get_codec().get_codec_type() == AVMediaType.AVMEDIA_TYPE_VIDEO)
					&& (video_index == -1) ){
				video_index = i;
			}
			
			// Find the first audio stream
			if ( (formatCtx.get_stream(i).get_codec().get_codec_type() == AVMediaType.AVMEDIA_TYPE_AUDIO)
					&& (audio_index == -1) ) {
				audio_index = i;
			}
		}
		
		if (video_index >= 0) {
			stream_component_open(video_index);
		}
		
		if (audio_index >= 0) {
			stream_component_open(audio_index);
		}
		
		if ( (video_stream == null) || (audio_stream == null) ) {
			System.out.println(uri.toString() + ": could not open codecs");
			return;
		}
		
		for (int i = 0 ; i < aCodecCtx.get_channels() ; i++) {
			channelsBuffers.add(new ArrayList<Short>());
			maxAudio.add((short)0);
			minAudio.add((short)0);
		}
		
		set_current_frame(0);
		nb_frames = (int)formatCtx.get_duration();	

		this.width = vCodecCtx.get_width();
		this.height = vCodecCtx.get_height();
		
		initialized = true;
		
	}
	
	public AVRational get_time_base() {
		return video_stream.get_time_base();
	}
	
	public float frame_to_second(long frameNum) {
		return frameNum * video_stream.get_time_base().get_den() / video_stream.get_time_base().get_num();
	}

	private void stream_component_open(int stream_index) {
		AVCodecContext codecCtx;
		AVCodec codec;
		
		// Bad index
		if ( (stream_index < 0 )|| (stream_index >= formatCtx.nb_streams()) ) {
			return;
		}
		
		// Get a pointer to the codec context for the video stream
		codecCtx = formatCtx.get_stream(stream_index).get_codec();
		
		if (codecCtx.get_codec_type() == AVMediaType.AVMEDIA_TYPE_AUDIO) {
			// Set audio settings from codec info
			sample_rate = codecCtx.get_sample_rate();
			channels = codecCtx.get_channels();
		}

		codec = AVCodec.find_decoder(codecCtx.get_codec_id());
		
		codecCtx.open2(codec);
		
		if (codec== null) {
			System.out.println("Unsupported codec!");
			return;
		}
		
		switch (codecCtx.get_codec_type()) {
		case AVMEDIA_TYPE_AUDIO:
			audio_index = stream_index;
			audio_stream = formatCtx.get_stream(stream_index);
			aCodecCtx = codecCtx;
			aCodec = codec;
			break;
		case AVMEDIA_TYPE_VIDEO:
			video_index = stream_index;
			video_stream = formatCtx.get_stream(stream_index);
			vCodecCtx = codecCtx;
			vCodec = codec;
			//formatCtx.set_bit_rate(vCodecCtx.get_bit_rate());
			break;
		default:
			break;
		}
		
		
	}


	public boolean full_video_queue() {
		return videoq.size() >= MAX_VIDEOQ_SIZE;
	}


	public boolean full_audio_queue() {
		return audioq.size() >= MAX_AUDIOQ_SIZE;
	}
	
	public int get_sample_rate() {
		return aCodecCtx.get_sample_rate();
	}
	
    // Maybe need some mutex or synchronized method to manage multiple threads.
    private void packet_queue_put(AVPacketList q, AVPacket pkt) {
    	q.add(pkt);
    	
    	if ( (nb_frames == -1) && (q.size() >= 2) ) {
        	long pos1 = q.get(0).get_pos();
    		long pos2 = q.get(1).get_pos();
    		long editUnitByteCount = pos2 - pos1;
    		long size_data = formatCtx.get_pb().get_reader().size() - formatCtx.get_pos_first_frame();
    		nb_frames = (int) (size_data / editUnitByteCount);
			formatCtx.set_duration((int)nb_frames);
    		
    	}
    	
    	
    }
    
    
    // Maybe need some mutex or synchronized method to manage multiple threads.
    private AVPacket packet_queue_get(AVPacketList q) {
    	boolean hasPkt = (q.size() > 0);
    	
    	while (!hasPkt) {
    		try {
    			Thread.sleep(10);
    		} catch(InterruptedException e) {}
    		hasPkt = (q.size() > 0);
    	}
    	
    	AVPacket pkt = q.get(0);
    	
    	q.pop();
    	
    	
    	return pkt;
    }
    
    public void skip_first_frame() {
    	packet_queue_get(videoq);
    }
    
    public void skip_first_sample() {
    	packet_queue_get(audioq);
    }
    
    
	public void decode() {
		while (true) {
			// if quit break ?
			
			if ( full_audio_queue() || full_video_queue() || 
			     ( (get_current_frame() >= nb_frames) && (nb_frames != -1) ) ) {
				try {
					Thread.sleep(100); 
				} catch (Exception e) {}  		
				continue;
			} 

			AVPacket pkt = formatCtx.av_read_frame();
			
			if (pkt == null) {
				continue;//break;
			}
			
			if ( pkt.get_stream_index() == video_index ) {
				packet_queue_put(videoq, pkt);				
			} else if (pkt.get_stream_index() == audio_index) {
			    packet_queue_put(audioq, pkt);
			}
			
			
		}
			
		
	}


	private BufferedImage video_decode_frame(AVPacket pkt) {

		int vid_w = vCodecCtx.get_width();
		int vid_h = vCodecCtx.get_height();				

		BufferedImage im = new BufferedImage(vid_w, vid_h, BufferedImage.TYPE_INT_RGB);
		
		try {		
			
			//AVFrame picture = vCodecCtx.avcodec_decode_video2(pkt);
			vCodecCtx.avcodec_decode_video2(pkt);
	
			int [] img = vCodec.getDisplayOutput().showScreen();
	
			int xx = 0;
			int yy = 0;
			for (int i = 0 ; i < img.length ; i++) {
				im.setRGB(xx, yy, img[i]);
	
				xx++;
	
				if (xx >= vid_w) {
					xx = 0;
					yy++;
				}
			}
		} catch (Exception e) {
			System.err.println("Decoding Problem");
		}
	
		return im;
		
	}
	

	private Long [] audio_decode_frame(AVPacket pkt) {
		Long [] samples = aCodecCtx.avcodec_decode_audio(pkt);
		return samples;
	}
	
	
	
	public int get_seek_frame_ref() {
		return seek_frame_ref;
	}


	public BufferedImage getFirstFrame() {
		set_current_frame(get_current_frame() + 1);
		AVPacket pkt = packet_queue_get(videoq);
		return video_decode_frame(pkt);
	}


	public int get_channels() {
		return channels;
	}


	public Long[] getFirstSample() {
		AVPacket pkt = packet_queue_get(audioq);
		Long [] samples = audio_decode_frame(pkt);
		audioBuffers.add(samples);
		return samples;
	}


	public void addSample(int channel, short sample) {
		channelsBuffers.get(channel).add(sample);		

        if (sample < minAudio.get(channel)) {
        	minAudio.set(channel, sample);
        } else if (sample > maxAudio.get(channel)) {
            maxAudio.set(channel, sample);
        }		
	}


	public ArrayList<Short> getChannel(int i) {
		if (channelsBuffers.size() > 0) 
			return channelsBuffers.get(0);
		else
			return null;
	}


	public short get_min_audio(int channel) {
		return minAudio.get(channel);
	}

	public short get_max_audio(int channel) {
		return maxAudio.get(channel);
	}


	public double get_master_clock() {
		// Maybe change if the audio is the master
		return get_video_clock();
	}


	private double get_video_clock() {
	//	  double delta = (av_gettime() - video_current_pts_time) / 1000000.0;
		 // return video_current_pts + delta;
		return 0;
	}


	public void seek_delta(int inc) {
		set_current_frame(get_current_frame() + inc);
		
		if (get_current_frame() < 0)
			set_current_frame(0);
		if (get_current_frame() > nb_frames)
			set_current_frame(nb_frames);
		
		formatCtx.av_seek_frame(video_index, get_current_frame(), 0);
		
		seek_frame_ref = get_current_frame();
		
	}


	public void flush_queues() {
		audioq.clear();
		videoq.clear();

		channelsBuffers.clear();
		maxAudio.clear();
		minAudio.clear();
		audioBuffers.clear();	
		
		for (int i = 0 ; i < aCodecCtx.get_channels() ; i++) {
			channelsBuffers.add(new ArrayList<Short>());
			maxAudio.add((short)0);
			minAudio.add((short)0);
		}
	}


	public long get_nb_frames() {
		return nb_frames;
	}

	public void seek_frame(int start_frame) {
		set_current_frame(start_frame);
		formatCtx.av_seek_frame(video_index, start_frame, 0);
		seek_frame_ref = start_frame;
	}


	public ArrayList<Long []> get_audioBuffers() {
		return audioBuffers;
	}


	public long get_bits_per_coded_sample() {
		return aCodecCtx.get_bits_per_coded_sample();
	}


	public ArrayList<String> get_metadata() {
		ArrayList<String> strings = new ArrayList<String>(); 
		
		StringBuilder sb = new StringBuilder();
		//"Duration: 00:01:15.08, start: 0.000000, bitrate: 50000 kb/s"
	//	mpeg2video (4:2:2), yuv422p, 720x608 [PAR 152:135 DAR 4:3], 50000 kb/s, 25 fps, 25 tbr, 25 tbn, 50 tbc
		//Audio: pcm_s16le, 48000 Hz, 4 channels, s16, 3072 kb/s
		sb.append("Duration: ");
		sb.append(formatCtx.get_duration());
		sb.append(" frames (");
		sb.append(frame_to_second(formatCtx.get_duration()));
		sb.append("s), bitrate: ");
		sb.append(formatCtx.get_bit_rate());
		sb.append(" kb/s");
		
		strings.add(sb.toString());
		
		
		for (AVStream st : formatCtx.get_streams()) {

			/*int g = Mathematics.av_gcd(st.get_time_base().get_num(), 
									   st.get_time_base().get_den());*/
			
			sb = new StringBuilder();
			sb.append("Stream # ");
			sb.append(st.get_index());
			sb.append(": ");
			sb.append(formatCtx.avcodec_string(st.get_codec(), false));

			if (st.get_codec().get_codec_type() == AVMediaType.AVMEDIA_TYPE_VIDEO) {
				sb.append(", ");
				sb.append(st.get_time_base().to_double());
				sb.append(" fps");
			}
			
			strings.add(sb.toString());
		}
		
	    return strings;
	}


	public String get_title() {
		StringBuilder sb = new StringBuilder();

		sb.append(uri);
	    
	    return sb.toString();
	}

	
	

}
