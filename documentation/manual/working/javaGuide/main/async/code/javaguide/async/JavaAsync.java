/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async;

import org.junit.Test;
import play.mvc.Result;

import java.time.Duration;
import java.util.concurrent.*;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static play.mvc.Results.ok;

public class JavaAsync {

    @Test
    public void promiseWithTimeout() throws Exception {
        //#timeout
        class MyClass implements play.libs.concurrent.Timeout {
            CompletionStage<Double> callWithOneSecondTimeout() {
                return timeout(computePIAsynchronously(), Duration.ofSeconds(1));
            }
        }
        //#timeout
        final Double actual = new MyClass().callWithOneSecondTimeout().toCompletableFuture().get(1, TimeUnit.SECONDS);
        final Double expected = Math.PI;
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void promisePi() throws Exception {
        //#promise-pi
        CompletionStage<Double> promiseOfPIValue = computePIAsynchronously();
        // Runs in same thread
        CompletionStage<Result> promiseOfResult = promiseOfPIValue.thenApply(pi ->
                        ok("PI value computed: " + pi)
        );
        //#promise-pi
        assertThat(promiseOfResult.toCompletableFuture().get(1, TimeUnit.SECONDS).status(), equalTo(200));
    }

    @Test
    public void promiseAsync() throws Exception {
        //#promise-async
        // import static java.util.concurrent.CompletableFuture.supplyAsync;
        // creates new task
        CompletionStage<Integer> promiseOfInt = supplyAsync(() ->
                intensiveComputation());
        //#promise-async
        assertEquals(intensiveComputation(), promiseOfInt.toCompletableFuture().get(1, TimeUnit.SECONDS));
    }

    private static CompletionStage<Double> computePIAsynchronously() {
        return CompletableFuture.completedFuture(Math.PI);
    }

    private static Integer intensiveComputation() {
        return 1 + 1;
    }

}
