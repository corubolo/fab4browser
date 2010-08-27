package phelps.util;



/**
	For debugging, track elapsed time and frequency.
	<ul>
	<li>time - 
	Tracks elapsed time between {@link #start()} and {@link #end()}.
	Reported as milliseconds by {@link #get()}, or as hour/min/sec String by {@link #get()}.
	<li>frequency -
	Records number of {@link #start()} .. {@link #end} pairs, reported by {@link #getCount()}.
	<li>log -
	if non-null message passed to constructor, start and end method emit message to System.out.
	LATER: use Java logging framework
	</ul>

<!--
ADD
show memory use (delta maybe or maybe not meaningful)
show file size
maybe if end(Object) shows object-specific info (File length, something else for memory use)
-->
*/
public class Tracker {
  private String msg_;
  private int count_ = 0;
  private long ms_ = 0L;
  private long mstotal_ = 0L;
  private long msmin_ = Long.MAX_VALUE, msmax_ = Long.MIN_VALUE;

  private boolean inmediares_ = false;
  private long start_ = 0L, end_ = 0L;


  public Tracker(String msg) {
	msg_ = msg;
	//start();	// ?
  }

/*
  public Tracker(String msg, Log level) {
	// if logging level too low, don't show
  }*/

  public void start() {
	start_ = System.currentTimeMillis();
	inmediares_ = true;

	if (msg_!=null) System.out.println("START: "+msg_);
  }

  public void end() {
	if (!inmediares_) return;
	end_ = System.currentTimeMillis();
	ms_ = end_ > start_? end_-start_: Long.MAX_VALUE - start_ + end_;
	mstotal_ += ms_;
	if (ms_ < msmin_) msmin_ = ms_; else if (ms_ > msmax_) msmax_ = ms_;
	count_++;
	inmediares_ = false;

	if (msg_!=null) System.out.println("END: "+msg_+" in "+asHMS(getElapsed()));
  }

  //public long getStart() { return start_; }
  //public long getEnd() { return end_; }
  public long getElapsed() { return ms_; }
  public long getMin() { return msmin_; }
  public long getMax() { return msmax_; }
  public String asHMS(long ms) {
	String txt;
	if (ms < 1000) {
		txt = ms + " ms";
	} else /*if (ms < 60 * 1000)*/ {
		txt = (ms/1000.0) + " sec";
	}
	return txt;
  }
  public long getTotal() { return mstotal_; }

  public int getCount() { return count_; }
}
