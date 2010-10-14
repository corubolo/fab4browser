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

import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.liverpool.fab4.behaviors.TimedMedia;

import java.awt.Color;
import java.awt.Insets;


/**
 * 
 * A simple toolbar used to give the users controls (play-pause-stop) over media files.
 * 
 * @author fabio
 *
 */

public class MediaUI extends JToolBar implements ChangeListener{
	private static final long serialVersionUID = 1L;

	Fab4 f;

	public JButton play;
	public JButton stop;
	public JButton pause;
	public JTextField time;
	public JSlider sl;
	public boolean ticking = false;
	public JTextField totalTime;

	public MediaUI(Fab4 fab4) {
		super();
		f = fab4;
		setFloatable(false);
		setRollover(true);
		play = f.getButton("mcplay");
		pause = f.getButton("mcpause");
		stop = f.getButton("mcstop");
		sl = new JSlider(SwingConstants.HORIZONTAL,0,100,0);
		sl.setPaintLabels(true);
		sl.addChangeListener(this);
		time = new JTextField("00:00:00");
		time.setEditable(false);
		time.setFont(time.getFont().deriveFont(9.0f));
		time.setMargin(new java.awt.Insets(0, 3,0, 3));
		//		sl.setPaintLabels(true);

		add(play);
		add(pause);
		add(stop);
		add(time);
		add (sl);
		
		totalTime = new JTextField("00:00:00");
		totalTime.setMargin(new Insets(0, 3, 0, 3));
		totalTime.setFont(totalTime.getFont().deriveFont(9f));
		totalTime.setEditable(false);
		add(totalTime);

		f.setActionS(play, TimedMedia.MSG_PLAY);
		f.setActionS(pause, TimedMedia.MSG_PAUSE);
		f.setActionS(stop, TimedMedia.MSG_STOP);
		doLayout();
		setBackground(Color.yellow);
		play.setBackground(Color.yellow);
		pause.setBackground(Color.yellow);
		stop.setBackground(Color.yellow);
		setVisible(false);

	}

	public void stateChanged(ChangeEvent e) {
		JSlider source = (JSlider)e.getSource();
		//System.out.println(source.getValueIsAdjusting());
		if (!ticking && !source.getValueIsAdjusting()) {
			//System.out.println("MOVEDDD");
			int percent = source.getValue();
			f.getCurBr().eventq(TimedMedia.MSG_SEEK, new Double (percent / 100.0d));
		}


	}

}
