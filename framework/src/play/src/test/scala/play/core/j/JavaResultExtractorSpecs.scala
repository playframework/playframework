package play.core.j

import scala.concurrent.ExecutionContext.Implicits.global

import org.specs2.mutable.Specification

import play.api.http.HeaderNames._
import play.api.http.Writeable
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Input
import play.api.mvc.ChunkedResult
import play.api.mvc.Codec
import play.api.mvc.ResponseHeader

object JavaResultExtractorSpecs extends Specification {
  def enumIndefinitely(): Enumerator[String] = {
    var index: Long = 0
    Enumerator.generateM(
      play.api.libs.concurrent.Promise.timeout(
        { val msg = "current time %d".format(index); index = index + 1; Some(msg) },
        500
      )
    )
  }

  def enum(): Enumerator[String] = {
    val enum0 = Enumerator("result-0", "result-1")
    val enum1 = Enumerator("result-2")
    val enum2 = Enumerator("result-3", "result-4")

    val enumEmpty = Enumerator.enumInput[String](Input.Empty)
    val enumEOF = Enumerator.enumInput[String](Input.EOF)

    // check for Input.EOF
    enum0.andThen(enumEmpty).andThen(enum1).andThen(enumEOF).andThen(enum2)
  }

  def stream(content: Enumerator[String]): play.mvc.Result = {
    implicit val code = Codec.utf_8
    implicit val writeable: Writeable[String] = Writeable.wString

    val header = ResponseHeader(
      200,
      writeable.contentType.map(ct => Map(CONTENT_TYPE -> ct)).getOrElse(Map.empty))

    val result: ChunkedResult[String] = ChunkedResult(
      header,
      iteratee => content |>> iteratee)

    new JavaResultExtractor.ResultWrapper(result)
  }

  "head of Indefinitely ChunkedResult" should {
    val chnukedResult: play.mvc.Result = stream(enumIndefinitely)
    val head: Option[Array[Byte]] = JavaResultExtractor.headChunks(chnukedResult)

    new String(head.get, "UTF-8") must beEqualTo("current time 0")
  }

  "head of ChunkedResult" should {
    val chnukedResult: play.mvc.Result = stream(enum)
    val head: Option[Array[Byte]] = JavaResultExtractor.headChunks(chnukedResult)

    new String(head.get, "UTF-8") must beEqualTo("result-0")
  }

  "getChunks" should {
    val chnukedResult: play.mvc.Result = stream(enum)
    val chunksList: List[Array[Byte]] = JavaResultExtractor.getChunks(chnukedResult)

    chunksList.size must beEqualTo(3)
  }

}

