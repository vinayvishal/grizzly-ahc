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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AsyncStreamHandlerTest extends AbstractBasicTest {
    private final static String RESPONSE = "param_1_";
    
    private String jetty8ContentTypeMadness(String saneValue) {
        return saneValue.replace(" ", "");
    }

    @Test(groups = { "standalone", "default_provider" })
    public void asyncStreamGETTest() throws Exception {
        final CountDownLatch l = new CountDownLatch(1);
        final AtomicReference<FluentCaseInsensitiveStringsMap> responseHeaders = new AtomicReference<>();
        final AtomicReference<Throwable> throwable = new AtomicReference<>();
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            client.prepareGet(getTargetUrl()).execute(new AsyncHandlerAdapter() {

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    try {
                        responseHeaders.set(content.getHeaders());
                        return STATE.ABORT;
                    } finally {
                        l.countDown();
                    }
                }

                @Override
                public void onThrowable(Throwable t) {
                    try {
                        throwable.set(t);
                    } finally {
                        l.countDown();
                    }
                }
            });

            if (!l.await(5, TimeUnit.SECONDS)) {
                fail("Timeout out");
            }
            
            FluentCaseInsensitiveStringsMap h = responseHeaders.get();
            assertNotNull(h, "No response headers");
            assertEquals(h.getJoinedValue("content-type", ","), jetty8ContentTypeMadness(TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET), "Unexpected content-type");
            assertNull(throwable.get(), "Unexpected exception");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void asyncStreamPOSTTest() throws Exception {

        final AtomicReference<FluentCaseInsensitiveStringsMap> responseHeaders = new AtomicReference<>();

        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Future<String> f = client.preparePost(getTargetUrl())//
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")//
                    .addFormParam("param_1", "value_1")//
                    .execute(new AsyncHandlerAdapter() {
                private StringBuilder builder = new StringBuilder();

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    responseHeaders.set(content.getHeaders());
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                    builder.append(new String(content.getBodyPartBytes()));
                    return STATE.CONTINUE;
                }

                @Override
                public String onCompleted() throws Exception {
                    return builder.toString().trim();
                }
            });

            String responseBody = f.get(10, TimeUnit.SECONDS);
            FluentCaseInsensitiveStringsMap h = responseHeaders.get();
            assertNotNull(h);
            assertEquals(h.getJoinedValue("content-type", ","), jetty8ContentTypeMadness(TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET));
            assertEquals(responseBody, RESPONSE);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void asyncStreamInterruptTest() throws Exception {
        final CountDownLatch l = new CountDownLatch(1);

        final AtomicReference<FluentCaseInsensitiveStringsMap> responseHeaders = new AtomicReference<>();
        final AtomicBoolean bodyReceived = new AtomicBoolean(false);
        final AtomicReference<Throwable> throwable = new AtomicReference<>();
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            client.preparePost(getTargetUrl())//
            .setHeader("Content-Type", "application/x-www-form-urlencoded")//
            .addFormParam("param_1", "value_1")//
            .execute(new AsyncHandlerAdapter() {

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    responseHeaders.set(content.getHeaders());
                    return STATE.ABORT;
                }

                @Override
                public STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
                    bodyReceived.set(true);
                    return STATE.ABORT;
                }

                @Override
                public void onThrowable(Throwable t) {
                    throwable.set(t);
                    l.countDown();
                }
            });

            l.await(5, TimeUnit.SECONDS);
            assertTrue(!bodyReceived.get(), "Interrupted not working");
            FluentCaseInsensitiveStringsMap h = responseHeaders.get();
            assertNotNull(h, "Should receive non null headers");
            assertEquals(h.getJoinedValue("content-type", ", "), jetty8ContentTypeMadness(TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET), "Unexpected content-type");
            assertNull(throwable.get(), "Should get an exception");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void asyncStreamFutureTest() throws Exception {
        final AtomicReference<FluentCaseInsensitiveStringsMap> responseHeaders = new AtomicReference<>();
        final AtomicReference<Throwable> throwable = new AtomicReference<>();
        try  (AsyncHttpClient client = getAsyncHttpClient(null)){
            Future<String> f = client.preparePost(getTargetUrl()).addFormParam("param_1", "value_1").execute(new AsyncHandlerAdapter() {
                private StringBuilder builder = new StringBuilder();

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    responseHeaders.set(content.getHeaders());
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                    builder.append(new String(content.getBodyPartBytes()));
                    return STATE.CONTINUE;
                }

                @Override
                public String onCompleted() throws Exception {
                    return builder.toString().trim();
                }

                @Override
                public void onThrowable(Throwable t) {
                    throwable.set(t);
                }
            });

            String responseBody = f.get(5, TimeUnit.SECONDS);
            FluentCaseInsensitiveStringsMap h = responseHeaders.get();
            assertNotNull(h, "Should receive non null headers");
            assertEquals(h.getJoinedValue("content-type", ", "), jetty8ContentTypeMadness(TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET), "Unexpected content-type");
            assertNotNull(responseBody, "No response body");
            assertEquals(responseBody.trim(), RESPONSE, "Unexpected response body");
            assertNull(throwable.get(), "Unexpected exception");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void asyncStreamThrowableRefusedTest() throws Exception {

        final CountDownLatch l = new CountDownLatch(1);
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            client.prepareGet(getTargetUrl()).execute(new AsyncHandlerAdapter() {

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    throw new RuntimeException("FOO");
                }

                @Override
                public void onThrowable(Throwable t) {
                    try {
                        if (t.getMessage() != null) {
                            assertEquals(t.getMessage(), "FOO");
                        }
                    } finally {
                        l.countDown();
                    }
                }
            });

            if (!l.await(10, TimeUnit.SECONDS)) {
                fail("Timed out");
            }
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void asyncStreamReusePOSTTest() throws Exception {

        final AtomicReference<FluentCaseInsensitiveStringsMap> responseHeaders = new AtomicReference<>();
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            BoundRequestBuilder rb = client.preparePost(getTargetUrl())//
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addFormParam("param_1", "value_1");
            
            Future<String> f = rb.execute(new AsyncHandlerAdapter() {
                private StringBuilder builder = new StringBuilder();

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    responseHeaders.set(content.getHeaders());
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                    builder.append(new String(content.getBodyPartBytes()));
                    return STATE.CONTINUE;
                }

                @Override
                public String onCompleted() throws Exception {
                    return builder.toString();
                }
            });

            String r = f.get(5, TimeUnit.SECONDS);
            FluentCaseInsensitiveStringsMap h = responseHeaders.get();
            assertNotNull(h, "Should receive non null headers");
            assertEquals(h.getJoinedValue("content-type", ", "), jetty8ContentTypeMadness(TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET), "Unexpected content-type");
            assertNotNull(r, "No response body");
            assertEquals(r.trim(), RESPONSE, "Unexpected response body");
            
            responseHeaders.set(null);

            // Let do the same again
            f = rb.execute(new AsyncHandlerAdapter() {
                private StringBuilder builder = new StringBuilder();

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    responseHeaders.set(content.getHeaders());
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                    builder.append(new String(content.getBodyPartBytes()));
                    return STATE.CONTINUE;
                }

                @Override
                public String onCompleted() throws Exception {
                    return builder.toString();
                }
            });

            f.get(5, TimeUnit.SECONDS);
            h = responseHeaders.get();
            assertNotNull(h, "Should receive non null headers");
            assertEquals(h.getJoinedValue("content-type", ", "), jetty8ContentTypeMadness(TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET), "Unexpected content-type");
            assertNotNull(r, "No response body");
            assertEquals(r.trim(), RESPONSE, "Unexpected response body");
        }
    }

    @Test(groups = { "online", "default_provider" })
    public void asyncStream302RedirectWithBody() throws Exception {
        final AtomicReference<Integer> statusCode = new AtomicReference<Integer>(0);
        final AtomicReference<FluentCaseInsensitiveStringsMap> responseHeaders = new AtomicReference<>();
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setFollowRedirect(true).build())) {
            Future<String> f = client.prepareGet("http://google.com").execute(new AsyncHandlerAdapter() {

                public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
                    statusCode.set(status.getStatusCode());
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    responseHeaders.set(content.getHeaders());
                    return STATE.CONTINUE;
                }

                @Override
                public String onCompleted() throws Exception {
                    return null;
                }
            });

            f.get(20, TimeUnit.SECONDS);
            assertTrue(statusCode.get() != 302);
            FluentCaseInsensitiveStringsMap h = responseHeaders.get();
            assertNotNull(h);
            assertEquals(h.getFirstValue("server"), "gws");
            // This assertion below is not an invariant, since implicitly contains locale-dependant settings
            // and fails when run in country having own localized Google site and it's locale relies on something
            // other than ISO-8859-1.
            // In Hungary for example, http://google.com/ redirects to http://www.google.hu/, a localized
            // Google site, that uses ISO-8892-2 encoding (default for HU). Similar is true for other
            // non-ISO-8859-1 using countries that have "localized" google, like google.hr, google.rs, google.cz, google.sk etc.
            //
            // assertEquals(h.getJoinedValue("content-type", ", "), "text/html; charset=ISO-8859-1");
        }
    }

    @Test(groups = { "standalone", "default_provider" }, timeOut = 3000, description = "Test behavior of 'read only status line' scenario.")
    public void asyncStreamJustStatusLine() throws Exception {
        final int STATUS = 0;
        final int COMPLETED = 1;
        final int OTHER = 2;
        final boolean[] whatCalled = new boolean[] { false, false, false };
        final CountDownLatch latch = new CountDownLatch(1);
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Future<Integer> statusCode = client.prepareGet(getTargetUrl()).execute(new AsyncHandler<Integer>() {
                private int status = -1;

                public void onThrowable(Throwable t) {
                    whatCalled[OTHER] = true;
                    latch.countDown();
                }

                public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                    whatCalled[OTHER] = true;
                    latch.countDown();
                    return STATE.ABORT;
                }

                public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
                    whatCalled[STATUS] = true;
                    status = responseStatus.getStatusCode();
                    latch.countDown();
                    return STATE.ABORT;
                }

                public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                    whatCalled[OTHER] = true;
                    latch.countDown();
                    return STATE.ABORT;
                }

                public Integer onCompleted() throws Exception {
                    whatCalled[COMPLETED] = true;
                    latch.countDown();
                    return status;
                }
            });

            if (!latch.await(2, TimeUnit.SECONDS)) {
                fail("Timeout");
                return;
            }
            Integer status = statusCode.get(TIMEOUT, TimeUnit.SECONDS);
            assertEquals((int) status, 200, "Expected status code failed.");

            if (!whatCalled[STATUS]) {
                fail("onStatusReceived not called.");
            }
            if (!whatCalled[COMPLETED]) {
                fail("onCompleted not called.");
            }
            if (whatCalled[OTHER]) {
                fail("Other method of AsyncHandler got called.");
            }
        }
    }

    @Test(groups = { "online", "default_provider" })
    public void asyncOptionsTest() throws Exception {
        final AtomicReference<FluentCaseInsensitiveStringsMap> responseHeaders = new AtomicReference<>();

        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            final String[] expected = { "GET", "HEAD", "OPTIONS", "POST" };
            Future<String> f = client.prepareOptions("http://www.apache.org/").execute(new AsyncHandlerAdapter() {

                @Override
                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    responseHeaders.set(content.getHeaders());
                    return STATE.ABORT;
                }

                @Override
                public String onCompleted() throws Exception {
                    return "OK";
                }
            });

            f.get(20, TimeUnit.SECONDS) ;
            FluentCaseInsensitiveStringsMap h = responseHeaders.get();
            assertNotNull(h);
            String[] values = h.get("Allow").get(0).split(",|, ");
            assertNotNull(values);
            assertEquals(values.length, expected.length);
            Arrays.sort(values);
            assertEquals(values, expected);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void closeConnectionTest() throws Exception {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Response r = client.prepareGet(getTargetUrl()).execute(new AsyncHandler<Response>() {

                private Response.ResponseBuilder builder = new Response.ResponseBuilder();

                public STATE onHeadersReceived(HttpResponseHeaders content) throws Exception {
                    builder.accumulate(content);
                    return STATE.CONTINUE;
                }

                public void onThrowable(Throwable t) {
                }

                public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                    builder.accumulate(content);

                    if (content.isLast()) {
                        content.markUnderlyingConnectionAsToBeClosed();
                    }
                    return STATE.CONTINUE;
                }

                public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
                    builder.accumulate(responseStatus);

                    return STATE.CONTINUE;
                }

                public Response onCompleted() throws Exception {
                    return builder.build();
                }
            }).get();

            assertNotNull(r);
            assertEquals(r.getStatusCode(), 200);
        }
    }
}
