/******************************************************************************
 *  
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 3 of the License, or (at your option) 
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Author     : Jerome Fuselier
 * Creation   : September 2011
 *  
 *****************************************************************************/

package uk.ac.liverpool.MXF;

import multivalent.Behavior;
import multivalent.Document;
import multivalent.SemanticEvent;

import uk.ac.liverpool.MXF.MXFDocument;

public class MXFBehaviour extends Behavior {

    public static final String MSG_NEXT = "nextFrame";
    public static final String MSG_PREV = "prevFrame";
    public static final String MSG_NEXT2 = "nextFrame2";
    public static final String MSG_PREV2 = "prevFrame2";
    public static final String MSG_PLAYPAUSE = "play";
	
	
	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		Document doc = getDocument();
		MXFDocument mxf = (MXFDocument) doc.getValue("mxfdoc");
		
		if (msg.equals(MXFBehaviour.MSG_NEXT))
			mxf.next_frame();
		else if (msg.equals(MXFBehaviour.MSG_PREV))
			mxf.previous_frame();
		else if (msg.equals(MXFBehaviour.MSG_NEXT2))
				mxf.next_frame2();
			else if (msg.equals(MXFBehaviour.MSG_PREV2))
				mxf.previous_frame2();
		else if (msg.equals(MXFBehaviour.MSG_PLAYPAUSE))
			mxf.playpause();
		
		return false;
	}
}
