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
import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.Response;

public abstract class ByteBufferCapacityTest extends AbstractBasicTest {
    private static final File TMP = new File(System.getProperty("java.io.tmpdir"), "ahc-tests-" + UUID.randomUUID().toString().substring(0, 8));

    private class BasicHandler extends AbstractHandler {

        public void handle(String s, org.eclipse.jetty.server.Request r, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

            Enumeration<?> e = httpRequest.getHeaderNames();
            String param;
            while (e.hasMoreElements()) {
                param = e.nextElement().toString();
                httpResponse.addHeader("X-" + param, httpRequest.getHeader(param));
            }

            int size = 10 * 1024;
            if (httpRequest.getContentLength() > 0) {
                size = httpRequest.getContentLength();
            }
            byte[] bytes = new byte[size];
            if (bytes.length > 0) {
                final InputStream in = httpRequest.getInputStream();
                final OutputStream out = httpResponse.getOutputStream();
                int read;
                while ((read = in.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
            }

            httpResponse.setStatus(200);
            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
        }
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new BasicHandler();
    }

    @Test(groups = { "standalone", "default_provider" })
    public void basicByteBufferTest() throws Throwable {
        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            byte[] bytes = "RatherLargeFileRatherLargeFileRatherLargeFileRatherLargeFile".getBytes("UTF-16");
            long repeats = (1024 * 100 * 10 / bytes.length) + 1;
            File largeFile = createTempFile(bytes, (int) repeats);
            final AtomicInteger byteReceived = new AtomicInteger();

            try {
                Response response = client.preparePut(getTargetUrl()).setBody(largeFile).execute(new AsyncCompletionHandlerAdapter() {
                    @Override
                    public STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
                        byteReceived.addAndGet(content.getBodyByteBuffer().capacity());
                        return super.onBodyPartReceived(content);
                    }

                }).get();

                assertNotNull(response);
                assertEquals(response.getStatusCode(), 200);
                assertEquals(byteReceived.get(), largeFile.length());
                assertEquals(response.getResponseBody().length(), largeFile.length());

            } catch (IOException ex) {
                fail("Should have timed out");
            }
        }
    }

    public String getTargetUrl() {
        return String.format("http://127.0.0.1:%d/foo/test", port1);
    }

    public static File createTempFile(byte[] pattern, int repeat) throws IOException {
        TMP.mkdirs();
        TMP.deleteOnExit();
        File tmpFile = File.createTempFile("tmpfile-", ".data", TMP);
        write(pattern, repeat, tmpFile);

        return tmpFile;
    }

    public static void write(byte[] pattern, int repeat, File file) throws IOException {
        file.deleteOnExit();
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            for (int i = 0; i < repeat; i++) {
                out.write(pattern);
            }
        }
    }
}
