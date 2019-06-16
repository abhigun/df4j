# Data Flow For Java Essentials

 Everything should be made as simple as possible, but not simpler. - Albert Einstein

### The Anatomy of Asynchronous procedure.
------------------------------------------
An asynchronous procedure is a kind of parallel activity, along with a Thread.
It differs from a thread so that while waiting for input information to be delivered, it does not use procedure stack and so does not wastes core memory.
As a result, we can manage millions of asynchronous procedures,
while 10000 threads is already a heavy load. This can be important, for example, when constructing a web-server.

An asynchronous procedure differs from ordinary synchronous procedure in the following way:
 - it resides in the heap and not on a thread's stack
 - its input and output parameters are not simple variables but separate objects in the heap
 
Parameter objects are tightly connected to the asynchronous procedure object.
Parameters cooperate to detect the moment when all the parameters are ready, and then submit the procedure to athread pool.

Input parameters are connected to output parameters of other procedures. As a result, the whole program is represented as a (dataflow) graph.
Each parameter implements some connecting protocol. Connected parameters must belong to the same protocol. A protocol defines:

- what kind of tokens are transferred through the connection: pure events or events with values. In terms of Petri Nets, these are black and colored tokens, respectively. 
- how many tokens can be transferred through the liftime of the connection: one or many
- is backpressure supported

### The two Realms of Asynchronous Programming
### Scalar realm

The Scalar realm contains one-shot events and asynchronous procedure calls to handle them. In Java SE, it is represented by CompletableFuture class.
Df4j has alternative implementation, interoperable with CompletableFuture - this the class org.df4j.core.asyncproc.AsyncProc.
To create typical one-shot asynchronous procedure in Df4j, user has to extend class org.df4j.core.asyncproc.AsyncProc, declare one or more input parameters,
and override the the computational method `run()`.
The class AsyncProc declares default output parameter named "result", but additional output parameters cam be declared.

For example, let's create an async computation to compute value of x^2+y^2, each arithmetic operation computed in its own asynchronous procedure call.

First, we need to create 2 classes, one to compute the square of a value, and second to compute the sum.

```java
    public static class Square extends AsyncProc<Integer> {
        final ScalarInput<Integer> param = new ScalarInput<>(this);

        public void run() {
            Integer arg = param.current();
            int res = arg*arg;
            result.onComplete(res);
        }
    }
```
Note the constructor of a bound parameter has additional parameter - a reference to the parent node (this).

```java
    public static class Sum extends AsyncProc<Integer> {
        final ScalarInput<Integer> paramX = new ScalarInput<>(this);
        final ScalarInput<Integer> paramY = new ScalarInput<>(this);

        public void run() {
            Integer argX = paramX.current();
            Integer argY = paramY.current();
            int res = argX + argY;
            result.onComplete(res);
        }
    }
```

Now we can create the dataflow graph consisting of 3 nodes, pass arguments to it and get the result:

```java
public class SumSquareTest {

    @Test
    public void testAP() throws ExecutionException {
        // create nodes
        Square sqX = new Square();
        Square sqY = new Square();
        Sum sum = new Sum();
        // make 2 connections
        sqX.result.subscribe(sum.paramX);
        sqY.result.subscribe(sum.paramY);
        // provide input information:
        sqX.param.onComplete(3);
        sqY.param.onComplete(4);
        // get the result
        int res = sum.asyncResult().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }
}
```
All the nodes in this graph execute once and cannot be reused.

### Actor realm
To handle streams of values efficiently, we need reusable nodes.
The theory of asynchronous programming knows two kinds of reusable nodes: `Actors` and `Coroutines`.
Df4J has actors only. because coroutines require compiler support or bytecode transformations.

The actor realm in df4j is located at the package org.df4j.core.actor. The base node class is org.df4j.core.actor.Actor.
It extends AsyncProc and so has the default result and can contain one-shot parameters (which can be set only once).

`Actors` here are [dataflow actors whith arbitrary number of parameters] (https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf).
After processing the first set of arguments, the actor purges them out of parameters and waits until next set of arguments is ready.
The [Hewitt's actors](https://en.wikipedia.org/wiki/Actor_model) (e.g. [Akka](https://akka.io/)) with single predifined input parameter
are trivial corner case of the dataflow actors. They are implemented in class class is org.df4j.core.actor.ext.Hactor.

Typical df4j actor is programmed as follows:
So the main difference is parameters which can keep a sequence of values. The node classes differ only that after calling the action procedure,
the method `AsyncTask.start()` is called again.



An interesting case is calling `start()` in an asynchronous callback like in
 [AsyncServerSocketChannel](../df4j-nio2/src/main/java/org/df4j/nio2/net/AsyncServerSocketChannel.java).

To effectively use Actors, they should declare stream parameters.

For example. let's construct an Actor which computes the module (size) of vectors represented by their coordinates x and y:

The dataflow graph can have cycles. The connections through the cycles must allow transfer of multiple tokens.

For simplicity, only two kinds of connection interfaces are used: for black and for colored tokens.
Implementations which allow single tokens and that which allow streams of tokens, use the same interfaces. Intrefaces for colored tokens are borrowed from
the Project Reactor (https://projectreactor.io/).   

In practice most async libraries tries to oversymplify and do not allow to create parameters separately from the body of async procedure. The result is overcomplicated API, which simultanousely is limited in expressiveness.


 The library also has a node class `AsyncFunc` which is an `Asynctask` with predefined output connector `result`.
 Using it, the code can be more compact, but we want to demonstrate the general plan to build asynchronous executions:
 
 - create nodes
 - connect output connectors with input connectors
 - start nodes
 - provide input information
 - take computed result(s)
 
Actually, the order of these steps can vary.

We can avoid creating new node classes, if our computation procedures are of standard java functional types like `Function`, 
`BiFunction` etc:

```java
    @Test
    public void testDFF() throws ExecutionException, InterruptedException {
        Function<Integer, Integer> square = arg -> arg * arg;
        BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
        // create nodes and connect them
        AsyncFunction<Integer, Integer> sqX = new AsyncFunction<>(square);
        AsyncFunction<Integer, Integer> sqY = new AsyncFunction<>(square);
        AsyncBiFunction<Integer, Integer, Integer> sum =
                     new AsyncBiFunction<Integer, Integer, Integer>(plus);
        // make 2 connections
        sqX.subscribe(sum.param1);
        sqY.subscribe(sum.param2);
        // start all the nodes
        sqX.start();
        sqY.start();
        sum.start();
        // provide input information:
        sqX.post(3);
        sqY.post(4);
        // get the result
        int res = sum.asyncResult().get();
        Assert.assertEquals(25, res);
```
The same computation can be build using `java.util.concurrent.CompletableFuture`:

```java
    @Test
    public void testCF() throws ExecutionException, InterruptedException {
        Function<Integer, Integer> square = arg -> arg * arg;
        BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
        // create nodes and connect them
        CompletableFuture<Integer> sqXParam = new CompletableFuture();
        CompletableFuture<Integer> sqYParam = new CompletableFuture();
        CompletableFuture<Integer> sum = sqXParam
                .thenApply(square)
                .thenCombine(sqYParam.thenApply(square),
                        plus);
        // provide input information:
        sqXParam.complete(3);
        sqYParam.complete(4);
        // get the result
        int res = sum.get();
        Assert.assertEquals(25, res);
    }
```

However, the symmetry beween computation of `x^2` and `x^2` is lost.
Worst of all, nodes with more than 2 input connectors or with more than 1 output connector cannot be created at all!
This is the clear result of using fluent API instead of explicit object construction.

The class [_CompletablePromise_](src/main/java/org/df4j/core/boundconnector/messagescalar/CompletablePromise.java) also provides
fluent API identical to that of _CompletableFuture_, only to demonstrate how a developer can live without it, or create his own.
Detailed explanation of that fluent API is in document [UnderTheHood](/UnderTheHood.md) (in Russian).
 

----------------------------------------------------

### Supported protocols.
-------------------
In the above example which compute expression `x^2+x^2` all connectors used the **scalar message** protocol, 
which consists of 2 steps:
- connection established: `publisher.subscribe(subscriber); subscriber.onSubscribe(subscription);`
- a value is passed and the connection is closed: `subscriber.post(value);`.

Connectors for this interface are located in the  package [connector/messagescalar](src/main/java/org/df4j/core/boundconnector/messagescalar). 
Nodes that support only scalar connectors are located in the  package [node/messagescalar](src/main/java/org/df4j/core/node/messagescalar). 
The subsequent subscriptions of the same or other subscribers can receive the same or different values. 
In the above example, all subscribers receive the same value, and this is natural, 
because the value is the result of concrete calculation. 
But publishers which provide different values for different connections can easily be implemented. 
One of such publishers is  [PickPoint](src/main/java/org/df4j/core/node/messagestream/PickPoint.java). 
It receives stream of messages and delivers each message to single subscriber.
It is asynchronous analogue of `java.util.concurrent.BlockingQueue`.
It even implements the `BlockingQueue` interface, and so it can connect both threads and asynchronous procedures in all combinations.
On the input side it uses **message stream** protocol, which consists of 3 steps:

- connection established: `publisher.subscribe(subscriber); subscriber.onSubscribe(subscription);`
- arbitrary number of values is passed to the same subscriber: `subscriber.post(value);`.
- the connection is closed by explicit request from publisher side: `subscriber.complete(value);`,
 or from the subscriber's side: 'subscription.cancel();'. 

Connectors for this interface are located in the  package [connector/messagestream](src/main/java/org/df4j/core/boundconnector/messagestream). 
Nodes that support both scalar and stream connectors are located in the 
package [node/messagestream](src/main/java/org/df4j/core/node/messagestream). 

The protocol **permit stream** is the same as **message stream** protocol,
 but transmited tokens does not carry any value and are indistinguishable.
In the synchronous world, the connector for this protocol is `java.util.concurrent.Semaphore`. 
Its asynchronous counterpart, [Semafor](src/main/java/org/df4j/core/boundconnector/permitstream/Semafor.java),
is a bound asynchronous connector and, as any other bound connector (parameter),
prevents the submission the bounded asynchronous procedure to the executor until its internal counter become positive. 
After the submission to the executor, the counter is automatically decreased by 1.

The last protocol, implemented currently in the **df4j** library is **reactive stream**.
Basically, its interfaces are roughly equivalent to the interfaces declared in the class `java.util.concurrent.Flow` in Java9.
What is interesting of this protocol, is that its connectors are implemented as a pair of connectors of lower level protocols:
 **permit stream** and **message stream**, and **permit stream** connectors are configured to work in opposite direction than **message stream**.
