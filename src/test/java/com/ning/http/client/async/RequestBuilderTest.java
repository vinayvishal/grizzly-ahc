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

package com.ning.http.client.async;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.ning.http.client.Param;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RequestBuilderTest {

    private final static String SAFE_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890-_~.";
    private final static String HEX_CHARS = "0123456789ABCDEF";

    @Test(groups = {"standalone", "default_provider"})
    public void testEncodesQueryParameters() throws UnsupportedEncodingException {
        String[] values = new String[]{
                "abcdefghijklmnopqrstuvwxyz",
                "ABCDEFGHIJKQLMNOPQRSTUVWXYZ",
                "1234567890", "1234567890",
                "`~!@#$%^&*()", "`~!@#$%^&*()",
                "_+-=,.<>/?", "_+-=,.<>/?",
                ";:'\"[]{}\\| ", ";:'\"[]{}\\| "
        };

        /* as per RFC-5849 (Oauth), and RFC-3986 (percent encoding) we MUST
         * encode everything except for "safe" characters; and nothing but them.
         * Safe includes ascii letters (upper and lower case), digits (0 - 9)
         * and FOUR special characters: hyphen ('-'), underscore ('_'),
         * tilde ('~') and period ('.')). Everything else must be percent-encoded,
         * byte-by-byte, using UTF-8 encoding (meaning three-byte Unicode/UTF-8
         * code points are encoded as three three-letter percent-encode entities).
         */
        for (String value : values) {
            RequestBuilder builder = new RequestBuilder("GET").
                    setUrl("http://example.com/").
                    addQueryParam("name", value);

            StringBuilder sb = new StringBuilder();
            for (int i = 0, len = value.length(); i < len; ++i) {
                char c = value.charAt(i);
                if (SAFE_CHARS.indexOf(c) >= 0) {
                    sb.append(c);
                } else {
                    int hi = (c >> 4);
                    int lo = c & 0xF;
                    sb.append('%').append(HEX_CHARS.charAt(hi)).append(HEX_CHARS.charAt(lo));
                }
            }
            String expValue = sb.toString();
            Request request = builder.build();
            assertEquals(request.getUrl(), "http://example.com/?name=" + expValue);
        }
    }

    @Test(groups = {"standalone", "default_provider"})
    public void testChaining() throws IOException, ExecutionException, InterruptedException {
        Request request = new RequestBuilder("GET")
                .setUrl("http://foo.com")
                .addQueryParam("x", "value")
                .build();

        Request request2 = new RequestBuilder(request).build();

        assertEquals(request2.getUri(), request.getUri());
    }

    @Test(groups = {"standalone", "default_provider"})
    public void testParsesQueryParams() throws IOException, ExecutionException, InterruptedException {
        Request request = new RequestBuilder("GET")
                .setUrl("http://foo.com/?param1=value1")
                .addQueryParam("param2", "value2")
                .build();

        assertEquals(request.getUrl(), "http://foo.com/?param1=value1&param2=value2");
        List<Param> params = request.getQueryParams();
        assertEquals(params.size(), 2);
        assertEquals(params.get(0), new Param("param1", "value1"));
        assertEquals(params.get(1), new Param("param2", "value2"));
    }

    @Test(groups = {"standalone", "default_provider"})
    public void testUserProvidedRequestMethod() {
        Request req = new RequestBuilder("ABC").setUrl("http://foo.com").build();
        assertEquals(req.getMethod(), "ABC");
        assertEquals(req.getUrl(), "http://foo.com");
    }

    @Test(groups = {"standalone", "default_provider"})
    public void testPercentageEncodedUserInfo() {
        final Request req = new RequestBuilder("GET").setUrl("http://hello:wor%20ld@foo.com").build();
        assertEquals(req.getMethod(), "GET");
        assertEquals(req.getUrl(), "http://hello:wor%20ld@foo.com");
    }

    @Test(groups = {"standalone", "default_provider"})
    public void testContentTypeCharsetToBodyEncoding() {
        final Request req = new RequestBuilder("GET").setHeader("Content-Type", "application/json; charset=XXXX").build();
        assertEquals(req.getBodyEncoding(), "XXXX");
        final Request req2 = new RequestBuilder("GET").setHeader("Content-Type", "application/json; charset=\"XXXX\"").build();
        assertEquals(req2.getBodyEncoding(), "XXXX");
    }

    @Test(groups = {"standalone", "default_provider"})
    public void testAddQueryParameter() throws UnsupportedEncodingException {
        RequestBuilder rb = new RequestBuilder("GET", false).setUrl("http://example.com/path")
                .addQueryParam("a", "1?&")
                .addQueryParam("b", "+ =");
        Request request = rb.build();
        assertEquals(request.getUrl(), "http://example.com/path?a=1%3F%26&b=%2B%20%3D");
    }

    @Test(groups = {"standalone", "default_provider"})
    public void testRawUrlQuery() throws UnsupportedEncodingException, URISyntaxException {
        String preEncodedUrl = "http://example.com/space%20mirror.php?%3Bteile";
        RequestBuilder rb = new RequestBuilder("GET", true).setUrl(preEncodedUrl);
        Request request = rb.build();
        assertEquals(request.getUrl(), preEncodedUrl);
        assertEquals(request.getUri().toJavaNetURI().toString(), preEncodedUrl);
    }
}
