/*
 * Copyright (c) 2009, 2010, Ken Arnold All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the myself nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @SimplxCopyright
 */

package org.simplx.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DocumentReader;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.DomWriter;
import com.thoughtworks.xstream.io.xml.JDomDriver;
import org.w3c.dom.Element;

/**
 * This is the superclass for specific "as document" converters that handle
 * particular XML object representations. Each subclass provides a specific
 * implementation that works properly for its type.
 * <p/>
 * Converts a field that contains an XML tree to an XML tree. This allows you to
 * embed an arbitrary XML tree, unparsed by XStream, into an object. For
 * example:
 * <pre>
 *  &#064;XStreamAlias("rawHolder")
 *  public static class RawHolder {
 *      public String name;
 *      public org.w3c.dom.Element doc;
 *      public long value;
 *  }
 * &nbsp;
 *     ...
 *     XStream xstream = new XStream(new DomDriver());
 *     xstream.registerConverter(new AsDomConverter());
 *     RawHolder rh = new RawHolder();
 *     xstream.fromXML(in, rh);
 *     ...
 * </pre>
 * In this case, the XML would look something like this:
 * <pre>
 *  &lt;rawHolder&gt;
 *    &lt;name&gt;rawName&lt;/name&gt;
 *    &lt;doc&gt;
 *      &lt;whatever&gt;inside&lt;/whatever&gt;
 *    &lt;/doc&gt;
 *    &lt;value&gt;13&lt;/value&gt;
 *  &lt;/rawHolder&gt;
 * </pre>
 * Everything inside the {@code <doc>} tag will be read into the {@code
 * RawHolder} object's {@code doc} field as an XML document. In this example
 * example, the driver is a {@link DomDriver}, the {@link DomReader} will create
 * the XML objects, and the object in {@code doc} will be an {@link
 * org.w3c.dom.Element} for the {@code <whatever>} tag and its contents, because
 * {@link org.w3c.dom.Element} is how DOM represents an element of an XML tree.
 * The type of the reader is determined by the {@link HierarchicalStreamDriver}.
 * <p/>
 * The driver chosen for the {@link XStream} object must be compatible with the
 * type of the field.  For a {@link DomDriver} the field will be from the {@link
 * org.w3c.dom} package; for a {@link JDomDriver} the field will be from the
 * {@link org.jdom} package.
 *
 * @author Ken Arnold
 */
@SuppressWarnings({"unchecked", "UnnecessaryFullyQualifiedName"})
public abstract class XmlDocConverter implements Converter {

    /** Creates a new {@link XmlDocConverter}. */
    public XmlDocConverter() {
    }

    /**
     * {@inheritDoc} The source object will be an XML representation object,
     * typically of an Element or Document. It will generate an equivalent XML
     * document in the output.
     * <p/>
     * To do so, it must be able to read the specific XML representation, which
     * requires an {@link HierarchicalStreamReader} that is appropriate for the
     * XML representation objects in use. This is the same type associated with
     * the driver used by the XStream object. To find this reader, this method
     * looks though the list of known reader types for one that can be created
     * to read an object of the representation type.
     * <p/>
     * For example, if XML representation package being used is DOM, the driver
     * will have been a {@link DomDriver}, which will generate a {@link
     * DomReader} or {@link DomWriter} on demand. Also, the objects used to
     * represent the XML will be DOM objects, such as {@link Element
     * org.w3c.dom.Element} and {@link org.w3c.dom.Document}.
     * <p/>
     * When marshaling a DOM element, {@link #marshal(Object,HierarchicalStreamWriter,MarshallingContext)
     * marshall} will look for a reader that can accept the specific element
     * type. This will be only the {@link DomReader}, which has two one-argument
     * constructors, for {@link Element org.w3c.dom.Element} and {@link
     * org.w3c.dom.Document}. Only one of these can take a DOM element, so it
     * will be used to create a reader that will read the XML representation
     * being marshalled and create a corresponding XML element in the marshaled
     * object, including attributes, text content, and child nodes.
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public void marshal(Object source, HierarchicalStreamWriter writer,
            MarshallingContext context) {

        MixedStreamReader raw = createReader(source);
        MixedStreamCopier copier = new MixedStreamCopier();
        copier.copy(raw, writer);
    }

    /**
     * Creates the reader that will be used to read the source.
     *
     * @param source The source to read from.
     */
    protected abstract MixedStreamReader createReader(Object source);

    /**
     * {@inheritDoc} The XML representation used by the driver's generated
     * {@link HierarchicalStreamReader} will be returned as the unmarshaled
     * object. In other words, the XML being read results in XML being stored in
     * the field for which this is object is a converter.
     * <p/>
     * For example, if the driver that is reading the XML source is a {@link
     * DomDriver}, the unmarshaled form of an element is the {@link Element
     * org.w3c.dom.Element} created by the {@link DomReader} when reading that
     * element.
     *
     * @param reader  The stream to read from.
     * @param context A context that allows nested objects to be processed by
     *                XStream.
     *
     * @return The XML representation object for the XML tree.
     */
    @Override
    public Object unmarshal(HierarchicalStreamReader reader,
            UnmarshallingContext context) {

        HierarchicalStreamReader r;
        for (r = reader; r != null; r = r.underlyingReader()) {
            if (r instanceof DocumentReader)
                break;
        }
        if (r == null)
            throw new IllegalArgumentException(
                    "Need a DocumentReader for raw conversion");
        DocumentReader docReader = (DocumentReader) r;

        Object elem = docReader.getCurrent();

        // This is done to produce a more useful error message
        if (!context.getRequiredType().isAssignableFrom(elem.getClass())) {
            throw new ConversionException(
                    "XML node not a usable type (" + elem.getClass().getName() +
                            "): Is XStream driver compatible with the class " +
                            context.getRequiredType().getName() + "?");
        }

        return elem;
    }
}