/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.ning.http.client.providers.grizzly;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.HttpContent;

import com.ning.http.client.HttpResponseBodyPart;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;


/**
 * {@link HttpResponseBodyPart} implementation using the Grizzly 2.0 HTTP client
 * codec.
 *
 * @author The Grizzly Team
 * @since 1.7.0
 */
public class GrizzlyResponseBodyPart extends HttpResponseBodyPart {

    private final HttpContent content;
    private final Connection connection;
    private final AtomicReference<byte[]> contentBytes =
            new AtomicReference<byte[]>();


    // ------------------------------------------------------------ Constructors


    public GrizzlyResponseBodyPart(final HttpContent content,
                                   final Connection connection) {
        super(false);
        this.content = content;
        this.connection = connection;

    }


    // --------------------------------------- Methods from HttpResponseBodyPart


    @Override
    public byte[] getBodyPartBytes() {

        byte[] bytes = contentBytes.get();
        if (bytes != null) {
            return bytes;
        }
        final Buffer b = content.getContent();
        final int origPos = b.position();
        bytes = new byte[b.remaining()];
        b.get(bytes);
        b.flip();
        b.position(origPos);
        contentBytes.compareAndSet(null, bytes);
        return bytes;

    }


    @Override
    public int writeTo(OutputStream outputStream) throws IOException {

        final byte[] bytes = getBodyPartBytes();
        outputStream.write(getBodyPartBytes());
        return bytes.length;

    }


    @Override
    public ByteBuffer getBodyByteBuffer() {

        return ByteBuffer.wrap(getBodyPartBytes());

    }

    @Override
    public boolean isLast() {
        return content.isLast();
    }

    @Override
    public void markUnderlyingConnectionAsToBeClosed() {
        content.getHttpHeader().getProcessingState().setKeepAlive(false);
    }

    @Override
    public boolean isUnderlyingConnectionToBeClosed() {
        return content.getHttpHeader().getProcessingState().isStayAlive();
    }

    // ----------------------------------------------- Package Protected Methods


    Buffer getBodyBuffer() {

        return content.getContent();

    }

    @Override
    public int length() {
        return content.getContent().remaining();
    }
}
