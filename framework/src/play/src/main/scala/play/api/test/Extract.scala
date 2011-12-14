package play.api.test

import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.iteratee._

case class ResultData(val status: Int, val headers: java.util.Map[String, String], val body: Any)

/**
 * provides extractors for various result types
 */
object Extract {

  import collection.JavaConverters._

  /*
   * used by the java API to extract data from a Result
   * @param result usually coming from a controller method call
   * @return return a data structure that contains status, headers and body as a string
   *
   */
  def fromAsJava(result: Result): ResultData = {
    val resultAsJava = from(result)
    ResultData(resultAsJava._1, resultAsJava._2.asJava, resultAsJava._3)
  }

  /*
   * helper method that lets you extract status, headers and the body as a string from a Result
   * @param result usually coming from a controller method call
   * @return a tuple with statust, headers and body information
   */
  def from(result: Result): Tuple3[Int, Map[String, String], String] =
    result match {
      case simp @ SimpleResult(ResponseHeader(status, headers), body) =>
        val later: Promise[String] = (Iteratee.fold[simp.BODY_CONTENT, String]("") { case (s, e) => s + e } <<: body).flatMap(_.run)
        (status, headers, { later.value match { case r: Redeemed[_] => r.a } })
      case _ => (0, Map.empty, "")
    }
}

