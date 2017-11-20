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

import static com.ning.http.client.AsyncHttpClientConfigDefaults.*;

import com.ning.http.client.filter.IOExceptionFilter;
import com.ning.http.client.filter.RequestFilter;
import com.ning.http.client.filter.ResponseFilter;
import com.ning.http.util.ProxyUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Simple JavaBean version of  {@link AsyncHttpClientConfig}
 */
public class AsyncHttpClientConfigBean extends AsyncHttpClientConfig {

    public AsyncHttpClientConfigBean() {
        configureExecutors();
        configureDefaults();
        configureFilters();
    }

    void configureFilters() {
        requestFilters = new LinkedList<>();
        responseFilters = new LinkedList<>();
        ioExceptionFilters = new LinkedList<>();
    }

    void configureDefaults() {
        maxConnections = defaultMaxConnections();
        maxConnectionsPerHost = defaultMaxConnectionsPerHost();
        connectTimeout = defaultConnectTimeout();
        webSocketTimeout = defaultWebSocketTimeout();
        pooledConnectionIdleTimeout = defaultPooledConnectionIdleTimeout();
        readTimeout = defaultReadTimeout();
        requestTimeout = defaultRequestTimeout();
        connectionTTL = defaultConnectionTTL();
        followRedirect = defaultFollowRedirect();
        maxRedirects = defaultMaxRedirects();
        compressionEnforced = defaultCompressionEnforced();
        userAgent = defaultUserAgent();
        allowPoolingConnections = defaultAllowPoolingConnections();
        useRelativeURIsWithConnectProxies = defaultUseRelativeURIsWithConnectProxies();
        maxRequestRetry = defaultMaxRequestRetry();
        ioThreadMultiplier = defaultIoThreadMultiplier();
        allowPoolingSslConnections = defaultAllowPoolingSslConnections();
        disableUrlEncodingForBoundRequests = defaultDisableUrlEncodingForBoundRequests();
        strict302Handling = defaultStrict302Handling();
        acceptAnyCertificate = defaultAcceptAnyCertificate();
        sslSessionCacheSize = defaultSslSessionCacheSize();
        sslSessionTimeout = defaultSslSessionTimeout();

        if (defaultUseProxySelector()) {
            proxyServerSelector = ProxyUtils.getJdkDefaultProxyServerSelector();
        } else if (defaultUseProxyProperties()) {
            proxyServerSelector = ProxyUtils.createProxyServerSelector(System.getProperties());
        }
    }

    void configureExecutors() {
        applicationThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AsyncHttpClient-Callback");
                t.setDaemon(true);
                return t;
            }
        });
    }

    public AsyncHttpClientConfigBean setMaxTotalConnections(int maxTotalConnections) {
        this.maxConnections = maxTotalConnections;
        return this;
    }

    public AsyncHttpClientConfigBean setMaxConnectionPerHost(int maxConnectionPerHost) {
        this.maxConnectionsPerHost = maxConnectionPerHost;
        return this;
    }

    public AsyncHttpClientConfigBean setConnectionTimeOut(int connectionTimeOut) {
        this.connectTimeout = connectionTimeOut;
        return this;
    }

    public AsyncHttpClientConfigBean setIdleConnectionInPoolTimeout(int idleConnectionInPoolTimeout) {
        this.pooledConnectionIdleTimeout = idleConnectionInPoolTimeout;
        return this;
    }

    public AsyncHttpClientConfigBean setStrict302Handling(boolean strict302Handling) {
        this.strict302Handling = strict302Handling;
        return this;
    }

    public AsyncHttpClientConfigBean setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public AsyncHttpClientConfigBean setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public AsyncHttpClientConfigBean setMaxConnectionLifeTime(int maxConnectionLifeTime) {
        this.connectionTTL = maxConnectionLifeTime;
        return this;
    }

    public AsyncHttpClientConfigBean setFollowRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
        return this;
    }

    public AsyncHttpClientConfigBean setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
        return this;
    }

    public AsyncHttpClientConfigBean setCompressionEnforced(boolean compressionEnforced) {
        this.compressionEnforced = compressionEnforced;
        return this;
    }

    public AsyncHttpClientConfigBean setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public AsyncHttpClientConfigBean setAllowPoolingConnection(boolean allowPoolingConnection) {
        this.allowPoolingConnections = allowPoolingConnection;
        return this;
    }

    public AsyncHttpClientConfigBean setApplicationThreadPool(ExecutorService applicationThreadPool) {
        if (this.applicationThreadPool != null) {
            this.applicationThreadPool.shutdownNow();
        }
        this.applicationThreadPool = applicationThreadPool;
        return this;
    }

    public AsyncHttpClientConfigBean setProxyServer(ProxyServer proxyServer) {
        this.proxyServerSelector = ProxyUtils.createProxyServerSelector(proxyServer);
        return this;
    }

    public AsyncHttpClientConfigBean setProxyServerSelector(ProxyServerSelector proxyServerSelector) {
        this.proxyServerSelector = proxyServerSelector;
        return this;
    }

    public AsyncHttpClientConfigBean setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public AsyncHttpClientConfigBean setProviderConfig(AsyncHttpProviderConfig<?, ?> providerConfig) {
        this.providerConfig = providerConfig;
        return this;
    }

    public AsyncHttpClientConfigBean setRealm(Realm realm) {
        this.realm = realm;
        return this;
    }

    public AsyncHttpClientConfigBean addRequestFilter(RequestFilter requestFilter) {
        requestFilters.add(requestFilter);
        return this;
    }

    public AsyncHttpClientConfigBean addResponseFilters(ResponseFilter responseFilter) {
        responseFilters.add(responseFilter);
        return this;
    }

    public AsyncHttpClientConfigBean addIoExceptionFilters(IOExceptionFilter ioExceptionFilter) {
        ioExceptionFilters.add(ioExceptionFilter);
        return this;
    }

    public AsyncHttpClientConfigBean setMaxRequestRetry(int maxRequestRetry) {
        this.maxRequestRetry = maxRequestRetry;
        return this;
    }

    public AsyncHttpClientConfigBean setAllowSslConnectionPool(boolean allowSslConnectionPool) {
        this.allowPoolingSslConnections = allowSslConnectionPool;
        return this;
    }

    public AsyncHttpClientConfigBean setDisableUrlEncodingForBoundRequests(boolean disableUrlEncodingForBoundRequests) {
        this.disableUrlEncodingForBoundRequests = disableUrlEncodingForBoundRequests;
        return this;
    }

    public AsyncHttpClientConfigBean setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    public AsyncHttpClientConfigBean setIoThreadMultiplier(int ioThreadMultiplier) {
        this.ioThreadMultiplier = ioThreadMultiplier;
        return this;
    }

    public AsyncHttpClientConfigBean setAcceptAnyCertificate(boolean acceptAnyCertificate) {
        this.acceptAnyCertificate = acceptAnyCertificate;
        return this;
    }

    public AsyncHttpClientConfigBean setSslSessionCacheSize(Integer sslSessionCacheSize) {
        this.sslSessionCacheSize = sslSessionCacheSize;
        return this;
    }

    public AsyncHttpClientConfigBean setSslSessionTimeout(Integer sslSessionTimeout) {
        this.sslSessionTimeout = sslSessionTimeout;
        return this;
    }
}
