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

import java.awt.Point;
import java.net.URI;
import java.util.Map;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Document;
import multivalent.Layer;
import multivalent.SemanticEvent;
import multivalent.std.adaptor.pdf.Anno;
import multivalent.std.span.SelectionSpan;
import multivalent.std.ui.Multipage;
import uk.ac.liverpool.fab4.behaviors.ForwardBack4;


/**
 * A generic behaviour that take care of reporting Multivalent events in the Fab4
 * and updating the Fab4 UI
 * 
 * 
 * @author fabio
 *
 */
public class UiBehavior extends Behavior {
	public static final String MSG_TAB_CHANGED = "tabChanged";
	public static final String MSG_SCROLLTO = "scrollTo";
	public static final String MSG_CLOSETAB = "closeTab";

	Fab4 fr = null;
	Browser br = null;
	ForwardBack4 fb4 = null;
	static Runnable run;

	static void execureAfterSpanSelection(Runnable r) {
		UiBehavior.run = r;

	}

	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		br = getBrowser(); // where the event is
		if (fr == null)
			fr = Fab4.getMVFrame(br); // frame that holds the browser
		if (Anno.MSG_EXECUTE==msg) {
			//System.out.println("EUREKA");
			Layer layer = getBrowser().getCurDocument().getLayer(Anno.LAYER);
			System.out.println(layer.size());

		}
		if (msg == UiBehavior.MSG_SCROLLTO) {
			Document mvDocument = (Document) getBrowser().getRoot().findBFS(
			"content");
			Point p = new Point(mvDocument.getHsb().getValue(), mvDocument
					.getVsb().getValue());
			if (se.getArg() instanceof Point) {
				Point i = (Point) se.getArg();
				if (!i.equals(p)) {
					// System.out.println(i+"  "+p);
					mvDocument.getHsb().setValue(i.x);
					mvDocument.getVsb().setValue(i.y);
				}
			}

		} else if (msg == SelectionSpan.MSG_SET) {
			if (UiBehavior.run != null)
				UiBehavior.run.run();
			UiBehavior.run = null;
		}

		if (msg != null && fr != null) {
			if (fb4 == null)
				fb4 = (ForwardBack4) Fab4utils.getBe("fwbk", br.getRoot()
						.getLayers());
			if (fr.getCurBr() == br) {
				if (fb4.isPrev && !fr.topButtonBar.bk.isEnabled())
					fr.topButtonBar.bk.setEnabled(true);
				else if (!fb4.isPrev && fr.topButtonBar.bk.isEnabled())
					fr.topButtonBar.bk.setEnabled(false);

				if (fb4.isNext && !fr.topButtonBar.fw.isEnabled())
					fr.topButtonBar.fw.setEnabled(true);
				else if (!fb4.isNext && fr.topButtonBar.fw.isEnabled())
					fr.topButtonBar.fw.setEnabled(false);
			}
			// manage doc opened

			if (msg == Document.MSG_OPENED) {

				fr.updateDocOpened(br);
				fr.doneLoading(br);
			} else if (msg.equals(Browser.MSG_NEW)) {
				Map ma = (Map) se.getArg();
				URI u = (URI) ma.get("uri");
				fr.openNewTab(u.toString());
				return true;

			} else if (msg.equals(Fab4.MSG_ASK_PASSWORD))
				return true;
			else if (msg == UiBehavior.MSG_CLOSETAB)
				fr.closeTab((Browser) se.getArg());
			else if (msg.equals(Document.MSG_FORMATTED))
				// System.out.println("***"+msg);
				fr.updateFormatted(br);
			// fr.clessidraOff(br);
			else if (msg.equals(Document.MSG_OPEN))
				fr.startedLoading(br);
			else if (msg.equals(Browser.MSG_STATUS))
				fr.setStatus((String) se.getArg());
			else if (msg == UiBehavior.MSG_TAB_CHANGED || msg == Multipage.MSG_FIRSTPAGE
					|| msg == Multipage.MSG_GOPAGE || msg == Multipage.MSG_LASTPAGE
					|| msg == Multipage.MSG_NEXTPAGE || msg == Multipage.MSG_PREVPAGE) {
				fr.updatePagec();
				fr.updateAnnoIcon();
			}
		}
		return super.semanticEventAfter(se, msg);
	}

}
