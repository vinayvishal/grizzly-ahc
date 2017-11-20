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

import com.ning.http.client.providers.grizzly.events.GracefulCloseEvent;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Request;
import com.ning.http.client.uri.Uri;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.util.AsyncHttpProviderUtils;
import com.ning.http.util.ProxyUtils;
import java.io.IOException;
import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.CloseType;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.http.HttpContext;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.websockets.HandShake;
import org.glassfish.grizzly.websockets.ProtocolHandler;

/**
 *
 * @author Grizzly team
 */
public final class HttpTransactionContext {
    private static final Attribute<HttpTransactionContext> REQUEST_STATE_ATTR =
            Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(HttpTransactionContext.class.getName());

    int redirectCount;
    final int maxRedirectCount;
    final boolean redirectsAllowed;
    final GrizzlyAsyncHttpProvider provider;
    final ProxyServer proxyServer;
        
    private final Request ahcRequest;
    Uri requestUri;
    
    private final Connection connection;
    
    PayloadGenerator payloadGenerator;
    
    StatusHandler statusHandler;
    // StatusHandler invocation status
    StatusHandler.InvocationStatus invocationStatus =
            StatusHandler.InvocationStatus.CONTINUE;
    
    GrizzlyResponseFuture future;
    HttpResponsePacket responsePacket;
    GrizzlyResponseStatus responseStatus;
    
    
    Uri lastRedirectUri;
    long totalBodyWritten;
    AsyncHandler.STATE currentState;
    Uri wsRequestURI;
    boolean isWSRequest;
    HandShake handshake;
    ProtocolHandler protocolHandler;
    WebSocket webSocket;
    boolean establishingTunnel;
    boolean skipCleanup;
    
    // don't recycle the context, don't return associated connection to
    // the pool
    boolean isReuseConnection;

    /**
     * <tt>true</tt> if the request is fully sent, or <tt>false</tt>otherwise.
     */
    private boolean isRequestFullySent;
    private CleanupTask cleanupTask;
    
    private final CloseListener listener = new CloseListener<Closeable, CloseType>() {
        @Override
        public void onClosed(Closeable closeable, CloseType type) throws IOException {
            if (isGracefullyFinishResponseOnClose() || isKeepAliveDisabled()) {
                // Connection was closed.
                // This event is fired only for responses, which don't have
                // associated transfer-encoding or content-length.
                // We have to complete such a request-response processing gracefully.
                final FilterChain fc = (FilterChain) connection.getProcessor();
                fc.fireEventUpstream(connection,
                        new GracefulCloseEvent(HttpTransactionContext.this), null);
            } else if (CloseType.REMOTELY.equals(type)) {
                abort(AsyncHttpProviderUtils.REMOTELY_CLOSED_EXCEPTION);
            } else {
                try {
                    closeable.assertOpen();
                } catch (IOException ioe) {
                    // unwrap the exception as it was wrapped by assertOpen.
                    abort(ioe.getCause());
                }
            }
        }
    };

    // -------------------------------------------------------- Static methods

    static void bind(final HttpContext httpCtx,
            final HttpTransactionContext httpTxContext) {
        httpCtx.getCloseable().addCloseListener(httpTxContext.listener);
        REQUEST_STATE_ATTR.set(httpCtx, httpTxContext);
    }

    static void cleanupTransaction(final HttpContext httpCtx,
            final CompletionHandler<HttpTransactionContext> completionHandler) {
        final HttpTransactionContext httpTxContext = currentTransaction(httpCtx);
        
        assert httpTxContext != null;
        
        httpTxContext.scheduleCleanup(httpCtx, completionHandler);
    }

    static HttpTransactionContext currentTransaction(
            final HttpHeader httpHeader) {
        return currentTransaction(httpHeader.getProcessingState().getHttpContext());
    }

    static HttpTransactionContext currentTransaction(final AttributeStorage storage) {
        return REQUEST_STATE_ATTR.get(storage);
    }

    static HttpTransactionContext currentTransaction(final HttpContext httpCtx) {
        return ((AhcHttpContext) httpCtx).getHttpTransactionContext();
    }
    
    static HttpTransactionContext startTransaction(
            final Connection connection, final GrizzlyAsyncHttpProvider provider,
            final Request request, final GrizzlyResponseFuture future) {
        return new HttpTransactionContext(provider, connection, future, request);
    }
    
    // -------------------------------------------------------- Constructors

    private HttpTransactionContext(final GrizzlyAsyncHttpProvider provider,
            final Connection connection,
            final GrizzlyResponseFuture future, final Request ahcRequest) {

        this.provider = provider;
        this.connection = connection;
        this.future = future;
        this.ahcRequest = ahcRequest;
        this.proxyServer = ProxyUtils.getProxyServer(
                provider.getClientConfig(), ahcRequest);
        redirectsAllowed = provider.getClientConfig().isFollowRedirect();
        maxRedirectCount = provider.getClientConfig().getMaxRedirects();
        this.requestUri = ahcRequest.getUri();
    }

    Connection getConnection() {
        return connection;
    }
    
    public AsyncHandler getAsyncHandler() {
        return future != null ? future.getAsyncHandler() : null;
    }
    
    Request getAhcRequest() {
        return ahcRequest;
    }

    ProxyServer getProxyServer() {
        return proxyServer;
    }
    
    // ----------------------------------------------------- Private Methods

    HttpTransactionContext cloneAndStartTransactionFor(
            final Connection connection) {
        return cloneAndStartTransactionFor(connection, ahcRequest);
    }

    HttpTransactionContext cloneAndStartTransactionFor(
            final Connection connection,
            final Request request) {
        final HttpTransactionContext newContext = startTransaction(
                connection, provider, request, future);
        newContext.invocationStatus = invocationStatus;
        newContext.payloadGenerator = payloadGenerator;
        newContext.currentState = currentState;
        newContext.statusHandler = statusHandler;
        newContext.lastRedirectUri = lastRedirectUri;
        newContext.redirectCount = redirectCount;
        
        // detach the future
        future = null;
        
        return newContext;
    }

    boolean isGracefullyFinishResponseOnClose() {
        final HttpResponsePacket response = responsePacket;
        return response != null &&
                !response.getProcessingState().isKeepAlive() &&
                !response.isChunked() &&
                response.getContentLength() == -1;
    }

    void abort(final Throwable t) {
        if (future != null) {
            future.abort(t);
        }
    }

    void done() {
        done(null);
    }

    @SuppressWarnings(value = {"unchecked"})
    void done(Object result) {
        if (future != null) {
            future.done(result);
        }
    }

    boolean isTunnelEstablished(final Connection c) {
        return c.getAttributes().getAttribute("tunnel-established") != null;
    }

    void tunnelEstablished(final Connection c) {
        c.getAttributes().setAttribute("tunnel-established", Boolean.TRUE);
    }
    
    void reuseConnection() {
        this.isReuseConnection = true;
    }

    boolean isReuseConnection() {
        return isReuseConnection;
    }

    void touchConnection() {
        provider.touchConnection(connection, ahcRequest);
    }

    void closeConnection() {
        connection.closeSilently();
    }

    void keepAliveDisabled() {
        connection.getAttributes().setAttribute("keep-alive-disabled", Boolean.TRUE);
    }

    boolean isKeepAliveDisabled() {
        return Boolean.TRUE.equals(connection.getAttributes().getAttribute("keep-alive-disabled"));
    }

    private void scheduleCleanup(final HttpContext httpCtx,
            final CompletionHandler<HttpTransactionContext> completionHandler) {
        synchronized (this) {
            if (!isRequestFullySent) {
                assert cleanupTask == null; // scheduleCleanup should be called only once
                cleanupTask = new CleanupTask(httpCtx, completionHandler);
                return;
            }
        }

        assert isRequestFullySent;
        cleanup(httpCtx);
        completionHandler.completed(this);
    }
    
    private void cleanup(final HttpContext httpCtx) {
        if (!skipCleanup) {
            httpCtx.getCloseable().removeCloseListener(listener);
            REQUEST_STATE_ATTR.remove(httpCtx);
        }
    }
    
    @SuppressWarnings("unchecked")
    void onRequestFullySent() {
        synchronized (this) {
            if (isRequestFullySent) {
                return;
            }
            
            isRequestFullySent = true;
        }
        
        if (cleanupTask != null) {
            cleanupTask.run();
        }
    }
    
    private class CleanupTask implements Runnable {
        private final HttpContext httpCtx;
        private final CompletionHandler<HttpTransactionContext> completionHandler;
        
        private CleanupTask(final HttpContext httpCtx,
                final CompletionHandler<HttpTransactionContext> completionHandler) {
            this.httpCtx = httpCtx;
            this.completionHandler = completionHandler;
        }

        @Override
        public void run() {
            cleanup(httpCtx);
            completionHandler.completed(HttpTransactionContext.this);
        }
        
    }
} // END HttpTransactionContext
