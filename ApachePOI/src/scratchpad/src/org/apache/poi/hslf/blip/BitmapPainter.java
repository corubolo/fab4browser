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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.apache.poi.hslf.model.Picture;
import org.apache.poi.hslf.usermodel.PictureData;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * Creates BufferedImage using javax.imageio.ImageIO and draws it in the specified graphics.
 *
 * @author  Yegor Kozlov.
 */


public final class BitmapPainter implements ImagePainter {
	protected POILogger logger = POILogFactory.getLogger(this.getClass());
	// This method returns a buffered image with the contents of an image
	protected static Map<String, SoftReference<Image>> cache_ = new HashMap<String, SoftReference<Image>>(
			13);
	
	public static void clearCache(){
		for (SoftReference<Image> i:cache_.values())
			i.clear();
		cache_.clear();

		
	}
	public void paint(Graphics2D graphics, PictureData pict, Picture parent) {
	
		Image scaledImg;
		Rectangle anchor = parent.getAnchor();
		SoftReference<Image> ref = cache_.get(stringID(pict.getUID()));
		
		if (ref != null && ref.get() != null) {
			scaledImg = ref.get();
		} else {
			try {
				BufferedImage img;
				Iterator<ImageReader> ir = ImageIO.getImageReadersBySuffix("jpg");
				img = ImageIO.read(new ByteArrayInputStream(pict.getData()));
//				FileOutputStream fos = new FileOutputStream("img" +stringID(pict.getUID())+".jpg" );
//				fos.write(pict.getData());
//				fos.close();
				scaledImg = img.getScaledInstance(anchor.width, anchor.height, Image.SCALE_SMOOTH);
				cache_.put(stringID(pict.getUID()), new SoftReference<Image>(scaledImg));
				System.out.println("Bitmap image: "+ pict.getType());
			}
			catch (Exception e){
				e.printStackTrace();
				logger.log(POILogger.WARN, "ImageIO failed to create image. image.type: " + pict.getType());
				return;
			}
		}
		
		graphics.drawImage(scaledImg, anchor.x, anchor.y, null);
	}
	public static String stringID(byte[]m){
		StringBuilder b = new StringBuilder();
		for (byte r:m){
			b.append(Integer.toHexString(r));
		}
		return b.toString();
	}

}
