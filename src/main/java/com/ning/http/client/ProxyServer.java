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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.ning.http.client.Realm.AuthScheme;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a proxy server.
 */
public class ProxyServer {

    public enum Protocol {
        HTTP("http"), HTTPS("https"), NTLM("NTLM"), KERBEROS("KERBEROS"), SPNEGO("SPNEGO");

        private final String protocol;

        private Protocol(final String protocol) {
            this.protocol = protocol;
        }

        public String getProtocol() {
            return protocol;
        }

        @Override
        public String toString() {
            return getProtocol();
        }
    }

    private final List<String> nonProxyHosts = new ArrayList<>();
    private final Protocol protocol;
    private final String host;
    private final String principal;
    private final String password;
    private final int port;
    private final String url;
    private Charset charset = UTF_8;
    private String ntlmDomain = System.getProperty("http.auth.ntlm.domain", "");
    private String ntlmHost;
    private AuthScheme scheme = AuthScheme.BASIC;
    private boolean forceHttp10 = false;

    public ProxyServer(final Protocol protocol, final String host, final int port, String principal, String password) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.principal = principal;
        this.password = password;
        this.url = protocol + "://" + host + ":" + port;
    }

    public ProxyServer(final String host, final int port, String principal, String password) {
        this(Protocol.HTTP, host, port, principal, password);
    }

    public ProxyServer(final Protocol protocol, final String host, final int port) {
        this(protocol, host, port, null, null);
    }

    public ProxyServer(final String host, final int port) {
        this(Protocol.HTTP, host, port, null, null);
    }

    public Realm.RealmBuilder realmBuilder() {
        return new Realm.RealmBuilder()//
        .setTargetProxy(true)
        .setNtlmDomain(ntlmDomain)
        .setNtlmHost(ntlmHost)
        .setPrincipal(principal)
        .setPassword(password)
        .setScheme(scheme);
    }
    
    public Protocol getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getPassword() {
        return password;
    }

    public ProxyServer setCharset(Charset charset) {
        this.charset = charset;
        return this;
    }

    public Charset getCharset() {
        return charset;
    }

    public ProxyServer addNonProxyHost(String uri) {
        nonProxyHosts.add(uri);
        return this;
    }

    public ProxyServer removeNonProxyHost(String uri) {
        nonProxyHosts.remove(uri);
        return this;
    }

    public List<String> getNonProxyHosts() {
        return Collections.unmodifiableList(nonProxyHosts);
    }

    public ProxyServer setNtlmDomain(String ntlmDomain) {
        this.ntlmDomain = ntlmDomain;
        return this;
    }

    public String getNtlmDomain() {
        return ntlmDomain;
    }
    
    public AuthScheme getScheme() {
        return scheme;
    }

    public void setScheme(AuthScheme scheme) {
        this.scheme = scheme;
    }

    public String getNtlmHost() {
        return ntlmHost;
    }

    public void setNtlmHost(String ntlmHost) {
        this.ntlmHost = ntlmHost;
    }

    public String getUrl() {
        return url;
    }
    
    public boolean isForceHttp10() {
        return forceHttp10;
    }

    public void setForceHttp10(boolean forceHttp10) {
        this.forceHttp10 = forceHttp10;
    }

    @Override
    public String toString() {
        return url;
    }
}

