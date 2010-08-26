package phelps.util;

import java.util.Calendar;
import java.text.DateFormat;
import java.text.ParseException;



/**
	Extensions to {@link java.util.Date}.

	<ul>
	<li>length: <a href='#lengths'>built-in units</a>, {@link #getLength(String, String)}, {@link #convertLength(String, String)}, {@link #addLength(String, String, String, double)}
	<li>paper size: <a href='#papers'>built-in papers sizes</a>, {@link #getPaperSize(String, int)}, {@link #addPaperSize(String,String)}
	<li>number range, as of page numbers: {@link #parseRange(String, int)}, {@link #toRange(int[])}
	<li>size with possible metric suffix K/MB/GB/...: {@link #prettySize(long)}, {@link #parseSize(String)}
	</ul>

	@version $Revision: 1.1 $ $Date: 2003/11/27 18:37:21 $
*/
public class Dates {
  public static final int SECOND = 1;
  public static final int MINUTE = 60 * SECOND;
  public static final int HOUR = 60 * MINUTE;
  public static final int DAY = 24 * HOUR;
  public static final int WEEK = 7 * DAY;
  public static final int YEAR = (int)(365.24 * DAY);

	// => take these from DateFormatSymbols
  private static final String[] DAYOFWEEK = { "XXX", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
  //private static final String[] DAY = { "XXX", "Sun", "Mon", "Tues", "Wed", "Thu", "Fri", "Sat" };
  private static final String[] MONTH = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
  private static final String[] MON = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
  private static final String[] AMPM = { "am", "pm" };


  private static DateFormat dateFormat_ = null;	// create on demand
  private static Calendar cal_ = null;


  private Dates() {}


  /**
	Parse <var>sdate</var> and pass on to {@link #relative(long)}.
	<var>sdate</var> can either be a number, in the standard Java milliseconds since 1970,
	or a human-readable date that was produced by {@link java.util.Date#toString()}.
  */
  public static long parse(String sdate) throws ParseException {
	long date = -1;

	try { date = Long.parseLong(sdate); } catch (NumberFormatException nfe) {}

	if (date==-1) try { date = com.pt.net.HTTP.parseDate(sdate); } catch (ParseException pe) {}

	if (date==-1) {
		if (dateFormat_==null) { dateFormat_ = DateFormat.getDateTimeInstance(); dateFormat_.setLenient(true); }
		try { date = dateFormat_.parse(sdate).getTime(); } catch (ParseException pe) {}
	}

	if (date == -1) throw new ParseException("can't parse  date as either number or human readable: "+sdate, -1);
	return date;
  }

  /** Returns <var>date</var> {@link #relative(long,long) relative} to now. */
  public static String relative(long date) { return relative(date, System.currentTimeMillis()); }

  /**
	Returns <var>date</var>, in the standard Java milliseconds since 1970, relative to <var>relativeTo</var>,
	using relations like "yesterday" and "3 hours ago".
  */
  public static String relative(long date, long relativeTo) {	//return relative(new Date(date), new Date(relativeTo)); }
	long diffsec=(relativeTo-date)/1000; boolean future=(diffsec<0);
	String ago = " ago";
	if (future) { diffsec=-diffsec; ago=""; }	// => should leave "+/-" and "...ago" to caller

	if (diffsec<2*MINUTE) return (future?"+":"")+diffsec+" seconds"+ago;
	else if (diffsec<2*HOUR) return (future?"+":"")+(diffsec/MINUTE)+" minutes"+ago;
	else if (diffsec<24*HOUR) return (future?"+":"")+(diffsec/HOUR)+" hours"+ago;

	// should take output format from a preference pattern
	if (cal_==null) cal_=Calendar.getInstance();
	Calendar cal = cal_;
	cal.setTimeInMillis(date);
	int hour=cal.get(Calendar.HOUR_OF_DAY), min=cal.get(Calendar.MINUTE), month=cal.get(Calendar.MONTH);
	if (diffsec<2*DAY) return hour+":"+(min<10?"0":"")+min+(future? " tomorrow":" yesterday");
	else if (diffsec<WEEK) return hour+":"+(min<10?"0":"")+min+" "+DAYOFWEEK[cal.get(Calendar.DAY_OF_WEEK)];
	else if (diffsec<45*WEEK) return cal.get(Calendar.DAY_OF_MONTH)+" "+MONTH[month];
	return cal.get(Calendar.DAY_OF_MONTH)+" "+MONTH[month]+" "+cal.get(Calendar.YEAR);
  }


  //public static String prettyMillis(long millis) {}

  /** Formats <var>sec</var>onds as <code>days/HH:MM:SS</code>. */	// d h m s more understandable than colons?
  public static String prettySeconds(int sec) {
	int mins = sec/60; sec -= mins*60;
	int hours = mins/60; mins -= hours*60;
	int days = hours/24; hours -= days*24;
	//int months = days/30;
	//int years = days / 365;

	StringBuffer sb = new StringBuffer(100);
	boolean ffill = false;
	//if (hours>0) sb.append(hours).append(" hour").append(hours>1? "s": "");
	if (days>0) { sb.append(days).append("/"); ffill=true; }
	if (hours>0) { sb.append(hours).append(":"); ffill=true; }
	if (mins>0 || ffill) { sb.append(mins<10 && ffill? "0": "").append(mins).append(":"); ffill=true; }
	/*if (true)*/ sb.append(sec<10 && ffill? "0": "").append(sec);

	return sb.toString();
  }
}
