/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * This interface is used to provide a non-blocking timeout on an operation
 * that returns a CompletionStage.
 */
public interface Timeout {

    /**
     * Creates a CompletionStage that returns either the input stage, or a timeout.
     *
     * Note that timeout is not the same as cancellation.  Even in case of timeout,
     * the given completion stage will still complete, even though that completed value
     * is not returned.
     *
     * @param stage the input completion stage that may time out.
     * @param delay The delay (expressed with the corresponding unit).
     * @param unit The time Unit.
     * @param <A> the completion's result type.
     * @return either the completed future, or a completion stage that failed with timeout.
     */
    default <A> CompletionStage<A> timeout(final CompletionStage<A> stage, final long delay, final TimeUnit unit) {
        requireNonNull(stage, "Null stage");
        requireNonNull(unit, "Null unit");
        final CompletionStage<A> timeoutFuture = Futures.timeout(delay, unit);
        // use this stage's default asynchronous execution facility for non-blocking.
        return stage.applyToEitherAsync(timeoutFuture, Function.identity());
    }

    /**
     * An alias for timeout(stage, delay, unit) that uses a java.time.Duration.
     *
     * @param stage the input completion stage that may time out.
     * @param delay The delay (expressed with the corresponding unit).
     * @param <A> the completion stage that should be wrapped with a future.
     * @return the completion stage, or a completion stage that failed with timeout.
     */
    default <A> CompletionStage<A> timeout(final CompletionStage<A> stage, final Duration delay) {
        requireNonNull(stage, "Null stage");
        requireNonNull(delay, "Null delay");
        return timeout(stage, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

}
