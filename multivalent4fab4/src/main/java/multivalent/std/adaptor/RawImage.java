/*
 * Copyright (C) 2006 Tom Phelps / Practical Thought
 *
 * Modifications are Copyright (C) 2008 Fabio Corubolo - The University of
 * Liverpool
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package multivalent.std.adaptor;

import java.awt.Color;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import multivalent.Behavior;
import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.IDInfo;
import multivalent.IDInfo.Confidence;
import multivalent.INode;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.StyleSheet;
import multivalent.node.LeafImage;
import multivalent.std.MediaLoader;
import phelps.awt.Colors;
import uk.ac.liverpool.fab4.ImageInternalDataFrame;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.metadata.iptc.IptcDirectory;

import static multivalent.IDInfo.Confidence.*;
import static multivalent.IDInfo.Confidence.HEURISTIC;
import static multivalent.IDInfo.Confidence.PARSE;
import static multivalent.IDInfo.Confidence.SUFFIX;

@SuppressWarnings({"UnusedDeclaration"})
public class RawImage extends MediaAdaptor {

    protected static Map<URI, SoftReference<BufferedImage>> cache =
            new HashMap<URI, SoftReference<BufferedImage>>(20);

    private static final Map<String, String> IMAGE_SUFFIXES;

    static {
        IMAGE_SUFFIXES = new HashMap<String, String>();
        IMAGE_SUFFIXES.put("art", "image/x-jg");
        IMAGE_SUFFIXES.put("bmp", "image/x-ms-bmp");
        IMAGE_SUFFIXES.put("cgm", "image/x-cgm");
        IMAGE_SUFFIXES.put("fit", "image/x-fits");
        IMAGE_SUFFIXES.put("fits", "image/x-fits");
        IMAGE_SUFFIXES.put("fts", "image/x-fits");
        IMAGE_SUFFIXES.put("gif", "image/gif");
        IMAGE_SUFFIXES.put("hpg", "image/x-hpgl");
        IMAGE_SUFFIXES.put("hpgl", "image/x-hpgl");
        IMAGE_SUFFIXES.put("ief", "image/ief");
        IMAGE_SUFFIXES.put("jpe", "image/jpeg");
        IMAGE_SUFFIXES.put("jpeg", "image/jpeg");
        IMAGE_SUFFIXES.put("jpg", "image/jpeg");
        IMAGE_SUFFIXES.put("pbm", "image/x-portable-bitmap");
        IMAGE_SUFFIXES.put("pcd", "image/x-photo-cd");
        IMAGE_SUFFIXES.put("pct", "image/pict");
        IMAGE_SUFFIXES.put("pcx", "image/x-pcx");
        IMAGE_SUFFIXES.put("pgm", "image/x-portable-graymap");
        IMAGE_SUFFIXES.put("pic", "image/pict");
        IMAGE_SUFFIXES.put("pict", "image/pict");
        IMAGE_SUFFIXES.put("png", "image/png");
        IMAGE_SUFFIXES.put("png", "image/x-png");
        IMAGE_SUFFIXES.put("pnm", "image/x-portable-anymap");
        IMAGE_SUFFIXES.put("ppm", "image/x-portable-pixmap");
        IMAGE_SUFFIXES.put("ras", "image/cmu-raster");
        IMAGE_SUFFIXES.put("ras", "image/x-cmu-raster");
        IMAGE_SUFFIXES.put("rgb", "image/x-rgb");
        IMAGE_SUFFIXES.put("tif", "image/tiff");
        IMAGE_SUFFIXES.put("tiff", "image/tiff");
        IMAGE_SUFFIXES.put("xbm", "image/x-xbitmap");
        IMAGE_SUFFIXES.put("xpm", "image/x-xpixmap");
        IMAGE_SUFFIXES.put("xwd", "image/x-xwindowdump");
    }

    @Override
    public LeafImage parse(INode parent) throws Exception {
        Document doc = parent.getDocument();
        if (doc.getFirstChild() != null)
            doc.clear();

        StyleSheet ss = doc.getStyleSheet();
        CLGeneral gs = new CLGeneral();
        gs.setForeground(Colors.getColor(getAttr("foreground"), Color.BLACK));
        gs.setBackground(Colors.getColor(getAttr("background"), Color.WHITE));

        gs.setPadding(8);
        ss.put(doc.getName(), gs);

        BufferedImage original = null;
        SoftReference<BufferedImage> ref = cache.get(doc.getURI());
        if (ref != null && ref.get() != null)
            original = ref.get();
        if (original == null) {
            original = ImageIO.read(getInputUni().getInputStream());
            cache.put(doc.getURI(), new SoftReference<BufferedImage>(original));
        }

        Image i2 = original;
        LeafImage l;
        if (getZoom() != 1.0f) {
            //noinspection ErrorNotRethrown
            try {
                AffineTransformOp pp = new AffineTransformOp(
                        AffineTransform.getScaleInstance(getZoom(), getZoom()),
                        AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                i2 = pp.filter(original, null);

                l = new LeafImage("image", null, parent, i2);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                setZoom(1.0f);
                l = new LeafImage("image", null, parent, original);
            }
        } else {
            l = new LeafImage("image", null, parent, i2);
        }

        File f = MediaLoader.FileCache.get(doc.getURI());
        if (f != null) {
            try {
                Metadata metadata = JpegMetadataReader.readMetadata(f);
                StringBuilder sb = new StringBuilder(5000);

                Directory exifDirectory = metadata.getDirectory(
                        ExifDirectory.class);
                Iterator tags = exifDirectory.getTagIterator();
                if (tags.hasNext())
                    sb.append("EXIF information:\n");
                List<String> ot = new LinkedList<String>();
                while (tags.hasNext()) {
                    Tag tag = (Tag) tags.next();
                    String c;
                    switch (tag.getTagType()) {
                    case ExifDirectory.TAG_DATETIME_ORIGINAL:
                        c = "Date taken";
                        break;

                    case ExifDirectory.TAG_COPYRIGHT:
                        c = "Copyright";
                        break;
                    case ExifDirectory.TAG_IMAGE_DESCRIPTION:
                        c = "Description";
                        break;
                    default:
                        c = null;
                        ot.add(tag.toString());
                        break;
                    }
                    if (c != null) {
                        sb.append(c);
                        sb.append(": ");
                        sb.append(tag.getDescription());
                        sb.append('\n');
                    }
                }

                Directory iptcDir = metadata.getDirectory(IptcDirectory.class);
                tags = iptcDir.getTagIterator();
                if (tags.hasNext())
                    sb.append("\nIPTC information:\n");
                while (tags.hasNext()) {
                    Tag tag = (Tag) tags.next();
                    String c;
                    switch (tag.getTagType()) {
                    case IptcDirectory.TAG_COPYRIGHT_NOTICE:
                        c = "Copyright";
                        break;
                    case IptcDirectory.TAG_CAPTION:
                        c = "Caption";
                        break;
                    case IptcDirectory.TAG_BY_LINE:
                        c = "Author";
                        break;
                    case IptcDirectory.TAG_DATE_CREATED:
                        c = "Date";
                        break;
                    case IptcDirectory.TAG_TIME_CREATED:
                        c = "Time";
                        break;
                    default:
                        c = null;
                        ot.add(tag.toString());
                        break;
                    }
                    if (c != null) {
                        sb.append(c);
                        sb.append(": ");
                        sb.append(tag.getDescription());
                        sb.append('\n');
                    }
                }
                sb.append("\nOther information:\n");
                for (String tt : ot) {
                    sb.append(tt);
                    sb.append('\n');
                }

                Iterator directories = metadata.getDirectoryIterator();
                while (directories.hasNext()) {
                    Directory directory = (Directory) directories.next();
                    tags = directory.getTagIterator();
                    while (tags.hasNext()) {
                        Tag tag = (Tag) tags.next();
                        System.out.println(tag);
                    }
                }
                Layer sc = doc.getLayer(Layer.SCRATCH);
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("text", sb.toString());
                ImageInternalDataFrame ls =
                        (ImageInternalDataFrame) Behavior.getInstance(
                                "ImageInternalDataFrame",
                                "uk.ac.liverpool.fab4.ImageInternalDataFrame",
                                null, m, sc);
                ls.setTitle("Image Metadata");
                ls.setTransparent(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Layer ll = doc.getLayer(Layer.PERSONAL);
            if (ll != null)
                ll.destroy();
        }
        return l; // constructed image
    }

    @Override
    public SortedSet<IDInfo> getTypeInfo(Confidence min, Confidence max,
            String path, boolean complete) throws IOException {

        SortedSet<IDInfo> infos = validateParams(min, max);

        if (inRange(min, MAGIC, max) || inRange(min, PARSE, max)) {
            ImageInputStream stream = new MemoryCacheImageInputStream(
                    getInputUni().getInputStreamRaw());
            IIORegistry iio = IIORegistry.getDefaultInstance();

            boolean quick = !inRange(min, PARSE, max);
            if (!quick)
                stream.mark();
            Iterator<ImageReader> it = ImageIO.getImageReaders(stream);
            if (!quick)
                stream.reset();

            IDInfo info = null;
            while (it.hasNext()) {
                boolean success = true;
                ImageReader reader = it.next();
                if (!quick) {
                    ImageReadParam param = reader.getDefaultReadParam();
                    reader.setInput(stream, true, true);
                    try {
                        BufferedImage img = reader.read(0, param);
                    } catch (Exception ignored) {
                        success = false;
                    } finally {
                        reader.dispose();
                        stream.close();
                    }
                }
                if (success) {
                    ImageReaderSpi spi = reader.getOriginatingProvider();
                    String type = spi.getMIMETypes()[0];
                    String desc = spi.getFormatNames()[0];
                    Confidence level = quick ? MAGIC : PARSE;
                    infos.add(new IDInfo(level, this, type, null, desc));
                    break;
                }
            }
        } else if (inRange(min, SUFFIX, max)) {
            String type = lookupSuffix(path, IMAGE_SUFFIXES);
            if (type != null)
                infos.add(new IDInfo(SUFFIX, this, type));
        }

        return infos;
    }
}
