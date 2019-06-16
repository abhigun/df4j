package org.df4j.nio2.net.echo;

import org.df4j.core.actor.ext.Hactor;
import org.df4j.core.asyncproc.ScalarResult;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.ClientConnection;
import org.junit.Assert;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/** receives sends and receives limited number of messages
 *
 */
class EchoClient extends Hactor<ByteBuffer> {
    protected static final Logger LOG = Logger.getLogger(EchoClient.class.getName());

    ScalarResult<Void> result = new ScalarResult<>();
    String message="hi there";
    ClientConnection clientConn;
    int count;

    @Override
    public void onError(Throwable ex) {
        result.onError(ex);
    }

    public EchoClient(SocketAddress addr, int count) throws IOException, InterruptedException {
        this.count = count;
        clientConn = new ClientConnection("Client", addr);
        clientConn.writer.output.subscribe(this);
        String message = this.message;
        ByteBuffer buf = Utils.toByteBuf(message);
        this.onNext(buf);
    }

    public void runAction(ByteBuffer b) {
        String m2 = Utils.fromByteBuf(b);
        LOG.info("client received message:"+m2);
        Assert.assertEquals(message, m2);
        count--;
        if (count==0) {
            LOG.info("client finished successfully");
            result.onSuccess(null);
            clientConn.close();
            stop();
            return;
        }
        this.onNext(b);
    }

}
