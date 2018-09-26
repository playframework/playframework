/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.concurrent;

import akka.Done;
import play.libs.Scala;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static scala.compat.java8.FutureConverters.toJava;

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

        FiniteDuration duration = FiniteDuration.apply(amount, unit);
        return toJava(delegate.timeout(duration, Scala.asScalaWithFuture(() -> stage)));
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

        FiniteDuration finiteDuration = FiniteDuration.apply(duration.toMillis(), TimeUnit.MILLISECONDS);
        return toJava(delegate.timeout(finiteDuration, Scala.asScalaWithFuture(() -> stage)));
    }

    /**
     * Create a CompletionStage which, after a delay, will be redeemed with the result of a
     * given supplier. The supplier will be called after the delay.
     *
     * @param callable the input completion stage that is delayed.
     * @param amount The time to wait.
     * @param unit The units to use for the amount.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    @Override
    public <A> CompletionStage<A> delayed(final Callable<CompletionStage<A>> callable, long amount, TimeUnit unit) {
        requireNonNull(callable, "Null callable");
        requireNonNull(amount, "Null amount");
        requireNonNull(unit, "Null unit");

        FiniteDuration duration = FiniteDuration.apply(amount, unit);
        return toJava(delegate.delayed(duration, Scala.asScalaWithFuture(callable)));
    }

    @Override
    public CompletionStage<Done> delay(Duration duration) {
        FiniteDuration finiteDuration = FiniteDuration.apply(duration.toMillis(), TimeUnit.MILLISECONDS);
        return toJava(delegate.delay(finiteDuration));
    }

    @Override
    public CompletionStage<Done> delay(long amount, TimeUnit unit) {
        FiniteDuration finiteDuration = FiniteDuration.apply(amount, unit);
        return toJava(delegate.delay(finiteDuration));
    }

    /**
     * Create a CompletionStage which, after a delay, will be redeemed with the result of a
     * given supplier. The supplier will be called after the delay.
     *
     * @param callable the input completion stage that is delayed.
     * @param duration to wait.
     * @param <A> the type of the completion's result.
     * @return the delayed CompletionStage wrapping supplier.
     */
    @Override
    public <A> CompletionStage<A> delayed(final Callable<CompletionStage<A>> callable, Duration duration) {
        requireNonNull(callable, "Null callable");
        requireNonNull(duration, "Null duration");

        FiniteDuration finiteDuration = FiniteDuration.apply(duration.toMillis(), TimeUnit.MILLISECONDS);
        return toJava(delegate.delayed(finiteDuration, Scala.asScalaWithFuture(callable)));
    }

}
