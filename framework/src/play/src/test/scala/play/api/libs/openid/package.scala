package play.api.libs

import java.net.URL

package object openid {
  implicit def stringToSeq(s: String): Seq[String] = Seq(s)

  trait RichUrl[A] {
    def hostAndPath:String
  }

  implicit def urlToRichUrl(url:URL) = new RichUrl[URL] {
    def hostAndPath = new URL(url.getProtocol, url.getHost, url.getPort, url.getPath).toExternalForm
  }
}