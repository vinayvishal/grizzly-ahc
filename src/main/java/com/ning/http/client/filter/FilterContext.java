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

package com.ning.http.client.filter;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Request;

import java.io.IOException;

/**
 * A {@link FilterContext} can be used to decorate {@link Request} and {@link AsyncHandler} from a list of {@link RequestFilter}.
 * {@link RequestFilter} gets executed before the HTTP request is made to the remote server. Once the response bytes are
 * received, a {@link FilterContext} is then passed to the list of {@link ResponseFilter}. {@link ResponseFilter}
 * gets invoked before the response gets processed, e.g. before authorization, redirection and invokation of {@link AsyncHandler}
 * gets processed.
 * <p/>
 * Invoking {@link com.ning.http.client.filter.FilterContext#getResponseStatus()} returns an instance of {@link HttpResponseStatus}
 * that can be used to decide if the response processing should continue or not. You can stop the current response processing
 * and replay the request but creating a {@link FilterContext}. The {@link com.ning.http.client.AsyncHttpProvider}
 * will interrupt the processing and "replay" the associated {@link Request} instance.
 */
public class FilterContext<T> {

    private final FilterContextBuilder<T> b;

    /**
     * Create a new {@link FilterContext}
     *
     * @param b a {@link FilterContextBuilder}
     */
    private FilterContext(FilterContextBuilder<T> b) {
        this.b = b;
    }

    /**
     * Return the original or decorated {@link AsyncHandler}
     *
     * @return the original or decorated {@link AsyncHandler}
     */
    public AsyncHandler<T> getAsyncHandler() {
        return b.asyncHandler;
    }

    /**
     * Return the original or decorated {@link Request}
     *
     * @return the original or decorated {@link Request}
     */
    public Request getRequest() {
        return b.request;
    }

    /**
     * Return the unprocessed response's {@link HttpResponseStatus}
     *
     * @return the unprocessed response's {@link HttpResponseStatus}
     */
    public HttpResponseStatus getResponseStatus() {
        return b.responseStatus;
    }

    /**
     * Return the response {@link HttpResponseHeaders}
     */
    public HttpResponseHeaders getResponseHeaders() {
        return b.headers;
    }

    /**
     * Return true if the current response's processing needs to be interrupted and a new {@link Request} be executed.
     *
     * @return true if the current response's processing needs to be interrupted and a new {@link Request} be executed.
     */
    public boolean replayRequest() {
        return b.replayRequest;
    }

    /**
     * Return the {@link IOException}
     *
     * @return the {@link IOException}
     */
    public IOException getIOException() {
        return b.ioException;
    }

    public static class FilterContextBuilder<T> {
        private AsyncHandler<T> asyncHandler = null;
        private Request request = null;
        private HttpResponseStatus responseStatus = null;
        private boolean replayRequest = false;
        private IOException ioException = null;
        private HttpResponseHeaders headers;

        public FilterContextBuilder() {
        }

        public FilterContextBuilder(FilterContext<T> clone) {
            asyncHandler = clone.getAsyncHandler();
            request = clone.getRequest();
            responseStatus = clone.getResponseStatus();
            replayRequest = clone.replayRequest();
            ioException = clone.getIOException();
        }

        public AsyncHandler<T> getAsyncHandler() {
            return asyncHandler;
        }

        public FilterContextBuilder<T> asyncHandler(AsyncHandler<T> asyncHandler) {
            this.asyncHandler = asyncHandler;
            return this;
        }

        public Request getRequest() {
            return request;
        }

        public FilterContextBuilder<T> request(Request request) {
            this.request = request;
            return this;
        }

        public FilterContextBuilder<T> responseStatus(HttpResponseStatus responseStatus) {
            this.responseStatus = responseStatus;
            return this;
        }

        public FilterContextBuilder<T> responseHeaders(HttpResponseHeaders headers) {
            this.headers = headers;
            return this;
        }

        public FilterContextBuilder<T> replayRequest(boolean replayRequest) {
            this.replayRequest = replayRequest;
            return this;
        }

        public FilterContextBuilder<T> ioException(IOException ioException) {
            this.ioException = ioException;
            return this;
        }

        public FilterContext<T> build() {
            return new FilterContext<>(this);
        }
    }
}
