package org.df4j.core.dataflow.actors;

import org.df4j.core.actor.Actor;
import org.df4j.core.port.InpMessage;
import org.df4j.core.protocol.MessageStream;
import org.junit.Assert;

public class Subscriber extends Actor {
    MessageStream.Publisher<Integer> pub;
    final int delay;
    InpMessage<Integer> inp = new InpMessage<>(this);
    Integer in = null;

    public Subscriber(MessageStream.Publisher<Integer> pub, int delay) {
        this.pub = pub;
        this.delay = delay;
        inp.subscribeTo(pub);
    }

    @Override
    protected void runAction() throws Throwable {
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
        Thread.sleep(delay);
    }
}
