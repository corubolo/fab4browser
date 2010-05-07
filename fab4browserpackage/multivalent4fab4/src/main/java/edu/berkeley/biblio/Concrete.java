package edu.berkeley.biblio;

import java.io.*;
//import java.awt.Graphics;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.*;

import multivalent.Node;
import multivalent.INode;
import multivalent.Document;
import multivalent.ESISNode;


/**
	Base class for concrete biblio output formats.

	Demonstration of selection extension across behaviors.

	@version $Revision$ $Date$
*/
public abstract class Concrete extends multivalent.Behavior {
  static final boolean DEBUG = true;

  int active;


  public Concrete() {
	active = -1;
	// check that set title string
  }

/*
  public String getCategory(int id) { return "BiblioSelect"; }
  public abstract Object getTitle(int id);
  public int getType(int id) { return Valence.RADIOBOX; }
  public String getStatusHelp(int id) { return "Paste selection as "+getTitle(0); }*/
  public void command(int id, Object arg) {
	active = id;
	getBrowser().clipboard();
  }

  // find all BIB nodes in tree, add self as observer --> call Biblio class directly
  public void buildAfter(Document doc) {
/*JB
	List<> bibs = biblio.getBibs();
	for (int i=0,imax=bibs.size(); i<imax; i++) ((Node)bibs.get(i)).addObserver(this);
*/
  }


/*
  public ESISNode save() {
	// write out group
	// annotation set?
	return null;
  }*/



  public boolean clipboardBefore(StringBuffer txt, Node node) {
	if (active==-1) return false;
	else {
		txt.append(translate((INode)node)); txt.append(' ');
		return true;
	}
  }



  public abstract String translate(INode bibinfo);
}
