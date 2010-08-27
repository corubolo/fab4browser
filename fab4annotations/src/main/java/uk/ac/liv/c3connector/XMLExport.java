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

package uk.ac.liv.c3connector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.swing.JFileChooser;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Mark;
import multivalent.Node;
import multivalent.Span;
import multivalent.node.LeafText;
import multivalent.std.span.BackgroundSpan;
import uk.ac.liverpool.fab4.Fab4;
/**
 * This class is work in progress, and is used to export a document to XML inlining annotations.
 * It is used by the linguistic annotation features.
 * 
 * @author fabio
 *
 */
public class XMLExport {

	private DistributedPersonalAnnos dpa;

	private JFileChooser jc_;

	HashSet<Span> spans = new HashSet<Span>();
	HashSet<Span> spane = new HashSet<Span>();


	int charnum;
	public XMLExport(DistributedPersonalAnnos dpa) {
		this.dpa = dpa;

	}


	Browser getBrowser(){
		return dpa.getBrowser();
	}


	private Stack<String> openTags= new Stack<String>();

	private Stack<String> openAtts= new Stack<String>();


	private File choseFile() {
		if (jc_==null) {
			jc_ = new JFileChooser();
			jc_.setCurrentDirectory(Fab4.getCurrentDir());
		}
		final Document mvDocument = (Document) getBrowser().getRoot().findBFS("content");
		jc_.setDialogType(JFileChooser.SAVE_DIALOG);
		jc_.setDialogTitle("Chose the xml export file name");
		File newpath = new File(jc_.getCurrentDirectory() ,mvDocument.getURI().getPath()+".xml");
		jc_.setSelectedFile(newpath);
		if (jc_.showSaveDialog(getBrowser()) == JFileChooser.APPROVE_OPTION)
			return jc_.getSelectedFile();
		return null;
	}
	public void exportToXml(int method) {
		File wt = choseFile();
		if (wt==null)
			return;

		Browser br = getBrowser();
		final Document mvDocument = (Document) br.getRoot().findBFS("content");
		Layer personal = mvDocument.getLayer(Layer.PERSONAL);

		for (int i = 0; i < personal.size(); i++) {
			Behavior annotationBehaviour = personal.getBehavior(i);
			//////		System.out.println(annotationBehaviour.getClass());
			if (annotationBehaviour instanceof BackgroundSpan) {
				BackgroundSpan sp = (BackgroundSpan) annotationBehaviour;
				sp.getStart();
				sp.getEnd();
			}
		}

		try {
			FileOutputStream fos = new FileOutputStream(wt);
			OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
			BufferedWriter fw = new BufferedWriter(osw);
			String s = getTextSpaced(mvDocument, method);
			System.out.println(s);
			fw.write(s);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(getTextSpaced(mvDocument));
	}

	public String getTextSpaced(Document doc, int method) {
		StringBuilder ret = new StringBuilder();
		openTag("articles",ret);
		switch (method) {
		case 1:
			walkAndSpace(doc.childAt(0), ret, doc);
			break;
		case 2:
			walkAndSpaceOld(doc.childAt(0), ret, doc);
			break;
		case 3:
			walkAndSpace2(doc.childAt(0), ret, doc, false);
			break;
		case 4:
			walkAndSpace2(doc.childAt(0), ret, doc, true);
			break;

		default:
			break;
		}

		closeTag("articles",ret);


		return ret.toString();
	}

	void walkAndSpace2(Node node, StringBuilder sb,Document doc, boolean single) {
		if (node == null)
			return;
		String name = node.getName();
		if (name == null) {
		} else if (node instanceof INode) {
			if (single)
				inodeHandle(node, sb, doc, name,4);
			else
				inodeHandle(node, sb, doc, name,4);
		} else if (node instanceof LeafText) {
			LeafText lt = (LeafText) node;
			String s = lt.getText();
			List<String>[] ot =new LinkedList[s.length()+1];
			List<String>[] ct =new LinkedList[s.length()+2];
			Layer personal = doc.getLayer(Layer.PERSONAL);
			for (int i=0;i<lt.sizeSticky();i++){
				Mark sticky = lt.getSticky(i);
				Object o = sticky.getOwner();
				if (o instanceof BackgroundSpan) {
					BackgroundSpan bgs = (BackgroundSpan) o;
					if (bgs.getLayer().equals(personal)){
						if (bgs.getStart().equals(sticky)){
							//if (!spans.contains(bgs)  || single){
							if (ot[sticky.offset] == null)
								ot[sticky.offset] = new LinkedList<String>();
							ot[sticky.offset].add(bgs.getColorName());

						}
						if (bgs.getEnd().equals(sticky)){
							int k = sticky.offset-1;
							if (k<0)
								k = s.length()+1;
							if (ct[k] == null)
								ct[k] = new LinkedList<String>();
							ct[k].add(bgs.getColorName());
						}
					}
				}
			}
			if (ct[s.length()+1]!=null)
				for (String ss:ct[s.length()+1])
					closeTag(ss, sb);
			for (int i = 0; i < s.length(); i++) {
				char ch = s.charAt(i);
				if (ot[i]!=null)
					for (String ss:ot[i])
						openTag(ss,sb);
				escape(sb, ch);
				if (ct[i]!=null && i!=s.length())
					for (String ss:ct[i])
						closeTag(ss, sb);
			}

			if (ot[s.length()]!=null)
				for (String ss:ot[s.length()])
					openTag(ss,sb);
			sb.append(' ');

			if (ct[s.length()]!=null)
				for (String ss:ct[s.length()])
					closeTag(ss, sb);
		}
	}

	void walkAndSpace(Node node, StringBuilder sb,Document doc) {
		if (node == null)
			return;
		String name = node.getName();
		if (name == null) {
		} else if (node instanceof INode)
			inodeHandle(node, sb, doc, name,1);
		else if (node instanceof LeafText) {
			LeafText lt = (LeafText) node;
			String s = lt.getText();
			List<String>[] ot =new LinkedList[s.length()+1];
			List<String>[] ct =new LinkedList[s.length()+2];

			Layer personal = doc.getLayer(Layer.PERSONAL);
			for (int i=0;i<lt.sizeSticky();i++){
				Mark sticky = lt.getSticky(i);
				//System.out.println(sticky);
				Object o = sticky.getOwner();
				if (o instanceof BackgroundSpan) {
					BackgroundSpan bgs = (BackgroundSpan) o;
					if (bgs.getLayer().equals(personal)){
						if (bgs.getStart().equals(sticky)){
							if (ot[sticky.offset] == null)
								ot[sticky.offset] = new LinkedList<String>();
							ot[sticky.offset].add(bgs.getColorName());
						}

						if (bgs.getEnd().equals(sticky)){
							int k = sticky.offset - 1;
							if (k<0)
								k = s.length() + 1 ;
							if (ct[k] == null)
								ct[k] = new LinkedList<String>();
							ct[k].add("ltag");
						}
					}
				}
			}
			if (ct[s.length()+1]!=null)
				for (String ss:ct[s.length()+1])
					closeTag(ss, sb);
			for (int i = 0; i < s.length(); i++) {
				char ch = s.charAt(i);
				if (ot[i]!=null)
					for (String ss:ot[i])
						openTag("ltag","name=\""+ss+"\"",sb);

				escape(sb, ch);

				if (ct[i]!=null && i!=s.length())
					for (String ss:ct[i])
						closeTag(ss, sb);
			}

			if (ot[s.length()]!=null)
				for (String ss:ot[s.length()])
					openTag("ltag","name=\""+ss+"\"",sb);

			sb.append(' ');

			if (ct[s.length()]!=null)
				for (String ss:ct[s.length()])
					closeTag(ss, sb);
		}
	}


	private void inodeHandle(Node node, StringBuilder sb, Document doc,
			String name, int sit) {
		INode inode = (INode) node;
		if (name.equals("div"))
			openTag(node.getAttr("class"), sb);
		if (name.equals("h2"))
			openTag("headline", sb);
		if (name.equals("p"))
			openTag("p", sb);
		for (int i = 0, imax = inode.size(); i < imax; i++)
			switch (sit) {
			case 1:
				walkAndSpace(inode.childAt(i), sb, doc);
				break;
			case 2:
				walkAndSpaceOld(inode.childAt(i), sb, doc);
				break;
			case 3:
				walkAndSpace2(inode.childAt(i), sb, doc, false);
				break;
			case 4:
				walkAndSpace2(inode.childAt(i), sb, doc, true);
				break;
			default:
				break;
			}

		if (name.equals("p"))
			closeTag("p", sb);
		if (name.equals("h2"))
			closeTag("headline", sb);
		if (name.equals("div"))
			closeTag(node.getAttr("class"), sb);

		sb.append('\n');
	}


	void walkAndSpaceOld(Node node, StringBuilder sb,Document doc) {
		if (node == null)
			return;
		String name = node.getName();
		if (name == null) {
		} else if (node instanceof INode)
			inodeHandle(node, sb, doc, name,2);
		else if (node instanceof LeafText) {
			LeafText lt = (LeafText) node;
			String s = lt.getText();
			String[] ot = new String[s.length()+1];
			String[] ct = new String[s.length()+2];

			Layer personal = doc.getLayer(Layer.PERSONAL);
			for (int i=0;i<lt.sizeSticky();i++){
				Mark sticky = lt.getSticky(i);

				Object o = sticky.getOwner();
				if (o instanceof BackgroundSpan) {
					BackgroundSpan bgs = (BackgroundSpan) o;
					if (bgs.getLayer().equals(personal)){
						//System.out.println(sticky);
						//System.out.println(bgs);

						if (bgs.getStart().equals(sticky))
							ot[sticky.offset] = bgs.getColorName();
						if (bgs.getEnd().equals(sticky)){
							int k = sticky.offset-1;
							if (k<0)
								k = s.length()+1;
							ct[k] = "ltag";
						}
					}
				}
			}
			//			if (ct[s.length()]!=null){
			//			closeTag(ct[s.length()], sb);
			//			}
			if (ct[s.length()+1]!=null)
				closeTag(ct[s.length()+1], sb);
			for (int i = 0; i < s.length(); i++) {
				char ch = s.charAt(i);
				if (ot[i]!=null)
					openTag("ltag","name=\""+ot[i]+"\"",sb);
				escape(sb, ch);

				if (ct[i]!=null && i!= s.length())
					closeTag(ct[i], sb);
			}
			if (ot[s.length()]!=null)
				openTag("ltag","name=\""+ot[s.length()]+"\"",sb);
			sb.append(' ');
			if (ct[s.length()]!=null)
				closeTag(ct[s.length()], sb);
		}
	}


	private void openTag(String name, String attrs, StringBuilder sb)  {
		if (attrs == null || attrs.length()==0)
			openTag(name, sb);
		else {
			sb.append('<').append(name).append(' ').append(attrs).append('>');
			openTags.push(name);
			openAtts.push(attrs);
			//			for (int i=0;i<openTags.size();i++)
			//			System.out.append("  ");
			//			System.out.println(name +"   " + attrs);
		}
	}

	private void openTag(String name,StringBuilder sb)  {
		sb.append('<').append(name).append('>');
		openTags.push(name);
		openAtts.push("");
		//		for (int i=0;i<openTags.size();i++)
		//		System.out.append("  ");
		//		System.out.println(name );
	}

	private void closeTag(String name, StringBuilder sb)  {

		String nt = openTags.pop();
		String at = openAtts.pop();

		boolean reopen = false;
		sb.append("</");
		sb.append(nt);
		sb.append('>');

		if (!nt.equals(name)) {
			closeTag(name, sb);
			reopen = true;
		}

		if (reopen)
			openTag(nt, at, sb);

	}


	private void escape(StringBuilder sb, char ch) {
		switch (ch) {
		case '"':
			sb.append("&quot;");
			break;
		case '\'':
			sb.append("&apos;");
			break;
		case '&':
			sb.append("&amp;");
			break;
		case '<':
			sb.append("&gt;");
			break;
		case '>':
			sb.append("&gt;");
			break;
		default:
			sb.append(ch);
			break;
		}
	}
}
