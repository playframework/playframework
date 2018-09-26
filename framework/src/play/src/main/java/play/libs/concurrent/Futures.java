/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.concurrent;

import akka.Done;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Utilities for creating {@link java.util.concurrent.CompletionStage} operations.
 */
public interface Futures {

    /**
     * Creates a {@link CompletionStage} that returns either the input stage, or a timeout.
     *
     * Note that timeout is not the same as cancellation.  Even in case of timeout,
     * the given completion stage will still complete, even though that completed value
     * is not returned.
     *
     * <pre>
     * {@code
     * CompletionStage<Double> callWithTimeout() {
     *     return futures.timeout(delayByOneSecond(), Duration.ofMillis(300));
     * }
     * }
     * </pre>
     *
     * @param stage the input completion stage that may time out.
     * @param amount The amount (expressed with the corresponding unit).
     * @param unit The time Unit.
     * @param <A> the completion's result type.
     * @return either the completed completion stage, or a completion stage that failed with timeout.
     */
    <A> CompletionStage<A> timeout(CompletionStage<A> stage, long amount, TimeUnit unit);

    /**
     * An alias for {@link #timeout(CompletionStage, long, TimeUnit) timeout} that uses a {@link java.time.Duration}.
     *
     * @param stage the input completion stage that may time out.
     * @param duration The duration after which there is a timeout.
     * @param <A> the completion stage that should be wrapped with a timeout.
     * @return the completion stage, or a completion stage that failed with timeout.
     */
    <A> CompletionStage<A> timeout(CompletionStage<A> stage, Duration duration);

    /**
     * Create a {@link CompletionStage} which, after a delay, will be redeemed with the result of a
     * given callable. The completion stage will be called after the delay.
     *
     * @param callable the input completion stage that is called after the delay.
     * @param amount The time to wait.
     * @param unit The units to use for the amount.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    <A> CompletionStage<A> delayed(Callable<CompletionStage<A>> callable, long amount, TimeUnit unit);

    /**
     * Creates a completion stage which is only completed after the delay.
     *
     * <pre>
     * {@code
     * Duration expected = Duration.ofSeconds(2);
     * long start = System.currentTimeMillis();
     * CompletionStage<Long> stage = futures.delay(expected).thenApply((v) -> {
     *     long end = System.currentTimeMillis();
     *     return (end - start);
     * });
     * }
     * </pre>
     *
     * @param duration the duration after which the completion stage is run.
     * @return the completion stage.
     */
    CompletionStage<Done> delay(Duration duration);

    /**
     * Creates a completion stage which is only completed after the delay.
     *
     * @param amount The time to wait.
     * @param unit The units to use for the amount.
     * @return the delayed CompletionStage.
     */
    CompletionStage<Done> delay(long amount, TimeUnit unit);

    /**
     * Create a {@link CompletionStage} which, after a delay, will be redeemed with the result of a
     * given supplier. The completion stage will be called after the delay.
     *
     * For example, to render a number indicating the delay, you can use the following method:
     *
     * <pre>
     * {@code
     * private CompletionStage<Long> renderAfter(Duration duration) {
     *     long start = System.currentTimeMillis();
     *     return futures.delayed(() -> {
     *          long end = System.currentTimeMillis();
     *          return CompletableFuture.completedFuture(end - start);
     *     }, duration);
     * }
     * }
     * </pre>
     *
     * @param callable the input completion stage that is called after the delay.
     * @param duration to wait.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    <A> CompletionStage<A> delayed(Callable<CompletionStage<A>> callable, Duration duration);

    /**
     * Combine the given CompletionStages into a single {@link CompletionStage} for the list of results.
     *
     * The sequencing operations are performed in the default ExecutionContext.
     *
     * @param promises The CompletionStages to combine
     * @param <A> the type of the completion's result.
     * @return A single CompletionStage whose methods act on the list of redeemed CompletionStages
     */
    static <A> CompletionStage<List<A>> sequence(Iterable<? extends CompletionStage<A>> promises) {
        CompletableFuture<List<A>> result = CompletableFuture.completedFuture(new ArrayList<>());
        for (CompletionStage<A> promise: promises) {
            result = result.thenCombine(promise, (list, a) -> {
                list.add(a);
                return list;
            });
        }
        return result;
    }

    /**
     * Combine the given CompletionStages into a single CompletionStage for the list of results.
     *
     * The sequencing operations are performed in the default ExecutionContext.
     * @param promises The CompletionStages to combine
     * @param <A> the type of the completion's result.
     * @return A single CompletionStage whose methods act on the list of redeemed CompletionStage
     */
    static <A> CompletionStage<List<A>> sequence(CompletionStage<A>... promises) {
        return sequence(Arrays.asList(promises));
    }
}
