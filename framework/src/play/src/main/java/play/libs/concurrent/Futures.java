/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;
import akka.Done;
import play.Play;
import play.libs.F;
import play.libs.Scala;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

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
     * <pre>{@code
     * CompletionStage<Double> callWithTimeout() {
     *     return futures.timeout(delayByOneSecond(), Duration.ofMillis(300));
     * }
     * }</pre>
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
     * <pre>@{code
     * Duration expected = Duration.ofSeconds(2);
     * long start = System.currentTimeMillis();
     * CompletionStage<Long> stage = futures.delay(expected).thenApply((v) -> {
     *     long end = System.currentTimeMillis();
     *     return (end - start);
     * });
     * }</pre>
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
     * <pre>{@code
     * private CompletionStage<Long> renderAfter(Duration duration) {
     *     long start = System.currentTimeMillis();
     *     return futures.delayed(() -> {
     *          long end = System.currentTimeMillis();
     *          return CompletableFuture.completedFuture(end - start);
     *     }, duration);
     * }
     * }</pre>
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
    public static <A> CompletionStage<List<A>> sequence(Iterable<? extends CompletionStage<A>> promises) {
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
    public static <A> CompletionStage<List<A>> sequence(CompletionStage<A>... promises) {
        return sequence(Arrays.asList(promises));
    }

    /**
     * Create a CompletionStage that is redeemed after a timeout.  This method
     * is useful for returning fallback values on futures.
     *
     * The underlying implementation uses TimerTask, which has a
     * resolution in milliseconds.
     *
     * @deprecated Use injected {@link #timeout(CompletionStage, long, TimeUnit)}, since 2.6.0
     * @param value The result value to use to complete the CompletionStage.
     * @param amount The amount (expressed with the corresponding unit).
     * @param unit The time unit, i.e. java.util.concurrent.TimeUnit.MILLISECONDS
     * @param <A> the type of the completion's result.
     * @return the CompletionStage wrapping the result value
     */
    @Deprecated
    public static <A> CompletionStage<A> timeout(A value, long amount, TimeUnit unit) {
        final Futures futures = Play.application().injector().instanceOf(Futures.class);
        CompletableFuture<A> future = CompletableFuture.completedFuture(value);
        return futures.timeout(future, amount, unit);
    }

    /**
     * Creates a {@link CompletionStage} timer that throws a PromiseTimeoutException after
     * a given timeout.
     *
     * The returned CompletionStage is usually combined with other CompletionStage,
     * i.e. {@code completionStage.applyToEither(Futures.timeout(delay, unit), Function.identity()) }
     *
     * The underlying implementation uses TimerTask, which has a
     * resolution in milliseconds.
     *
     * A previous implementation used {@code CompletionStage<Void>} which made
     * it unsuitable for composition.  Cast with {@code Futures.<Void>timeout} if
     * necessary.
     *
     * @deprecated Use injected {@link #timeout(CompletionStage, long, TimeUnit)}, since 2.6.0
     * @param delay The delay (expressed with the corresponding unit).
     * @param unit The time Unit.
     * @param <A> the type of the completion's result.
     * @return a CompletionStage that failed exceptionally
     */
    @Deprecated
    public static <A> CompletionStage<A> timeout(final long delay, final TimeUnit unit) {
        requireNonNull(unit, "Null unit");
        final Futures futures = Play.application().injector().instanceOf(Futures.class);
        String msg = "Timeout in promise after " + delay + " " + unit.toString();
        final CompletableFuture<A> future = new CompletableFuture<>();
        final F.PromiseTimeoutException ex = new F.PromiseTimeoutException(msg);
        future.completeExceptionally(ex);
        return futures.timeout(future, delay, unit);
    }

    /**
     * Create a {@link CompletionStage} which, after a delay, will be redeemed with the result of a
     * given supplier. The completion stage will be called after the delay.
     *
     * @deprecated Use injected {@link #delayed(Callable, long, TimeUnit)}, since 2.6.0
     * @param supplier The supplier to call to fulfill the CompletionStage.
     * @param delay The time to wait.
     * @param unit The units to use for the delay.
     * @param executor The executor to run the supplier in.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    @Deprecated
    public static <A> CompletionStage<A> delayed(Supplier<A> supplier, long delay, TimeUnit unit, Executor executor) {
        final Futures futures = Play.application().injector().instanceOf(Futures.class);

        return futures.delayed((() -> supplyAsync(supplier, executor)), delay, unit);
    }

}
