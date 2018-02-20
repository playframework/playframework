/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.utils

import org.specs2.mutable.Specification

class HttpHeaderParameterEncodingSpec extends Specification {
  "HttpHeaderParameterEncoding.encode" should {

    "support RFC6266 examples" in {
      // Examples taken from https://tools.ietf.org/html/rfc6266#section-5
      // with some modifications.

      "encode (filename, example.html) with a regular parameter only" in {
        HttpHeaderParameterEncoding.encode("filename", "example.html") must_== "filename=\"example.html\""
      }

      "encode (filename, € rates) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("filename", "€ rates") must_== "filename=\"? rates\"; filename*=utf-8''%e2%82%ac%20rates"
      }
    }

    "support RFC5987 example" in {
      // Example taken from https://tools.ietf.org/html/rfc5987#section-4.2
      // with some modifications.

      "Encode (title, € exchange rates) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("title", "€ exchange rates") must_== "title=\"? exchange rates\"; title*=utf-8''%e2%82%ac%20exchange%20rates"
      }
    }

    "support examples from http://greenbytes.de/tech/tc2231/" in {
      // Examples taken from http://greenbytes.de/tech/tc2231/
      // with some modifications.
      "encode (filename, foo-ä-€.html) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("filename", "foo-ä-€.html") must_== "filename=\"foo-?-?.html\"; filename*=utf-8''foo-%c3%a4-%e2%82%ac.html"
      }
      "encode (filename, foo-ä.html) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("filename", "foo-ä.html") must_== "filename=\"foo-?.html\"; filename*=utf-8''foo-%c3%a4.html"
      }
      "encode (filename, A-%41.html) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("filename", "A-%41.html") must_== "filename=\"A-?41.html\"; filename*=utf-8''A-%2541.html"
      }
      "encode (filename, \\foo.html) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("filename", "\\foo.html") must_== "filename=\"?foo.html\"; filename*=utf-8''%5cfoo.html"
      }
      "encode (filename, Here's a semicolon;.html) with a regular parameter only" in {
        HttpHeaderParameterEncoding.encode("filename", "Here's a semicolon;.html") must_== "filename=\"Here's a semicolon;.html\""
      }
    }

    "support examples from https://github.com/jshttp/content-disposition" in {
      // Examples taken from https://github.com/jshttp/content-disposition/blob/master/test/test.js
      // with some modifications.
      "encode (filename, планы.pdf) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("filename", "планы.pdf") must_== "filename=\"?????.pdf\"; filename*=utf-8''%d0%bf%d0%bb%d0%b0%d0%bd%d1%8b.pdf"
      }
      "encode (filename, £ and € rates.pdf) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("filename", "£ and € rates.pdf") must_== "filename=\"? and ? rates.pdf\"; filename*=utf-8''%c2%a3%20and%20%e2%82%ac%20rates.pdf"
      }
      "encode (filename, €\\'*%().pdf) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("filename", "€'*%().pdf") must_== "filename=\"?'*?().pdf\"; filename*=utf-8''%e2%82%ac%27%2a%25%28%29.pdf"
      }
      "encode (filename, the%20plans.pdf) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("filename", "the%20plans.pdf") must_== "filename=\"the?20plans.pdf\"; filename*=utf-8''the%2520plans.pdf"
      }
      "encode (filename, Here's a semicolon;.html) with a regular parameter only" in {
        HttpHeaderParameterEncoding.encode("filename", "Here's a semicolon;.html") must_== "filename=\"Here's a semicolon;.html\""
      }
    }

    "support misc examples" in {
      "encode (filename, README.md) with a regular parameter only" in {
        // Example from https://github.com/playframework/playframework/issues/7501
        HttpHeaderParameterEncoding.encode("filename", "README.md") must_== "filename=\"README.md\""
      }
      "encode (filename, READM•.md) with both regular and extended parameters" in {
        // Tested to give correct filename when downloading a file on Chrome 58
        HttpHeaderParameterEncoding.encode("filename", "READM•.md") must_== "filename=\"READM?.md\"; filename*=utf-8''READM%e2%80%a2.md"
      }
      "encode (filename, test.tmp) with a regular parameter only" in {
        // Example from https://github.com/playframework/playframework/pull/6042
        HttpHeaderParameterEncoding.encode("filename", "test.tmp") must_== "filename=\"test.tmp\""
      }
      "encode (filename, video.mp4) with a regular parameter only" in {
        // Example from https://github.com/playframework/playframework/pull/6042
        HttpHeaderParameterEncoding.encode("filename", "video.mp4") must_== "filename=\"video.mp4\""
      }
      "encode (filename, 测 试.tmp) with both regular and extended parameters" in {
        // Example from https://github.com/playframework/playframework/pull/6042
        HttpHeaderParameterEncoding.encode("filename", "测 试.tmp") must_== "filename=\"? ?.tmp\"; filename*=utf-8''%e6%b5%8b%20%e8%af%95.tmp"
      }
      "encode (filename, Museum 博物馆.jpg) with both regular and extended parameters" in {
        // Example from https://stackoverflow.com/questions/11302361/handling-filename-parameters-with-spaces-via-rfc-5987-results-in-in-filenam
        HttpHeaderParameterEncoding.encode("filename", "Museum 博物馆.jpg") must_== "filename=\"Museum ???.jpg\"; filename*=utf-8''Museum%20%e5%8d%9a%e7%89%a9%e9%a6%86.jpg"
      }
    }

    "handle some special cases properly" in {
      "encode (foo, <empty>) with a regular parameter only" in {
        HttpHeaderParameterEncoding.encode("foo", "") must_== "foo=\"\""
      }
      "encode (foo, <space>) with a regular parameter only" in {
        HttpHeaderParameterEncoding.encode("foo", " ") must_== "foo=\" \""
      }
      "encode (foo, =) with a regular parameter only" in {
        HttpHeaderParameterEncoding.encode("foo", "=") must_== "foo=\"=\""
      }
      "encode (foo, ') with a regular parameter only" in {
        HttpHeaderParameterEncoding.encode("foo", "'") must_== "foo=\"'\""
      }
      "encode (foo, %) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("foo", "%") must_== "foo=\"?\"; foo*=utf-8''%25"
      }
      "encode (foo, \") with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("foo", "\"") must_== "foo=\"?\"; foo*=utf-8''%22"
      }
      "encode (foo, ;) with a regular parameter only" in {
        HttpHeaderParameterEncoding.encode("foo", ";") must_== "foo=\";\""
      }
      "encode (foo, 0x80) with both regular and extended parameters" in {
        HttpHeaderParameterEncoding.encode("foo", 0x80.toChar.toString) must_== "foo=\"?\"; foo*=utf-8''%c2%80"
      }
    }
  }
}