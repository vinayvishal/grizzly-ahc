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

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.BodyDeferringAsyncHandler;
import com.ning.http.client.BodyDeferringAsyncHandler.BodyDeferringInputStream;
import com.ning.http.client.Response;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public abstract class BodyDeferringAsyncHandlerTest extends AbstractBasicTest {

    // not a half gig ;) for test shorter run's sake
    protected static final int HALF_GIG = 100000;

    public static class SlowAndBigHandler extends AbstractHandler {

        public void handle(String pathInContext, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

            // 512MB large download
            // 512 * 1024 * 1024 = 536870912
            httpResponse.setStatus(200);
            httpResponse.setContentLength(HALF_GIG);
            httpResponse.setContentType("application/octet-stream");

            httpResponse.flushBuffer();

            final boolean wantFailure = httpRequest.getHeader("X-FAIL-TRANSFER") != null;
            final boolean wantSlow = httpRequest.getHeader("X-SLOW") != null;

            OutputStream os = httpResponse.getOutputStream();
            for (int i = 0; i < HALF_GIG; i++) {
                os.write(i % 255);

                if (wantSlow) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ex) {
                        // nuku
                    }
                }

                if (wantFailure) {
                    if (i > HALF_GIG / 2) {
                        // kaboom
                        // yes, response is commited, but Jetty does aborts and
                        // drops connection
                        httpResponse.sendError(500);
                        break;
                    }
                }
            }

            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
        }
    }

    // a /dev/null but counting how many bytes it ditched
    public static class CountingOutputStream extends OutputStream {
        private int byteCount = 0;

        @Override
        public void write(int b) throws IOException {
            // /dev/null
            byteCount++;
        }

        public int getByteCount() {
            return byteCount;
        }
    }

    // simple stream copy just to "consume". It closes streams.
    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();
        out.close();
        in.close();
    }

    public AbstractHandler configureHandler() throws Exception {
        return new SlowAndBigHandler();
    }

    public AsyncHttpClientConfig getAsyncHttpClientConfig() {
        // for this test brevity's sake, we are limiting to 1 retries
        return new AsyncHttpClientConfig.Builder().setMaxRequestRetry(0).setRequestTimeout(10000).build();
    }

    @Test(groups = { "standalone", "default_provider" })
    public void deferredSimple() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        try (AsyncHttpClient client = getAsyncHttpClient(getAsyncHttpClientConfig())) {
            AsyncHttpClient.BoundRequestBuilder r = client.prepareGet("http://127.0.0.1:" + port1 + "/deferredSimple");

            CountingOutputStream cos = new CountingOutputStream();
            BodyDeferringAsyncHandler bdah = new BodyDeferringAsyncHandler(cos);
            Future<Response> f = r.execute(bdah);
            Response resp = bdah.getResponse();
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(true, resp.getHeader("content-length").equals(String.valueOf(HALF_GIG)));
            // we got headers only, it's probably not all yet here (we have BIG file
            // downloading)
            assertEquals(true, HALF_GIG >= cos.getByteCount());

            // now be polite and wait for body arrival too (otherwise we would be
            // dropping the "line" on server)
            f.get();
            // it all should be here now
            assertEquals(true, HALF_GIG == cos.getByteCount());
        }
    }

    @Test(groups = { "standalone", "default_provider" }, enabled = false)
    public void deferredSimpleWithFailure() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        try (AsyncHttpClient client = getAsyncHttpClient(getAsyncHttpClientConfig())) {
            AsyncHttpClient.BoundRequestBuilder r = client.prepareGet("http://127.0.0.1:" + port1 + "/deferredSimpleWithFailure").addHeader("X-FAIL-TRANSFER", Boolean.TRUE.toString());

            CountingOutputStream cos = new CountingOutputStream();
            BodyDeferringAsyncHandler bdah = new BodyDeferringAsyncHandler(cos);
            Future<Response> f = r.execute(bdah);
            Response resp = bdah.getResponse();
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(true, resp.getHeader("content-length").equals(String.valueOf(HALF_GIG)));
            // we got headers only, it's probably not all yet here (we have BIG file
            // downloading)
            assertEquals(true, HALF_GIG >= cos.getByteCount());

            // now be polite and wait for body arrival too (otherwise we would be
            // dropping the "line" on server)
            try {
                f.get();
                Assert.fail("get() should fail with IOException!");
            } catch (Exception e) {
                // good
            }
            // it's incomplete, there was an error
            assertEquals(false, HALF_GIG == cos.getByteCount());
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void deferredInputStreamTrick() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        try (AsyncHttpClient client = getAsyncHttpClient(getAsyncHttpClientConfig())) {
            AsyncHttpClient.BoundRequestBuilder r = client.prepareGet("http://127.0.0.1:" + port1 + "/deferredInputStreamTrick");

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            BodyDeferringAsyncHandler bdah = new BodyDeferringAsyncHandler(pos);

            Future<Response> f = r.execute(bdah);

            BodyDeferringInputStream is = new BodyDeferringInputStream(f, bdah, pis);

            Response resp = is.getAsapResponse();
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(true, resp.getHeader("content-length").equals(String.valueOf(HALF_GIG)));
            // "consume" the body, but our code needs input stream
            CountingOutputStream cos = new CountingOutputStream();
            copy(is, cos);

            // now we don't need to be polite, since consuming and closing
            // BodyDeferringInputStream does all.
            // it all should be here now
            assertEquals(true, HALF_GIG == cos.getByteCount());
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void deferredInputStreamTrickWithFailure() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        try (AsyncHttpClient client = getAsyncHttpClient(getAsyncHttpClientConfig())) {
            AsyncHttpClient.BoundRequestBuilder r = client.prepareGet("http://127.0.0.1:" + port1 + "/deferredInputStreamTrickWithFailure").addHeader("X-FAIL-TRANSFER", Boolean.TRUE.toString());

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            BodyDeferringAsyncHandler bdah = new BodyDeferringAsyncHandler(pos);

            Future<Response> f = r.execute(bdah);

            BodyDeferringInputStream is = new BodyDeferringInputStream(f, bdah, pis);

            Response resp = is.getAsapResponse();
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(true, resp.getHeader("content-length").equals(String.valueOf(HALF_GIG)));
            // "consume" the body, but our code needs input stream
            CountingOutputStream cos = new CountingOutputStream();
            try {
                copy(is, cos);
                Assert.fail("InputStream consumption should fail with IOException!");
            } catch (IOException e) {
                // good!
            }
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testConnectionRefused() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        try (AsyncHttpClient client = getAsyncHttpClient(getAsyncHttpClientConfig())) {
            int newPortWithoutAnyoneListening = findFreePort();
            AsyncHttpClient.BoundRequestBuilder r = client.prepareGet("http://127.0.0.1:" + newPortWithoutAnyoneListening + "/testConnectionRefused");

            CountingOutputStream cos = new CountingOutputStream();
            BodyDeferringAsyncHandler bdah = new BodyDeferringAsyncHandler(cos);
            r.execute(bdah);
            try {
                bdah.getResponse();
                Assert.fail("IOException should be thrown here!");
            } catch (IOException e) {
                // good
            }
        }
    }
}
