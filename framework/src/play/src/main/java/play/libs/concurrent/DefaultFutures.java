/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;

import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static scala.compat.java8.FutureConverters.toJava;
import static scala.compat.java8.FutureConverters.toScala;

/**
 * The default implementation of the Futures trait.  This provides an
 * implementation that uses the scheduler of the application's ActorSystem.
 */
public class DefaultFutures implements Futures {

    private final play.api.libs.concurrent.Futures delegate;

    @Inject
    public DefaultFutures(play.api.libs.concurrent.Futures delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a CompletionStage that returns either the input stage, or a futures.
     *
     * Note that timeout is not the same as cancellation.  Even in case of futures,
     * the given completion stage will still complete, even though that completed value
     * is not returned.
     *
     * @param stage the input completion stage that may time out.
     * @param amount The amount (expressed with the corresponding unit).
     * @param unit The time Unit.
     * @param <A> the completion's result type.
     * @return either the completed future, or a completion stage that failed with futures.
     */
    @Override
    public <A> CompletionStage<A> timeout(final CompletionStage<A> stage, final long amount, final TimeUnit unit) {
        requireNonNull(stage, "Null stage");
        requireNonNull(unit, "Null unit");

        Future<A> future = toScala(stage);
        FiniteDuration duration = FiniteDuration.apply(amount, unit);
        return toJava(delegate.timeout(duration, future));
    }

    /**
     * An alias for futures(stage, delay, unit) that uses a java.time.Duration.
     *
     * @param stage the input completion stage that may time out.
     * @param duration The duration after which there is a timeout.
     * @param <A> the completion stage that should be wrapped with a future.
     * @return the completion stage, or a completion stage that failed with futures.
     */
    @Override
    public <A> CompletionStage<A> timeout(final CompletionStage<A> stage, final Duration duration) {
        requireNonNull(stage, "Null stage");
        requireNonNull(duration, "Null duration");

        Future<A> future = toScala(stage);
        FiniteDuration finiteDuration = FiniteDuration.apply(duration.toMillis(), TimeUnit.MILLISECONDS);
        return toJava(delegate.timeout(finiteDuration, future));
    }

    /**
     * Create a CompletionStage which, after a delay, will be redeemed with the result of a
     * given supplier. The supplier will be called after the delay.
     *
     * @param stage the input completion stage that is delayed.
     * @param amount The time to wait.
     * @param unit The units to use for the amount.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    @Override
    public <A> CompletionStage<A> delayed(final CompletionStage<A> stage, long amount, TimeUnit unit) {
        requireNonNull(stage, "Null stage");
        requireNonNull(amount, "Null amount");
        requireNonNull(unit, "Null unit");

        Future<A> future = toScala(stage);
        FiniteDuration duration = FiniteDuration.apply(amount, unit);
        return toJava(delegate.delayed(duration, future));
    }

    /**
     * Create a CompletionStage which, after a delay, will be redeemed with the result of a
     * given supplier. The supplier will be called after the delay.
     *
     * @param stage the input completion stage that is delayed.
     * @param duration to wait.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    @Override
    public <A> CompletionStage<A> delayed(CompletionStage<A> stage, Duration duration) {
        requireNonNull(stage, "Null stage");
        requireNonNull(duration, "Null duration");

        Future<A> future = toScala(stage);
        FiniteDuration finiteDuration = FiniteDuration.apply(duration.toMillis(), TimeUnit.MILLISECONDS);
        return toJava(delegate.delayed(finiteDuration, future));
    }

}
