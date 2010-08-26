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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for iRODS url connections, using cahcing mechanism.
 *
 */
public class Handler extends URLStreamHandler {

	/* (non-Javadoc)
	 * @see java.net.URLStreamHandler#openConnection(java.net.URL)
	 */
	/**
	 */
	private static final Map<URL, IRODSURLConnection> cache = new HashMap<URL, IRODSURLConnection>();

	@Override
	protected synchronized URLConnection openConnection(URL u) throws IOException {
		IRODSURLConnection c = Handler.cache.get(u); //(SRBURLConnection)
		if (c==null) {
			c = new IRODSURLConnection(u);
			Handler.cache.put(u, c);
		}
		return c;
	}
}
