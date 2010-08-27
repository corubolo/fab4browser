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

import com.thoughtworks.xstream.io.xml.DomReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A reader that manages mixed text and nodes in an XML stream.
 *
 * @author Ken Arnold
 */
public class MixedDomStreamReader extends DomReader
        implements MixedStreamReader {

    /**
     * Creates a new {@link MixedDomStreamReader}.
     *
     * @param elem The element at the top of the subtree of XML that is managed
     *             by this reader.
     */
    public MixedDomStreamReader(Element elem) {
        super(elem);
    }

    @Override
    public Iterator partIterator() {
        Node elem = (Node) getCurrent();
        NodeList kids = elem.getChildNodes();
        Object[] parts = new Object[kids.getLength()];
        for (int i = 0; i < kids.getLength(); i++) {
            final Node child = kids.item(i);
            if (child instanceof Element) {
                parts[i] = new NodePart() {
                    @Override
                    public String nodeName() {
                        return child.getNodeName();
                    }
                };
            } else {
                parts[i] = new TextPart() {
                    @Override
                    public String contents() {
                        return child.getTextContent();
                    }
                };
            }
        }
        return Arrays.asList(parts).iterator();
    }
}