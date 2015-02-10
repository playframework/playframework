/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing.sird

import org.specs2.mutable.Specification
import play.core.test.FakeRequest

object DslSpec extends Specification {

  "Play routing DSL" should {

    "support extracting GET requests" in {
      "match" in {
        FakeRequest("GET", "/foo") must beLike {
          case GET(_) => ok
        }
      }
      "no match" in {
        FakeRequest("POST", "/foo") must not(beLike {
          case GET(_) => ok
        })
      }
    }

    "support extracting POST requests" in {
      "match" in {
        FakeRequest("POST", "/foo") must beLike {
          case POST(_) => ok
        }
      }
      "no match" in {
        FakeRequest("GET", "/foo") must not(beLike {
          case POST(_) => ok
        })
      }
    }

    "support extracting PUT requests" in {
      "match" in {
        FakeRequest("PUT", "/foo") must beLike {
          case PUT(_) => ok
        }
      }
      "no match" in {
        FakeRequest("POST", "/foo") must not(beLike {
          case PUT(_) => ok
        })
      }
    }

    "support extracting DELETE requests" in {
      "match" in {
        FakeRequest("DELETE", "/foo") must beLike {
          case DELETE(_) => ok
        }
      }
      "no match" in {
        FakeRequest("POST", "/foo") must not(beLike {
          case DELETE(_) => ok
        })
      }
    }

    "support extracting PATCH requests" in {
      "match" in {
        FakeRequest("PATCH", "/foo") must beLike {
          case PATCH(_) => ok
        }
      }
      "no match" in {
        FakeRequest("POST", "/foo") must not(beLike {
          case PATCH(_) => ok
        })
      }
    }

    "support extracting OPTIONS requests" in {
      "match" in {
        FakeRequest("OPTIONS", "/foo") must beLike {
          case OPTIONS(_) => ok
        }
      }
      "no match" in {
        FakeRequest("POST", "/foo") must not(beLike {
          case OPTIONS(_) => ok
        })
      }
    }

    "support extracting HEAD requests" in {
      "match" in {
        FakeRequest("HEAD", "/foo") must beLike {
          case HEAD(_) => ok
        }
      }
      "no match" in {
        FakeRequest("POST", "/foo") must not(beLike {
          case HEAD(_) => ok
        })
      }
    }

    "allow combining method and path matchers" in {
      FakeRequest("GET", "/foo/bar") must beLike {
        case GET(p"/foo/$bar") => bar must_== "bar"
      }
    }
  }
}
