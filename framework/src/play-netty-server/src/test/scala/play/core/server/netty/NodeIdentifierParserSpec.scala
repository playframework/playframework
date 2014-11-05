package play.core.server.netty

import java.net.InetAddress.getByName
import org.specs2.mutable.Specification
import play.core.server.netty.NodeIdentifierParser._

class NodeIdentifierParserSpec extends Specification {
  "NodeIdentifierParser" should {

    "parse an ip v6 address with port" in {
      parseNode("[8F:F3B::FF]:9000").right.get mustEqual Ip(getByName("8F:F3B::FF")) -> Some(PortNumber(9000))
    }

    "parse an ip v6 address with obfuscated port" in {
      parseNode("[::FF]:_obf").right.get mustEqual Ip(getByName("::FF")) -> Some(ObfuscatedPort("_obf"))
    }

    "parse an ip v4 address with port" in {
      parseNode("127.0.0.1:8080").right.get mustEqual Ip(getByName("127.0.0.1")) -> Some(PortNumber(8080))
    }

    "parse an ip v4 address without port" in {
      parseNode("192.168.0.1").right.get mustEqual Ip(getByName("192.168.0.1")) -> None
    }

    "parse an unknown ip address without port" in {
      parseNode("unknown").right.get mustEqual UnknownIp -> None
    }

    "parse an obfuscated ip address without port" in {
      parseNode("_harry").right.get mustEqual ObfuscatedIp("_harry") -> None
    }
  }
}
