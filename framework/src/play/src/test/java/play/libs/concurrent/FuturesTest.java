/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.concurrent;

import akka.actor.ActorSystem;
import org.junit.*;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.text.MessageFormat.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
                return futures.timeout(delayByOneSecond(), Duration.ofMillis(300));
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

    @Test
    public void successfulDelayed() throws Exception {
        Duration expected = Duration.ofSeconds(3);
        final CompletionStage<Long> stage = renderAfter(expected);

        Duration actual = Duration.ofMillis(stage.toCompletableFuture().get());
        assertTrue( format("Expected duration {0} is smaller than actual duration {1}!", expected, actual), actual.compareTo(expected) > 0);
    }

    @Test
    public void failedDelayed() throws Exception {
        Duration expected = Duration.ofSeconds(3);
        final CompletionStage<Long> stage = renderAfter(Duration.ofSeconds(1));

        Duration actual = Duration.ofMillis(stage.toCompletableFuture().get());
        assertTrue(format("Expected duration {0} is larger from actual duration {1}!", expected, actual), actual.compareTo(expected) < 0);
    }

    @Test
    public void testDelay() throws Exception{
        Duration expected = Duration.ofSeconds(2);
        long start = System.currentTimeMillis();
        CompletionStage<Long> stage = futures.delay(expected).thenApply((v) -> {
            long end = System.currentTimeMillis();
            return (end - start);
        });

        Duration actual = Duration.ofMillis(stage.toCompletableFuture().get());
        assertTrue( format("Expected duration {0} is smaller than actual duration {1}!", expected, actual), actual.compareTo(expected) > 0);
    }

    private CompletionStage<Double> computePIAsynchronously() {
        return completedFuture(Math.PI);
    }

    private CompletionStage<Double> delayByOneSecond() {
        return futures.delayed(this::computePIAsynchronously, Duration.ofSeconds(1));
    }

    private CompletionStage<Long> renderAfter(Duration duration) {
        long start = System.currentTimeMillis();
        return futures.delayed(() -> {
            long end = System.currentTimeMillis();
            return completedFuture(end - start);
        }, duration);
    }

}
