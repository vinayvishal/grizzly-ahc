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

package com.ning.http.util;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static com.ning.http.util.AsyncHttpProviderUtils.getNonEmptyPath;
import static com.ning.http.util.AsyncHttpProviderUtils.getNTLM;
import static com.ning.http.util.MiscUtils.isNonEmpty;

import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.ntlm.NTLMEngine;
import com.ning.http.client.spnego.SpnegoEngine;
import com.ning.http.client.uri.Uri;
import java.io.IOException;

import java.nio.charset.Charset;
import java.util.List;

public final class AuthenticatorUtils {
    private static final String PROXY_AUTH_HEADER = "Proxy-Authorization";
    
    public static String perConnectionAuthorizationHeader(Request request,
            Uri uri, ProxyServer proxyServer, Realm realm) throws IOException {
        String authorizationHeader = null;

        if (realm != null && realm.getUsePreemptiveAuth()) {
            switch (realm.getScheme()) {
            case NTLM:
                String msg = NTLMEngine.INSTANCE.generateType1Msg();
                authorizationHeader = "NTLM " + msg;
                break;
            case KERBEROS:
            case SPNEGO:
                String host;
                if (proxyServer != null)
                    host = proxyServer.getHost();
                else if (request.getVirtualHost() != null)
                    host = request.getVirtualHost();
                else
                    host = uri.getHost();

                try {
                    authorizationHeader = "Negotiate " + SpnegoEngine.INSTANCE.generateToken(host);
                } catch (Throwable e) {
                    throw new IOException(e);
                }
                break;
            default:
                break;
            }
        }
        
        return authorizationHeader;
    }
    
    public static String perRequestAuthorizationHeader(Request request,
            Uri uri, Realm realm) {

        String authorizationHeader = null;

        if (realm != null && realm.getUsePreemptiveAuth() && !realm.isTargetProxy()) {

            switch (realm.getScheme()) {
            case BASIC:
                authorizationHeader = computeBasicAuthentication(realm);
                break;
            case DIGEST:
                if (isNonEmpty(realm.getNonce()))
                    authorizationHeader = computeDigestAuthentication(realm);
                break;
            case NTLM:
            case KERBEROS:
            case SPNEGO:
                // NTLM, KERBEROS and SPNEGO are only set on the first request, see firstRequestOnlyAuthorizationHeader
            case NONE:
                break;
            default:
                throw new IllegalStateException("Invalid Authentication " + realm);
            }
        }

        return authorizationHeader;
    }
    
    public static String perConnectionProxyAuthorizationHeader(
            Request request, ProxyServer proxyServer, boolean connect)
            throws IOException {
        
        String proxyAuthorization = null;

        if (connect) {
            List<String> auth = request.getHeaders().get(PROXY_AUTH_HEADER);
            String ntlmHeader = getNTLM(auth);
            if (ntlmHeader != null) {
                proxyAuthorization = ntlmHeader;
            } else {
                String msg = NTLMEngine.INSTANCE.generateType1Msg();
                proxyAuthorization = "NTLM " + msg;                
            }
        } else if (proxyServer != null && proxyServer.getPrincipal() != null && isNonEmpty(proxyServer.getNtlmDomain())) {
            List<String> auth = request.getHeaders().get(PROXY_AUTH_HEADER);
            if (getNTLM(auth) == null) {
                String msg = NTLMEngine.INSTANCE.generateType1Msg();
                proxyAuthorization = "NTLM " + msg;
            }
        }

        return proxyAuthorization;
    }
    
    public static String perRequestProxyAuthorizationHeader(Request request,
            Realm realm, ProxyServer proxyServer, boolean connect) {

        String proxyAuthorization = null;

        if (proxyServer != null && proxyServer.getPrincipal() != null
                && proxyServer.getScheme() == Realm.AuthScheme.BASIC) {
            proxyAuthorization = computeBasicAuthentication(proxyServer);

        } else if (realm != null && realm.getUsePreemptiveAuth() && realm.isTargetProxy()) {

            switch (realm.getScheme()) {
            case BASIC:
                proxyAuthorization = computeBasicAuthentication(realm);
                break;
            case DIGEST:
                if (isNonEmpty(realm.getNonce()))
                    proxyAuthorization = computeDigestAuthentication(realm);
                break;
            case NTLM:
            case KERBEROS:
            case SPNEGO:
                // NTLM, KERBEROS and SPNEGO are only set on the first request, see firstRequestOnlyAuthorizationHeader
            case NONE:
                break;
            default:
                throw new IllegalStateException("Invalid Authentication " + realm);
            }
        }

        return proxyAuthorization;
    }
    
    public static String computeBasicAuthentication(Realm realm) {
        return computeBasicAuthentication(realm.getPrincipal(), realm.getPassword(), realm.getCharset());
    }

    public static String computeBasicAuthentication(ProxyServer proxyServer) {
        return computeBasicAuthentication(proxyServer.getPrincipal(), proxyServer.getPassword(), proxyServer.getCharset());
    }

    private static String computeBasicAuthentication(String principal, String password, Charset charset) {
        String s = principal + ":" + password;
        return "Basic " + Base64.encode(s.getBytes(charset));
    }

    public static String computeRealmURI(Realm realm) {
        return computeRealmURI(realm.getUri(), realm.isUseAbsoluteURI(), realm.isOmitQuery());
    }
    
    public static String computeRealmURI(Uri uri, boolean useAbsoluteURI, boolean omitQuery) {
        if (useAbsoluteURI) {
            return omitQuery && MiscUtils.isNonEmpty(uri.getQuery()) ? uri.withNewQuery(null).toUrl() : uri.toUrl();
        } else {
            String path = getNonEmptyPath(uri);
            return omitQuery || !MiscUtils.isNonEmpty(uri.getQuery()) ? path : path + "?" + uri.getQuery();
        }
    }
    
    public static String computeDigestAuthentication(Realm realm) {

        StringBuilder builder = new StringBuilder().append("Digest ");
        append(builder, "username", realm.getPrincipal(), true);
        append(builder, "realm", realm.getRealmName(), true);
        append(builder, "nonce", realm.getNonce(), true);
        append(builder, "uri", computeRealmURI(realm), true);
        if (isNonEmpty(realm.getAlgorithm()))
            append(builder, "algorithm", realm.getAlgorithm(), false);

        append(builder, "response", realm.getResponse(), true);

        if (realm.getOpaque() != null)
            append(builder, "opaque", realm.getOpaque(), true);

        if (realm.getQop() != null) {
            append(builder, "qop", realm.getQop(), false);
            // nc and cnonce only sent if server sent qop
            append(builder, "nc", realm.getNc(), false);
            append(builder, "cnonce", realm.getCnonce(), true);
        }
        builder.setLength(builder.length() - 2); // remove tailing ", "

        // FIXME isn't there a more efficient way?
        return new String(StringUtils.charSequence2Bytes(builder, ISO_8859_1));
    }

    private static StringBuilder append(StringBuilder builder, String name, String value, boolean quoted) {
        builder.append(name).append('=');
        if (quoted)
            builder.append('"').append(value).append('"');
        else
            builder.append(value);

        return builder.append(", ");
    }
}
