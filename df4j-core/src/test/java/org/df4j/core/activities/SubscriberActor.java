package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.protocol.Flow;
import org.junit.Assert;

public class SubscriberActor extends Actor {
    Flow.Publisher<Integer> pub;
    final int delay;
    public InpFlow<Integer> inp = new InpFlow<>(this);
    Integer in = null;

    public SubscriberActor(Dataflow parent, int delay) {
        this.pub = pub;
        this.delay = delay;
    }

    public SubscriberActor(Flow.Publisher<Integer> pub, int delay) {
        this.pub = pub;
        this.delay = delay;
        pub.subscribe(inp);
    }

    @Override
    protected void runAction() throws Throwable {
        Thread.sleep(delay);
        if (inp.isCompleted()) {
            Throwable completionException = inp.getCompletionException();
            System.out.println(" completed with: " + completionException);
            stop(completionException);
            return;
        }
        Integer in = inp.remove();
        System.out.println(" got: " + in);
        if (this.in != null) {
            Assert.assertEquals(this.in.intValue() - 1, in.intValue());
        }
        this.in = in;
    }
}
