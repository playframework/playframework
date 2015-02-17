/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.streams

import play.api.libs.streams.impl._
import org.specs2.mutable.Specification
import scala.concurrent.Future

class StreamsSpec extends Specification {

  "Streams helper interface" should {
    // TODO: Better tests needed, these are only here to ensure Streams compiles
    "create a Publisher from a Future" in {
      val pubr = Streams.futureToPublisher(Future.successful(1))
      pubr must haveClass[FuturePublisher[Int]]
    }
  }

}
