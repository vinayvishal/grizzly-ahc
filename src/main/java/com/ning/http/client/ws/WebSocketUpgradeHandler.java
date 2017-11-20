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

package com.ning.http.client.ws;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.UpgradeHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link AsyncHandler} which is able to execute WebSocket upgrade. Use the Builder for configuring WebSocket options.
 */
public class WebSocketUpgradeHandler implements UpgradeHandler<WebSocket>, AsyncHandler<WebSocket> {

    private WebSocket webSocket;
    private final List<WebSocketListener> listeners;
    private final AtomicBoolean ok = new AtomicBoolean(false);
    private boolean onSuccessCalled;
    private int status;

    protected WebSocketUpgradeHandler() {
        this.listeners = new LinkedList<>();
    }

    protected WebSocketUpgradeHandler(List<WebSocketListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onThrowable(Throwable t) {
        onFailure(t);
    }

    public boolean touchSuccess() {
        boolean prev = onSuccessCalled;
        onSuccessCalled = true;
        return prev;
    }

    @Override
    public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
        return STATE.CONTINUE;
    }

    @Override
    public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
        status = responseStatus.getStatusCode();
        return status == 101 ? STATE.UPGRADE : STATE.ABORT;
    }

    @Override
    public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        return STATE.CONTINUE;
    }

    @Override
    public WebSocket onCompleted() throws Exception {

        if (status != 101) {
            IllegalStateException e = new IllegalStateException("Invalid Status Code " + status);
            for (WebSocketListener listener : listeners) {
                listener.onError(e);
            }
            throw e;
        }

        if (webSocket == null) {
            throw new NullPointerException("webSocket");
        }
        return webSocket;
    }

    @Override
    public void onSuccess(WebSocket webSocket) {
        this.webSocket = webSocket;
        for (WebSocketListener listener : listeners) {
            webSocket.addWebSocketListener(listener);
            listener.onOpen(webSocket);
        }
        ok.set(true);
    }

    @Override
    public void onFailure(Throwable t) {
        for (WebSocketListener w : listeners) {
            if (!ok.get() && webSocket != null) {
                webSocket.addWebSocketListener(w);
            }
            w.onError(t);
        }
    }

    public void onClose(WebSocket webSocket, int status, String reasonPhrase) {
        // Connect failure
        if (this.webSocket == null) this.webSocket = webSocket;

        for (WebSocketListener listener : listeners) {
            if (webSocket != null) {
                webSocket.addWebSocketListener(listener);
            }
            listener.onClose(webSocket);
            if (listener instanceof WebSocketCloseCodeReasonListener) {
                WebSocketCloseCodeReasonListener.class.cast(listener).onClose(webSocket, status, reasonPhrase);
            }
        }
    }

    /**
     * Build a {@link WebSocketUpgradeHandler}
     */
    public final static class Builder {

        private List<WebSocketListener> listeners = new ArrayList<>(1);

        /**
         * Add a {@link WebSocketListener} that will be added to the {@link WebSocket}
         *
         * @param listener a {@link WebSocketListener}
         * @return this
         */
        public Builder addWebSocketListener(WebSocketListener listener) {
            listeners.add(listener);
            return this;
        }

        /**
         * Remove a {@link WebSocketListener}
         *
         * @param listener a {@link WebSocketListener}
         * @return this
         */
        public Builder removeWebSocketListener(WebSocketListener listener) {
            listeners.remove(listener);
            return this;
        }

        /**
         * Build a {@link WebSocketUpgradeHandler}
         *
         * @return a {@link WebSocketUpgradeHandler}
         */
        public WebSocketUpgradeHandler build() {
            return new WebSocketUpgradeHandler(listeners);
        }
    }
}
