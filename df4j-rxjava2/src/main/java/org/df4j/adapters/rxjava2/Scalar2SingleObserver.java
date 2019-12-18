package org.df4j.adapters.rxjava2;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.df4j.core.protocol.ScalarMessage;

public class Scalar2SingleObserver<T> implements SingleObserver<T>, org.df4j.core.protocol.Disposable {
    private final ScalarMessage.Subscriber<T> scalar;
    private Disposable subscription;

    public Scalar2SingleObserver(ScalarMessage.Subscriber<T> scalar) {
        this.scalar = scalar;
    }

    @Override
    public void onSubscribe(io.reactivex.disposables.Disposable subscription) {
        this.subscription = subscription;
        scalar.onSubscribe(this);
    }

    @Override
    public void onSuccess(T o) {
        scalar.onSuccess(o);
    }

    @Override
    public void onError(Throwable e) {
        scalar.onError(e);
    }


    @Override
    public void dispose() {
        subscription.dispose();
    }

    @Override
    public boolean isDisposed() {
        return subscription.isDisposed();
    }
}
