/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;

import play.libs.F;
import play.libs.F.Promise;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utilities for creating {@link java.util.concurrent.CompletionStage}.
 */
public class Futures {

    private static Timer timer = new Timer(true);

    /**
     * Combine the given promises into a single promise for the list of results.
     *
     * The sequencing operations are performed in the default ExecutionContext.
     *
     * @param promises The promises to combine
     * @return A single promise whose methods act on the list of redeemed promises
     */
    public static <A> Promise<List<A>> sequence(Iterable<? extends CompletionStage<A>> promises) {
        CompletableFuture<List<A>> result = CompletableFuture.completedFuture(new ArrayList<>());
        for (CompletionStage<A> promise: promises) {
            result = result.thenCombine(promise, (list, a) -> {
                list.add(a);
                return list;
            });
        }
        return Promise.wrap(result);
    }

    /**
     * Combine the given promises into a single promise for the list of results.
     *
     * The sequencing operations are performed in the default ExecutionContext.
     *
     * @param promises The promises to combine
     * @return A single promise whose methods act on the list of redeemed promises
     */
    public static <A> Promise<List<A>> sequence(CompletionStage<A>... promises) {
        return sequence(Arrays.asList(promises));
    }

    /**
     * Create a Promise that is redeemed after a timeout.
     *
     * @param message The message to use to redeem the Promise.
     * @param delay The delay (expressed with the corresponding unit).
     * @param unit The Unit.
     */
    public static <A> Promise<A> timeout(A message, long delay, TimeUnit unit) {
        CompletableFuture<A> future = new CompletableFuture<>();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                future.complete(message);
            }
        }, unit.toMillis(delay));
        return Promise.wrap(future);
    }

    /**
     * Create a Promise timer that throws a PromiseTimeoutException after
     * a given timeout.
     *
     * The returned Promise is usually combined with other Promises.
     *
     * @param delay The delay (expressed with the corresponding unit).
     * @param unit The Unit.
     * @return a promise without a real value
     */
    public static Promise<Void> timeout(long delay, TimeUnit unit) {
        return timeout(null, delay, unit).thenApply(n -> {
            throw new F.PromiseTimeoutException("Timeout in promise");
        });
    }

    /**
     * Create a Promise which, after a delay, will be redeemed with the result of a
     * given supplier. The supplier will be called after the delay.
     *
     * @param supplier The supplier to call to fulfill the Promise.
     * @param delay The time to wait.
     * @param unit The units to use for the delay.
     * @param executor The executor to run the supplier in.
     */
    public static <A> Promise<A> delayed(Supplier<A> supplier, long delay, TimeUnit unit, Executor executor) {
        CompletableFuture<A> future = new CompletableFuture<>();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                executor.execute(() -> future.complete(supplier.get()));
            }
        }, unit.toMillis(delay));
        return Promise.wrap(future);
    }
}
