/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import akka.stream.scaladsl._
import org.specs2.mutable.Specification
import play.api.http.ContentTypes
import play.api.mvc.Results

class EventSourceSpec extends Specification {

  import EventSource.Event

  "EventSource event formatter" should {

    "format an event" in {
      Event("foo", None, None).formatted must equalTo("data: foo\n\n")
    }

    "format an event with an id" in {
      Event("foo", Some("42"), None).formatted must equalTo("id: 42\ndata: foo\n\n")
    }

    "format an event with a name" in {
      Event("foo", None, Some("message")).formatted must equalTo("event: message\ndata: foo\n\n")
    }

    "split data by lines" in {
      Event("a\nb").formatted must equalTo("data: a\ndata: b\n\n")
    }

    "support '\\r' as an end of line" in {
      Event("a\rb").formatted must equalTo("data: a\ndata: b\n\n")
    }

    "support '\\r\\n' as an end of line" in {
      Event("a\r\nb").formatted must equalTo("data: a\ndata: b\n\n")
    }

  }

  "EventSource.Event" should {

    "be writeable as a response body using an Akka Source" in {
      val stringSource = Source(Vector("foo", "bar", "baz"))
      val flow = stringSource via EventSource.flow
      val result = Results.Ok.chunked(flow)
      result.body.contentType must beSome(ContentTypes.EVENT_STREAM)
    }

  }

}
