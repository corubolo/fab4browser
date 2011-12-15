package uk.ac.liverpool.media.jcodecma;



import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import multivalent.CLGeneral;
import multivalent.Document;
import multivalent.INode;
import multivalent.Layer;
import multivalent.MediaAdaptor;
import multivalent.StyleSheet;
import phelps.awt.Colors;


public class LeafVideoJCodecMA extends MediaAdaptor {

    LeafVideoJCodec l;
	Document doc;

	public void close() throws IOException {
		
		if (l!=null)
			l.close();
		l = null;
		//doc.removeAttr(TimedMedia.TIMEDMEDIA);
		super.close();
	}
	
	public Object parse(INode parent) throws Exception {
		System.out.println("** Jcodec VIDEO media adaptor");
		 doc = parent.getDocument();
		if (doc.getFirstChild() != null)
			doc.clear();
		final StyleSheet ss = doc.getStyleSheet();
		CLGeneral gs = new CLGeneral();
		gs.setForeground(Colors.getColor(getAttr("foreground"), Color.WHITE));
		gs.setBackground(Colors.getColor(getAttr("background"), Color.BLACK));
		gs.setPadding(8);
		ss.put(doc.getName(), gs);
		Map<String, Object> attr = new HashMap<String, Object>(1);
		attr.put("resize", true);
		attr.put("embedded", false);
		attr.put("uri", getURI().toString());
		doc.uri = getURI();
		if (getURI() == null){
			//new LeafUnicode("File not found",attr,parent);
			throw new IOException("File not found");
		}

		l =  new LeafVideoJCodec("video", attr, parent);
		
		//Try to display meta-data
		/*
		if (l != null ) {
			try {
				Metadata metadata = VideoMetadataReader.readMetadata(doc.uri);
				StringBuffer sb = new StringBuffer(5000);

				Directory exifDirectory = metadata
						.getDirectory(ExifDirectory.class);
				Iterator tags = exifDirectory.getTagIterator();
				if (tags.hasNext())
					sb.append("EXIF information:\n");
				List<String> ot = new LinkedList<String>();
				while (tags.hasNext()) {
					Tag tag = (Tag) tags.next();
					String c = "";
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
					String c = "";
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
				ImageInternalDataFrame ls = (ImageInternalDataFrame) Behavior
						.getInstance("ImageInternalDataFrame",
								"uk.ac.liverpool.fab4.ImageInternalDataFrame",
								null, m, sc);
				ls.setTitle("Image Metadata");
				ls.setTransparent(true);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}*/
			
		//END: Try to display meta-data
		
		
		//l.setStatus(Status.PLAY);
		//System.out.println("AG: Have set Status to PLAY");
		Layer ll = doc.getLayer(Layer.PERSONAL);
		if (ll != null)
			ll.destroy();
		
		
		//doc.putAttr(TimedMedia.TIMEDMEDIA, l);
		
//		Map<String, Object> hm = new HashMap<String, Object>(3);
//		hm.put("signal", "viewOcrAs");
//		hm.put("value", "ocr");
//		hm.put("title", "Show OCR");
//		Behavior.getInstance("TimedMC",
//				"uk.ac.liverpool.fab4.behaviors.TimedMediaControl", null, hm,
//				getCurBr().getRoot().getLayer(Layer.SYSTEM));
		
		return parent;
	}

}
