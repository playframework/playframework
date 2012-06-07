package play.api.libs.iteratee

import play.api.libs.concurrent.execution.defaultContext
import Parsing._
import play.api.libs.concurrent._

import org.specs2.mutable._

object ParsingSpec extends Specification {

  "Parsing" should {

    "split case 1" in {

      val data = Enumerator(List("xx", "kxckikixckikio", "cockik", "isdodskikisd", "ksdloii").map(_.getBytes): _*)
      val parsed = data |>> Parsing.search("kiki".getBytes).transform(Iteratee.fold(List.empty[MatchInfo[Array[Byte]]]) { (s, c) => s :+ c })

      val result = parsed.flatMap(_.run).await.get.map {
        case Matched(kiki) => "Matched(" + new String(kiki) + ")"
        case Unmatched(data) => "Unmatched(" + new String(data) + ")"
      }.mkString(", ")

      result must equalTo(
        "Unmatched(xxkxc), Matched(kiki), Unmatched(xc), Matched(kiki), Unmatched(ococ), Matched(kiki), Unmatched(sdods), Matched(kiki), Unmatched(sdks), Unmatched(dloii)")

    }

    "split case 1" in {

      val data = Enumerator(List("xx", "kxckikixcki", "k", "kicockik", "isdkikodskikisd", "ksdlokiikik", "i").map(_.getBytes): _*)
      val parsed = data |>> Parsing.search("kiki".getBytes).transform(Iteratee.fold(List.empty[MatchInfo[Array[Byte]]]) { (s, c) => s :+ c })

      val result = parsed.flatMap(_.run).await.get.map {
        case Matched(kiki) => "Matched(" + new String(kiki) + ")"
        case Unmatched(data) => "Unmatched(" + new String(data) + ")"
      }.mkString(", ")

      result must equalTo(
        "Unmatched(xxkxc), Matched(kiki), Unmatched(xckikkico), Unmatched(c), Matched(kiki), Unmatched(sdkikods), Matched(kiki), Unmatched(sdksdlok), Unmatched(ii), Matched(kiki), Unmatched()")

    }

  }
}
