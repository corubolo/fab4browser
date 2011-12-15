package uk.ac.liverpool.media;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;

import org.jcodec.samples.player.pipeline.Frame;
import org.jcodec.samples.player.pipeline.JAVCSource;
import org.jcodec.samples.player.pipeline.Pipeline;
import org.jcodec.samples.player.pipeline.Sink;
import org.jcodec.samples.player.utils.FrameUtils;

public class DecodeToRaw implements Sink {

    private  boolean isfile;
    private  long durationBytes;
    private  Pipeline pipeline;
    int[] buffer;
    Dimension s = new Dimension();
    private BufferedImage lastFrame;
    int n=0;
    /**
     * @param args
     * @throws URISyntaxException 
     * @throws MalformedURLException 
     */
    public static void main(String[] args) throws URISyntaxException, MalformedURLException {
        
        URI uri = new URI(args[0]);
        String scheme = uri.getScheme();
        if (scheme == null) {
            new DecodeToRaw().decodeToRaw(new File(args[0]).toURI());
        } else 
            new DecodeToRaw().decodeToRaw(uri);

    }
    
    public void decodeToRaw(URI uri) throws MalformedURLException {
        String scheme = uri.getScheme();
        JAVCSource javcSource;
       
        if (scheme.contains("file")) {
            isfile  = true;
            File file = new File(uri);
            durationBytes = file.length();
            javcSource = new JAVCSource(file); 
        } else 
        {
            javcSource = new JAVCSource(uri.toURL()); 
        }

        if (pipeline != null)
            pipeline.stopPipeline();

        pipeline = new Pipeline(javcSource, this, 25);

        pipeline.start();
        pipeline.play();
    }

    public void display(Frame f) {
        int bufferSize = f.getWidth() * f.getHeight();

      
        if (buffer == null || bufferSize != buffer.length) {
            buffer = new int[bufferSize];
             lastFrame = new BufferedImage (f.getWidth(), f
                    .getHeight(), BufferedImage.TYPE_INT_RGB);
            s.width = f.getWidth();

            s.height = f.getHeight();
            
             }

        FrameUtils.YUV2RGB(f, buffer);

       
        lastFrame.setRGB(0,0,f.getWidth(), f
                .getHeight(), buffer, 0, f.getWidth());
        n++;
        try {
            ImageIO.write(lastFrame,"PNG", new File("fr"+n+".png"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void forcePaint() {
        // TODO Auto-generated method stub
        
    }

    public void start() {
        // TODO Auto-generated method stub
        
    }

    public void stop() {
        // TODO Auto-generated method stub
        
    }

}
