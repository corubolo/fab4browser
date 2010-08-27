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
package uk.ac.liverpool.fab4.behaviors;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Document;
import multivalent.SemanticEvent;
import uk.ac.liverpool.fab4.Fab4;
import uk.ac.liverpool.fab4.MediaUI;

public class TimedMediaControl extends Behavior {


	public TimedMediaControl() {
	}

	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		Browser br = getBrowser();
		Document doc = br.getCurDocument();
		Object o = doc.getFirstChild();

		// System.out.println(o);
		if (o instanceof TimedMedia) {
			TimedMedia leaf = (TimedMedia) o;
			if (msg == TimedMedia.MSG_PLAY)
				leaf.setStatus(TimedMedia.Status.PLAY);
			else if (msg == TimedMedia.MSG_PAUSE)
				leaf.setStatus(TimedMedia.Status.PAUSE);
			else if (msg == TimedMedia.MSG_SEEK) {
				if (se.getArg() instanceof Double) {
					Double pos = (Double) se.getArg();
					leaf.setPosition(pos);
				}
			} else if (msg == TimedMedia.MSG_STOP)
				leaf.setStatus(TimedMedia.Status.STOP);
			else if (msg == TimedMedia.MSG_PlAYTIME) {
				int time = ((Double) se.getArg()).intValue();
				int sec = time % 60;
				int min = time / 60;
				int hour = min / 60;
				min %= 60;
				String s = "" + hour + ":" + (min < 10 ? "0" + min : "" + min)
				+ ":" + (sec < 10 ? "0" + sec : "" + sec);
				String so = Fab4.getMVFrame(getBrowser()).pmedia.time.getText();
				if (!so.equals(s))
					Fab4.getMVFrame(getBrowser()).pmedia.time.setText(s);
			}

			else if (msg == TimedMedia.MSG_PlAYTIMEPERCENT) {
				double pc  = (Double) se.getArg();
				Fab4 ff = Fab4.getMVFrame(getBrowser());
				//System.out.println(ff.pmedia.sl.getValueIsAdjusting());
				//System.out.println(SwingUtilities.isEventDispatchThread());
				if (!ff.pmedia.sl.getValueIsAdjusting()){
					//ff.pmedia.sl.setValueIsAdjusting(true);
					ff.pmedia.ticking = true;
					ff.pmedia.sl.setValue((int)(pc * 100.d));
					ff.pmedia.ticking = false;
					//ff.pmedia.sl.setValueIsAdjusting(false);
				}
			} else if (msg == TimedMedia.MSG_STATUS_CHANGED) {
				TimedMedia.Status s = (TimedMedia.Status) se.getArg();
				updateUI(s);
			} else if (msg == TimedMedia.MSG_RES) {
				// Fab4.getMVFrame(getBrowser()).setStatus(se.getArg().toString());
			} else if (msg == TimedMedia.MSG_GOT_DURATION) {
				//				System.out.println("Duration: " + se.getArg().toString());
				//				Fab4 ff = Fab4.getMVFrame(getBrowser());
				//				//System.out.println(ff.pmedia.sl.getValueIsAdjusting());
				//				//System.out.println(SwingUtilities.isEventDispatchThread());
				//				int time = ((Double) se.getArg()).intValue();
				//				int sec = time % 60;
				//				int min = time / 60;
				//				int hour = min / 60;
				//				min %= 60;
				//				String s = "" + hour + ":" + (min < 10 ? "0" + min : "" + min)
				//						+ ":" + (sec < 10 ? "0" + sec : "" + sec);
				//				Hashtable<Integer, JComponent> h = new Hashtable<Integer, JComponent>();
				//				h.put(100, new JLabel(s));
				//				ff.pmedia.sl.setLabelTable(h);
			}

		}

		return super.semanticEventAfter(se, msg);
	}

	private void updateUI(TimedMedia.Status s) {
		MediaUI mu = Fab4.getMVFrame(getBrowser()).pmedia;
		switch (s) {
		case PLAY:
			mu.pause.setEnabled(true);
			mu.stop.setEnabled(true);
			mu.play.setEnabled(false);
			mu.sl.setEnabled(true);
			break;
		case PAUSE:
			mu.pause.setEnabled(false);
			mu.stop.setEnabled(true);
			mu.play.setEnabled(true);
			mu.sl.setEnabled(true);
			break;
		case STOP:
			mu.pause.setEnabled(false);
			mu.stop.setEnabled(false);
			mu.play.setEnabled(true);
			mu.sl.setEnabled(false);
			break;
		case ERROR:
			mu.pause.setEnabled(false);
			mu.stop.setEnabled(false);
			mu.play.setEnabled(false);
			mu.sl.setEnabled(false);
			break;
		default:
			break;
		}
	}
}
