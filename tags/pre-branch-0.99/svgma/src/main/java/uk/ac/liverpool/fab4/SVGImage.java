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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.ref.SoftReference;
import java.net.URI;

import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.StyleSheet;
import multivalent.node.LeafImage;
import multivalent.std.MediaLoader;
import phelps.awt.Colors;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

public class SVGImage extends MediaAdaptor {

    private static SoftReference<SVGDiagram> last = null;
    private static URI lastUri = null;

    @Override
    public Object parse(INode parent) throws Exception {
        Document doc = parent.getDocument();
        if (doc.getFirstChild() != null) {
            doc.clear();
        }
        final StyleSheet ss = doc.getStyleSheet();
        CLGeneral gs = new CLGeneral();
        gs.setForeground(Colors.getColor(getAttr("foreground"), Color.BLACK));
        gs.setBackground(Colors.getColor(getAttr("background"), Color.WHITE));

        gs.setPadding(8);
        ss.put(doc.getName(), gs);
        SVGDiagram d = null;
        File f = MediaLoader.FileCache.get(doc.getURI());
        URI urr = null;
        if (f != null) {
            urr = f.toURI();
        } else {
            urr = getURI();
        }
        if (last != null && last.get() != null) {
            d = last.get();
            // System.out.println(lastUri);
            // System.out.println(getURI());
            if (lastUri == null || !lastUri.equals(getURI())) {
                d = null;
            }

        }
        if (d == null) {
            SVGUniverse u = SVGCache.getSVGUniverse();
            d = u.getDiagram(urr, true);
            d.setIgnoringClipHeuristic(true);
            // System.out.println("load " + getURI());
            last = new SoftReference<SVGDiagram>(d);
            lastUri = getURI();
        }
        int w = (int) (d.getWidth() * getZoom());
        int h = (int) (d.getHeight() * getZoom());
        BufferedImage original = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) original.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
        // RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        // System.out.println("aint");
        g.setColor(Color.white);
        g.setBackground(Color.white);
        g.fill(new Rectangle(0, 0, w, h));
        if (getZoom() != 1.0) {
            g.setTransform(AffineTransform.getScaleInstance(getZoom(),
                    getZoom()));
        }
        d.render(g);
        new LeafImage("image", null, parent, original);
        Layer ll = doc.getLayer(Layer.PERSONAL);
        if (ll != null) {
            ll.destroy();
        }
        return parent;
    }

}
