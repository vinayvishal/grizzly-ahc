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

import static org.testng.Assert.*;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Response;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.annotations.Test;

/**
 * Proxy usage tests.
 * 
 * @author Hubert Iwaniuk
 */
public abstract class ProxyTest extends AbstractBasicTest {
    private class ProxyHandler extends AbstractHandler {
        public void handle(String s, Request r, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                response.addHeader("target", r.getUri().getPath());
                response.setStatus(HttpServletResponse.SC_OK);
            } else { // this handler is to handle POST request
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            r.setHandled(true);
        }
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new ProxyHandler();
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testRequestLevelProxy() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            String target = "http://127.0.0.1:1234/";
            Future<Response> f = client.prepareGet(target).setProxyServer(new ProxyServer("127.0.0.1", port1)).execute();
            Response resp = f.get(3, TimeUnit.SECONDS);
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(resp.getHeader("target"), "/");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testGlobalProxy() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        AsyncHttpClientConfig cfg = new AsyncHttpClientConfig.Builder().setProxyServer(new ProxyServer("127.0.0.1", port1)).build();
        try (AsyncHttpClient client = getAsyncHttpClient(cfg)) {
            String target = "http://127.0.0.1:1234/";
            Future<Response> f = client.prepareGet(target).execute();
            Response resp = f.get(3, TimeUnit.SECONDS);
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(resp.getHeader("target"), "/");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testBothProxies() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        AsyncHttpClientConfig cfg = new AsyncHttpClientConfig.Builder().setProxyServer(new ProxyServer("127.0.0.1", port1 - 1)).build();
        try (AsyncHttpClient client = getAsyncHttpClient(cfg)) {
            String target = "http://127.0.0.1:1234/";
            Future<Response> f = client.prepareGet(target).setProxyServer(new ProxyServer("127.0.0.1", port1)).execute();
            Response resp = f.get(3, TimeUnit.SECONDS);
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(resp.getHeader("target"), "/");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testNonProxyHosts() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        AsyncHttpClientConfig cfg = new AsyncHttpClientConfig.Builder().setProxyServer(new ProxyServer("127.0.0.1", port1 - 1)).build();
        try (AsyncHttpClient client = getAsyncHttpClient(cfg)) {

            String target = "http://127.0.0.1:1234/";
            client.prepareGet(target).setProxyServer(new ProxyServer("127.0.0.1", port1).addNonProxyHost("127.0.0.1")).execute().get();
            assertFalse(true);
        } catch (Throwable e) {
            assertNotNull(e.getCause());
            assertEquals(e.getCause().getClass(), ConnectException.class);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testNonProxyHostIssue202() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            String target = "http://127.0.0.1:" + port1 + "/";
            Future<Response> f = client.prepareGet(target).setProxyServer(new ProxyServer("127.0.0.1", port1 - 1).addNonProxyHost("127.0.0.1")).execute();
            Response resp = f.get(3, TimeUnit.SECONDS);
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(resp.getHeader("target"), "/");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testProxyProperties() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        Properties originalProps = System.getProperties();
        try {
            Properties props = new Properties();
            props.putAll(originalProps);

            System.setProperties(props);

            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", String.valueOf(port1));
            System.setProperty("http.nonProxyHosts", "localhost");

            AsyncHttpClientConfig cfg = new AsyncHttpClientConfig.Builder().setUseProxyProperties(true).build();
            try (AsyncHttpClient client = getAsyncHttpClient(cfg)) {
                String target = "http://127.0.0.1:1234/";
                Future<Response> f = client.prepareGet(target).execute();
                Response resp = f.get(3, TimeUnit.SECONDS);
                assertNotNull(resp);
                assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
                assertEquals(resp.getHeader("target"), "/");

                target = "http://localhost:1234/";
                f = client.prepareGet(target).execute();
                try {
                    resp = f.get(3, TimeUnit.SECONDS);
                    fail("should not be able to connect");
                } catch (ExecutionException e) {
                    // ok, no proxy used
                }
            }
        } finally {
            System.setProperties(originalProps);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testIgnoreProxyPropertiesByDefault() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        Properties originalProps = System.getProperties();
        try {
            Properties props = new Properties();
            props.putAll(originalProps);

            System.setProperties(props);

            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", String.valueOf(port1));
            System.setProperty("http.nonProxyHosts", "localhost");

            try (AsyncHttpClient client = getAsyncHttpClient(null)) {
                String target = "http://127.0.0.1:1234/";
                Future<Response> f = client.prepareGet(target).execute();
                try {
                    f.get(3, TimeUnit.SECONDS);
                    fail("should not be able to connect");
                } catch (ExecutionException e) {
                    // ok, no proxy used
                }
            }
        } finally {
            System.setProperties(originalProps);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testProxyActivationProperty() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        Properties originalProps = System.getProperties();
        try {
            Properties props = new Properties();
            props.putAll(originalProps);

            System.setProperties(props);

            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", String.valueOf(port1));
            System.setProperty("http.nonProxyHosts", "localhost");
            System.setProperty("com.ning.http.client.AsyncHttpClientConfig.useProxyProperties", "true");

            try (AsyncHttpClient client = getAsyncHttpClient(null)) {
                String target = "http://127.0.0.1:1234/";
                Future<Response> f = client.prepareGet(target).execute();
                Response resp = f.get(3, TimeUnit.SECONDS);
                assertNotNull(resp);
                assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
                assertEquals(resp.getHeader("target"), "/");

                target = "http://localhost:1234/";
                f = client.prepareGet(target).execute();
                try {
                    resp = f.get(3, TimeUnit.SECONDS);
                    fail("should not be able to connect");
                } catch (ExecutionException e) {
                    // ok, no proxy used
                }
            }
        } finally {
            System.setProperties(originalProps);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testWildcardNonProxyHosts() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        Properties originalProps = System.getProperties();
        try {
            Properties props = new Properties();
            props.putAll(originalProps);

            System.setProperties(props);

            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", String.valueOf(port1));
            System.setProperty("http.nonProxyHosts", "127.*");

            AsyncHttpClientConfig cfg = new AsyncHttpClientConfig.Builder().setUseProxyProperties(true).build();
            try (AsyncHttpClient client = getAsyncHttpClient(cfg)) {
                String target = "http://127.0.0.1:1234/";
                Future<Response> f = client.prepareGet(target).execute();
                try {
                    f.get(3, TimeUnit.SECONDS);
                    fail("should not be able to connect");
                } catch (ExecutionException e) {
                    // ok, no proxy used
                }
            }
        } finally {
            System.setProperties(originalProps);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testUseProxySelector() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        ProxySelector originalProxySelector = ProxySelector.getDefault();
        try {
            ProxySelector.setDefault(new ProxySelector() {
                public List<Proxy> select(URI uri) {
                    if (uri.getHost().equals("127.0.0.1")) {
                        return Arrays.asList(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", port1)));
                    } else {
                        return Arrays.asList(Proxy.NO_PROXY);
                    }
                }

                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                }
            });

            AsyncHttpClientConfig cfg = new AsyncHttpClientConfig.Builder().setUseProxySelector(true).build();
            try (AsyncHttpClient client = getAsyncHttpClient(cfg)) {
                String target = "http://127.0.0.1:1234/";
                Future<Response> f = client.prepareGet(target).execute();
                Response resp = f.get(3, TimeUnit.SECONDS);
                assertNotNull(resp);
                assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
                assertEquals(resp.getHeader("target"), "/");

                target = "http://localhost:1234/";
                f = client.prepareGet(target).execute();
                try {
                    f.get(3, TimeUnit.SECONDS);
                    fail("should not be able to connect");
                } catch (ExecutionException e) {
                    // ok, no proxy used
                }
            }
        } finally {
            ProxySelector.setDefault(originalProxySelector);
        }
    }
}
