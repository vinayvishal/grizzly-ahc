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

package com.ning.http.client.async;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.filter.FilterContext;
import com.ning.http.client.filter.FilterException;
import com.ning.http.client.filter.ResponseFilter;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PostRedirectGetTest extends AbstractBasicTest {

    // ------------------------------------------------------ Test Configuration

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new PostRedirectGetHandler();
    }

    // ------------------------------------------------------------ Test Methods

    // FIXME reimplement test since only some headers are propagated on redirect
    @Test(groups = { "standalone", "post_redirect_get" }, enabled = false)
    public void postRedirectGet302Test() throws Exception {
        doTestPositive(302);
    }

    // FIXME reimplement test since only some headers are propagated on redirect
    @Test(groups = { "standalone", "post_redirect_get" }, enabled = false)
    public void postRedirectGet302StrictTest() throws Exception {
        doTestNegative(302, true);
    }

    // FIXME reimplement test since only some headers are propagated on redirect
    @Test(groups = { "standalone", "post_redirect_get" }, enabled = false)
    public void postRedirectGet303Test() throws Exception {
        doTestPositive(303);
    }

    // FIXME reimplement test since only some headers are propagated on redirect
    @Test(groups = { "standalone", "post_redirect_get" }, enabled = false)
    public void postRedirectGet301Test() throws Exception {
        doTestNegative(301, false);
    }

    // FIXME reimplement test since only some headers are propagated on redirect
    @Test(groups = { "standalone", "post_redirect_get" }, enabled = false)
    public void postRedirectGet307Test() throws Exception {
        doTestNegative(307, false);
    }

    // --------------------------------------------------------- Private Methods

    private void doTestNegative(final int status, boolean strict) throws Exception {

        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setFollowRedirect(true).setStrict302Handling(strict).addResponseFilter(new ResponseFilter() {
            public FilterContext filter(FilterContext ctx) throws FilterException {
                // pass on the x-expect-get and remove the x-redirect
                // headers if found in the response
                ctx.getResponseHeaders().getHeaders().get("x-expect-post");
                ctx.getRequest().getHeaders().add("x-expect-post", "true");
                ctx.getRequest().getHeaders().remove("x-redirect");
                return ctx;
            }
        }).build();

        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Request request = new RequestBuilder("POST").setUrl(getTargetUrl()).addFormParam("q", "a b").addHeader("x-redirect", +status + "@" + "http://localhost:" + port1 + "/foo/bar/baz").addHeader("x-negative", "true").build();
            Future<Integer> responseFuture = client.executeRequest(request, new AsyncCompletionHandler<Integer>() {

                @Override
                public Integer onCompleted(Response response) throws Exception {
                    return response.getStatusCode();
                }

                @Override
                public void onThrowable(Throwable t) {
                    t.printStackTrace();
                    Assert.fail("Unexpected exception: " + t.getMessage(), t);
                }

            });
            int statusCode = responseFuture.get();
            Assert.assertEquals(statusCode, 200);
        }
    }

    private void doTestPositive(final int status) throws Exception {
        
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setFollowRedirect(true).addResponseFilter(new ResponseFilter() {
            public FilterContext filter(FilterContext ctx) throws FilterException {
                // pass on the x-expect-get and remove the x-redirect
                // headers if found in the response
                ctx.getResponseHeaders().getHeaders().get("x-expect-get");
                ctx.getRequest().getHeaders().add("x-expect-get", "true");
                ctx.getRequest().getHeaders().remove("x-redirect");
                return ctx;
            }
        }).build();
        
        try (AsyncHttpClient client = getAsyncHttpClient(config)) {
            Request request = new RequestBuilder("POST").setUrl(getTargetUrl()).addFormParam("q", "a b").addHeader("x-redirect", +status + "@" + "http://localhost:" + port1 + "/foo/bar/baz").build();
            Future<Integer> responseFuture = client.executeRequest(request, new AsyncCompletionHandler<Integer>() {

                @Override
                public Integer onCompleted(Response response) throws Exception {
                    return response.getStatusCode();
                }

                @Override
                public void onThrowable(Throwable t) {
                    t.printStackTrace();
                    Assert.fail("Unexpected exception: " + t.getMessage(), t);
                }

            });
            int statusCode = responseFuture.get();
            Assert.assertEquals(statusCode, 200);
        }
    }

    // ---------------------------------------------------------- Nested Classes

    public static class PostRedirectGetHandler extends AbstractHandler {

        final AtomicInteger counter = new AtomicInteger();

        @Override
        public void handle(String pathInContext, org.eclipse.jetty.server.Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

            final boolean expectGet = (httpRequest.getHeader("x-expect-get") != null);
            final boolean expectPost = (httpRequest.getHeader("x-expect-post") != null);
            if (expectGet) {
                final String method = request.getMethod();
                if (!"GET".equals(method)) {
                    httpResponse.sendError(500, "Incorrect method.  Expected GET, received " + method);
                    return;
                }
                httpResponse.setStatus(200);
                httpResponse.getOutputStream().write("OK".getBytes());
                httpResponse.getOutputStream().flush();
                return;
            } else if (expectPost) {
                final String method = request.getMethod();
                if (!"POST".equals(method)) {
                    httpResponse.sendError(500, "Incorrect method.  Expected POST, received " + method);
                    return;
                }
                httpResponse.setStatus(200);
                httpResponse.getOutputStream().write("OK".getBytes());
                httpResponse.getOutputStream().flush();
                return;
            }

            String header = httpRequest.getHeader("x-redirect");
            if (header != null) {
                // format for header is <status code>|<location url>
                String[] parts = header.split("@");
                int redirectCode;
                try {
                    redirectCode = Integer.parseInt(parts[0]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    httpResponse.sendError(500, "Unable to parse redirect code");
                    return;
                }
                httpResponse.setStatus(redirectCode);
                if (httpRequest.getHeader("x-negative") == null) {
                    httpResponse.addHeader("x-expect-get", "true");
                } else {
                    httpResponse.addHeader("x-expect-post", "true");
                }
                httpResponse.setContentLength(0);
                httpResponse.addHeader("Location", parts[1] + counter.getAndIncrement());
                httpResponse.getOutputStream().flush();
                return;
            }

            httpResponse.sendError(500);
            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
        }
    }
}
