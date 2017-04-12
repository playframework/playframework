package play.utils

import akka.NotUsed
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.stream.scaladsl.Flow
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.util.ByteString

// Make private[play] once Play Assets controller is moved out of controllers package
object StreamsUtils {

  // See https://github.com/akka/akka-http/blob/master/akka-http-core/src/main/scala/akka/http/impl/util/StreamUtils.scala#L76
  def sliceBytesTransformer(start: Long, length: Option[Long]): Flow[ByteString, ByteString, NotUsed] = {
    val transformer = new GraphStage[FlowShape[ByteString, ByteString]] {
      val in: Inlet[ByteString] = Inlet("Slicer.in")
      val out: Outlet[ByteString] = Outlet("Slicer.out")

      override val shape: FlowShape[ByteString, ByteString] = FlowShape.of(in, out)
      override def createLogic(inheritedAttributes: Attributes) =

        new GraphStageLogic(shape) with InHandler with OutHandler {

          var toSkip: Long = start
          var remaining: Long = length.getOrElse(Int.MaxValue)

          override def onPush(): Unit = {
            val element = grab(in)
            if (toSkip >= element.length)
              pull(in)
            else {
              val data = element.drop(toSkip.toInt).take(math.min(remaining, Int.MaxValue).toInt)
              remaining -= data.size
              push(out, data)
              if (remaining <= 0) completeStage()
            }
            toSkip -= element.length
          }

          override def onPull() = {
            pull(in)
          }

          setHandlers(in, out, this)
        }
    }

    Flow[ByteString].via(transformer).named("sliceBytes")
  }

}
