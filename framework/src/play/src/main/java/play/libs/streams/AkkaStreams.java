/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.streams;

import akka.stream.FlowShape;
import akka.stream.Graph;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.Broadcast;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.Flow;
import play.libs.F;
import play.libs.Scala;

import java.util.function.Function;

/**
 * Akka streams utilities.
 */
public class AkkaStreams {

    /**
     * Bypass the given flow using the given splitter function.
     * <p>
     * If the splitter function returns Left, they will go through the flow.  If it returns Right, they will bypass the
     * flow.
     * <p>
     * Uses onlyFirstCanFinishMerge(2) by default.
     *
     * @param <In>     the In type parameter for Flow
     * @param <FlowIn> the FlowIn type parameter for the left branch in Either.
     * @param <Out>    the Out type parameter for Flow
     * @param flow     the original flow
     * @param splitter the splitter function to use
     *
     * @return the flow with a bypass.
     */
    public static <In, FlowIn, Out> Flow<In, Out, ?> bypassWith(Function<In, F.Either<FlowIn, Out>> splitter,
                                                                Flow<FlowIn, Out, ?> flow) {
        return bypassWith(Flow.<In>create().map(splitter::apply),
                play.api.libs.streams.AkkaStreams.onlyFirstCanFinishMerge(2), flow);
    }

    /**
     * Using the given splitter flow, allow messages to bypass a flow.
     * <p>
     * If the splitter flow produces Left, they will be fed into the flow. If it produces Right, they will bypass the
     * flow.
     *
     * @param <In>          the In type parameter for Flow
     * @param <FlowIn>      the FlowIn type parameter for the left branch in Either.
     * @param <Out>         the Out type parameter for Flow.
     * @param flow          the original flow.
     * @param splitter      the splitter function.
     * @param mergeStrategy the merge strategy (onlyFirstCanFinishMerge, ignoreAfterFinish, ignoreAfterCancellation)
     *
     * @return the flow with a bypass.
     */
    public static <In, FlowIn, Out> Flow<In, Out, ?> bypassWith(Flow<In, F.Either<FlowIn, Out>, ?> splitter,
                                                                Graph<UniformFanInShape<Out, Out>, ?> mergeStrategy, Flow<FlowIn, Out, ?> flow) {
        return splitter.via(Flow.fromGraph(GraphDSL.<FlowShape<F.Either<FlowIn, Out>, Out>>create(builder -> {

            // Eager cancel must be true so that if the flow cancels, that will be propagated upstream.
            // However, that means the bypasser must block cancel, since when this flow finishes, the merge
            // will result in a cancel flowing up through the bypasser, which could lead to dropped messages.
            // Using scaladsl here because of https://github.com/akka/akka/issues/18384
            UniformFanOutShape<F.Either<FlowIn, Out>, F.Either<FlowIn, Out>> broadcast = builder.add(Broadcast.create(2, true));
            UniformFanInShape<Out, Out> merge = builder.add(mergeStrategy);

            Flow<F.Either<FlowIn, Out>, FlowIn, ?> collectIn = Flow.<F.Either<FlowIn, Out>>create().collect(Scala.partialFunction(x -> {
                if (x.left.isPresent()) {
                    return x.left.get();
                } else {
                    throw Scala.noMatch();
                }
            }));

            Flow<F.Either<FlowIn, Out>, Out, ?> collectOut = Flow.<F.Either<FlowIn, Out>>create().collect(Scala.partialFunction(x -> {
                if (x.right.isPresent()) {
                    return x.right.get();
                } else {
                    throw Scala.noMatch();
                }
            }));

            Flow<F.Either<FlowIn, Out>, F.Either<FlowIn, Out>, ?> blockCancel =
                    play.api.libs.streams.AkkaStreams.<F.Either<FlowIn, Out>>ignoreAfterCancellation().asJava();

            // Normal flow
            builder.from(broadcast.out(0)).via(builder.add(collectIn)).via(builder.add(flow)).toInlet(merge.in(0));

            // Bypass flow, need to ignore downstream finish
            builder.from(broadcast.out(1)).via(builder.add(blockCancel)).via(builder.add(collectOut)).toInlet(merge.in(1));

            return new FlowShape<>(broadcast.in(), merge.out());
        })));
    }

}
