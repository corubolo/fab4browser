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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.DocInfo;
import multivalent.Document;
import multivalent.ESISNode;
import multivalent.Layer;
import multivalent.Leaf;
import multivalent.Node;
import multivalent.SemanticEvent;
import multivalent.Span;
import multivalent.gui.VFrame;
import multivalent.std.MediaLoader;
import multivalent.std.Note;
import multivalent.std.adaptor.XML;
import multivalent.std.span.BIUSpan;
import multivalent.std.span.BackgroundSpan;
import multivalent.std.ui.Multipage;

import org.apache.lucene.search.Hits;
import org.apache.xbean.finder.ResourceFinder;

import uk.ac.liv.c3connector.crypto.PKCS5SimpleKeyStore;
import uk.ac.liv.c3connector.ui.AnnotationProperties;
import uk.ac.liv.c3connector.ui.Authenticator;
import uk.ac.liv.c3connector.ui.DocumentInfoRequester;
import uk.ac.liv.c3connector.ui.FabAnnoAuthorComparator;
import uk.ac.liv.c3connector.ui.FabAnnoListRenderer;
import uk.ac.liv.c3connector.ui.FabAnnoModificationDateComparator;
import uk.ac.liv.c3connector.ui.FabAnnoPositionComparator;
import uk.ac.liv.c3connector.ui.FabAnnoTrustComparator;
import uk.ac.liv.c3connector.ui.LinguisticAnnoListRenderer;
import uk.ac.liv.c3connector.ui.PreferencesDialog;
import uk.ac.liv.c3connector.ui.StartupWizard;
import uk.ac.liverpool.annotationConnector.AnnotationServerConnectorInterface;
import uk.ac.liverpool.fab4.AnnotationSidePanel;
import uk.ac.liverpool.fab4.Fab4;
import uk.ac.liverpool.fab4.Fab4utils;
import uk.ac.liverpool.fab4.FabIcons;
import uk.ac.liverpool.fab4.FabPreferences;
import uk.ac.liverpool.fab4.PersonalAnnos;
import uk.ac.liverpool.fab4.behaviors.Fab4LabelSpan;
import uk.ac.liverpool.fab4.behaviors.TextSpanNote;

import com.base64;
import com.pt.io.InputUni;

/**
 * This is the main class implementing the distributed annotations methods.
 * 
 * A replacement for Multivalent's Personal annotations.
 * 
 * This supports distributed annotations using the Cheshire3 server and the SRW
 * protocol.
 * 
 * The submission uses a Cheshire3 Connector web service which has been
 * developed for Kepler.
 * 
 * Lexical signatures are computed by another web service.
 * 
 * This class implements the methods needed to sign and verify digital
 * signatures, based on the standard XML-Signature Syntax and Processing
 * 
 * http://www.w3.org/TR/xmldsig-core/
 * 
 * @author Fabio Corubolo - f.corubolo@liv.ac.uk
 * 
 */

enum Servers {RDF_SWORD, REST};

@SuppressWarnings("deprecation")
public class DistributedPersonalAnnos extends PersonalAnnos {

	/** name of the file */
	private static final String FAB4_TRUSTED_PARTIES_FILE = ".fab4_trustedParties";
	/** name of the file */
	private static final String FAB4_KEY_STORE_FILE = ".fab4_keyStore";

	public static final String FABANNO = "fabanno";

	public static char[] dafPass = null;

	public static String PrevAnnotationServerLocation = "";

	/** Location of the lexical signature server (not used by default) */
	public static String lswsAddress = "http://bodoni.lib.liv.ac.uk/c3c/services/ComputeLexical";

	///SAM
	private static final String annotationResourceURI = 
//		"http://localhost:8888/annotationResource"; 
		"http://hypatia.cs.ualberta.ca:8888/annotationResource"; 
		// 
		//"http://shaman.cheshire3.org/services/annotations/";


	/** Default publishing service URL */
	public static String publishServiceURL = 
//		"http://localhost:8888/publishService"; 
		"http://hypatia.cs.ualberta.ca:8888/publishService";
		// 
		//"http://shaman.cheshire3.org/sword/annotations/";

	/** Default search service URL */
	public static String searchServiceURL = 
//		"http://localhost:8888/searchService"; 
		"http://hypatia.cs.ualberta.ca:8888/searchService";
		// 
		//"http://shaman.cheshire3.org/services/annotations";
	
	private static String RESTpublish =
		"http://localhost:8888/publishService"; 
//		"http://hypatia.cs.ualberta.ca:8888/publishService";
	private static String RESTsearch = 
		"http://localhost:8888/searchService"; 
//		"http://hypatia.cs.ualberta.ca:8888/searchService";
	///
	

	/** Default preferences */
	public static String author = "Demo user";

	public static String organisation = "Default Organisation";

	public static String email = "example@email.com";

	public static String userid = "demo";

	public static String pass = "";

	public static boolean sameUrl = true;

	public static boolean sameDigest = true;

	public static boolean sameLexical = false;

	public static boolean sameTxtDigest = true;

	public static boolean trustedParty = false;
	
	///SAM
	public static boolean sameBib = false; //may point to more than one url
	
	public static boolean sameDOI = false; //some bibs may not contain DOI, this may lead to bringing unrelated annos where bibs are similar in title,..
						//but since this a very low risk, even maybe impossible! (if really no two papers have same title) we put this aside for future if feeling the need
	///

	public static boolean showFab4 = true;

	public static boolean showPlay = true;

	public static boolean showVoice = true;

	//public static AnnotationModelSerialiser ams = new JDomAnnotationModelSerialiser();

	/** Default personal keystore file */
	public static final File defksfile = new File(Fab4utils.USER_HOME_DIR,
			DistributedPersonalAnnos.FAB4_KEY_STORE_FILE);
	/** Default trusted keys keystore file */
	public static final File deftrusted = new File(Fab4utils.USER_HOME_DIR,
			DistributedPersonalAnnos.FAB4_TRUSTED_PARTIES_FILE);

	public static final String ITSANOTE = "intsanotespan";

	public static TrustedIdentity myTI;

	static boolean toLoad = true;

	/** personal keystore */
	public static PKCS5SimpleKeyStore ks = new PKCS5SimpleKeyStore(DistributedPersonalAnnos.defksfile);

	public static KeyPair kp = null;

	public static List<TrustedIdentity> trusted;

	private static final boolean deleteall = false;

	/** the local annotation store */
	private static LocalAnnotationServer las;

	LinkedList<Behavior> order = new LinkedList<Behavior>();
	
	///SAM I made ras and was static
	/** the remote one */
	private static AnnotationServerConnectorInterface ras;

	/** where we write to */
	private static AnnotationServerConnectorInterface was;

	public JList theList;

	/** Semaphores handle the async anno loading */
	public Semaphore sem = new Semaphore(10);

	Component[] toDisableIfNotOwn;

	Thread loadThread;

	protected Object[] prevSelected = null;

	Thread pt;

	protected boolean cancelPublish;

	private Thread delThread;

	///SAM made it static
	/**
	 * this variable handles the specific limitations of Cheshire3 annotation
	 * servers
	 */
	private static boolean c3as = false;
	
	///SAM	
	public static Servers currentServer = Servers.RDF_SWORD;
	///

	private long lastCheck;

	private JMenuItem mtopublic;

	private JMenuItem mtoprivate;
	
	///SAM
	private JMenuItem mreply;
	///

	private boolean toSave = true;

	private XMLExport xmlExport;

	private JCheckBoxMenuItem lingmi;

	private boolean linguisticMode;
	private String dbLocation = "annoDb";
	private static Class<AnnotationServerConnectorInterface> remoteAsc;

	///SAM
	private static boolean copyToPub = false;
	//this should have been as an attribute in the Document or DocInfo, but I didn't want to edit multivalent
	private static HashMap<String,Integer> bibForDocument = new HashMap<String,Integer>(); 	//String: document uri, Integer: resourceId
	//those who don't provide bibtex, dont need resourceid, will be found by url
	private static HashMap<String, ArrayList<String>> tagsForDocument = new HashMap<String, ArrayList<String>>();
	///

	/**
	 * 
	 * Saves the distributed annotation preferences from the annotation
	 * preferences dialog. This includes the trusted users store.
	 * 
	 */
	public void savePrefs() {
		Properties props = Fab4.prefs.getProps();
		props.setProperty("author", DistributedPersonalAnnos.author);
		props.setProperty("organisation", DistributedPersonalAnnos.organisation);
		props.setProperty("email", DistributedPersonalAnnos.email);
		if (Fab4.prefs.searchaddress == null)
			props.setProperty("SRULocation",
					DistributedPersonalAnnos.searchServiceURL);
		if (Fab4.prefs.wsserver == null)
			props.setProperty("AnnotationServerLocation",
					DistributedPersonalAnnos.publishServiceURL);
		if (Fab4.prefs.wsuser == null) {
			// System.out.println("n");
			props.setProperty("userid", DistributedPersonalAnnos.userid);
			///SAM
			if(currentServer == Servers.RDF_SWORD) ///
				props.setProperty("pass", DistributedPersonalAnnos.pass);
			///SAM
			else if(currentServer == Servers.REST )
				props.setProperty("pass", ""); //don't save password
			///
		} else if (DistributedPersonalAnnos.pass != null)
			Fab4.prefs.getProps().setProperty(
					DistributedPersonalAnnos.userid + "|" + DistributedPersonalAnnos.publishServiceURL, DistributedPersonalAnnos.pass);
		props.setProperty("sameUrl", Boolean.toString(DistributedPersonalAnnos.sameUrl));
		props.setProperty("sameDigest", Boolean.toString(DistributedPersonalAnnos.sameDigest));
		props.setProperty("sameLexical", Boolean.toString(DistributedPersonalAnnos.sameLexical));
		props.setProperty("sameTxtDigest", Boolean.toString(DistributedPersonalAnnos.sameTxtDigest));
		
		///SAM
		props.setProperty("sameBib", Boolean.toString(DistributedPersonalAnnos.sameBib));
		///
		
		props.setProperty("showFab4", Boolean.toString(DistributedPersonalAnnos.showFab4));
		props.setProperty("showPlay", Boolean.toString(DistributedPersonalAnnos.showPlay));
		props.setProperty("showVoice", Boolean.toString(DistributedPersonalAnnos.showVoice));
		props.setProperty("trustedParty", Boolean.toString(DistributedPersonalAnnos.trustedParty));
		props.setProperty("isPublic", Boolean.toString(PersonalAnnos.useRemoteServer));
		props.setProperty("linguisticMode", Boolean.toString(linguisticMode));

		try {
			if (DistributedPersonalAnnos.trusted != null)
				TrustedIdentity.writeStore(DistributedPersonalAnnos.deftrusted, DistributedPersonalAnnos.trusted);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// getAs();

	}

	/**
	 * Deletes an annotation from the server
	 * 
	 * @param fa
	 */
	void deleteAnno(final FabAnnotation fa) {
		final JDialog splash = showSplash("Deleting annotation");
		delThread = new Thread(new Runnable() {
			public void run() {
				try {
					setWasToLocalOrRemote();
					int r = 0;
					if (c3as)
						r = was.deleteAnnotation(
								fa.ann.getId(), DistributedPersonalAnnos.userid, fa
								.getStringAnno());
					///SAM
					else if(currentServer == Servers.RDF_SWORD ){
						///
						r = was.deleteAnnotation(
								fa.ann.getId(), DistributedPersonalAnnos.userid, DistributedPersonalAnnos.pass);
						///SAM
					}
					else if(currentServer == Servers.REST){
						
						boolean auth = true; 
						if(useRemoteServer)	
							auth = authenticate();
						if(auth){
													
							/*if(was == null) //TODO , I don't know why it is null when setWas was called just before..
								setWasToLocalOrRemote();*/
							
							if( fa.ann.getUniqueId() != null )
								r = was.deleteAnnotation(
										String.valueOf(fa.ann.getUniqueId()), DistributedPersonalAnnos.userid, DistributedPersonalAnnos.pass);
							
							else 
							///  //was only this line originally
								r = was.deleteAnnotation(
									fa.ann.getId(), DistributedPersonalAnnos.userid, DistributedPersonalAnnos.pass);
						}
						else
							r = 1;
						
					}
					
					///SAM
					if( r == 1 ){
						JOptionPane
						.showMessageDialog(
								getBrowser(),
								"User is not authenticated. Operation canceled",
								"Try again...",
								JOptionPane.INFORMATION_MESSAGE);
					}
					else if( r == 2 ){
						JOptionPane
						.showMessageDialog(
								getBrowser(),
								"Sorry, the annotation server is not responding. Please try later.",
								"Try later...",
								JOptionPane.INFORMATION_MESSAGE);
					}
					else if( r == 3 ){
						JOptionPane
						.showMessageDialog(
								getBrowser(),
								"You are not allowed to delete an annotation that has a reply on it",
								"Not allowed...",
								JOptionPane.INFORMATION_MESSAGE);
					}
					///					
					
					int k = 0;
					while (k < 2 && PersonalAnnos.useRemoteServer) {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
						}
						// I guess I should query using IDSearch and see if it
						// is still there...
						// a = man.query_job_status(r);
						if (was.IDSearch(fa.getAnn().getId()) != null)
							break;
						k++;
					}
					if( r == 0 ){ ///SAM added if(r == 0)
					if (!SwingUtilities.isEventDispatchThread())
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								updateDeleted(fa);
							}
						});
					else
						updateDeleted(fa);
					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					hideSplash(splash);
				}
			}

		});
		delThread.start();
	}

	/** sets the was to be the right annotation server (local or remote) */
	private void setWasToLocalOrRemote() {
		createLocalAs();
		if (DistributedPersonalAnnos.remoteAsc == null){
			///SAM
			toLoad = true;
			loadSettings();
			if(DistributedPersonalAnnos.remoteAsc == null){
			///
				Fab4.getMVFrame(getBrowser()).annoPanels.get(getBrowser()).annotationPaneLabel
				.setSelectedIndex(1);
				Fab4.getMVFrame(getBrowser()).annoPanels.get(getBrowser()).annotationPaneLabel.setEnabled(false);
				PersonalAnnos.useRemoteServer= false;				
			}
		}
		if (PersonalAnnos.useRemoteServer && DistributedPersonalAnnos.remoteAsc != null) {
			getRemoteAs();
			///SAM: getremote may fail, so before setting was we should again check:
			if(PersonalAnnos.useRemoteServer )
				///
				was = ras;
			///SAM
			else
				was = DistributedPersonalAnnos.las;
			///
		} else
			was = DistributedPersonalAnnos.las;
	}

	/**
	 * Connects to the remote annotation server and sets it as ras. this is
	 * taking care of error situations and previously set ras
	 */
	private void getRemoteAs() {
		if (DistributedPersonalAnnos.PrevAnnotationServerLocation.equals(DistributedPersonalAnnos.publishServiceURL))
			if (ras != null)
				return;
			else {
				if (System.currentTimeMillis() - lastCheck < 10000) {
					final Browser br = getBrowser();
					PersonalAnnos.useRemoteServer = false;
					Fab4.getMVFrame(br).annoPanels.get(br).annotationPaneLabel
					.setSelectedIndex(1);
					reloadDocument();
					return;
				}
				lastCheck = System.currentTimeMillis();
			}

		getRemote();

	}

	/** internal method */
	private void getRemote() {
		try {
			ras = generateRemote();
			DistributedPersonalAnnos.PrevAnnotationServerLocation = DistributedPersonalAnnos.publishServiceURL;
			if (ras.getRepositoryType().indexOf("cheshire3") >= 0)
				c3as = true;
			else
				c3as = false;
			reloadDocument();
		} catch (Exception e) {
			e.printStackTrace();
			ras = null;
			final Browser br = getBrowser();
			PersonalAnnos.useRemoteServer = false;
			JOptionPane
			.showMessageDialog(
					getBrowser(),
					"Swiching to private annotations, the server is not responding!",
					"Warning", JOptionPane.WARNING_MESSAGE);
			Fab4.getMVFrame(br).annoPanels.get(br).annotationPaneLabel
			.setSelectedIndex(1);
			reloadDocument();
		}
	}

	///SAM I made this static
	private static AnnotationServerConnectorInterface generateRemote() throws InstantiationException, IllegalAccessException {
		AnnotationServerConnectorInterface c = DistributedPersonalAnnos.remoteAsc.newInstance();
		c.init(DistributedPersonalAnnos.publishServiceURL, DistributedPersonalAnnos.searchServiceURL);
		return c;
	}

	/** creates the local annotation server ans sets in as las */
	private LocalAnnotationServer createLocalAs() {
		if (DistributedPersonalAnnos.las == null) {
			Browser br = getBrowser();
			Fab4 f = Fab4.getMVFrame(br);
			if (f == null)
				return null;
			if (DistributedPersonalAnnos.remoteAsc != null){
				System.out.println("remote");
				try {
					ras = generateRemote();
					dbLocation = ras.getDbLocation();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			DistributedPersonalAnnos.las = new LocalAnnotationServer(new File(new File(System
					.getProperty("user.home"), ".Multivalent"), dbLocation ));
			///SAM
			if (DistributedPersonalAnnos.remoteAsc == null){
				toLoad = true;
				loadSettings();
			}
			///
			if (DistributedPersonalAnnos.remoteAsc != null)
				DistributedPersonalAnnos.las.setDefaultAMS(ras.getDefaultAMS());
			AnnotationSidePanel asp = f.annoPanels.get(br);
			asp.annotationPaneLabel.setSelectedIndex(PersonalAnnos.useRemoteServer ? 0 : 1);
		}
		return DistributedPersonalAnnos.las;
	}

	/** switches to distributed annotations (was = ras) */
	public void goDistributed() {
		System.out.println("Switching to public annotations");
		PersonalAnnos.useRemoteServer = true;
		getRemoteAs();
		createLocalAs();
		was = ras;
		reloadDocument();
	}

	/** swithces to local annotations (was = las) */
	public void goLocal() {
		System.out.println("Switching to private annotations");
		PersonalAnnos.useRemoteServer = false;
		createLocalAs();
		was = DistributedPersonalAnnos.las;
		reloadDocument();
	}

	/**
	 * This method takes care of modifying Fab4's interface in order to add the
	 * Distributed annotation features (menues, buttons etc.)
	 * 
	 * It is a rather long method (a better way should be implemented on the
	 * next release).
	 */
	static boolean once = !true;
	public void addUIElementsToFab4List(final JList l, final Component[] b,
			final JComboBox cb) {

		final Fab4 ff = Fab4.getMVFrame(getBrowser());
		if (!once) {
			once = true;
			ff.mtools.addSeparator();
			xmlExport = new XMLExport(this);
			JMenuItem mi2 = new JMenuItem(
			"1)Export annotations to XML (multiple tags)");
			mi2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					xmlExport.exportToXml(1);
				}
			});
			ff.mtools.add(mi2);
			JMenuItem mi3 = new JMenuItem(
			"2)Export annotations to XML (multiple tags / overlapping)");
			mi3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					xmlExport.exportToXml(3);
				}
			});

			ff.mtools.add(mi3);

			JMenu em = new JMenu("Export...");
			JMenu im = new JMenu("Import...");
			JMenu dm = new JMenu("Delete...");
			JMenuItem ila = new JMenuItem("Private annotation database");
			JMenuItem ipr = new JMenuItem("Preferences and identity");
			JMenuItem ipr2 = new JMenuItem("Preferences");
			JMenuItem epa = new JMenuItem("Private annotations on this page");
			JMenuItem epa2 = new JMenuItem("Selected annotations on this page");
			JMenuItem ela = new JMenuItem("Private annotation database");
			JMenuItem epr = new JMenuItem("Preferences and identity");
			JMenuItem epr2 = new JMenuItem("Preferences");
			JMenuItem dla = new JMenuItem("Private annotation database");
			em.add(epa);
			em.add(epa2);
			em.add(ela);
			em.add(epr);
			em.add(epr2);

			im.add(ila);
			im.add(ipr);
			im.add(ipr2);

			dm.add(dla);

			ff.mtools.add(em);
			ff.mtools.add(im);
			ff.mtools.add(dm);

			lingmi = new JCheckBoxMenuItem("Linguistic annotation mode",
					linguisticMode);
			lingmi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					linguisticMode = lingmi.isSelected();
					if (!linguisticMode)
						l.setCellRenderer(new FabAnnoListRenderer());
					else
						l.setCellRenderer(new LinguisticAnnoListRenderer());
					reloadDocument();
				}
			});
			ff.mtools.add(lingmi);

			dla.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteLocalDatabase();
				}
			});

			ila.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						importLocalNotes();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
			ipr.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						importLocalPrefAndId();
					} catch (InvalidPropertiesFormatException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
			ipr2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						importLocalPref(false);
					} catch (InvalidPropertiesFormatException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});

			epa.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					exportPageNotes(false);

				}
			});
			epa2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					exportPageNotes(true);

				}

			});
			ela.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					exportLocalNotes();
				}
			});
			epr.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					exportPreferences(true);
				}
			});
			epr2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					exportPreferences(false);
				}

			});
		}
		if (!linguisticMode)
			l.setCellRenderer(new FabAnnoListRenderer());
		else
			l.setCellRenderer(new LinguisticAnnoListRenderer());

		theList = l;
		toDisableIfNotOwn = b;
		final JPopupMenu poppa = new JPopupMenu();
		JMenuItem mprop = new JMenuItem("Properties", new ImageIcon(getClass()
				.getResource("/res/report.png")));
		mprop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FabAnnotation fa = (FabAnnotation) l.getSelectedValue();
				AnnotationProperties ap = new AnnotationProperties(ff, false,
						fa);
				ap.setVisible(true);
			}
		});
		poppa.add(mprop);
		final JMenuItem mdele = new JMenuItem("Delete", new ImageIcon(
				getClass().getResource("/res/note_delete.gif")));
		mdele.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] m = l.getSelectedValues();
				boolean allMine = areAllSelectedValuesOwn(m);
				
				///SAM
				if(currentServer == Servers.RDF_SWORD ){ ///
					if (DistributedPersonalAnnos.deleteall || allMine || !PersonalAnnos.useRemoteServer)
						for (Object o : m) {
							FabAnnotation fa = (FabAnnotation) o;
							getBrowser().eventq(PersonalAnnos.MSG_DELETE, fa);
						}
				///SAM TODO I don't remember why I did this!
				}
				else if(currentServer == Servers.REST ){
					if (DistributedPersonalAnnos.deleteall || allMine )
						for (Object o : m) {
							FabAnnotation fa = (FabAnnotation) o;
							//TODO if( fa.ann.hasReply() )
							getBrowser().eventq(PersonalAnnos.MSG_DELETE, fa);
						}
				}
				///
			}
		});
		mdele.setToolTipText("Delete selected notes");
		poppa.add(mdele);

		mtopublic = new JMenuItem("Copy to public", FabIcons.getIcons().ICOPUB);
		mtopublic.addActionListener(new ActionListener() {
			// TODO!!!
			public void actionPerformed(ActionEvent e) {
				copyToServer();
			}
		});

		mtoprivate = new JMenuItem("Copy to private",
				FabIcons.getIcons().ICOPUB);
		mtoprivate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				copyToClient();
			}
		});

		///SAM:
		mreply = new JMenuItem("Reply to this comment",
				FabIcons.getIcons().ICOPUB);
		mreply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				replyToAnno();
			}
		});
		
		if(currentServer != Servers.REST)
			mreply.setEnabled(false);
		///
		
		mtopublic
		.setToolTipText("Publishes the selected notes to the central server. Only own notes can be copied.");
		poppa.add(mtopublic);
		mtoprivate.setToolTipText("Makes a local copy of the selected notes. ");
		poppa.add(mtoprivate);
		
		///SAM
		mreply.setToolTipText("Opens a new note on which you can post your reply on selected annotation");
		poppa.add(mreply);
		///
		
		cb.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String s = (String) cb.getSelectedItem();
				if (s.equals("Date"))
					user.setComp(new FabAnnoModificationDateComparator(false));
				else if (s.equals("Author"))
					user.setComp(new FabAnnoAuthorComparator(true));
				else if (s.equals("Trust"))
					user.setComp(new FabAnnoTrustComparator(false));
				else if (s.equals("Position"))
					user.setComp(new FabAnnoPositionComparator(false));

			}

		});

		poppa.pack();
		l.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int i = l.locationToIndex(e.getPoint());
					int[] k = l.getSelectedIndices();
					boolean b = true;
					for (int m : k)
						if (i == m) {
							b = false;
							break;
						}
					if (b)
						l.setSelectedIndex(i);
					poppa.show(e.getComponent(), e.getX(), e.getY());

				}
			}

			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int i = l.locationToIndex(e.getPoint());
					l.setSelectedIndex(i);
					poppa.show(e.getComponent(), e.getX(), e.getY());
				}
			}

		});

		l.addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				Object[] index = l.getSelectedValues();
				FabAnnotation ff;
				Behavior bb;
				if (prevSelected != null)
					for (Object o : prevSelected) {
						ff = (FabAnnotation) o;
						bb = ff.getBehaviour();
						if (bb instanceof FabNote) {
							FabNote aa = (FabNote) bb;
							if (aa.getTitleBg() != null)
								aa.setTitleBg(aa.getTitleBg().brighter());
						} else if (bb instanceof Span) {
							Fab4LabelSpan aa;
							if (bb instanceof BackgroundSpan) {
								BackgroundSpan bs = (BackgroundSpan) bb;
								bs.setSelected(false);
								bs.markDirty();
							}
							if (bb instanceof TextSpanNote) {
								TextSpanNote ts = (TextSpanNote) bb;
								hideNoteContent(ts);
							}
							aa = (Fab4LabelSpan) bb.getValue(DistributedPersonalAnnos.ITSANOTE);
							if (aa != null) {
								aa.setColor(aa.defColor);
								aa.setTextColor(aa.defTextColor);
							}

						}
					}
				boolean allMine = areAllSelectedValuesOwn(index);
				prevSelected = index;

				// setEnableOwnComponents(allMine || deleteall);
				mdele.setEnabled(allMine || DistributedPersonalAnnos.deleteall);

				setEnableOwnComponents(allMine || DistributedPersonalAnnos.deleteall);
				getBrowser().repaint(500);
			}

		});

		l.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				FabAnnotation ff;
				Behavior bb;

				if (e.getClickCount() == 1
						&& e.getButton() == MouseEvent.BUTTON1) {
					int i = l.locationToIndex(e.getPoint());
					if (i >= 0) {
						ff = (FabAnnotation) user.get(i);
						if (ff.getSigner() != null)
							if (ff.getSigner().isItsme())
								setEnableOwnComponents(true);
							else
								setEnableOwnComponents(false);
						bb = ff.getBehaviour();

						if (bb instanceof FabNote) {
							FabNote aa = (FabNote) bb;
							if (aa.win_ != null) {
								aa.setLampshade(false);
								aa.show(false);
								aa.fitIntoPage();
								int x = aa.win_.bbox.x;
								int y = aa.win_.bbox.y;
								scrollToIfNeeded(aa.win_, x, y);
							}
						}
						if (bb instanceof Span) {
							Span aa = (Span) bb;
							if (aa.getStart() != null) {
								Leaf leaf = aa.getStart().leaf;
								if (leaf != null) {
									Document doc = (Document) getBrowser()
									.getRoot().findBFS("content");
									Point p = leaf.getRelLocation(doc);
									scrollToIfNeeded(leaf, p.x, p.y);
								}
							}
						}
					}
				} else {
					// l.clearSelection();
				}
				if (e.getClickCount() == 2
						&& e.getButton() == MouseEvent.BUTTON1) {
					int i = l.locationToIndex(e.getPoint());
					if (i >= 0) {
						ff = (FabAnnotation) user.get(i);
						int apn = 0;
						apn = ff.getAnn().getPageNumber();
						if (apn != 0 && ff.totPages > 0)
							getBrowser().eventq(Multipage.MSG_GOPAGE,
									new Integer(apn));
					}
				}

			}
		}
		);

		// super.handleList(l);
	}

	/**
	 * Exports the notes on a specific document to a ZIP file, containing a
	 * Lucene database.
	 * 
	 * @param selected
	 */

	protected void exportPageNotes(boolean selected) {
		Object[] m;
		JList l = theList;
		if (selected)
			m = l.getSelectedValues();
		else
			m = user.getArray();
		if (m.length == 0) {
			System.out.println("Nothing to save");
			return;
		}
		if (DistributedPersonalAnnos.jc_ == null) {
			DistributedPersonalAnnos.jc_ = new JFileChooser();
			DistributedPersonalAnnos.jc_.setCurrentDirectory(Fab4.getCurrentDir());
		}
		DistributedPersonalAnnos.jc_.setDialogType(JFileChooser.SAVE_DIALOG);
		DistributedPersonalAnnos.jc_.setDialogTitle("Chose the backup file");
		File newpath = new File(DistributedPersonalAnnos.jc_.getCurrentDirectory(), "Annotation "
				+ (selected ? "selection" : "page") + " Backup "
				+ getDateForFile() + ".zip");
		DistributedPersonalAnnos.jc_.setSelectedFile(newpath);
		if (DistributedPersonalAnnos.jc_.showSaveDialog(getBrowser()) == JFileChooser.APPROVE_OPTION) {
			File dest = DistributedPersonalAnnos.jc_.getSelectedFile();
			try {
				// create a temp index
				File temp = File.createTempFile("index", "aaa");
				temp.delete();
				temp.mkdir();
				temp.deleteOnExit();
				LocalAnnotationServer ll = new LocalAnnotationServer(temp);
				LocalLuceneConnector llc = ll.delegate;

				// add the notes
				Behavior[] theNotes = new Behavior[m.length];
				for (int i = 0; i < m.length; i++) {
					FabAnnotation fa = (FabAnnotation) m[i];
					theNotes[i] = fa.getBehaviour();
				}
				publishAnnotations(false, ll, theNotes);
				llc.closeIndexes();
				// compress and delete tem index
				File dir = llc.getIndexDirectory();
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
						dest));
				Fab4utils.zipDir(dir, zos, false);
				zos.close();
				llc.deleteIndexDiretory();
				llc = null;
				temp.delete();
				System.gc();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	/** deletes the local annotation database */
	protected void deleteLocalDatabase() {
		int i = JOptionPane
		.showConfirmDialog(
				getBrowser(),
				"WARNING! Your local annotation database will be deleted, are you sure?",
				"Delete local annotations", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (i != JOptionPane.YES_OPTION)
			return;
		DistributedPersonalAnnos.las.delegate.deleteIndexDiretory();
		DistributedPersonalAnnos.las.delegate.reopenIndexes();
		reloadDocument();

	}

	private File choseOpenFile() {
		if (DistributedPersonalAnnos.jc_ == null) {
			DistributedPersonalAnnos.jc_ = new JFileChooser();
			DistributedPersonalAnnos.jc_.setCurrentDirectory(Fab4.getCurrentDir());
		}
		DistributedPersonalAnnos.jc_.setDialogType(JFileChooser.OPEN_DIALOG);
		DistributedPersonalAnnos.jc_.setDialogTitle("Chose the backup file");
		File newpath = DistributedPersonalAnnos.jc_.getCurrentDirectory();
		DistributedPersonalAnnos.jc_.setSelectedFile(newpath);
		if (DistributedPersonalAnnos.jc_.showOpenDialog(getBrowser()) == JFileChooser.APPROVE_OPTION)
			return DistributedPersonalAnnos.jc_.getSelectedFile();
		return null;
	}

	/**
	 * Import preferences and Identity (optional)
	 * 
	 * @param loadId
	 *            has to load identity too?
	 * @throws InvalidPropertiesFormatException
	 * @throws IOException
	 */
	protected void importLocalPref(boolean loadId)
	throws InvalidPropertiesFormatException, IOException {
		File ret = choseOpenFile();
		if (ret == null)
			return;
		int i = JOptionPane.showConfirmDialog(getBrowser(), "Your preferences"
				+ (loadId ? " and identity" : "")
				+ " will be replaced, are you sure?", "Replace preferences",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (i != JOptionPane.YES_OPTION)
			return;
		// FileInputStream fis = new FileInputStream(ret);
		ZipFile zf = new ZipFile(ret);
		ZipEntry e = zf.getEntry(FabPreferences.FAB4_ANNO_PREFS);
		if (e == null) {
			System.out.println("no preferences in zip file");
			return;
		}
		BufferedInputStream is = new BufferedInputStream(zf.getInputStream(e));
		Properties p = new Properties();
		p.loadFromXML(is);
		Set enu = p.keySet();
		for (Object o : enu) {
			String k = ((String) o).toLowerCase();
			if ((k.equals("pass") || k.equals("userid")
					|| k.equals("organisation") || k.equals("author") || k
					.equals("pass"))
					&& !loadId)
				continue;
			// ZipInputStream zin = new ZipInputStream(new
			// BufferedInputStream(fis));
			// System.out.println(k+" = " +p.get(o));
			Fab4.prefs.getProps().put(o, p.get(o));
		}
		loadPrefs();
		if (loadId) {
			e = zf.getEntry(DistributedPersonalAnnos.FAB4_KEY_STORE_FILE);
			if (e == null) {
				System.out.println("no id in zip file");
				return;
			}
			is = new BufferedInputStream(zf.getInputStream(e));
			Fab4utils.copyToFile(is, DistributedPersonalAnnos.defksfile, true);
			e = zf.getEntry(DistributedPersonalAnnos.FAB4_TRUSTED_PARTIES_FILE);
			if (e == null)
				System.out.println("no trusted parties");
			else {
				is = new BufferedInputStream(zf.getInputStream(e));
				Fab4utils.copyToFile(is, DistributedPersonalAnnos.deftrusted, true);
			}
			DistributedPersonalAnnos.kp = null;
			DistributedPersonalAnnos.trusted = null;
			loadSettings();
			reloadDocument();
		}

	}

	/** convenience method to reload a document */
	private void reloadDocument() {
		
		///SAM: before reloading, check if going to public, so authenticate
		if(currentServer == Servers.REST ){
			boolean auth = true;
			if( PersonalAnnos.useRemoteServer )
				auth = authenticate(); //if authentication failed (or cancelled): go back to local
					
			
			if(!auth)
				return;
		}
		///
		
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		
		///SAM: before reloading...
		if(currentServer == Servers.REST )
			askForDocumentInfo(doc.uri.toString());
		///
		
		getBrowser().event(new SemanticEvent(this, Document.MSG_RELOAD, doc));
	}

	protected void importLocalPrefAndId()
	throws InvalidPropertiesFormatException, IOException {
		importLocalPref(true);
	}

	/** import notes from a annotation database export */
	protected void importLocalNotes() throws IOException {
		int i = JOptionPane
		.showConfirmDialog(
				getBrowser(),
				"Do you want to create a backup of your current database before merging?",
				"Merge annotations", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (i == JOptionPane.CANCEL_OPTION)
			return;
		if (i == JOptionPane.YES_OPTION) {
			File destinationFile = new File(Fab4.getCurrentDir(),
					"Annotation Database Backup " + getDateForFile());
			try {
				File dir = DistributedPersonalAnnos.las.delegate.getIndexDirectory();
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
						destinationFile));
				DistributedPersonalAnnos.las.delegate.closeIndexes();
				Fab4utils.zipDir(dir, zos, false);
				zos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			DistributedPersonalAnnos.las.delegate.reopenIndexes();
		}
		File temp = File.createTempFile("index", "aaa");
		temp.delete();
		temp.mkdir();
		temp.deleteOnExit();
		if (DistributedPersonalAnnos.jc_ == null) {
			DistributedPersonalAnnos.jc_ = new JFileChooser();
			DistributedPersonalAnnos.jc_.setCurrentDirectory(Fab4.getCurrentDir());
		}
		DistributedPersonalAnnos.jc_.setDialogType(JFileChooser.OPEN_DIALOG);
		DistributedPersonalAnnos.jc_.setDialogTitle("Chose the backup file");
		if (DistributedPersonalAnnos.jc_.showOpenDialog(getBrowser()) == JFileChooser.APPROVE_OPTION) {
			File dest = DistributedPersonalAnnos.jc_.getSelectedFile();
			Fab4utils.unzipFile(dest, temp);
			mergeLocalDatabaseWith(temp);
		}
		reloadDocument();

	}

	private void mergeLocalDatabaseWith(File temp) {
		DistributedPersonalAnnos.las.delegate.setUseCompoundFile(true);
		LocalLuceneConnector llc = new LocalLuceneConnector(temp
				, true);
		Hits res = llc.general_search(LocalLuceneConnector.IDX_APPLICATION,
		"multivalent/fab4");
		for (int i = 0; i < res.length(); i++)
			try {
				org.apache.lucene.document.Document d = res.doc(i);
				String id = d.get(llc.IDX_ANNO_ID);
				String ret = DistributedPersonalAnnos.las.IDSearch(id);
				if (i % 100 == 0)
					DistributedPersonalAnnos.las.delegate.optimize();
				if (ret == null)
					DistributedPersonalAnnos.las.postAnnotation(d.get(llc.IDX_CONTENT), null, null);
				else
					DistributedPersonalAnnos.las.updateAnnotation(d.get(llc.IDX_CONTENT), null, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			llc.deleteIndexDiretory();
			llc = null;
			temp.delete();
			System.gc();

	}

	static public JFileChooser jc_ = null; // static OK because modal means can
	// only have one at a time

	/** exports all the local notes */

	protected void exportLocalNotes() {

		if (DistributedPersonalAnnos.jc_ == null) {
			DistributedPersonalAnnos.jc_ = new JFileChooser();
			DistributedPersonalAnnos.jc_.setCurrentDirectory(Fab4.getCurrentDir());
		}
		DistributedPersonalAnnos.jc_.setDialogType(JFileChooser.SAVE_DIALOG);
		DistributedPersonalAnnos.jc_.setDialogTitle("Chose the backup file");
		File newpath = new File(DistributedPersonalAnnos.jc_.getCurrentDirectory(),
				"Annotation Database Backup " + getDateForFile() + ".zip");
		DistributedPersonalAnnos.jc_.setSelectedFile(newpath);
		if (DistributedPersonalAnnos.jc_.showSaveDialog(getBrowser()) == JFileChooser.APPROVE_OPTION) {
			File dest = DistributedPersonalAnnos.jc_.getSelectedFile();
			try {
				File dir = DistributedPersonalAnnos.las.delegate.getIndexDirectory();
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
						dest));
				DistributedPersonalAnnos.las.delegate.closeIndexes();
				Fab4utils.zipDir(dir, zos, false);
				zos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			DistributedPersonalAnnos.las.delegate.reopenIndexes();
		}
	}

	private static String getDateForFile() {
		String s = Fab4utils.DATEFTIME_SHORT.format(new Date());
		s = s.replaceAll("/", "-");
		s = s.replaceAll(":", "-");
		return s;
	}

	/** export local preferences */
	private void exportPreferences(boolean id) {
		File[] a;
		Fab4.getMVFrame(getBrowser()).prefs.savePrefs();
		if (id) {
			a = new File[3];
			a[0] = DistributedPersonalAnnos.defksfile;
			a[1] = DistributedPersonalAnnos.deftrusted;
			a[2] = FabPreferences.defprofile;
		} else {
			a = new File[1];
			a[0] = FabPreferences.defprofile;
		}
		exportFiles(a);
	}

	protected void exportFiles(File[] dir) {

		if (DistributedPersonalAnnos.jc_ == null) {
			DistributedPersonalAnnos.jc_ = new JFileChooser();
			DistributedPersonalAnnos.jc_.setCurrentDirectory(Fab4.getCurrentDir());
		}
		DistributedPersonalAnnos.jc_.setDialogType(JFileChooser.SAVE_DIALOG);
		DistributedPersonalAnnos.jc_.setDialogTitle("Chose the backup file");
		File newpath = new File(DistributedPersonalAnnos.jc_.getCurrentDirectory(), "Annotation Backup "
				+ getDateForFile() + ".zip");
		DistributedPersonalAnnos.jc_.setSelectedFile(newpath);
		if (DistributedPersonalAnnos.jc_.showSaveDialog(getBrowser()) == JFileChooser.APPROVE_OPTION) {
			DistributedPersonalAnnos.las.delegate.closeIndexes();
			File dest = DistributedPersonalAnnos.jc_.getSelectedFile();
			try {
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
						dest));
				Fab4utils.zipFiles(dir, zos);
				zos.close();
			} catch (Exception e) {
			}
			DistributedPersonalAnnos.las.delegate.reopenIndexes();
		}
	}

	/**
	 * enables / disables different buttons depending on being in local or
	 * remote mode
	 * 
	 * @param t
	 */
	private void setEnableOwnComponents(boolean t) {

		for (Component b : toDisableIfNotOwn)
			b.setEnabled(t || !PersonalAnnos.useRemoteServer);

		if (PersonalAnnos.useRemoteServer) {
			mtopublic.setEnabled(false);
			mtoprivate.setEnabled(true);
		} else {
			mtopublic.setEnabled(true);
			mtoprivate.setEnabled(false);
		}
		if (t && !PersonalAnnos.useRemoteServer)
			mtopublic.setEnabled(true);
		else
			mtopublic.setEnabled(false);
		///SAM
		if(currentServer == Servers.REST )
			mreply.setEnabled(true);
		else
			mreply.setEnabled(false);
		///
	}

	protected void hideNoteContent(TextSpanNote ts) {
		ts.hideNoteContents();
	}

	/**
	 * hides the progress splash screen
	 * 
	 * @param br
	 * @param splash
	 */
	void hideSplash(final JDialog splash) {
		if (splash == null)
			return;
		if (!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					splash.dispose();
					Fab4.getMVFrame(getBrowser()).setEnabled(true);
					Fab4.getMVFrame(getBrowser()).toFront();
					pt = null;
				}
			});
		else {
			splash.dispose();
			Fab4.getMVFrame(getBrowser()).setEnabled(true);
			pt = null;
		}
	}

	public boolean isLocal() {
		return !PersonalAnnos.useRemoteServer;
	}

	public static boolean isRemote() {
		return PersonalAnnos.useRemoteServer;
	}

	/** searches for annotations containing the specified text */
	private void searchAnnos(final String text) {

		final Browser br = getBrowser();

		if (Fab4.getMVFrame(br) != null)
			Fab4.getMVFrame(br).setStatus("searching annotations...");

		final HashMap<String, FabAnnotation> hm = new HashMap<String, FabAnnotation>();
		user.clear();

		Thread searchTherad = new Thread(new Runnable() {
			public void run() {
				//System.out.println(text);
				List<FabAnnotation> annoList;
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setWasToLocalOrRemote();
				String[] slist = null;
				if (c3as)
					slist = was.genericSearch("dc.description = " + text,
					"SRW");
				else
					slist = was.genericSearch(text, "");

				//} catch (RemoteException e) {
				//e.printStackTrace();
				//	}
				annoList = valdateAndCrateList(slist);
				for (FabAnnotation ad : annoList)
					hm.put(ad.getAnn().getId(), ad);
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							for (FabAnnotation fa : hm.values()) {
								if (!canRead(fa))
									continue;
								user.add(fa);
							}
							Fab4.getMVFrame(br).updateAnnoIcon();
							Fab4.getMVFrame(br).setStatus("");
						}


					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		});
		sem.acquireUninterruptibly();
		searchTherad.start();
	}
	private boolean canRead(FabAnnotation fa) {
		String app = fa.getAnn().getApplication();
		if (ras == null)
			return app.equalsIgnoreCase(DistributedPersonalAnnos.las.getDefaultAMS().getApplicationName()) ;
		return app.equalsIgnoreCase(DistributedPersonalAnnos.las.getDefaultAMS().getApplicationName()) || app.equalsIgnoreCase(ras.getDefaultAMS().getApplicationName()) ;
	}
	/**
	 * loads annotations for the given document
	 * 
	 * @param mvDocument
	 * @param mod
	 */
	private void loadAnnotations(final Document mvDocument) {
		prevSelected = null;
		final boolean mod = false;
		final String uri = mvDocument.getURI().toString();
		final Browser br = getBrowser();
		final URI urio = mvDocument.getURI();
		Fab4 f = Fab4.getMVFrame(br);
		if (f != null) {
			f.setStatus("Loading public annotations...");
			AnnotationSidePanel asp = f.annoPanels.get(br);
			asp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}

		final HashMap<String, FabAnnotation> hm = new HashMap<String, FabAnnotation>();

		user.clear();
		Layer personal = mvDocument.getLayer(Layer.PERSONAL);
		personal.clearBehaviors();

		loadThread = new Thread(new Runnable() {
			public void run() {
				List<FabAnnotation> annoList;
				try {
					setWasToLocalOrRemote();
					if (was == ras)
						Thread.sleep(600);
					else
						Thread.sleep(400);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				if (loadThread != Thread.currentThread()) {
					sem.release();
					return;
				}

				final String txt = Fab4utils.getTextNonSpaced(mvDocument);

				final byte[] digest = MediaLoader.MD5Cache.get(urio);
				String stxt;
				if (txt.trim().length() == 0)
					stxt = uri.toString();
				else
					stxt = txt;
				byte[] digestTxt = null;
				try {
					MessageDigest md = MessageDigest.getInstance("MD5");
					md.update(stxt.getBytes());
					digestTxt = md.digest(); // 128 bit or 16 bytes
				} catch (NoSuchAlgorithmException e1) {
				}

				try {
					setWasToLocalOrRemote();
					if (loadThread != Thread.currentThread()) {
						sem.release();
						return;
					}
					if (DistributedPersonalAnnos.sameUrl ) {
						String[] slist = was.URISearch(uri);
						annoList = valdateAndCrateList(slist);
						for (FabAnnotation ad : annoList) {
							hm.put(ad.getAnn().getId(), ad);
							ad.setSameUrl(true);
						}
					}
					if (loadThread != Thread.currentThread()) {
						sem.release();
						return;
					}
					if (DistributedPersonalAnnos.sameDigest)
						if (digest != null) {
							setWasToLocalOrRemote();
							String[] slist = was.checksumSearch(base64
									.toString(digest), "MD5", "base64");
							annoList = valdateAndCrateList(slist);
							for (FabAnnotation ad : annoList) {
								FabAnnotation cd = hm.get(ad.getAnn().getId());
								if (cd == null) {
									ad.setSameCRC(true);
									hm.put(ad.getAnn().getId(),ad);
								} else
									cd.setSameCRC(true);
							}
						}
					if (loadThread != Thread.currentThread()) {
						sem.release();
						return;
					}
					if (DistributedPersonalAnnos.sameTxtDigest) {
						setWasToLocalOrRemote();
						String[] slist = was.customIdSearch(base64
								.toString(digestTxt), "multivalent annotation",
						"base64-MD5-text");

						annoList = valdateAndCrateList(slist);
						for (FabAnnotation ad : annoList) {
							FabAnnotation cd = hm.get(ad.getAnn().getId());

							if (cd == null) {
								ad.setSameTxtCRC(true);
								hm.put(ad.getAnn().getId(), ad);
							} else
								cd.setSameTxtCRC(true);
						}
					}
					if (loadThread != Thread.currentThread()) {
						sem.release();
						return;
					}
					if (DistributedPersonalAnnos.sameLexical) {
						//ComputeLexical cl = new ComputeLexicalServiceLocator()
						//		.getComputeLexical(new URL(lswsAddress));
						String txt2 = getText(mvDocument);
						String ls = "";//cl.computeLexicalSignature(txt2);
						if (!ls.trim().equals("")) {
							// query = "anno.lexicalSignature = \"" + ls + "\"";
							setWasToLocalOrRemote();
							String[] slist = was.lexicalSignatureSearch(ls);
							annoList = valdateAndCrateList(slist);
							for (FabAnnotation ad : annoList) {
								FabAnnotation cd = hm.get(ad.getAnn().getId());
								if (cd == null) {
									ad.setSameLs(true);
									hm.put(ad.getAnn().getId(),ad);
								} else
									cd.setSameLs(true);

							}
						}
					}
					if (loadThread != Thread.currentThread()) {
						sem.release();
						return;
					}
					
					///SAM
					if(currentServer == Servers.REST ){
						if (DistributedPersonalAnnos.sameBib) {
							setWasToLocalOrRemote();
							String[] slist = was.bibtexSearch(uri);
	
							annoList = valdateAndCrateList(slist);
							for (FabAnnotation ad : annoList) {
								FabAnnotation cd = hm.get(ad.getAnn().getId());
	
								if (cd == null) {
									ad.setSameBib(true);
									hm.put(ad.getAnn().getId(), ad);
								} else
									cd.setSameBib(true);
							}
						}
						if (loadThread != Thread.currentThread()) {
							sem.release();
							return;
						}
					}
					///
					
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							int pc = mvDocument
							.getAttr(Document.ATTR_PAGECOUNT) != null ? Integer
									.parseInt(mvDocument
											.getAttr(Document.ATTR_PAGECOUNT))
											: 0;
									int pn = mvDocument.getAttr(Document.ATTR_PAGE) != null ? Integer
											.parseInt(mvDocument
													.getAttr(Document.ATTR_PAGE))
													: 1;
											for (FabAnnotation fa : hm.values()) {
												if (!canRead(fa))
													continue;
												fa.totPages = pc;
												user.add(fa);
												// System.out
												// .println(fa.getStringAnno());
											}
											if (!mod) {
												Fab4.getMVFrame(br).updateAnnoIcon();
												Fab4.getMVFrame(br).setStatus("");
												if (pc <= 1)
													for (FabAnnotation fa : hm.values()) {
														if (!canRead(fa))
															continue;
														loadAnnotation(br, mvDocument, fa);
													}
												if (pc > 1)
													for (FabAnnotation fa : hm.values())
													{
														if (!canRead(fa))
															continue;
														int apn = fa.getAnn().getPageNumber();
														if (apn == pn)
															loadAnnotation(br, mvDocument,
																	fa);
														// System.out.println("*" + apn
														// + "--" + pn);
													}
											} else {
												for (FabAnnotation fa : hm.values())
												{
													if (!canRead(fa))
														continue;
													int apn = fa.getAnn().getPageNumber();
													if (apn == pn)
														loadAnnotation(br, mvDocument, fa);
												}
												Fab4.getMVFrame(br).updateAnnoIcon();
												Fab4.getMVFrame(br).setStatus("");

											}

						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					int r = JOptionPane
					.showConfirmDialog(
							getBrowser(),
							"Error contacting the annotation server. Switch to private annotations?",
							"Warning", JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE);

					if (r == 0) {
						Fab4.getMVFrame(br).annoPanels.get(br).annotationPaneLabel
						.setSelectedIndex(1);
						reloadDocument();
					}
				} finally {
					sem.release();
					Fab4 f = Fab4.getMVFrame(br);
					if (f != null) {
						f.setStatus("");
						AnnotationSidePanel asp = f.annoPanels.get(br);
						asp.setCursor(Cursor
								.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}

				}
			}
		});
		sem.acquireUninterruptibly();
		loadThread.start();
	}

	/* Support methods and classes */

	/**
	 * 
	 * Loads a single annotation
	 * 
	 * @param br
	 * @param doc
	 * @param fa
	 * @param doc2
	 */
	void loadAnnotation(Browser br, Document doc, FabAnnotation fa) {
		AnnotationModel doc2 = fa.getAnn();
		Layer personal = doc.getLayer(Layer.PERSONAL);
		
		///SAM
		if((fa.getBehaviour() instanceof RateResource) || (fa.getBehaviour() instanceof TagResource) )
			return;
		///
		
		if (fa.isLoaded() && fa.getBehaviour() instanceof FabNote)
			if (fa.getBehaviour() != null) {
				FabNote ff = (FabNote) fa.getBehaviour();
				ff.setLampshade(false);
				ff.show(false);
				return;
			}
		String anno;
		try {
			anno = doc2.getAnnotationBody();
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		try {

			XML xml = (XML) Behavior.getInstance("xml", "XML",null, null);
			xml.setInput(InputUni.getInstance(anno, null, null));
			ESISNode n = xml.parse();
			
						
			n.putAttr("closed", "false");
			n.putAttr(DistributedPersonalAnnos.FABANNO, fa);
			
			
			
			Behavior newlay = Behavior.getInstance(n.getGI(), n
					.getAttr("behavior"), n, n.attrs, personal);
			fa.setBehaviour(newlay);
			
			///SAM
			if( newlay != null && ((newlay instanceof RateResource) ||
					(n.getAttr("name") != null && n.getAttr("name").startsWith("RATE"))) )
			{
				RateResource rateNote = (RateResource) newlay;
				rateNote.setVisible(false);
			}
			///
			
			if (newlay instanceof FabNote) {
				FabNote fabNote = (FabNote) newlay;
				if (fa.getSigner() != null) {
					if (fa.getVerificationStatus() != 1) {
						fabNote.setFontTitle(VFrame.FONT_TITLE_LIGHT);
						fabNote.setTitle("(" + fa.getSigner().getName() + ")");
					} else if (fa.getSigner().isItsme()) {
						fabNote.setFontTitle(VFrame.FONT_TITLE_BOLD);
						fabNote.setTitle(fa.getSigner().getName());
					} else
						fabNote.setTitle(fa.getSigner().getName());

				} else {
					fabNote.setFontTitle(VFrame.FONT_TITLE_LIGHT);
					///SAM
					if(currentServer == Servers.RDF_SWORD ) ///
						fabNote.setTitle("("
								+ fa.getAnn().getUserid() + ")");
					///SAM
					else if(currentServer == Servers.REST )
						fabNote.setTitle("("
								+ fa.getAnn().getAuthor() + ")");
					///
				}
				fabNote.setLampshade(true);
				if (fa.getAnn().getUserid().equals("anonymous"))
					fabNote.setTitleBg(FabAnnoListRenderer.ANON);
				else if (fa.getSigner() != null && fa.getSigner().isItsme())
					fabNote.setTitleBg(FabAnnoListRenderer.PERSONAL);
				else if (fa.getVerificationStatus() == 1)
					fabNote.setTitleBg(FabAnnoListRenderer.KNOWN);
				else
					fabNote.setTitleBg(FabAnnoListRenderer.UNKNOWN);

			} else if (newlay != null && newlay instanceof Span) {
				/* WE decorate the annotation to identify it... */
				Layer sc = null;
				if (!(newlay instanceof BIUSpan)) { // !(newlay instanceof
					// TextSpanNote)
					sc = doc.getLayer(Layer.SCRATCH);
					Fab4LabelSpan ls = (Fab4LabelSpan) Behavior.getInstance(
							"Note",
							"uk.ac.liverpool.fab4.behaviors.Fab4LabelSpan", n,
							n.attrs, sc);
					if (newlay instanceof BackgroundSpan && linguisticMode) {
						BackgroundSpan bs = (BackgroundSpan) newlay;
						ls.setLabel("");

					} else
						ls.setLabel(fa.getAnn().getUserid());
					ls.putAttr("decorates", newlay);
					newlay.putAttr(DistributedPersonalAnnos.ITSANOTE, ls);
				}
				if (doc.size() > 0 && doc.childAt(0).isStruct()) {
					personal.buildBeforeAfter(doc);
					if (sc != null)
						sc.buildBeforeAfter(doc);
					br.repaint(1000);
				}
			}
			fa.setLoaded(true);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@SuppressWarnings("boxing")
	/* * loads the preferences */
	public void loadPrefs() {
		if (toSave) {
			Fab4.prefs.addBeforeSavePrefs(new Runnable() {
				public void run() {
					savePrefs();
				}
			});
			toSave = false;
		}
		Properties props = Fab4.prefs.getProps();
		System.out.println("loadPre");
		if (!Fab4.prefs.isFirstRun()) {
			DistributedPersonalAnnos.author = props.getProperty("author", DistributedPersonalAnnos.author);
			DistributedPersonalAnnos.organisation = props.getProperty("organisation", DistributedPersonalAnnos.organisation);
			DistributedPersonalAnnos.email = props.getProperty("email", DistributedPersonalAnnos.email);
			if (Fab4.prefs.wsserver != null) {
				DistributedPersonalAnnos.publishServiceURL = Fab4.prefs.wsserver;
				System.out.println("Server>" + DistributedPersonalAnnos.publishServiceURL);
			} else
				DistributedPersonalAnnos.publishServiceURL = props.getProperty(
						"AnnotationServerLocation", DistributedPersonalAnnos.publishServiceURL);
			if (Fab4.prefs.searchaddress != null) {
				DistributedPersonalAnnos.searchServiceURL = Fab4.prefs.searchaddress;
				System.out.println("Location>" + DistributedPersonalAnnos.searchServiceURL);
			} else
				DistributedPersonalAnnos.searchServiceURL = props.getProperty(
						"SRULocation", DistributedPersonalAnnos.searchServiceURL);
			if (Fab4.prefs.wsuser != null) {
				System.out.println(">User");
				DistributedPersonalAnnos.userid = Fab4.prefs.wsuser;
				if (Fab4.prefs.wspass!=null){
					System.out.println(">Pass");
					DistributedPersonalAnnos.pass = Fab4.prefs.wspass;
				} else
					DistributedPersonalAnnos.pass = props.getProperty(DistributedPersonalAnnos.userid + "|"
							+ DistributedPersonalAnnos.publishServiceURL);
				if (DistributedPersonalAnnos.pass == null) {
					// Now we must ask the password, we use the standard Fab4
					// behaviuor
					// getBrowser().eventq(PasswordBehaviour.MSG_ASK_PASS,"The document is encrypted.\n Please enter the password:");
					DistributedPersonalAnnos.pass = JOptionPane.showInputDialog(getBrowser(),
							"Please enter a password for server:"
							+ DistributedPersonalAnnos.publishServiceURL + ", user: "
							+ DistributedPersonalAnnos.userid, "Please enter Password",
							JOptionPane.QUESTION_MESSAGE);
					if (DistributedPersonalAnnos.pass != null)
						Fab4.prefs.getProps().setProperty(
								DistributedPersonalAnnos.userid + "|" + DistributedPersonalAnnos.publishServiceURL, DistributedPersonalAnnos.pass);
				}
				// we will get back a semantic event (asyncr)
				// } //else
				// JOptionPane.showConfirmDialog(getBrowser(), pass);

			} else {
				DistributedPersonalAnnos.userid = props.getProperty("userid", DistributedPersonalAnnos.userid);
				DistributedPersonalAnnos.pass = props.getProperty("pass", DistributedPersonalAnnos.pass);
			}
			DistributedPersonalAnnos.sameUrl = Boolean.valueOf(props.getProperty("sameUrl", Boolean
					.toString(DistributedPersonalAnnos.sameUrl)));
			DistributedPersonalAnnos.sameDigest = Boolean.valueOf(props.getProperty("sameDigest",
					Boolean.toString(DistributedPersonalAnnos.sameDigest)));
			DistributedPersonalAnnos.sameLexical = Boolean.valueOf(props.getProperty("sameLexical",
					Boolean.toString(DistributedPersonalAnnos.sameLexical)));
			DistributedPersonalAnnos.sameTxtDigest = Boolean.valueOf(props.getProperty("sameTxtDigest",
					Boolean.toString(DistributedPersonalAnnos.sameTxtDigest)));
			
			///SAM
			DistributedPersonalAnnos.sameBib = Boolean.valueOf(props.getProperty("sameBib", Boolean
					.toString(DistributedPersonalAnnos.sameBib)));
			///
			
			DistributedPersonalAnnos.trustedParty = Boolean.valueOf(props.getProperty("trustedParty",
					Boolean.toString(DistributedPersonalAnnos.trustedParty)));
			DistributedPersonalAnnos.showFab4 = Boolean.valueOf(props.getProperty("showFab4", Boolean
					.toString(DistributedPersonalAnnos.showFab4)));
			DistributedPersonalAnnos.showPlay = Boolean.valueOf(props.getProperty("showPlay", Boolean
					.toString(DistributedPersonalAnnos.showPlay)));
			DistributedPersonalAnnos.showVoice = Boolean.valueOf(props.getProperty("showVoice", Boolean
					.toString(DistributedPersonalAnnos.showVoice)));
			PersonalAnnos.useRemoteServer = Boolean.valueOf(props.getProperty("isPublic",
					Boolean.toString(PersonalAnnos.useRemoteServer)));

			linguisticMode = Boolean.valueOf(props.getProperty(
					"linguisticMode", Boolean.toString(linguisticMode)));
			// Fab4.getMVFrame(this.getBrowser()).setAlwaysOnTop(true);//annotationPaneLabel.setSelectedIndex(loadRemote?0:1);
		} else
			new StartupWizard(this, Fab4.getMVFrame(getBrowser()), true)
		.setVisible(true);


	}

	/** loads the settings */
	public void loadSettings() {
		if (DistributedPersonalAnnos.toLoad) {
			loadPrefs();

			try {
				ResourceFinder finder = new ResourceFinder("META-INF/services/");
				List<Class> ascServices;
				ascServices = finder.findAllImplementations(AnnotationServerConnectorInterface.class);
				Iterator<Class> ir = ascServices.iterator();
				while (ir.hasNext()){
					Class c = ir.next();
					System.out.println("Set remoteasc to:" + c);
					DistributedPersonalAnnos.remoteAsc = c;
										
					///SAM
					if(c.equals(Class.forName("ca.ualberta.cs.RESTServerConnector.RESTAnnotationServer"))){
						currentServer = Servers.REST;
						/*annotationResourceURI = 
							//"http://localhost:8888/annotationResource"; 
							"http://hypatia.cs.ualberta.ca:8888/annotationResource";*/
						publishServiceURL = RESTpublish;
						searchServiceURL = RESTsearch;
					}
					else if(c.equals(Class.forName("uk.ac.liverpool.annotationConnector.SwordAnnotationServer"))){
						currentServer = Servers.RDF_SWORD;
//						annotationResourceURI = "http://shaman.cheshire3.org/services/annotations/";
						publishServiceURL = "http://shaman.cheshire3.org/sword/annotations/";
						searchServiceURL = "http://shaman.cheshire3.org/services/annotations";
					}
					///
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			DistributedPersonalAnnos.toLoad = false;
		}
		if (DistributedPersonalAnnos.kp == null)
			try {
				DistributedPersonalAnnos.kp = DistributedPersonalAnnos.ks.read(DistributedPersonalAnnos.dafPass);
			} catch (Exception e) {
				DistributedPersonalAnnos.kp = SignatureUtils.createKeystore();
				System.out.println("new key pair generated");
			}
			DistributedPersonalAnnos.myTI = new TrustedIdentity(DistributedPersonalAnnos.author, DistributedPersonalAnnos.organisation, DistributedPersonalAnnos.email, DistributedPersonalAnnos.kp.getPublic());
			DistributedPersonalAnnos.myTI.setItsme(true);
			if (DistributedPersonalAnnos.trusted == null)
				DistributedPersonalAnnos.trusted = TrustedIdentity.readStore(DistributedPersonalAnnos.deftrusted);
			// getAs();
	}

	/**
	 * The main method to publish all the annotations in a page, including
	 * updating the modified ones
	 * 
	 * @param reload
	 *            determines if the page needs to be reloaded after publishing
	 */
	private void publishAnnotations(final boolean reload,
			final AnnotationServerConnectorInterface as, final Behavior[] toStore) {
		
		/* The publishing thread */
		pt = new Thread(new Runnable() {

			public void run() {
				cancelPublish = false;
				//final ComputeLexicalServiceLocator sl = new ComputeLexicalServiceLocator();
				final Browser br = getBrowser();
				JDialog splash = null;
				if (as == ras)
					splash = showSplash("Publishing annotations");
				final Document mvDocument = (Document) br.getRoot().findBFS(
				"content");
				/* first we use the URI */
				final String uri = mvDocument.getURI().toString();
				/* and the TXT Digest */
				final byte[] digest = MediaLoader.MD5Cache.get(mvDocument
						.getURI());
				Layer personal = mvDocument.getLayer(Layer.PERSONAL);
				// / FIRST we compute the MD5 txt etc. *once for all
				// annotations, since they are document properties
				String txt = Fab4utils.getTextNonSpaced(mvDocument);
				if (txt.trim().length() == 0)
					txt = uri.toString();
				byte[] digestTxt = null;
				int pc = mvDocument.getAttr(Document.ATTR_PAGE) != null ? Integer
						.parseInt(mvDocument.getAttr(Document.ATTR_PAGE))
						: 0;
						try {
							MessageDigest md = MessageDigest.getInstance("MD5");
							md.update(txt.getBytes());
							digestTxt = md.digest(); // 128 bit or 16 bytes
						} catch (NoSuchAlgorithmException e1) {
						}
						String ls = null;
						if (DistributedPersonalAnnos.sameLexical)
							try {
								//	ComputeLexical cl = sl.getComputeLexical(new URL(
								//			lswsAddress));
								String txt2 = getText(mvDocument);
								//ls = cl.computeLexicalSignature(txt2);
							} catch (Exception ee) {
								ee.printStackTrace();
							}
							if (cancelPublish) {
								hideSplash(splash);
								return;
							}
							if (toStore == null) {
								// / Then we look for all the annotations
								for (int i = 0; i < personal.size(); i++) {
									Behavior annotationBehaviour = personal.getBehavior(i);

									chackAndPublishSingleAnnotation(as, digestTxt, pc, ls,
											annotationBehaviour, uri, digest);
									if (cancelPublish) {
										hideSplash(splash);
										return;
									}
								}

								try {
									System.out.println("reload " + reload);
									if (reload)
										if (!SwingUtilities.isEventDispatchThread())
											SwingUtilities.invokeLater(new Runnable() {
												public void run() {
													Document doc = (Document) br.getRoot()
													.findBFS("content");
													if (doc == mvDocument) {
														br.setCurDocument(doc);
														br.event(new SemanticEvent(this,
																Document.MSG_RELOAD, doc));
														setEnableOwnComponents(false);
													}
												}
											});
										else {
											Document doc = (Document) br.getRoot().findBFS(
											"content");
											if (doc == mvDocument) {
												br.setCurDocument(doc);
												br.event(new SemanticEvent(this,
														Document.MSG_RELOAD, doc));
												setEnableOwnComponents(false);
											}
										}
								} catch (Exception e) {
									e.printStackTrace();
								} finally {
								}
							} else
								for (Behavior b : toStore) {
									FabAnnotation fa = (FabAnnotation) b.getValue(DistributedPersonalAnnos.FABANNO);
									String stringAnno = fa.getStringAnno();
									boolean anonymous = b.getAttr("anonymous") != null;
									try {
										if (anonymous)
											as.postAnonymousAnnotation(stringAnno, "any");
										else{
											///SAM
											int state = 0; 
											
											if(currentServer != Servers.REST )
												state = as.postAnnotation(stringAnno, DistributedPersonalAnnos.userid, DistributedPersonalAnnos.pass);
											else{
												boolean auth = true;
												if( (as != las && ras != null) || was == ras || useRemoteServer)
//												if( (was == ras && ras != null) || useRemoteServer || DistributedPersonalAnnos.copyToPub )
													auth = authenticate();
												
												
												if(auth) ///
													state = as.postAnnotation(stringAnno, DistributedPersonalAnnos.userid, DistributedPersonalAnnos.pass);
												///SAM
												else
													state = 1;
											}
											
											if( state == 1 ){
												JOptionPane
												.showMessageDialog(
														getBrowser(),
														"User is not authenticated. Operation canceled",
														"Try again...",
														JOptionPane.INFORMATION_MESSAGE);
											}
											else if( state == 2 ){
												JOptionPane
												.showMessageDialog(
														getBrowser(),
														"Sorry, the annotation server is not responding. Please try later.",
														"Try later...",
														JOptionPane.INFORMATION_MESSAGE);
											}
											else if( state != 0 ){
												JOptionPane
												.showMessageDialog(
														getBrowser(),
														"Sorry, a problem exists either in the annotation format or in server. Please try later.",
														"Try later...",
														JOptionPane.INFORMATION_MESSAGE);
											}
											/*else if( state > 0 ){ //when id is returned
												fa.getAnn().setUniqueId(state);
												if(las != null && DistributedPersonalAnnos.copyToPub ) //update local db if it was a request for copying from private to local
													las.updateAnnotation(stringAnno, fa.getAnn().getUserid(), DistributedPersonalAnnos.pass);
											}*/
											///
										}
									} catch (Exception e) {

										e.printStackTrace();
									}

								}
							hideSplash(splash);
			}
///SAM commented this section
		});

		pt.start();
		if (!reload)
			try {
				pt.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

	}

	public void restore(ESISNode n, Map<String, Object> attr, Layer layer) {
		super.restore(n, attr, layer);
		user.setComp(new FabAnnoModificationDateComparator(false));
		loadSettings();

	}

	/**
	 * scrolls to a specific sticky note, if not currently visible.
	 * 
	 * @param aa
	 * @param x
	 * @param y
	 */
	void scrollToIfNeeded(Node aa, int x, int y) {
		Document mvDocument = (Document) getBrowser().getRoot().findBFS(
		"content");
		Rectangle vis = new Rectangle(mvDocument.getHsb().getValue(),
				mvDocument.getVsb().getValue(),
				mvDocument.getParentNode().bbox.width, mvDocument
				.getParentNode().bbox.height);
		boolean h, v;
		h = v = false;
		if (x < vis.x || x > vis.width + vis.x)
			h = true;
		if (y < vis.y || y > vis.height + vis.y)
			v = true;
		if (h && v)
			aa.scrollTo();
		else if (v)
			mvDocument.getVsb().setValue(y);
		else if (h)
			mvDocument.getHsb().setValue(x);
	}

	@SuppressWarnings("boxing")
	public boolean semanticEventAfter(SemanticEvent se, String msg) {
		Browser br = getBrowser();

		if (PersonalAnnos.MSG_GO_LOCAL == msg) {
			goLocal();
			checkPupup();
		} else if (PersonalAnnos.MSG_GO_REMOTE == msg) {
			goDistributed();
			checkPupup();
		} else if (PersonalAnnos.MSG_COPY == msg) {
			if (PersonalAnnos.useRemoteServer)
				copyToClient();
			else
				copyToServer();
		} else if (PersonalAnnos.MSG_DIST_ANNO_PREF_CREATE == msg)
			new PreferencesDialog(this, Fab4.getMVFrame(getBrowser()), true)
		.setVisible(true);
		else if (PersonalAnnos.MSG_SEARCH_ANNO == msg) {
			String text = (String) se.getArg();
			searchAnnos(text);
		} else if (PersonalAnnos.MSG_REFRESH_LIST == msg) {
			Document doc = (Document) br.getRoot().findBFS("content");
			// System.out.println("list refresh!");
			loadAnnotations(doc);
		} else if (msg == Note.MSG_SHOW)
			for (int i = 0; i < user.getSize(); i++) {
				FabAnnotation fa = (FabAnnotation) user.get(i);
				if (fa.getBehaviour() instanceof FabNote) {
					FabNote ff = (FabNote) fa.getBehaviour();
					ff.setLampshade(false);
					ff.show(false);

				}
				if (fa.getBehaviour() instanceof BackgroundSpan) {
					BackgroundSpan bs = (BackgroundSpan) fa.getBehaviour();
					bs.setVisible(true);
				}
			}
		else if (msg == PersonalAnnos.MSG_ICON)
			for (int i = 0; i < user.getSize(); i++) {
				FabAnnotation fa = (FabAnnotation) user.get(i);
				if (fa.getBehaviour() instanceof FabNote) {
					FabNote ff = (FabNote) fa.getBehaviour();
					ff.setLampshade(true);
					ff.show(false);

				}
				if (fa.getBehaviour() instanceof BackgroundSpan) {
					BackgroundSpan bs = (BackgroundSpan) fa.getBehaviour();
					bs.setVisible(true);
				}// fa.getBehaviour().
			}
		else if (msg == PersonalAnnos.MSG_HIDE)
			for (int i = 0; i < user.getSize(); i++) {
				FabAnnotation fa = (FabAnnotation) user.get(i);
				if (fa.getBehaviour() instanceof FabNote) {
					FabNote ff = (FabNote) fa.getBehaviour();
					ff.hide();
				}
				if (fa.getBehaviour() instanceof BackgroundSpan) {
					BackgroundSpan bs = (BackgroundSpan) fa.getBehaviour();
					bs.setVisible(false);
				}
			}
		else if (PersonalAnnos.MSG_LOAD_REMOTE == msg) {
			Document doc = (Document) br.getRoot().findBFS("content");
			FabAnnotation fa = (FabAnnotation) se.getArg();
			int pc = doc.getAttr(Document.ATTR_PAGE) != null ? Integer
					.parseInt(doc.getAttr(Document.ATTR_PAGE)) : 1;
					int pn = fa.ann.getPageNumber();
					if (pn > 1 && pc != pn) {
						br.event(new SemanticEvent(this, Multipage.MSG_GOPAGE,
								fa.ann.getPageNumber()));
						loadAnnotation(br, doc, fa);// br.eventq(MSG_LOAD_REMOTE,
						// fa);
						return false;
					}

					loadAnnotation(br, doc, fa);
		} else if (PersonalAnnos.MSG_DELETE == msg) {
			Document doc = (Document) br.getRoot().findBFS("content");
			FabAnnotation fa = (FabAnnotation) se.getArg();
			// deledele
			if (DistributedPersonalAnnos.deleteall)
				deleteAnno(fa);
			
			else{
				///SAM
				if(currentServer == Servers.REST ){
					if (fa.getSigner() != null && fa.getSigner().isItsme()
						|| fa.getAnn().getUserid().equals(
						"anonymous") || fa.getAnn().getUserid().equals(DistributedPersonalAnnos.userid) //.split("-")[0]) 
						|| !PersonalAnnos.useRemoteServer)
						deleteAnno(fa);
				} 
				else /// 
					if (fa.getSigner() != null && fa.getSigner().isItsme()
						|| fa.getAnn().getUserid().equals(
						"anonymous") || !PersonalAnnos.useRemoteServer)
					deleteAnno(fa);
			}
			
		} else if (PersonalAnnos.MSG_PUBLISH_ANNOS == msg
				&& br.getCurDocument().getAttr(Document.ATTR_LOADING) == null) {
			setWasToLocalOrRemote();
			if (se.getArg() != null && se.getArg() == PersonalAnnos.MSG_PUBLISH_ANNOS) {
				if (ras == null)
					getRemote();
				/* FIXME: for remote annotations, no reload since C3 takes some times to index them */
				///SAM
				if(currentServer == Servers.RDF_SWORD ) ///
					publishAnnotations(false, ras, null);
				///SAM: I want reload:
				else if(currentServer == Servers.REST )
					publishAnnotations(true, ras, null); ///
				
			} else if (se.getArg() != null && se.getArg() == PersonalAnnos.MSG_LOAD_REMOTE)
				publishAnnotations(true, DistributedPersonalAnnos.las, null);
			else
				publishAnnotations(true, was, null);
		}
		else if(msg != null && msg.equals("showAllTags")){
			setWasToLocalOrRemote();			
			while (ras == null) //TODO once I encountered an exception here, may be because of ras. how come ras is sometimes null?
				getRemote();
			String[] tags = ras.retreiveAllTags(br.getCurDocument().uri.toString());
			if(tags == null || tags.length == 0){
				tags = new String[1];
				tags[0] = "No tags yet assigned";
			}
			JOptionPane
			.showMessageDialog(
					getBrowser(),
					tags,
					"This resource's tags",
					JOptionPane.INFORMATION_MESSAGE);
		}
		///
		else if (Document.MSG_OPENED == msg) {
			DocInfo di = (DocInfo) se.getArg();
			Document doc2 = di.doc;
			URI uri = doc2.getURI(); // may have been normalized
			if (/* loadRemote && */uri != null)
				loadAnnotations(doc2);
			// System.out.println(doc2);
			// System.out.println(uri);

		} else if (msg == Multipage.MSG_NEXTPAGE
				|| msg == Multipage.MSG_FIRSTPAGE
				|| msg == Multipage.MSG_GOPAGE || msg == Multipage.MSG_LASTPAGE
				|| msg == Multipage.MSG_PREVPAGE || Document.MSG_RELOAD == msg
				|| msg == Multipage.MSG_OPENEDPAGE
				|| msg == Multipage.MSG_RELOADPAGE) {
			Document doc = (Document) br.getRoot().findBFS("content");
			loadAnnotations(doc);
		} else if (msg == Multipage.MSG_RELOADPAGE) {
			Document doc = (Document) br.getRoot().findBFS("content");
			int pc = doc.getAttr(Document.ATTR_PAGECOUNT) != null ? Integer
					.parseInt(doc.getAttr(Document.ATTR_PAGECOUNT)) : 1;
					int pn = doc.getAttr(Document.ATTR_PAGE) != null ? Integer
							.parseInt(doc.getAttr(Document.ATTR_PAGE)) : 1;
							if (pc > 1)
								for (Object ffa : user) {
									FabAnnotation fa = (FabAnnotation) ffa;
									int apn = fa.getAnn().getPageNumber();
									if (apn == pn)
										loadAnnotation(br, doc, fa);
								}

		} else if (msg == VFrame.MSG_RAISED) {
			VFrame f = (VFrame) se.getArg();
			for (Object o : user) {
				FabAnnotation fa = (FabAnnotation) o;
				if (fa.getBehaviour() instanceof FabNote) {
					FabNote fn = (FabNote) fa.getBehaviour();
					if (fn.win_ == f)
						theList.setSelectedValue(fa, true);
				}
			}
		} else if (msg == Fab4LabelSpan.MSG_SPAN_SEL) {
			Behavior f = (Behavior) se.getArg();
			Span db = (Span) f.getValue("decorates");
			for (Object o : user) {
				FabAnnotation fa = (FabAnnotation) o;
				if (fa.getBehaviour() instanceof Span) {
					Span fn = (Span) fa.getBehaviour();
					if (fn == db)
						theList.setSelectedValue(fa, true);
				}
			}

		}
		return false;
	}

	private void checkPupup() {
		if (PersonalAnnos.useRemoteServer) {
			mtoprivate.setEnabled(true);
			mtopublic.setEnabled(false);
		} else {
			mtoprivate.setEnabled(true);
			mtopublic.setEnabled(true);
		}
		///SAM
		if(currentServer == Servers.REST )
			mreply.setEnabled(true);
		else
			mreply.setEnabled(false);
		///
	}

	private void showNoteContent(TextSpanNote ts) {
		ts.showContents();
	}

	public boolean semanticEventBefore(SemanticEvent se, String msg) {
		// System.out.println("***"+msg);
		if (msg == Multipage.MSG_NEXTPAGE || msg == Multipage.MSG_GOPAGE
				|| msg == Multipage.MSG_LASTPAGE
				|| msg == Multipage.MSG_FIRSTPAGE
				|| msg == Multipage.MSG_PREVPAGE || Document.MSG_CLOSE == msg
				|| msg == Multipage.MSG_CLOSEPAGE

		) {
			Document mvDocument = (Document) getBrowser().getRoot().findBFS(
			"content"); 
			Layer personal = mvDocument.getLayer(Layer.PERSONAL);
			int pc = mvDocument.getAttr(Document.ATTR_PAGECOUNT) != null ? Integer
					.parseInt(mvDocument.getAttr(Document.ATTR_PAGECOUNT))
					: 1;
					//
					int k = 0;
					int kk = 0;
					for (int i = 0; i < personal.size(); i++) {
						Behavior bb = personal.getBehavior(i);
						FabAnnotation fa = (FabAnnotation) bb.getValue(DistributedPersonalAnnos.FABANNO);
						if (bb.getValue(DistributedPersonalAnnos.FABANNO) == null)
							k++;
						else if (fa != null && fa.getSigner() != null
								&& fa.getSigner().isItsme())
							if (bb instanceof Span)
								kk += ((Span) bb).modified ? 1 : 0;
							else if (bb instanceof FabNote)
								kk += ((FabNote) bb).isModified() ? 1 : 0;

					}
					if (k > 0 || kk > 0)
						if (!PersonalAnnos.useRemoteServer) {
							setWasToLocalOrRemote();
							publishAnnotations(false, was, null);
						} else if (k == 0) {
							setWasToLocalOrRemote();
							publishAnnotations(false, was, null);
						} else {

							String att = "There " + (k + kk > 1 ? "are" : "is");
							if (k > 0)
								att += " " + k + " new";
							if (kk > 0 && k > 0)
								att += " and";
							if (kk > 0)
								att += " " + kk + " modified";
							att += "  annotation" + (k + kk > 1 ? "s" : "")
							+ ": Save public, save private or lose?";
							String[] opts = new String[] { "Public", "Private", "Lose" };
							int n = JOptionPane.showOptionDialog(getBrowser(), att,
									"Attention", JOptionPane.YES_NO_CANCEL_OPTION,
									JOptionPane.INFORMATION_MESSAGE, null, opts,
									opts[0]);

							if (n == 0) {
								getRemoteAs();
								if (ras != null)
									publishAnnotations(false, ras, null);
								else {
									publishAnnotations(false, DistributedPersonalAnnos.las, null);
									JOptionPane
									.showMessageDialog(
											getBrowser(),
											"The annotation server is not responding. The notes will be stored in your private database.",
											"Warning",
											JOptionPane.WARNING_MESSAGE);
								}
							} else if (n == 1) {
								setWasToLocalOrRemote();
								publishAnnotations(false, DistributedPersonalAnnos.las, null);
							} else if (n == 2)
								return true;
						}
					// if (Document.MSG_CLOSE != msg)
					// if (Multipage.MSG_CLOSEPAGE!= msg && Document.MSG_CLOSE != msg)
					if (Document.MSG_CLOSE != msg)
						// )
						personal.clearBehaviors();
					// if (Multipage.MSG_CLOSEPAGE== msg) {
					// return true;
					// }
		}
		return false;
	}

	/** shows a temporary splash dialog */
	private JDialog showSplash(String message) {
		if (!PersonalAnnos.useRemoteServer)
			return null;
		final Browser br = getBrowser();
		Fab4 f = Fab4.getMVFrame(br);
		if (f != null)
			f.setEnabled(false);
		final JDialog splash = new JDialog(Fab4.getMVFrame(br), message);
		JPanel p = new JPanel(new BorderLayout(1, 1));
		JLabel splashlab = new JLabel(message);
		splashlab.setHorizontalAlignment(SwingConstants.CENTER);
		splashlab.setFont(splashlab.getFont().deriveFont(20.0f));
		splashlab.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		p.add(splashlab, BorderLayout.CENTER);
		JButton bu = new JButton("Cancel");
		bu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelPublish = true;
				splash.dispose();
				pt = null;
			}
		});
		p.add(bu, BorderLayout.SOUTH);
		splash.getContentPane().add(p);
		Rectangle d;
		if (f != null)
			d = Fab4.getMVFrame(br).getBounds();
		else
			d = new Rectangle(10, 10, 300, 300);
		int sw = 260;
		int sh = 100;
		int x = (d.width - sw) / 2;
		int y = (d.height - sh) / 2;
		splash.setBounds(x + d.x, y + d.y, sw, sh);
		splash.setVisible(true);
		return splash;
	}

	/**
	 * @param fa
	 */
	void updateDeleted(FabAnnotation fa) {
		user.remove(fa);
		theList.setSelectedIndex(-1);
		if (fa.behaviour != null) {
			Span ns = (Span) fa.behaviour.getValue(DistributedPersonalAnnos.ITSANOTE);
			if (ns != null)
				ns.destroy();
			fa.behaviour.destroy();
		}
		setEnableOwnComponents(false);

	}

	/** creates a list of FabAnnotations from a vector of XML representations */
	List<FabAnnotation> valdateAndCrateList(String[] notes) {
		List<FabAnnotation> retAnnoList = new ArrayList<FabAnnotation>(20);
		if (notes == null)
			return retAnnoList;
		for (String me : notes) {
			FabAnnotation fa = new FabAnnotation();
			try {

				fa.setAnn(DistributedPersonalAnnos.las.getDefaultAMS().parse(me));
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				continue;
			}
			fa.setStringAnno(me);
			try {
				if (!linguisticMode) {
					validationReturn vr = SignatureUtils
					.validateSignature(me);
					fa.setVerificationStatus(vr.returnValue);
					fa.setSigner(vr.ti);
				}
			} catch (Exception e) {
				e.printStackTrace();
				fa.setVerificationStatus(-200);
			}
			if (!(DistributedPersonalAnnos.trustedParty && fa.getSigner() == null))
				retAnnoList.add(fa);

		}
		return retAnnoList;
	}

	/** checks if the user onws all the selected notes */
	private boolean areAllSelectedValuesOwn(Object[] index) {
		FabAnnotation ff;
		Behavior bb;
		boolean allMine = true;
		if (index == null || index.length == 0)
			setEnableOwnComponents(false);
		else
			for (Object o : index) {
				ff = (FabAnnotation) o;
				if (ff.getSigner() != null) {
					if (!ff.getSigner().isItsme())
						allMine = false;
				} else if (!ff.getAnn().getUserid().equals(
				"anonymous"))
					allMine = false;
				
				///SAM for REST only
				//TODO without considering security, I want to let it work by this:
				//if( ff.getAnn().getUserid().equals(DistributedPersonalAnnos.userid))
				if(currentServer == Servers.REST )	
					allMine = true; //overrides previous decisions!
				///
				
				bb = ff.getBehaviour();
				if (bb != null && bb instanceof FabNote) {
					FabNote aa = (FabNote) bb;
					// prevColor = aa.getTitleBg()
					if (aa.getTitleBg() != null)
						aa.setTitleBg(aa.getTitleBg().darker());
					aa.setLampshade(false);
					aa.show(false);
				} else if (bb instanceof Span) {
					Fab4LabelSpan aa;
					aa = (Fab4LabelSpan) bb.getValue(DistributedPersonalAnnos.ITSANOTE);
					if (aa != null) {
						aa.setColor(Color.ORANGE);
						aa.setTextColor(Color.RED.darker().darker());
						if (bb instanceof TextSpanNote) {
							TextSpanNote ts = (TextSpanNote) bb;
							showNoteContent(ts);
						}
					}
					if (bb instanceof BackgroundSpan) {
						BackgroundSpan bs = (BackgroundSpan) bb;
						bs.setSelected(true);
						bs.markDirty();
					}
				}
			}
		return allMine;
	}

	/** checks publishes a single annotation */
	private void chackAndPublishSingleAnnotation(
			final AnnotationServerConnectorInterface destination, byte[] digestTxtDocument,
			int PageCount, String LexicalSignature,
			Behavior annotationBehaviour, String uri, byte[] digest) {

		FabAnnotation fa = (FabAnnotation) annotationBehaviour
		.getValue(DistributedPersonalAnnos.FABANNO);
		// if it exists already (on server) we check if it's modified
		boolean modified = false;
		if (fa != null && fa.getSigner() != null && fa.getSigner().isItsme())
			if (annotationBehaviour instanceof Span)
				modified = ((Span) annotationBehaviour).modified;
			else if (annotationBehaviour instanceof FabNote)
				modified = ((FabNote) annotationBehaviour).isModified();
			///SAM
			else if (annotationBehaviour instanceof RateResource)
				modified = ((RateResource) annotationBehaviour).isModified();
			///
		if (modified && fa != null) {
			annotationBehaviour.removeAttr(DistributedPersonalAnnos.FABANNO);
			annotationBehaviour.removeAttr("decorates");
			annotationBehaviour.removeAttr(DistributedPersonalAnnos.ITSANOTE);
		}
		// so, if we don't have it on server or if it is modified
		if (fa == null || modified)
			publishSingleAnnotation(destination, digestTxtDocument, PageCount,
					LexicalSignature, annotationBehaviour, uri, digest,
					modified, fa);
	}

	/** publishes a single annotation */
	private void publishSingleAnnotation(final AnnotationServerConnectorInterface destination,
			byte[] digestTxtDocument, int PageCount, String LexicalSignature,
			Behavior annotationBehaviour, String uri, byte[] digest,
			boolean modified, FabAnnotation fa) {

		String id = null;
		if (modified && fa != null)
			if (fa.ann != null && fa.ann != null) {
				id = fa.ann.getId();
				System.out.println("ID Modified annotation, using the old ID : "+ id);
			}
		if (id == null)
			id = DistributedPersonalAnnos.userid + "-"
			+ Long.toHexString(Math.abs(new Random().nextLong()));

		AnnotationModel am = new AnnotationModel();

		///SAM
		if( annotationBehaviour instanceof FabNote ){
			if( ((FabNote)annotationBehaviour).replyOnSth ){ 
				am.setReplyToSth(true);
				am.setReplyToFabId(((FabNote)annotationBehaviour).replyOnFabId);
				am.setReplyTo( ((FabNote)annotationBehaviour).replyOn ); //null or not null, no problem 				
			}
		}
		
		if (modified && fa != null) //taken from up
			if (fa.ann != null && fa.ann != null)
				if( fa.ann.getUniqueId() != null ) //I think it has unique id only if modified, and if modified it has ann filled
					am.setUniqueId(fa.ann.getUniqueId());
		
		//TODO set unique id as an attr in annotationBehavior, here, find if it is needed
		///
		
		am.setId(id);
		// we write down the annotation body (from Multivalent);
		String annoBody = annotationBehaviour.save().writeXML();
		// and populate the whole annotation:
		am.setAnnotationBody(annoBody);

		System.out.println(annoBody);
		boolean anonymous = annotationBehaviour.getAttr("anonymous") != null;

		Date now = new Date();
		am.setDateModified(now);
		if (modified && fa != null)
			am.setDateCreated(fa.getAnn().getDateModified());
		else
			am.setDateCreated(now);


		if (anonymous) {
			am.setUserid("anonymous");
			am.setAuthor("anonymous");

		} else {
			am.setUserid(DistributedPersonalAnnos.userid);
			am.setAuthor(DistributedPersonalAnnos.author);
		}
		am.setLexicalSignature(LexicalSignature);
		System.out.println("On Publish "+ base64.toString(digestTxtDocument));
		am.setDocumentTextDigest(base64.toString(digestTxtDocument));
		am.setPageNumber(PageCount);
		
		///SAM:
		if( digest != null )
			am.setDocumentDigest(base64.toString(digest));
		else{
			am.setDocumentDigest(null);
			System.err.println("\n**************");
			System.err.println("DIGEST was NULL......\n");
		}
		/// (there were no if-else, but only the instruction in 'if' -without if		
		
		am.setUri(DistributedPersonalAnnos.annotationResourceURI);
		am.setResourceUri(uri);

		///SAM
		Document doc = (Document) getBrowser().getRoot().findBFS("content");
		if(bibForDocument.containsKey(doc)){
			int resourceId = bibForDocument.get(doc);
			if(resourceId != -1)
				am.setResourceId(resourceId);
		}
		///
		
		String stringAnno;
		try {
			stringAnno = destination.getDefaultAMS().serialise(am);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		annotationBehaviour.putAttr(DistributedPersonalAnnos.FABANNO, FabAnnotation.dummy_);

		//		 so we can sign it
		if (!anonymous && destination == DistributedPersonalAnnos.las )
			try {
				stringAnno = SignatureUtils.sign(stringAnno);
			} catch (Exception e3) {
				e3.printStackTrace();
			}
			//		 then submit, depending on the situation
			try {
				if (modified) {
					if (anonymous)
						destination.updateAnonymousAnnotation(id, stringAnno,
						"any");
					else
						destination.updateAnnotation(stringAnno, DistributedPersonalAnnos.userid, DistributedPersonalAnnos.pass);
				} else if (anonymous)
					destination.postAnonymousAnnotation(stringAnno, "any");
				else{
					///SAM
					int state = 0;
					if(currentServer == Servers.REST ){
						boolean auth = true;
						if( (was == ras && ras != null ) || useRemoteServer || DistributedPersonalAnnos.copyToPub )
							auth = authenticate();
						
						
						if(auth) ///
							state = destination.postAnnotation(stringAnno, DistributedPersonalAnnos.userid, DistributedPersonalAnnos.pass);
						///SAM
						else 
							state = 1;
					}
					else
						state = destination.postAnnotation(stringAnno, DistributedPersonalAnnos.userid, DistributedPersonalAnnos.pass);
					
					if( state == 1 ){
						JOptionPane
						.showMessageDialog(
								getBrowser(),
								"User is not authenticated. Operation canceled",
								"Try again...",
								JOptionPane.INFORMATION_MESSAGE);
					}
					else if( state == 2 ){
						JOptionPane
						.showMessageDialog(
								getBrowser(),
								"Sorry, the annotation server is not responding. Please try later.",
								"Try later...",
								JOptionPane.INFORMATION_MESSAGE);
					}
					else if( state != 0 ){
						JOptionPane
						.showMessageDialog(
								getBrowser(),
								"Sorry, a problem exists either in the annotation format or in server. Please try later.",
								"Try later...",
								JOptionPane.INFORMATION_MESSAGE);
					}
					///
				}
			} catch (Exception e2) {
				e2.printStackTrace();

			}
	}

	/** copies selected annotations to the server */
	private void copyToServer() {
		JList l = theList;
		Object[] m = l.getSelectedValues();
		boolean allMine = areAllSelectedValuesOwn(m);
		Behavior[] theNotes = new Behavior[m.length];
		if ((DistributedPersonalAnnos.deleteall || allMine) && !PersonalAnnos.useRemoteServer) {
			for (int i = 0; i < m.length; i++) {
				FabAnnotation fa = (FabAnnotation) m[i];
				theNotes[i] = fa.getBehaviour();
			}
			///SAM:
			int i = 0;
			do{///
				getRemoteAs();
				///SAM
				i++;
				if( i > 1000 ){
					JOptionPane
					.showMessageDialog(
							getBrowser(),
							"Sorry, the annotation server is not responding. Please try later.",
							"Try later...",
							JOptionPane.INFORMATION_MESSAGE);
					break;
				}
				//System.out.print("*");
			}while(ras == null); ///
			
			if (ras != null){
				///SAM
				/*AnnotationServerConnectorInterface temp = was;
				was = ras; //going to public for a while
				DistributedPersonalAnnos.copyToPub = true;*/
				///
				
				///SAM 
				if(currentServer == Servers.REST)
					publishAnnotations(true, ras, theNotes);
				else ///
					publishAnnotations(false, ras, theNotes);
				
				///SAM
				/*was = temp; //going back
				DistributedPersonalAnnos.copyToPub = false;*/
				///
			}
		}
	}

	/** copies selected annotations to the local database */
	private void copyToClient() {
		JList l = theList;
		Object[] m = l.getSelectedValues();
		Behavior[] theNotes = new Behavior[m.length];
		for (int i = 0; i < m.length; i++) {
			FabAnnotation fa = (FabAnnotation) m[i];
			theNotes[i] = fa.getBehaviour();
		}
		publishAnnotations(false, DistributedPersonalAnnos.las, theNotes);
	}

	///SAM
	private void replyToAnno(){
		
	  try{	
		JList l = theList;
		Object[] m = l.getSelectedValues();
		
		//just get the last selected one! TODO: make it to select only one
		//for (int i = 0; i < m.length; i++) {
		FabAnnotation fa = (FabAnnotation) m[m.length-1];
		Behavior replyTo = fa.getBehaviour();
		//}
		//create an anchored note
		
		HashMap<String, Object> hh = new HashMap<String, Object>(1);
		hh.put("callout", "");
		hh.put("replyOnSth", "");
		
		//String replyToName = fa.getAnn().getUniqueName();
		/*annotationUri + fa.getAnn().id;
		try {
			replyToName = fa.getAnn().annotationUri + URLEncoder.encode(fa.getAnn().id, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			
			e.printStackTrace();
		}*/
		//hh.put("replyOn", replyToName);
		
		if(fa.getAnn().getUniqueId() != null){
			hh.put("replyOn", String.valueOf(fa.getAnn().getUniqueId() ) );
		}
		hh.put("replyOnFabId", fa.getAnn().getId());
		
		hh.put("px", replyTo.getValue("x"));
		hh.put("py", replyTo.getValue("y"));
		///needed: f.getCurDoc().getLayer(Layer.PERSONAL) I thought I can use: (TODO: make sure)
		Document mvDocument = (Document) getBrowser().getRoot().findBFS("content");
		Layer personal = mvDocument.getLayer(Layer.PERSONAL);
		///
		
		Behavior.getInstance("Note","uk.ac.liv.c3connector.FabNote", null, hh, personal);
		
		
		/*FabNote reply = new FabNote();
		reply.callout = true;
		reply.replyOnSth = true;
		reply.replyOn = replyTo;
		reply.show(true); //don't know!!
*/		
	  }catch(Exception e){
		e.printStackTrace();
	  }
	}
	///

	///SAM
	private boolean authenticate(){
		String msg = "Swiching to private annotations. Sorry, the server is not responding!";
		boolean ret = true;
		
		if(pass != null && !pass.equals("")) //already authenticated
			return true;
		
		try{
			Authenticator.running = true;
			Authenticator au = new Authenticator(this, Fab4.getMVFrame(getBrowser()), true, Authenticator.LOGIN);
			au.setAlwaysOnTop(true);
			au.setLocationRelativeTo(null);
			au.setVisible(true);
			//wait();
			
			if( Authenticator.CANCEL ){
				Authenticator.CANCEL = false;
				ret = false;
				msg = "Going to public zone canceled.. Returning to private annotations.";
			}
			
		}catch(Exception e){			
			e.printStackTrace();
			ret = false;
		}
		
		if( !ret ){
			//the following block is a copy of what was given in getRemote's exception handler
			ras = null;
			final Browser br = getBrowser();
			PersonalAnnos.useRemoteServer = false;
			JOptionPane
			.showMessageDialog(
					getBrowser(),
					msg,
					"Warning", JOptionPane.WARNING_MESSAGE);
			Fab4.getMVFrame(br).annoPanels.get(br).annotationPaneLabel
			.setSelectedIndex(1);
			//reloadDocument();
		}
		
		return ret;
	}
	
	
	private static Integer requestBibOrUrl(URI oldURi){
			
//		HashMap<URI,Integer> info = new HashMap<URI, Integer>();
		
		Integer resourceId = -1;
		String uri = oldURi.toString();
//		String newUrl = uri;
		String bibtex = null;
		String doi = null;
		String keywords = null;
		String needed = "0";
		try{			
			/*if(ras == null)
				setWasToLocalOrRemote();*/
			if(ras != null)
				needed = ras.urlLacksBibDoiKeywords(uri);
			if(!needed.equals("11")){
				while( Authenticator.running );
//				if(!Authenticator.CANCEL){
					DocumentInfoRequester docR = new DocumentInfoRequester(/*this,*/ /*Fab4.getMVFrame(getBrowser())*/ null, true, /*uri,*/needed);
					docR.setLocationRelativeTo(null);
					docR.setVisible(true);
//				}
			}
			
			/*if(DocumentInfoRequester.newUrl != null){
				newUrl = DocumentInfoRequester.newUrl;
				DocumentInfoRequester.newUrl = null;
			}*/
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
			resourceId = addNewResource(bibtex, /*newUrl*/ uri);
		else if(doi != null || keywords != null)
			resourceId = updateResourceBib(uri,doi,keywords);
		
		}catch(Exception e){			
			e.printStackTrace();		
		}
		/*URI newURi = oldURi;
		try {
			newURi = new URI(newUrl);
		} catch (URISyntaxException e) {
			JOptionPane
			.showMessageDialog(
					getBrowser()null,
					"Invalid URL. URL Ignored. Document's URI will be used instead.",
					"Warning", JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
		}
		
		info.put(newURi, resourceId);
		return info;*/
		return resourceId;
	}
	
	
	///
	
	///SAM
	public HashMap<String,String> isAuthenticated(String username, String pass){
		/*if( DistributedPersonalAnnos.copyToPub )
			return ras.authenticated(username, pass);
		else*/ 
		/*if( was != null )
			return was.authenticated(username, pass);
		else 
		if( ras != null && PersonalAnnos.useRemoteServer )
			return ras.authenticated(username, pass);
		else
			return las.authenticated(username, pass);
			*/
		if( ras != null )
			return ras.authenticated(username, pass);
		else{
			HashMap<String,String> ret = new HashMap<String, String>();
			ret.put("3", "");
			return ret;
		}
	}
	
	public int createNewUser(String username, String passw, String email, String name, String des, String aff){
		if( was != null )
			return was.createNewUser(username, passw, email, name, des, aff);		
		else if( PersonalAnnos.useRemoteServer )
			return ras.createNewUser(username, passw, email, name, des, aff);
		else
			return las.createNewUser(username, passw, email, name, des, aff);
	}
	
	/**
	 * @param bib
	 * @param url
	 * @return resourceId if ok, otherwise a negative number
	 */
	public static int addNewResource(String bib, String url){
		if(ras != null)
			return ras.addAnnotatedResource(bib, url);
		else{
			try {
				ras = generateRemote();
				return ras.addAnnotatedResource(bib, url);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return -1;
	}
	
	public static int updateResourceBib(String url, String doi, String keywords){
		if(ras != null)
			return ras.updateResourceBib(url, doi, keywords);
		else{
			try {
				ras = generateRemote();
				return ras.updateResourceBib(url, doi, keywords);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			return -1;
	}
	
	public static void askForDocumentInfo(String uri){
		
		///SAM
		if( !bibForDocument.containsKey(uri) && PersonalAnnos.useRemoteServer){	//FIXME what about copy to server?		
			try {
				/*HashMap<URI,Integer> info = requestBibOrUrl(new URI(uri));
				Set<URI> key = info.keySet();
				URI newUrl;
				
					newUrl = new URI(uri);
				
				Integer resourceId = null;
				for(URI k : key ){
					newUrl = k;
					resourceId = info.get(k);
				}
				uri = newUrl.toString(); //FIXME, why no setter?
*/				
				Integer resourceId = requestBibOrUrl(new URI(uri));
				//even if resourceId==-1, because we don't want to bother user
				bibForDocument.put(uri,resourceId);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static String getCurrentRemoteServer(){
		if(currentServer == Servers.REST)
			return "REST";
		else if(currentServer == Servers.RDF_SWORD)
			return "RDF";
		return "";
	}
	
	public static void setCurrentRemoteServer(String cur){
		
		Class temp = remoteAsc;
		try {
			
			if(cur.equals("REST")){				
				remoteAsc = (Class<AnnotationServerConnectorInterface>) Class.forName("ca.ualberta.cs.RESTServerConnector.RESTAnnotationServer");
				publishServiceURL = RESTpublish;
				searchServiceURL = RESTsearch;	
			}
			else if(cur.equals("RDF")){			
				remoteAsc = (Class<AnnotationServerConnectorInterface>) Class.forName("uk.ac.liverpool.annotationConnector.SwordAnnotationServer");
				publishServiceURL = "http://shaman.cheshire3.org/sword/annotations/";
				searchServiceURL = "http://shaman.cheshire3.org/services/annotations";
			}
			
			ras = generateRemote();
			DistributedPersonalAnnos.PrevAnnotationServerLocation = DistributedPersonalAnnos.publishServiceURL;
			if (ras.getRepositoryType().indexOf("cheshire3") >= 0)
				c3as = true;
			else
				c3as = false;
			
			//if no exception occurred:
			if(cur.equals("REST"))
				currentServer = Servers.REST;
			else if(cur.equals("RDF"))
				currentServer = Servers.RDF_SWORD;
			
			las.setDefaultAMS(ras.getDefaultAMS());
			
		} catch (Exception e) {
			e.printStackTrace();			
						
			JOptionPane
			.showMessageDialog(
					null,
					"The chosen server is not responding! Going back to previous one!",
					"Warning", JOptionPane.WARNING_MESSAGE);
			
			remoteAsc = temp;
			if(currentServer == Servers.REST){							
				publishServiceURL = RESTpublish;
				searchServiceURL = RESTsearch;				
			}
			else if(currentServer == Servers.RDF_SWORD){							
				publishServiceURL = "http://shaman.cheshire3.org/sword/annotations/";
				searchServiceURL = "http://shaman.cheshire3.org/services/annotations";				
			}
			
		}
			
	}
	
	public void publishAnnos(){
		getBrowser().eventq(PersonalAnnos.MSG_PUBLISH_ANNOS,
				PersonalAnnos.MSG_PUBLISH_ANNOS);
	}
	
}
