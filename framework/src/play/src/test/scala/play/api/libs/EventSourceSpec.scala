package play.api.libs

import org.specs2.mutable.Specification
import play.api.http.{ ContentTypes, HeaderNames }
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Results

object EventSourceSpec extends Specification {

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
    "be writeable as a response body" in {
      val result = Results.Ok.chunked(Enumerator("foo", "bar", "baz") &> EventSource())
      result.header.headers.get(HeaderNames.CONTENT_TYPE) must beSome(ContentTypes.EVENT_STREAM)
    }
  }

}
