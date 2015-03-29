package play.core.server.akkahttp

import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
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
  def sourceToEnumerator[Out, Mat](source: Source[Out, Mat])(implicit fm: FlowMaterializer): Enumerator[Out] = {
    val pubr: Publisher[Out] = source.runWith(Sink.publisher[Out])
    Streams.publisherToEnumerator(pubr)
  }
  def enumeratorToSource[Out](enum: Enumerator[Out], emptyElement: Option[Out] = None): Source[Out, Unit] = {
    val pubr: Publisher[Out] = Streams.enumeratorToPublisher(enum, emptyElement)
    Source(pubr)
  }

}