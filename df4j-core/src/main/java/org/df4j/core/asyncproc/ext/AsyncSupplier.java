package org.df4j.core.asyncproc.ext;

import org.df4j.core.asyncproc.ScalarPublisher;
import org.df4j.core.asyncproc.ScalarSubscriber;
import org.df4j.core.util.invoker.Invoker;
import org.df4j.core.util.invoker.RunnableInvoker;
import org.df4j.core.util.invoker.SupplierInvoker;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Base class for scalar nodes
 * Has predefined unbound output connector to keep the result of computation.
 *
 * Even if the computation does not produce a resulting value,
 * that connector is useful to monitor the end of the computation.
 *
 * @param <R> type of the result
 */
public class AsyncSupplier<R> extends AsyncAction<R> implements ScalarPublisher<R>, Future<R> {

    public AsyncSupplier() {}

    public AsyncSupplier(Invoker invoker) {
        super(invoker);
    }

    public AsyncSupplier(Supplier<R> proc) {
        super(new SupplierInvoker<>(proc));
    }

    public AsyncSupplier(Runnable proc) {
        super(new RunnableInvoker<R>(proc));
    }

    @Override
    public void subscribe(ScalarSubscriber<? super R> subscriber) {
        asyncResult().subscribe(subscriber);
    }

    protected void completeResult(R res) {
        asyncResult().onComplete(res);
    }

    protected void completeResultExceptionally(Throwable ex) {
        result.onError(ex);
    }

    @Override
    public boolean cancel(boolean b) {
        return result.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return result.isCancelled();
    }

    @Override
    public boolean isDone() {
        return result.isDone();
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        return asyncResult().get();
    }

    @Override
    public R get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return asyncResult().get(l, timeUnit);
    }

    @Override
    protected void run() {
        try {
            R res = callAction();
            result.onComplete(res);
        } catch (Throwable e) {
            result.onError(e);
        }
    }
}