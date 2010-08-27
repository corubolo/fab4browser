package multivalent.std;

import java.util.TimerTask;
import java.util.Observable;



/**
	{@link java.util.TimerTask} that notifies all of its {@link java.util.Observable}s
	every time it is {@link java.util.TimerTask#run()}.
	Alternatively sends as <var>arg</var> {@link java.lang.Boolean#TRUE} and {@link java.lang.Boolean#FALSE}.

	@version $Revision: 1.2 $ $Date: 2002/01/16 05:08:16 $
*/
public class SyncTimerTask extends TimerTask {
  private Boolean state_ = Boolean.TRUE;

  private Observable obs_ = new Observable() {
	public void notifyObservers(Object arg) { setChanged();/*always notify*/ super.notifyObservers(arg); }
  };


  //public SyncTimerTask() {}
  //public SyncTimerTask(Observer first) { o.addObserver(first); }

  public Observable getObservable() { return obs_; }

  public void run() {
	state_ = state_==Boolean.TRUE? Boolean.FALSE: Boolean.TRUE;
	obs_.notifyObservers(state_);
  }
}
