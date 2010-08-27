/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hslf.model;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * Paint a shape into java.awt.Graphics2D
 *
 * @author Yegor Kozlov
 */
public final class ShapePainter {
	protected static POILogger logger = POILogFactory.getLogger(ShapePainter.class);

	public static void paint(SimpleShape shape, Graphics2D graphics){
		Rectangle2D anchor = shape.getLogicalAnchor2D();

		
		java.awt.Shape outline = shape.getOutline();
		//System.out.println(shape.getShapeName());

		//flip vertical
		if(shape.getFlipVertical()){
			graphics.translate(anchor.getX(), anchor.getY() + anchor.getHeight());
			graphics.scale(1, -1);
			graphics.translate(-anchor.getX(), -anchor.getY());
		}
		//flip horizontal
		if(shape.getFlipHorizontal()){
			graphics.translate(anchor.getX() + anchor.getWidth(), anchor.getY());
			graphics.scale(-1, 1);
			graphics.translate(-anchor.getX() , -anchor.getY());
		}

		//rotate transform
		double angle = shape.getRotation();

		if(angle != 0){
			double centerX = anchor.getX() + anchor.getWidth()/2;
			double centerY = anchor.getY() + anchor.getHeight()/2;

			graphics.translate(centerX, centerY);
			graphics.rotate(Math.toRadians(angle));
			graphics.translate(-centerX, -centerY);
		}

		//fill
		Color fillColor = shape.getFill().getForegroundColor();
		if (fillColor != null) {
			//TODO: implement gradient and texture fill patterns
			graphics.setPaint(fillColor);
			graphics.fill(outline);
		}

		//border
		Color lineColor = shape.getLineColor();
		if (lineColor != null){
			graphics.setPaint(lineColor);
			float width = (float)shape.getLineWidth();
			if (width == 0)
				width = 0.01f;

			int dashing = shape.getLineDashing();
			//TODO: implement more dashing styles
			float[] dashptrn = null;
			switch(dashing){
			case Line.PEN_SOLID:
				dashptrn = null;
				break;
			case Line.PEN_PS_DASH:
				dashptrn = new float[]{width, width};
				break;
			case Line.PEN_DOTGEL:
				dashptrn = new float[]{width*4, width*3};
				break;
			default:
				logger.log(POILogger.WARN, "unsupported dashing: " + dashing);
				dashptrn = new float[]{width, width};
				break;
			}
			Stroke old = graphics.getStroke();
			Stroke stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashptrn, 0.0f);
			graphics.setStroke(stroke);
			if (shape instanceof Line) {
				Line l = (Line) shape;
				int as = l.getLineStartArrowHead();
				int ae = l.getLineEndArrowHead();
				int i1, i2;
				if (as >0) {
					//stroke = new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dashptrn, 0.0f);
					//					Line2D.Double(anchor.getX(), anchor.getY(), anchor.getX() + anchor.getWidth(), anchor.getY() + anchor.getHeight());
					i1 = l.getLineStartArrowLenght();
					i2 = l.getLineStartArrowWidth();
					if (i1==0)
						i1 = i2;
					if (i2==0)
						i2 = i1;
					drawArrow(graphics, (int)(anchor.getX() + anchor.getWidth()), (int)(anchor.getY() + anchor.getHeight()),(int)anchor.getX(), (int)anchor.getY(), width, i1, i2);

				}
				if (ae >0) {
					//					Line2D.Double(anchor.getX(), anchor.getY(), anchor.getX() + anchor.getWidth(), anchor.getY() + anchor.getHeight());
					i1 = l.getLineEndArrowLenght();
					i2 = l.getLineEndArrowWidth();
					if (i1==0)
						i1 = i2;
					if (i2==0)
						i2 = i1;
					drawArrow(graphics, (int)anchor.getX(), (int)anchor.getY(), (int)(anchor.getX() + anchor.getWidth()), (int)(anchor.getY() + anchor.getHeight()), width, i1, i2);

				}

			}
			else {
				//graphics.setStroke(stroke);
				if (shape instanceof AutoShape){
					if (((AutoShape) shape)._txtrun!=null){
						graphics.setStroke(old);
						return;
					}
						
				}
				graphics.draw(outline);
			            if (shape instanceof AutoShape){
			                AutoShape as = (AutoShape)  shape;
			                
			                    graphics.drawString(as.getShapeName(),(float) anchor.getCenterX(), (float)anchor.getCenterY());
			                    
			            
			            }
				//System.out.println("Drawshape " + outline);
				graphics.setStroke(old);


			}
		}



	}
	private static int yCor(int len, double dir) {
		return (int) (len * Math.cos(dir));
	}

	private static int xCor(int len, double dir) {
		return (int) (len * Math.sin(dir));
	}
	public static void drawArrow(Graphics2D g2d, int xCenter, int yCenter,
			int x, int y, float stroke, int i1, int i2) {
		double aDir = Math.atan2(xCenter - x, yCenter - y);
		Stroke s = g2d.getStroke();

		Polygon tmpPoly = new Polygon();
		if (i1 == 0) {
			i1 = 8 + (int) (stroke );
			i2 = 6 + (int) stroke;
		} else {
			i1*=stroke+2;
			i2*=stroke+1;
		}
		g2d.drawLine(x + xCor(i1-3, aDir ), y + yCor(i1-3, aDir ), xCenter, yCenter);
		//System.out.printf("%d %d %d %d \n", x + xCor(i1, aDir ), y + yCor(i1, aDir ), xCenter, yCenter);
		g2d.setStroke(new BasicStroke(1));

		tmpPoly.addPoint(x, y);
		tmpPoly.addPoint(x + xCor(i1, aDir + .5), y + yCor(i1, aDir + .5));
		tmpPoly.addPoint(x + xCor(i2, aDir), y + yCor(i2, aDir));
		tmpPoly.addPoint(x + xCor(i1, aDir - .5), y + yCor(i1, aDir - .5));
		tmpPoly.addPoint(x, y); 
		g2d.drawPolygon(tmpPoly);
		g2d.fillPolygon(tmpPoly);
		g2d.setStroke(s);
	}
}
