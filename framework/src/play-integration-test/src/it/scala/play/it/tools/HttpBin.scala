/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.tools

import java.nio.charset.StandardCharsets

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.Files
import play.api.libs.json.{ JsObject, _ }
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api._
import play.filters.gzip.GzipFilter

/**
 * This is a reimplementation of the excellent httpbin.org service
 * by Kenneth Reitz
 *
 * Motivation: We couldn't use httpbin.org directly for our CI.
 */
object HttpBinApplication {

  private val requestHeaderWriter = new Writes[RequestHeader] {
    def writes(r: RequestHeader): JsValue = Json.obj(
      "origin" -> r.remoteAddress,
      "url" -> "",
      "args" -> r.queryString.mapValues(_.head),
      "headers" -> r.headers.toSimpleMap
    )
  }

  private def requestWriter[A]: Writes[Request[A]] = new Writes[Request[A]] {
    def readFileToString(ref: Files.TemporaryFile): String = {
      new String(java.nio.file.Files.readAllBytes(ref), StandardCharsets.UTF_8)
    }
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
            case m: play.api.mvc.AnyContentAsMultipartFormData @unchecked =>
              Json.obj(
                "form" -> m.mfd.dataParts.map { case (k, v) => k -> JsString(v.mkString) },
                "file" -> JsString(m.mfd.file("upload").map(v => readFileToString(v.ref)).getOrElse(""))
              )
            case b =>
              Json.obj("data" -> JsString(b.toString))
          })
  }

  def getIp(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/ip") =>
      Action { request =>
        Ok(Json.obj("origin" -> request.remoteAddress))
      }
  }

  def getUserAgent(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/user-agent") =>
      Action { request =>
        Ok(Json.obj("user-agent" -> request.headers.get("User-Agent")))
      }
  }

  def getHeaders(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/headers") =>
      Action { request =>
        Ok(Json.obj("headers" -> request.headers.toSimpleMap))
      }
  }

  def get(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/get") =>
      Action { request =>
        Ok(requestHeaderWriter.writes(request))
      }
  }

  def patch(implicit Action: DefaultActionBuilder): Routes = {
    case PATCH(p"/patch") =>
      Action { request =>
        Ok(requestWriter.writes(request))
      }
  }

  def post(implicit Action: DefaultActionBuilder): Routes = {
    case POST(p"/post") =>
      Action { request =>
        Ok(requestWriter.writes(request))
      }
  }

  def put(implicit Action: DefaultActionBuilder): Routes = {
    case PUT(p"/put") =>
      Action { request =>
        Ok(requestWriter.writes(request))
      }
  }

  def delete(implicit Action: DefaultActionBuilder): Routes = {
    case DELETE(p"/delete") =>
      Action { request =>
        Ok(requestHeaderWriter.writes(request))
      }
  }

  private def gzipFilter(mat: Materializer) = new GzipFilter()(mat)

  def gzip(implicit mat: Materializer, Action: DefaultActionBuilder): Routes = Seq("GET", "PATCH", "POST", "PUT", "DELETE").map { method =>
    val route: Routes = {
      case r @ p"/gzip" if r.method == method =>
        gzipFilter(mat)(Action { request =>
          Ok(requestHeaderWriter.writes(request).as[JsObject] ++ Json.obj("gzipped" -> true, "method" -> method))
        })
    }
    route
  }.reduceLeft((a, b) => a.orElse(b))

  def status(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/status/$status<[0-9]+>") =>
      Action {
        val code = status.toInt
        Results.Status(code)
      }
  }

  def responseHeaders(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/response-header") =>
      Action { request =>
        Ok("").withHeaders(request.queryString.mapValues(_.mkString(",")).toSeq: _*)
      }
  }

  def redirect(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/redirect/0") =>
      Action {
        Redirect("/get")
      }
    case GET(p"/redirect/$param<([0-9]+)>") =>
      Action {
        Redirect("redirect/" + param)
      }
  }

  def redirectTo(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/redirect-to") =>
      Action { request =>
        request.queryString.get("url").map { u =>
          Redirect(u.head)
        }.getOrElse {
          BadRequest("")
        }
      }
  }

  def cookies(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/cookies") =>
      Action { request =>
        Ok(Json.obj("cookies" -> JsObject(request.cookies.toSeq.map(x => x.name -> JsString(x.value)))))
      }
  }

  def cookiesSet(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/cookies/set") =>
      Action { request =>
        Redirect("/cookies").withCookies(request.queryString.mapValues(_.head).toSeq.map {
          case (k, v) => Cookie(k, v)
        }: _*)
      }
  }

  def cookiesDelete(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/cookies/delete") =>
      Action { request =>
        Redirect("/cookies").discardingCookies(request.queryString.keys.toSeq.map(DiscardingCookie(_)): _*)
      }
  }

  def basicAuth(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/basic-auth/$username/$password") =>
      Action { request =>
        request.headers.get("Authorization").flatMap { authorization =>
          authorization.split(" ").drop(1).headOption.filter { encoded =>
            new String(java.util.Base64.getDecoder.decode(encoded.getBytes)).split(":").toList match {
              case u :: p :: Nil if u == username && password == p => true
              case _ => false
            }
          }.map(_ => Ok(Json.obj("authenticated" -> true)))
        }.getOrElse {
          Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured"""")
        }
      }
  }

  def stream(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/stream/$param<[0-9]+>") =>
      Action {
        val contentLength = param.toInt
        val content = (0 to contentLength).map(ByteString(_))
        Ok.sendEntity(HttpEntity.Streamed(Source(content), Option(contentLength), Option("application/json")))
      }
  }

  def delay(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/delay/$duration<[0-9+]") =>
      Action.async { request =>
        import scala.concurrent.ExecutionContext.Implicits.global
        import scala.concurrent.{ Await, Future, Promise }
        import scala.concurrent.duration._
        import scala.util.Try
        val p = Promise[Result]()

        Future {
          Try {
            Await.result(p.future, Duration(duration.toLong, SECONDS))
          }.getOrElse {
            p.success(Ok(requestWriter.writes(request)))
          }
        }

        p.future
      }
  }

  def html(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/html") =>
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

  def robots(implicit Action: DefaultActionBuilder): Routes = {
    case GET(p"/robots.txt") =>
      Action {
        Ok("User-agent: *\nDisallow: /deny")
      }
    case GET(p"deny") =>
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
  }

  def app: Application = {
    new BuiltInComponentsFromContext(ApplicationLoader.Context.create(Environment.simple())) with AhcWSComponents with NoHttpFiltersComponents {
      override implicit lazy val Action = defaultActionBuilder
      override def router = SimpleRouter(
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
    }.application
  }

}
