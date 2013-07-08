package play.core.coffeescript

import java.io._
import org.specs2.mutable._
import sbt.PlayExceptions.AssetCompilationException

object AssetsSpec extends Specification {

  "coffee script compiler" should {

    "compile in javacript given a .coffe file" in {
      compileFixture()._1 must contain("list = [1, 2, 3, 4, 5];")
    }

    "works again if same file is loaded in another thread" in {
      compileFixture()._1 must contain("number = 42;")
    }

    "compile in javascript with top-level function safety wrapper" in {
      compileFixture()._1 must startWith("(function() {")
    }

    "compile in javascript without top-level function safety wrapper" in {
      compileFixture(Seq("bare"))._1 must not startWith("(function() {")
    }

    "compile in javascript and generate source map file" in {
      val (javascript, sourceMap) = compileFixture(Seq("map"))
      val excerpt = "\"sources\": [\n    \"coffee-script-fixture.coffee\"\n  ],"
      sourceMap must beSome and sourceMap.map(_ must contain(excerpt)).get
    }

    "throw an error with line number when wrong format" in {
      {
        compileForFileName("error.coffee")
      } must throwAn[AssetCompilationException].like {
          case e => e.getMessage must contain("unexpected IF")
      }
    }
  }

  def compileFixture(options: Seq[String] = Nil) = compileForFileName("coffee-script-fixture.coffee", options)

  def compileForFileName(fileName: String, options: Seq[String] = Nil): (String, Option[String]) = {
    val file = loadFile(fileName)
    CoffeescriptCompiler.compile(file, options)
  }

  def loadFile(fileName: String):File = {
    val fixtureURL = this.getClass()
      .getClassLoader()
      .getResource(fileName)

    new File(fixtureURL.toURI())
  }
}
