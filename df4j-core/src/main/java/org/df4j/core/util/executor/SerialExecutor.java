package org.df4j.core.util.executor;

import org.df4j.core.actor.Hactor;

import java.util.concurrent.Executor;

/**
 * works like a single-threaded Executor, but does not own a thread.
 */
public class SerialExecutor extends Hactor<Runnable> implements Executor {

    public SerialExecutor() {
        start();
    }

    public SerialExecutor(Executor executor) {
        setExecutor(executor);
        start();
    }

    @Override
    public void execute(Runnable command) {
        super.onNext(command);
    }

    @Override
    protected void runAction(Runnable arg) {
        arg.run();
    }
}
