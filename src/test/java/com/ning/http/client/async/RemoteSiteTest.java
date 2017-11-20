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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.cookie.Cookie;
import com.ning.http.util.AsyncHttpProviderUtils;

/**
 * Unit tests for remote site.
 * <p/>
 * see http://github.com/MSch/ning-async-http-client-bug/tree/master
 * 
 * @author Martin Schurrer
 */
public abstract class RemoteSiteTest extends AbstractBasicTest {

    public static final String URL = "http://google.com?q=";
    public static final String REQUEST_PARAM = "github github \n" + "github";

    @Test(groups = { "online", "default_provider" })
    public void testGoogleCom() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeout(10000).build())) {
            Response response = client.prepareGet("http://www.google.com/").execute().get(10, TimeUnit.SECONDS);
            assertNotNull(response);
        }
    }

    @Test(groups = { "online", "default_provider" }, enabled = false)
    public void testMicrosoftCom() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeout(10000).build())) {
            Response response = client.prepareGet("http://microsoft.com/").execute().get(10, TimeUnit.SECONDS);
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 301);
        }
    }

    @Test(groups = { "online", "default_provider" }, enabled = false)
    public void testWwwMicrosoftCom() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeout(10000).build())) {
            Response response = client.prepareGet("http://www.microsoft.com/").execute().get(10, TimeUnit.SECONDS);
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 302);
        }
    }

    @Test(groups = { "online", "default_provider" }, enabled = false)
    public void testUpdateMicrosoftCom() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeout(10000).build())) {
            Response response = client.prepareGet("http://update.microsoft.com/").execute().get(10, TimeUnit.SECONDS);
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 302);
        }
    }

    @Test(groups = { "online", "default_provider" })
    public void testGoogleComWithTimeout() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeout(10000).build())) {
            Response response = client.prepareGet("http://google.com/").execute().get(10, TimeUnit.SECONDS);
            assertNotNull(response);
            // depends on user IP/Locale
            assertTrue(response.getStatusCode() == 301 || response.getStatusCode() == 302);
        }
    }

    @Test(groups = { "online", "default_provider" })
    public void asyncStatusHEADContentLenghtTest() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setFollowRedirect(true).build())) {
            final CountDownLatch l = new CountDownLatch(1);
            Request request = new RequestBuilder("HEAD").setUrl("http://www.google.com/").build();

            client.executeRequest(request, new AsyncCompletionHandlerAdapter() {
                @Override
                public Response onCompleted(Response response) throws Exception {
                    Assert.assertEquals(response.getStatusCode(), 200);
                    l.countDown();
                    return response;
                }
            }).get();

            if (!l.await(5, TimeUnit.SECONDS)) {
                Assert.fail("Timeout out");
            }
        }
    }

    @Test(groups = { "online", "default_provider" }, enabled = false)
    public void invalidStreamTest2() throws Throwable {
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setRequestTimeout(10000).setFollowRedirect(true).setAllowPoolingConnections(false).setMaxRedirects(6).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Response response = client.prepareGet("http://bit.ly/aUjTtG").execute().get();
            if (response != null) {
                System.out.println(response);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            assertNotNull(t.getCause());
            assertEquals(t.getCause().getMessage(), "invalid version format: ICY");
        }
    }

    @Test(groups = { "online", "default_provider" })
    public void asyncFullBodyProperlyRead() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Response r = client.prepareGet("http://www.cyberpresse.ca/").execute().get();

            InputStream stream = r.getResponseBodyAsStream();
            int available = stream.available();
            int[] lengthWrapper = new int[1];
            AsyncHttpProviderUtils.readFully(stream, lengthWrapper);
            int byteToRead = lengthWrapper[0];

            Assert.assertEquals(available, byteToRead);
        }
    }

    @Test(groups = { "online", "default_provider" })
    public void testUrlRequestParametersEncoding() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            String requestUrl2 = URL + URLEncoder.encode(REQUEST_PARAM, "UTF-8");
            log.info(String.format("Executing request [%s] ...", requestUrl2));
            Response response = client.prepareGet(requestUrl2).execute().get();
            Assert.assertTrue(response.getStatusCode() == 301 || response.getStatusCode() == 302);
        }
    }

    @Test(groups = { "online", "default_provider" })
    public void stripQueryStringTest() throws Throwable {

        AsyncHttpClientConfig cg = new AsyncHttpClientConfig.Builder().setFollowRedirect(true).build();
        try (AsyncHttpClient client = getAsyncHttpClient(cg)) {
            Response response = client.prepareGet("http://www.freakonomics.com/?p=55846").execute().get();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
        }
    }

    @Test(groups = { "online", "default_provider" })
    public void evilCoookieTest() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            RequestBuilder builder2 = new RequestBuilder("GET");
            builder2.setFollowRedirects(true);
            builder2.setUrl("http://www.google.com/");
            builder2.addHeader("Content-Type", "text/plain");
            builder2.addCookie(new Cookie("evilcookie", "test", false, ".google.com", "/", 10L, false, false));
            com.ning.http.client.Request request2 = builder2.build();
            Response response = client.executeRequest(request2).get();

            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
        }
    }

    @Test(groups = { "online", "default_provider" }, enabled = false)
    public void testAHC62Com() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setFollowRedirect(true).build())) {
            Response response = client.prepareGet("http://api.crunchbase.com/v/1/financial-organization/kinsey-hills-group.js").execute(new AsyncHandler<Response>() {

                private Response.ResponseBuilder builder = new Response.ResponseBuilder();

                public void onThrowable(Throwable t) {
                    t.printStackTrace();
                }

                public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                    System.out.println(bodyPart.getBodyPartBytes().length);
                    builder.accumulate(bodyPart);

                    return STATE.CONTINUE;
                }

                public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
                    builder.accumulate(responseStatus);
                    return STATE.CONTINUE;
                }

                public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                    builder.accumulate(headers);
                    return STATE.CONTINUE;
                }

                public Response onCompleted() throws Exception {
                    return builder.build();
                }
            }).get(10, TimeUnit.SECONDS);
            assertNotNull(response);
            assertTrue(response.getResponseBody().length() >= 3870);
        }
    }
}
