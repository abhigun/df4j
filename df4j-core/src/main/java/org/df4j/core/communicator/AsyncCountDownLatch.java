package org.df4j.core.communicator;

import org.df4j.protocol.Completion;
import org.df4j.protocol.Signal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  A {@link CountDownLatch} extended asynchronous interface to publish the completion signal.
 */
public class AsyncCountDownLatch extends CountDownLatch implements Signal.Publisher {
    private final Lock bblock = new ReentrantLock();
    protected Trigger completionSignal = new Trigger();

    public AsyncCountDownLatch(int count) {
        super(count);
        if (getCount() == 0) {
            completionSignal.onComplete();
        }
    }

    public boolean isCompleted() {
        return completionSignal.isCompleted();
    }

    @Override
    public void subscribe(Signal.Subscriber subscriber) {
        completionSignal.subscribe(subscriber);
    }

    @Override
    public boolean unsubscribe(Signal.Subscriber subscriber) {
        completionSignal.unsubscribe(subscriber);
        return false;
    }

    public void countDown() {
        if (getCount() == 0) {
            return;
        }
        bblock.lock();
        try {
            super.countDown();
            if (getCount() > 0) {
                return;
            }
        } finally {
            bblock.unlock();
        }
        completionSignal.onComplete();
    }

    public static AsyncCountDownLatch allOf(Signal.Publisher... sources) {
        AsyncCountDownLatch latch = new AsyncCountDownLatch(sources.length);
        for (int k = 0; k < sources.length; k++) {
            sources[k].subscribe(latch::countDown);
        }
        return latch;
    }

    public static AsyncCountDownLatch allOf(Completion.CompletableSource... sources) {
        AsyncCountDownLatch latch = new AsyncCountDownLatch(sources.length);
        Completion.CompletableObserver completableObserver = new Completion.CompletableObserver() {
            @Override
            public void onError(Throwable e) {
                latch.countDown();
            }
            @Override
            public void onComplete() {
                latch.countDown();
            }
        };
        for (int k = 0; k < sources.length; k++) {
            sources[k].subscribe(completableObserver);
        }
        return latch;
    }

}
