/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.streams

import akka.stream.scaladsl._
import akka.stream.stage._
import akka.stream._
import akka.Done

import scala.concurrent.Future

/**
 * Utilities for Akka Streams merging and bypassing of packets.
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
  def bypassWith[In, FlowIn, Out](
    splitter: Flow[In, Either[FlowIn, Out], _],
    mergeStrategy: Graph[UniformFanInShape[Out, Out], _] = onlyFirstCanFinishMerge[Out](2)): Flow[FlowIn, Out, _] => Flow[In, Out, _] = { flow =>

    val bypasser = Flow.fromGraph(GraphDSL.create[FlowShape[Either[FlowIn, Out], Out]]() { implicit builder =>
      import GraphDSL.Implicits._

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
      broadcast.out(1) ~> ignoreAfterCancellation[Either[FlowIn, Out]] ~> Flow[Either[FlowIn, Out]].collect {
        case Right(out) => out
      } ~> merge.in(1)

      FlowShape(broadcast.in, merge.out)
    })

    splitter via bypasser
  }

  def onlyFirstCanFinishMerge[T](inputPorts: Int) = GraphDSL.create[UniformFanInShape[T, T]]() { implicit builder =>
    import GraphDSL.Implicits._

    val merge = builder.add(Merge[T](inputPorts, eagerComplete = true))

    val blockFinishes = (1 until inputPorts).map { i =>
      val blockFinish = builder.add(ignoreAfterFinish[T])
      blockFinish.out ~> merge.in(i)
      blockFinish.in
    }

    val inlets = Seq(merge.in(0)) ++ blockFinishes

    UniformFanInShape(merge.out, inlets: _*)
  }

  /**
   * A flow that will ignore upstream finishes.
   */
  def ignoreAfterFinish[T]: Flow[T, T, _] = Flow[T].via(new GraphStage[FlowShape[T, T]] {

    val in = Inlet[T]("AkkaStreams.in")
    val out = Outlet[T]("AkkaStreams.out")

    override def shape: FlowShape[T, T] = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with OutHandler with InHandler {

        override def onPush(): Unit = push(out, grab(in))

        override def onPull(): Unit = {
          if (!isClosed(in)) {
            pull(in)
          }
        }

        override def onUpstreamFinish() = {
          if (isAvailable(out)) onPull()
        }

        override def onUpstreamFailure(cause: Throwable) = {
          if (isAvailable(out)) onPull()
        }

        setHandlers(in, out, this)
      }
  })

  /**
   * A flow that will ignore downstream cancellation, and instead will continue receiving and ignoring the stream.
   */
  def ignoreAfterCancellation[T]: Flow[T, T, Future[Done]] = {
    Flow.fromGraph(GraphDSL.create(Sink.ignore) { implicit builder => ignore =>
      import GraphDSL.Implicits._
      // This pattern is an effective way to absorb cancellation, Sink.ignore will keep the broadcast always flowing
      // even after sink.inlet cancels.
      val broadcast = builder.add(Broadcast[T](2, eagerCancel = false))
      broadcast.out(0) ~> ignore.in
      FlowShape(broadcast.in, broadcast.out(1))
    })
  }
}
