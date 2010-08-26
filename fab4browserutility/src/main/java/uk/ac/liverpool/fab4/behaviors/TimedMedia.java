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

/**
 * @author fabio
 *
 */
public interface TimedMedia {

	static final String TIMEDMEDIA = "TimedMedia";

	public static final String MSG_PLAY = "PLAY";
	public static final String MSG_PAUSE = "pause";
	public static final String MSG_STOP = "stop";
	public static final String MSG_SEEK = "seek";
	public static final String MSG_STATUS_CHANGED = "stch";
	public static final String MSG_RES = "resource";
	public static final String MSG_GOT_DURATION = "duration";
	public static final String MSG_PlAYTIME = "PLAYTIME";
	public static final String MSG_PlAYTIMEPERCENT = "PTTTP";


	public enum Status {PLAY, PAUSE, STOP, ERROR};
	public enum DurationUnit {FRAMES, SEC, MSEC, BYTES };

	/**
	 * returns the play status of the current media file
	 * @return the status
	 */
	public Status getStatus();
	/**
	 * 
	 * @return the duration of the media file, expressed in the Unit defined in {@link #getDurationUnit()}
	 */
	public double getDuration();

	/**
	 * 
	 * @return the current position of the media file, expressed in the Unit defined in {@link #getPositionUnit()}
	 */
	public double getPosition();

	/**
	 *
	 * this method sets the status of the media, so it DOES stop, play or pause the media
	 * 
	 * @param st the desired play status
	 * @return the previous status
	 */
	public Status setStatus(Status st);

	/**
	 * This method seeks to the specified position; the position is expressed as a percentage of the lenght of the media
	 * @param d
	 * @return
	 */
	public boolean setPosition(double d);


	public DurationUnit getDurationUnit ();
	public DurationUnit getPositionUnit();

	public boolean getDisplayGlobalUI();
}
