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
    val pathStringInvalid = "/path/to/invalide%2"

    "Unbind Path string as string" in {
      subject.unbind("key", pathStringBinded) must equalTo(pathString)
    }

    "Unbind path with unicode character" in {
      subject.unbind("key", "hello/中文") must equalTo("hello/%E4%B8%AD%E6%96%87")
    }

    "Unbind path in javascript with unicode characters" in {

      import org.mozilla.javascript.Context

      val subject = implicitly[PathBindable[String]]

      try {
        val cx = Context.enter()
        val scope = cx.initStandardObjects()
        cx.evaluateString(scope, "var reverseRoute = " + subject.javascriptUnbind, "javascriptUnbind", 1, null)
        val fct = scope.get("reverseRoute", scope).asInstanceOf[org.mozilla.javascript.Function]
        val result = fct.call(cx, scope, scope, Array("someName", "hello/中文"))
        Context.jsToJava(result, classOf[String]) must beEqualTo("hello/%E4%B8%AD%E6%96%87")
      } finally {
        Context.exit()
      }
    }

    "Bind Path string as string" in {
      subject.bind("key", pathString) must equalTo(Right(pathStringBinded))
    }
    "Fail on unparseable Path string" in {
      subject.bind("key", pathStringInvalid) must equalTo(Left("Cannot parse parameter key as String: Malformed escape pair at index 17: /path/to/invalide%2"))
    }
  }
}
