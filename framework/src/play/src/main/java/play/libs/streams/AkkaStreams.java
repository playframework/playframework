package play.libs.streams;

import akka.japi.Pair;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.scaladsl.Broadcast$;
import akka.stream.javadsl.Flow;
import akka.stream.scaladsl.FlexiMerge;
import play.api.libs.streams.AkkaStreams$;
import play.libs.F;
import play.libs.Scala;
import scala.runtime.AbstractFunction0;
import scala.runtime.BoxedUnit;

import java.util.function.Function;

/**
 * Akka streams utilities.
 */
public class AkkaStreams {

    /**
     * Bypass the given flow using the given splitter function.
     *
     * If the splitter function returns Left, they will go through the flow.  If it returns Right, they will bypass the
     * flow.
     */
    public static <In, FlowIn, Out> Flow<In, Out, ?> bypassWith(Function<In, F.Either<FlowIn, Out>> splitter,
                                                                Flow<FlowIn, Out, ?> flow) {
        return bypassWith(Flow.<In>create().map(splitter::apply),
                new play.api.libs.streams.AkkaStreams.OnlyFirstCanFinishMerge(2), flow);
    }

    /**
     * Using the given splitter flow, allow messages to bypass a flow.
     *
     * If the splitter flow produces Left, they will be fed into the flow. If it produces Right, they will bypass the
     * flow.
     */
    public static <In, FlowIn, Out> Flow<In, Out, ?> bypassWith(Flow<In, F.Either<FlowIn, Out>, ?> splitter,
                                                             FlexiMerge<Out, UniformFanInShape<Out, Out>> mergeStrategy,
                                                             Flow<FlowIn, Out, ?> flow) {
        return splitter.via(Flow.<F.Either<FlowIn, Out>, Out>factory().create(builder -> {

            // Eager cancel must be true so that if the flow cancels, that will be propagated upstream.
            // However, that means the bypasser must block cancel, since when this flow finishes, the merge
            // will result in a cancel flowing up through the bypasser, which could lead to dropped messages.
            // Using scaladsl here because of https://github.com/akka/akka/issues/18384
            UniformFanOutShape<F.Either<FlowIn, Out>, F.Either<FlowIn, Out>> broadcast = builder.graph(Broadcast$.MODULE$.apply(2, true));
            UniformFanInShape<Out, Out> merge = builder.graph(mergeStrategy);

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
                    AkkaStreams$.MODULE$.<F.Either<FlowIn, Out>>blockCancel(new AbstractFunction0<BoxedUnit>() {
                        @Override
                        public BoxedUnit apply() {
                            return BoxedUnit.UNIT;
                        }
                    }).asJava();

            // Normal flow
            builder.from(broadcast.out(0)).via(collectIn).via(flow).to(merge.in(0));

            // Bypass flow, need to ignore downstream finish
            builder.from(broadcast.out(1)).via(blockCancel).via(collectOut).to(merge.in(1));

            return Pair.create(broadcast.in(), merge.out());
        }));
    }

}
