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

import com.thoughtworks.xstream.io.xml.JDomReader;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Parent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A reader that manages mixed text and nodes in an XML stream.
 *
 * @author Ken Arnold
 */
@SuppressWarnings({"unchecked"})
public class MixedJDomStreamReader extends JDomReader
        implements MixedStreamReader {

    /**
     * Creates a new {@link MixedJDomStreamReader}.
     *
     * @param elem The element at the top of the subtree of XML that is managed
     *             by this reader.
     */
    public MixedJDomStreamReader(Element elem) {
        super(elem);
    }

    @Override
    public Iterator partIterator() {
        Parent elem = (Parent) getCurrent();
        List kids = elem.getContent();
        List parts = new ArrayList<Object>();
        for (final Object child : kids) {
            if (child instanceof Element) {
                parts.add(new NodePart() {
                    @Override
                    public String nodeName() {
                        return ((Element) child).getName();
                    }
                });
            } else {
                parts.add(new TextPart() {
                    @Override
                    public String contents() {
                        return ((Content) child).getValue();
                    }
                });
            }
        }
        return Collections.unmodifiableList(parts).iterator();
    }
}