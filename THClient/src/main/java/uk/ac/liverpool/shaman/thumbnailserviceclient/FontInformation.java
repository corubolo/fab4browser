
package uk.ac.liverpool.shaman.thumbnailserviceclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for fontInformation complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="fontInformation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fontName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="charset" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fontType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fontFlags" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="isEnbedded" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="isSubset" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="isToUnicode" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="pitchAndFamily" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fontFamily" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fontStretch" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fontWeight" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *         &lt;element name="numGlyph" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fontInformation", propOrder = {
    "fontName",
    "charset",
    "fontType",
    "fontFlags",
    "isEnbedded",
    "isSubset",
    "isToUnicode",
    "pitchAndFamily",
    "fontFamily",
    "fontStretch",
    "fontWeight",
    "numGlyph"
})
public class FontInformation {

    protected String fontName;
    protected String charset;
    protected String fontType;
    protected int fontFlags;
    protected boolean isEnbedded;
    protected boolean isSubset;
    protected boolean isToUnicode;
    protected String pitchAndFamily;
    protected String fontFamily;
    protected String fontStretch;
    protected float fontWeight;
    protected int numGlyph;

    /**
     * Gets the value of the fontName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * Sets the value of the fontName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFontName(String value) {
        this.fontName = value;
    }

    /**
     * Gets the value of the charset property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Sets the value of the charset property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCharset(String value) {
        this.charset = value;
    }

    /**
     * Gets the value of the fontType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFontType() {
        return fontType;
    }

    /**
     * Sets the value of the fontType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFontType(String value) {
        this.fontType = value;
    }

    /**
     * Gets the value of the fontFlags property.
     * 
     */
    public int getFontFlags() {
        return fontFlags;
    }

    /**
     * Sets the value of the fontFlags property.
     * 
     */
    public void setFontFlags(int value) {
        this.fontFlags = value;
    }

    /**
     * Gets the value of the isEnbedded property.
     * 
     */
    public boolean isIsEnbedded() {
        return isEnbedded;
    }

    /**
     * Sets the value of the isEnbedded property.
     * 
     */
    public void setIsEnbedded(boolean value) {
        this.isEnbedded = value;
    }

    /**
     * Gets the value of the isSubset property.
     * 
     */
    public boolean isIsSubset() {
        return isSubset;
    }

    /**
     * Sets the value of the isSubset property.
     * 
     */
    public void setIsSubset(boolean value) {
        this.isSubset = value;
    }

    /**
     * Gets the value of the isToUnicode property.
     * 
     */
    public boolean isIsToUnicode() {
        return isToUnicode;
    }

    /**
     * Sets the value of the isToUnicode property.
     * 
     */
    public void setIsToUnicode(boolean value) {
        this.isToUnicode = value;
    }

    /**
     * Gets the value of the pitchAndFamily property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPitchAndFamily() {
        return pitchAndFamily;
    }

    /**
     * Sets the value of the pitchAndFamily property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPitchAndFamily(String value) {
        this.pitchAndFamily = value;
    }

    /**
     * Gets the value of the fontFamily property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFontFamily() {
        return fontFamily;
    }

    /**
     * Sets the value of the fontFamily property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFontFamily(String value) {
        this.fontFamily = value;
    }

    /**
     * Gets the value of the fontStretch property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFontStretch() {
        return fontStretch;
    }

    /**
     * Sets the value of the fontStretch property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFontStretch(String value) {
        this.fontStretch = value;
    }

    /**
     * Gets the value of the fontWeight property.
     * 
     */
    public float getFontWeight() {
        return fontWeight;
    }

    /**
     * Sets the value of the fontWeight property.
     * 
     */
    public void setFontWeight(float value) {
        this.fontWeight = value;
    }

    /**
     * Gets the value of the numGlyph property.
     * 
     */
    public int getNumGlyph() {
        return numGlyph;
    }

    /**
     * Sets the value of the numGlyph property.
     * 
     */
    public void setNumGlyph(int value) {
        this.numGlyph = value;
    }

}
