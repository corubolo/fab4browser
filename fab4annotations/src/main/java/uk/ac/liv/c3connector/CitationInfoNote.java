package uk.ac.liv.c3connector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.thoughtworks.xstream.core.TreeMarshaller.CircularReferenceException;

import multivalent.Behavior;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.INode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.SemanticEvent;
import multivalent.StyleSheet;
import multivalent.gui.VButton;
import multivalent.gui.VFrame;
import multivalent.gui.VRadiobox;
import multivalent.gui.VRadiogroup;
import multivalent.gui.VScrollbar;
import multivalent.gui.VTextArea;
import multivalent.node.IParaBox;
import multivalent.node.IVBox;
import multivalent.node.LeafUnicode;
import phelps.awt.Colors;
import phelps.lang.Integers;
import uk.ac.liv.c3connector.ui.Authenticator;
import uk.ac.liv.c3connector.ui.DocumentInfoRequester;
import uk.ac.liv.c3connector.ui.FabAnnoListRenderer;
import uk.ac.liverpool.fab4.AnnotationSidePanel;
import uk.ac.liverpool.fab4.Fab4;
import uk.ac.liverpool.fab4.Fab4utils;
import uk.ac.liverpool.fab4.PersonalAnnos;

public class CitationInfoNote extends Behavior{

//	VFrame win_ ;
//	JDialog dialog ;
	DocumentInfoRequester docInfoRequester;
	
	private static boolean modified = false;
	
	@Override
	public void restore(ESISNode n, Map<String,Object> attr, Layer layer){
		modified = false;
		
		this.setName("bibtex");
		super.restore(n,attr, layer);
		
		
		final Document doc = (Document) getBrowser().getRoot().findBFS("content");
//		float zl = doc.getMediaAdaptor().getZoom();
				
//		dialog = new JDialog(Fab4.getMVFrame(getBrowser()));
				
		String name = "BIBTEX"+String.valueOf(Math.abs(FabNote.random.nextInt()));
		putAttr("name", name);		
		
		String needed = "0";	
		String url = doc.getURI().toString();
		try{		
			needed = DistributedPersonalAnnos.askIfUrlLacksBibDoiKeywords(url);
			if(!needed.equals("11")){
				//while( Authenticator.running );
				docInfoRequester = new DocumentInfoRequester(/*this, Fab4.getMVFrame(getBrowser())*/ null, true, /*uri,*/needed);
			}
			
			boolean sthGiven = false;
			if(DocumentInfoRequester.title != null){				
				putAttr("bibtitle", DocumentInfoRequester.title);
				DocumentInfoRequester.title = null;
				sthGiven = true;
			}
			if(DocumentInfoRequester.authors != null){				
				putAttr("bibauthors", DocumentInfoRequester.authors);
				DocumentInfoRequester.authors = null;
				sthGiven = true;
			}
			if(DocumentInfoRequester.doi != null){
				putAttr("doi", DocumentInfoRequester.doi);
				DocumentInfoRequester.doi = null;
				sthGiven = true;
			}
			if(DocumentInfoRequester.keywords != null){
				putAttr("keywords", DocumentInfoRequester.keywords);
				DocumentInfoRequester.keywords = null;
				sthGiven = true;
			}
			//wait();
					
			if(sthGiven){
				if(getValue(DistributedPersonalAnnos.FABANNO) != null)
					putAttr(DistributedPersonalAnnos.FABANNO, null);

				putAttr("needed", needed);
				putAttr("url",url);
				modified = true;
				
				AnnotationSidePanel.bPubAnno.doClick();
			}
			else{
				if(getValue(DistributedPersonalAnnos.FABANNO) == null)
					putAttr(DistributedPersonalAnnos.FABANNO, FabAnnotation.dummy_);
			}
		}catch(Exception e ){
			System.out.println("*****caught:");
			e.printStackTrace();
			System.out.println("******");
		}
		
		if(layer.getBehavior("bibtex") == null)
			layer.addBehavior(this);
	}
	
	@Override
	public ESISNode save(){
		ESISNode e;
		this.setName("bibtex");

		e = super.save();
		if (getAttr("uri")==null) {  // inline -- for now only editable kind
			StringBuffer csb = getStringContent();
			ESISNode content = new ESISNode("content");
			e.appendChild(content);
			content.appendChild(csb.toString());
		}
		
		
		return e;
	}
	
	private StringBuffer getStringContent() {
		StringBuffer csb = new StringBuffer(2000);
//		doc_.clipboardBeforeAfter(csb);  // doesn't save character attributes yet
		String trim = csb.substring(0).trim();
		csb.setLength(0);
		for (int i=0,imax=trim.length(); i<imax; i++) {
			char ch = trim.charAt(i);
			if (ch=='\n')
				csb.append("<br/>");
			else
				csb.append(ch);
		}
		// ampty string creates a <content/> that breaks MV's xml parser
		if (csb.length()==0)
			csb.append("<br/>");
		//System.out.println("00!!!!!");
		return csb;
	}
	
	@Override
	public void destroy() {
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		/*if (win_!=null)
			win_.close();
		win_=null;*/
		if (getValue(DistributedPersonalAnnos.FABANNO)!=null)
			((FabAnnotation)getValue(DistributedPersonalAnnos.FABANNO)).setLoaded(false);
		try {
			if (doc!=null)
				doc.deleteObserver(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.destroy();
	}
	
	public void setVisible(boolean vis){
		if(vis)
			docInfoRequester.setVisible(true);
		else
			docInfoRequester.setVisible(false);
	}

	public boolean isModified(){
		return modified;
	}
	
	
	/**
	 * 
	 * @param oldURi
	 * @return resourceId if a resource (with some bibtex information) is added or updated, 
	 * 			-1 if any problem happens while adding/updating, 
	 * 			-2 if no updates in bibtex needed
	 */
	/*private void requestBibOrUrl(String uri){
			
//		HashMap<URI,Integer> info = new HashMap<URI, Integer>();
		
//		Integer resourceId = -1;
		
//		String uri = oldURi.toString();
//		String newUrl = uri;
		String bibtex = null;
		String doi = null;
		String keywords = null;
		String needed = "0";	
		
		try{		
			if(ras == null)
				setWasToLocalOrRemote();
			needed = DistributedPersonalAnnos.askIfUrlLacksBibDoiKeywords(uri);
			if(!needed.equals("11")){
				while( Authenticator.running );
//				if(!Authenticator.CANCEL){
					DocumentInfoRequester docR = new DocumentInfoRequester(this, Fab4.getMVFrame(getBrowser()) null, true, uri,needed);
//					docR.setLocationRelativeTo(null);
//					docR.setVisible(true);
//				}
			}
			
			
			
			if(DocumentInfoRequester.bibtex != null){
				bibtex = DocumentInfoRequester.bibtex;
				DocumentInfoRequester.bibtex = null;
			}
			if(DocumentInfoRequester.doi != null){
				doi = DocumentInfoRequester.doi;
				DocumentInfoRequester.doi = null;
			}
			if(DocumentInfoRequester.keywords != null){
				keywords = DocumentInfoRequester.keywords;
				DocumentInfoRequester.keywords = null;
			}
			//wait();
					
		
		
		
		if(needed.equals("0"))
			resourceId = DistributedPersonalAnnos.addNewResource(bibtex, newUrl uri);
		else if(doi != null || keywords != null)
			resourceId = DistributedPersonalAnnos.updateResourceBib(uri,doi,keywords);
		 //should be in the server side!
		
		}catch(Exception e){			
			e.printStackTrace();		
		}
		
//		return resourceId;
	}*/
	
}
