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
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.webdav.WebDavCompletionHandlerBase;
import com.ning.http.client.webdav.WebDavResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Embedded;
import org.apache.coyote.http11.Http11NioProtocol;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public abstract class WebDavBasicTest extends AbstractBasicTest {

    public Embedded embedded;

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception {

        port1 = findFreePort();
        embedded = new Embedded();
        String path = new File(".").getAbsolutePath();
        embedded.setCatalinaHome(path);

        Engine engine = embedded.createEngine();
        engine.setDefaultHost("127.0.0.1");

        Host host = embedded.createHost("127.0.0.1", path);
        engine.addChild(host);

        Context c = embedded.createContext("/", path);
        c.setReloadable(false);
        Wrapper w = c.createWrapper();
        w.addMapping("/*");
        w.setServletClass(org.apache.catalina.servlets.WebdavServlet.class.getName());
        w.addInitParameter("readonly", "false");
        w.addInitParameter("listings", "true");

        w.setLoadOnStartup(0);

        c.addChild(w);
        host.addChild(c);

        Connector connector = embedded.createConnector("127.0.0.1", port1, Http11NioProtocol.class.getName());
        connector.setContainer(host);
        embedded.addEngine(engine);
        embedded.addConnector(connector);
        embedded.start();
    }

    protected String getTargetUrl() {
        return String.format("http://127.0.0.1:%s/folder1", port1);
    }

    @AfterMethod(alwaysRun = true)
    public void clean() throws InterruptedException, Exception {
        AsyncHttpClient c = getAsyncHttpClient(null);

        Request deleteRequest = new RequestBuilder("DELETE").setUrl(getTargetUrl()).build();
        c.executeRequest(deleteRequest).get();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws InterruptedException, Exception {
        embedded.stop();
    }

    @Test(groups = { "standalone", "default_provider" })
    public void mkcolWebDavTest1() throws InterruptedException, IOException, ExecutionException {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Request mkcolRequest = new RequestBuilder("MKCOL").setUrl(getTargetUrl()).build();
            Response response = client.executeRequest(mkcolRequest).get();

            assertEquals(response.getStatusCode(), 201);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void mkcolWebDavTest2() throws InterruptedException, IOException, ExecutionException {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Request mkcolRequest = new RequestBuilder("MKCOL").setUrl(getTargetUrl() + "/folder2").build();
            Response response = client.executeRequest(mkcolRequest).get();
            assertEquals(response.getStatusCode(), 409);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void basicPropFindWebDavTest() throws InterruptedException, IOException, ExecutionException {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Request propFindRequest = new RequestBuilder("PROPFIND").setUrl(getTargetUrl()).build();
            Response response = client.executeRequest(propFindRequest).get();
            assertEquals(response.getStatusCode(), 404);
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void propFindWebDavTest() throws InterruptedException, IOException, ExecutionException {

        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Request mkcolRequest = new RequestBuilder("MKCOL").setUrl(getTargetUrl()).build();
            Response response = client.executeRequest(mkcolRequest).get();
            assertEquals(response.getStatusCode(), 201);

            Request putRequest = new RequestBuilder("PUT").setUrl(String.format("http://127.0.0.1:%s/folder1/Test.txt", port1)).setBody("this is a test").build();
            response = client.executeRequest(putRequest).get();
            assertEquals(response.getStatusCode(), 201);

            Request propFindRequest = new RequestBuilder("PROPFIND").setUrl(String.format("http://127.0.0.1:%s/folder1/Test.txt", port1)).build();
            response = client.executeRequest(propFindRequest).get();

            assertEquals(response.getStatusCode(), 207);
            assertTrue(response.getResponseBody().contains("<status>HTTP/1.1 200 OK</status>"));
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void propFindCompletionHandlerWebDavTest() throws InterruptedException, IOException, ExecutionException {

        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            Request mkcolRequest = new RequestBuilder("MKCOL").setUrl(getTargetUrl()).build();
            Response response = client.executeRequest(mkcolRequest).get();
            assertEquals(response.getStatusCode(), 201);

            Request propFindRequest = new RequestBuilder("PROPFIND").setUrl(getTargetUrl()).build();
            WebDavResponse webDavResponse = client.executeRequest(propFindRequest, new WebDavCompletionHandlerBase<WebDavResponse>() {
                @Override
                public void onThrowable(Throwable t) {
                    t.printStackTrace();
                }

                @Override
                public WebDavResponse onCompleted(WebDavResponse response) throws Exception {
                    return response;
                }
            }).get();

            assertNotNull(webDavResponse);
            assertEquals(webDavResponse.getStatusCode(), 200);
        }
    }
}
