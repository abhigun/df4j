package org.df4j.core.dataflow;

import org.df4j.core.communicator.Completable;
import org.df4j.core.util.Utils;
import org.df4j.protocol.Completion;

import java.util.Timer;
import java.util.concurrent.Executor;

/**
 * A dataflow graph, consisting of 1 or more {@link BasicBlock}s and, probably, nested {@link Dataflow}s.
 * Completion signals (errors or success) propagate from the leaf nodes to the root node.
 * Component {@link BasicBlock}s plays the same role as basic blocks in a flow chart.
 */
public class Dataflow extends Completable implements Activity, Completion.CompletableSource {
    protected Dataflow parent;
    protected Executor executor;
    protected Timer timer;
    protected int nodeCount = 0;

    /**
     *  creates root {@link Dataflow} graph.
     */
    public Dataflow() {
    }

    /**
     *  creates nested {@link Dataflow} graph.
     * @param parent the parent {@link Dataflow}
     */
    public Dataflow(Dataflow parent) {
        this.parent = parent;
        parent.enter();
    }

    public void setExecutor(Executor executor) {
        bblock.lock();
        try {
            this.executor = executor;
        } finally {
            bblock.unlock();
        }
    }

    protected Executor getExecutor() {
        bblock.lock();
        try {
            if (executor != null) {
                return executor;
            } else if (parent != null) {
                return parent.getExecutor();
            } else {
                return executor = Utils.getThreadLocalExecutor();
            }
        } finally {
            bblock.unlock();
        }
    }

    public void setTimer(Timer timer) {
        bblock.lock();
        try {
            this.timer = timer;
        } finally {
            bblock.unlock();
        }
    }

    public Timer getTimer() {
        bblock.lock();
        try {
            if (timer != null) {
                return timer;
            } else if (parent != null) {
                return timer = parent.getTimer();
            } else {
                timer = new Timer();
                return timer;
            }
        } finally {
            bblock.unlock();
        }
    }

    /**
     * indicates that a node has added to this graph.
     */
    public void enter() {
        bblock.lock();
        try {
            nodeCount++;
        } finally {
            bblock.unlock();
        }
    }

    /**
     * indicates that a node has left this graph because of successful completion.
     * when all the nodes has left this graph, it is considered successfully completed itself
     * and leaves the pareng graph, if any.
     */
    public void leave() {
        bblock.lock();
        try {
            if (nodeCount==0) {
                throw new IllegalStateException();
            }
            nodeCount--;
            if (nodeCount==0) {
                super.onComplete();
                if (parent != null) {
                    parent.leave();
                }
            }
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAlive() {
        return !super.isCompleted();
    }

    public void onError(Throwable t) {
        super.onError(t);
        if (parent != null) {
            parent.onError(t);
        }
    }

    @Override
    public void join() {
        super.blockingAwait();
    }
}
