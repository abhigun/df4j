package org.df4j.core.connector.reactivestream;

import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.node.AsyncTaskBase;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * A Queue of tokens of type <T>
 *
 * @param <T>
 */
public class ReactiveInput<T> extends StreamInput<T> implements Subscriber<T>, Iterator<T> {
    protected Deque<T> queue;
    protected boolean closeRequested = false;
    protected int capacity;
    protected Subscription subscription;

    public ReactiveInput(AsyncTaskBase actor, int capacity) {
        super(actor);
        this.queue = new ArrayDeque<>(capacity);
        this.capacity = capacity;
    }

    public ReactiveInput(AsyncTaskBase actor) {
        this(actor, 8);
    }

    protected int size() {
        return queue.size();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(capacity);
    }

    @Override
    public synchronized void post(T token) {
        if (subscription == null) {
            throw new IllegalStateException("not yet subscribed");
        }
        if (queue.size() >= capacity) {
            throw new IllegalStateException("no space for next token");
        }
        super.post(token);
    }

    @Override
    public synchronized T next() {
        subscription.request(1);
        return super.next();
    }

    public synchronized boolean  isClosed() {
        return closeRequested && (value == null);
    }
}
