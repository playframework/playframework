package play.core.less

import org.specs2.mutable.Specification
import java.io.File
import play.PlayExceptions.AssetCompilationException

object LessCompilerSpec extends Specification {

  sequential

  "the less compiler" should {
    "compile a valid less file" in {
      val result = LessCompiler.compile(getFile("valid.less"))
      // debug
      result._1 must contain(".box")
      // minified
      result._2 must beSome.like {
        case css =>
          css must contain(".box")
      }
    }

    "support imports" in {
      val result = LessCompiler.compile(getFile("importvalid.less"))
      result._1 must contain(".box")
    }

    "correctly handle errors in an invalid less file" in {
      val file = getFile("invalid.less")
      LessCompiler.compile(file) must throwAn[AssetCompilationException].like {
        case e: AssetCompilationException =>
          e.atLine must beSome(13)
          e.column must beSome(24)
          e.message must_== "variable @doesnotexist is undefined"
          e.source must beSome(file)
      }
    }

    "correctly handle errors in an imported invalid less file" in {
      LessCompiler.compile(getFile("importinvalid.less")) must throwAn[AssetCompilationException].like {
        case e: AssetCompilationException =>
          e.atLine must beSome(13)
          e.column must beSome(24)
          e.message must_== "variable @doesnotexist is undefined"
          e.source must beSome(getFile("invalid.less"))
      }
    }

    "correctly handle errors in a less file with a parse error" in {
      val file = getFile("parseerror.less")
      LessCompiler.compile(file) must throwAn[AssetCompilationException].like {
        case e: AssetCompilationException =>
          e.atLine must beSome(3)
          e.column must beSome(4)
          e.message must_== "Unrecognised input"
          e.source must beSome(file)
      }
    }

    "correctly handle errors in an imported less file with a parse error" in {
      LessCompiler.compile(getFile("importparseerror.less")) must throwAn[AssetCompilationException].like {
        case e: AssetCompilationException =>
          e.atLine must beSome(3)
          e.column must beSome(4)
          e.message must_== "Unrecognised input"
          e.source must beSome(getFile("parseerror.less"))
      }
    }

    "correctly handle missing import files" in {
      LessCompiler.compile(getFile("importmissing.less")) must throwAn[AssetCompilationException].like {
        case e: AssetCompilationException =>
          e.atLine must beSome(5)
          e.column must beSome(0)
          e.message must contain("File not found: ")
          e.message must contain("/missing.less")
          e.source must beSome(getFile("importmissing.less"))
      }
    }

  }

  def getFile(fileName: String):File = {
    new File(this.getClass.getClassLoader.getResource(fileName).toURI)
  }
}
