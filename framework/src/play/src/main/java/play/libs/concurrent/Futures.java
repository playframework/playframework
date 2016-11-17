/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;

import play.libs.F;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Utilities for creating {@link java.util.concurrent.CompletionStage}.
 */
public class Futures {

    private static Timer timer = new Timer(true);

    /**
     * Combine the given CompletionStages into a single CompletionStage for the list of results.
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
     * is useful for returning fallback values on timeout.
     *
     * The underlying implementation uses TimerTask, which has a
     * resolution in milliseconds.
     *
     * @param value The result value to use to complete the CompletionStage.
     * @param delay The delay (expressed with the corresponding unit).
     * @param unit The time unit, i.e. java.util.concurrent.TimeUnit.MILLISECONDS
     * @param <A> the type of the completion's result.
     * @return the CompletionStage wrapping the result value
     */
    public static <A> CompletionStage<A> timeout(A value, long delay, TimeUnit unit) {
        CompletableFuture<A> future = new CompletableFuture<>();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                future.complete(value);
            }
        }, unit.toMillis(delay));
        return future;
    }

    /**
     * Creates a CompletionStage timer that throws a PromiseTimeoutException after
     * a given timeout.
     *
     * The returned CompletionStage is usually combined with other CompletionStage,
     * i.e. {@code completionStage.applyToEither(timeout, Function.identity()) }
     *
     * The underlying implementation uses TimerTask, which has a
     * resolution in milliseconds.
     *
     * A previous implementation used {@code CompletionStage<Void>} which made
     * it unsuitable for composition.  Cast with {@code Futures.<Void>timeout} if
     * necessary.
     *
     * @param delay The delay (expressed with the corresponding unit).
     * @param unit The time Unit.
     * @param <A> the type of the completion's result.
     * @return a CompletionStage that failed exceptionally
     */
    public static <A> CompletionStage<A> timeout(final long delay, final TimeUnit unit) {
        requireNonNull(unit, "Null unit");
        final CompletableFuture<A> future = new CompletableFuture<>();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String msg = "Timeout in promise after " + delay + " " + unit.toString();
                final F.PromiseTimeoutException ex = new F.PromiseTimeoutException(msg);
                future.completeExceptionally(ex);
            }
        }, unit.toMillis(delay));
        return future;
    }

    /**
     * Create a CompletionStage which, after a delay, will be redeemed with the result of a
     * given supplier. The supplier will be called after the delay.
     *
     * @param supplier The supplier to call to fulfill the CompletionStage.
     * @param delay The time to wait.
     * @param unit The units to use for the delay.
     * @param executor The executor to run the supplier in.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    public static <A> CompletionStage<A> delayed(Supplier<A> supplier, long delay, TimeUnit unit, Executor executor) {
        CompletableFuture<A> future = new CompletableFuture<>();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                executor.execute(() -> future.complete(supplier.get()));
            }
        }, unit.toMillis(delay));
        return future;
    }
}
