package phelps.awt;

import java.awt.Color;
import java.util.Map;
import java.util.HashMap;



/**
	Color management.

<!--
	{@link #getColor(String)},
	{@link #getColor(String, Color)}
	<li>table management: {@link #addColor(String, Object)}
-->

	@version $Revision: 1.2 $ $Date: 2003/06/01 08:00:03 $
*/
public class Colors {
  /**
	Canonical transparent color.
	Use <code>==</code>, not <code>equals()</code>.
  */
  public static final Color TRANSPARENT = new Color(0,0,0, 0);


  static final Color[] sRGB = {
	Color.BLACK, new Color(0,0x80,0), new Color(0xC0,0xC0,0xC0), Color.GREEN,
	Color.GRAY, new Color(0x80,0x80,0), Color.WHITE, Color.YELLOW,
	new Color(0x80,0,0), new Color(0,0,0x80), Color.RED, Color.BLUE,
	new Color(0x80,0,0x80), new Color(0,0x80,0x80), new Color(0xff,0,0xff), new Color(0,0xff,0xff)
  };
  static final String[] sRGBNAME = {
	"Black", "Green", "Silver", "Lime",
	"Gray", "Olive", "White", "Yellow",
	"Maroon", "Navy", "Red", "Blue",
	"Purple", "Teal", "Fuchsia", "Aqua"
  };

  private static Map<String,Color> string2color = new HashMap<String,Color>(50);
  private static Map<Color,String> color2string = new HashMap<Color,String>(50);
  static {
	// built-in Java colors
	addColor("Black",Color.BLACK); addColor("White",Color.WHITE); addColor("Blue",Color.BLUE); addColor("Cyan",Color.CYAN);
	addColor("DarkGray",Color.DARK_GRAY); addColor("Gray",Color.GRAY); addColor("Green",Color.GREEN); addColor("LightGray",Color.LIGHT_GRAY);
	addColor("Magenta",Color.MAGENTA); addColor("Orange",Color.ORANGE); addColor("Pink",Color.PINK); addColor("Red",Color.RED);
	addColor("Yellow",Color.YELLOW);

//	/*addColor("Black","#000000");*/ /*addColor("Green","#008000");*/ addColor("Silver","#C0C0C0"); addColor("Lime","#00FF00");
//	/*addColor("Gray","#808080");*/ addColor("Olive","#808000"); /*addColor("White","#FFFFFF");*/ /*addColor("Yellow", #FFFF00");*/
//	addColor("Maroon","#800000"); addColor("Navy","#000080"); /*addColor("Red","#FF0000");*/ /*addColor("Blue","#0000FF");*/
//	addColor("Purple","#800080"); addColor("Teal","#008080"); addColor("Fuchsia","#FF00FF"); addColor("Aqua","#00FFFF");

	// override with sRGB
	for (int i=0,imax=sRGB.length; i<imax; i++) addColor(sRGBNAME[i], sRGB[i]);
  }

  private Colors() {}


  /** Put color in system list, which some behaviors use in menus. */
  public static void addColor(String n, Object o) {
	Color c=null;
	if (o instanceof Color) c=(Color)o;
	else c = new Color(Integer.parseInt(((String)o).substring(1),16));
	string2color.put(n.toLowerCase(), c);
	color2string.put(c, n/*not .toLowerCase()*/);
  }
  // color name => color.  later rgb values et cetera
  /*
  public static Color name2Color(String name) {
	if (name==null) return null;
	Color color = (Color)string2color.get(name.toLowerCase());
	if (color==null && name.startsWith("#")) {
		try {
			int val = Integer.parseInt(name.substring(1),16), r,g,b;
			color = new Color(r,g,b);
		} catch (NumberFormatException nfe) {}
	}
	return color;
  }*/

  public static String getName(Color c) {
	if (c==null) return null;
	String name = (String)color2string.get(c);
	if (name==null) { name = "#" + Integer.toHexString(c.getRGB()).substring(2)/*skip alpha*/; }
	//Integer.toString(c.getRed(),16)+Integer.toString(c.getGreen(),16)+Integer.toString(c.getBlue(),16); }
	return name;
  }

  public static Color getColor(String spec) { return getColor(spec, null); }
  /** Retrieve color by name (from sRGB set: "red", "maroon", ...) or create new color by hex code ("#ffffff", "#900"). */
  public static Color getColor(String spec, Color defaultColor) {
	Color newcolor = null;
	if (spec==null || spec.indexOf("default")!=-1) newcolor=defaultColor;	// "default", "(default)", ...
	else if ("transparent".equalsIgnoreCase(spec)) newcolor=null;   // => public static Color TRANSPARENT = new Color(0, 0,0,0);
	else {
		// named ("red", "maroon")
		if (!spec.startsWith("#")) newcolor = (Color)string2color.get(spec.toLowerCase());
//if (!spec.startsWith("#")) System.out.println("*********name "+spec+" => "+newcolor);

		// hex ("#ff0000", "#900") -- ok if missing leading hash
		/*no else*/ if (newcolor==null) {	// color doesn't exist or forget leading "#"
			int val=-1, r,g,b, off=(spec.startsWith("#")? 1: 0);
			if (spec.length()>=6+off) try {
				val = Integer.parseInt(spec.substring(0+off,6+off),16);
				r=(val&0xff0000)>>16; g=(val&0xff00)>>8; b=(val&0xff);
				newcolor = new Color(r,g,b);
			} catch (NumberFormatException nfe) {}
			if (newcolor==null && spec.length()>=3+off) try {
				val = Integer.parseInt(spec.substring(0+off,3+off),16);
				r=(val&0xf00)>>4; g=(val&0xf0); b=(val&0xf)<<4;
				newcolor = new Color(r,g,b);
			} catch (NumberFormatException nfe2) {}
		}
		if (newcolor==null) newcolor=defaultColor;
	}
//System.out.println("Colors.getColor "+spec+" => "+newcolor);    //string2color.get(spec.toLowerCase()));
	return newcolor;
  }
}
