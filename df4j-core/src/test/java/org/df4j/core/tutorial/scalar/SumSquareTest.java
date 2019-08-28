package org.df4j.core.tutorial.scalar;

import org.df4j.core.asyncproc.AsyncProc;
import org.df4j.core.asyncproc.ScalarResult;
import org.df4j.core.asyncproc.ScalarInput;
import org.df4j.core.asyncproc.AsyncBiFunction;
import org.df4j.core.asyncproc.AsyncFunction;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SumSquareTest {

    public static class Square extends AsyncProc<Integer> {
        final ScalarInput<Integer> param = new ScalarInput<>(this);

        public void run() {
            Integer arg = param.current();
            int res = arg*arg;
            result.onSuccess(res);
        }
    }

    public static class Sum extends AsyncProc<Integer> {
        final ScalarInput<Integer> paramX = new ScalarInput<>(this);
        final ScalarInput<Integer> paramY = new ScalarInput<>(this);

        public void run() {
            Integer argX = paramX.current();
            Integer argY = paramY.current();
            int res = argX + argY;
            result.onSuccess(res);
        }
    }

    /**
     * computes arithmetic expression sum = 3*3 + 4*4
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Test
    public void testAP() throws ExecutionException, InterruptedException, TimeoutException {
        // create 3 nodes
        Square sqX = new Square();
        Square sqY = new Square();
        Sum sum = new Sum();
        // make 2 connections
        sqX.asyncResult().subscribe(sum.paramX);
        sqY.asyncResult().subscribe(sum.paramY);
        // provide input information:
        sqX.param.onSuccess(3);
        sqY.param.onSuccess(4);
        // get the result
        int res = sum.asyncResult().get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }

    /**
     * computes arithmetic expression sum = 3*3 + 4*4
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Test
    public void testDFF() throws ExecutionException, InterruptedException, TimeoutException {
        Function<Integer, Integer> square = arg -> arg * arg;
        BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
        // create nodes and connect them
        AsyncFunction<Integer, Integer> sqX = new AsyncFunction<>(square);
        AsyncFunction<Integer, Integer> sqY = new AsyncFunction<>(square);
        AsyncBiFunction<Integer, Integer, Integer> sum = new AsyncBiFunction<Integer, Integer, Integer>(plus);
        // make 2 connections
        sqX.subscribe(sum.param1);
        sqY.subscribe(sum.param2);
        // provide input information:
        sqX.onSuccess(3);
        sqY.onSuccess(4);
        // get the result
        ScalarResult<Integer> result = sum.asyncResult();
        int res = result.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }

    @Test
    public void testCF() throws ExecutionException, InterruptedException, TimeoutException {
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
        int res = sum.get(1, TimeUnit.SECONDS);
        Assert.assertEquals(25, res);
    }

}
