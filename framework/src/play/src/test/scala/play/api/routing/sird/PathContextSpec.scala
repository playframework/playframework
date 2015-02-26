/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing.sird

import java.net.{ URL, URI }

import org.specs2.mutable.Specification
import play.core.test.FakeRequest

object PathContextSpec extends Specification {

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

}
