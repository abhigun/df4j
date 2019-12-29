package org.df4j.core.dataflow;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.activities.SubscriberActor;
import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.junit.Assert;
import org.junit.Test;

public class PubSubTest {

    public void pubSubTest(int cnt, int delay1, int delay2) throws InterruptedException {
        PublisherActor pub = new PublisherActor(cnt, delay1);
        SubscriberActor sub = new SubscriberActor(delay2);
        pub.out.subscribe(sub.inp);
        pub.start();
        sub.start();
        sub.blockingAwait(1000);
        pub.blockingAwait(1000);
        Assert.assertFalse(sub.isAlive());
        Assert.assertFalse(pub.isAlive());
    }

    @Test
    public void slowPubConsTest() throws InterruptedException {
        pubSubTest(0, 100, 0);
    }

    @Test
    public void slowPubTest() throws InterruptedException {
        pubSubTest(3, 100, 0);
    }

    @Test
    public void pubSlowSubConnTest() throws InterruptedException {
        pubSubTest(0, 0, 100);
    }

    @Test
    public void pubSlowSubTest() throws InterruptedException {
        pubSubTest(4, 0, 100);
    }
}

