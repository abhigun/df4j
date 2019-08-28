/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.asyncproc;

import org.df4j.core.asyncproc.base.Transition;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * AsyncProc is an Asynchronous Procedure.
 *
 * It consists of asynchronous connectors, implemented as inner classes,
 * user-defined asynchronous procedure, and a mechanism to call that procedure
 * using supplied {@link Executor} as soon as all connectors are unblocked.
 *
 * This class contains predefined components, likely useful in each async task node:
 *  - reference to an executor
 *  - scalar result. Even if this action will produce a stream of results or no result at all,
 *  it can be used as a place for unexpected errors.
 */
public abstract class AsyncProc<R> extends Transition {
    public static final Executor syncExec = (Runnable r)->r.run();

    public static final ForkJoinPool asyncExec = ForkJoinPool.commonPool();

    public static final Executor newThreadExec = (Runnable r)->new Thread(r).start();

    private static InheritableThreadLocal<Executor> threadLocalExecutor = new InheritableThreadLocal<Executor>(){
        @Override
        protected Executor initialValue() {
            Thread currentThread = Thread.currentThread();
            if (currentThread instanceof ForkJoinWorkerThread) {
                return ((ForkJoinWorkerThread) currentThread).getPool();
            } else {
                return asyncExec;
            }
        }
    };

    /**
     * for debug purposes, call
     * <pre>
     *    setThreadLocalExecutor(AsyncProc.currentThreadExec);
     * </pre>
     * before creating {@link Transition} instances.
     *
     * @return an Executor ;ocal to current thread
     */
    public static Executor getThreadLocalExecutor() {
        return threadLocalExecutor.get();
    }

    public static void setThreadLocalExecutor(Executor exec) {
        threadLocalExecutor.set(exec);
    }

    private Executor executor;

    protected final ScalarResult<R> result = new ScalarResult<>(this);

    public <A extends AsyncProc<R>> A setExecutor(Executor exec) {
        this.executor = exec;
        return (A) this;
    }

    public ScalarResult<R> asyncResult() {
        return result;
    }

    /**
     * invoked when all asyncTask asyncTask are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        synchronized (this) {
            if (executor == null) {
                executor = threadLocalExecutor.get();
            }
        }
        executor.execute(this::run);
    }

    protected abstract void run();


    /**
     * for debugging
     * Serial executor working on current thread
     * <code>
     *   {@link CurrentThreadExecutor executor} = new {@link CurrentThreadExecutor ()};
     *   actor.setExecutor(executor);
     *   actor.start();
     *   executor.runAll();
     * </code>
     */
    public static class CurrentThreadExecutor implements Executor {
        ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

        @Override
        public void execute(Runnable command) {
            queue.add(command);
        }

        void runAll(){
            for (;;) {
                Runnable command = queue.poll();
                if (command == null) {
                    return;
                }
                command.run();
            }
        }
    }

}