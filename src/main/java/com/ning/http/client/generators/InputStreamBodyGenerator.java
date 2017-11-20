/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package com.ning.http.client.generators;

import com.ning.http.client.Body;
import com.ning.http.client.BodyGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A {@link BodyGenerator} which use an {@link InputStream} for reading bytes, without having to read the entire
 * stream in memory.
 * <p/>
 * NOTE: The {@link InputStream} must support the {@link InputStream#mark} and {@link java.io.InputStream#reset()} operation.
 * If not, mechanisms like authentication, redirect, or resumable download will not works.
 */
public class InputStreamBodyGenerator implements BodyGenerator {

    private final static byte[] END_PADDING = "\r\n".getBytes();
    private final static byte[] ZERO = "0".getBytes();
    private final InputStream inputStream;
    private final static Logger logger = LoggerFactory.getLogger(InputStreamBodyGenerator.class);
    private boolean patchNettyChunkingIssue = false;

    public InputStreamBodyGenerator(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public Body createBody() throws IOException {
        return new ISBody();
    }

    protected class ISBody implements Body {
        private boolean eof = false;
        private int endDataCount = 0;
        private byte[] chunk;

        @Override
        public long getContentLength() {
            return -1;
        }

        @Override
        public long read(ByteBuffer buffer) throws IOException {

            // To be safe.
            chunk = new byte[buffer.remaining() - 10];


            int read = -1;
            try {
                read = inputStream.read(chunk);
            } catch (IOException ex) {
                logger.warn("Unable to read", ex);
            }

            if (patchNettyChunkingIssue) {
                if (read == -1) {
                    // Since we are chunked, we must output extra bytes before considering the input stream closed.
                    // chunking requires to end the chunking:
                    // - A Terminating chunk of  "0\r\n".getBytes(),
                    // - Then a separate packet of "\r\n".getBytes()
                    if (!eof) {
                        endDataCount++;
                        if (endDataCount == 2)
                            eof = true;

                        if (endDataCount == 1)
                            buffer.put(ZERO);

                        buffer.put(END_PADDING);

                        return buffer.position();
                    } else {
                        eof = false;
                    }
                    return -1;
                }

                /**
                 * Netty 3.2.3 doesn't support chunking encoding properly, so we chunk encoding ourself.
                 */

                buffer.put(Integer.toHexString(read).getBytes());
                // Chunking is separated by "<bytesreads>\r\n"
                buffer.put(END_PADDING);
                buffer.put(chunk, 0, read);
                // Was missing the final chunk \r\n.
                buffer.put(END_PADDING);
            } else if (read > 0) {
                buffer.put(chunk, 0, read);
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }

    /**
     * HACK: This is required because Netty has issues with chunking.
     *
     * @param patchNettyChunkingIssue
     */
    public void patchNettyChunkingIssue(boolean patchNettyChunkingIssue) {
        this.patchNettyChunkingIssue = patchNettyChunkingIssue;
    }
}
