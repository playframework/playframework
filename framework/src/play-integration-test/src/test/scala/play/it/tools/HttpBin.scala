/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package play.it.tools

import play.api.test.FakeApplication

import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.Writeable._

import play.api.libs.json._

import play.filters.gzip.GzipFilter

import scala.util.matching.Regex

/**
 * This is a reimplementation of the excellent httpbin.org service
 * by Kenneth Reitz
 *
 * Motivation: We couldn't use httpbin.org directly for our CI.
 */
object HttpBinApplication {
  private def route(verb: String, path: Regex)(handler: String => EssentialAction): PartialFunction[(String, String), Handler] = {
    case (v, path(p)) if v == verb => handler(p)
  }

  private def route(verb: String, path: String)(handler: EssentialAction): PartialFunction[(String, String), Handler] = {
    case (v, p) if v == verb && p == path => handler
  }

  private val requestHeaderWriter = new Writes[RequestHeader] {
    def writes(r: RequestHeader): JsValue = Json.obj(
      "origin" -> r.remoteAddress,
      "url" -> "",
      "args" -> r.queryString.mapValues(_.head),
      "headers" -> r.headers.toSimpleMap
    )
  }

  private def requestWriter[A] = new Writes[Request[A]] {
    def writes(r: Request[A]): JsValue =
      requestHeaderWriter.writes(r).as[JsObject] ++
        Json.obj(
          "json" -> JsNull,
          "data" -> "",
          "form" -> JsObject(Nil)
        ) ++ (r.body match {
            // Json Body
            case e: JsValue =>
              Json.obj("json" -> e)
            // X-WWW-Form-Encoded
            case f: Map[String, Seq[String]] @unchecked =>
              Json.obj("form" -> JsObject(f.mapValues(x => JsString(x.mkString(", "))).toSeq))
            // Anything else
            case b =>
              Json.obj("data" -> JsString(b.toString))
          })
  }

  val getIp = route("GET", "/ip") {
    Action { request =>
      Ok(Json.obj("origin" -> request.remoteAddress))
    }
  }

  val getUserAgent = route("GET", "/user-agent") {
    Action { request =>
      Ok(Json.obj("user-agent" -> request.headers.get("User-Agent")))
    }
  }

  val getHeaders = route("GET", "/headers") {
    Action { request =>
      Ok(Json.obj("headers" -> request.headers.toSimpleMap))
    }
  }

  val get = route("GET", "/get") {
    Action { request =>
      Ok(requestHeaderWriter.writes(request))
    }
  }

  val patch = route("PATCH", "/patch") {
    Action { request =>
      Ok(requestWriter.writes(request))
    }
  }

  val post = route("POST", "/post") {
    Action { request =>
      Ok(requestWriter.writes(request))
    }
  }

  val put = route("PUT", "/put") {
    Action { request =>
      Ok(requestWriter.writes(request))
    }
  }

  val delete = route("DELETE", "/delete") {
    Action { request =>
      Ok(requestHeaderWriter.writes(request))
    }
  }

  private val gzipFilter = new GzipFilter()

  val gzip = Seq("GET", "PATCH", "POST", "PUT", "DELETE").map { method =>
    route("GET", "/gzip") {
      gzipFilter(Action { request =>
        Ok(requestHeaderWriter.writes(request).as[JsObject] ++ Json.obj("gzipped" -> true, "method" -> method))
      })
    }
  }.reduceLeft((a, b) => a.orElse(b))

  val status = route("GET", "^/status/([0-9]+)$".r) { param =>
    Action {
      val code = param.toInt
      Results.Status(code)
    }
  }

  val responseHeaders = route("GET", "/response-header") {
    Action { request =>
      Ok("").withHeaders(request.queryString.mapValues(_.mkString(",")).toSeq: _*)
    }
  }

  val redirect = route("GET", "/redirect/0") {
    Action {
      Redirect("/get")
    }
  }.orElse(route("GET", "^/redirect/([0-9]+)".r) { param =>
    Action {
      Redirect("redirect/" + param)
    }
  })

  val redirectTo = route("GET", "/redirect-to") {
    Action { request =>
      request.queryString.get("url").map { u =>
        Redirect(u.head)
      }.getOrElse {
        BadRequest("")
      }
    }
  }

  val cookies = route("GET", "/cookies") {
    Action { request =>
      Ok(Json.obj("cookies" -> JsObject(request.cookies.toSeq.map(x => x.name -> JsString(x.value)))))
    }
  }

  val cookiesSet = route("GET", "/cookies/set") {
    Action { request =>
      Redirect("/cookies").withCookies(request.queryString.mapValues(_.head).toSeq.map {
        case (k, v) => Cookie(k, v)
      }: _*)
    }
  }

  val cookiesDelete = route("GET", "/cookies/delete") {
    Action { request =>
      Redirect("/cookies").discardingCookies(request.queryString.keys.toSeq.map(DiscardingCookie(_)): _*)
    }
  }

  val basicAuth = route("GET", "^/basic-auth/([^/]+/[^/]+)".r) { param =>
    val username = param.split("/")(0)
    val password = param.split("/")(1)
    Action { request =>
      request.headers.get("Authorization").flatMap { authorization =>
        authorization.split(" ").drop(1).headOption.filter { encoded =>
          new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
            case u :: p :: Nil if u == username && password == p => true
            case _ => false
          }
        }.map(_ => Ok(Json.obj("authenticated" -> true)))
      }.getOrElse {
        Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured"""")
      }
    }
  }

  val stream = route("GET", "^/stream/([0-9]+)".r) { param =>
    import play.api.libs.iteratee.Enumerator
    Action { request =>
      val body = requestHeaderWriter.writes(request).as[JsObject]

      val content = 0.to(param.toInt).map { index =>
        body ++ Json.obj("id" -> index)
      }

      Ok.chunked(Enumerator(content: _*)).as("application/json")
    }
  }

  val delay = route("GET", "/delay/([0-9]+)".r) { param =>
    Action.async { request =>
      import scala.concurrent.Await
      import scala.concurrent.Promise
      import scala.concurrent.Future
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.duration._
      import scala.util.Try
      val p = Promise[Result]()

      Future {
        Try {
          Await.result(p.future, Duration(param.toInt, SECONDS))
        }.getOrElse {
          p.success(Ok(requestWriter.writes(request)))
        }
      }

      p.future
    }
  }

  val html = route("GET", "/html") {
    Action {
      Ok("""
<!DOCTYPE html>
<html>
  <head>
  </head>
  <body>
      <h1>Herman Melville - Moby-Dick</h1>

      <div>
        <p>
          Availing himself of the mild, summer-cool weather that now reigned in these latitudes,
          and in preparation for the peculiarly active pursuits shortly to be anticipated, Perth,
          the begrimed, blistered old blacksmith, had not removed his portable forge to the hold
          again, after concluding his contributory work for Ahab's leg, but still retained it on
          deck, fast lashed to ringbolts by the foremast; being now almost incessantly invoked
          by the headsmen, and harpooneers, and bowsmen to do some little job for them; altering,
          or repairing, or new shaping their various weapons and boat furniture. Often he would
          be surrounded by an eager circle, all waiting to be served; holding boat-spades,
          pike-heads, harpoons, and lances, and jealously watching his every sooty movement, as
          he toiled. Nevertheless, this old man's was a patient hammer wielded by a patient arm.
          No murmur, no impatience, no petulance did come from him. Silent, slow, and solemn;
          bowing over still further his chronically broken back, he toiled away, as if toil were
          life itself, and the heavy beating of his hammer the heavy beating of his heart. And
          so it was.—Most miserable! A peculiar walk in this old man, a certain slight but
          painful appearing yawing in his gait, had at an early period of the voyage excited the
          curiosity of the mariners. And to the importunity of their persisted questionings he
          had finally given in; and so it came to pass that every one now knew the shameful
          story of his wretched fate. Belated, and not innocently, one bitter winter's midnight,
          on the road running between two country towns, the blacksmith half-stupidly felt the
          deadly numbness stealing over him, and sought refuge in a leaning, dilapidated barn.
          The issue was, the loss of the extremities of both feet. Out of this revelation,
          part by part, at last came out the four acts of the gladness, and the one long, and as
          yet uncatastrophied fifth act of the grief of his life's drama. He was an old man, who,
          at the age of nearly sixty, had postponedly encountered that thing in sorrow's
          technicals called ruin. He had been an artisan of famed excellence, and with plenty to
          do; owned a house and garden; embraced a youthful, daughter-like, loving wife, and
          three blithe, ruddy children; every Sunday went to a cheerful-looking church, planted
          in a grove. But one night, under cover of darkness, and further concealed in a most
          cunning disguisement, a desperate burglar slid into his happy home, and robbed them
          all of everything. And darker yet to tell, the blacksmith himself did ignorantly
          conduct this burglar into his family's heart. It was the Bottle Conjuror! Upon the
          opening of that fatal cork, forth flew the fiend, and shrivelled up his home. Now,
          for prudent, most wise, and economic reasons, the blacksmith's shop was in the
          basement of his dwelling, but with a separate entrance to it; so that always had
          the young and loving healthy wife listened with no unhappy nervousness, but with
          vigorous pleasure, to the stout ringing of her young-armed old husband's hammer;
          whose reverberations, muffled by passing through the floors and walls, came up to
          her, not unsweetly, in her nursery; and so, to stout Labor's iron lullaby, the
          blacksmith's infants were rocked to slumber. Oh, woe on woe! Oh, Death, why canst
          thou not sometimes be timely? Hadst thou taken this old blacksmith to thyself ere
          his full ruin came upon him, then had the young widow had a delicious grief, and
          her orphans a truly venerable, legendary sire to dream of in their after years; and
          all of them a care-killing competency.
        </p>
      </div>
  </body>
</html>""").as("text/html")
    }
  }

  val robots = route("GET", "/robots.txt") {
    Action {
      Ok("User-agent: *\nDisallow: /deny")
    }
  }.orElse(route("GET", "/deny") {
    Action {
      Ok("""
          .-''''''-.
        .' _      _ '.
       /   O      O   \
      :                :
      |                |
      :       __       :
       \  .-"`  `"-.  /
        '.          .'
          '-......-'
      YOU SHOUDN'T BE HERE
""")
    }
  })

  def app = FakeApplication(
    withRoutes =
      PartialFunction.empty
        .orElse(getIp)
        .orElse(getUserAgent)
        .orElse(getHeaders)
        .orElse(get)
        .orElse(patch)
        .orElse(post)
        .orElse(put)
        .orElse(delete)
        .orElse(gzip)
        .orElse(status)
        .orElse(responseHeaders)
        .orElse(redirect)
        .orElse(redirectTo)
        .orElse(cookies)
        .orElse(cookiesSet)
        .orElse(cookiesDelete)
        .orElse(basicAuth)
        .orElse(stream)
        .orElse(delay)
        .orElse(html)
        .orElse(robots)
  )

}

