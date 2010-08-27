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
package uk.ac.liverpool.irods;

import java.net.URL;

import multivalent.Behavior;
import multivalent.SemanticEvent;

/**
 * This very simple behaviour is used to register the irods URL handler when loaded by Multivalent.
 * The registration must happen only once per virtual machine.
 * 
 *
 */

public class IRODSBehaviour extends Behavior {
	static boolean first= true;

	@Override
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		if (IRODSBehaviour.first) {
			IRODSBehaviour.first = false;
			System.out.println("Registering IRODS URL handler");
			//			register();
			IRODSURLStreamHandlerFactory fac = new IRODSURLStreamHandlerFactory();
			URL.setURLStreamHandlerFactory(fac);
			//			//SRBAccount.setVersion("SRB-3.3.1jargon&G");
		}
		return super.semanticEventAfter(se, msg);
	}

	public void register() {
		final String packageName = Handler.class.getPackage().getName();
		final String pkg = packageName.substring(0, packageName.lastIndexOf(  '.' ) );
		final String protocolPathProp =
			"java.protocol.handler.pkgs";

		String uriHandlers = System.getProperty(protocolPathProp, "" );
		if ( uriHandlers.indexOf( pkg ) == -1 ) {
			if ( uriHandlers.length() != 0 )
				uriHandlers += "|";
			uriHandlers += pkg;
			System.setProperty( protocolPathProp,uriHandlers );
			// System.out.println(protocolPathProp + " " + uriHandlers);
		}
	}
}

