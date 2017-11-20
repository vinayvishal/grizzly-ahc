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

package com.ning.http.client.providers.grizzly.websocket;

import com.ning.http.client.ws.WebSocketByteListener;
import com.ning.http.client.ws.WebSocketCloseCodeReasonListener;
import com.ning.http.client.ws.WebSocketListener;
import com.ning.http.client.ws.WebSocketPingListener;
import com.ning.http.client.ws.WebSocketPongListener;
import com.ning.http.client.ws.WebSocketTextListener;
import java.io.ByteArrayOutputStream;
import org.glassfish.grizzly.websockets.ClosingFrame;
import org.glassfish.grizzly.websockets.DataFrame;

/**
 * AHC WebSocketListener
 */
final class AHCWebSocketListenerAdapter implements org.glassfish.grizzly.websockets.WebSocketListener {
    private final WebSocketListener ahcListener;
    private final GrizzlyWebSocketAdapter webSocket;
    private final StringBuilder stringBuffer;
    private final ByteArrayOutputStream byteArrayOutputStream;

    // -------------------------------------------------------- Constructors
    AHCWebSocketListenerAdapter(final WebSocketListener ahcListener, final GrizzlyWebSocketAdapter webSocket) {
        this.ahcListener = ahcListener;
        this.webSocket = webSocket;
        if (webSocket.bufferFragments) {
            stringBuffer = new StringBuilder();
            byteArrayOutputStream = new ByteArrayOutputStream();
        } else {
            stringBuffer = null;
            byteArrayOutputStream = null;
        }
    }

    // ------------------------------ Methods from Grizzly WebSocketListener
    @Override
    public void onClose(org.glassfish.grizzly.websockets.WebSocket gWebSocket, DataFrame dataFrame) {
        try {
            if (ahcListener instanceof WebSocketCloseCodeReasonListener) {
                ClosingFrame cf = ClosingFrame.class.cast(dataFrame);
                WebSocketCloseCodeReasonListener.class.cast(ahcListener).onClose(webSocket, cf.getCode(), cf.getReason());
            } else {
                ahcListener.onClose(webSocket);
            }
        } catch (Throwable e) {
            ahcListener.onError(e);
        }
    }

    @Override
    public void onConnect(org.glassfish.grizzly.websockets.WebSocket gWebSocket) {
        try {
            ahcListener.onOpen(webSocket);
        } catch (Throwable e) {
            ahcListener.onError(e);
        }
    }

    @Override
    public void onMessage(org.glassfish.grizzly.websockets.WebSocket webSocket, String s) {
        try {
            if (ahcListener instanceof WebSocketTextListener) {
                WebSocketTextListener.class.cast(ahcListener).onMessage(s);
            }
        } catch (Throwable e) {
            ahcListener.onError(e);
        }
    }

    @Override
    public void onMessage(org.glassfish.grizzly.websockets.WebSocket webSocket, byte[] bytes) {
        try {
            if (ahcListener instanceof WebSocketByteListener) {
                WebSocketByteListener.class.cast(ahcListener).onMessage(bytes);
            }
        } catch (Throwable e) {
            ahcListener.onError(e);
        }
    }

    @Override
    public void onPing(org.glassfish.grizzly.websockets.WebSocket webSocket, byte[] bytes) {
        try {
            if (ahcListener instanceof WebSocketPingListener) {
                WebSocketPingListener.class.cast(ahcListener).onPing(bytes);
            }
        } catch (Throwable e) {
            ahcListener.onError(e);
        }
    }

    @Override
    public void onPong(org.glassfish.grizzly.websockets.WebSocket webSocket, byte[] bytes) {
        try {
            if (ahcListener instanceof WebSocketPongListener) {
                WebSocketPongListener.class.cast(ahcListener).onPong(bytes);
            }
        } catch (Throwable e) {
            ahcListener.onError(e);
        }
    }

    @Override
    public void onFragment(org.glassfish.grizzly.websockets.WebSocket webSocket, String s, boolean last) {
        try {
            if (this.webSocket.bufferFragments) {
                synchronized (this.webSocket) {
                    stringBuffer.append(s);
                    if (last) {
                        if (ahcListener instanceof WebSocketTextListener) {
                            final String message = stringBuffer.toString();
                            stringBuffer.setLength(0);
                            WebSocketTextListener.class.cast(ahcListener).onMessage(message);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            ahcListener.onError(e);
        }
    }

    @Override
    public void onFragment(org.glassfish.grizzly.websockets.WebSocket webSocket, byte[] bytes, boolean last) {
        try {
            if (this.webSocket.bufferFragments) {
                synchronized (this.webSocket) {
                    byteArrayOutputStream.write(bytes);
                    if (last) {
                        if (ahcListener instanceof WebSocketByteListener) {
                            final byte[] bytesLocal = byteArrayOutputStream.toByteArray();
                            byteArrayOutputStream.reset();
                            WebSocketByteListener.class.cast(ahcListener).onMessage(bytesLocal);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            ahcListener.onError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AHCWebSocketListenerAdapter that = (AHCWebSocketListenerAdapter) o;
        if (ahcListener != null ? !ahcListener.equals(that.ahcListener) : that.ahcListener != null) {
            return false;
        }
        if (webSocket != null ? !webSocket.equals(that.webSocket) : that.webSocket != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = ahcListener != null ? ahcListener.hashCode() : 0;
        result = 31 * result + (webSocket != null ? webSocket.hashCode() : 0);
        return result;
    }
    
} // END AHCWebSocketListenerAdapter
