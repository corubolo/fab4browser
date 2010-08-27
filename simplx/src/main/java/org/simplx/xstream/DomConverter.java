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

import com.thoughtworks.xstream.io.xml.DomDriver;
import org.w3c.dom.Element;

/**
 * Converts XML to a DOM element and vice versa. This converter allows you to
 * embed an arbitrary DOM-based XML tree, unparsed by XStream, into an object.
 * For example:
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
 * RawHolder} object's {@code doc} field as an XML element.  The driver must be
 * {@link DomDriver} (or any other driver that produces {@link
 * org.w3c.dom.Element} objects) to be compatible with the field type. The
 * {@link DomDriver} will create the XML objects, and the object in {@code doc}
 * will be an {@link  org.w3c.dom.Element Element} for the {@code <whatever>}
 * tag and its contents.
 *
 * @author Ken Arnold
 */
@SuppressWarnings({"UnnecessaryFullyQualifiedName"})
public class DomConverter extends XmlDocConverter {
    /**
     * Returns {@code true} if the type is named {@code "org.w3c.dom.Element"}.
     *
     * @param type The type.
     */
    @Override
    public boolean canConvert(Class type) {
        try {
            Class elemClass = Class.forName("org.w3c.dom.Element");
            return elemClass.isAssignableFrom(type);
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    protected MixedStreamReader createReader(Object source) {
        return new MixedDomStreamReader((Element) source);
    }
}
