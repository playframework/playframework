/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import java.time.Instant

import org.specs2.mutable.Specification
import play.api.http.{ DefaultFileMimeTypesProvider, FileMimeTypes, FileMimeTypesConfiguration }
import play.api.mvc.ResponseHeader
import play.utils.InvalidUriEncodingException

class AssetsSpec extends Specification {

  "Assets controller" should {

    "look up assets with the correct resource name" in {
      Assets.resourceNameAt("a", "") must beNone
      Assets.resourceNameAt("a", "b") must beNone
      Assets.resourceNameAt("a", "/") must beNone
      Assets.resourceNameAt("a", "/b") must beNone
      Assets.resourceNameAt("a", "/b/c") must beNone
      Assets.resourceNameAt("a", "/b/") must beNone
      Assets.resourceNameAt("/a", "") must beSome("/a/")
      Assets.resourceNameAt("/a", "b") must beSome("/a/b")
      Assets.resourceNameAt("/a", "/") must beSome("/a/")
      Assets.resourceNameAt("/a", "/b") must beSome("/a/b")
      Assets.resourceNameAt("/a", "/b/c") must beSome("/a/b/c")
      Assets.resourceNameAt("/a", "/b/") must beSome("/a/b/")
    }

    "not look up assets with Windows file separators" in {
      Assets.resourceNameAt("a\\z", "") must beNone
      Assets.resourceNameAt("a\\z", "b") must beNone
      Assets.resourceNameAt("a\\z", "/") must beNone
      Assets.resourceNameAt("a\\z", "/b") must beNone
      Assets.resourceNameAt("a\\z", "/b/c") must beNone
      Assets.resourceNameAt("a\\z", "/b/") must beNone
      Assets.resourceNameAt("/a\\z", "") must beSome("/a\\z/")
      Assets.resourceNameAt("/a\\z", "b") must beSome("/a\\z/b")
      Assets.resourceNameAt("/a\\z", "/") must beSome("/a\\z/")
      Assets.resourceNameAt("/a\\z", "/b") must beSome("/a\\z/b")
      Assets.resourceNameAt("/a\\z", "/b/c") must beSome("/a\\z/b/c")
      Assets.resourceNameAt("/a\\z", "/b/") must beSome("/a\\z/b/")
      Assets.resourceNameAt("\\a\\z", "") must beNone
      Assets.resourceNameAt("\\a\\z", "b") must beNone
      Assets.resourceNameAt("\\a\\z", "/") must beNone
      Assets.resourceNameAt("\\a\\z", "/b") must beNone
      Assets.resourceNameAt("\\a\\z", "/b/c") must beNone
      Assets.resourceNameAt("\\a\\z", "/b/") must beNone
      Assets.resourceNameAt("x:\\a\\z", "") must beNone
      Assets.resourceNameAt("x:\\a\\z", "b") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/b") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/b/c") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/b/") must beNone
    }

    "not look up assets with Windows resource path separators encoded for Windows" in {
      // %5C is "\" URL encoded
      Assets.resourceNameAt("a%5Cz", "") must beNone
      Assets.resourceNameAt("a%5Cz", "b") must beNone
      Assets.resourceNameAt("a%5Cz", "/") must beNone
      Assets.resourceNameAt("a%5Cz", "/b") must beNone
      Assets.resourceNameAt("a%5Cz", "/b/c") must beNone
      Assets.resourceNameAt("a%5Cz", "/b/") must beNone
      Assets.resourceNameAt("/a%5Cz", "") must beSome("/a%5Cz/")
      Assets.resourceNameAt("/a%5Cz", "b") must beSome("/a%5Cz/b")
      Assets.resourceNameAt("/a%5Cz", "/") must beSome("/a%5Cz/")
      Assets.resourceNameAt("/a%5Cz", "/b") must beSome("/a%5Cz/b")
      Assets.resourceNameAt("/a%5Cz", "/b/c") must beSome("/a%5Cz/b/c")
      Assets.resourceNameAt("/a%5Cz", "/b/") must beSome("/a%5Cz/b/")
      Assets.resourceNameAt("%5Ca%5Cz", "") must beNone
      Assets.resourceNameAt("%5Ca%5Cz", "b") must beNone
      Assets.resourceNameAt("%5Ca%5Cz", "/") must beNone
      Assets.resourceNameAt("%5Ca%5Cz", "/b") must beNone
      Assets.resourceNameAt("%5Ca%5Cz", "/b/c") must beNone
      Assets.resourceNameAt("%5Ca%5Cz", "/b/") must beNone
      Assets.resourceNameAt("x:%5Ca%5Cz", "") must beNone
      Assets.resourceNameAt("x:%5Ca%5Cz", "b") must beNone
      Assets.resourceNameAt("x:%5Ca%5Cz", "/") must beNone
      Assets.resourceNameAt("x:%5Ca%5Cz", "/b") must beNone
      Assets.resourceNameAt("x:%5Ca%5Cz", "/b/c") must beNone
      Assets.resourceNameAt("x:%5Ca%5Cz", "/b/") must beNone
    }

    "not look up assets with Windows resource path separators encoded for Linux" in {
      // %2F is "/" URL encoded
      Assets.resourceNameAt("a%2Fz", "") must beNone
      Assets.resourceNameAt("a%2Fz", "b") must beNone
      Assets.resourceNameAt("a%2Fz", "/") must beNone
      Assets.resourceNameAt("a%2Fz", "/b") must beNone
      Assets.resourceNameAt("a%2Fz", "/b/c") must beNone
      Assets.resourceNameAt("a%2Fz", "/b/") must beNone
      Assets.resourceNameAt("/a%2Fz", "") must beSome("/a%2Fz/")
      Assets.resourceNameAt("/a%2Fz", "b") must beSome("/a%2Fz/b")
      Assets.resourceNameAt("/a%2Fz", "/") must beSome("/a%2Fz/")
      Assets.resourceNameAt("/a%2Fz", "/b") must beSome("/a%2Fz/b")
      Assets.resourceNameAt("/a%2Fz", "/b/c") must beSome("/a%2Fz/b/c")
      Assets.resourceNameAt("/a%2Fz", "/b/") must beSome("/a%2Fz/b/")
      Assets.resourceNameAt("%2Fa%2Fz", "") must beNone
      Assets.resourceNameAt("%2Fa%2Fz", "b") must beNone
      Assets.resourceNameAt("%2Fa%2Fz", "/") must beNone
      Assets.resourceNameAt("%2Fa%2Fz", "/b") must beNone
      Assets.resourceNameAt("%2Fa%2Fz", "/b/c") must beNone
      Assets.resourceNameAt("%2Fa%2Fz", "/b/") must beNone
      Assets.resourceNameAt("x:%2Fa%2Fz", "") must beNone
      Assets.resourceNameAt("x:%2Fa%2Fz", "b") must beNone
      Assets.resourceNameAt("x:%2Fa%2Fz", "/") must beNone
      Assets.resourceNameAt("x:%2Fa%2Fz", "/b") must beNone
      Assets.resourceNameAt("x:%2Fa%2Fz", "/b/c") must beNone
      Assets.resourceNameAt("x:%2Fa%2Fz", "/b/") must beNone
    }

    "not look up assets with Windows resource filename separators encoded for Windows" in {
      // %5C is "\" URL encoded
      Assets.resourceNameAt("a\\z", "") must beNone
      Assets.resourceNameAt("a\\z", "b") must beNone
      Assets.resourceNameAt("a\\z", "%5C") must beNone
      Assets.resourceNameAt("a\\z", "%5Cb") must beNone
      Assets.resourceNameAt("a\\z", "%5Cbc") must beNone
      Assets.resourceNameAt("a\\z", "%5Cb%5C") must beNone
      Assets.resourceNameAt("/a\\z", "") must beSome("/a\\z/")
      Assets.resourceNameAt("/a\\z", "b") must beSome("/a\\z/b")
      Assets.resourceNameAt("/a\\z", "%5C") must beSome("/a\\z/\\")
      Assets.resourceNameAt("/a\\z", "%5Cb") must beSome("/a\\z/\\b")
      Assets.resourceNameAt("/a\\z", "%5Cb%5Cc") must beSome("/a\\z/\\b\\c")
      Assets.resourceNameAt("/a\\z", "/b/") must beSome("/a\\z/b/")
      Assets.resourceNameAt("\\a\\z", "") must beNone
      Assets.resourceNameAt("\\a\\z", "b") must beNone
      Assets.resourceNameAt("\\a\\z", "/") must beNone
      Assets.resourceNameAt("\\a\\z", "/b") must beNone
      Assets.resourceNameAt("\\a\\z", "/b/c") must beNone
      Assets.resourceNameAt("\\a\\z", "/b/") must beNone
      Assets.resourceNameAt("x:\\a\\z", "") must beNone
      Assets.resourceNameAt("x:\\a\\z", "b") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/b") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/b/c") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/b/") must beNone
    }

    "not look up assets with Windows resource filename separators encoded for Linux" in {
      // %2F is "/" URL encoded
      Assets.resourceNameAt("a\\z", "") must beNone
      Assets.resourceNameAt("a\\z", "b") must beNone
      Assets.resourceNameAt("a\\z", "%2F") must beNone
      Assets.resourceNameAt("a\\z", "%2Fb") must beNone
      Assets.resourceNameAt("a\\z", "%2Fbc") must beNone
      Assets.resourceNameAt("a\\z", "%2Fb%2F") must beNone
      Assets.resourceNameAt("/a\\z", "") must beSome("/a\\z/")
      Assets.resourceNameAt("/a\\z", "b") must beSome("/a\\z/b")
      Assets.resourceNameAt("/a\\z", "%2F") must beSome("/a\\z/")
      Assets.resourceNameAt("/a\\z", "%2Fb") must beSome("/a\\z/b")
      Assets.resourceNameAt("/a\\z", "%2Fb%2Fc") must beSome("/a\\z/b/c")
      Assets.resourceNameAt("/a\\z", "/b/") must beSome("/a\\z/b/")
      Assets.resourceNameAt("\\a\\z", "") must beNone
      Assets.resourceNameAt("\\a\\z", "b") must beNone
      Assets.resourceNameAt("\\a\\z", "/") must beNone
      Assets.resourceNameAt("\\a\\z", "/b") must beNone
      Assets.resourceNameAt("\\a\\z", "/b/c") must beNone
      Assets.resourceNameAt("\\a\\z", "/b/") must beNone
      Assets.resourceNameAt("x:\\a\\z", "") must beNone
      Assets.resourceNameAt("x:\\a\\z", "b") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/b") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/b/c") must beNone
      Assets.resourceNameAt("x:\\a\\z", "/b/") must beNone
    }

    "look up assets without percent-decoding the base path" in {
      Assets.resourceNameAt(" ", "x") must beNone
      Assets.resourceNameAt("/1 + 2 = 3", "x") must beSome("/1 + 2 = 3/x")
      Assets.resourceNameAt("/1%20+%202%20=%203", "x") must beSome("/1%20+%202%20=%203/x")
    }

    "look up assets with percent-encoded resource paths" in {
      Assets.resourceNameAt("/x", "1%20+%202%20=%203") must beSome("/x/1 + 2 = 3")
      Assets.resourceNameAt("/x", "foo%20bar.txt") must beSome("/x/foo bar.txt")
      Assets.resourceNameAt("/x", "foo+bar%3A%20baz.txt") must beSome("/x/foo+bar: baz.txt")
    }

    "look up assets with percent-encoded file separators" in {
      Assets.resourceNameAt("/x", "%2f") must beSome("/x/")
      Assets.resourceNameAt("/x", "a%2fb") must beSome("/x/a/b")
      Assets.resourceNameAt("/x", "a/%2fb") must beSome("/x/a/b")
    }

    "fail when looking up assets with invalid chars in the URL" in {
      Assets.resourceNameAt("a", "|") must throwAn[InvalidUriEncodingException]
      Assets.resourceNameAt("a", "hello world") must throwAn[InvalidUriEncodingException]
      Assets.resourceNameAt("a", "b/[c]/d") must throwAn[InvalidUriEncodingException]
    }

    "look up assets even if the file path is a valid URI" in {
      Assets.resourceNameAt("/a", "http://localhost/x") must beSome("/a/http:/localhost/x")
      Assets.resourceNameAt("/a", "//localhost/x") must beSome("/a/localhost/x")
      Assets.resourceNameAt("/a", "../") must beNone
    }

    "look up assets with dot-segments in the path" in {
      Assets.resourceNameAt("/a/b", "./c/d") must beSome("/a/b/./c/d")
      Assets.resourceNameAt("/a/b", "c/./d") must beSome("/a/b/c/./d")
      Assets.resourceNameAt("/a/b", "../b/c/d") must beSome("/a/b/../b/c/d")
      Assets.resourceNameAt("/a/b", "c/../d") must beSome("/a/b/c/../d")
      Assets.resourceNameAt("/a/b", "c/d/..") must beSome("/a/b/c/d/..")
      Assets.resourceNameAt("/a/b", "c/d/../../x") must beSome("/a/b/c/d/../../x")
      Assets.resourceNameAt("/a/b", "../../a/b/c/d") must beSome("/a/b/../../a/b/c/d")
    }

    "not look up assets with dot-segments that escape the parent path" in {
      Assets.resourceNameAt("/a/b", "..") must beNone
      Assets.resourceNameAt("/a/b", "../") must beNone
      Assets.resourceNameAt("/a/b", "../c") must beNone
      Assets.resourceNameAt("/a/b", "../../c/d") must beNone
    }

    "not look up assets with dot-segments that escape the parent path with a encoded separator for Windows" in {
      // %5C is "\" URL encoded
      Assets.resourceNameAt("/a/b", "..") must beNone
      Assets.resourceNameAt("/a/b", "..%5C") must beNone
      Assets.resourceNameAt("/a/b", "..%5Cc") must beNone
      Assets.resourceNameAt("/a/b", "../..%5Cc%5Cd") must beNone
      Assets.resourceNameAt("/a/b", "..%5C..%5Cc%5Cd") must beNone
    }

    "not look up assets with dot-segments that escape the parent path with a encoded separator for Linux" in {
      // %2F is "\" URL encoded
      Assets.resourceNameAt("/a/b", "..") must beNone
      Assets.resourceNameAt("/a/b", "..%2F") must beNone
      Assets.resourceNameAt("/a/b", "..%2Fc") must beNone
      Assets.resourceNameAt("/a/b", "../..%2Fc%2Fd") must beNone
      Assets.resourceNameAt("/a/b", "..%2F..%2Fc%2Fd") must beNone
    }

    "use the unescaped path when finding the last modified date of an asset" in {
      val url = this.getClass.getClassLoader.getResource("file withspace.css")
      implicit val fileMimeTypes: FileMimeTypes = new DefaultFileMimeTypesProvider(FileMimeTypesConfiguration()).get

      val assetInfo = new AssetInfo("file withspace.css", url, Seq(), None, AssetsConfiguration(), fileMimeTypes)
      val lastModified = ResponseHeader.httpDateFormat.parse(assetInfo.lastModified.get)
      // If it uses the escaped path, the file won't be found, and so last modified will be 0
      Instant.from(lastModified).toEpochMilli must_!= 0
    }
  }
}
