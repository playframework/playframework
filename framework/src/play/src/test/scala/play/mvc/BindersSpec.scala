package play.api.mvc

import java.util.UUID
import org.specs2.mutable._

object BindersSpec extends Specification {

  val uuid = UUID.randomUUID

  "UUID path binder" should {
    val subject = implicitly[PathBindable[UUID]]

    "Unbind UUID as string" in {
      subject.unbind("key", uuid) must be_==(uuid.toString)
    }
    "Bind parameter to UUID" in {
      subject.bind("key", uuid.toString) must be_==(Right(uuid))
    }
    "Fail on unparseable UUID" in {
      subject.bind("key", "bad-uuid") must be_==(Left("Cannot parse parameter key as UUID: Invalid UUID string: bad-uuid"))
    }
  }

  "UUID query string binder" should {
    val subject = implicitly[QueryStringBindable[UUID]]

    "Unbind UUID as string" in {
      subject.unbind("key", uuid) must be_==("key=" + uuid.toString)
    }
    "Bind parameter to UUID" in {
      subject.bind("key", Map("key" -> Seq(uuid.toString))) must be_==(Some(Right(uuid)))
    }
    "Fail on unparseable UUID" in {
      subject.bind("key", Map("key" -> Seq("bad-uuid"))) must be_==(Some(Left("Cannot parse parameter key as UUID: Invalid UUID string: bad-uuid")))
    }
  }

  "URL Path string binder" should {
    val subject = implicitly[PathBindable[String]]
    val pathString = "/path/to/some%20file"
    val pathStringBinded = "/path/to/some file"

    "Unbind Path string as string" in {
      subject.unbind("key", pathString) must equalTo(pathString)
    }
    "Bind Path string as string without any decoding" in {
      subject.bind("key", pathString) must equalTo(Right(pathString))
    }
  }
}
