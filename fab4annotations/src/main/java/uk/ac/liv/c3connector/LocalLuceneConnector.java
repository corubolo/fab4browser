/*******************************************************************************
 *
 *  * Copyright (C) 2007, 2010 - The University of Liverpool
 *  * This program is free software; you can redistribute it and/or modify it under the terms
 *  * of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License,
 *  * or (at your option) any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 *  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  * Author: Fabio Corubolo
 *  * Email: corubolo@gmail.com
 * 
 *******************************************************************************/


package uk.ac.liv.c3connector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import uk.ac.liverpool.annotationConnector.AnnotationServerConnectorInterface;


/**
 * Class to implement a local lucene database for the annotations.
 * @author fabio
 *
 */
public class LocalLuceneConnector implements AnnotationServerConnectorInterface {

	private IndexWriter iw;

	private IndexSearcher is;

	private Analyzer sa;

	private FSDirectory dir;

	private AnnotationModelSerialiser ams;

	public static final int MAX_RESULTS = 7000;

	public static final String IDX_DATE_SUBMITTED = "dateSubmitted";
	public static final String IDX_DATE_MODIFIED = "dateModified";
	public static final String IDX_ANNO_ID = "annoId";
	public static final String IDX_URI = "uri";
	public static final String IDX_LEXICAL_SIGNATURE = "lexicalSignature";
	public static final String IDX_CHECKSUM = "checksum";
	public static final String IDX_COSTUM_IDS = "costumId";
	public static final String IDX_APPLICATION = "application";
	public static final String IDX_USER_ID = "userId";
	public static final String IDX_VISIBILITY = "visibility";
	/** the Whole annotation content (envelope and body) */
	public static final String IDX_CONTENT = "content";
	public static final String IDX_BODY = "body";

	/** parses an XML annotation to a lucene Document
	 * @throws Exception */
	public Document luceneDocumentify(String stringDocuemnt) throws Exception {
		AnnotationModel a;

		a = ams.parse(stringDocuemnt);
		if (a== null)
			System.out.println("Null model; error parsing annotation");
		//a.printModel(System.out,	a	);
		Document ret = new Document();
		// dates
		ret.add(new Field(LocalLuceneConnector.IDX_DATE_SUBMITTED, DateTools.timeToString(System
				.currentTimeMillis(), DateTools.Resolution.MINUTE),
				Field.Store.NO, Field.Index.NOT_ANALYZED));
		ret.add(new Field(LocalLuceneConnector.IDX_DATE_MODIFIED, DateTools.timeToString(a
				.getDateModified().getTime(),
				DateTools.Resolution.MINUTE), Field.Store.NO,
				Field.Index.NOT_ANALYZED));
		// ID
		ret.add(new Field(LocalLuceneConnector.IDX_ANNO_ID, a.getId(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		// Resource identifiers
		ret.add(new Field(LocalLuceneConnector.IDX_URI, a.getResourceUri(),
				Field.Store.YES, Field.Index.NOT_ANALYZED));
		if (a.getLexicalSignature() != null)
			ret.add(new Field(LocalLuceneConnector.IDX_LEXICAL_SIGNATURE, a
					.getLexicalSignature(), Field.Store.YES,
					Field.Index.NOT_ANALYZED));
		ret.add(new Field(LocalLuceneConnector.IDX_CHECKSUM, a.getDocumentDigest(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		ret.add(new Field(LocalLuceneConnector.IDX_COSTUM_IDS, a.getDocumentTextDigest(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		ret.add(new Field(LocalLuceneConnector.IDX_APPLICATION, a.getApplication(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		ret.add(new Field(LocalLuceneConnector.IDX_USER_ID, a.getUserid(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		// THE whole content (XML) of the annotation is stored here...
		ret.add(new Field(LocalLuceneConnector.IDX_CONTENT, stringDocuemnt, Field.Store.YES,
				Field.Index.NO));

		// here we build the annotation body - description
		String body = null;
		if (a.getStringDescription()!= null && a.getStringDescription().length() > 0)
			body = a.getStringDescription();
		else if (a.getAnnotationBody() != null) {
			String anno = a.getAnnotationBody();
			StringBuilder cont = new StringBuilder();
			int x = anno.indexOf('>');
			int y = anno.indexOf('<', x);
			while (x >= 0 && y >= 0) {
				String t = anno.substring(x + 1, y).trim();
				if (t.length() > 0) {
					cont.append(t);
					cont.append(' ');
				}
				x = anno.indexOf('>', y);
				y = anno.indexOf('<', x);
			}
			body = cont.toString();

		}

		ret.add(new Field(LocalLuceneConnector.IDX_BODY, body, Field.Store.YES,
				Field.Index.ANALYZED));
		return ret;
	}

	/** deletes the index directory, with all files */
	public void deleteIndexDiretory() {
		try {
			if (is != null)
				is.close();
			if (iw != null)
				iw.close();
			is = null;
			iw = null;
		} catch (IOException e) {
			e.printStackTrace();
		}

		File dir = getIndexDirectory();
		for (File f : dir.listFiles())
			if (f.isFile())
				f.delete();

	}

	/** creates a new lucene database at the given path, or opens it if existing. The database WILL be readonly.
	 * 
	 * @param path
	 * @param readonly
	 */
	public LocalLuceneConnector(File path, boolean readonly) {
		if (is == null)
			try {
				sa = new SimpleAnalyzer();
				dir = FSDirectory.open(path);
				if (IndexWriter.isLocked(dir)) {
					IndexWriter.unlock(dir);
					System.err
					.println("WARNING: I had to force an unlock. somebody else is using the index or unclean close...");
				}
				try {
					// iw = new IndexWriter(dir, true, sa);
					// IndexReader ir = IndexReader.open(dir);
					is = new IndexSearcher(dir, readonly);
				} catch (FileNotFoundException e) {
					// iw.optimize();
					is = new IndexSearcher(dir, readonly);
				}

				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					public void run() {
						try {
							if (iw != null)
								iw.close();
							if (is != null)
								is.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}));
			} catch (Exception e) {
				e.printStackTrace();
				iw = null;
				return;
			}

	}
	/** creates a new lucene database at the given path, or opens it if existing.
	 * 
	 * @param path
	 */
	public LocalLuceneConnector(File path, AnnotationModelSerialiser ams) {
		this.ams = ams;
		if (iw == null)
			try {
				sa = new SimpleAnalyzer();
				dir = FSDirectory.open(path);
				if (IndexWriter.isLocked(dir)) {
					IndexWriter.unlock(dir);
					System.err
					.println("WARNING: I had to force an unlock. somebody else is using the index or unclean close...");
					
				}
				try {
					iw = new IndexWriter(dir, true, sa);
					// IndexReader ir = IndexReader.open(dir);
					is = new IndexSearcher(dir, true);
				} catch (Exception e) {
					e.printStackTrace();

					iw.optimize();
					is = new IndexSearcher(dir, true);
				}
				iw.setMergeFactor(6);
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					public void run() {
						try {
							if (iw != null)
								iw.close();
							if (is != null)
								is.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}));
			} catch (Exception e) {
				e.printStackTrace();
				iw = null;
				return;
			}

	}

	public String[] checksumSearch(String checksum, String method,
			String encoding) {
		return internal_search(LocalLuceneConnector.IDX_CHECKSUM, checksum);
	}

	public String[] customIdSearch(String costomId, String application,
			String method) {
		return internal_search(LocalLuceneConnector.IDX_COSTUM_IDS, costomId);
	}

	public int deleteAnnotation(String id, String user, String secret) {
		try {
			iw.deleteDocuments(new Term(LocalLuceneConnector.IDX_ANNO_ID, id));
			iw.commit();
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -2;
		}
	}

	public void closeIndexes() {
		try {
			is.close();

			iw.optimize();
			iw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			iw = null;
			is = null;
		}
	}

	public File getIndexDirectory() {
		return dir.getFile();
	}

	public void reopenIndexes() {
		if (iw == null && is == null)
			try {
				try {
					iw = new IndexWriter(dir, true, sa);
					// IndexReader ir = IndexReader.open(dir);
					is = new IndexSearcher(dir, true);
				} catch (FileNotFoundException e) {
					iw.optimize();
					is = new IndexSearcher(dir, true);
				}
			} catch (CorruptIndexException e) {
				e.printStackTrace();
			} catch (LockObtainFailedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

	}

	public void optimize() {
		if (iw != null)
			try {
				iw.optimize();
			} catch (CorruptIndexException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public int deleteAnonymousAnnotation(String id, String secretKey) {
		return deleteAnnotation(id, "", secretKey);
	}

	public String[] genericSearch(String query, String type) {
		currentise();
		Hits res;
		try {
			QueryParser parser = new QueryParser(LocalLuceneConnector.IDX_BODY, sa);
			Query q = parser.parse(query);
			res = is.search(q);
			int m = Math.min(LocalLuceneConnector.MAX_RESULTS, res.length());
			if (m == 0)
				return null;
			String[] ret = new String[m];
			for (int i = 0; i < m; i++)
				ret[i] = res.doc(i).get(LocalLuceneConnector.IDX_CONTENT);
			return ret;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getAuthenticationType() {
		return "none";
	}

	public String getIndexingType() {
		return "Lucene";
	}

	public synchronized String getNewIdentifier(String user, String secret) {
		// TODO implement a real UNUQUE id system (counter? maybe just get some
		String s = "s" + user + "-"
		+ Long.toHexString(Math.abs(new Random().nextLong()));
		System.out.println("ID local connector creating ID: " + s);
		// standard implementation)
		return s;
	}

	public String getRepositoryType() {
		return "lucene";
	}

	public String IDSearch(String ID) {
		Hits res;
		currentise();
		try {
			res = is.search(new TermQuery(new Term(LocalLuceneConnector.IDX_ANNO_ID, ID)));
			Iterator<Hit> i = res.iterator();
			if (i.hasNext())
				return i.next().get(LocalLuceneConnector.IDX_CONTENT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void currentise() {
		try {
			if (is != null)
				if (!is.getIndexReader().isCurrent()) {
					is.close();
					is = new IndexSearcher(dir, true);
				}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private String[] internal_search(String fn, String val) {
		currentise();
		Hits res;
		try {
			res = is.search(new TermQuery(new Term(fn, val)));
			int m = Math.min(LocalLuceneConnector.MAX_RESULTS, res.length());
			if (m == 0)
				return null;
			String[] ret = new String[m];
			for (int i = 0; i < m; i++)
				ret[i] = res.doc(i).get(LocalLuceneConnector.IDX_CONTENT);
			return ret;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Hits general_search(String fn, String val) {
		currentise();
		Hits res;
		try {
			res = is.search(new TermQuery(new Term(fn, val)));
			return res;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean isGenericSearchSupported() {
		return true;
	}

	public boolean isVerifyingAnnotations() {
		return false;
	}

	public String[] lexicalSignatureSearch(String lexicalSignature) {
		return internal_search(LocalLuceneConnector.IDX_LEXICAL_SIGNATURE, lexicalSignature);
	}

	public int postAnnotation(String annotation, String user, String secret) throws Exception {
		Document d = luceneDocumentify(annotation);
		if (d == null)
			return -1;
		try {
			iw.addDocument(d);
			//iw.flush();
			iw.commit();
		} catch (Exception e) {
			e.printStackTrace();
			return -2;
		}
		return 0;
	}

	int writeAnnotation(Document d) {
		if (d == null)
			return -1;
		try {
			iw.addDocument(d);
			//iw.flush();
			iw.commit();
		} catch (Exception e) {
			e.printStackTrace();
			return -2;
		}
		return 0;
	}

	public int postAnonymousAnnotation(String annotation, String secretKey) throws Exception {
		return postAnnotation(annotation, "", secretKey);
	}

	public void setVerifyAnnotation(boolean verify) {
	}

	public void setUseCompoundFile(boolean b) {
		iw.setUseCompoundFile(b);
	}

	public int updateAnnotation(String replacement, String user, String secret) throws Exception {
		Document d = luceneDocumentify(replacement);
		if (d == null)
			return -1;
		String id = d.get(LocalLuceneConnector.IDX_ANNO_ID);
		try {
			iw.updateDocument(new Term(LocalLuceneConnector.IDX_ANNO_ID, id), d);
			iw.commit();
		} catch (Exception e) {
			e.printStackTrace();
			return -2;
		}
		return 0;

	}

	public int updateAnonymousAnnotation(String id, String replacement,
			String secretKey) throws Exception {
		return updateAnnotation(replacement, "", secretKey);
	}

	public String[] URISearch(String URI) {
		return internal_search(LocalLuceneConnector.IDX_URI, URI);
	}

	public void init(String submitUri, String searchUri) {
		// TODO Auto-generated method stub

	}

	public AnnotationModelSerialiser getDefaultAMS() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDefaultAMS(AnnotationModelSerialiser defaultAMS) {
		ams = defaultAMS;

	}

	public String getDbLocation() {
		// TODO Auto-generated method stub
		return "annoDb";
	}

}
