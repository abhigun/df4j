package org.df4j.core.communicator;

import org.df4j.protocol.Completable;
import org.df4j.protocol.ScalarSubscription;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Completes successfully or with failure, without emitting any value.
 * Similar to {@link CompletableFuture}&lt;Void&gt;
 */
public class Completion implements Completable.Source {
    protected final Lock bblock = new ReentrantLock();
    private final Condition completedCond = bblock.newCondition();
    protected Throwable completionException;
    protected LinkedList<Subscription> subscriptions;
    protected boolean completed;

    /**
     * @return completion Exception, if this {@link Completable} was completed exceptionally;
     *         null otherwise
     */
    public Throwable getCompletionException() {
        bblock.lock();
        try {
            return completionException;
        } finally {
            bblock.unlock();
        }
    }

    /**
     * @return true if this {@link Completable} was completed normally or exceptionally;
     *         false otherwise
     */
    public boolean isCompleted() {
        bblock.lock();
        try {
            return completed;
        } finally {
            bblock.unlock();
        }
    }

    public void subscribe(Completable.Observer co) {
        bblock.lock();
        try {
            if (!completed) {
                Subscription subscription = new Subscription(co);
                subscriptions.add(subscription);
                co.onSubscribe(subscription);
                return;
            }
        } finally {
            bblock.unlock();
        }
        if (getCompletionException() == null) {
            co.onComplete();
        } else {
            Throwable completionException = getCompletionException();
            co.onError(completionException);
        }
    }

    /**
     * completes this {@link Completable} exceptionally
     * @param e completion exception
     */
    protected void onError(Throwable e) {
        LinkedList<Subscription> subs;
        bblock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            this.completionException = e;
            completedCond.signalAll();
            if (subscriptions == null) {
                return;
            }
            subs = subscriptions;
            subscriptions = null;
        } finally {
            bblock.unlock();
        }
        for (;;) {
            Subscription sub = subs.poll();
            if (sub == null) {
                break;
            }
            if (e == null) {
                sub.onError(e);
            } else {
                sub.onComplete();
            }
        }
    }

    /**
     * completes this {@link Completable} normally
     */
    public void onComplete() {
        onError(null);
    }

    /**
     * waits this {@link Completable} to complete
     */
    public void join() {
        bblock.lock();
        try {
            while (!completed) {
                try {
                    completedCond.await();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
            }
        } finally {
            bblock.unlock();
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
    }

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeout timeout in millisecomds
     * @return true if completed;
     *         false if timout reached
     */
    public boolean blockingAwait(long timeout) {
        boolean result;
        bblock.lock();
        try {
            long targetTime = System.currentTimeMillis()+ timeout;
            for (;;) {
                if (completed) {
                    result = true;
                    break;
                }
                if (timeout <= 0) {
                    result = false;
                    break;
                }
                try {
                    completedCond.await(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
                timeout = targetTime - System.currentTimeMillis();
            }
        } finally {
            bblock.unlock();
        }
        if (!result) {
            return false;
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
        return true;
    }

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeout timeout in units
     * @param unit time unit
     * @return true if completed;
     *         false if timout reached
     */
    public boolean blockingAwait(long timeout, TimeUnit unit) {
        bblock.lock();
        try {
            boolean result = blockingAwait(unit.toMillis(timeout));
            if (!result) {
                return false;
            }
            if (completionException != null) {
                throw new CompletionException(completionException);
            }
            return true;
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        LinkedList<Subscription> subscribers = this.subscriptions;
        Throwable completionException = this.completionException;
        int size = 0;
        if (subscribers!=null) {
            size=subscribers.size();
        }
        if (!completed) {
            sb.append("not completed; subscribers: "+size);
        } else if (completionException == null) {
            sb.append("completed successfully");
        } else {
            sb.append("completed with exception: ");
            sb.append(completionException.toString());
        }
        return sb.toString();
    }


    class Subscription implements ScalarSubscription {
        final Completable.Observer subscriber;
        private boolean cancelled;

        public Subscription(Completable.Observer subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void cancel() {
            bblock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                subscriptions.remove(this);
            } finally {
                bblock.unlock();
            }
        }

        @Override
        public boolean isCancelled() {
            bblock.lock();
            try {
                return cancelled;
            } finally {
                bblock.unlock();
            }
        }

        public void onComplete() {
            subscriber.onComplete();
        }

        public void onError(Throwable e) {
            subscriber.onError(e);
        }
    }

}
