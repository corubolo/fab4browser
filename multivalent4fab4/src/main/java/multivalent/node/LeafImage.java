/*
 * 
 * Copyright (C) 2006 Tom Phelps / Practical Thought  
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of Liverpool
 * This program is free software; you can redistribute it and/or modify it under the terms 
 * of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, 
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package multivalent.node;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.io.File;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import multivalent.Context;
import multivalent.INode;
import multivalent.Leaf;
import multivalent.Node;
import multivalent.TreeEvent;
import phelps.net.URIs;


/**
 Medium-specific leaf node.

 LATER: Expose information that's information hidden by Java's getImage(), such as animated GIF frames delays.

 @version $Revision$ $Date$
 */
public class LeafImage extends Leaf implements ImageObserver {
    protected static Image Broken_; //=null;    //Toolkit.getDefaultToolkit().getImage(new CHashMap().getClass().getResource("/sys/images/broken.gif"));
    static {
        try {
            Broken_ = ImageIO.read(new multivalent.CHashMap().getClass().getResource(
                "/sys/images/broken.gif"));
        } catch (java.io.IOException ioe) {}
    }

    protected static Map<URI, SoftReference<Image>> cache_ = new HashMap<URI, SoftReference<Image>>(
        20);
    
    Image image_ = Broken_;
    boolean loaded_ = true; // assume in cache or local;  unset in first imageUpdate call; set to true because may not get any imageUpdate call
    boolean stop_ = false; // animated GIFs
    int width_ = -1, height_ = -1; // => AffineTransform?
    boolean error = false;
    private Rectangle2D lb;

    boolean acq = false;
    /** Take Image from passed parameter. */
    public LeafImage(String name, Map<String, Object> attr, INode parent, Image img) {
        super(name, attr, parent);
        image_ = (img != null ? img : Broken_);
        //loaded_ = true;
    }

    public boolean isBroken() {
        return image_ == Broken_;
    }

    /** Take Image from passed URI. */
    public LeafImage(String name, Map<String, Object> attr, INode parent, URI uri) {
        super(name, attr, parent);
        image_ = Broken_;
        setImage(uri);
    }

    public Dimension getDimension() {
    	return new Dimension(width_,height_);
    }
    public Image getImage() {
        return image_;
    }

    public void setImage(Image img) {
        stop_ = false; // assumed loaded or in some cache somewhere

        if (img != null) {
            image_ = img;
            loaded_ = false; // can't be certain
        } else {
            image_ = Broken_;
            loaded_ = true;
        }
        //    reformat(this);
    }

    public void setImage(final URI uri) {
        //System.out.println("image uri = "+uri);
        loaded_ = true;
        stop_ = false; // assumed loaded or in some cache somewhere

        if (uri == null) {
            image_ = Broken_;
            error = true;
        } else try {
            // LATER: drag through cache
            // for now rely on Java's built-in images, but should drag through cache    XXX load up raw data and decode yourself
            // but for now want to retain ImageObserver, so catch systemresource protocol specially.
            //if ("systemresource".equals(url.getProtocol())) image_=Toolkit.getDefaultToolkit().getImage(getClass().getResource(url.getFile()));
            ImageIO.setUseCache(false); // don't write to disk
            //System.out.println("load image "+uri);
            if ("systemresource".equals(uri.getScheme())) { // small, icon images from JARs cached
                SoftReference<Image> ref = cache_.get(uri);
                if (ref != null && ref.get() != null) {
                    image_ = ref.get();
                } else {
                    image_ = ImageIO.read(getClass().getResource(uri.getPath()));
                    cache_.put(uri, new SoftReference<Image>(image_));
                }

            } else if ("file".equals(uri.getScheme())) { // local images created immediately
                image_ = ImageIO.read(new File(uri.getPath()));
                /** SRB!!! */
            } else { // images on network loaded asynchronously (LATER cached to disk)
                // in cache?

                //else /*ImageIO.setUseCache(true);?*/ image_ = ImageIO.read(URIs.toURL(uri));  // waits until fully loaded
                //System.out.println("cache = "+ImageIO.getCacheDirectory()+", use?="+ImageIO.getUseCache());   //=> null unless setCacheDirectory()
                //else
                image_ = Toolkit.getDefaultToolkit().getImage(URIs.toURL(uri));
                //image_ = getBrowser().getImage(URIs.toURL(uri));  //-- also slow
                loaded_ = false; // exception
            }

        } catch (Exception e) {
            image_ = Broken_;
            error = true;
            System.out.println("can't load image " + uri + " => " + e);
        }
        //    loaded_ = ((/*Component*/.checkImage(image, this) & ImageObserver.ALLBITS)!=0);
        //reformat(this);
    }

    /** Scale image as necessary to <code>width</code> x <code>height</code>, or to natural dimension of parameter is <code>-1</code>. */
    public void setSize(int width, int height) {
        width_ = width;
        height_ = height;
    }

    //public void subelementCalc(Context cx) {}
    //public int subelementHit(Point rel) { return (rel.x<image_.getWidth(this)/2?0:1); }
    @Override
	public int subelementHit(Point rel) {
        return (rel.x < bbox.width/*image_.getWidth(this)*// 2 ? 0 : 1);
    }

    @Override
	public boolean formatNodeContent(Context cx, int start, int end) {
        /*  if (width_!=-1 && height_!=-1 && start==0) {
         bbox.setSize(width_, height_);
         } else*/if (image_ != null && start == 0) {
            int w = image_.getWidth(this), h = image_.getHeight(this);
            //if (w==-1) w = (width_!=-1? width_: 10); if (h==-1) h = (heigth_!=-1? height_: 10);
            //System.err.println("FNC: w="+w+", h="+h);
            //bbox.setSize(w,h); baseline=h;

            if (w != -1 && h != -1 /*&& width>0*/) {
                bbox.setSize(w, h);
                baseline = h;
            } else {
                valid_ = false; // undo Leaf.formatNode's assumption
                bbox.setSize(10, 10);
                baseline = 10;
            }

            if (width_ != -1) bbox.width = width_;
            if (height_ != -1) bbox.height = baseline = height_;
            //System.out.println(bbox.width+"x"+bbox.height+" vs "+width_+"x"+height_);

            //      int neww = (w!=-1? w: 10), newh = (h!=-1? h: 10);
            //System.out.print("neww="+neww+", width="+width+", newh="+newh+", height="+height);
            //System.out.println("neww="+neww+", w="+w+", newh="+newh+", h="+h);
            //if (neww > width) neww=Math.max(1, width); //if (newh > height) newh=Math.max(1, height); => only HTML
            //System.out.println(" => "+neww+"x"+newh);
            //      bbox.setSize(neww, newh); baseline = newh;
            //System.out.println("IMG "+getAttr("src")+", w="+bbox.width+", baseline="+baseline+" vs "+h+"/"+bbox.height);
        } else bbox.setSize(0, 0);

        return !valid_;
    }

    /** For images, just mark dirty and batch to next repaint.
     This way, more likely to batch HTML images when load.  Images don't change size, so efficiency not supremely important.
     */
    @Override
	public void reformat(Node dirty) {
        int w = image_.getWidth(this), h = image_.getHeight(this);
        //System.out.println("IMG reformat "+getAttr("src")+", valid_="+valid_+", w="+w+", h="+h);
        if (w != -1 && h != -1 && (w != bbox.width || h != bbox.height)) {
            markDirty();
            getBrowser().repaint(500);
        }
    }

    /**
     Contract violation: painting an image does not paint the background color in place of any transparent pixels,
     because that might clear out essential existing background.
     */
    @Override
	public boolean paintNodeContent(Context cx, int start, int end) {
        if (start == 0) {
            //if (image_==null /*|| Broken_==null*/ || bbox==null || cx==null || cx.g==null) {
            //System.out.println("image_="+image_/*+", Broken="+Broken_*/+", bbox="+bbox+", cx="+cx/*+", g="+cx.g*/+", bkgnd="+cx.background); //System.exit(1); }

            Graphics2D g = cx.g;
            int w = image_.getWidth(this), h = image_.getHeight(this);
            int x = 0, y = Math.max(baseline - h, 0);
            if (!loaded_ && !error && w == -1 && h == -1) {
                Color c = g.getColor();
                g.setColor(new Color(235, 235, 235));
                g.fillRect(1, 1, width_ - 1, height_ - 1);
                //g.drawImage(image_, x, y,this);
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(1, 1, width_ - 1, height_ - 1);
                g.setColor(Color.DARK_GRAY);
                Font f = g.getFont();
                Font f2 = f.deriveFont(9.0f);
                g.setFont(f2);
                if (lb == null) {
                    lb = f2.getStringBounds("loading", g.getFontRenderContext());
                }
                if (lb.getBounds().width + 3 < width_
                    && lb.getBounds().height + (-lb.getY() + 3f) < height_)
                    g.drawString("loading", 3f, (float) (-lb.getY() + 3f));
                g.setColor(c);
                g.setFont(f);
            } else if (w != -1 && h != -1)
                try {
                    
                    //g.drawImage(image_, 0,0+Math.max(baseline-bbox.height,0), bbox.width,bbox.height,/*maybe enlarged by annos*/ cx.background, this);
                    //System.out.println("draw @0,"+y+", "+w+"x"+h+" => "+width_+"x"+height_);
                    //System.out.println("g.drawImage(x+","+y+","+(x+(width_!=-1? width_: w))+","+(y+(height_!=-1? height_: h))+"  0,0,"+(x+w)+","+(y+h)+"  "+cx.background);
                    g.drawImage(image_, x, y, x + (width_ != -1 ? width_ : w), y
                        + (height_ != -1 ? height_ : h), 0, 0,/*x,y,*/x + w, y + h, /*cx.background,--transparent pixel in image*/
                    this);
                    //g.drawImage(image_,  AffineTransform, /*no background!*/, this);
                    //g.setColor(java.awt.Color.RED); g.drawRect(x,y, w, h);

                } catch (Exception shouldnthappen) { // Java 1.4 throws NullPointerException deep in sun.java2d on some GIFs, such as http://graphics4.nytimes.com/images/misc/spacer.gif.  RESTORE
                    //System.out.println("exploding GIF: "+getAttr("src"));
                    //shouldnthappen.printStackTrace(System.out);
                    //System.out.println(getAttr("src")+", image_="+image_+", Broken="+Broken_+", bbox="+bbox+", baseline="+baseline+", cx="+cx+", bkgnd="+cx.background);
                    //System.out.println(image_.getWidth(this)+"x"+image_.getHeight(this)+", drawn to "+bbox.width+"x"+bbox.height);
                    //System.exit(1);
                }
            /*g.drawImage(image_,
             0,0+Math.max(baseline-bbox.height,0), bbox.width,bbox.height,
             bbox.width,bbox.height, 0,0+Math.max(baseline-bbox.height,0),
             Color.GREEN, this);*/

            //System.err.println("im "+getName()+", h="+bbox.height);
            cx.x += bbox.width;
            //cx.x += bbox.width; g.drawImage(image_, 0,0, cx.background, this);
            //System.out.println("IMG yoff="+baseline+"-"+bbox.height+"="+(baseline-bbox.height));
            //g.setColor(Color.ORANGE); g.drawRect(0,0, bbox.width,bbox.height);
            //String title=getAttr("src"); if (title!=null) g.drawString(title, 5,12);
        }
        //if (!loaded_ && image_.getWidth(this)!=-1) { loaded_=true; reformat(this); }  // if images cached, don't get imageUpdate call, grr
        return false;
    }

    @Override
	public boolean eventNode(AWTEvent e, Point rel) {
        if (e.getID() == TreeEvent.STOP) {
            //System.out.println("stopping "+getAttr("src")+"/"+loaded_);
            //loaded_ true; // seems to screw up Java's image loading
            stop_ = true;
        }
        return super.eventNode(e, rel);
    }

    //
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width,
        int height) {
        //System.out.println("loaded_="+loaded_+" "+getAttr("src"));
        if (!loaded_) { // called after loaded with FRAMEBITS (animated GIFs)
            loaded_ = (infoflags & ImageObserver.ALLBITS) != 0;
            //if ((infoflags&ImageOberser.ERROR)!=0) { /*point to broken image*/ loaded_=true; }
            /*      if ((infoflags&ImageObserver.WIDTH)!=0 && (infoflags&ImageObserver.HEIGHT)!=0) {
             //reformat(this);
             System.err.println("getting w, h");
             System.err.println("w="+image_.getWidth(this)+", h="+image_.getHeight(this));
             markDirty();
             repaint(100);
             }*/
            //      if (image_.getWidth(this)!=bbox.width || image_.getHeight(this)!=bbox.height) {
            //          bbox.setSize(image_.getWidth(this), image_.getHeight(this));    // shouldn't be necessary
            if (loaded_) {// || (isValid() && (infoflags&ImageObserver.WIDTH)!=0 && (infoflags&ImageObserver.HEIGHT)!=0)) {
                reformat(this);
                //System.out.println(getName()+"  DONE LOADING");
            }
            //if (loaded_) works -- reformat(this);
            //      }
        }
        //if ((infoflags & ImageObserver.PROPERTIES)!=0 && image_.getProperty("COMMENT",this)!=image_.UndefinedProperty) System.out.println("image comment = "+image_.getProperty("comment", this));

        // frames of animated GIFs
        if ((infoflags & ImageObserver.FRAMEBITS) != 0) {
            if (!stop_) repaint(100); // FRAMEBITS delivered according to GIF animation rate
            //repaint(1000*2);  //-- no animation for now
            //        return false;

        } else if ((infoflags & ImageObserver.ALLBITS) != 0 /*|| (infoflags & ImageObserver.SOMEBITS)!=0*/) { // SOMEBITS doesn't show partial image and just slows things down
            repaint(250);
            //System.out.println(getName()+" "+infoflags);

        } else if ((infoflags & ImageObserver.ERROR) != 0) {
            //if (!loaded_) System.out.println("ImageObserver.ERROR flag: "+getAttr("src"));    // happens a lot, for some reason
            //image_ = Broken_;
            loaded_ = true;

        } // else -- get lots of infoflags==0!

        return !loaded_;
    }

    public void clipboardNode(StringBuilder sb) { /*nothing--HTML can put in ALT if it wants*/}
}
