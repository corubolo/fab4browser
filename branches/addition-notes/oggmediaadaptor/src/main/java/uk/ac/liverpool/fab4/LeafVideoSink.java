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

package uk.ac.liverpool.fab4;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ImageProducer;

import com.fluendo.jst.Buffer;
import com.fluendo.jst.Caps;
import com.fluendo.jst.Pad;
import com.fluendo.jst.Sink;
import com.fluendo.utils.Debug;

public class LeafVideoSink extends Sink {
    private boolean keepAspect;
    private boolean ignoreAspect;
    private boolean scale;

    private int width, height;
    private int aspectX, aspectY;
    private Rectangle bounds;

    private LeafVideo component;

    public LeafVideoSink() {
        keepAspect = true;
        scale = true;
        bounds = null;
    }

    @Override
    protected boolean setCapsFunc(Caps caps) {
        String mime = caps.getMime();
        if (!mime.equals("video/raw")) {
            return false;
        }

        width = caps.getFieldInt("width", -1);
        height = caps.getFieldInt("height", -1);

        if (width == -1 || height == -1) {
            return false;
        }

        component.aspectx = aspectX = caps.getFieldInt("aspect_x", 1);
        component.aspecty = aspectY = caps.getFieldInt("aspect_y", 1);
        System.out.println(aspectX + "  " + aspectY);
        if (!ignoreAspect) {
            Debug.log(Debug.DEBUG, this + " dimension: " + width + "x" + height
                    + ", aspect: " + aspectX + "/" + aspectY);

            if (aspectY > aspectX) {
                height = height * aspectY / aspectX;
            } else {
                width = width * aspectX / aspectY;
            }
            Debug.log(Debug.DEBUG, this + " scaled source: " + width + "x"
                    + height);
        }

        return true;
    }

    @Override
    protected int preroll(Buffer buf) {
        System.out.println("pre");
        return render(buf);
    }

    @Override
    protected int render(Buffer buf) {
        if (buf.object instanceof ImageProducer) {
            ImageProducer ip = (ImageProducer) buf.object;
            // if (!ip.isConsumer(component)){
            ip.startProduction(component);
            // }
        } else if (buf.object instanceof Image) {
            System.out.println("SHOULD NOT HAPPEN?");
            component.createImage((ImageProducer) buf.object);
        } else {
            System.out.println(this + ": unknown buffer received " + buf);
            return Pad.ERROR;
        }

        return Pad.OK;
    };

    @Override
    public String getFactoryName() {
        return "leafvideosink";
    }

    @Override
    public boolean setProperty(String name, java.lang.Object value) {
        if (name.equals("component")) {
            component = (LeafVideo) value;
        } else {
            return super.setProperty(name, value);
        }

        return true;
    }

    @Override
    public java.lang.Object getProperty(String name) {
        if (name.equals("component")) {
            return component;
        } else {
            return super.getProperty(name);
        }
    }

    @Override
    protected int changeState(int transition) {
        if (currentState == STOP && pendingState == PAUSE && component == null) {
            // frame = new Frame();
            // component = (Component) frame;
        }
        return super.changeState(transition);
    }
}
