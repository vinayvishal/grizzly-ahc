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

import static com.ning.http.util.MiscUtils.isNonEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.multipart.Part;
import com.ning.http.client.uri.Uri;
import com.ning.http.util.AsyncHttpProviderUtils;
import com.ning.http.util.UriEncoder;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builder for {@link Request}
 * 
 * @param <T>
 */
public abstract class RequestBuilderBase<T extends RequestBuilderBase<T>> {
    private final static Logger logger = LoggerFactory.getLogger(RequestBuilderBase.class);

    private static final Uri DEFAULT_REQUEST_URL = Uri.create("http://localhost");

    private static final class RequestImpl implements Request {
        private String method;
        private Uri uri;
        private InetAddress address;
        private InetAddress localAddress;
        private FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
        private ArrayList<Cookie> cookies;
        private byte[] byteData;
        private List<byte[]> compositeByteData;
        private String stringData;
        private InputStream streamData;
        private BodyGenerator bodyGenerator;
        private List<Param> formParams;
        private List<Part> parts;
        private String virtualHost;
        private long length = -1;
        public ProxyServer proxyServer;
        private Realm realm;
        private File file;
        private Boolean followRedirects;
        private int requestTimeout;
        private long rangeOffset;
        public String charset;
        private ConnectionPoolPartitioning connectionPoolPartitioning = ConnectionPoolPartitioning.PerHostConnectionPoolPartitioning.INSTANCE;
        private NameResolver nameResolver = NameResolver.JdkNameResolver.INSTANCE;
        private List<Param> queryParams;

        public RequestImpl() {
        }

        public RequestImpl(Request prototype) {
            if (prototype != null) {
                this.method = prototype.getMethod();
                this.uri = prototype.getUri();
                this.address = prototype.getInetAddress();
                this.localAddress = prototype.getLocalAddress();
                this.headers = new FluentCaseInsensitiveStringsMap(prototype.getHeaders());
                this.cookies = new ArrayList<>(prototype.getCookies());
                this.byteData = prototype.getByteData();
                this.compositeByteData = prototype.getCompositeByteData();
                this.stringData = prototype.getStringData();
                this.streamData = prototype.getStreamData();
                this.bodyGenerator = prototype.getBodyGenerator();
                this.formParams = prototype.getFormParams() == null ? null : new ArrayList<>(prototype.getFormParams());
                this.parts = prototype.getParts() == null ? null : new ArrayList<>(prototype.getParts());
                this.virtualHost = prototype.getVirtualHost();
                this.length = prototype.getContentLength();
                this.proxyServer = prototype.getProxyServer();
                this.realm = prototype.getRealm();
                this.file = prototype.getFile();
                this.followRedirects = prototype.getFollowRedirect();
                this.requestTimeout = prototype.getRequestTimeout();
                this.rangeOffset = prototype.getRangeOffset();
                this.charset = prototype.getBodyEncoding();
                this.connectionPoolPartitioning = prototype.getConnectionPoolPartitioning();
                this.nameResolver = prototype.getNameResolver();
            }
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public InetAddress getInetAddress() {
            return address;
        }

        @Override
        public InetAddress getLocalAddress() {
            return localAddress;
        }

        @Override
        public Uri getUri() {
            return uri;
        }

        @Override
        public String getUrl() {
            return uri.toUrl();
        }

        @Override
        public FluentCaseInsensitiveStringsMap getHeaders() {
            return headers;
        }

        @Override
        public Collection<Cookie> getCookies() {
            return cookies != null ? Collections.unmodifiableCollection(cookies) : Collections.<Cookie> emptyList();
        }

        @Override
        public byte[] getByteData() {
            return byteData;
        }

        @Override
        public List<byte[]> getCompositeByteData() {
            return compositeByteData;
        }

        @Override
        public String getStringData() {
            return stringData;
        }

        @Override
        public InputStream getStreamData() {
            return streamData;
        }

        @Override
        public BodyGenerator getBodyGenerator() {
            return bodyGenerator;
        }

        @Override
        public long getContentLength() {
            return length;
        }

        @Override
        public List<Param> getFormParams() {
            return formParams != null ? formParams : Collections.<Param> emptyList();
        }

        @Override
        public List<Part> getParts() {
            return parts != null ? parts : Collections.<Part> emptyList();
        }

        @Override
        public String getVirtualHost() {
            return virtualHost;
        }

        @Override
        public ProxyServer getProxyServer() {
            return proxyServer;
        }

        @Override
        public Realm getRealm() {
            return realm;
        }

        @Override
        public File getFile() {
            return file;
        }

        @Override
        public Boolean getFollowRedirect() {
            return followRedirects;
        }

        @Override
        public int getRequestTimeout() {
            return requestTimeout;
        }

        @Override
        public long getRangeOffset() {
            return rangeOffset;
        }

        @Override
        public String getBodyEncoding() {
            return charset;
        }

        @Override
        public ConnectionPoolPartitioning getConnectionPoolPartitioning() {
            return connectionPoolPartitioning;
        }

        @Override
        public NameResolver getNameResolver() {
            return nameResolver;
        }
        
        @Override
        public List<Param> getQueryParams() {
            if (queryParams == null)
                // lazy load
                if (isNonEmpty(uri.getQuery())) {
                    queryParams = new ArrayList<>(1);
                    for (String queryStringParam : uri.getQuery().split("&")) {
                        int pos = queryStringParam.indexOf('=');
                        if (pos <= 0)
                            queryParams.add(new Param(queryStringParam, null));
                        else
                            queryParams.add(new Param(queryStringParam.substring(0, pos), queryStringParam.substring(pos + 1)));
                    }
                } else
                    queryParams = Collections.emptyList();
            return queryParams;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(getUrl());

            sb.append("\t");
            sb.append(method);
            sb.append("\theaders:");
            if (isNonEmpty(headers)) {
                for (String name : headers.keySet()) {
                    sb.append("\t");
                    sb.append(name);
                    sb.append(":");
                    sb.append(headers.getJoinedValue(name, ", "));
                }
            }
            if (isNonEmpty(formParams)) {
                sb.append("\tformParams:");
                for (Param param : formParams) {
                    sb.append("\t");
                    sb.append(param.getName());
                    sb.append(":");
                    sb.append(param.getValue());
                }
            }

            return sb.toString();
        }
    }

    private final Class<T> derived;
    protected final RequestImpl request;
    protected UriEncoder uriEncoder;
    protected List<Param> rbQueryParams;
    protected SignatureCalculator signatureCalculator;

    protected RequestBuilderBase(Class<T> derived, String method, boolean disableUrlEncoding) {
        this(derived, method, UriEncoder.uriEncoder(disableUrlEncoding));
    }

    protected RequestBuilderBase(Class<T> derived, String method, UriEncoder uriEncoder) {
        this.derived = derived;
        request = new RequestImpl();
        request.method = method;
        this.uriEncoder = uriEncoder;
    }

    protected RequestBuilderBase(Class<T> derived, Request prototype) {
        this(derived, prototype, UriEncoder.FIXING);
    }

    protected RequestBuilderBase(Class<T> derived, Request prototype, UriEncoder uriEncoder) {
        this.derived = derived;
        request = new RequestImpl(prototype);
        this.uriEncoder = uriEncoder;
    }
    
    public T setUrl(String url) {
        return setUri(Uri.create(url));
    }

    public T setUri(Uri uri) {
        request.uri = uri;
        return derived.cast(this);
    }

    public T setInetAddress(InetAddress address) {
        request.address = address;
        return derived.cast(this);
    }

    public T setLocalInetAddress(InetAddress address) {
        request.localAddress = address;
        return derived.cast(this);
    }

    public T setVirtualHost(String virtualHost) {
        request.virtualHost = virtualHost;
        return derived.cast(this);
    }

    public T setHeader(String name, String value) {
        request.headers.replaceWith(name, value);
        return derived.cast(this);
    }

    public T addHeader(String name, String value) {
        if (value == null) {
            logger.warn("Value was null, set to \"\"");
            value = "";
        }

        request.headers.add(name, value);
        return derived.cast(this);
    }

    public T setHeaders(FluentCaseInsensitiveStringsMap headers) {
        request.headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : new FluentCaseInsensitiveStringsMap(headers));
        return derived.cast(this);
    }

    public T setHeaders(Map<String, Collection<String>> headers) {
        request.headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : new FluentCaseInsensitiveStringsMap(headers));
        return derived.cast(this);
    }

    public T setContentLength(int length) {
        request.length = length;
        return derived.cast(this);
    }

    private void lazyInitCookies() {
        if (request.cookies == null)
            request.cookies = new ArrayList<>(3);
    }

    public T setCookies(Collection<Cookie> cookies) {
        request.cookies = new ArrayList<>(cookies);
        return derived.cast(this);
    }

    public T addCookie(Cookie cookie) {
        lazyInitCookies();
        request.cookies.add(cookie);
        return derived.cast(this);
    }

    public T addOrReplaceCookie(Cookie cookie) {
        String cookieKey = cookie.getName();
        boolean replace = false;
        int index = 0;
        lazyInitCookies();
        for (Cookie c : request.cookies) {
            if (c.getName().equals(cookieKey)) {
                replace = true;
                break;
            }

            index++;
        }
        if (replace)
            request.cookies.set(index, cookie);
        else
            request.cookies.add(cookie);
        return derived.cast(this);
    }
    
    public void resetCookies() {
        if (request.cookies != null)
            request.cookies.clear();
    }
    
    public void resetQuery() {
        rbQueryParams = null;
        request.uri = request.uri.withNewQuery(null);
    }
    
    public void resetFormParams() {
        request.formParams = null;
    }

    public void resetNonMultipartData() {
        request.byteData = null;
        request.compositeByteData = null;
        request.stringData = null;
        request.streamData = null;
        request.bodyGenerator = null;
        request.length = -1;
    }

    public void resetMultipartData() {
        request.parts = null;
    }

    public T setBody(File file) {
        request.file = file;
        return derived.cast(this);
    }

    public T setBody(byte[] data) {
        resetFormParams();
        resetNonMultipartData();
        resetMultipartData();
        request.byteData = data;
        return derived.cast(this);
    }

    public T setBody(List<byte[]> data) {
        resetFormParams();
        resetNonMultipartData();
        resetMultipartData();
        request.compositeByteData = data;
        return derived.cast(this);
    }
    
    public T setBody(String data) {
        resetFormParams();
        resetNonMultipartData();
        resetMultipartData();
        request.stringData = data;
        return derived.cast(this);
    }

    public T setBody(InputStream stream) {
        resetFormParams();
        resetNonMultipartData();
        resetMultipartData();
        request.streamData = stream;
        return derived.cast(this);
    }

    public T setBody(BodyGenerator bodyGenerator) {
        request.bodyGenerator = bodyGenerator;
        return derived.cast(this);
    }

    public T addQueryParam(String name, String value) {
        if (rbQueryParams == null)
            rbQueryParams = new ArrayList<>(1);
        rbQueryParams.add(new Param(name, value));
        return derived.cast(this);
    }

    public T addQueryParams(List<Param> params) {
        if (rbQueryParams == null)
            rbQueryParams = params;
        else
            rbQueryParams.addAll(params);
        return derived.cast(this);
    }

    private List<Param> map2ParamList(Map<String, List<String>> map) {
        if (map == null)
            return null;

        List<Param> params = new ArrayList<>(map.size());
        for (Map.Entry<String, List<String>> entries : map.entrySet()) {
            String name = entries.getKey();
            for (String value : entries.getValue())
                params.add(new Param(name, value));
        }
        return params;
    }
    
    public T setQueryParams(Map<String, List<String>> map) {
        return setQueryParams(map2ParamList(map));
    }

    public T setQueryParams(List<Param> params) {
        // reset existing query
        if (isNonEmpty(request.uri.getQuery()))
            request.uri = request.uri.withNewQuery(null);
        rbQueryParams = params;
        return derived.cast(this);
    }
    
    public T addFormParam(String name, String value) {
        resetNonMultipartData();
        resetMultipartData();
        if (request.formParams == null)
            request.formParams = new ArrayList<>(1);
        request.formParams.add(new Param(name, value));
        return derived.cast(this);
    }

    public T setFormParams(Map<String, List<String>> map) {
        return setFormParams(map2ParamList(map));
    }
    public T setFormParams(List<Param> params) {
        resetNonMultipartData();
        resetMultipartData();
        request.formParams = params;
        return derived.cast(this);
    }

    public T addBodyPart(Part part) {
        resetFormParams();
        resetNonMultipartData();
        if (request.parts == null)
            request.parts = new ArrayList<Part>();
        request.parts.add(part);
        return derived.cast(this);
    }

    public T setProxyServer(ProxyServer proxyServer) {
        request.proxyServer = proxyServer;
        return derived.cast(this);
    }

    public T setRealm(Realm realm) {
        request.realm = realm;
        return derived.cast(this);
    }

    public T setFollowRedirects(boolean followRedirects) {
        request.followRedirects = followRedirects;
        return derived.cast(this);
    }

    public T setRequestTimeout(int requestTimeout) {
        request.requestTimeout = requestTimeout;
        return derived.cast(this);
    }

    public T setRangeOffset(long rangeOffset) {
        request.rangeOffset = rangeOffset;
        return derived.cast(this);
    }

    public T setMethod(String method) {
        request.method = method;
        return derived.cast(this);
    }

    public T setBodyEncoding(String charset) {
        request.charset = charset;
        return derived.cast(this);
    }

    public T setConnectionPoolKeyStrategy(ConnectionPoolPartitioning connectionPoolKeyStrategy) {
        request.connectionPoolPartitioning = connectionPoolKeyStrategy;
        return derived.cast(this);
    }

    public T setNameResolver(NameResolver nameResolver) {
        request.nameResolver = nameResolver;
        return derived.cast(this);
    }

    public T setSignatureCalculator(SignatureCalculator signatureCalculator) {
        this.signatureCalculator = signatureCalculator;
        return derived.cast(this);
    }

    private void executeSignatureCalculator() {
        /* Let's first calculate and inject signature, before finalizing actual build
         * (order does not matter with current implementation but may in future)
         */
        if (signatureCalculator != null) {
            RequestBuilder rb = new RequestBuilder(request).setSignatureCalculator(null);
            rb.rbQueryParams = this.rbQueryParams;
            Request unsignedRequest = rb.build();
            signatureCalculator.calculateAndAddSignature(unsignedRequest, this);
        }
    }
    
    private void computeRequestCharset() {
        if (request.charset == null) {
            try {
                final String contentType = request.headers.getFirstValue("Content-Type");
                if (contentType != null) {
                    final String charset = AsyncHttpProviderUtils.parseCharset(contentType);
                    if (charset != null) {
                        // ensure that if charset is provided with the Content-Type header,
                        // we propagate that down to the charset of the Request object
                        request.charset = charset;
                    }
                }
            } catch (Throwable e) {
                // NoOp -- we can't fix the Content-Type or charset from here
            }
        }
    }
    
    private void computeRequestLength() {
        if (request.length < 0 && request.streamData == null) {
            // can't concatenate content-length
            final String contentLength = request.headers.getFirstValue("Content-Length");

            if (contentLength != null) {
                try {
                    request.length = Long.parseLong(contentLength);
                } catch (NumberFormatException e) {
                    // NoOp -- we wdn't specify length so it will be chunked?
                }
            }
        }
    }

    private void validateSupportedScheme(Uri uri) {
        final String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("ws")
                && !scheme.equalsIgnoreCase("wss")) {
            throw new IllegalArgumentException("The URI scheme, of the URI " + uri
                    + ", must be equal (ignoring case) to 'http', 'https', 'ws', or 'wss'");
        }
    }
    
    private void computeFinalUri() {

        if (request.uri == null) {
            logger.debug("setUrl hasn't been invoked. Using {}", DEFAULT_REQUEST_URL);
            request.uri = DEFAULT_REQUEST_URL;
        } else {
            validateSupportedScheme(request.uri);
        }

        request.uri =  uriEncoder.encode(request.uri, rbQueryParams);
    }

    public Request build() {
        executeSignatureCalculator();
        computeFinalUri();
        computeRequestCharset();
        computeRequestLength();
        return request;
    }
}
