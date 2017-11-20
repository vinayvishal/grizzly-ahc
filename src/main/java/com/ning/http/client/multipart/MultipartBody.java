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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.RandomAccessBody;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

public class MultipartBody implements RandomAccessBody {

    private final static Logger LOGGER = LoggerFactory.getLogger(MultipartBody.class);

    private final byte[] boundary;
    private final long contentLength;
    private final String contentType;
    private final List<Part> parts;
    private final List<RandomAccessFile> pendingOpenFiles = new ArrayList<>();

    private boolean transfertDone = false;

    private int currentPart = 0;
    private byte[] currentBytes;
    private int currentBytesPosition = -1;
    private boolean doneWritingParts = false;
    private FileLocation fileLocation = FileLocation.NONE;
    private FileChannel currentFileChannel;

    enum FileLocation {
        NONE, START, MIDDLE, END
    }

    public MultipartBody(List<Part> parts, String contentType, long contentLength, byte[] boundary) {
        this.boundary = boundary;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.parts = parts;
    }

    public void close() throws IOException {
        for (RandomAccessFile file : pendingOpenFiles) {
            file.close();
        }
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBoundary() {
        return boundary;
    }

    // RandomAccessBody API, suited for HTTP but not for HTTPS
    public long transferTo(long position, WritableByteChannel target) throws IOException {

        if (transfertDone) {
            throw new UnsupportedOperationException("Transfer is already done");
        }

        long overallLength = 0;

        for (Part part : parts) {
            overallLength += part.write(target, boundary);
        }

        overallLength += MultipartUtils.writeBytesToChannel(target, MultipartUtils.getMessageEnd(boundary));

        transfertDone = true;

        return overallLength;
    }

    // Regular Body API
    public long read(ByteBuffer buffer) throws IOException {
        try {
            int overallLength = 0;

            int maxLength = buffer.remaining();

            if (currentPart == parts.size() && transfertDone) {
                return -1;
            }

            boolean full = false;

            while (!full && !doneWritingParts) {
                Part part = null;

                if (currentPart < parts.size()) {
                    part = parts.get(currentPart);
                }
                if (currentFileChannel != null) {
                    overallLength += writeCurrentFile(buffer);
                    full = overallLength == maxLength;

                } else if (currentBytesPosition > -1) {
                    overallLength += writeCurrentBytes(buffer, maxLength - overallLength);
                    full = overallLength == maxLength;

                    if (currentPart == parts.size() && currentBytesFullyRead()) {
                        doneWritingParts = true;
                    }

                } else if (part instanceof StringPart) {
                    StringPart stringPart = (StringPart) part;
                    // set new bytes, not full, so will loop to writeCurrentBytes above
                    initializeCurrentBytes(stringPart.getBytes(boundary));
                    currentPart++;

                } else if (part instanceof AbstractFilePart) {

                    AbstractFilePart filePart = (AbstractFilePart) part;

                    switch (fileLocation) {
                    case NONE:
                        // set new bytes, not full, so will loop to writeCurrentBytes above
                        initializeCurrentBytes(filePart.generateFileStart(boundary));
                        fileLocation = FileLocation.START;
                        break;
                    case START:
                        // set current file channel so code above executes first
                        initializeFileBody(filePart);
                        fileLocation = FileLocation.MIDDLE;
                        break;
                    case MIDDLE:
                        initializeCurrentBytes(filePart.generateFileEnd());
                        fileLocation = FileLocation.END;
                        break;
                    case END:
                        currentPart++;
                        fileLocation = FileLocation.NONE;
                        if (currentPart == parts.size()) {
                            doneWritingParts = true;
                        }
                    }
                }
            }

            if (doneWritingParts) {
                if (currentBytesPosition == -1) {
                    initializeCurrentBytes(MultipartUtils.getMessageEnd(boundary));
                }

                if (currentBytesPosition > -1) {
                    overallLength += writeCurrentBytes(buffer, maxLength - overallLength);

                    if (currentBytesFullyRead()) {
                        currentBytes = null;
                        currentBytesPosition = -1;
                        transfertDone = true;
                    }
                }
            }
            return overallLength;

        } catch (Exception e) {
            LOGGER.error("Read exception", e);
            return 0;
        }
    }

    private boolean currentBytesFullyRead() {
        return currentBytes == null || currentBytesPosition == -1;
    }

    private void initializeFileBody(AbstractFilePart part) throws IOException {

        if (part instanceof FilePart) {
            RandomAccessFile raf = new RandomAccessFile(FilePart.class.cast(part).getFile(), "r");
            pendingOpenFiles.add(raf);
            currentFileChannel = raf.getChannel();

        } else if (part instanceof ByteArrayPart) {
            initializeCurrentBytes(ByteArrayPart.class.cast(part).getBytes());

        } else {
            throw new IllegalArgumentException("Unknow AbstractFilePart type");
        }
    }

    private void initializeCurrentBytes(byte[] bytes) throws IOException {
        currentBytes = bytes;
        currentBytesPosition = 0;
    }

    private int writeCurrentFile(ByteBuffer buffer) throws IOException {

        int read = currentFileChannel.read(buffer);

        if (currentFileChannel.position() == currentFileChannel.size()) {

            currentFileChannel.close();
            currentFileChannel = null;

            int currentFile = pendingOpenFiles.size() - 1;
            pendingOpenFiles.get(currentFile).close();
            pendingOpenFiles.remove(currentFile);
        }

        return read;
    }

    private int writeCurrentBytes(ByteBuffer buffer, int length) throws IOException {

        if (currentBytes.length == 0) {
            currentBytesPosition = -1;
            currentBytes = null;
            return 0;
        }

        int available = currentBytes.length - currentBytesPosition;

        int writeLength = Math.min(available, length);

        if (writeLength > 0) {
            buffer.put(currentBytes, currentBytesPosition, writeLength);

            if (available <= length) {
                currentBytesPosition = -1;
                currentBytes = null;
            } else {
                currentBytesPosition += writeLength;
            }
        }

        return writeLength;
    }
}
