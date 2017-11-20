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
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public abstract class InputStreamTest extends AbstractBasicTest {

    private class InputStreamHandler extends AbstractHandler {
        public void handle(String s, Request r, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                byte[] b = new byte[3];
                request.getInputStream().read(b, 0, 3);

                response.setStatus(HttpServletResponse.SC_OK);
                response.addHeader("X-Param", new String(b));
            } else { // this handler is to handle POST request
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            response.getOutputStream().flush();
            response.getOutputStream().close();
        }
    }

    @Test(groups = { "standalone", "default_provider" })
    public void testInvalidInputStream() throws IOException, ExecutionException, TimeoutException, InterruptedException {

        try (AsyncHttpClient client = getAsyncHttpClient(null)) {
            FluentCaseInsensitiveStringsMap h = new FluentCaseInsensitiveStringsMap();
            h.add("Content-Type", "application/x-www-form-urlencoded");

            InputStream is = new InputStream() {

                public int readAllowed;

                @Override
                public int available() {
                    return 1; // Fake
                }

                @Override
                public int read() throws IOException {
                    int fakeCount = readAllowed++;
                    if (fakeCount == 0) {
                        return (int) 'a';
                    } else if (fakeCount == 1) {
                        return (int) 'b';
                    } else if (fakeCount == 2) {
                        return (int) 'c';
                    } else {
                        return -1;
                    }

                }
            };

            Response resp = client.preparePost(getTargetUrl()).setHeaders(h).setBody(is).execute().get();
            assertNotNull(resp);
            assertEquals(resp.getStatusCode(), HttpServletResponse.SC_OK);
            assertEquals(resp.getHeader("X-Param"), "abc");
        }
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new InputStreamHandler();
    }
}
