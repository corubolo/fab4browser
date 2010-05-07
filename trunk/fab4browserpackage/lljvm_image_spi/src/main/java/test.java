import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;

import lljvm.lib.c;
import lljvm.runtime.Memory;


public class test {
    
    
    public static void main(String[] args) {
        if (true) {
            System.out.println(new File(args[0]).exists());
            String[] aargh = new String[]{"djpeg",args[0]};
            djpeg.main(aargh.length, Memory.storeStack(aargh));
            
            return;
        }
        
        ImageIO.scanForPlugins();
        IIORegistry iio = IIORegistry.getDefaultInstance();
        Iterator<ImageReader> i = ImageIO.getImageReadersBySuffix("jpg");
        while (i.hasNext()){
            ImageReader ir = i.next();
            if ((!ir.getOriginatingProvider().getVendorName().contains("fab4"))){
                System.out.println("Unregister " + ir);
                iio.deregisterServiceProvider(ir.getOriginatingProvider());
            }
        }
        i = ImageIO.getImageReadersBySuffix("jp2");
        while (i.hasNext()){
            ImageReader ir = i.next();
            if ((!ir.getOriginatingProvider().getVendorName().contains("fab4"))){
                System.out.println("Unregister " + ir);
                iio.deregisterServiceProvider(ir.getOriginatingProvider());
            }
        }

        try {
            final BufferedImage img = ImageIO.read(new File(args[0]));
            JFrame f = new JFrame();
            Icon im = new Icon() {

                public void paintIcon(Component c, Graphics g, int x, int y) {
                        g.drawImage(img, x, y, null);
                }

                public int getIconWidth() {
                        // TODO Auto-generated method stub
                        return img.getWidth();
                }

                public int getIconHeight() {
                        // TODO Auto-generated method stub
                        return img.getHeight();
                }
        };
        

            JButton b = new JButton(im);
            
            f.getContentPane().add(b);
            f.pack();
            f.setVisible(true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
 
}
