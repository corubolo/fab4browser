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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.Attributes;

/**
 * 
 * This class is the base for tag adapters (subparsers).
 * The idea is: we have a hiracy of BasicContentHandlers that allows us
 * to divide functionality per content type.
 * To implement the parser one extends the BasicContantHandler and implements the
 * methods for the tags (Start, Content and End) as PUBLIC s_text_p() etc.
 * 
 * Now, once in a tag like Index, we have few choices of allowed subtags,
 * that can only appear there. In order to divide the parser per tag and have it more readable
 * we create extension of this class to manage all the content of a tag (or more than one) and its subtags.
 * 
 * This way <text:list><text:list-item>1</text:list-Item><text:list> can all be managed in the same class
 * making it much clearer and faster (less choices in the main index of methods and in subindexes!)
 * 
 * 
 * <br><br>
 * 
 * 
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * TagAdapter
 */
public abstract class TagAdapter implements SCECallable{


	protected BasicContentHandler bc;

	public static boolean prints = false;

	public TagAdapter() {
	}

	protected boolean inheritCalls = true;

	/** dafault is true */
	public boolean isInheritingCalls() {
		return inheritCalls;
	}

	public void setInheritingCalls(boolean inheritCalls) {
		this.inheritCalls = inheritCalls;
	}

	protected static MethodName[][] initMethods(Class c,boolean inheritCalls) {
		Method[] ems;
		MethodName[] startMethods, contentMethods, endMethods;
		System.out.println("init: "+c);
		if (!inheritCalls) ems = c.getDeclaredMethods();
		else ems = c.getMethods();

		int s1 = 0, s2 = 0, s3 = 0;
		for (Method em : ems) {
			String name = em.getName();
			if (name.charAt(1) != '_') continue;
			if (name.charAt(0) == 's')
				s1++;
			else if (name.charAt(0) == 'c')
				s2++;
			else if (name.charAt(0) == 'e')
				s3++;
		}
		startMethods = new MethodName[s1];
		contentMethods = new MethodName[s2];
		endMethods = new MethodName[s3];
		s1 = 0;
		s2 = 0;
		s3 = 0;
		for (Method em : ems) {
			if (em.getName().charAt(1) != '_') continue;
			String name = em.getName().replaceAll("__", "-").replaceAll("_", ":");
			if (em.getName().charAt(0) == 's') {
				startMethods[s1] = new MethodName(em, name);
				s1++;
			} else if (em.getName().charAt(0) == 'c') {
				contentMethods[s2] = new MethodName(em, name);
				s2++;
			} else if (em.getName().charAt(0) == 'e') {
				endMethods[s3] = new MethodName(em, name);
				s3++;
			}
		}
		Arrays.sort(startMethods);
		Arrays.sort(contentMethods);
		Arrays.sort(endMethods);
		return new MethodName[][] {startMethods,contentMethods,endMethods};

	}


	public abstract MethodName[] getStartMethods() ;
	public abstract MethodName[] getContentMethods();

	public abstract MethodName[] getEndMethods() ;

	protected void init(BasicContentHandler bc1) {
		bc = bc1;
	}
	/** defaults to false
	 * override to change be careful to change outside of tags.
	 */
	public boolean isEatingAll() {
		return false;
	}

	/**
	 * Override to say if the tag is managed by a single object
	 * dafault is false
	 * 
	 * Has to be implemented still
	 */
	public boolean isSingleInstance() {
		return false;
	}

	public static void printAttr(Attributes att) {
		if (!TagAdapter.prints)
			return;
		for (int i=0;i<att.getLength();i++)
			System.out.println(att.getQName(i)+" = "+att.getValue(i));
		System.out.println("***");

	}
	public static void printMap(Map<String, String>att) {
		if (!TagAdapter.prints)
			return;
		Iterator<String> i = att.keySet().iterator();
		while (i.hasNext()) {
			String s = i.next();
			System.out.println(s+" = "+att.get(s));
		}
		System.out.println("***");

	}

	public Attributes getAttr() {
		return bc.startAttirbs;
	}
}

