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

package com.ning.http.client.webdav;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;
import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.uri.Uri;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple {@link AsyncHandler} that add support for WebDav's response manipulation.
 *
 * @param <T>
 */
public abstract class WebDavCompletionHandlerBase<T> implements AsyncHandler<T> {
    private final Logger logger = LoggerFactory.getLogger(AsyncCompletionHandlerBase.class);

    private final List<HttpResponseBodyPart> bodies =
            Collections.synchronizedList(new ArrayList<HttpResponseBodyPart>());
    private HttpResponseStatus status;
    private HttpResponseHeaders headers;

    @Override
    public final STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
        bodies.add(content);
        return STATE.CONTINUE;
    }

    @Override
    public final STATE onStatusReceived(final HttpResponseStatus status) throws Exception {
        this.status = status;
        return STATE.CONTINUE;
    }

    @Override
    public final STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
        this.headers = headers;
        return STATE.CONTINUE;
    }

    @Override
    public final T onCompleted() throws Exception {
        if (status != null) {
            Response response = status.prepareResponse(headers, bodies);
            Document document = null;
            if (status.getStatusCode() == 207) {
                document = readXMLResponse(response.getResponseBodyAsStream());
            }
            return onCompleted(new WebDavResponse(status.prepareResponse(headers, bodies), document));
        } else {
            throw new IllegalStateException("Status is null");
        }
    }

    @Override
    public void onThrowable(Throwable t) {
        logger.debug(t.getMessage(), t);
    }

    /**
     * Invoked once the HTTP response has been fully read.
     *
     * @param response The {@link com.ning.http.client.Response}
     * @return Type of the value that will be returned by the associated {@link java.util.concurrent.Future}
     */
    abstract public T onCompleted(WebDavResponse response) throws Exception;


    private class HttpStatusWrapper extends HttpResponseStatus {

        private final HttpResponseStatus wrapped;

        private final String statusText;

        private final int statusCode;

        public HttpStatusWrapper(HttpResponseStatus wrapper, String statusText, int statusCode) {
            super(wrapper.getUri(), wrapper.getConfig());
            this.wrapped = wrapper;
            this.statusText = statusText;
            this.statusCode = statusCode;
        }

        @Override
        public Response prepareResponse(HttpResponseHeaders headers, List<HttpResponseBodyPart> bodyParts) {
            final Response wrappedResponse = wrapped.prepareResponse(headers, bodyParts);

            return new Response() {

                @Override
                public int getStatusCode() {
                    return statusCode;
                }

                @Override
                public String getStatusText() {
                    return statusText;
                }

                @Override
                public byte[] getResponseBodyAsBytes() throws IOException {
                    return wrappedResponse.getResponseBodyAsBytes();
                }

                @Override
                public ByteBuffer getResponseBodyAsByteBuffer() throws IOException {
                    return wrappedResponse.getResponseBodyAsByteBuffer();
                }

                @Override
                public InputStream getResponseBodyAsStream() throws IOException {
                    return wrappedResponse.getResponseBodyAsStream();
                }

                @Override
                public String getResponseBodyExcerpt(int maxLength, String charset) throws IOException {
                    return wrappedResponse.getResponseBodyExcerpt(maxLength, charset);
                }

                @Override
                public String getResponseBody(String charset) throws IOException {
                    return wrappedResponse.getResponseBody(charset);
                }

                @Override
                public String getResponseBodyExcerpt(int maxLength) throws IOException {
                    return wrappedResponse.getResponseBodyExcerpt(maxLength);
                }

                @Override
                public String getResponseBody() throws IOException {
                    return wrappedResponse.getResponseBody();
                }

                @Override
                public Uri getUri() {
                    return wrappedResponse.getUri();
                }

                @Override
                public String getContentType() {
                    return wrappedResponse.getContentType();
                }

                @Override
                public String getHeader(String name) {
                    return wrappedResponse.getHeader(name);
                }

                @Override
                public List<String> getHeaders(String name) {
                    return wrappedResponse.getHeaders(name);
                }

                @Override
                public FluentCaseInsensitiveStringsMap getHeaders() {
                    return wrappedResponse.getHeaders();
                }

                @Override
                public boolean isRedirected() {
                    return wrappedResponse.isRedirected();
                }

                @Override
                public List<Cookie> getCookies() {
                    return wrappedResponse.getCookies();
                }

                @Override
                public boolean hasResponseStatus() {
                    return wrappedResponse.hasResponseStatus();
                }

                @Override
                public boolean hasResponseHeaders() {
                    return wrappedResponse.hasResponseHeaders();
                }

                @Override
                public boolean hasResponseBody() {
                    return wrappedResponse.hasResponseBody();
                }
            };
        }

        @Override
        public int getStatusCode() {
            return (statusText == null ? wrapped.getStatusCode() : statusCode);
        }

        @Override
        public String getStatusText() {
            return (statusText == null ? wrapped.getStatusText() : statusText);
        }

        @Override
        public String getProtocolName() {
            return wrapped.getProtocolName();
        }

        @Override
        public int getProtocolMajorVersion() {
            return wrapped.getProtocolMajorVersion();
        }

        @Override
        public int getProtocolMinorVersion() {
            return wrapped.getProtocolMinorVersion();
        }

        @Override
        public String getProtocolText() {
            return wrapped.getStatusText();
        }
    }

    private Document readXMLResponse(InputStream stream) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document = null;
        try {
            document = factory.newDocumentBuilder().parse(stream);
            parse(document);
        } catch (SAXException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return document;
    }

    private void parse(Document document) {
        Element element = document.getDocumentElement();
        NodeList statusNode = element.getElementsByTagName("status");
        for (int i = 0; i < statusNode.getLength(); i++) {
            Node node = statusNode.item(i);

            String value = node.getFirstChild().getNodeValue();
            int statusCode = Integer.valueOf(value.substring(value.indexOf(" "), value.lastIndexOf(" ")).trim());
            String statusText = value.substring(value.lastIndexOf(" "));
            status = new HttpStatusWrapper(status, statusText, statusCode);
        }
    }
}
