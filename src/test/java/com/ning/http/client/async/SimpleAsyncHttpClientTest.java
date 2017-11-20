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

import static java.nio.charset.StandardCharsets.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;

import com.ning.http.client.Response;
import com.ning.http.client.SimpleAsyncHttpClient;
import com.ning.http.client.consumers.AppendableBodyConsumer;
import com.ning.http.client.consumers.OutputStreamBodyConsumer;
import com.ning.http.client.generators.FileBodyGenerator;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.ning.http.client.multipart.ByteArrayPart;
import com.ning.http.client.simple.HeaderMap;
import com.ning.http.client.simple.SimpleAHCTransferListener;
import com.ning.http.client.uri.Uri;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class SimpleAsyncHttpClientTest extends AbstractBasicTest {

    private final static String MY_MESSAGE = "my message";
    
    public abstract String getProviderClass();

    @Test(groups = { "standalone", "default_provider" })
    public void inputStreamBodyConsumerTest() throws Throwable {

        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setPooledConnectionIdleTimeout(100).setMaximumConnectionsTotal(50).setRequestTimeout(5 * 60 * 1000).setUrl(getTargetUrl()).setHeader("Content-Type", "text/html").build();
        try {
            Future<Response> future = client.post(new InputStreamBodyGenerator(new ByteArrayInputStream(MY_MESSAGE.getBytes())));

            System.out.println("waiting for response");
            Response response = future.get();
            assertEquals(response.getStatusCode(), 200);
            assertEquals(response.getResponseBody(), MY_MESSAGE);
        } finally {
            client.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void stringBuilderBodyConsumerTest() throws Throwable {

        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setPooledConnectionIdleTimeout(100).setMaximumConnectionsTotal(50).setRequestTimeout(5 * 60 * 1000).setUrl(getTargetUrl()).setHeader("Content-Type", "text/html").build();
        try {
            StringBuilder s = new StringBuilder();
            Future<Response> future = client.post(new InputStreamBodyGenerator(new ByteArrayInputStream(MY_MESSAGE.getBytes())), new AppendableBodyConsumer(s));

            System.out.println("waiting for response");
            Response response = future.get();
            assertEquals(response.getStatusCode(), 200);
            assertEquals(s.toString(), MY_MESSAGE);
        } finally {
            client.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void byteArrayOutputStreamBodyConsumerTest() throws Throwable {

        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setPooledConnectionIdleTimeout(100).setMaximumConnectionsTotal(50).setRequestTimeout(5 * 60 * 1000).setUrl(getTargetUrl()).setHeader("Content-Type", "text/html").build();
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream(10);
            Future<Response> future = client.post(new InputStreamBodyGenerator(new ByteArrayInputStream(MY_MESSAGE.getBytes())), new OutputStreamBodyConsumer(o));

            System.out.println("waiting for response");
            Response response = future.get();
            assertEquals(response.getStatusCode(), 200);
            assertEquals(o.toString(), MY_MESSAGE);
        } finally {
            client.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void requestByteArrayOutputStreamBodyConsumerTest() throws Throwable {

        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setUrl(getTargetUrl()).build();
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream(10);
            Future<Response> future = client.post(new InputStreamBodyGenerator(new ByteArrayInputStream(MY_MESSAGE.getBytes())), new OutputStreamBodyConsumer(o));

            System.out.println("waiting for response");
            Response response = future.get();
            assertEquals(response.getStatusCode(), 200);
            assertEquals(o.toString(), MY_MESSAGE);
        } finally {
            client.close();
        }
    }

    /**
     * See https://issues.sonatype.org/browse/AHC-5
     */
    @Test(groups = { "standalone", "default_provider" }, enabled = true)
    public void testPutZeroBytesFileTest() throws Throwable {
        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setPooledConnectionIdleTimeout(100).setMaximumConnectionsTotal(50).setRequestTimeout(5 * 1000).setUrl(getTargetUrl() + "/testPutZeroBytesFileTest.txt").setHeader("Content-Type", "text/plain")
                .build();
        try {
            File tmpfile = File.createTempFile("testPutZeroBytesFile", ".tmp");
            tmpfile.deleteOnExit();

            Future<Response> future = client.put(new FileBodyGenerator(tmpfile));

            Response response = future.get();

            tmpfile.delete();

            assertEquals(response.getStatusCode(), 200);
        } finally {
            client.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testDerive() throws Exception {
        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).build();
        SimpleAsyncHttpClient derived = client.derive().build();
        try {
            assertNotSame(derived, client);
        } finally {
            client.close();
            derived.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testDeriveOverrideURL() throws Exception {
        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setUrl("http://invalid.url").build();
        SimpleAsyncHttpClient derived = client.derive().setUrl(getTargetUrl()).build();
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream(10);

            InputStreamBodyGenerator generator = new InputStreamBodyGenerator(new ByteArrayInputStream(MY_MESSAGE.getBytes()));
            OutputStreamBodyConsumer consumer = new OutputStreamBodyConsumer(o);

            Future<Response> future = derived.post(generator, consumer);

            Response response = future.get();
            assertEquals(response.getStatusCode(), 200);
            assertEquals(o.toString(), MY_MESSAGE);
        } finally {
            client.close();
            derived.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testSimpleTransferListener() throws Exception {

        SimpleAHCTransferListener listener = new SimpleAHCTransferListener() {

            public void onStatus(Uri uri, int statusCode, String statusText) {
                assertEquals(statusCode, 200);
                assertEquals(uri.toUrl(), getTargetUrl());
            }

            public void onHeaders(Uri uri, HeaderMap headers) {
                assertEquals(uri.toUrl(), getTargetUrl());
                assertNotNull(headers);
                assertTrue(!headers.isEmpty());
                assertEquals(headers.getFirstValue("X-Custom"), "custom");
            }

            public void onCompleted(Uri uri, int statusCode, String statusText) {
                assertEquals(statusCode, 200);
                assertEquals(uri.toUrl(), getTargetUrl());
            }

            public void onBytesSent(Uri uri, long amount, long current, long total) {
                assertEquals(uri.toUrl(), getTargetUrl());
                assertEquals(total, MY_MESSAGE.getBytes().length);
            }

            public void onBytesReceived(Uri uri, long amount, long current, long total) {
                assertEquals(uri.toUrl(), getTargetUrl());
                assertEquals(total, -1);
            }
        };

        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setUrl(getTargetUrl()).setHeader("Custom", "custom").setListener(listener).build();
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream(10);

            InputStreamBodyGenerator generator = new InputStreamBodyGenerator(new ByteArrayInputStream(MY_MESSAGE.getBytes()));
            OutputStreamBodyConsumer consumer = new OutputStreamBodyConsumer(o);

            Future<Response> future = client.post(generator, consumer);

            Response response = future.get();
            assertEquals(response.getStatusCode(), 200);
            assertEquals(o.toString(), MY_MESSAGE);
        } finally {
            client.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testNullUrl() throws Exception {
        SimpleAsyncHttpClient client = null;
        try {
            client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).build().derive().build();
        } finally {
            if (client != null)
                client.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testCloseDerivedValidMaster() throws Exception {
        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setUrl(getTargetUrl()).build();
        try {
            SimpleAsyncHttpClient derived = client.derive().build();
            derived.get().get();

            derived.close();

            Response response = client.get().get();

            assertEquals(response.getStatusCode(), 200);
        } finally {
            client.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testCloseMasterInvalidDerived() throws Exception {
        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setUrl(getTargetUrl()).build();
        SimpleAsyncHttpClient derived = client.derive().build();

        client.close();

        try {
            derived.get().get();
            fail("Expected closed AHC");
            // expected -- Seems to me that this behavior conflicts with the requirements of Future.get()
            
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IOException);
        
        } finally {
            client.close();
            derived.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testMultiPartPut() throws Exception {
        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setUrl(getTargetUrl() + "/multipart").build();
        try {
            Response response = client.put(new ByteArrayPart("baPart", "testMultiPart".getBytes(UTF_8), "application/test", UTF_8, "fileName")).get();

            String body = response.getResponseBody();
            String contentType = response.getHeader("X-Content-Type");

            assertTrue(contentType.contains("multipart/form-data"));

            String boundary = contentType.substring(contentType.lastIndexOf("=") + 1);

            assertTrue(body.startsWith("--" + boundary));
            assertTrue(body.trim().endsWith("--" + boundary + "--"));
            assertTrue(body.contains("Content-Disposition:"));
            assertTrue(body.contains("Content-Type: application/test"));
            assertTrue(body.contains("name=\"baPart"));
            assertTrue(body.contains("filename=\"fileName"));
        } finally {
            client.close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testMultiPartPost() throws Exception {
        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder().setProviderClass(getProviderClass()).setUrl(getTargetUrl() + "/multipart").build();
        try {
            Response response = client.post(new ByteArrayPart("baPart", "testMultiPart".getBytes(UTF_8), "application/test", UTF_8, "fileName")).get();

            String body = response.getResponseBody();
            String contentType = response.getHeader("X-Content-Type");

            assertTrue(contentType.contains("multipart/form-data"));

            String boundary = contentType.substring(contentType.lastIndexOf("=") + 1);

            assertTrue(body.startsWith("--" + boundary));
            assertTrue(body.trim().endsWith("--" + boundary + "--"));
            assertTrue(body.contains("Content-Disposition:"));
            assertTrue(body.contains("Content-Type: application/test"));
            assertTrue(body.contains("name=\"baPart"));
            assertTrue(body.contains("filename=\"fileName"));
        } finally {
            client.close();
        }
    }
}
