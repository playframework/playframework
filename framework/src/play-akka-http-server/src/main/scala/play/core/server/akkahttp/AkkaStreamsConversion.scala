package play.core.server.akkahttp

import akka.stream.scaladsl2._
import org.reactivestreams._
import play.api.libs.iteratee._
import play.api.libs.streams.Streams

/**
 * Conversion of Enumerators into Akka Streams objects. In the future
 * this object will probably end up in the Play-Streams module or in
 * its own module, and we will probably add native Akka Streams support
 * rather than going via Reactive Streams objects. However the Akka
 * Streams API is in flux at the moment so this isn't worth doing yet.
 */
object AkkaStreamsConversion {
  def sourceToEnumerator[A](source: Source[A])(implicit fm: FlowMaterializer): Enumerator[A] = {
    val pubrDrain = PublisherDrain[A]()
    val flowGraph = FlowGraph { implicit b ⇒
      import FlowGraphImplicits._
      source ~> pubrDrain
    }.run()
    val pubr = flowGraph.materializedDrain(pubrDrain)
    Streams.publisherToEnumerator(pubr)
  }
  def enumeratorToSource[T](enum: Enumerator[T], emptyElement: Option[T] = None): Source[T] = {
    val pubr = Streams.enumeratorToPublisher(enum, emptyElement)
    Source(pubr)
  }

}