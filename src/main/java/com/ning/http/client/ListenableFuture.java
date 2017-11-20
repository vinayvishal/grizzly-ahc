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

/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ning.http.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Extended {@link Future}
 *
 * @param <V> Type of the value that will be returned.
 */
public interface ListenableFuture<V> extends Future<V> {

    /**
     * Terminate and if there is no exception, mark this Future as done and release the internal lock.
     *
     * @param callable
     */
    void done();

    /**
     * Abort the current processing, and propagate the {@link Throwable} to the {@link AsyncHandler} or {@link Future}
     *
     * @param t
     */
    void abort(Throwable t);

    /**
     * Touch the current instance to prevent external service to times out.
     */
    void touch();

    /**
     * <p>Adds a listener and executor to the ListenableFuture.
     * The listener will be {@linkplain java.util.concurrent.Executor#execute(Runnable) passed
     * to the executor} for execution when the {@code Future}'s computation is
     * {@linkplain Future#isDone() complete}.
     * <p/>
     * <p>There is no guaranteed ordering of execution of listeners, they may get
     * called in the order they were added and they may get called out of order,
     * but any listener added through this method is guaranteed to be called once
     * the computation is complete.
     *
     * @param listener the listener to run when the computation is complete.
     * @param exec     the executor to run the listener in.
     * @return this Future
     * @throws NullPointerException if the executor or listener was null.
     * @throws java.util.concurrent.RejectedExecutionException
     *                              if we tried to execute the listener
     *                              immediately but the executor rejected it.
     */
    ListenableFuture<V> addListener(Runnable listener, Executor exec);

    public class CompletedFailure<T> implements ListenableFuture<T>{

        private final ExecutionException e;

        public CompletedFailure(Throwable t) {
            e = new ExecutionException(t);
        }

        public CompletedFailure(String message, Throwable t) {
            e = new ExecutionException(message, t);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return true;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            throw e;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw e;
        }

        @Override
        public void done() {
        }

        @Override
        public void abort(Throwable t) {
        }

        @Override
        public void touch() {
        }

        @Override
        public ListenableFuture<T> addListener(Runnable listener, Executor exec) {
            exec.execute(listener);
            return this;
        }
    }
}
