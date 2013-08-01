package scalaguide.templates

import org.specs2.mutable.Specification
import play.api.templates.Html

// These have to be in the same package as the template
package views.html.Application {
  case class Order(title: String)
  case class Customer(name: String)
}

package html.models {
  case class Order(title: String)
  case class Customer(name: String)
  case class Product(name: String, price: String)
  case class User(firstName: String, lastName: String)
  case class Article(content: String)

  case class MyFieldConstructor() {
    val working = "implicit working"
  }

  object ImplicitTester {
    def test(implicit f: MyFieldConstructor) = f.working
  }
}

package html.utils {
  object ImportTester {
    def test = "import working"
  }
}

object ScalaTemplatesSpec extends Specification {

  import html.models._

  val customer = Customer("mr customer")
  val orders = List(Order("foo"), Order("bar"))


  "Scala templates" should {
    "support an example template" in {
      import views.html.Application._

      val c = Customer("mr customer")
      val o = List(Order("foo"), Order("bar"))

      //#invoke-template
      val content = views.html.Application.index(c, o)
      //#invoke-template

      val body = content.body
      body must contain("mr customer")
      body must contain("foo")
      body must contain("bar")
    }

    "allow simple parameters" in {
      val body = html.simpleParameters(customer, orders).body
      body must contain(customer.toString)
      body must contain(orders(0).toString)
      body must contain(orders(1).toString)
    }

    "allow default parameters" in {
      html.defaultParameters("foo").body must contain("foo")
      html.defaultParameters().body must contain("Home")
    }

    "allow curried parameters" in {
      val body = html.curriedParameters("foo")(Html("bar")).body
      body must contain("foo")
      body must contain("bar")
    }

    "allow import statements" in {
      html.importStatement(customer, orders).body must contain("import working")
    }

    "allow absolute import statements" in {
      html.importStatement(customer, orders).body must contain("absolute import is working")
    }

    "allow comments on the first line" in {
      val body = html.firstLineComment("blah").body
      body must contain("blah")
      body must not contain("Home page")
    }

    {
      val body = html.snippets(Seq(Product("p1", "1"), Product("p2", "2")), User("John", "Doe"), Article("<foo>")).body
      def segment(name: String) = {
        body.lines.dropWhile(_ != "<span class=\"" + name + "\">").drop(1).takeWhile(_ != "</span>").mkString("\n")
      }

      "allow escaping the @ character" in {
        body must contain("bob@example.com")
      }

      "allow iterating" in {
        segment("for-loop") must contain("p1 ($1)")
        segment("for-loop") must contain("p2 ($2)")
      }

      "allow conditionals" in {
        body must contain("2 items!")
      }

      "allow reusable code blocks" in {
        segment("reusable") must contain("p1 ($1)")
        segment("reusable") must contain("p2 ($2)")
      }

      "allow pure scala reusable code blocks" in {
        body must contain("Hello World")
      }

      "allow declaring implicit variables" in {
        body must contain("implicit working")
      }

      "allow defining variables" in {
        body must contain("Hello John Doe")
      }

      "allow comments" in {
        body must not contain("This is a comment")
      }

      "allow intering raw HTML" in {
        body must contain("<foo>")
      }
    }
  }
}