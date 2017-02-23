/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.streams;

import akka.stream.Materializer;
import akka.stream.javadsl.*;
import play.api.libs.streams.Accumulator$;
import scala.compat.java8.FutureConverters;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Accumulates inputs asynchronously into an output value.
 *
 * An accumulator is a view over an Akka streams Sink that materialises to a future, that is focused on the value of
 * that future, rather than the Stream. This means methods such as <code>map</code>, <code>recover</code> and so on are
 * provided for the eventually redeemed future value.
 *
 * In order to be in line with the Java ecosystem, the future implementation that this uses for the materialised value
 * of the Sink is java.util.concurrent.CompletionStage, and running this accumulator will yield a CompletionStage. The
 * constructor allows an accumulator to be created from such a sink. Many methods in the Akka streams API however
 * materialise a scala.concurrent.Future, hence the <code>fromSink</code> method is provided to create an accumulator
 * from a typical Akka streams <code>Sink</code>.
 */
public abstract class Accumulator<E, A> {

    private Accumulator() {}

    /**
     * Map the accumulated value.
     *
     * @param f The function to perform the map with.
     * @param executor The executor to run the function in.
     * @return A new accumulator with the mapped value.
     */
    public abstract <B> Accumulator<E, B> map(Function<? super A, ? extends B> f, Executor executor);

    /**
     * Map the accumulated value with a function that returns a future.
     *
     * @param f The function to perform the map with.
     * @param executor The executor to run the function in.
     * @return A new accumulator with the mapped value.
     */
    public abstract <B> Accumulator<E, B> mapFuture(Function<? super A, ? extends CompletionStage<B>> f, Executor executor);

    /**
     * Recover from any errors encountered by the accumulator.
     *
     * @param f The function to use to recover from errors.
     * @param executor The executor to run the function in.
     * @return A new accumulator that has recovered from errors.
     */
    public abstract Accumulator<E, A> recover(Function<? super Throwable, ? extends A> f, Executor executor);

    /**
     * Recover from any errors encountered by the accumulator.
     *
     * @param f The function to use to recover from errors.
     * @param executor The executor to run the function in.
     * @return A new accumulator that has recovered from errors.
     */
    public abstract Accumulator<E, A> recoverWith(Function<? super Throwable, ? extends CompletionStage<A>> f, Executor executor);

    /**
     * Pass the stream through the given flow before forwarding it to the accumulator.
     *
     * @param flow The flow to send the stream through first.
     * @return A new accumulator with the given flow in its graph.
     */
    public abstract <D> Accumulator<D, A> through(Flow<D, E, ?> flow);

    /**
     * Run the accumulator with an empty source.
     *
     * @param mat The flow materializer.
     * @return A future that will be redeemed when the accumulator is done.
     */
    public abstract CompletionStage<A> run(Materializer mat);

    /**
     * Run the accumulator with the given source.
     *
     * @param source The source to feed into the accumulator.
     * @param mat The flow materializer.
     * @return A future that will be redeemed when the accumulator is done.
     */
    public abstract CompletionStage<A> run(Source<E, ?> source, Materializer mat);

    /**
     * Convert this accumulator to a sink.
     *
     * @return The sink.
     */
    public abstract Sink<E, CompletionStage<A>> toSink();

    /**
     * Convert this accumulator to a Scala accumulator.
     *
     * @return The Scala Accumulator.
     */
    public abstract play.api.libs.streams.Accumulator<E, A> asScala();

    /**
     * Create an accumulator from an Akka streams sink.
     *
     * @param sink The sink.
     * @return An accumulator created from the sink.
     */
    public static <E, A> Accumulator<E, A> fromSink(Sink<E, CompletionStage<A>> sink) {
        return new SinkAccumulator<>(sink);
    }


    /**
     * Create an accumulator that forwards the stream fed into it to the source it produces.
     *
     * This is useful for when you want to send the consumed stream to another API that takes a Source as input.
     *
     * Extreme care must be taken when using this accumulator - the source *must always* be materialized and consumed.
     * If it isn't, this could lead to resource leaks and deadlocks upstream.
     *
     * @return An accumulator that forwards the stream to the produced source.
     */
    public static <E> Accumulator<E, Source<E, ?>> source() {
        // If Akka streams ever provides Sink.source(), we should use that instead.
        // https://github.com/akka/akka/issues/18406
        return new SinkAccumulator<>(Sink.<E>asPublisher(AsPublisher.WITHOUT_FANOUT).mapMaterializedValue(publisher ->
                        CompletableFuture.completedFuture(Source.fromPublisher(publisher))
        ));
    }

    /**
     * Create a done accumulator with the given value.
     *
     * @param a The done value for the accumulator.
     * @return The accumulator.
     */
    public static <E, A> Accumulator<E, A> done(A a) {
        return done(CompletableFuture.completedFuture(a));
    }

    /**
     * Create a done accumulator with the given future.
     *
     * @param a A future of the done value.
     * @return The accumulator.
     */
    public static <E, A> Accumulator<E, A> done(CompletionStage<A> a) {
        return new DoneAccumulator<>(a);
    }

    private static final class SinkAccumulator<E, A> extends Accumulator<E, A> {

        private final Sink<E, CompletionStage<A>> sink;

        private SinkAccumulator(Sink<E, CompletionStage<A>> sink) {
            this.sink = sink;
        }

        public <B> Accumulator<E, B> map(Function<? super A, ? extends B> f, Executor executor) {
            return new SinkAccumulator<>(sink.mapMaterializedValue(cs -> cs.thenApplyAsync(f, executor)));
        }

        public <B> Accumulator<E, B> mapFuture(Function<? super A, ? extends CompletionStage<B>> f, Executor executor) {
            return new SinkAccumulator<>(sink.mapMaterializedValue(cs -> cs.thenComposeAsync(f, executor)));
        }

        public Accumulator<E, A> recover(Function<? super Throwable, ? extends A> f, Executor executor) {
            return new SinkAccumulator<>(
                sink.mapMaterializedValue(cs -> completionStageRecover(cs, f, executor))
            );
        }

        public Accumulator<E, A> recoverWith(Function<? super Throwable, ? extends CompletionStage<A>> f, Executor executor) {
            return new SinkAccumulator<>(
                sink.mapMaterializedValue(cs -> completionStageRecoverWith(cs, f, executor))
            );
        }

        public <D> Accumulator<D, A> through(Flow<D, E, ?> flow) {
            return new SinkAccumulator<>(flow.toMat(sink, Keep.right()));
        }

        public CompletionStage<A> run(Materializer mat) {
            return Source.<E>empty().runWith(sink, mat);
        }

        public CompletionStage<A> run(Source<E, ?> source, Materializer mat) {
            return source.runWith(sink, mat);
        }

        public Sink<E, CompletionStage<A>> toSink() {
            return sink;
        }

        public play.api.libs.streams.Accumulator<E, A> asScala() {
            return Accumulator$.MODULE$.apply(sink.mapMaterializedValue(FutureConverters::toScala).asScala());
        }

    }

    private static final class DoneAccumulator<E, A> extends Accumulator<E, A> {

        private final CompletionStage<A> value;

        private DoneAccumulator(CompletionStage<A> value) {
            this.value = value;
        }

        public <B> Accumulator<E, B> map(Function<? super A, ? extends B> f, Executor executor) {
            return new DoneAccumulator<>(value.thenApplyAsync(f, executor));
        }

        public <B> Accumulator<E, B> mapFuture(Function<? super A, ? extends CompletionStage<B>> f, Executor executor) {
            return new DoneAccumulator<>(value.thenComposeAsync(f, executor));
        }

        public Accumulator<E, A> recover(Function<? super Throwable, ? extends A> f, Executor executor) {
            return new DoneAccumulator<>(completionStageRecover(value, f, executor));
        }

        public Accumulator<E, A> recoverWith(Function<? super Throwable, ? extends CompletionStage<A>> f, Executor executor) {
            return new DoneAccumulator<>(completionStageRecoverWith(value, f, executor));
        }

        @SuppressWarnings("unchecked")
        public <D> Accumulator<D, A> through(Flow<D, E, ?> flow) {
            return (Accumulator<D, A>) this;
        }

        public CompletionStage<A> run(Materializer mat) {
            return value;
        }

        public CompletionStage<A> run(Source<E, ?> source, Materializer mat) {
            source.runWith(Sink.cancelled(), mat);
            return value;
        }

        public Sink<E, CompletionStage<A>> toSink() {
            return Sink.<E>cancelled().mapMaterializedValue(u -> value);
        }

        @SuppressWarnings("unchecked")
        public play.api.libs.streams.Accumulator<E, A> asScala() {
            return (play.api.libs.streams.Accumulator<E, A>) Accumulator$.MODULE$.done(FutureConverters.toScala(value));
        }

    }

    private static <A> CompletionStage<A> completionStageRecoverWith(CompletionStage<A> cs,
        Function<? super Throwable, ? extends CompletionStage<A>> f, Executor executor) {
        return cs.handleAsync((a, error) -> {
            if (a != null) {
                return CompletableFuture.completedFuture(a);
            } else {
                if (error instanceof CompletionException) {
                    return f.apply(error.getCause());
                } else {
                    return f.apply(error);
                }
            }
        }, executor).thenCompose(Function.identity());
    }

    private static <A> CompletionStage<A> completionStageRecover(CompletionStage<A> cs,
        Function<? super Throwable, ? extends A> f, Executor executor) {
        return cs.handleAsync((a, error) -> {
            if (a != null) {
                return a;
            } else {
                return f.apply(error);
            }
        }, executor);
    }

}
