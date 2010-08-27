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
package uk.ac.liverpool.fab4;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import multivalent.Behavior;
import multivalent.Browser;
import multivalent.Layer;


/**
 * 
 * 
 * This class implements the UI for the side annotation panel that lists the annotations on the left.
 * 
 * @author fabio
 *
 */

public class AnnotationSidePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public AnnoToolBar atb = null;
	static Color privateColor = new Color(10, 200, 10);

	static Color publicColor = new Color(200, 10, 10);
	static Color searchColor = new Color(10, 10, 200);
	public JComboBox annotationPaneLabel;

	JButton bdeleAnno;

	JButton bMoveAnno;

	JButton bPubAnno;
	JButton bSaveAnno;

	JButton bShowAnno;

	JButton bIconAnno;

	JButton bHideAnno;

	JButton bCopy;

	JList annoUserList;

	JLabel nsel;

	boolean local = false;

	private Browser bro;
	private Fab4 fab;

	public AnnotationSidePanel(final Browser br, final Fab4 f) {
		super(new BorderLayout(5, 3));
		bro = br;
		fab = f;
		final PersonalAnnos bu = (PersonalAnnos) Fab4utils.getBe("Personal", br
				.getRoot().getLayers());
		// JPanel icos = new JPanel(new FlowLayout(FlowLayout.CENTER,3,2));
		setBorder(new LineBorder(AnnotationSidePanel.publicColor, 2));
		JPanel topp = new JPanel(new BorderLayout(4, 1));
		annotationPaneLabel = new JComboBox(new String[] { "Public notes",
		"Private notes" });
		annotationPaneLabel.setToolTipText("Choose if public or private");
		annotationPaneLabel.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					loadRemoteCombo();
			}
		});
		annotationPaneLabel.setFont(annotationPaneLabel.getFont().deriveFont(
				Font.BOLD));

		topp.add(BorderLayout.NORTH, annotationPaneLabel);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));
		final JTextField tt = new JTextField("type search");
		tt.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getButton() == java.awt.event.MouseEvent.BUTTON1)
					tt.selectAll();
			}
		});
		tt.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent arg0) {
				if (arg0.getKeyChar() == '\n') {
					String txt = ((JTextField) arg0.getSource()).getText();
					search(txt);
				}
			}

		});
		JButton lb = new JButton(FabIcons.getIcons().ICOLIGHT);
		lb.setToolTipText("Search in the annotations");
		lb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				search(tt.getText());
			}

		});
		south.add(tt);
		south.add(lb);

		// TODO: do a better search, separate window
		JComboBox cb = new JComboBox(new String[] { "Date", "Author", "Trust",
		"Position" });
		cb.setToolTipText("Sort notes by:");
		cb.setEditable(false);
		JPanel t = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 1));
		t.add(new JLabel("sort"));
		t.add(cb);
		topp.add(BorderLayout.SOUTH, t);

		// int wi = bHideAnno.getPreferredSize().width*3+20;
		// wi = Math.max(wi,annotationPaneLabel.getPreferredSize().width);
		int hei = annotationPaneLabel.getPreferredSize().height + /*
		 * bHideAnno.getPreferredSize
		 * ().height
		 */+10
		 + tt.getPreferredSize().height;
		Dimension dim = new Dimension(
				annotationPaneLabel.getPreferredSize().width, hei);
		topp.setPreferredSize(dim);
		topp.setMinimumSize(dim);
		topp.setMaximumSize(dim);

		annoUserList = new JList();
		annoUserList.setAutoscrolls(true);
		annoUserList.setBackground(new Color(255, 255, 230));
		annoUserList
		.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		bdeleAnno = new JButton("Delete", FabIcons.getIcons().ICODEL);
		bdeleAnno.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i : annoUserList.getSelectedIndices())
					br.eventq(PersonalAnnos.MSG_DELETE, bu.user.get(i));
			}

		});
		bdeleAnno.setToolTipText("Delete selected notes");
		if (bu != null) {
			annoUserList.setModel(bu.user);
			Component[] t1 = new Component[1];
			t1[0] = bdeleAnno;
			bu.addUIElementsToFab4List(annoUserList, t1, cb);
		}
		bdeleAnno.setEnabled(false);
		add(topp, BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(annoUserList);
		add(scrollPane, BorderLayout.CENTER);
		annoUserList.addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				Object[] index = annoUserList.getSelectedValues();
				if (index.length > 0)
					bCopy.setEnabled(true);
				else
					bCopy.setEnabled(false);
				nsel.setText("" + index.length);

			}

		});

		JPanel ppp = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
		bCopy = new JButton("Copy  ", FabIcons.getIcons().ICOPUB);
		Font nf = bCopy.getFont();
		nf.deriveFont((float) (nf.getSize2D() * 0.8));
		bCopy.setFont(nf);
		bCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				br.eventq(PersonalAnnos.MSG_COPY, "");
			}
		});
		bCopy
		.setToolTipText("Copies the selected notes to the server or local database");
		ppp.add(bCopy);
		bCopy.setEnabled(false);

		bdeleAnno.setFont(nf);

		ppp.add(bdeleAnno);
		ppp.setPreferredSize(new Dimension(ppp.getPreferredSize().width,
				bdeleAnno.getPreferredSize().height * 2 + 3));

		nsel = new JLabel(" ");
		nsel.setFont(nf);
		ppp.add(nsel);
		add(ppp, BorderLayout.SOUTH);

	}

	public void loadRemoteCombo() {
		int i = annotationPaneLabel.getSelectedIndex();
		if (i == 0) {
			//	fab.loadRemote.setSelected(true);
			setBorder(new LineBorder(AnnotationSidePanel.publicColor, 2));
			// bPubAnno.setText("Publish");
			// bPubAnno.setIcon(FabIcons.getIcons().ICOPUB);
			fab.mpublish.setText("Save public");
			bCopy.setText("Copy to private");
			local = false;
			if (!PersonalAnnos.useRemoteServer) {
				bro.eventq(PersonalAnnos.MSG_GO_REMOTE, null);
				bro.eventq(PersonalAnnos.MSG_REFRESH_LIST, null);

			}
		} else if (i == 1) {
			//	fab.loadRemote.setSelected(false);
			setBorder(new LineBorder(AnnotationSidePanel.privateColor, 2));
			// bPubAnno.setText("Save");
			bCopy.setText("Copy to public");
			fab.mpublish.setText("Save private");
			local = true;
			// bPubAnno.setIcon(FabIcons.getIcons().ICOSAVE);
			if (PersonalAnnos.useRemoteServer) {
				bro.eventq(PersonalAnnos.MSG_GO_LOCAL, null);
				bro.eventq(PersonalAnnos.MSG_REFRESH_LIST, null);
			}
		}

	}

	private void search(String txt) {
		bro.eventq(PersonalAnnos.MSG_SEARCH_ANNO, txt);
		setBorder(new LineBorder(AnnotationSidePanel.searchColor, 2));
	}

	public void loadRemote() {
		if (!PersonalAnnos.useRemoteServer)
			annotationPaneLabel.setSelectedIndex(0);
		else
			annotationPaneLabel.setSelectedIndex(1);
	}

	public Container getAtb() {
		// TODO Auto-generated method stub
		return atb;
	}
}


class AnnoToolBar extends JToolBar {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final String SAVE = "";

	public AnnoToolBar(final Browser br, final Fab4 f,
			final AnnotationSidePanel asp) {
		super("Notes", SwingConstants.HORIZONTAL);

		asp.atb = this;
		asp.bShowAnno = new JButton("Show", FabIcons.getIcons().ICOSHO);
		Font nf = asp.bShowAnno.getFont();
		nf.deriveFont((float) (nf.getSize2D() * 0.8));

		asp.bShowAnno.setFont(nf);
		// asp.bShowAnno.setMargin(new java.awt.Insets(0, 3, 0, 3));
		asp.bShowAnno.setToolTipText("Show all the notes");
		f.setAction(asp.bShowAnno, "showNotes");
		JLabel tit = new JLabel("Notes");
		tit.setFont(nf);
		add(tit);
		addSeparator();
		add(asp.bShowAnno);

		asp.bIconAnno = new JButton("Icon", FabIcons.getIcons().ICOICO);
		// asp.bIconAnno.setMargin(new java.awt.Insets(0, 3, 0, 3));
		asp.bIconAnno.setToolTipText("Iconify all the notes");
		f.setAction(asp.bIconAnno, "iconNotes");
		asp.bIconAnno.setFont(nf);
		// add(asp.bIconAnno);

		asp.bHideAnno = new JButton("Hide", FabIcons.getIcons().ICOHID);
		// asp.bHideAnno.setMargin(new java.awt.Insets(0, 3, 0, 3));
		asp.bHideAnno.setToolTipText("Hide all the notes");
		f.setAction(asp.bHideAnno, "hideAnnos");
		add(asp.bHideAnno);

		addSeparator();
		addSeparator();
		asp.bPubAnno = new JButton("Save public", FabIcons.getIcons().ICOPUB);
		// asp.bPubAnno.setMargin(new java.awt.Insets(1, 3, 1, 3));
		asp.bPubAnno.setFont(nf);
		asp.bPubAnno.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				br.eventq(PersonalAnnos.MSG_PUBLISH_ANNOS,
						PersonalAnnos.MSG_PUBLISH_ANNOS);
			}
		});
		asp.bPubAnno.setToolTipText("Save the new notes to the server");
		add(asp.bPubAnno);

		asp.bSaveAnno = new JButton("Save private", FabIcons.getIcons().ICOSAVE);
		// asp.bPubAnno.setMargin(new java.awt.Insets(1, 3, 1, 3));
		asp.bSaveAnno.setFont(nf);
		asp.bSaveAnno.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				br.eventq(PersonalAnnos.MSG_PUBLISH_ANNOS,
						PersonalAnnos.MSG_LOAD_REMOTE);
			}
		});
		asp.bSaveAnno
		.setToolTipText("Save the new notes to the local database");
		add(asp.bSaveAnno);
		addSeparator();
		addSeparator();

		JButton create = new JButton("Note", FabIcons.getIcons().NOTE_ICO);
		create.setFont(nf);
		create.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (f.annotationExtension)
					Behavior.getInstance("Note",
							"uk.ac.liv.c3connector.FabNote", null,
							new HashMap<String, Object>(1), f.getCurDoc()
							.getLayer(Layer.PERSONAL));
				else
					Behavior.getInstance("Note", "multivalent.std.Note", null,
							new HashMap<String, Object>(1), f.getCurDoc()
							.getLayer(Layer.PERSONAL));
				// asp.annoUserList.setSelectedIndex(-1);
			}

		});
		create.setToolTipText("Create a new note");

		create.setFont(nf);
		add(create);

		try {
			Class.forName("uk.ac.liv.c3connector.CalloutNote");
			create = new JButton("Anchored", FabIcons.getIcons().NOTE_ICO_CALL);
			create.setToolTipText("New anchor note");
			create.addActionListener(new ActionListener() {
				@SuppressWarnings("unchecked")
				public void actionPerformed(ActionEvent e) {
					HashMap hh = new HashMap<String, Object>(1);
					hh.put("callout", "");
					Behavior.getInstance("Note",
							"uk.ac.liv.c3connector.FabNote", null, hh, f
							.getCurDoc().getLayer(Layer.PERSONAL));
				}
			});
			create.setFont(nf);
			add(create);

		} catch (Exception edd) {
		}
		try {
                    Class.forName("uk.ac.liverpool.fab4.jreality.SceneNote");
                    create = new JButton("3d model note", FabIcons.getIcons().NOTE_ICO_CALL);
                    create.setToolTipText("3d model note");
                    create.addActionListener(new ActionListener() {
                            @SuppressWarnings("unchecked")
                            public void actionPerformed(ActionEvent e) {
                                    HashMap hh = new HashMap<String, Object>(1);
                                    hh.put("callout", "");
                                    Behavior.getInstance("3dNote",
                                                    "uk.ac.liverpool.fab4.jreality.SceneNote", null, hh, f
                                                    .getCurDoc().getLayer(Layer.PERSONAL));
                            }
                    });
                    create.setFont(nf);
                    add(create);

            } catch (Exception edd) {
            }
		JButton high = f.getSpanButton("Mark", "Highlight",
				"multivalent.std.span.BackgroundSpan", false, null, FabIcons
				.getIcons().ICOHIGHLIGHT);
		high.setFont(nf);
		add(high);
		high.setToolTipText("New highlight note");
		high = f.getSpanButton("Comment", "TextSpanNote",
				"uk.ac.liverpool.fab4.behaviors.TextSpanNote", true, null,
				FabIcons.getIcons().ICOCOM);
		high.setToolTipText("New comment on highlight");
		high.setFont(nf);
		add(high);
		high = f.getSpanButton("New Citation note", "CitesSpanNote",
				"uk.ac.liverpool.fab4.behaviors.CitesSpanNote", true, null,
				FabIcons.getIcons().ICOCOM);
		high.setToolTipText("New comment on highlight");
		high.setFont(nf);
		add(high);

	}

}
