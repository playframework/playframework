package play.core.coffeescript

import java.io._
import org.specs2.mutable._
import play.PlayExceptions.AssetCompilationException

object AssetsSpec extends Specification {

  "coffee script compiler" should {

    "compile in javacript given a .coffe file" in {
      compileForFileName("coffee-script-fixture.coffee") must contain("list = [1, 2, 3, 4, 5];")
    }

    "works again if same file is loaded in another thread" in {
      compileForFileName("coffee-script-fixture.coffee") must contain("number = 42;")
    }

    "throw an error with line number when wrong format" in {
      {
        compileForFileName("error.coffee")
      } must throwAn[AssetCompilationException].like {
          case e => e.getMessage must contain("Parse error on line 2")
      }
    }

  }

  def compileForFileName(fileName: String):String = {
    val file = loadFile(fileName);
    CoffeescriptCompiler.compile(file, Seq("bare"))
  }

  def loadFile(fileName: String):File = {
    val fixtureURL = this.getClass()
      .getClassLoader()
      .getResource(fileName)

    new File(fixtureURL.toURI())
  }
}
