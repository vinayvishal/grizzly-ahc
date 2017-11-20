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

import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Request;
import com.ning.http.client.uri.Uri;
import com.ning.http.util.ProxyUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ConnectorHandler;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.connectionpool.ConnectionInfo;
import org.glassfish.grizzly.connectionpool.Endpoint;
import org.glassfish.grizzly.connectionpool.MultiEndpointPool;
import org.glassfish.grizzly.connectionpool.SingleEndpointPool;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.utils.Exceptions;

/**
 * Connection manager.
 * 
 * @author Grizzly team
 */
class ConnectionManager {
    private static final Attribute<Boolean> IS_NOT_KEEP_ALIVE =
            Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(
                    ConnectionManager.class.getName() + ".is-not-keepalive");

    private final boolean poolingEnabled;
    private final MultiEndpointPool<SocketAddress> pool;
    
    private final TCPNIOTransport transport;
    private final TCPNIOConnectorHandler defaultConnectionHandler;
    private final AsyncHttpClientConfig config;
    private final boolean poolingSSLConnections;
    private final Map<String, Endpoint> endpointMap =
            new ConcurrentHashMap<String,Endpoint>();

    // -------------------------------------------------------- Constructors
    ConnectionManager(final GrizzlyAsyncHttpProvider provider,
            final TCPNIOTransport transport,
            final GrizzlyAsyncHttpProviderConfig providerConfig) {
        
        this.transport = transport;
        config = provider.getClientConfig();
        this.poolingEnabled = config.isAllowPoolingConnections();
        this.poolingSSLConnections = config.isAllowPoolingSslConnections();
        
        defaultConnectionHandler = TCPNIOConnectorHandler.builder(transport).build();
        
        if (providerConfig != null && providerConfig.getConnectionPool() != null) {
            pool = providerConfig.getConnectionPool();
        } else {
            if (poolingEnabled) {
                final MultiEndpointPool.Builder<SocketAddress> builder
                        = MultiEndpointPool.builder(SocketAddress.class)
                        .connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                        .asyncPollTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                        .maxConnectionsTotal(config.getMaxConnections())
                        .maxConnectionsPerEndpoint(config.getMaxConnectionsPerHost())
                        .keepAliveTimeout(config.getPooledConnectionIdleTimeout(), TimeUnit.MILLISECONDS)
                        .keepAliveCheckInterval(1, TimeUnit.SECONDS)
                        .connectorHandler(defaultConnectionHandler)
                        .connectionTTL(config.getConnectionTTL(), TimeUnit.MILLISECONDS)
                        .failFastWhenMaxSizeReached(true);

                if (!poolingSSLConnections) {
                    builder.endpointPoolCustomizer(new NoSSLPoolCustomizer());
                }

                pool = builder.build();
            } else {
                pool = MultiEndpointPool.builder(SocketAddress.class)
                        .connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                        .asyncPollTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                        .maxConnectionsTotal(config.getMaxConnections())
                        .maxConnectionsPerEndpoint(config.getMaxConnectionsPerHost())
                        .keepAliveTimeout(0, TimeUnit.MILLISECONDS) // no pool
                        .connectorHandler(defaultConnectionHandler)
                        .failFastWhenMaxSizeReached(true)
                        .build();
            }
        }
    }

    // ----------------------------------------------------- Private Methods
    void openAsync(final Request request,
            final CompletionHandler<Connection> completionHandler)
            throws IOException {
        
        final ProxyServer proxy = ProxyUtils.getProxyServer(config, request);
        
        final String scheme;
        final String host;
        final int port;
        if (proxy != null) {
            scheme = proxy.getProtocol().getProtocol();
            host = proxy.getHost();
            port = getPort(scheme, proxy.getPort());
        } else {
            final Uri uri = request.getUri();
            scheme = uri.getScheme();
            host = uri.getHost();
            port = getPort(scheme, uri.getPort());
        }
        
        final String partitionId = getPartitionId(request.getInetAddress(), request, proxy);
        Endpoint endpoint = endpointMap.get(partitionId);
        if (endpoint == null) {
            final boolean isSecure = Utils.isSecure(scheme);
            endpoint = new AhcEndpoint(partitionId,
                    isSecure, request.getInetAddress(), host, port, request.getLocalAddress(),
                    defaultConnectionHandler);

            endpointMap.put(partitionId, endpoint);
        }

        pool.take(endpoint, completionHandler);
    }

    Connection openSync(final Request request)
            throws IOException {
        
        final ProxyServer proxy = ProxyUtils.getProxyServer(config, request);
        
        final String scheme;
        final String host;
        final int port;
        if (proxy != null) {
            scheme = proxy.getProtocol().getProtocol();
            host = proxy.getHost();
            port = getPort(scheme, proxy.getPort());
        } else {
            final Uri uri = request.getUri();
            scheme = uri.getScheme();
            host = uri.getHost();
            port = getPort(scheme, uri.getPort());
        }
        
        final boolean isSecure = Utils.isSecure(scheme);
        
        final String partitionId = getPartitionId(request.getInetAddress(), request, proxy);
        Endpoint endpoint = endpointMap.get(partitionId);
        if (endpoint == null) {
            endpoint = new AhcEndpoint(partitionId,
                    isSecure, request.getInetAddress(), host, port, request.getLocalAddress(),
                    defaultConnectionHandler);

            endpointMap.put(partitionId, endpoint);
        }

        Connection c = pool.poll(endpoint);
        
        if (c == null) {
            final Future<Connection> future =
                    defaultConnectionHandler.connect(
                    new InetSocketAddress(host, port),
                    request.getLocalAddress() != null
                            ? new InetSocketAddress(request.getLocalAddress(), 0)
                            : null);

            final int cTimeout = config.getConnectTimeout();
            try {
                c = cTimeout > 0
                        ? future.get(cTimeout, TimeUnit.MILLISECONDS)
                        : future.get();
            } catch (ExecutionException ee) {
                throw Exceptions.makeIOException(ee.getCause());
            } catch (Exception e) {
                throw Exceptions.makeIOException(e);
            } finally {
                future.cancel(false);
            }
        }

        assert c != null; // either connection is not null or exception thrown
        return c;
    }

    boolean returnConnection(final Connection c) {
        return pool.release(c);
    }

    void destroy() {
        pool.close();
    }

    boolean isReadyInPool(final Connection c) {
        final ConnectionInfo<SocketAddress> ci = pool.getConnectionInfo(c);
        return ci != null && ci.isReady();
    }
    
    static boolean isKeepAlive(final Connection connection) {
        return !IS_NOT_KEEP_ALIVE.isSet(connection);
    }
    
    private static String getPartitionId(InetAddress overrideAddress, Request request,
            ProxyServer proxyServer) {
        return (overrideAddress != null ? overrideAddress.toString() + "_" : "") + 
            request.getConnectionPoolPartitioning()
             .getPartitionKey(request.getUri(), proxyServer).toString();
    }

    private static int getPort(final String scheme, final int p) {
        int port = p;
        if (port == -1) {
            final String protocol = scheme.toLowerCase(Locale.ENGLISH);
            if ("http".equals(protocol) || "ws".equals(protocol)) {
                port = 80;
            } else if ("https".equals(protocol) || "wss".equals(protocol)) {
                port = 443;
            } else {
                throw new IllegalArgumentException("Unknown protocol: " + protocol);
            }
        }
        return port;
    }

    private class AhcEndpoint extends Endpoint<SocketAddress> {

        private final String partitionId;
        private final boolean isSecure;
        private final InetAddress remoteOverrideAddress;
        private final String host;
        private final int port;
        private final InetAddress localAddress;
        private final ConnectorHandler<SocketAddress> connectorHandler;
        
        private AhcEndpoint(final String partitionId,
                final boolean isSecure,
                final InetAddress remoteOverrideAddress, final String host, final int port,
                final InetAddress localAddress,
                final ConnectorHandler<SocketAddress> connectorHandler) {
            
            this.partitionId = partitionId;
            this.isSecure = isSecure;
            this.remoteOverrideAddress = remoteOverrideAddress;
            this.host = host;
            this.port = port;
            this.localAddress = localAddress;
            this.connectorHandler = connectorHandler;
        }

        public boolean isSecure() {
            return isSecure;
        }
        
        @Override
        public Object getId() {
            return partitionId;
        }

        @Override
        public GrizzlyFuture<Connection> connect() {
            return (GrizzlyFuture<Connection>) connectorHandler.connect(
                    buildRemoteSocketAddress(),
                    localAddress != null
                            ? new InetSocketAddress(localAddress, 0)
                            : null);
        }

        private InetSocketAddress buildRemoteSocketAddress()
        {
            return remoteOverrideAddress != null
                    ? new InetSocketAddress(remoteOverrideAddress, port)
                    : new InetSocketAddress(host, port);
        }

        @Override
        protected void onConnect(final Connection connection,
                final SingleEndpointPool<SocketAddress> pool) {
            if (pool.getKeepAliveTimeout(TimeUnit.MILLISECONDS) == 0) {
                IS_NOT_KEEP_ALIVE.set(connection, Boolean.TRUE);
            }
        }
    }
    
    private class NoSSLPoolCustomizer
            implements MultiEndpointPool.EndpointPoolCustomizer<SocketAddress> {

        @Override
        public void customize(final Endpoint<SocketAddress> endpoint,
                final MultiEndpointPool.EndpointPoolBuilder<SocketAddress> builder) {
            final AhcEndpoint ahcEndpoint = (AhcEndpoint) endpoint;
            if (ahcEndpoint.isSecure()) {
                builder.keepAliveTimeout(0, TimeUnit.SECONDS); // don't pool
            }
        }
        
    }
} // END ConnectionManager
