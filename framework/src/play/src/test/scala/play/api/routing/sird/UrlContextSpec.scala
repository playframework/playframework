/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing.sird

import java.net.{ URL, URI }

import org.specs2.mutable.Specification
import play.core.test.FakeRequest

object UrlContextSpec extends Specification {

  "path interpolation" should {

    "match a plain path" in {
      "match" in {
        "/foo/bar" must beLike {
          case p"/foo/bar" => ok
        }
      }
      "no match" in {
        "/foo/notbar" must beLike {
          case p"/foo/bar" => ko
          case _ => ok
        }
      }
    }

    "match a parameterised path" in {
      "match" in {
        "/foo/testing/bar" must beLike {
          case p"/foo/$id/bar" => id must_== "testing"
        }
      }
      "no match" in {
        "/foo/testing/notbar" must beLike {
          case p"/foo/$id/bar" => ko
          case _ => ok
        }
      }
      "decoded" in {
        "/foo/te%24ting/bar" must beLike {
          case p"/foo/$id/bar" => id must_== "te$ting"
        }
      }
    }

    "match a regex path" in {
      "match" in {
        "/foo/1234/bar" must beLike {
          case p"/foo/$id<[0-9]+>/bar" => id must_== "1234"
        }
      }
      "no match" in {
        "/foo/123n4/bar" must beLike {
          case p"/foo/$id<[0-9]+>/bar" => ko
          case _ => ok
        }
      }
      "raw" in {
        "/foo/te%24ting/bar" must beLike {
          case p"/foo/$id<[^/]+>/bar" => id must_== "te%24ting"
        }
      }
    }

    "match a star path" in {
      "match" in {
        "/foo/path/to/something" must beLike {
          case p"/foo/$path*" => path must_== "path/to/something"
        }
      }
      "no match" in {
        "/foo/path/to/something" must beLike {
          case p"/foob/$path*" => ko
          case _ => ok
        }
      }
      "raw" in {
        "/foo/path/to/%24omething" must beLike {
          case p"/foo/$path*" => path must_== "path/to/%24omething"
        }
      }
    }

    "match a path with a nested extractor" in {
      "match" in {
        "/foo/1234/bar" must beLike {
          case p"/foo/${ int(id) }/bar" => id must_== 1234l
        }
      }
      "no match" in {
        "/foo/testing/bar" must beLike {
          case p"/foo/${ int(id) }/bar" => ko
          case _ => ok
        }
      }
    }

    "match a request" in {
      FakeRequest("GET", "/foo/testing/bar") must beLike {
        case p"/foo/$id/bar" => id must_== "testing"
      }
    }

    "match a uri" in {
      URI.create("/foo/testing/bar") must beLike {
        case p"/foo/$id/bar" => id must_== "testing"
      }
    }

    "match a url" in {
      new URL("http://example.com/foo/testing/bar") must beLike {
        case p"/foo/$id/bar" => id must_== "testing"
      }
    }
  }

  "query string interpolation" should {
    def qs(params: (String, String)*) = {
      params.groupBy(_._1).mapValues(_.map(_._2))
    }

    "allow required parameter extraction" in {
      "match" in {
        qs("foo" -> "bar") must beLike {
          case q"foo=$foo" => foo must_== "bar"
        }
      }
      "no match" in {
        qs("foo" -> "bar") must beLike {
          case q"notfoo=$foo" => ko
          case _ => ok
        }
      }
    }

    "allow optional parameter extraction" in {
      "existing" in {
        qs("foo" -> "bar") must beLike {
          case q_o"foo=$foo" => foo must beSome("bar")
        }
      }
      "not existing" in {
        qs("foo" -> "bar") must beLike {
          case q_o"notfoo=$foo" => foo must beNone
        }
      }
    }

    "allow seq parameter extraction" in {
      "none" in {
        qs() must beLike {
          case q_s"foo=$foo" => foo must beEmpty
        }
      }
      "one" in {
        qs("foo" -> "bar") must beLike {
          case q_s"foo=$foo" => Seq("bar") must_== Seq("bar")
        }
      }
      "many" in {
        qs("foo" -> "bar1", "foo" -> "bar2", "foo" -> "bar3") must beLike {
          case q_s"foo=$foos" => foos must_== Seq("bar1", "bar2", "bar3")
        }
      }
    }

  }

}
