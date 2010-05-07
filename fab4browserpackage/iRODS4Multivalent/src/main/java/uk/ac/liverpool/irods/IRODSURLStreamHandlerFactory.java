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
/**
 * Author: Fabio Corubolo - f.corubolo@liv.ac.uk
 * (c) 2005 University of Liverpool
 */
package uk.ac.liverpool.irods;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * @author fabio
 * 
 */
public class IRODSURLStreamHandlerFactory implements URLStreamHandlerFactory {

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.URLStreamHandlerFactory#createURLStreamHandler(java.lang.String)
	 */
	Handler sh;




	public URLStreamHandler createURLStreamHandler(String protocol) {
		if (protocol.equals("irods")) {
			if (sh == null)
				sh = new Handler();
			return sh;
		}
		return null;
	}

}
