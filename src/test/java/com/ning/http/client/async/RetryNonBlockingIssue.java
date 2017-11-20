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
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static org.testng.Assert.assertTrue;


public class RetryNonBlockingIssue {

    private URI servletEndpointUri;

    private Server server;

    private int port1;

    public static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            // 0 is open a socket on any free port
            return socket.getLocalPort();
        }
    }


    @BeforeMethod
    public void setUp() throws Exception {
        server = new Server();

        port1 = findFreePort();

        Connector listener = new SelectChannelConnector();
        listener.setHost("127.0.0.1");
        listener.setPort(port1);

        server.addConnector(listener);


        ServletContextHandler context = new
                ServletContextHandler(ServletContextHandler.SESSIONS);

        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new
                MockExceptionServlet()), "/*");

        server.start();

        servletEndpointUri = new URI("http://127.0.0.1:" + port1 + "/");
    }

    @AfterMethod
    public void stop() {

        try {
            if (server != null) server.stop();
        } catch (Exception e) {
        }


    }

    private ListenableFuture<Response> testMethodRequest(AsyncHttpClient
            fetcher, int requests, String action, String id) throws IOException {
        RequestBuilder builder = new RequestBuilder("GET");
        builder.addQueryParam(action, "1");

        builder.addQueryParam("maxRequests", "" + requests);
        builder.addQueryParam("id", id);
        builder.setUrl(servletEndpointUri.toString());
        com.ning.http.client.Request r = builder.build();
        return fetcher.executeRequest(r);

    }

    /**
     * Tests that a head request can be made
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testRetryNonBlocking() throws IOException, InterruptedException,
            ExecutionException {
        AsyncHttpClientConfig.Builder bc = new AsyncHttpClientConfig.Builder()//
        .setAllowPoolingConnections(true)//
        .setMaxConnections(100)//
        .setConnectTimeout(60000)//
        .setRequestTimeout(30000);
        
        List<ListenableFuture<Response>> res = new ArrayList<>();
        try (AsyncHttpClient client = new AsyncHttpClient(bc.build())) {
            for (int i = 0; i < 32; i++) {
                res.add(testMethodRequest(client, 3, "servlet", UUID.randomUUID().toString()));
            }

            StringBuilder b = new StringBuilder();
            for (ListenableFuture<Response> r : res) {
                Response theres = r.get();
                b.append("==============\r\n");
                b.append("Response Headers\r\n");
                Map<String, List<String>> heads = theres.getHeaders();
                b.append(heads + "\r\n");
                b.append("==============\r\n");
                assertTrue(heads.size() > 0);
            }
            System.out.println(b.toString());
            System.out.flush();
        }
    }

    @Test
    public void testRetryNonBlockingAsyncConnect() throws IOException, InterruptedException,
            ExecutionException {
        AsyncHttpClientConfig.Builder bc = new AsyncHttpClientConfig.Builder()//
        .setAllowPoolingConnections(true)//
        .setMaxConnections(100)//
        .setConnectTimeout(60000)//
        .setRequestTimeout(30000);
        List<ListenableFuture<Response>> res = new ArrayList<>();
        try (AsyncHttpClient client = new AsyncHttpClient(bc.build())) {
            for (int i = 0; i < 32; i++) {
                res.add(testMethodRequest(client, 3, "servlet", UUID.randomUUID().toString()));
            }

            StringBuilder b = new StringBuilder();
            for (ListenableFuture<Response> r : res) {
                Response theres = r.get();
                b.append("==============\r\n");
                b.append("Response Headers\r\n");
                Map<String, List<String>> heads = theres.getHeaders();
                b.append(heads + "\r\n");
                b.append("==============\r\n");
                assertTrue(heads.size() > 0);
            }
            System.out.println(b.toString());
            System.out.flush();
        }
    }

    @Test
    public void testRetryBlocking() throws IOException, InterruptedException,
            ExecutionException {
        AsyncHttpClientConfig.Builder bc = new AsyncHttpClientConfig.Builder()//
        .setAllowPoolingConnections(true)//
        .setMaxConnections(100)//
        .setConnectTimeout(30000)//
        .setRequestTimeout(30000);
        List<ListenableFuture<Response>> res = new ArrayList<>();
        try (AsyncHttpClient client = new AsyncHttpClient(bc.build())) {
            for (int i = 0; i < 32; i++) {
                res.add(testMethodRequest(client, 3, "servlet", UUID.randomUUID().toString()));
            }

            StringBuilder b = new StringBuilder();
            for (ListenableFuture<Response> r : res) {
                Response theres = r.get();
                b.append("==============\r\n");
                b.append("Response Headers\r\n");
                Map<String, List<String>> heads = theres.getHeaders();
                b.append(heads + "\r\n");
                b.append("==============\r\n");
                assertTrue(heads.size() > 0);

            }
            System.out.println(b.toString());
            System.out.flush();
        }
    }

    @SuppressWarnings("serial")
    public class MockExceptionServlet extends HttpServlet {

        private Map<String, Integer> requests = new ConcurrentHashMap<>();

        private synchronized int increment(String id) {
            int val = 0;
            if (requests.containsKey(id)) {
                Integer i = requests.get(id);
                val = i + 1;
                requests.put(id, val);
            } else {
                requests.put(id, 1);
                val = 1;
            }
            System.out.println("REQUESTS: " + requests);
            return val;
        }

        public void service(HttpServletRequest req, HttpServletResponse res)
                throws ServletException, IOException {
            String maxRequests = req.getParameter("maxRequests");
            int max = 0;
            try {
                max = Integer.parseInt(maxRequests);
            }
            catch (NumberFormatException e) {
                max = 3;
            }
            String id = req.getParameter("id");
            int requestNo = increment(id);
            String servlet = req.getParameter("servlet");
            String io = req.getParameter("io");
            String error = req.getParameter("500");


            if (requestNo >= max) {
                res.setHeader("Success-On-Attempt", "" + requestNo);
                res.setHeader("id", id);
                if (servlet != null && servlet.trim().length() > 0)
                    res.setHeader("type", "servlet");
                if (error != null && error.trim().length() > 0)
                    res.setHeader("type", "500");
                if (io != null && io.trim().length() > 0)
                    res.setHeader("type", "io");
                res.setStatus(200);
                res.setContentLength(0);
                return;
            }


            res.setStatus(200);
            res.setContentLength(100);
            res.setContentType("application/octet-stream");

            res.flushBuffer();

            if (servlet != null && servlet.trim().length() > 0)
                throw new ServletException("Servlet Exception");

            if (io != null && io.trim().length() > 0)
                throw new IOException("IO Exception");

            if (error != null && error.trim().length() > 0)
                res.sendError(500, "servlet process was 500");
        }
    }
}

