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
package uk.ac.liverpool.ODMA.textTags;

import uk.ac.liverpool.ODMA.BasicContentHandler;
import uk.ac.liverpool.ODMA.TagAdapter;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * This ignores tracked changes (eating all subtags)
 * 
 * TODO: if interesting, implement tracking changes (using annos/mv features)
 */
public class TrackedChangesTag extends TagAdapter {


	/* CUT AND PASTE PART STARTS HERE */

	/* Change this to return the tag name  */
	public String getTagName() {
		return "text:tracked-changes";
	}
	/* Change this method to include other init actions */
	@Override
	public void init(BasicContentHandler cc) {
		super.init(cc);
		if (TrackedChangesTag.startMethods == null) {
			MethodName [][] m = initMethods(getClass(), true);
			TrackedChangesTag.startMethods = m[0];TrackedChangesTag.contentMethods = m[1];TrackedChangesTag.endMethods = m[2];
		}
	}
	private static MethodName[] startMethods = null;
	private static MethodName[] contentMethods = null;
	private static MethodName[] endMethods = null;
	@Override
	public MethodName[] getContentMethods() {
		return TrackedChangesTag.contentMethods;
	}
	@Override
	public MethodName[] getEndMethods() {
		return TrackedChangesTag.endMethods;
	}
	@Override
	public MethodName[] getStartMethods() {
		return TrackedChangesTag.startMethods;
	}
	/* CUT AND PASTE PART ENDS HERE*/
	// and tracked changes !!

	/*
    public void s_text_tracked__changes() {
    }
	 */
	/* since it appears only once per document (In the PRELUDE), we can remove at the end,
	 * saving some time (?)
	 */
	public void e_text_tracked__changes() {
		bc.removeAutomaticRedirection(getTagName());

	}

	@Override
	public boolean isEatingAll() {
		return true;
	}


}
