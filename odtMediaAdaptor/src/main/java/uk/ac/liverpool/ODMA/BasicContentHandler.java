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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 * BasicContentHandler
 * 
 * this class implements all is needed for the model: start content end.
 * All unimplemented tags are ignored.
 * 
 * This is the base for all other handlers. Does not take into account particular features
 * of text documents.
 * 
 * Use: extend this class in your own and implement s_, c_ and e_ methods for the
 * different tags.
 * example: start (text:p-piu) calls method
 * <code>public void s_text_p__piu()</code>
 * 
 * Declare the s_, c_ and e_ always public!
 * 
 * We have 2 options on how methods are called.
 * 
 * Default option is option 2:
 * 
 * option 1
 * In child classes (extension classes) s_, c_ and e_ tags of the parent are ignored even if
 * no sobstitution is specified in the current class.
 * To put in another way, we consider s, c and e method declared in this class , not inherited s,c,e methods
 *
 * option 2
 * works like normal inheritance
 * 
 * TODO: document better this class and the model
 * 
 */
public class BasicContentHandler extends DefaultHandler implements SCECallable {

	/** The max depth of the tree, 640 should be enough for everyone :)
	 * Rally, it's hard do go very deep with this trees
	 * 
	 * */
	protected static final int MAX_DEPTH = 640;

	private static final int NOEAT=-100;
	/** the content of the current element. Used in the content methods (c_tagname) */
	protected StringBuilder content;

	/** array of tag names in depth; needed?
	 * YES because case:<code><T>pippo <b>bold</b> pluto <i>topolino</i> e minne</t>
	 *                               content(t)          content(b)
	 * if I use previous tag!
	 * */
	protected String[] tagName = new String[BasicContentHandler.MAX_DEPTH];

	/**
	 * we keep track of the attributes too, tree depth should
	 * not be too big
	 * have to check how much we consume with these
	 * seems not to work??
	 * */
	//Attributes[] attribs = new Attributes[MAX_DEPTH];
	public Attributes startAttirbs;

	/** depth in the parsing tree (the XML depth) */
	protected int depth = -1;

	/** maybe remove this?? */
	//    public int defaultMethod[] = new int[MAX_DEPTH]; // the standard method of type n, if n>0

	protected String currentTag = null;

	public multivalent.Document doc;

	private HashMap <String, Class>redirections = new HashMap<String, Class>(200,0.5f);

	// main methods
	private MethodName[] startMethods = null;
	private MethodName[] contentMethods = null;
	private MethodName[] endMethods = null;

	// redirected object for the methods!
	// for now we have single callable, in the future it may be replaced by a vector
	// the furure is now :)
	private Stack <SCECallable>callstack = new Stack<SCECallable>();
	SCECallable callable;
	//private SCECallable callable[] = new SCECallable[MAX_DEPTH];

	private compa compaC;
	private boolean inheritCalls = true;
	private int eatingDepth = BasicContentHandler.NOEAT;

	/** dafault is true */
	public boolean isInheritingCalls() {
		return inheritCalls;
	}

	/** dafault is true */
	public void setInheritingCalls(boolean inheritCalls) {
		if (this.inheritCalls != inheritCalls) {
			this.inheritCalls = inheritCalls;
			initMethods();
		}

	}

	public BasicContentHandler(multivalent.Document d) {
		doc = d;
		compaC = new compa();
		callable=this;
		initMethods();
	}

	/** This is for automatic redirection: since in most (ideally all)
	 * cases we will want to redirect on all occurrences of a tag, and
	 * go back at the end of the tag, this can be set up in the main class.
	 * redirection overrides everything else!

	 */
	public void addAutomaticRedirection(String tagN, Class tagClass) {
		redirections.put(tagN, tagClass);
	}

	public void removeAutomaticRedirection(String tag) {
		redirections.remove(tag);
	}

	private void initMethods() {
		/*
		 * V getMethods
		 * all PUBLIC methods (inherited too)
		 */
		Method[] ems;
		if (!inheritCalls) ems = this.getClass().getDeclaredMethods();
		else ems = this.getClass().getMethods();

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
	}

	/**
	 * This gets called every content. the state is set to the containing tag.
	 * calls method c_tag_name__1 if in tag:name-1 for the content of the tag
	 * can be overridden, but please do
	 * super.manageContent();
	 * 
	 * */
	protected void manageContent() {
		call("c:" + tagName[depth]);
	}

	/** Implemented in ContentHandler
	 * TODO: DOC this and the othe methods
	 * */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (content != null) {
			manageContent();
			content = null;
		}
		depth++;
		/* setup */
		currentTag = qName;
		tagName[depth] = qName;
		//        defaultMethod[depth] = 0;
		startAttirbs = attributes;
		// we do get redirections (if existing) otherwise we try where we are
		// that is in the current parser (this or a redirection)

		// TODO: take care of single instance (use a single instance of the tag class)
		// se: classic methods, factory?? baybe implementing other interface!
		Class red = redirections.get(qName);
		if (red != null && !callable.isEatingAll()) try {
			callstack.push(callable);
			/* TODO: implement the single instance classes (if needed) */
			callable = (TagAdapter)red.newInstance();
			((TagAdapter)callable).init(this);
			if (callable.isEatingAll())
				eatingDepth = depth;
		} catch (Exception e) {
			e.printStackTrace();
		}
		call("s:" + qName);

		startAttirbs = null;
	}

	/** Implemented in ContentHandler */
	@Override
	public void endElement(String uri, String localName, String qName)
	throws SAXException {
		if (content != null) {
			manageContent();
			content = null;
		}
		//        Class red = (Class) redirections.get(qName);
		// THIS HAS TO BE HERE because some tag parsers can remove themselves
		// from the redirections on endtag (NON nested ones)
		boolean exits = redirections.containsKey(qName);
		//System.out.println(qName+" -- "+callable.getTagName());
		call("e:" + qName);
		// here we return after the call,
		// so the redirected tag gets called on both start and end
		if (eatingDepth >= depth)
			eatingDepth = BasicContentHandler.NOEAT;
		// we pop the stack only if exiting
		if (exits && eatingDepth==BasicContentHandler.NOEAT) try {
			callable = callstack.pop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		/* cleanup! */
		tagName[depth] = null;
		//        defaultMethod[depth] = 0;
		depth--;
		if (depth > 0) currentTag = tagName[depth];

	}


	/*
    /** This now has a single level: main (this) -> tag
	 * works only if we are in main!
	 * In the future it's possible to add avector to manage
	 * nested changes, but that seem to create easy problems
	 * 
	 * @param c The redirection callable
	 * @throws Exception if nested

    protected void redirect(SCECallable c) {
        callable[depth]=c;
    }

 This now has a single level: tag -> main (this)
	 * works only if we are in tag (not in main)!
	 * In the future it's possible to add avector to manage
	 * nested changes, but that seem to create easy problems
	 * 
	 * @throws Exception if already in main


    protected void unRedirect(){
        callable(de
    }
	 */

	/** Implemented in ContentHandler */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (content == null) content = new StringBuilder(length);
		content.append(ch, start, length);
	}

	/**
	 * DECIDE: we call both specific and default method or specific , default if no specific?
	 * NOW ve call:  default if no specific
	 * 
	 * Calls the specified method. if no such method is available,
	 * tries to call the default method for the current tag:
	 * <code>
	 * String name2 = name.substring(0, 2) + "default_method_" + defaultMethod[depth];
	 * <code>
	 * example: for name = e:text:p
	 * tries to call method e_text_p()
	 * if it doesn't exist, and defaultMethod[depth] >0 , tries to call
	 * e_default_method_<defaultMethod[depth]>()
	 * this way we can define different default methods for classes of tags
	 * Default metods are defined in the s_tagname ie. in the start tag handler and can be overridden
	 * meaning that specific handlers override default ones.
	 * 
	 * @param name
	 */

	private void call(String name) {
		/* First we try the specific method */
		MethodName[] methods;
		if (name.charAt(0) == 's') methods = callable.getStartMethods();
		else if (name.charAt(0) == 'c') methods = callable.getContentMethods();
		else if (name.charAt(0) == 'e') methods = callable.getEndMethods();
		else {
			System.out.println("invalid call on: " + name);
			return;
		}
		int num = Arrays.binarySearch(methods, name, compaC);
		Method m;
		if (num < 0) {
			/* If no specific, we can try with the default method
			 * for this case.
			 * If specified (>0) the default method SHOULD exist!
			 * */
			//            if (defaultMethod[depth] > 0) {
			//                String name2 = name.substring(0, 2) + "default:method:"
			//                    + defaultMethod[depth];
			//                num = Arrays.binarySearch(methods, name2, compaC);
			//            }
		}
		// nothing to do
		if (num < 0)
			return;
		m = methods[num].m;
		try {
			// changed from this to callable!
			m.invoke(callable, (Object[])null);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.getTargetException().printStackTrace();
		}

	}

	private class compa implements Comparator {
		public int compare(Object arg0, Object arg1) {
			return ((MethodName) arg0).name.compareTo((String) arg1);
		}

	}

	public MethodName[] getStartMethods() {
		return startMethods;
	}

	public MethodName[] getContentMethods() {
		return contentMethods;
	}

	public MethodName[] getEndMethods() {
		return endMethods;
	}

	public static int getMAX_DEPTH() {
		return BasicContentHandler.MAX_DEPTH;
	}

	public StringBuilder getContent() {
		return content;
	}

	public String getCurrentTag() {
		return currentTag;
	}

	//    public int[] getDefaultMethod() {
		//        return defaultMethod;
	//    }

	public int getDepth() {
		return depth;
	}

	public multivalent.Document getDoc() {
		return doc;
	}

	public Attributes getAttr() {
		return startAttirbs;
	}

	public String[] getTagNames() {
		return tagName;
	}

	/* (non-Javadoc)
	 * @see uk.ac.liverpool.ODMA.SCECallable#getTagName()
	 */
	public String getTagName() {

		return null;
	}

	/** defaults to false
	 * if true, we don't call subtags :)
	 */
	public boolean isEatingAll() {
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.liverpool.ODMA.SCECallable#isSingleInstance()
	 */
	public boolean isSingleInstance() {
		return true;
	}


	public void copyAttributesTo(Map <String, String>m) {
		for (int i=0;i<startAttirbs.getLength();i++)
			m.put(startAttirbs.getQName(i),startAttirbs.getValue(i));

	}
	public static void copyAttributesTo(Attributes att, Map <String, String>m) {
		for (int i=0;i<att.getLength();i++)
			m.put(att.getQName(i),att.getValue(i));

	}

}