package play.api.test;

import play.api.mvc._

case class FakeHeaders(data: Map[String, Seq[String]] = Map.empty) extends Headers {
  lazy val keys = data.keySet
  def getAll(key: String) = data.get(key).getOrElse(Seq.empty)
}

case class FakeRequest[A](method: String, uri: String, headers: FakeHeaders, body: A) extends Request[A] {

  lazy val path = uri.split('?').take(1).mkString
  lazy val queryString = play.core.parsers.UrlFormEncodedParser.parse(rawQueryString)

  def withHeaders(newHeaders: (String, String)*): FakeRequest[A] = {
    copy(headers = FakeHeaders(
      headers.data ++ newHeaders.groupBy(_._1).mapValues(_.map(_._2))
    ))
  }

  def withUrlFormEncodedBody(data: (String, String)*) = {
    copy(body = AnyContentAsUrlFormEncoded(data.groupBy(_._1).mapValues(_.map(_._2))))
  }

}

object FakeRequest {

  def apply(): FakeRequest[play.api.mvc.AnyContent] = {
    FakeRequest("GET", "/", FakeHeaders(), AnyContentAsEmpty)
  }

  def apply(method: String, path: String): FakeRequest[play.api.mvc.AnyContent] = {
    FakeRequest(method, path, FakeHeaders(), AnyContentAsEmpty)
  }

}

case class FakeApplication(
    override val path: java.io.File = new java.io.File("."),
    override val classloader: ClassLoader = classOf[FakeApplication].getClassLoader,
    val additionalPlugins: Seq[String] = Nil,
    val withoutPlugins: Seq[String] = Nil,
    val additionalConfiguration: Map[String, String] = Map.empty) extends play.api.Application(path, classloader, None, play.api.Mode.Test) {

  override def pluginClasses = {
    additionalPlugins ++ super.pluginClasses.diff(withoutPlugins)
  }

  override def configuration = {
    super.configuration ++ play.api.Configuration.from(additionalConfiguration)
  }

}