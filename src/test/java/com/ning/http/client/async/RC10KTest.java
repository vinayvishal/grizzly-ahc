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

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Reverse C10K Problem test.
 * 
 * @author Hubert Iwaniuk
 */
public abstract class RC10KTest extends AbstractBasicTest {
    private static final int C10K = 1000;
    private static final String ARG_HEADER = "Arg";
    private static final int SRV_COUNT = 10;
    protected List<Server> servers = new ArrayList<>(SRV_COUNT);
    private int[] ports;

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception {
        ports = new int[SRV_COUNT];
        for (int i = 0; i < SRV_COUNT; i++) {
            ports[i] = createServer();
        }
        log.info("Local HTTP servers started successfully");
    }

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws Exception {
        for (Server srv : servers) {
            srv.stop();
        }
    }

    private int createServer() throws Exception {
        Server srv = new Server();
        Connector listener = new SelectChannelConnector();
        listener.setHost("127.0.0.1");
        int port = findFreePort();
        listener.setPort(port);
        srv.addConnector(listener);
        srv.setHandler(configureHandler());
        srv.start();
        servers.add(srv);
        return port;
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new AbstractHandler() {
            public void handle(String s, Request r, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
                resp.setContentType("text/pain");
                String arg = s.substring(1);
                resp.setHeader(ARG_HEADER, arg);
                resp.setStatus(200);
                resp.getOutputStream().print(arg);
                resp.getOutputStream().flush();
                resp.getOutputStream().close();
            }
        };
    }

    @Test(timeOut = 10 * 60 * 1000, groups = "scalability")
    public void rc10kProblem() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        try (AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setMaxConnectionsPerHost(C10K).setAllowPoolingConnections(true).build())) {
            List<Future<Integer>> resps = new ArrayList<>(C10K);
            int i = 0;
            while (i < C10K) {
                resps.add(client.prepareGet(String.format("http://127.0.0.1:%d/%d", ports[i % SRV_COUNT], i)).execute(new MyAsyncHandler(i++)));
            }
            i = 0;
            for (Future<Integer> fResp : resps) {
                Integer resp = fResp.get();
                assertNotNull(resp);
                assertEquals(resp.intValue(), i++);
            }
        }
    }

    private class MyAsyncHandler implements AsyncHandler<Integer> {
        private String arg;
        private AtomicInteger result = new AtomicInteger(-1);

        public MyAsyncHandler(int i) {
            arg = String.format("%d", i);
        }

        public void onThrowable(Throwable t) {
            log.warn("onThrowable called.", t);
        }

        public STATE onBodyPartReceived(HttpResponseBodyPart event) throws Exception {
            String s = new String(event.getBodyPartBytes());
            result.compareAndSet(-1, new Integer(s.trim().isEmpty() ? "-1" : s));
            return STATE.CONTINUE;
        }

        public STATE onStatusReceived(HttpResponseStatus event) throws Exception {
            assertEquals(event.getStatusCode(), 200);
            return STATE.CONTINUE;
        }

        public STATE onHeadersReceived(HttpResponseHeaders event) throws Exception {
            assertEquals(event.getHeaders().getJoinedValue(ARG_HEADER, ", "), arg);
            return STATE.CONTINUE;
        }

        public Integer onCompleted() throws Exception {
            return result.get();
        }
    }
}
