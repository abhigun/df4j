/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;

import com.github.rfqu.df4j.core.CompletableFuture;

/**
 * For using on server side.
 * <pre><code>AsyncServerSocketChannel assc=...;
 * assc.bind(SocketAddress);
 * AsyncSocketChannel asc=assc.accept(); // non-blocking operation
 * asc.write(Request); // real I/O will start after actual client connection 
 * asc.read(Request);
 * </code></pre>
 */
public abstract class AsyncServerSocketChannel implements Closeable {
    protected SocketAddress addr;
    protected CompletableFuture<AsyncServerSocketChannel> closeEvent =
            new CompletableFuture<AsyncServerSocketChannel>();

    public abstract void bind(SocketAddress addr) throws IOException;

    public CompletableFuture<AsyncServerSocketChannel> getCloseEvent() {
        return closeEvent;
    }
    
    /**
     * creates new AsyncSocketChannel, and makes it to wait for incoming client connection request.
     * 
     * @return 
     */
    public abstract AsyncSocketChannel accept();

    public abstract void close();
}
