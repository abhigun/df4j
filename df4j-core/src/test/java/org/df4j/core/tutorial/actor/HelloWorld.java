package org.df4j.core.tutorial.actor;

import org.df4j.core.actor.StreamInput;
import org.df4j.core.actor.ext.FancyActor;
import org.df4j.core.asyncproc.ext.Action;
import org.junit.Test;

public class HelloWorld {
    /**
     * collects strings
     * prints collected strings when argument is an empty string
     */
    class Collector extends FancyActor {
        StreamInput<String> input = new StreamInput<String>(this); // actor's parameter
        StringBuilder sb = new StringBuilder();

        @Action
        protected void act(String message) {
            // empty string indicates the end of stream
            // nulls are not allowed
            if (message == null) {
                System.out.println(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(message);
                sb.append(" ");
            }
        }
    }

    @Test
    public void test() {
        Collector coll = new Collector();
        coll.start();
        coll.input.onNext("Hello");
        coll.input.onNext("World");
        coll.input.onComplete();
    }

}
