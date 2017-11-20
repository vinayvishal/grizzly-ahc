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

package com.ning.http.client.resumable;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response.ResponseBuilder;
import com.ning.http.client.listener.TransferCompletionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An {@link AsyncHandler} which support resumable download, e.g when used with an {@link ResumableIOExceptionFilter},
 * this handler can resume the download operation at the point it was before the interruption occured. This prevent having to
 * download the entire file again. It's the responsibility of the {@link com.ning.http.client.listener.TransferListener}
 * to track how many bytes has been transferred and to properly adjust the file's write position.
 * <p/>
 * In case of a JVM crash/shutdown, you can create an instance of this class and pass the last valid bytes position.
 */
public class ResumableAsyncHandler<T> implements AsyncHandler<T> {
    private final static Logger logger = LoggerFactory.getLogger(TransferCompletionHandler.class);
    private final AtomicLong byteTransferred;
    private String url;
    private final ResumableProcessor resumableProcessor;
    private final AsyncHandler<T> decoratedAsyncHandler;
    private static Map<String, Long> resumableIndex;
    private final static ResumableIndexThread resumeIndexThread = new ResumableIndexThread();
    private ResponseBuilder responseBuilder = new ResponseBuilder();
    private final boolean accumulateBody;
    private ResumableListener resumableListener = new NULLResumableListener();

    private ResumableAsyncHandler(long byteTransferred,
                                  ResumableProcessor resumableProcessor,
                                  AsyncHandler<T> decoratedAsyncHandler,
                                  boolean accumulateBody) {

        this.byteTransferred = new AtomicLong(byteTransferred);

        if (resumableProcessor == null) {
            resumableProcessor = new NULLResumableHandler();
        }
        this.resumableProcessor = resumableProcessor;

        resumableIndex = resumableProcessor.load();
        resumeIndexThread.addResumableProcessor(resumableProcessor);

        this.decoratedAsyncHandler = decoratedAsyncHandler;
        this.accumulateBody = accumulateBody;
    }

    public ResumableAsyncHandler(long byteTransferred) {
        this(byteTransferred, null, null, false);
    }

    public ResumableAsyncHandler(boolean accumulateBody) {
        this(0, null, null, accumulateBody);
    }

    public ResumableAsyncHandler() {
        this(0, null, null, false);
    }

    public ResumableAsyncHandler(AsyncHandler<T> decoratedAsyncHandler) {
        this(0, new PropertiesBasedResumableProcessor(), decoratedAsyncHandler, false);
    }

    public ResumableAsyncHandler(long byteTransferred, AsyncHandler<T> decoratedAsyncHandler) {
        this(byteTransferred, new PropertiesBasedResumableProcessor(), decoratedAsyncHandler, false);
    }

    public ResumableAsyncHandler(ResumableProcessor resumableProcessor) {
        this(0, resumableProcessor, null, false);
    }

    public ResumableAsyncHandler(ResumableProcessor resumableProcessor, boolean accumulateBody) {
        this(0, resumableProcessor, null, accumulateBody);
    }

    @Override
    public AsyncHandler.STATE onStatusReceived(final HttpResponseStatus status) throws Exception {
        responseBuilder.accumulate(status);
        if (status.getStatusCode() == 200 || status.getStatusCode() == 206) {
            url = status.getUri().toUrl();
        } else {
            return AsyncHandler.STATE.ABORT;
        }

        if (decoratedAsyncHandler != null) {
            return decoratedAsyncHandler.onStatusReceived(status);
        }

        return AsyncHandler.STATE.CONTINUE;
    }

    @Override
    public void onThrowable(Throwable t) {
        if (decoratedAsyncHandler != null) {
            decoratedAsyncHandler.onThrowable(t);
        } else {
            logger.debug("", t);
        }
    }

    @Override
    public AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {

        if (accumulateBody) {
            responseBuilder.accumulate(bodyPart);
        }

        STATE state = STATE.CONTINUE;
        try {
            resumableListener.onBytesReceived(bodyPart.getBodyByteBuffer());
        } catch (IOException ex) {
            return AsyncHandler.STATE.ABORT;
        }

        if (decoratedAsyncHandler != null) {
            state = decoratedAsyncHandler.onBodyPartReceived(bodyPart);
        }

        byteTransferred.addAndGet(bodyPart.getBodyPartBytes().length);
        resumableProcessor.put(url, byteTransferred.get());

        return state;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T onCompleted() throws Exception {
        resumableProcessor.remove(url);
        resumableListener.onAllBytesReceived();

        if (decoratedAsyncHandler != null) {
            decoratedAsyncHandler.onCompleted();
        }
        // Not sure
        return (T) responseBuilder.build();
    }

    @Override
    public AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        responseBuilder.accumulate(headers);
        String contentLengthHeader = headers.getHeaders().getFirstValue("Content-Length");
        if (contentLengthHeader != null) {
            if (Long.parseLong(contentLengthHeader) == -1L) {
                return AsyncHandler.STATE.ABORT;
            }
        }

        if (decoratedAsyncHandler != null) {
            return decoratedAsyncHandler.onHeadersReceived(headers);
        }
        return AsyncHandler.STATE.CONTINUE;
    }

    /**
     * Invoke this API if you want to set the Range header on your {@link Request} based on the last valid bytes
     * position.
     *
     * @param request {@link Request}
     * @return a {@link Request} with the Range header properly set.
     */
    public Request adjustRequestRange(Request request) {

        Long ri = resumableIndex.get(request.getUrl());
        if (ri != null) {
            byteTransferred.set(ri);
        }

        // The Resumbale
        if (resumableListener != null && resumableListener.length() > 0 && byteTransferred.get() != resumableListener.length()) {
            byteTransferred.set(resumableListener.length());
        }

        RequestBuilder builder = new RequestBuilder(request);
        if (request.getHeaders().get("Range").isEmpty() && byteTransferred.get() != 0) {
            builder.setHeader("Range", "bytes=" + byteTransferred.get() + "-");
        }
        return builder.build();
    }

    /**
     * Set a {@link ResumableListener}
     *
     * @param resumableListener a {@link ResumableListener}
     * @return this
     */
    @SuppressWarnings("rawtypes")
    public ResumableAsyncHandler setResumableListener(ResumableListener resumableListener) {
        this.resumableListener = resumableListener;
        return this;
    }

    private static class ResumableIndexThread extends Thread {

        public final ConcurrentLinkedQueue<ResumableProcessor> resumableProcessors = new ConcurrentLinkedQueue<>();

        public ResumableIndexThread() {
            Runtime.getRuntime().addShutdownHook(this);
        }

        public void addResumableProcessor(ResumableProcessor p) {
            resumableProcessors.offer(p);
        }

        public void run() {
            for (ResumableProcessor p : resumableProcessors) {
                p.save(resumableIndex);
            }
        }
    }

    /**
     * An interface to implement in order to manage the way the incomplete file management are handled.
     */
    public static interface ResumableProcessor {

        /**
         * Associate a key with the number of bytes sucessfully transferred.
         *
         * @param key              a key. The recommended way is to use an url.
         * @param transferredBytes The number of bytes sucessfully transferred.
         */
        void put(String key, long transferredBytes);

        /**
         * Remove the key associate value.
         *
         * @param key key from which the value will be discarted
         */
        void remove(String key);

        /**
         * Save the current {@link Map} instance which contains information about the current transfer state.
         * This method *only* invoked when the JVM is shutting down.
         *
         * @param map
         */
        void save(Map<String, Long> map);

        /**
         * Load the {@link Map} in memory, contains information about the transferred bytes.
         *
         * @return {@link Map}
         */
        Map<String, Long> load();
    }

    private static class NULLResumableHandler implements ResumableProcessor {

        @Override
        public void put(String url, long transferredBytes) {
        }

        @Override
        public void remove(String uri) {
        }

        @Override
        public void save(Map<String, Long> map) {
        }

        @Override
        public Map<String, Long> load() {
            return new HashMap<>();
        }
    }

    private static class NULLResumableListener implements ResumableListener {

        private long length = 0L;

        public void onBytesReceived(ByteBuffer byteBuffer) throws IOException {
            length += byteBuffer.remaining();
        }

        public void onAllBytesReceived() {
        }

        public long length() {
            return length;
        }
    }
}
