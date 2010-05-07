package multivalent.gui;

import java.util.ArrayList;
import java.util.List;


/**
	Used by VRadiobox to keep track of which in group is selected.
	x@deprecated - key on some attribute instead, which button queries to see if active, so can share with new behaviors

	@version $Revision$ $Date$
*/
public class VRadiogroup {
  VRadiobox active_=null;
  List<VRadiobox> group_ = new ArrayList<VRadiobox>(10);

  /** Return active radiobox. */
  public VRadiobox getActive() { return active_; }

  /** Set the passed radiobox to be the active one in the group, automatically unselecting the old active. */
  public void setActive(VRadiobox r) {
	if (r!=active_) {
		VRadiobox old = active_;
		active_=r;
		if (old!=null) old.repaint();
		if (active_!=null) active_.repaint();
	}
  }

  //** Return list of members of this radio group. */
  public List<VRadiobox> getGroup() { return group_; }
  /** Add radiobox to group. */
  public void addObserver(VRadiobox r) { if (group_.indexOf(r)==-1) group_.add(r); }
  /** Remove radiobox from group. */
  public void deleteObserver(VRadiobox r) { group_.remove(r); }

  public void clear() {
	group_.clear(); active_=null;
  }
}
