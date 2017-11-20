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

package com.ning.http.client;

import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.uri.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the asynchronous HTTP response callback for an {@link AsyncCompletionHandler}
 */
public interface Response {
    /**
     * Returns the status code for the request.
     * 
     * @return The status code
     */
    int getStatusCode();

    /**
     * Returns the status text for the request.
     * 
     * @return The status text
     */
    String getStatusText();

    /**
     * Return the entire response body as a byte[].
     * 
     * @return the entire response body as a byte[].
     * @throws IOException
     */
    byte[] getResponseBodyAsBytes() throws IOException;

    /**
     * Return the entire response body as a ByteBuffer.
     * 
     * @return the entire response body as a ByteBuffer.
     * @throws IOException
     */
    ByteBuffer getResponseBodyAsByteBuffer() throws IOException;

    /**
     * Returns an input stream for the response body. Note that you should not try to get this more than once, and that you should not close the stream.
     * 
     * @return The input stream
     * @throws java.io.IOException
     */
    InputStream getResponseBodyAsStream() throws IOException;

    /**
     * Returns the first maxLength bytes of the response body as a string. Note that this does not check whether the content type is actually a textual one, but it will use the
     * charset if present in the content type header.
     * 
     * @param maxLength
     *            The maximum number of bytes to read
     * @param charset
     *            the charset to use when decoding the stream
     * @return The response body
     * @throws java.io.IOException
     */
    String getResponseBodyExcerpt(int maxLength, String charset) throws IOException;

    /**
     * Return the entire response body as a String.
     * 
     * @param charset
     *            the charset to use when decoding the stream
     * @return the entire response body as a String.
     * @throws IOException
     */
    String getResponseBody(String charset) throws IOException;

    /**
     * Returns the first maxLength bytes of the response body as a string. Note that this does not check whether the content type is actually a textual one, but it will use the
     * charset if present in the content type header.
     * 
     * @param maxLength
     *            The maximum number of bytes to read
     * @return The response body
     * @throws java.io.IOException
     */
    String getResponseBodyExcerpt(int maxLength) throws IOException;

    /**
     * Return the entire response body as a String.
     * 
     * @return the entire response body as a String.
     * @throws IOException
     */
    String getResponseBody() throws IOException;

    /**
     * Return the request {@link Uri}. Note that if the request got redirected, the value of the {@link URI} will be the last valid redirect url.
     * 
     * @return the request {@link Uri}.
     */
    Uri getUri();

    /**
     * Return the content-type header value.
     * 
     * @return the content-type header value.
     */
    String getContentType();

    /**
     * Return the response header
     * 
     * @return the response header
     */
    String getHeader(String name);

    /**
     * Return a {@link List} of the response header value.
     * 
     * @return the response header
     */
    List<String> getHeaders(String name);

    FluentCaseInsensitiveStringsMap getHeaders();

    /**
     * Return true if the response redirects to another object.
     * 
     * @return True if the response redirects to another object.
     */
    boolean isRedirected();

    /**
     * Subclasses SHOULD implement toString() in a way that identifies the response for logging.
     * 
     * @return The textual representation
     */
    String toString();

    /**
     * Return the list of {@link Cookie}.
     */
    List<Cookie> getCookies();

    /**
     * Return true if the response's status has been computed by an {@link AsyncHandler}
     * 
     * @return true if the response's status has been computed by an {@link AsyncHandler}
     */
    boolean hasResponseStatus();

    /**
     * Return true if the response's headers has been computed by an {@link AsyncHandler} It will return false if the either
     * {@link AsyncHandler#onStatusReceived(HttpResponseStatus)} or {@link AsyncHandler#onHeadersReceived(HttpResponseHeaders)} returned {@link AsyncHandler.STATE#ABORT}
     * 
     * @return true if the response's headers has been computed by an {@link AsyncHandler}
     */
    boolean hasResponseHeaders();

    /**
     * Return true if the response's body has been computed by an {@link AsyncHandler}. It will return false if the either {@link AsyncHandler#onStatusReceived(HttpResponseStatus)}
     * or {@link AsyncHandler#onHeadersReceived(HttpResponseHeaders)} returned {@link AsyncHandler.STATE#ABORT}
     * 
     * @return true if the response's body has been computed by an {@link AsyncHandler}
     */
    boolean hasResponseBody();

    public static class ResponseBuilder {
        private final List<HttpResponseBodyPart> bodyParts = new ArrayList<>();
        private HttpResponseStatus status;
        private HttpResponseHeaders headers;

        public ResponseBuilder accumulate(HttpResponseStatus status) {
            this.status = status;
            return this;
        }

        public ResponseBuilder accumulate(HttpResponseHeaders headers) {
            this.headers = headers;
            return this;
        }

        /**
         * @param bodyPart
         *            a body part (possibly empty, but will be filtered out)
         * @return this
         */
        public ResponseBuilder accumulate(HttpResponseBodyPart bodyPart) {
            if (bodyPart.length() > 0)
                bodyParts.add(bodyPart);
            return this;
        }

        /**
         * Build a {@link Response} instance
         * 
         * @return a {@link Response} instance
         */
        public Response build() {
            return status == null ? null : status.prepareResponse(headers, bodyParts);
        }

        /**
         * Reset the internal state of this builder.
         */
        public void reset() {
            bodyParts.clear();
            status = null;
            headers = null;
        }
    }
}
