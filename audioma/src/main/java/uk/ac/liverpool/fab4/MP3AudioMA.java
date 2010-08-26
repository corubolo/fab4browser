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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.StyleSheet;
import phelps.awt.Colors;

public class MP3AudioMA extends MediaAdaptor {
    Document doc;
    private AudioDocument l;

    @Override
    public void close() throws IOException {
        //		
        if (l != null) {
            l.close();
        }
        l = null;
        // //doc.removeAttr(TimedMedia.TIMEDMEDIA);
        super.close();
    }

    @Override
    public Object parse(INode parent) throws Exception {

        doc = parent.getDocument();
        System.out.println(doc);
        if (doc.getFirstChild() != null) {
            doc.clear();
        }
        final StyleSheet ss = doc.getStyleSheet();
        CLGeneral gs = new CLGeneral();
        gs.setForeground(Colors.getColor(getAttr("foreground"), Color.WHITE));
        gs.setBackground(Colors.getColor(getAttr("background"), Color.BLACK));
        gs.setPadding(8);
        ss.put(doc.getName(), gs);
        Map<String, Object> attr = new HashMap<String, Object>(1);
        attr.put("RESIZE", false);
        attr.put("GLOBAL", true);
        if (getURI() == null) {
            // new LeafUnicode("File not found",attr,parent);
            throw new IOException("File not found");
        }
        // new LeafUnicode("File not found",attr,parent);
        l = new AudioDocument("audio", attr, doc, getURI());
        doc.uri = getURI();
        Layer ll = doc.getLayer(Layer.PERSONAL);
        if (ll != null) {
            ll.destroy();
            // doc.putAttr(TimedMedia.TIMEDMEDIA, l);
        }

        // Map<String, Object> hm = new HashMap<String, Object>(3);
        // hm.put("signal", "viewOcrAs");
        // hm.put("value", "ocr");
        // hm.put("title", "Show OCR");
        // Behavior.getInstance("TimedMC",
        // "uk.ac.liverpool.fab4.behaviors.TimedMediaControl", null, hm,
        // getCurBr().getRoot().getLayer(Layer.SYSTEM));

        return parent;
    }

}
