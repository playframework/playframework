/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;

import akka.actor.ActorSystem;
import org.junit.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class FuturesTest {

    private ActorSystem system;
    private Futures futures;

    @Before
    public void setup() {
        system = ActorSystem.create();
        futures = new DefaultFutures(new play.api.libs.concurrent.DefaultFutures(system));
    }

    @After
    public void teardown() {
        system.terminate();
        futures = null;
    }

    @Test
    public void successfulTimeout() throws Exception {
        class MyClass {
            CompletionStage<Double> callWithTimeout() {
                return futures.timeout(computePIAsynchronously(), Duration.ofSeconds(1));
            }
        }
        final Double actual = new MyClass().callWithTimeout().toCompletableFuture().get(1, TimeUnit.SECONDS);
        final Double expected = Math.PI;
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void failedTimeout() throws Exception {
        class MyClass {
            CompletionStage<Double> callWithTimeout() {
                return futures.timeout(delayedFuture(), Duration.ofMillis(300));
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

    private CompletionStage<Double> computePIAsynchronously() {
        return CompletableFuture.completedFuture(Math.PI);
    }

    private CompletionStage<Double> delayedFuture() {
        return futures.delayed(CompletableFuture.supplyAsync(() -> Math.PI, ForkJoinPool.commonPool()), 1, TimeUnit.SECONDS);
    }

}
