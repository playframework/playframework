/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.streams;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.reactivestreams.Subscriber;

import akka.stream.Materializer;
import akka.stream.javadsl.*;
import play.api.libs.streams.Accumulator$;
import scala.Option;
import scala.compat.java8.FutureConverters;
import scala.compat.java8.OptionConverters;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Accumulates inputs asynchronously into an output value.
 *
 * An accumulator is a view over an Akka streams Sink that materialises to a future, that is focused on the value of
 * that future, rather than the Stream. This means methods such as {@code map}, {@code recover} and so on are
 * provided for the eventually redeemed future value.
 *
 * In order to be in line with the Java ecosystem, the future implementation that this uses for the materialised value
 * of the Sink is java.util.concurrent.CompletionStage, and running this accumulator will yield a CompletionStage. The
 * constructor allows an accumulator to be created from such a sink. Many methods in the Akka streams API however
 * materialise a scala.concurrent.Future, hence the {@code fromSink} method is provided to create an accumulator
 * from a typical Akka streams {@code Sink}.
 */
public abstract class Accumulator<E, A> {

    private Accumulator() {}

    /**
     * Map the accumulated value.
     *
     * @param <B> the mapped value type
     * @param f The function to perform the map with.
     * @param executor The executor to run the function in.
     * @return A new accumulator with the mapped value.
     */
    public abstract <B> Accumulator<E, B> map(Function<? super A, ? extends B> f, Executor executor);

    /**
     * Map the accumulated value with a function that returns a future.
     *
     * @param <B> the mapped value type
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
     * @param <D> the "In" type for the flow parameter.
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
     * Run the accumulator with a single element.
     *
     * @param element The element to feed into the accumulator.
     * @param mat The flow materilaizer.
     * @return A future that will be redeemed when the accumulator is done.
     */
    public abstract CompletionStage<A> run(E element, Materializer mat);

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
     * @param <E> the "in" type of the sink parameter.
     * @param <A> the materialized result of the accumulator.
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
     * @param <E> the "in" type of the parameter.
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
     * @param <E> the "in" type of the parameter.
     * @param <A> the materialized result of the accumulator.
     * @param a The done value for the accumulator.
     * @return The accumulator.
     */
    public static <E, A> Accumulator<E, A> done(A a) {
        return done(CompletableFuture.completedFuture(a));
    }

    /**
     * Create a done accumulator with the given future.
     *
     * @param <E> the "in" type of the parameter.
     * @param <A> the materialized result of the accumulator.
     * @param a A future of the done value.
     * @return The accumulator.
     */
    public static <E, A> Accumulator<E, A> done(CompletionStage<A> a) {
        return new StrictAccumulator<>(e -> a, Sink.<E>cancelled().mapMaterializedValue(notUsed -> a));
    }

    /**
     * Create a done accumulator with the given future.
     *
     * @param <E> the "in" type of the parameter.
     * @param <A> the materialized result of the accumulator.
     * @param strictHandler the handler to handle the stream if it can be expressed as a single element.
     * @param toSink The sink representation of this accumulator, in case the stream can't be expressed as a single element.
     * @return The accumulator.
     */
    public static <E, A> Accumulator<E, A> strict(Function<Optional<E>, CompletionStage<A>> strictHandler, Sink<E, CompletionStage<A>> toSink) {
        return new StrictAccumulator<>(strictHandler, toSink);
    }

    /**
     * Flatten a completion stage of an accumulator to an accumulator.
     *
     * @param <E> the "in" type of the parameter.
     * @param <A> the materialized result of the accumulator.
     * @param stage the CompletionStage (asynchronous) accumulator
     * @param materializer the stream materializer
     * @return The accumulator using the given completion stage
     */
    public static <E, A> Accumulator<E, A> flatten(CompletionStage<Accumulator<E, A>> stage, Materializer materializer) {
        final CompletableFuture<A> result = new CompletableFuture<A>();
        final FlattenSubscriber<A, E> subscriber = 
            new FlattenSubscriber<>(stage, result, materializer);

        final Sink<E, CompletableFuture<A>> sink =
            Sink.fromSubscriber(subscriber).
            mapMaterializedValue(x -> result);

        return new SinkAccumulator(sink);
    }

    private static final class NoOpSubscriber<E> implements Subscriber<E> {
        public void onSubscribe(Subscription sub) { }
        public void onError(Throwable t) { }
        public void onComplete() { }
        public void onNext(E next) { }
    }

    private static final class FlattenSubscriber<A, E>
        implements Subscriber<E> {

        private final CompletionStage<Accumulator<E, A>> stage;
        private final CompletableFuture<A> result;
        private final Materializer materializer;
        private volatile Subscriber<? super E> underlying =
            new NoOpSubscriber<E>();

        public FlattenSubscriber(CompletionStage<Accumulator<E, A>> stage,
                                 CompletableFuture<A> result,
                                 Materializer materializer) {

            this.stage = stage;
            this.result = result;
            this.materializer = materializer;
        }

        private Publisher<E> publisher(final Subscription sub) {
            return s -> {
                underlying = s;
                s.onSubscribe(sub);
            };
        }

        private BiFunction<A, Throwable, Void> completionHandler =
            new BiFunction<A, Throwable, Void>() {
                public Void apply(A completion, Throwable err) {
                    if (completion != null) {
                        result.complete(completion);
                    } else {
                        result.completeExceptionally(err);
                    }

                    return null;
                }
            };

        private CompletableFuture<A> completeResultWith(final CompletionStage<A> asyncRes) {
            asyncRes.handleAsync(completionHandler);

            return this.result;
        }
    
        private BiFunction<Accumulator<E, A>, Throwable, Void> handler(final Subscription sub) {
            return (acc, error) -> {
                if (acc != null) {
                    Source.fromPublisher(publisher(sub)).runWith(acc.toSink().mapMaterializedValue(this::completeResultWith), materializer);
                } else {
                    // On error
                    sub.cancel();
                    result.completeExceptionally(error);
                }
                return null;
            };
        }

        public void onSubscribe(Subscription sub) {
            this.stage.handleAsync(handler(sub));
        }

        public void onError(Throwable t) { underlying.onError(t); }
        public void onComplete() { underlying.onComplete(); }
        public void onNext(E next) { underlying.onNext(next); }
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

        public CompletionStage<A> run(E element, Materializer mat) {
            return run(Source.single(element), mat);
        }

        public Sink<E, CompletionStage<A>> toSink() {
            return sink;
        }

        public play.api.libs.streams.Accumulator<E, A> asScala() {
            return Accumulator$.MODULE$.apply(sink.mapMaterializedValue(FutureConverters::toScala).asScala());
        }
    }

    private static final class StrictAccumulator<E, A> extends Accumulator<E, A> {

        private final Function<Optional<E>, CompletionStage<A>> strictHandler;
        private final Sink<E, CompletionStage<A>> toSink;

        public StrictAccumulator(Function<Optional<E>, CompletionStage<A>> strictHandler, Sink<E, CompletionStage<A>> toSink) {
            this.strictHandler = strictHandler;
            this.toSink = toSink;
        }

        private <B> Accumulator<E, B> mapMat(Function<CompletionStage<A>, CompletionStage<B>> f) {
            return new StrictAccumulator<>(strictHandler.andThen(f), toSink.mapMaterializedValue(f::apply));
        }

        public <B> Accumulator<E, B> map(Function<? super A, ? extends B> f, Executor executor) {
            return mapMat(cs -> cs.thenApplyAsync(f, executor));
        }

        public <B> Accumulator<E, B> mapFuture(Function<? super A, ? extends CompletionStage<B>> f, Executor executor) {
            return mapMat(cs -> cs.thenComposeAsync(f, executor));
        }

        public Accumulator<E, A> recover(Function<? super Throwable, ? extends A> f, Executor executor) {
            return mapMat(cs -> completionStageRecover(cs, f, executor));
        }

        public Accumulator<E, A> recoverWith(Function<? super Throwable, ? extends CompletionStage<A>> f, Executor executor) {
            return mapMat(cs -> completionStageRecoverWith(cs, f, executor));
        }

        public <D> Accumulator<D, A> through(Flow<D, E, ?> flow) {
            return new SinkAccumulator<>(flow.toMat(toSink, Keep.right()));
        }

        public CompletionStage<A> run(Materializer mat) {
            return strictHandler.apply(Optional.empty());
        }

        public CompletionStage<A> run(Source<E, ?> source, Materializer mat) {
            return source.runWith(toSink, mat);
        }

        public CompletionStage<A> run(E element, Materializer mat) {
            return strictHandler.apply(Optional.of(element));
        }

        public Sink<E, CompletionStage<A>> toSink() {
            return toSink;
        }

        @SuppressWarnings("unchecked")
        public play.api.libs.streams.Accumulator<E, A> asScala() {
            return Accumulator$.MODULE$.strict(
                    new AbstractFunction1<Option<E>, Future<A>>() {
                        @Override
                        public Future<A> apply(Option<E> v1) {
                            return FutureConverters.toScala(strictHandler.apply(OptionConverters.toJava(v1)));
                        }
                    },
                    toSink.mapMaterializedValue(FutureConverters::toScala).asScala()
            );
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
