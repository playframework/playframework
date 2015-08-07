package play.api.libs.streams

import akka.stream.scaladsl.FlexiMerge.{ MergeLogic, ReadAny }
import akka.stream.scaladsl._
import akka.stream.{ Attributes, UniformFanInShape }
import play.api.libs.iteratee._

import scala.util.{ Failure, Success }

/**
 * Utilities for
 */
object AkkaStreams {

  /**
   * Bypass the given flow using the given splitter function.
   *
   * If the splitter function returns Left, they will go through the flow.  If it returns Right, they will bypass the
   * flow.
   */
  def bypassWith[In, FlowIn, Out](splitter: In => Either[FlowIn, Out]): Flow[FlowIn, Out, _] => Flow[In, Out, _] = {
    bypassWith(Flow[In].map(splitter))
  }

  /**
   * Using the given splitter flow, allow messages to bypass a flow.
   *
   * If the splitter flow produces Left, they will be fed into the flow. If it produces Right, they will bypass the
   * flow.
   */
  def bypassWith[In, FlowIn, Out](splitter: Flow[In, Either[FlowIn, Out], _],
    mergeStrategy: FlexiMerge[Out, UniformFanInShape[Out, Out]] = OnlyFirstCanFinishMerge[Out](2)): Flow[FlowIn, Out, _] => Flow[In, Out, _] = { flow =>

    splitter via Flow[Either[FlowIn, Out], Out]() { implicit builder =>
      import FlowGraph.Implicits._

      // Eager cancel must be true so that if the flow cancels, that will be propagated upstream.
      // However, that means the bypasser must block cancel, since when this flow finishes, the merge
      // will result in a cancel flowing up through the bypasser, which could lead to dropped messages.
      val broadcast = builder.add(Broadcast[Either[FlowIn, Out]](2, eagerCancel = true))
      val merge = builder.add(mergeStrategy)

      // Normal flow
      broadcast.out(0) ~> Flow[Either[FlowIn, Out]].collect {
        case Left(in) => in
      } ~> flow ~> merge.in(0)

      // Bypass flow, need to ignore downstream finish
      broadcast.out(1) ~> blockCancel[Either[FlowIn, Out]](() => ()) ~> Flow[Either[FlowIn, Out]].collect {
        case Right(out) => out
      } ~> merge.in(1)

      broadcast.in -> merge.out
    }
  }

  /**
   * Can be replaced if https://github.com/akka/akka/issues/18175 ever gets implemented.
   */
  case class EagerFinishMerge[T](inputPorts: Int) extends FlexiMerge[T, UniformFanInShape[T, T]](new UniformFanInShape[T, T](inputPorts), Attributes.name("EagerFinishMerge")) {
    def createMergeLogic(s: UniformFanInShape[T, T]): MergeLogic[T] =
      new MergeLogic[T] {
        def initialState: State[T] = State[T](ReadAny(s.inSeq)) {
          case (ctx, port, in) =>
            ctx.emit(in)
            SameState
        }
        override def initialCompletionHandling: CompletionHandling = eagerClose
      }
  }

  /**
   * A merge that only allows the first inlet to finish downstream.
   */
  case class OnlyFirstCanFinishMerge[T](inputPorts: Int) extends FlexiMerge[T, UniformFanInShape[T, T]](new UniformFanInShape[T, T](inputPorts), Attributes.name("EagerFinishMerge")) {
    def createMergeLogic(s: UniformFanInShape[T, T]): MergeLogic[T] =
      new MergeLogic[T] {
        def initialState: State[T] = State[T](ReadAny(s.inSeq)) {
          case (ctx, port, in) =>
            ctx.emit(in)
            SameState
        }
        override def initialCompletionHandling: CompletionHandling = CompletionHandling(
          onUpstreamFinish = { (ctx, port) =>
            if (port == s.in(0)) {
              ctx.finish()
            }
            SameState
          }, onUpstreamFailure = { (ctx, port, error) =>
            if (port == s.in(0)) {
              ctx.fail(error)
            }
            SameState
          }
        )
      }
  }

  /**
   * Because https://github.com/akka/akka/issues/18189
   */
  private[play] def blockCancel[T](onCancel: () => Unit): Flow[T, T, Unit] = {
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    val (enum, channel) = Concurrent.broadcast[T]
    val sink = Sink.foreach[T](channel.push).mapMaterializedValue(_.onComplete {
      case Success(_) => channel.end()
      case Failure(t) => channel.end(t)
    })
    // This enumerator ignores cancel by flatmapping the iteratee to an Iteratee.ignore
    val cancelIgnoring = new Enumerator[T] {
      def apply[A](i: Iteratee[T, A]) =
        enum(i.flatMap { a =>
          onCancel()
          Iteratee.ignore[T].map(_ => a)
        })
    }
    Flow.wrap(sink, Source(Streams.enumeratorToPublisher(cancelIgnoring)))(Keep.none)
  }
}
