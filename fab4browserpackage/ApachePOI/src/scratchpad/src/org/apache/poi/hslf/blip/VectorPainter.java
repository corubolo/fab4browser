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

package org.apache.poi.hslf.blip;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.apache.batik.transcoder.wmf.tosvg.WMFPainter;
import org.apache.batik.transcoder.wmf.tosvg.WMFRecordStore;
import org.apache.poi.hslf.model.Picture;
import org.apache.poi.hslf.usermodel.PictureData;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.freehep.graphicsio.emf.EMFInputStream;
import org.freehep.graphicsio.emf.EMFRenderer;

import fi.faidon.jvg.PICTReader;

/**
 * Creates BufferedImage using javax.imageio.ImageIO and draws it in the specified graphics.
 *
 * @author  Yegor Kozlov.
 */
public final class VectorPainter implements ImagePainter {
	private static boolean noChace = false;

	protected POILogger logger = POILogFactory.getLogger(this.getClass());

	protected static Map<String, SoftReference<Image>> cache_ = new HashMap<String, SoftReference<Image>>(
			13);


	public static void clearCache(){
		for (SoftReference<Image> i:cache_.values())
			i.clear();
		cache_.clear();
	}
	public void paint(Graphics2D graphics, PictureData pict, Picture parent) {
		Image scaledImg = null;
		Rectangle anchor = parent.getAnchor();
		SoftReference<Image> ref = cache_.get(BitmapPainter.stringID(pict.getUID()));


		if (ref != null && ref.get() != null) {
			scaledImg = ref.get();
		} else {
			try {
				if (pict.getType() == Picture.PICT) {
					System.out.println("Vector image: PICT");
					PICTReader p = new PICTReader(new ByteArrayInputStream(pict.getData()));
					if (noChace){

						RenderingHints rh = graphics.getRenderingHints();
						AffineTransform a = graphics.getTransform();
						graphics.scale(((double)anchor.width)/((double)p.getWidth())  , ((double)anchor.height)/((double)p.getHeight()));
						graphics.translate(anchor.x, anchor.y);

						graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
						graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
						p.playIt(graphics);
						graphics.setRenderingHints(rh);
						graphics.setTransform(a);
						return;
					} else {

						scaledImg = new BufferedImage(anchor.width, anchor.height, BufferedImage.TYPE_INT_ARGB);
						Graphics2D g = ((BufferedImage)scaledImg).createGraphics();
						g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
						g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

						g.scale(((double)anchor.width)/((double)p.getWidth())  , ((double)anchor.height)/((double)p.getHeight()));
						p.playIt(g);
						
						g.dispose();
					}
				}
				else if (pict.getType() == Picture.EMF) {
					System.out.println("Vector image: EMF");
					EMFInputStream emf = new EMFInputStream(new ByteArrayInputStream(pict.getData()));
					EMFRenderer renderer = new EMFRenderer(emf);
					scaledImg = new BufferedImage(anchor.width, anchor.height, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = ((BufferedImage)scaledImg).createGraphics();
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
					Dimension p = renderer.getSize();
					g.scale(((double)anchor.width)/((double)p.getWidth())  , ((double)anchor.height)/((double)p.getHeight()));
					renderer.paint(g);

					g.dispose();
				} else if (pict.getType() == Picture.WMF) {
					System.out.println("Vector image: WMF");

					WMFRecordStore currentStore = new WMFRecordStore();
					currentStore.read(new DataInputStream(new ByteArrayInputStream(pict.getData())));
					Rectangle2D o = currentStore.getRectanglePixel();
					double rx = ((double)anchor.width)/((double)o.getWidth());

					// Build a painter for the RecordStore
					WMFPainter painter = new WMFPainter(currentStore,(int)(o.getX()),(int)(o.getY()),(float)rx);
					scaledImg = new BufferedImage(anchor.width, anchor.height, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = ((BufferedImage)scaledImg).createGraphics();
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
					painter.paint(g);

					g.dispose();
				}
				cache_.put(BitmapPainter.stringID(pict.getUID()), new SoftReference<Image>(scaledImg));
			}
			catch (Exception e){
				e.printStackTrace();
				logger.log(POILogger.WARN, "ImageIO failed to create image. image.type: " + pict.getType());
				return;
			}
		}

		graphics.drawImage(scaledImg, anchor.x, anchor.y, null);


	}

}
