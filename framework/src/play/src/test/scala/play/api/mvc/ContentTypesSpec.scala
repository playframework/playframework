package play.api.mvc

import org.specs2.mutable.Specification

object ContentTypesSpec extends Specification {

  import play.api.mvc.BodyParsers.parse.Multipart._

  "FileInfoMatcher" should {

    "parse headers with semicolon inside quotes" in {
      val result = FileInfoMatcher.unapply(Map("content-disposition" -> """form-data; name="document"; filename="semicolon;inside.jpg"""", "content-type" -> "image/jpeg"))
      result must not beEmpty;
      result.get must equalTo(("document", "semicolon;inside.jpg", Option("image/jpeg")));
    }
    
    "parse headers with escaped quote inside quotes" in {
      val result = FileInfoMatcher.unapply(Map("content-disposition" -> """form-data; name="document"; filename="quotes\"\".jpg"""", "content-type" -> "image/jpeg"))
      result must not beEmpty;
      result.get must equalTo(("document", """quotes"".jpg""", Option("image/jpeg")));
    }

  }
}

