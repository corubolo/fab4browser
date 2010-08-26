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
package uk.ac.liverpool.ODMA;

import java.lang.reflect.Method;


/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * SCECallable
 * 
 * Main intercace for classes which have Start Content End (SCE) methods
 * used by the BasicContentHandler and by subparsers.
 * 
 * 
 */
public interface SCECallable {

	public class MethodName implements Comparable {
		Method m;
		String name;

		public MethodName(Method m, String name) {
			this.m = m;
			this.name = name;
		}

		public int compareTo(Object arg0) {
			return name.compareTo(((MethodName) arg0).name);
		}
	}

	/** override this to return the Start tag methods.
	 * See the template... */
	public MethodName[] getStartMethods();

	/** override this to return the Start tag methods */
	public MethodName[] getContentMethods();

	/** override this to return the Start tag methods */
	public MethodName[] getEndMethods();

	public boolean isInheritingCalls();

	/**
	 * Specifies if this tag will receive all subtags, even if otherwise specified
	 * @return true if will override all subtags for its content.
       defaults to false
	 */
	public boolean isEatingAll();


	/** the tag name that this class methods will be called for, including subtags (if not defined elsewere)
	 * 
	 * 
	 * @return the tag that this class takes care of
	 */
	public String getTagName();

	/** dafault is true
	 * (do not change) if methods are looked also on the ancestors of this class.
	 * 
	 * */
	public void setInheritingCalls(boolean inheritCalls);

	public boolean isSingleInstance();

}
