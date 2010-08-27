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
package uk.ac.liv.freettsMA;

import java.awt.Color;
import java.util.Map;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.CursorMark;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.node.LeafText;
import uk.ac.liverpool.fab4.Fab4;

public class FreeTTSBehaviour extends Behavior {

    static Color BACKGROUND = new Color(0xe0, 0xe0, 0xe0);

    static int ix_ = 100, iy_ = 100, iwidth_ = 300, iheight_ = 100;

    Node point_ = null;

    private ReadAloudDialog dial;

    @Override
    public boolean semanticEventAfter(SemanticEvent se, String msg) {
        if (Document.MSG_CLOSE == msg) {
            dial.dispose();
        } else if (CursorMark.MSG_SET == msg) {
            setStart();
        }
        return super.semanticEventAfter(se, msg);
    }

    @Override
    public void destroy() {
        dial.dispose();
    }

    /** Set current point from cursor/selection/first word in document. */
    void setStart() {
        Browser br = getBrowser();
        CursorMark curs = br.getCursorMark();
        Span span = br.getSelectionSpan();

        if (curs.isSet()) {
            point_ = curs.getMark().leaf;
            curs.move(null, -1);
        } else if (br.getSelectionSpan().isSet()) {
            point_ = span.getStart().leaf;
            span.moveq(null); // remove();
        } else {
            point_ = br.getCurDocument().getFirstLeaf();
        }
    }

    public static String getText(Document doc, Node p) {
        StringBuilder ret = new StringBuilder();
        walk(doc, ret);
        return ret.toString();
    }

    public static void walk(Node node, StringBuilder sb) {
        if (node == null) {
            return;
        }
        String name = node.getName();
        if (name == null) {
        } else if (node instanceof INode) {
            INode inode = (INode) node;
            for (int i = 0, imax = inode.size(); i < imax; i++) {
                walk(inode.childAt(i), sb);
            }

        } else if (node instanceof LeafText) {
            String s = node.getName();
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (Character.isLetterOrDigit(ch)) {
                    sb.append(ch);
                }
            }
            sb.append(" ");
        }
    }

    /*
     * starts reading (non-Javadoc)
     * 
     * @see multivalent.Behavior#restore(multivalent.ESISNode, java.util.Map,
     * multivalent.Layer)
     */
    @Override
    public void restore(ESISNode n, Map<String, Object> attr, Layer layer) {
        super.restore(n, attr, layer);
        Browser br = getBrowser();
        System.setProperty("freetts.voices",
                "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        // Span sel = br.getSelectionSpan();
        // Document doc = br.getCurDocument();
        // setStart();

        String text;
        text = br.clipboard();
        System.out.println(text);
        if (text == null || text.length() == 0) {
            Document doc = br.getCurDocument();
            setStart();

            text = getText(doc, point_);
        }
        // else

        dial = new ReadAloudDialog(Fab4.getMVFrame(getBrowser()), text);

    }
}
