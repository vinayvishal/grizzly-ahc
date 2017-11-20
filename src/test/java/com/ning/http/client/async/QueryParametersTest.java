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

import static com.ning.http.util.MiscUtils.isNonEmpty;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Testing query parameters support.
 * 
 * @author Hubert Iwaniuk
 */
public abstract class QueryParametersTest extends AbstractBasicTest {
    private class QueryStringHandler extends AbstractHandler {
        public void handle(String s, Request r, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                String qs = request.getQueryString();
                if (isNonEmpty(qs)) {
                    for (String qnv : qs.split("&")) {
                        String nv[] = qnv.split("=");
                        response.addHeader(nv[0], nv[1]);
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                }
            } else { // this handler is to handle POST request
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            r.setHandled(true);
        }
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new QueryStringHandler();
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testQueryParameters() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Future<Response> f = client.prepareGet("http://127.0.0.1:" + port1).addQueryParam("a", "1").addQueryParam("b", "2").execute();
            Response resp = f.get(3, TimeUnit.SECONDS);
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(resp.getHeader("a"), "1");
            assertEquals(resp.getHeader("b"), "2");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testUrlRequestParametersEncoding() throws IOException, ExecutionException, InterruptedException {
        String URL = getTargetUrl() + "?q=";
        String REQUEST_PARAM = "github github \ngithub";

        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            String requestUrl2 = URL + URLEncoder.encode(REQUEST_PARAM, "UTF-8");
            LoggerFactory.getLogger(QueryParametersTest.class).info("Executing request [{}] ...", requestUrl2);
            Response response = client.prepareGet(requestUrl2).execute().get();
            String s = URLDecoder.decode(response.getHeader("q"), "UTF-8");
            assertEquals(s, REQUEST_PARAM);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void urlWithColonTest() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            String query = "test:colon:";
            Response response = client.prepareGet(String.format("http://127.0.0.1:%d/foo/test/colon?q=%s", port1, query)).setHeader("Content-Type", "text/html").execute().get(TIMEOUT, TimeUnit.SECONDS);
            assertEquals(response.getHeader("q"), query);
        }
    }
}
