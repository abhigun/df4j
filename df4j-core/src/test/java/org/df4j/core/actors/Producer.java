package org.df4j.core.actors;

import org.df4j.core.actor.Actor;
import org.df4j.core.port.OutChannel;
import org.df4j.core.protocol.ReverseFlow;

public class Producer extends Actor {
    final int delay;
    int cnt;
    OutChannel<Integer> out;

    public Producer(int cnt, ReverseFlow.Publisher<Integer> inp, int delay) {
        out = new OutChannel<>(this, inp);
        this.delay = delay;
        this.cnt = cnt;
    }

    @Override
    protected void runAction() throws Throwable {
        System.out.println("cnt: "+cnt);
        if (cnt == 0) {
            out.onComplete();
            stop();
        } else {
            out.onNext(cnt);
            cnt--;
            Thread.sleep(delay);
        }
    }
}
