/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package com.ning.http.client.providers.grizzly;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.listenable.AbstractListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

/**
 * {@link AbstractListenableFuture} implementation adaptation of Grizzly's
 * {@link FutureImpl}.
 *
 * @author The Grizzly Team
 * @since 1.7.0
 */
final class GrizzlyResponseFuture<V> extends AbstractListenableFuture<V>
        implements CompletionHandler<V> {

    private final FutureImpl<V> delegate;
//    private final GrizzlyAsyncHttpProvider provider;
//    private Request request;
//    private Connection connection;
    private AsyncHandler asyncHandler;
    
    // transaction context. Not null if connection is established
    private volatile HttpTransactionContext transactionCtx;


    // ------------------------------------------------------------ Constructors


    GrizzlyResponseFuture(final AsyncHandler asyncHandler) {
        this.asyncHandler = asyncHandler;
        
        delegate = Futures.<V>createSafeFuture();
        delegate.addCompletionHandler(this);
    }


    // ----------------------------------- Methods from AbstractListenableFuture


    public void done() {
        done(null);
    }

    public void done(V result) {
        delegate.result(result);
    }

    public void abort(Throwable t) {

        delegate.failure(t);

    }

    public void touch() {
        final HttpTransactionContext tx = transactionCtx;
        if (tx != null) {
            tx.touchConnection();
        }

    }


    public boolean getAndSetWriteHeaders(boolean writeHeaders) {

        // TODO This doesn't currently do anything - and may not make sense
        // with our implementation.  Needs further analysis.
        return writeHeaders;

    }


    public boolean getAndSetWriteBody(boolean writeBody) {

        // TODO This doesn't currently do anything - and may not make sense
        // with our implementation.  Needs further analysis.
        return writeBody;

    }


    // ----------------------------------------------------- Methods from Future


    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }


    public boolean isCancelled() {

        return delegate.isCancelled();

    }


    public boolean isDone() {

        return delegate.isDone();

    }


    public V get() throws InterruptedException, ExecutionException {

        return delegate.get();

    }


    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {

        return delegate.get(timeout, unit);

    }

    // ----------------------------------------------------- Methods from CompletionHandler

    @Override
    public void cancelled() {
        final AsyncHandler ah = asyncHandler;
        if (ah != null) {
            try {
                ah.onThrowable(new CancellationException());
            } catch (Throwable ignore) {
            }
        }
        
        runListeners();
    }

    @Override
    public void failed(final Throwable t) {
        final AsyncHandler ah = asyncHandler;
        if (ah != null) {
            try {
                ah.onThrowable(t);
            } catch (Throwable ignore) {
            }
        }
            
        final HttpTransactionContext tx = transactionCtx;
        if (tx != null) {
            tx.closeConnection();
        }

        runListeners();
    }

    @Override
    public void completed(V result) {
        runListeners();
    }

    @Override
    public void updated(V result) {
    }        

    // ------------------------------------------------- Package Private Methods

    AsyncHandler getAsyncHandler() {
        return asyncHandler;
    }

    void setAsyncHandler(final AsyncHandler asyncHandler) {
        this.asyncHandler = asyncHandler;
    }

    /**
     * @return {@link HttpTransactionContext}, or <tt>null</tt> if connection is
     *          not established
     */
    HttpTransactionContext getHttpTransactionCtx() {
        return transactionCtx;
    }

    /**
     * @param transactionCtx
     * @return <tt>true</tt> if we can continue request/response processing,
     *          or <tt>false</tt> if future has been aborted
     */
    boolean setHttpTransactionCtx(
            final HttpTransactionContext transactionCtx) {
        this.transactionCtx = transactionCtx;
        return !delegate.isDone();
    }
}
