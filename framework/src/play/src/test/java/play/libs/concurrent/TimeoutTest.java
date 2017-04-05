/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TimeoutTest {

    @Test
    public void successfulTimeout() throws Exception {
        class MyClass implements play.libs.concurrent.Timeout {
            CompletionStage<Double> callWithTimeout() {
                return timeout(computePIAsynchronously(), Duration.ofSeconds(1));
            }
        }
        final Double actual = new MyClass().callWithTimeout().toCompletableFuture().get(1, TimeUnit.SECONDS);
        final Double expected = Math.PI;
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void failedTimeout() throws Exception {
        class MyClass implements play.libs.concurrent.Timeout {
            CompletionStage<Double> callWithTimeout() {
                return timeout(delayedFuture(), Duration.ofMillis(300));
            }
        }
        final Double actual = new MyClass()
                .callWithTimeout()
                .toCompletableFuture()
                .exceptionally(e -> 100d)
                .get(1, TimeUnit.SECONDS);
        final Double expected = 100d;
        assertThat(actual, equalTo(expected));
    }

    private static CompletionStage<Double> computePIAsynchronously() {
        return CompletableFuture.completedFuture(Math.PI);
    }


    private static CompletionStage<Double> delayedFuture() {
        return Futures.delayed(() -> Math.PI, 1, TimeUnit.SECONDS, ForkJoinPool.commonPool());
    }

    private static Integer intensiveComputation() {
        return 1 + 1;
    }
}
