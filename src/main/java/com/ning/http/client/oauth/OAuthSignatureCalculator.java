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

package com.ning.http.client.oauth;

import static com.ning.http.util.MiscUtils.isNonEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.ning.http.client.Param;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilderBase;
import com.ning.http.client.SignatureCalculator;
import com.ning.http.client.uri.Uri;
import com.ning.http.util.Base64;
import com.ning.http.util.StringUtils;
import com.ning.http.util.UTF8UrlEncoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simple OAuth signature calculator that can used for constructing client signatures
 * for accessing services that use OAuth for authorization.
 * <p/>
 * Supports most common signature inclusion and calculation methods: HMAC-SHA1 for
 * calculation, and Header inclusion as inclusion method. Nonce generation uses
 * simple random numbers with base64 encoding.
 *
 * @author tatu (tatu.saloranta@iki.fi)
 */
public class OAuthSignatureCalculator implements SignatureCalculator {
    public final static String HEADER_AUTHORIZATION = "Authorization";

    private static final String KEY_OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    private static final String KEY_OAUTH_NONCE = "oauth_nonce";
    private static final String KEY_OAUTH_SIGNATURE = "oauth_signature";
    private static final String KEY_OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
    private static final String KEY_OAUTH_TIMESTAMP = "oauth_timestamp";
    private static final String KEY_OAUTH_TOKEN = "oauth_token";
    private static final String KEY_OAUTH_VERSION = "oauth_version";

    private static final String OAUTH_VERSION_1_0 = "1.0";
    private static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";

    protected static final ThreadLocal<byte[]> NONCE_BUFFER = new ThreadLocal<byte[]>() {
        protected byte[] initialValue() {
            return new byte[16];
        }
    };

    protected final ThreadSafeHMAC mac;

    protected final ConsumerKey consumerAuth;

    protected final RequestToken userAuth;

    /**
     * @param consumerAuth Consumer key to use for signature calculation
     * @param userAuth     Request/access token to use for signature calculation
     */
    public OAuthSignatureCalculator(ConsumerKey consumerAuth, RequestToken userAuth) {
        mac = new ThreadSafeHMAC(consumerAuth, userAuth);
        this.consumerAuth = consumerAuth;
        this.userAuth = userAuth;
    }

    @Override
    public void calculateAndAddSignature(Request request, RequestBuilderBase<?> requestBuilder) {
        String nonce = generateNonce();
        long timestamp = generateTimestamp();
        String signature = calculateSignature(request.getMethod(), request.getUri(), timestamp, nonce, request.getFormParams(), request.getQueryParams());
        String headerValue = constructAuthHeader(signature, nonce, timestamp);
        requestBuilder.setHeader(HEADER_AUTHORIZATION, headerValue);
    }

    private String baseUrl(Uri uri) {
        /* 07-Oct-2010, tatu: URL may contain default port number; if so, need to extract
         *   from base URL.
         */
        String scheme = uri.getScheme();

        StringBuilder sb = StringUtils.stringBuilder();
        sb.append(scheme).append("://").append(uri.getHost());
        
        int port = uri.getPort();
        if (scheme.equals("http")) {
            if (port == 80)
                port = -1;
        } else if (scheme.equals("https")) {
            if (port == 443)
                port = -1;
        }

        if (port != -1)
            sb.append(':').append(port);

        if (isNonEmpty(uri.getPath()))
            sb.append(uri.getPath());
        
        return sb.toString();
    }

    private String encodedParams(long oauthTimestamp, String nonce, List<Param> formParams, List<Param> queryParams) {
        /**
         * List of all query and form parameters added to this request; needed
         * for calculating request signature
         */
        int allParametersSize = 5
                + (userAuth.getKey() != null ? 1 : 0)
                + (formParams != null ? formParams.size() : 0)
                + (queryParams != null ? queryParams.size() : 0);
        OAuthParameterSet allParameters = new OAuthParameterSet(allParametersSize);

        // start with standard OAuth parameters we need
        allParameters.add(KEY_OAUTH_CONSUMER_KEY, UTF8UrlEncoder.encodeQueryElement(consumerAuth.getKey()));
        allParameters.add(KEY_OAUTH_NONCE, UTF8UrlEncoder.encodeQueryElement(nonce));
        allParameters.add(KEY_OAUTH_SIGNATURE_METHOD, OAUTH_SIGNATURE_METHOD);
        allParameters.add(KEY_OAUTH_TIMESTAMP, String.valueOf(oauthTimestamp));
        if (userAuth.getKey() != null) {
            allParameters.add(KEY_OAUTH_TOKEN, UTF8UrlEncoder.encodeQueryElement(userAuth.getKey()));
        }
        allParameters.add(KEY_OAUTH_VERSION, OAUTH_VERSION_1_0);

        if (formParams != null) {
            for (Param param : formParams) {
                // formParams are not already encoded
                allParameters.add(UTF8UrlEncoder.encodeQueryElement(param.getName()), UTF8UrlEncoder.encodeQueryElement(param.getValue()));
            }
        }
        if (queryParams != null) {
            for (Param param : queryParams) {
             // queryParams are already encoded
                allParameters.add(param.getName(), param.getValue());
            }
        }
        return allParameters.sortAndConcat();
    }

    StringBuilder signatureBaseString(String method, Uri uri, long oauthTimestamp, String nonce,
                                     List<Param> formParams, List<Param> queryParams) {
        
        // beware: must generate first as we're using pooled StringBuilder
        String baseUrl = baseUrl(uri);
        String encodedParams = encodedParams(oauthTimestamp, nonce, formParams, queryParams);

        StringBuilder sb = StringUtils.stringBuilder();
        sb.append(method); // POST / GET etc (nothing to URL encode)
        sb.append('&');
        UTF8UrlEncoder.encodeAndAppendQueryElement(sb, baseUrl);


        // and all that needs to be URL encoded (... again!)
        sb.append('&');
        UTF8UrlEncoder.encodeAndAppendQueryElement(sb, encodedParams);
        return sb;
    }
    
    /**
     * Method for calculating OAuth signature using HMAC/SHA-1 method.
     */
    public String calculateSignature(String method, Uri uri, long oauthTimestamp, String nonce,
                                     List<Param> formParams, List<Param> queryParams) {

        StringBuilder sb = signatureBaseString(method, uri, oauthTimestamp, nonce, formParams, queryParams);

        ByteBuffer rawBase = StringUtils.charSequence2ByteBuffer(sb, UTF_8);
        byte[] rawSignature = mac.digest(rawBase);
        // and finally, base64 encoded... phew!
        return Base64.encode(rawSignature);
    }

    /**
     * Method used for constructing
     */
    private String constructAuthHeader(String signature, String nonce, long oauthTimestamp) {
        StringBuilder sb = StringUtils.stringBuilder();
        sb.append("OAuth ");
        sb.append(KEY_OAUTH_CONSUMER_KEY).append("=\"").append(consumerAuth.getKey()).append("\", ");
        if (userAuth.getKey() != null) {
            sb.append(KEY_OAUTH_TOKEN).append("=\"").append(userAuth.getKey()).append("\", ");
        }
        sb.append(KEY_OAUTH_SIGNATURE_METHOD).append("=\"").append(OAUTH_SIGNATURE_METHOD).append("\", ");

        // careful: base64 has chars that need URL encoding:
        sb.append(KEY_OAUTH_SIGNATURE).append("=\"");
        UTF8UrlEncoder.encodeAndAppendQueryElement(sb, signature).append("\", ");
        sb.append(KEY_OAUTH_TIMESTAMP).append("=\"").append(oauthTimestamp).append("\", ");

        // also: nonce may contain things that need URL encoding (esp. when using base64):
        sb.append(KEY_OAUTH_NONCE).append("=\"");
        UTF8UrlEncoder.encodeAndAppendQueryElement(sb, nonce);
        sb.append("\", ");

        sb.append(KEY_OAUTH_VERSION).append("=\"").append(OAUTH_VERSION_1_0).append("\"");
        return sb.toString();
    }

    protected long generateTimestamp() {
        return System.currentTimeMillis() / 1000L;
    }

    protected String generateNonce() {
        byte[] nonceBuffer = NONCE_BUFFER.get();
        ThreadLocalRandom.current().nextBytes(nonceBuffer);
        // let's use base64 encoding over hex, slightly more compact than hex or decimals
        return Base64.encode(nonceBuffer);
//      return String.valueOf(Math.abs(random.nextLong()));
    }

    /**
     * Container for parameters used for calculating OAuth signature.
     * About the only confusing aspect is that of whether entries are to be sorted
     * before encoded or vice versa: if my reading is correct, encoding is to occur
     * first, then sorting; although this should rarely matter (since sorting is primary
     * by key, which usually has nothing to encode)... of course, rarely means that
     * when it would occur it'd be harder to track down.
     */
    final static class OAuthParameterSet {
        private final ArrayList<Parameter> allParameters;

        public OAuthParameterSet(int size) {
            allParameters = new ArrayList<>(size);
        }

        public OAuthParameterSet add(String key, String value) {
            allParameters.add(new Parameter(key, value));
            return this;
        }

        public String sortAndConcat() {
            // then sort them (AFTER encoding, important)
            Parameter[] params = allParameters.toArray(new Parameter[allParameters.size()]);
            Arrays.sort(params);

            // and build parameter section using pre-encoded pieces:
            StringBuilder encodedParams = new StringBuilder(100);
            for (Parameter param : params) {
                if (encodedParams.length() > 0) {
                    encodedParams.append('&');
                }
                encodedParams.append(param.key()).append('=').append(param.value());
            }
            return encodedParams.toString();
        }
    }

    /**
     * Helper class for sorting query and form parameters that we need
     */
    final static class Parameter implements Comparable<Parameter> {
        private final String key, value;

        public Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String key() {
            return key;
        }

        public String value() {
            return value;
        }

        @Override
        public int compareTo(Parameter other) {
            int diff = key.compareTo(other.key);
            if (diff == 0) {
                diff = value.compareTo(other.value);
            }
            return diff;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Parameter parameter = (Parameter) o;

            if (!key.equals(parameter.key)) return false;
            if (!value.equals(parameter.value)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }
    }
}
