package uk.ac.liverpool.thumbnails;

public class FontInformation implements Comparable<FontInformation> {

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FontInformation) {
            FontInformation n = (FontInformation) obj;
            return  ( fontName.equals(n.fontName)  &&  (fontType.equals(n.fontType)) );
        }
        return false;
    }
    @Override
    public int hashCode() {
        return fontName.hashCode() + fontType.hashCode();
    } 
    public String getFontName() {
        return fontName;
    }
    public void setFontname(String fontname) {
        this.fontName = fontname;
    }
    public String getCharset() {
        return charset;
    }
    public void setCharset(String charset) {
        this.charset = charset;
    }
    public String getFontType() {
        return fontType;
    }
    public void setFontType(String fontType) {
        this.fontType = fontType;
    }

    public String fontName;
    public String charset;
    public String fontType;
    public int fontFlags;
    public boolean isEnbedded;
    public boolean isSubset;
    public boolean isToUnicode;
    public  String pitchAndFamily;
    public String fontFamily;
    public String fontStretch;
    public float fontWeight;
    public int numGlyph;
    public String getPitchAndFamily() {
        return pitchAndFamily;
    }
    public void setPitchAndFamily(String pitchAndFamily) {
        this.pitchAndFamily = pitchAndFamily;
    }
    @Override
    public int compareTo(FontInformation o2) {
        int a = fontName.compareTo(o2.fontName);
        if (a !=0)
        return a;
        else 
            return fontType.compareTo(o2.fontType);
    }

}
