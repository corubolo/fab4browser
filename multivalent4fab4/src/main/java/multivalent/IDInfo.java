package multivalent;

import org.simplx.object.Reflx;

import java.util.Comparator;

public class IDInfo implements Comparable<IDInfo> {
    /**
     * Enumeration that defines the various levels of confidence that
     * identification can take.  The names are according to which kind of test
     * was (or should be) done to achieve the identification, whether in actual
     * fact or a test of equivalent value. For example, the {@link #MAGIC}
     * confidence could indicate that a check was done for a magic number at the
     * beginning or the end of the content.  But it would also be used to mark
     * an identification based on a sample of the file's content, such as to
     * determine a text file's character text.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public enum Confidence {
        /** The suffix of the path. */
        SUFFIX,

        /**
         * A simple test, such as for a magic number. (This is where a
         * determination that a file is a null file would be, which allows other
         * data to determine if it is a typed-null file, such as an empty data
         * set for a program that does not require magic numbers or other
         * content.  Such a file could be {@link #TYPED} or {@link #MAXIMUM}.)
         */
        MAGIC,

        /**
         * An externally-specified type definition, such as a mime type in an
         * HTML header or data from a typed file system.
         */
        TYPED,

        /**
         * A quick heuristic check of the data.  This might include looking for
         * keywords, or checking the first 1000 bytes, or ...
         */
        HEURISTIC,

        /**
         * A complete parse of the data.  This is the lowest level confidence
         * that includes a component of verification as well as simple
         * identification.
         */
        PARSE,

        /**
         * A full processing of the data, such as by rendering a document fully
         * via a graphics system.
         */
        PROCESS,

        /** A trained human or expert system judgement. */
        MAXIMUM;

        @SuppressWarnings({"InnerClassFieldHidesOuterClassField"})
        public static final Comparator<Confidence> BEST_FIRST =
                new Comparator<Confidence>() {
   
                    public int compare(Confidence o1, Confidence o2) {
                        return o2.compareTo(o1);
                    }
                };
    }

    /** The confidence in the file type identification. */
    public final Confidence confidence;
    /** The mime type that has been identified. */
    public final String mimeType;
    /** The file format version that has been identified. */
    public final String formatVersion;
    /** Any human-readable description of the file type. */
    public final String description;
    /** The class of the object that has done the identification. */
    public final Class<?> sourceType;

    public IDInfo(Confidence confidence, Object source) {
        this(confidence, source, null);
    }

    public IDInfo(Confidence confidence, Object source, String mimeType) {
        this(confidence, source, mimeType, null, null);
    }

    public IDInfo(Confidence confidence, Object source, String mimeType,
            String version, String description) {
        this.confidence = confidence;
        sourceType = source.getClass();
        this.mimeType = mimeType;
        formatVersion = version;
        this.description = description;
    }

    public static final Comparator<IDInfo> BEST_FIRST =
            new Comparator<IDInfo>() {
                public int compare(IDInfo o1, IDInfo o2) {
                    return compareInfo(o1, o2, Confidence.BEST_FIRST);
                }
            };


    public int compareTo(IDInfo that) {
        return compareInfo(this, that, null);
    }

    private static int compareInfo(IDInfo o1, IDInfo o2,
            Comparator<Confidence> cmpConfidence) {

        if (o1 == o2)
            return 0;

        int cmp;

        if (cmpConfidence != null)
            cmp = cmpConfidence.compare(o1.confidence, o2.confidence);
        else
            cmp = o1.confidence.compareTo(o2.confidence);
        if (cmp != 0)
            return cmp;

        if ((cmp = doCompare(o1.mimeType, o2.mimeType)) != 0)
            return cmp;

        if ((cmp = doCompare(o1.formatVersion, o2.formatVersion)) != 0)
            return cmp;

        return o1.sourceType.getName().compareTo(o2.sourceType.getName());
    }

    private static int doCompare(String v1, String v2) {
        if (v1 == v2)
            return 0;
        else if (v1 == null)
            return -1;
        else if (v2 == null)
            return 1;
        else
            return String.CASE_INSENSITIVE_ORDER.compare(v1, v2);
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof IDInfo && compareTo((IDInfo) that) == 0;
    }

    @Override
    public int hashCode() {
        int h = confidence.hashCode() ^ sourceType.hashCode();
        if (mimeType != null)
            h ^= mimeType.hashCode();
        if (formatVersion != null)
            h ^= formatVersion.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return Reflx.toStringReflx(this);
    }
}