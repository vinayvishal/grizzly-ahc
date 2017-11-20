/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package com.ning.http.client.ws.grizzly;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

/**
 *
 */
public class ResponseRefLeak {
    @Test
    public void referencedResponseHC() throws InterruptedException
    {
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().build();
        AsyncHttpClient client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config);

        final CountDownLatch responseLatch = new CountDownLatch(1);
        final AtomicReference<Response> responseRef = new AtomicReference<>();

        client.prepareGet("http://www.ning.com/").execute(new AsyncCompletionHandler<Response>()
        {

            @Override
            public Response onCompleted(Response response) throws Exception
            {
                responseLatch.countDown();
                responseRef.set(response);
                return response;
            }

            @Override
            public void onThrowable(Throwable t)
            {
                // Something wrong happened.
            }
        });

        responseLatch.await(5, TimeUnit.SECONDS);
        verifyNotLeaked(new PhantomReference<>(responseRef.getAndSet(null), new ReferenceQueue<>()));
    }

    private void verifyNotLeaked(PhantomReference possibleLeakPhantomRef) throws InterruptedException
    {
        for (int i = 0; i < 10; ++i)
        {
            System.gc();
            Thread.sleep(100);
            if (possibleLeakPhantomRef.isEnqueued())
            {
                break;
            }
        }
        assertTrue(possibleLeakPhantomRef.isEnqueued());
    }
}
