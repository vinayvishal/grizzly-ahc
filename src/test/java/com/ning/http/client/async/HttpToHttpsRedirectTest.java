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
import com.ning.http.client.Response;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public abstract class HttpToHttpsRedirectTest extends AbstractBasicTest {

    // FIXME super NOT threadsafe!!!
    private final AtomicBoolean isSet = new AtomicBoolean(false);

    private class Relative302Handler extends AbstractHandler {

        public void handle(String s, Request r, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

            String param;
            httpResponse.setContentType("text/html; charset=utf-8");
            Enumeration<?> e = httpRequest.getHeaderNames();
            while (e.hasMoreElements()) {
                param = e.nextElement().toString();

                if (param.startsWith("X-redirect") && !isSet.getAndSet(true)) {
                    httpResponse.addHeader("Location", httpRequest.getHeader(param));
                    httpResponse.setStatus(302);
                    httpResponse.getOutputStream().flush();
                    httpResponse.getOutputStream().close();
                    return;
                }
            }

            if (r.getScheme().equalsIgnoreCase("https")) {
                httpResponse.addHeader("X-httpToHttps", "PASS");
                isSet.getAndSet(false);
            }

            httpResponse.setStatus(200);
            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception {
        server = new Server();

        port1 = findFreePort();
        port2 = findFreePort();

        Connector listener = new SelectChannelConnector();

        listener.setHost("127.0.0.1");
        listener.setPort(port1);
        server.addConnector(listener);

        SslSocketConnector connector = new SslSocketConnector();
        connector.setHost("127.0.0.1");
        connector.setPort(port2);

        ClassLoader cl = getClass().getClassLoader();
        // override system properties
        URL cacertsUrl = cl.getResource("ssltest-cacerts.jks");
        String trustStoreFile = new File(cacertsUrl.toURI()).getAbsolutePath();
        connector.setTruststore(trustStoreFile);
        connector.setTrustPassword("changeit");
        connector.setTruststoreType("JKS");

        log.info("SSL certs path: {}", trustStoreFile);

        // override system properties
        URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
        String keyStoreFile = new File(keystoreUrl.toURI()).getAbsolutePath();
        connector.setKeystore(keyStoreFile);
        connector.setKeyPassword("changeit");
        connector.setKeystoreType("JKS");

        log.info("SSL keystore path: {}", keyStoreFile);

        server.addConnector(connector);

        server.setHandler(new Relative302Handler());
        server.start();
        log.info("Local HTTP server started successfully");
    }

    @Test(groups = { "standalone", "default_provider" })
    public void httpToHttpsRedirect() throws Throwable {
        isSet.getAndSet(false);

        AsyncHttpClientConfig cg = new AsyncHttpClientConfig.Builder()//
                .setMaxRedirects(5)//
                .setFollowRedirect(true)//
                .setAcceptAnyCertificate(true)//
                .build();

        try (AsyncHttpClient client = getAsyncHttpClient(cg)) {
            Response response = client.prepareGet(getTargetUrl()).setHeader("X-redirect", getTargetUrl2()).execute().get();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
            assertEquals(response.getHeader("X-httpToHttps"), "PASS");
        }
    }

    public String getTargetUrl2() {
        return String.format("https://127.0.0.1:%d/foo/test", port2);
    }

    @Test(groups = { "standalone", "default_provider" })
    public void httpToHttpsProperConfig() throws Throwable {
        isSet.getAndSet(false);

        AsyncHttpClientConfig cg = new AsyncHttpClientConfig.Builder()//
                .setMaxRedirects(5)//
                .setFollowRedirect(true)//
                .setAcceptAnyCertificate(true)//
                .build();
        try (AsyncHttpClient client = getAsyncHttpClient(cg)) {
            Response response = client.prepareGet(getTargetUrl()).setHeader("X-redirect", getTargetUrl2() + "/test2").execute().get();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
            assertEquals(response.getHeader("X-httpToHttps"), "PASS");

            // Test if the internal channel is downgraded to clean http.
            response = client.prepareGet(getTargetUrl()).setHeader("X-redirect", getTargetUrl2() + "/foo2").execute().get();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
            assertEquals(response.getHeader("X-httpToHttps"), "PASS");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void relativeLocationUrl() throws Throwable {
        isSet.getAndSet(false);

        AsyncHttpClientConfig cg = new AsyncHttpClientConfig.Builder()//
                .setMaxRedirects(5)//
                .setFollowRedirect(true)//
                .setAcceptAnyCertificate(true)//
                .build();
        try (AsyncHttpClient client = getAsyncHttpClient(cg)) {
            Response response = client.prepareGet(getTargetUrl()).setHeader("X-redirect", "/foo/test").execute().get();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), 200);
            assertEquals(response.getUri().toString(), getTargetUrl());
        }
    }
}
