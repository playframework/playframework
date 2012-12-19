package controllers

import java.util.{Date, Calendar, Locale}
import org.specs2.mutable.Specification
import play.api.test.Helpers._
import play.api.test.WithServer
import scala.collection.JavaConverters._
import play.api.test.Port

class AssetsSpec extends Specification {

  "assets controller" should {

    val startDate = new Date()
    val format = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
    format.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))

    def assetAt(asset: String)(implicit port: Port) = wsCall(routes.Assets.at(asset))

    "add etag and last modified headers" in new WithServer() {
      // -- Etags
      val resp = await(assetAt("stylesheets/main.css").get())
      resp.header("Last-Modified") must beSome
      resp.header("LAST-MODIFIED") must beSome
      resp.header("Etag").get must startWith("\"")
      resp.header("Etag").get must endWith("\"")
      resp.header("ETAG").get must endWith("\"")
      //the map queries are case insensitive, but the underlying map still contains the original headers
      val keys = resp.getAHCResponse.getHeaders.keySet.asScala
      keys must contain("Etag")
      keys must not(contain("ETAG"))
    }

    "return 304 if the if modified since header is after the date modified" in new WithServer() {
      val resp = await(assetAt("stylesheets/main.css").withHeaders("If-Modified-Since"-> format.format(startDate)).get())
      resp.status must_== 304

      // return Date header with 304 response
      resp.header(DATE) must beSome
    }

    "return 200 if the if modified since header is before the date modified" in new WithServer() {
      val cal = Calendar.getInstance()
      val f = new java.io.File("public/stylesheets/main.css")
      cal.setTime(new java.util.Date(f.lastModified))
      cal.add(Calendar.HOUR, -1)
      val earlierDate =  cal.getTime

      val resp = await(assetAt("stylesheets/main.css").withHeaders("If-Modified-Since"-> format.format(earlierDate)).get())
      resp.header("Last-Modified").isDefined must beTrue
      resp.status must_== 200
    }

    "return 200 if the if modified since header can't be parsed" in new WithServer() {
      val resp = await(assetAt("stylesheets/main.css").withHeaders("If-Modified-Since" -> "Not a date").get())
      resp.header("Last-Modified").isDefined must beTrue
      resp.status must_== 200
    }

    "return 200 if the asset is empty" in new WithServer() {
      await(assetAt("empty.txt").get()).status must_== 200
    }
  }
}
