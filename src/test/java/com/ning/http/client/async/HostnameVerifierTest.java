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
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.Response;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.net.ssl.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;

public abstract class HostnameVerifierTest extends AbstractBasicTest {

    protected final Logger log = LoggerFactory.getLogger(HostnameVerifierTest.class);

    public static class EchoHandler extends AbstractHandler {

        @Override
        public void handle(String pathInContext, Request r, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

            httpResponse.setContentType("text/html; charset=utf-8");
            Enumeration<?> e = httpRequest.getHeaderNames();
            String param;
            while (e.hasMoreElements()) {
                param = e.nextElement().toString();

                if (param.startsWith("LockThread")) {
                    try {
                        Thread.sleep(40 * 1000);
                    } catch (InterruptedException ex) { // nothing to do here
                    }
                }

                httpResponse.addHeader("X-" + param, httpRequest.getHeader(param));
            }

            Enumeration<?> i = httpRequest.getParameterNames();

            StringBuilder requestBody = new StringBuilder();
            while (i.hasMoreElements()) {
                param = i.nextElement().toString();
                httpResponse.addHeader("X-" + param, httpRequest.getParameter(param));
                requestBody.append(param);
                requestBody.append("_");
            }

            String pathInfo = httpRequest.getPathInfo();
            if (pathInfo != null)
                httpResponse.addHeader("X-pathInfo", pathInfo);

            String queryString = httpRequest.getQueryString();
            if (queryString != null)
                httpResponse.addHeader("X-queryString", queryString);

            httpResponse.addHeader("X-KEEP-ALIVE", httpRequest.getRemoteAddr() + ":" + httpRequest.getRemotePort());

            javax.servlet.http.Cookie[] cs = httpRequest.getCookies();
            if (cs != null) {
                for (javax.servlet.http.Cookie c : cs) {
                    httpResponse.addCookie(c);
                }
            }

            if (requestBody.length() > 0) {
                httpResponse.getOutputStream().write(requestBody.toString().getBytes());
            }

            int size = 10 * 1024;
            if (httpRequest.getContentLength() > 0) {
                size = httpRequest.getContentLength();
            }
            byte[] bytes = new byte[size];
            int pos = 0;
            if (bytes.length > 0) {
                // noinspection ResultOfMethodCallIgnored
                int read = 0;
                while (read != -1) {
                    read = httpRequest.getInputStream().read(bytes, pos, bytes.length - pos);
                    pos += read;
                }

                httpResponse.getOutputStream().write(bytes);
            }

            httpResponse.setStatus(200);
            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();

        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws Exception {
        server.stop();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownProps() throws Exception {
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.trustStore");
    }

    protected String getTargetUrl() {
        return String.format("https://127.0.0.1:%d/foo/test", port1);
    }

    public AbstractHandler configureHandler() throws Exception {
        return new EchoHandler();
    }

    protected int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception {
        server = new Server();
        port1 = findFreePort();
        SslSocketConnector connector = new SslSocketConnector();
        connector.setHost("127.0.0.1");
        connector.setPort(port1);

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

        server.setHandler(configureHandler());
        server.start();
        log.info("Local HTTP server started successfully");
    }

    @Test(groups = { "standalone", "default_provider" })
    public void positiveHostnameVerifierTest() throws Throwable {

        try (AsyncHttpClient client = getAsyncHttpClient(new Builder().setHostnameVerifier(new PositiveHostVerifier()).setSSLContext(createSSLContext()).build())) {
            ClassLoader cl = getClass().getClassLoader();
            // override system properties
            URL url = cl.getResource("SimpleTextFile.txt");
            File file = new File(url.toURI());

            Future<Response> f = client.preparePost(getTargetUrl()).setBody(file).setHeader("Content-Type", "text/html").execute();
            Response resp = f.get();
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(resp.getResponseBody(), "This is a simple test file");
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void negativeHostnameVerifierTest() throws Throwable {

        try (AsyncHttpClient client = getAsyncHttpClient(new Builder().setHostnameVerifier(new NegativeHostVerifier()).setSSLContext(createSSLContext()).build())) {
            ClassLoader cl = getClass().getClassLoader();
            // override system properties
            URL url = cl.getResource("SimpleTextFile.txt");
            File file = new File(url.toURI());

            try {
                client.preparePost(getTargetUrl()).setBody(file).setHeader("Content-Type", "text/html").execute().get();
            } catch (ExecutionException ex) {
                assertTrue(ex.getCause() instanceof ConnectException);
            }
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void remoteIDHostnameVerifierTest() throws Throwable {

        try (AsyncHttpClient client = getAsyncHttpClient(new Builder().setHostnameVerifier(new CheckHost("bouette")).setSSLContext(createSSLContext()).build())) {
            ClassLoader cl = getClass().getClassLoader();
            // override system properties
            URL url = cl.getResource("SimpleTextFile.txt");
            File file = new File(url.toURI());

            try {
                client.preparePost(getTargetUrl()).setBody(file).setHeader("Content-Type", "text/html").execute().get();
            } catch (ExecutionException ex) {
                assertTrue(ex.getCause() instanceof ConnectException);
            }
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void remotePosHostnameVerifierTest() throws Throwable {

        try (AsyncHttpClient client = getAsyncHttpClient(new Builder().setHostnameVerifier(new CheckHost("localhost")).setSSLContext(createSSLContext()).build())) {
            ClassLoader cl = getClass().getClassLoader();
            // override system properties
            URL url = cl.getResource("SimpleTextFile.txt");
            File file = new File(url.toURI());

            try {
                client.preparePost(getTargetUrl()).setBody(file).setHeader("Content-Type", "text/html").execute().get();
            } catch (ExecutionException ex) {
                assertTrue(ex.getCause() instanceof ConnectException);
            }
        }
    }

    public static class PositiveHostVerifier implements HostnameVerifier {

        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    public static class NegativeHostVerifier implements HostnameVerifier {

        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    public static class CheckHost implements HostnameVerifier {

        private final String hostName;

        public CheckHost(String hostName) {
            this.hostName = hostName;
        }

        public boolean verify(String s, SSLSession sslSession) {

            if (s != null && s.equalsIgnoreCase(hostName)) {
                return true;
            }

            return false;
        }
    }

    private static SSLContext createSSLContext() {
        try {
            InputStream keyStoreStream = HostnameVerifierTest.class.getResourceAsStream("ssltest-cacerts.jks");
            char[] keyStorePassword = "changeit".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(keyStoreStream, keyStorePassword);

            // Set up key manager factory to use our key store
            char[] certificatePassword = "changeit".toCharArray();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, certificatePassword);

            // Initialize the SSLContext to work with our key managers.
            KeyManager[] keyManagers = kmf.getKeyManagers();
            TrustManager[] trustManagers = new TrustManager[] { DUMMY_TRUST_MANAGER };
            SecureRandom secureRandom = new SecureRandom();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, secureRandom);

            return sslContext;
        } catch (Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        }
    }

    private static final AtomicBoolean TRUST_SERVER_CERT = new AtomicBoolean(true);
    private static final TrustManager DUMMY_TRUST_MANAGER = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (!TRUST_SERVER_CERT.get()) {
                throw new CertificateException("Server certificate not trusted.");
            }
        }
    };

}
