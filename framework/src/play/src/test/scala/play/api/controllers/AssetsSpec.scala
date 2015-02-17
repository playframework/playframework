/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import org.specs2.mutable.Specification
import play.utils.InvalidUriEncodingException

object AssetsSpec extends Specification {

  "Assets controller" should {

    "look up assets with the the correct resource name" in {
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

    "use the unescaped path when finding the last modified date of an asset" in {
      val url = AssetsSpec.getClass.getClassLoader.getResource("file withspace.css")
      val assetInfo = new AssetInfo("file withspace.css", url, None, None)
      val lastModified = AssetInfo.dateFormat.parseDateTime(assetInfo.lastModified.get)
      // If it uses the escaped path, the file won't be found, and so last modified will be 0
      lastModified.toDate.getTime must_!= 0
    }
  }
}
