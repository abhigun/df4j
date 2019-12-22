package org.df4j.core.activities;

import org.df4j.core.dataflow.ActivityThread;
import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.util.Utils;

public class ProducerT extends Thread implements ActivityThread {
    final int delay;
    int cnt;
    AsyncArrayBlockingQueue<Integer> queue;

    public ProducerT(int cnt, AsyncArrayBlockingQueue<Integer> queue, int delay) {
        this.queue = queue;
        this.delay = delay;
        this.cnt = cnt;
    }

    @Override
    public void run() {
        System.out.println("ProducerT started");
        for (;;) {
            System.out.println("cnt: "+cnt);
            if (cnt == 0) {
                queue.onComplete();
                return;
            }
            try {
                queue.put(cnt);
                cnt--;
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Utils.sneakyThrow(e);
            }
        }
    }
}
