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

package org.simplx.xml;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An exception thrown by the "match" methods of {@link CompareXML} ({@link
 * CompareXML#match(Node,Node,String)}, etc.).
 *
 * @author Ken Arnold
 * @see CompareXML#match(Node,Node,String)
 * @see CompareXML#match(NodeList,NodeList,String)
 * @see CompareXML#match(NamedNodeMap,NamedNodeMap,String)
 * @see CompareXML#match(int,int,String)
 * @see CompareXML#match(String,String,String)
 */
public class XMLMismatchException extends RuntimeException {
    private static final long serialVersionUID = 7526230908139335203L;

    /**
     * Creates a new exception.
     *
     * @param msg The exception message.
     */
    public XMLMismatchException(String msg) {
        super(msg);
    }

    /**
     * Creates a new exception.
     *
     * @param msg   The exception message.
     * @param cause The cause of the exception.
     */

    public XMLMismatchException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
