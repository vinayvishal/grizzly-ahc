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

import java.net.InetAddress;

/**
 * This interface hosts new low level callback methods on {@link AsyncHandler}.
 * For now, those methods are in a dedicated interface in order not to break the existing API,
 * but could be merged into one of the existing ones in AHC 2.
 * 
 * More additional hooks might come, such as:
 * <ul>
 *   <li>onConnectionClosed()</li>
 *   <li>onBytesSent(long numberOfBytes)</li>
 *   <li>onBytesReceived(long numberOfBytes)</li>
 * </ul>
 */
public interface AsyncHandlerExtensions {

    /**
     * Notify the callback when trying to open a new connection.
     */
    void onOpenConnection();

    /**
     * Notify the callback when a new connection was successfully opened.
     */
    void onConnectionOpen();

    /**
     * Notify the callback when trying to fetch a connection from the pool.
     */
    void onPoolConnection();

    /**
     * Notify the callback when a new connection was successfully fetched from the pool.
     */
    void onConnectionPooled();

    /**
     * Notify the callback when a request is about to be written on the wire.
     * If the original request causes multiple requests to be sent, for example, because of authorization or retry,
     * it will be notified multiple times.
     * 
     * @param request the real request object (underlying provider model)
     */
    void onSendRequest(Object request);

    /**
     * Notify the callback every time a request is being retried.
     */
    void onRetry();

    /**
     * Notify the callback after DNS resolution has completed.
     * 
     * @param address the resolved address
     */
    void onDnsResolved(InetAddress address);

    /**
     * Notify the callback when the SSL handshake performed to establish an HTTPS connection has been completed.
     */
    void onSslHandshakeCompleted();
}
