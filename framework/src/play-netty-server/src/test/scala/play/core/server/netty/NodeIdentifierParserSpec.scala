package play.core.server.netty

import java.net.InetAddress.getByName
import org.specs2.mutable.Specification
import NodeIdentifierParser.parseNode

class NodeIdentifierParserSpec extends Specification {
  "NodeIdentifierParser" should {

    "parse an ip v6 address with port" in {
      parseNode("[8F:F3B::FF]:9000").right.get mustEqual Right(getByName("8F:F3B::FF")) -> Some(Right(9000))
    }

    "parse an ip v6 address with obfuscated port" in {
      parseNode("[::FF]:_obf").right.get mustEqual Right(getByName("::FF")) -> Some(Left("_obf"))
    }

    "parse an ip v4 address with port" in {
      parseNode("127.0.0.1:8080").right.get mustEqual Right(getByName("127.0.0.1")) -> Some(Right(8080))
    }

    "parse an ip v4 address without port" in {
      parseNode("192.168.0.1").right.get mustEqual Right(getByName("192.168.0.1")) -> None
    }

    "parse an unknown ip address without port" in {
      parseNode("unknown").right.get mustEqual Left("unknown") -> None
    }

    "parse an obfuscated ip address without port" in {
      parseNode("_harry").right.get mustEqual Left("_harry") -> None
    }
  }
}
