/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async;

import org.junit.Test;
import play.mvc.Result;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static play.mvc.Results.ok;

public class JavaAsync {

    @Test
    public void promisePi() throws Exception {
        //#promise-pi
        CompletionStage<Double> promiseOfPIValue = computePIAsynchronously();
        CompletionStage<Result> promiseOfResult = promiseOfPIValue.thenApply(pi ->
                        ok("PI value computed: " + pi)
        );
        //#promise-pi
        assertThat(promiseOfResult.toCompletableFuture().get(1, TimeUnit.SECONDS).status(), equalTo(200));
    }

    @Test
    public void promiseAsync() throws Exception {
        //#promise-async
        CompletionStage<Integer> promiseOfInt = CompletableFuture.supplyAsync(() -> intensiveComputation());
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
