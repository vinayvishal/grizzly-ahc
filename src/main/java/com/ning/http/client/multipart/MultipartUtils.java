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

package com.ning.http.client.multipart;

import static java.nio.charset.StandardCharsets.*;
import static com.ning.http.client.multipart.Part.CRLF_BYTES;
import static com.ning.http.client.multipart.Part.EXTRA_BYTES;
import static com.ning.http.util.MiscUtils.isNonEmpty;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MultipartUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultipartUtils.class);

    /**
     * The Content-Type for multipart/form-data.
     */
    private static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data";

    /**
     * The pool of ASCII chars to be used for generating a multipart boundary.
     */
    private static byte[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .getBytes(US_ASCII);

    private MultipartUtils() {
    }

    /**
     * Creates a new multipart entity containing the given parts.
     * 
     * @param parts
     *            The parts to include.
     */
    public static MultipartBody newMultipartBody(List<Part> parts, FluentCaseInsensitiveStringsMap requestHeaders) {
        if (parts == null) {
            throw new NullPointerException("parts");
        }

        byte[] multipartBoundary;
        String contentType;

        String contentTypeHeader = requestHeaders.getFirstValue("Content-Type");
        if (isNonEmpty(contentTypeHeader)) {
            int boundaryLocation = contentTypeHeader.indexOf("boundary=");
            if (boundaryLocation != -1) {
                // boundary defined in existing Content-Type
                contentType = contentTypeHeader;
                multipartBoundary = (contentTypeHeader.substring(boundaryLocation + "boundary=".length()).trim())
                        .getBytes(US_ASCII);
            } else {
                // generate boundary and append it to existing Content-Type
                multipartBoundary = generateMultipartBoundary();
                contentType = computeContentType(contentTypeHeader, multipartBoundary);
            }
        } else {
            multipartBoundary = generateMultipartBoundary();
            contentType = computeContentType(MULTIPART_FORM_CONTENT_TYPE, multipartBoundary);
        }

        long contentLength = getLengthOfParts(parts, multipartBoundary);

        return new MultipartBody(parts, contentType, contentLength, multipartBoundary);
    }

    private static byte[] generateMultipartBoundary() {
        Random rand = new Random();
        byte[] bytes = new byte[rand.nextInt(11) + 30]; // a random size from 30 to 40
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)];
        }
        return bytes;
    }

    private static String computeContentType(String base, byte[] multipartBoundary) {
        StringBuilder buffer = StringUtils.stringBuilder().append(base);
        if (!base.endsWith(";"))
            buffer.append(';');
        return buffer.append(" boundary=").append(new String(multipartBoundary, US_ASCII)).toString();
    }

    public static long writeBytesToChannel(WritableByteChannel target, byte[] bytes) throws IOException {

        int written = 0;
        int maxSpin = 0;
        ByteBuffer message = ByteBuffer.wrap(bytes);

        if (target instanceof SocketChannel) {
            final Selector selector = Selector.open();
            try {
                final SocketChannel channel = (SocketChannel) target;
                channel.register(selector, SelectionKey.OP_WRITE);

                while (written < bytes.length) {
                    selector.select(1000);
                    maxSpin++;
                    final Set<SelectionKey> selectedKeys = selector.selectedKeys();

                    for (SelectionKey key : selectedKeys) {
                        if (key.isWritable()) {
                            written += target.write(message);
                            maxSpin = 0;
                        }
                    }
                    if (maxSpin >= 10) {
                        throw new IOException("Unable to write on channel " + target);
                    }
                }
            } finally {
                selector.close();
            }
        } else {
            while ((target.isOpen()) && (written < bytes.length)) {
                long nWrite = target.write(message);
                written += nWrite;
                if (nWrite == 0 && maxSpin++ < 10) {
                    LOGGER.info("Waiting for writing...");
                    try {
                        bytes.wait(1000);
                    } catch (InterruptedException e) {
                        LOGGER.trace(e.getMessage(), e);
                    }
                } else {
                    if (maxSpin >= 10) {
                        throw new IOException("Unable to write on channel " + target);
                    }
                    maxSpin = 0;
                }
            }
        }
        return written;
    }

    public static byte[] getMessageEnd(byte[] partBoundary) throws IOException {

        if (!isNonEmpty(partBoundary))
            throw new IllegalArgumentException("partBoundary may not be empty");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamPartVisitor visitor = new OutputStreamPartVisitor(out);
        visitor.withBytes(EXTRA_BYTES);
        visitor.withBytes(partBoundary);
        visitor.withBytes(EXTRA_BYTES);
        visitor.withBytes(CRLF_BYTES);

        return out.toByteArray();
    }

    public static long getLengthOfParts(List<Part> parts, byte[] partBoundary) {

        try {
            if (parts == null) {
                throw new NullPointerException("parts");
            }
            long total = 0;
            for (Part part : parts) {
                long l = part.length(partBoundary);
                if (l < 0) {
                    return -1;
                }
                total += l;
            }
            total += EXTRA_BYTES.length;
            total += partBoundary.length;
            total += EXTRA_BYTES.length;
            total += CRLF_BYTES.length;
            return total;
        } catch (Exception e) {
            LOGGER.error("An exception occurred while getting the length of the parts", e);
            return 0L;
        }
    }
}
