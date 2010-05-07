package multivalent.std;

import java.awt.*;
import java.awt.Graphics;
/*
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Polygon;
import java.awt.Image;
*/
import java.awt.image.ImageObserver;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator;
import java.util.Map;



/**
	Some, but not many, kinds of Lenses are easiest to implement as subclasses of Graphics,
	as remarked in "Debugging Lenses", UIST 97, Hudson/Rodenstein/Smith,
	but every method of java.awt.Graphics2D is abstract; GraphicsWrap provides a conveniently subclassable Graphics2D.

	@see multivalent.std.lens.Cypher
<!--
	x@see berkeley.lens.Shadow
-->

	@version $Revision: 1.2 $ $Date: 2002/02/01 04:07:12 $
*/
public abstract class GraphicsWrap extends Graphics2D {
  protected Graphics2D wrapped_;

  protected GraphicsWrap(Graphics2D wrapped) { super(); wrapped_=wrapped; }
  /** Subclasses should define as something like "return new <subclassname>((Graphics2D)wrapped_.create());". */
  public abstract Graphics create(); // { return wrapped_.create(); }
  //public Graphics create(int x, int y, int width, int height) { return wrapped_.create(x,y,width,height); }
  public void translate(int x, int y) { wrapped_.translate(x,y); }
  public Color getColor() { return wrapped_.getColor(); }
  public void setColor(Color c) { wrapped_.setColor(c); }
  public void setPaintMode() { wrapped_.setPaintMode(); }
  public void setXORMode(Color c1) { wrapped_.setXORMode(c1); }
  public Font getFont() { return wrapped_.getFont(); }
  public void setFont(Font font) { wrapped_.setFont(font); }
  public FontMetrics getFontMetrics() { return wrapped_.getFontMetrics(); } //co?
  public FontMetrics getFontMetrics(Font f) { return wrapped_.getFontMetrics(f); }
// deprecated	 public Rectangle getClipRect() { return wrapped_.getClipRect(); } //co?
  public Rectangle getClipBounds() { return wrapped_.getClipBounds(); }
  //Rectangle getClipBounds(Rectangle r) //co?
  public Shape getClip() { return wrapped_.getClip(); }
  public void clipRect(int x, int y, int width, int height) { wrapped_.clipRect(x,y,width,height); }
  //boolean hitClip(int x, int y, int width, int height) { wrapped_.hitClip(x,y, width,height); } //co?
  public void setClip(int x, int y, int width, int height) { wrapped_.setClip(x,y,width,height); }
  public void setClip(Shape clip) { wrapped_.setClip(clip); }
  public void copyArea(int x, int y, int width, int height, int dx, int dy) { wrapped_.copyArea(x,y, width,height, dx,dy); }
  public void drawLine(int x1, int y1, int x2, int y2) { wrapped_.drawLine(x1,y1, x2,y2); }
  public void drawPolyline(int xPoints[], int yPoints[], int nPoints) { wrapped_.drawPolyline(xPoints, yPoints, nPoints); }
  public void fillRect(int x, int y, int width, int height) { wrapped_.fillRect(x,y, width,height); }
  //public void drawRect(int x, int y, int width, int height) { wrapped_.drawRect(x,y, width, height); } //co?
  public void clearRect(int x, int y, int width, int height) { wrapped_.clearRect(x,y, width,height); }
  public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { wrapped_.drawRoundRect(x,y, width,height, arcWidth,arcHeight); }
  public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { wrapped_.fillRoundRect(x,y, width,height, arcWidth,arcHeight); }
  //public void draw3DRect(int x, int y, int width, int height, boolean raised) { wrapped_.draw3DRect(x,y, width,height, raised); } //co?
  //public void fill3DRect(int x, int y, int width, int height, boolean raised) { wrapped_.draw3DRect(x,y, width,height, raised); } //co?
  public void drawOval(int x, int y, int width, int height) { wrapped_.drawOval(x,y, width,height); }
  public void fillOval(int x, int y, int width, int height) { wrapped_.fillOval(x,y, width,height); }
  public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) { wrapped_.drawArc(x,y, width,height, startAngle,arcAngle); }
  public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) { wrapped_.fillArc(x,y, width,height, startAngle,arcAngle); }
  public void drawPolygon(int xPoints[], int yPoints[], int nPoints) { wrapped_.drawPolygon(xPoints, yPoints, nPoints); }
  //public void drawPolygon(Polygon p) { wrapped_.drawPolygon(p); } //co?
  public void fillPolygon(int xPoints[], int yPoints[], int nPoints) { wrapped_.fillPolygon(xPoints, yPoints, nPoints); }
  //public void fillPolygon(Polygon p) { wrapped_.fillPolygon(p); } //co?
  public void drawString(String str, int x, int y) { wrapped_.drawString(str, x,y); }
  public void drawString(AttributedCharacterIterator iterator, int x, int y) { wrapped_.drawString(iterator, x, y) ; }
  //public void drawChars(char data[], int offset, int length, int x, int y) { wrapped_.drawChars(data,offset,length, x,y); } //co?
  //public void drawBytes(byte data[], int offset, int length, int x, int y) { wrapped_.drawBytes(data,offset,length, x,y); } //co?
  public boolean drawImage(Image img, int x, int y, ImageObserver observer) { return wrapped_.drawImage(img, x,y, observer); }
  public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) { return wrapped_.drawImage(img, x,y, width,height, observer); }
  public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) { return wrapped_.drawImage(img, x,y, bgcolor, observer); }
  public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) { return wrapped_.drawImage(img, x,y, width,height, bgcolor, observer); }
  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) { return wrapped_.drawImage(img, dx1,dy1,dx2,dy2, sx1,sy1,sx2,sy2, observer); }
  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) { return wrapped_.drawImage(img, dx1,dy1,dx2,dy2, sx1,sy1,sx2,sy2, bgcolor, observer); }

  public void dispose() { wrapped_.dispose(); }
  //public void finalize() { wrapped_.finalize(); } //co?
  //public String toString() { return wrapped_.toString(); } //co?


  public void addRenderingHints(Map hints) { wrapped_.addRenderingHints(hints); }
  public void clip(Shape s) { wrapped_.clip(s); }
  public void draw(Shape s) { wrapped_.draw(s); }
  //public void draw3DRect(int x, int y, int width, int height, boolean raised) { wrapped_.draw3DRect(x,y, width,height, raised); } //co?
  public void drawGlyphVector(GlyphVector g, float x, float y) { wrapped_.drawGlyphVector(g, x,y); }
  public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) { wrapped_.drawImage(img, op, x,y); }
  public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) { return wrapped_.drawImage(img, xform, obs); }
  public void drawRenderableImage(RenderableImage img, AffineTransform xform) { wrapped_.drawRenderableImage(img, xform); }
  public void drawRenderedImage(RenderedImage img, AffineTransform xform) { wrapped_.drawRenderedImage(img, xform); }
  public void drawString(AttributedCharacterIterator iterator, float x, float y) { wrapped_.drawString(iterator, x,y); }
  //public void drawString(AttributedCharacterIterator iterator, int x, int y) { wrapped_.drawString(iterator, x,y); } //co?
  public void drawString(String s, float x, float y) { wrapped_.drawString(s, x,y); }
  //public void drawString(String str, int x, int y) { wrapped_.drawString(str, x,y); }
  public void fill(Shape s) { wrapped_.fill(s); }
  //public void fill3DRect(int x, int y, int width, int height, boolean raised) { wrapped_.fill3DRect(x,y, width,height, raised); } //co?
  public Color getBackground() { return wrapped_.getBackground(); }
  public Composite getComposite() { return wrapped_.getComposite(); }
  public GraphicsConfiguration getDeviceConfiguration() { return wrapped_.getDeviceConfiguration(); }
  public FontRenderContext getFontRenderContext() { return wrapped_.getFontRenderContext(); }
  public Paint getPaint() { return wrapped_.getPaint(); }
  public Object getRenderingHint(RenderingHints.Key hintKey) { return wrapped_.getRenderingHint(hintKey); }
  public RenderingHints getRenderingHints() { return wrapped_.getRenderingHints(); }
  public Stroke getStroke() { return wrapped_.getStroke(); }
  public AffineTransform getTransform() { return wrapped_.getTransform(); }
  public boolean hit(Rectangle rect, Shape s, boolean onStroke) { return wrapped_.hit(rect, s, onStroke); }
  public void rotate(double theta) { wrapped_.rotate(theta); }
  public void rotate(double theta, double x, double y) { wrapped_.rotate(theta, x,y); }
  public void scale(double sx, double sy) { wrapped_.scale(sx,sy); }
  public void setBackground(Color color) { wrapped_.setBackground(color); }
  public void setComposite(Composite comp) { wrapped_.setComposite(comp); }
  public void setPaint(Paint paint) { wrapped_.setPaint(paint); }
  public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) { wrapped_.setRenderingHint(hintKey, hintValue); }
  public void setRenderingHints(Map hints) { wrapped_.setRenderingHints(hints); }
  public void setStroke(Stroke s) { wrapped_.setStroke(s); }
  public void setTransform(AffineTransform Tx) { wrapped_.setTransform(Tx); }
  public void shear(double shx, double shy) { wrapped_.shear(shx,shy); }
  public void transform(AffineTransform Tx) { wrapped_.transform(Tx); }
  public void translate(double tx, double ty) { wrapped_.translate(tx,ty); }
  //public void translate(int x, int y) { wrapped_.translate(x,y); } //co?

  public String toString() { return "GraphicsWrap"; }
}
