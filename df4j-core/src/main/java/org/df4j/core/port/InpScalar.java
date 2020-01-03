package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.reactivestreams.*;
import org.df4j.protocol.Scalar;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one message.
 * After the message is received, this port stays ready forever.
 *
 * It can connect both to {@link Scalar.Source} and {@link Publisher}.
 *
 * @param <T> type of accepted tokens.
 */
public class InpScalar<T> extends BasicBlock.Port implements Scalar.Observer<T> {//}, Subscriber<T> {
    protected T value;
    protected volatile boolean completed = false;
    private Throwable completionException = null;
    private Scalar.Subscription subscription;

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpScalar(BasicBlock parent) {
        parent.super(false);
    }

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onSubscribe(Scalar.Subscription subscription) {
        this.subscription = subscription;
    }

    public void unsubscribe() {
        plock.lock();
        Scalar.Subscription sub;
        try {
            if (subscription == null) {
                return;
            }
            sub = subscription;
            subscription = null;
        } finally {
            plock.unlock();
        }
        sub.cancel();
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    public T current() {
        plock.lock();
        try {
            return value;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public  void onSuccess(T message) {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            this.value = message;
            unblock();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public  void onError(Throwable throwable) {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            unblock();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onComplete() {
        onError(null);
    }

    //// Flow protocol //////
/*
    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(T t) {
        onSuccess(t);
        unsubscribe();
    }

    public void reset() {
        plock.lock();
        try {
            if (subscription != null) {
                subscription.cancel();
            }
            subscription = null;
            completionException = null;
            completed = false;
            value = null;
            block();
        } finally {
            plock.unlock();
        }
    }
    */

}
