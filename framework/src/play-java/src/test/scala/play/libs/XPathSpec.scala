/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs

import org.specs2.mutable.Specification
import scala.collection.JavaConverters._

object XPathSpec extends Specification {
  //XPathFactory.newInstance() used internally by XPath is not thread safe so forcing sequential execution
  sequential

  val xmlWithNamespace = XML.fromString("""<x:foo xmlns:x="http://foo.com/"><x:bar><x:baz>hey</x:baz></x:bar></x:foo>""")
  val xmlWithoutNamespace = XML.fromString("""<foo><bar><baz>hey</baz></bar><bizz></bizz><bizz></bizz></foo>""")

  "XPath" should {
    "ignore already bound namespaces" in {
      val ns = Map("x" -> "http://foo.com/", "ns" -> "http://www.w3.org/XML/1998/namespace", "y" -> "http://foo.com/")
      XPath.selectText("//x:baz", xmlWithNamespace, ns.asJava) must not(throwAn[UnsupportedOperationException])
    }

    "find text with namespace" in {
      val text = XPath.selectText("//x:baz", xmlWithNamespace, Map("ns" -> "http://www.w3.org/XML/1998/namespace", "x" -> "http://foo.com/").asJava)
      text must_== "hey"
    }

    "find text without namespace" in {
      val text = XPath.selectText("//baz", xmlWithoutNamespace, null)
      text must_== "hey"
    }

    "find node with namespace" in {
      val node = XPath.selectNode("//x:baz", xmlWithNamespace, Map("ns" -> "http://www.w3.org/XML/1998/namespace", "x" -> "http://foo.com/").asJava)
      node.getNodeName must_== "x:baz"
    }

    "find nodes" in {
      val nodeList = XPath.selectNodes("//bizz", xmlWithoutNamespace, null)
      nodeList.getLength === 2
    }
  }
}
