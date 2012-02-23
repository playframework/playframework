package play.api.templates {

  trait Template0[Result] { def render(): Result }
  trait Template1[A, Result] { def render(a: A): Result }
  trait Template2[A, B, Result] { def render(a: A, b: B): Result }
  trait Template3[A, B, C, Result] { def render(a: A, b: B, c: C): Result }
  trait Template4[A, B, C, D, Result] { def render(a: A, b: B, c: C, d: D): Result }
  trait Template5[A, B, C, D, E, Result] { def render(a: A, b: B, c: C, d: D, e: E): Result }
  trait Template6[A, B, C, D, E, F, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F): Result }
  trait Template7[A, B, C, D, E, F, G, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G): Result }
  trait Template8[A, B, C, D, E, F, G, H, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H): Result }
  trait Template9[A, B, C, D, E, F, G, H, I, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I): Result }
  trait Template10[A, B, C, D, E, F, G, H, I, J, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J): Result }
  trait Template11[A, B, C, D, E, F, G, H, I, J, K, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K): Result }
  trait Template12[A, B, C, D, E, F, G, H, I, J, K, L, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L): Result }
  trait Template13[A, B, C, D, E, F, G, H, I, J, K, L, M, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M): Result }
  trait Template14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N): Result }
  trait Template15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O): Result }
  trait Template16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P): Result }
  trait Template17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q): Result }
  trait Template18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R): Result }
  trait Template19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S): Result }
  trait Template20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T): Result }
  trait Template21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U): Result }
  trait Template22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, Result] { def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U, v: V): Result }

}

package play.templates {

  import scalax.file._
  import java.io.File
  import scala.annotation.tailrec

  object Hash {

    def apply(bytes: Array[Byte]): String = {
      import java.security.MessageDigest
      val digest = MessageDigest.getInstance("SHA-1")
      digest.reset()
      digest.update(bytes)
      digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
    }

  }

  case class TemplateCompilationError(source: File, message: String, line: Int, column: Int) extends RuntimeException(message)

  object MaybeGeneratedSource {

    def unapply(source: File): Option[GeneratedSource] = {
      val generated = GeneratedSource(source)
      if (generated.meta.isDefinedAt("SOURCE")) {
        Some(generated)
      } else {
        None
      }
    }

  }

  case class GeneratedSource(file: File) {

    lazy val meta: Map[String, String] = {
      val Meta = """([A-Z]+): (.*)""".r
      val UndefinedMeta = """([A-Z]+):""".r
      Map.empty[String, String] ++ {
        try {
          Path(file).slurpString.split("-- GENERATED --")(1).trim.split('\n').map { m =>
            m.trim match {
              case Meta(key, value) => (key -> value)
              case UndefinedMeta(key) => (key -> "")
              case _ => ("UNDEFINED", "")
            }
          }.toMap
        } catch {
          case _ => Map.empty[String, String]
        }
      }
    }

    lazy val matrix: Seq[(Int, Int)] = {
      for (pos <- meta("MATRIX").split('|'); val c = pos.split("->"))
        yield try {
        Integer.parseInt(c(0)) -> Integer.parseInt(c(1))
      } catch {
        case _ => (0, 0) // Skip if MATRIX meta is corrupted
      }
    }

    lazy val lines: Seq[(Int, Int)] = {
      for (pos <- meta("LINES").split('|'); val c = pos.split("->"))
        yield try {
        Integer.parseInt(c(0)) -> Integer.parseInt(c(1))
      } catch {
        case _ => (0, 0) // Skip if LINES meta is corrupted
      }
    }

    def needRecompilation: Boolean = (!file.exists ||
      // A generated source already exist but
      source.isDefined && ((source.get.lastModified > file.lastModified) || // the source has been modified
        (meta("HASH") != Hash(Path(source.get).byteArray))) // or the hash don't match
    )

    def mapPosition(generatedPosition: Int): Int = {
      matrix.indexWhere(p => p._1 > generatedPosition) match {
        case 0 => 0
        case i if i > 0 => {
          val pos = matrix(i - 1)
          pos._2 + (generatedPosition - pos._1)
        }
        case _ => {
          val pos = matrix.takeRight(1)(0)
          pos._2 + (generatedPosition - pos._1)
        }
      }
    }

    def mapLine(generatedLine: Int): Int = {
      lines.indexWhere(p => p._1 > generatedLine) match {
        case 0 => 0
        case i if i > 0 => {
          val line = lines(i - 1)
          line._2 + (generatedLine - line._1)
        }
        case _ => {
          val line = lines.takeRight(1)(0)
          line._2 + (generatedLine - line._1)
        }
      }
    }

    def toSourcePosition(marker: Int): (Int, Int) = {
      try {
        val targetMarker = mapPosition(marker)
        val line = Path(source.get).slurpString.substring(0, targetMarker).split('\n').size
        (line, targetMarker)
      } catch {
        case _ => (0, 0)
      }
    }

    def source: Option[File] = {
      val s = new File(meta("SOURCE"))
      if (s == null || !s.exists) {
        None
      } else {
        Some(s)
      }
    }

    def sync() {
      if (file.exists && !source.isDefined) {
        file.delete()
      }
    }

  }

  object ScalaTemplateCompiler {

    import scala.util.parsing.input.Positional
    import scala.util.parsing.input.CharSequenceReader
    import scala.util.parsing.combinator.JavaTokenParsers

    abstract class TemplateTree
    abstract class ScalaExpPart

    case class Params(code: String) extends Positional
    case class Template(name: PosString, comment: Option[Comment], params: PosString, imports: Seq[Simple], defs: Seq[Def], sub: Seq[Template], content: Seq[TemplateTree]) extends Positional
    case class PosString(str: String) extends Positional {
      override def toString = str
    }
    case class Def(name: PosString, params: PosString, code: Simple) extends Positional
    case class Plain(text: String) extends TemplateTree with Positional
    case class Display(exp: ScalaExp) extends TemplateTree with Positional
    case class Comment(msg: String) extends TemplateTree with Positional
    case class ScalaExp(parts: Seq[ScalaExpPart]) extends TemplateTree with Positional
    case class Simple(code: String) extends ScalaExpPart with Positional
    case class Block(whitespace: String, args: Option[String], content: Seq[TemplateTree]) extends ScalaExpPart with Positional
    case class Value(ident: PosString, block: Block) extends Positional

    def compile(source: File, sourceDirectory: File, generatedDirectory: File, resultType: String, formatterType: String, additionalImports: String = "") = {
      val (templateName, generatedSource) = generatedFile(source, sourceDirectory, generatedDirectory)
      if (generatedSource.needRecompilation) {
        val generated = templateParser.parser(new CharSequenceReader(Path(source).slurpString)) match {
          case templateParser.Success(parsed, rest) if rest.atEnd => {
            generateFinalTemplate(
              source,
              templateName.dropRight(1).mkString("."),
              templateName.takeRight(1).mkString,
              parsed,
              resultType,
              formatterType,
              additionalImports)
          }
          case templateParser.Success(_, rest) => {
            throw new TemplateCompilationError(source, "Not parsed?", rest.pos.line, rest.pos.column)
          }
          case templateParser.NoSuccess(message, input) => {
            throw new TemplateCompilationError(source, message, input.pos.line, input.pos.column)
          }
        }

        Path(generatedSource.file).write(generated.toString)

        Some(generatedSource.file)
      } else {
        None
      }
    }

    def generatedFile(template: File, sourceDirectory: File, generatedDirectory: File) = {
      val templateName = source2TemplateName(template, sourceDirectory, template.getName.split('.').takeRight(1).head).split('.')
      templateName -> GeneratedSource(new File(generatedDirectory, templateName.mkString("/") + ".template.scala"))
    }

    @tailrec
    def source2TemplateName(f: File, sourceDirectory: File, ext: String, suffix: String = "", topDirectory: String = "views", setExt: Boolean = true): String = {
      val Name = """([a-zA-Z0-9_]+)[.]scala[.]([a-z]+)""".r
      (f, f.getName) match {
        case (f, _) if f == sourceDirectory => {
          if (setExt) {
            val parts = suffix.split('.')
            Option(parts.dropRight(1).mkString(".")).filterNot(_.isEmpty).map(_ + ".").getOrElse("") + ext + "." + parts.takeRight(1).mkString
          } else suffix
        }
        case (f, name) if name == topDirectory => source2TemplateName(f.getParentFile, sourceDirectory, ext, name + "." + ext + "." + suffix, topDirectory, false)
        case (f, Name(name, _)) if f.isFile => source2TemplateName(f.getParentFile, sourceDirectory, ext, name, topDirectory, setExt)
        case (f, name) if !f.isFile => source2TemplateName(f.getParentFile, sourceDirectory, ext, name + "." + suffix, topDirectory, setExt)
        case (f, name) => throw TemplateCompilationError(f, "Invalid template name [" + name + "]", 0, 0)
      }
    }

    val templateParser = new JavaTokenParsers {

      def as[T](parser: Parser[T], error: String) = {
        Parser(in => parser(in) match {
          case s @ Success(_, _) => s
          case Failure(_, next) => Failure("`" + error + "' expected but `" + next.first + "' found", next)
          case Error(_, next) => Error(error, next)
        })
      }

      def several[T](p: => Parser[T]): Parser[List[T]] = Parser { in =>
        import scala.collection.mutable.ListBuffer
        val elems = new ListBuffer[T]
        def continue(in: Input): ParseResult[List[T]] = {
          val p0 = p // avoid repeatedly re-evaluating by-name parser
          @tailrec
          def applyp(in0: Input): ParseResult[List[T]] = p0(in0) match {
            case Success(x, rest) => elems += x; applyp(rest)
            case Failure(_, _) => Success(elems.toList, in0)
            case err: Error => err
          }
          applyp(in)
        }
        continue(in)
      }

      def at = "@"

      def eof = """\Z""".r

      def newLine = (("\r"?) ~> "\n")

      def identifier = as(ident, "identifier")

      def whiteSpaceNoBreak = """[ \t]+""".r

      def escapedAt = at ~> at

      def any = {
        Parser(in => if (in.atEnd) {
          Failure("end of file", in)
        } else {
          Success(in.first, in.rest)
        })
      }

      def plain: Parser[Plain] = {
        positioned(
          ((escapedAt | (not(at) ~> (not("{" | "}") ~> any))) +) ^^ {
            case charList => Plain(charList.mkString)
          })
      }

      def squareBrackets: Parser[String] = {
        "[" ~ (several((squareBrackets | not("]") ~> any))) ~ commit("]") ^^ {
          case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
        }
      }

      def parentheses: Parser[String] = {
        "(" ~ (several((parentheses | not(")") ~> any))) ~ commit(")") ^^ {
          case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
        }
      }

      def comment: Parser[Comment] = {
        (at ~ "*") ~> ((not("*@") ~> any *) ^^ { case chars => Comment(chars.mkString) }) <~ ("*" ~ at)
      }

      def brackets: Parser[String] = {
        ensureMatchedBrackets((several((brackets | not("}") ~> any)))) ^^ {
          case charList => "{" + charList.mkString + "}"
        }
      }

      def ensureMatchedBrackets[T](p: Parser[T]): Parser[T] = Parser { in =>
        val pWithBrackets = "{" ~> p <~ ("}" | eof ~ err("EOF"))
        pWithBrackets(in) match {
          case s @ Success(_, _) => s
          case f @ Failure(_, _) => f
          case Error("EOF", _) => Error("Unmatched bracket", in)
          case e: Error => e
        }
      }

      def block: Parser[Block] = {
        positioned(
          (whiteSpaceNoBreak?) ~ ensureMatchedBrackets((blockArgs?) ~ several(mixed)) ^^ {
            case w ~ (args ~ content) => Block(w.getOrElse(""), args, content.flatten)
          })
      }

      def blockArgs: Parser[String] = (not("=>" | newLine) ~> any *) ~ "=>" ^^ { case args ~ arrow => args.mkString + arrow }

      def methodCall: Parser[String] = identifier ~ (squareBrackets?) ~ (parentheses?) ^^ {
        case methodName ~ types ~ args => methodName + types.getOrElse("") + args.getOrElse("")
      }

      def expression: Parser[Display] = {
        at ~> commit(positioned(methodCall ^^ { case code => Simple(code) })) ~ several(expressionPart) ^^ {
          case first ~ parts => Display(ScalaExp(first :: parts))
        }
      }

      def expressionPart: Parser[ScalaExpPart] = {
        chainedMethods | block | (whiteSpaceNoBreak ~> scalaBlockChained) | elseCall | (parentheses ^^ { case code => Simple(code) })
      }

      def chainedMethods: Parser[Simple] = {
        positioned(
          "." ~> rep1sep(methodCall, ".") ^^ {
            case calls => Simple("." + calls.mkString("."))
          })
      }

      def elseCall: Parser[Simple] = {
        (whiteSpaceNoBreak?) ~> positioned("else" ^^ { case e => Simple(e) }) <~ (whiteSpaceNoBreak?)
      }

      def safeExpression: Parser[Display] = {
        at ~> positioned(parentheses ^^ { case code => Simple(code) }) ^^ {
          case code => Display(ScalaExp(code :: Nil))
        }
      }

      def matchExpression: Parser[Display] = {
        at ~> positioned(identifier ~ whiteSpaceNoBreak ~ "match" ^^ { case i ~ w ~ m => Simple(i + w + m) }) ~ block ^^ {
          case expr ~ block => {
            Display(ScalaExp(List(expr, block)))
          }
        }
      }

      def forExpression: Parser[Display] = {
        at ~> positioned("for" ~ parentheses ^^ { case f ~ p => Simple(f + p + " yield ") }) ~ block ^^ {
          case expr ~ block => {
            Display(ScalaExp(List(expr, block)))
          }
        }
      }

      def caseExpression: Parser[ScalaExp] = {
        (whiteSpace?) ~> positioned("""case (.+)=>""".r ^^ { case c => Simple(c) }) ~ block <~ (whiteSpace?) ^^ {
          case pattern ~ block => ScalaExp(List(pattern, block))
        }
      }

      def importExpression: Parser[Simple] = {
        positioned(
          at ~> """import .*(\r)?\n""".r ^^ {
            case stmt => Simple(stmt)
          })
      }

      def scalaBlock: Parser[Simple] = {
        at ~> positioned(
          brackets ^^ { case code => Simple(code) })
      }

      def scalaBlockChained: Parser[Block] = {
        scalaBlock ^^ {
          case code => Block("", None, ScalaExp(code :: Nil) :: Nil)
        }
      }

      def scalaBlockDisplayed: Parser[Display] = {
        scalaBlock ^^ {
          case code => Display(ScalaExp(code :: Nil))
        }
      }

      def mixed: Parser[Seq[TemplateTree]] = {
        ((comment | scalaBlockDisplayed | caseExpression | matchExpression | forExpression | safeExpression | plain | expression) ^^ { case t => List(t) }) |
          ("{" ~ several(mixed) ~ "}") ^^ { case p1 ~ content ~ p2 => Plain(p1) +: content.flatten :+ Plain(p2) }
      }

      def template: Parser[Template] = {
        templateDeclaration ~ """[ \t]*=[ \t]*[{]""".r ~ templateContent <~ "}" ^^ {
          case declaration ~ assign ~ content => {
            Template(declaration._1, None, declaration._2, content._1, content._2, content._3, content._4)
          }
        }
      }

      def localDef: Parser[Def] = {
        templateDeclaration ~ """[ \t]*=[ \t]*""".r ~ scalaBlock ^^ {
          case declaration ~ w ~ code => {
            Def(declaration._1, declaration._2, code)
          }
        }
      }

      def templateDeclaration: Parser[(PosString, PosString)] = {
        at ~> positioned(identifier ^^ { case s => PosString(s) }) ~ positioned(opt(squareBrackets) ~ several(parentheses) ^^ { case t ~ p => PosString(t.getOrElse("") + p.mkString) }) ^^ {
          case name ~ params => name -> params
        }
      }

      def templateContent: Parser[(List[Simple], List[Def], List[Template], List[TemplateTree])] = {
        (several(importExpression | localDef | template | mixed)) ^^ {
          case elems => {
            elems.foldLeft((List[Simple](), List[Def](), List[Template](), List[TemplateTree]())) { (s, e) =>
              e match {
                case i: Simple => (s._1 :+ i, s._2, s._3, s._4)
                case d: Def => (s._1, s._2 :+ d, s._3, s._4)
                case v: Template => (s._1, s._2, s._3 :+ v, s._4)
                case c: Seq[_] => (s._1, s._2, s._3, s._4 ++ c.asInstanceOf[Seq[TemplateTree]])
              }
            }
          }
        }
      }

      def parser: Parser[Template] = {
        opt(comment) ~ opt(whiteSpace) ~ opt(at ~> positioned((parentheses+) ^^ { case s => PosString(s.mkString) })) ~ templateContent ^^ {
          case comment ~ _ ~ args ~ content => {
            Template(PosString(""), comment, args.getOrElse(PosString("()")), content._1, content._2, content._3, content._4)
          }
        }
      }

      override def skipWhitespace = false

    }

    def visit(elem: Seq[TemplateTree], previous: Seq[Any]): Seq[Any] = {
      elem match {
        case head :: tail =>
          val tripleQuote = "\"\"\""
          visit(tail, head match {
            case p @ Plain(text) => (if (previous.isEmpty) Nil else previous :+ ",") :+ "format.raw" :+ Source("(", p.pos) :+ tripleQuote :+ text :+ tripleQuote :+ ")"
            case Comment(msg) => previous
            case Display(exp) => (if (previous.isEmpty) Nil else previous :+ ",") :+ "_display_(Seq(" :+ visit(Seq(exp), Nil) :+ "))"
            case ScalaExp(parts) => previous :+ parts.map {
              case s @ Simple(code) => Source(code, s.pos)
              case b @ Block(whitespace, args, content) if (content.forall(_.isInstanceOf[ScalaExp])) => Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ visit(content, Nil) :+ "}"
              case b @ Block(whitespace, args, content) => Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ "_display_(Seq(" :+ visit(content, Nil) :+ "))}"
            }
          })
        case Nil => previous
      }
    }

    def templateCode(template: Template, resultType: String): Seq[Any] = {

      val defs = (template.sub ++ template.defs).map { i =>
        i match {
          case t: Template if t.name == "" => templateCode(t, resultType)
          case t: Template => {
            Nil :+ (if (t.name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(t.name.str, t.name.pos) :+ Source(t.params.str, t.params.pos) :+ ":" :+ resultType :+ " = {_display_(" :+ templateCode(t, resultType) :+ ")};"
          }
          case Def(name, params, block) => {
            Nil :+ (if (name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(name.str, name.pos) :+ Source(params.str, params.pos) :+ " = {" :+ block.code :+ "};"
          }
        }
      }

      val imports = template.imports.map(_.code).mkString("\n")

      Nil :+ imports :+ "\n" :+ defs :+ "\n" :+ "Seq(" :+ visit(template.content, Nil) :+ ")"
    }

    def generateFinalTemplate(template: File, packageName: String, name: String, root: Template, resultType: String, formatterType: String, additionalImports: String): String = {

      val extra = TemplateAsFunctionCompiler.getFunctionMapping(
        root.params.str,
        resultType)

      val generated = {
        Nil :+ """
package """ :+ packageName :+ """

import play.templates._
import play.templates.TemplateMagic._

""" :+ additionalImports :+ """
/*""" :+ root.comment.map(_.msg).getOrElse("") :+ """*/
object """ :+ name :+ """ extends BaseScalaTemplate[""" :+ resultType :+ """,Format[""" :+ resultType :+ """]](""" :+ formatterType :+ """) with """ :+ extra._3 :+ """ {

    /*""" :+ root.comment.map(_.msg).getOrElse("") :+ """*/
    def apply""" :+ Source(root.params.str, root.params.pos) :+ """:""" :+ resultType :+ """ = {
        _display_ {""" :+ templateCode(root, resultType) :+ """}
    }
    
    """ :+ extra._1 :+ """
    
    """ :+ extra._2 :+ """
    
    def ref = this

}"""
      }

      Source.finalSource(template, generated)
    }

    object TemplateAsFunctionCompiler {

      import java.io.File
      import scala.tools.nsc.interactive.{ Response, Global }
      import scala.tools.nsc.io.AbstractFile
      import scala.tools.nsc.util.{ SourceFile, Position, BatchSourceFile }
      import scala.tools.nsc.Settings
      import scala.tools.nsc.reporters.ConsoleReporter

      def getFunctionMapping(signature: String, returnType: String): (String, String, String) = {

        type Tree = PresentationCompiler.global.Tree
        type DefDef = PresentationCompiler.global.DefDef
        type TypeDef = PresentationCompiler.global.TypeDef

        def filterType(t: String) = t match {
          case vararg if vararg.startsWith("_root_.scala.<repeated>") => vararg.replace("_root_.scala.<repeated>", "Array")
          case synthetic if synthetic.contains("<synthetic>") => synthetic.replace("<synthetic>", "")
          case t => t
        }

        def findSignature(tree: Tree): Option[DefDef] = {
          tree match {
            case t: DefDef if t.name.toString == "signature" => Some(t)
            case t: Tree => t.children.flatMap(findSignature).headOption
          }
        }

        val params = findSignature(
          PresentationCompiler.treeFrom("object FT { def signature" + signature + " }")).get.vparamss

        val functionType = "(" + params.map(group => "(" + group.map {
          case a if a.mods.isByNameParam => " => " + a.tpt.children(1).toString
          case a => filterType(a.tpt.toString)
        }.mkString(",") + ")").mkString(" => ") + " => " + returnType + ")"

        val renderCall = "def render%s = apply%s".format(
          "(" + params.flatten.map {
            case a if a.mods.isByNameParam => a.name.toString + ":" + a.tpt.children(1).toString
            case a => a.name.toString + ":" + filterType(a.tpt.toString)
          }.mkString(",") + ")",
          params.map(group => "(" + group.map { p =>
            p.name.toString + Option(p.tpt.toString).filter(_.startsWith("_root_.scala.<repeated>")).map(_ => ":_*").getOrElse("")
          }.mkString(",") + ")").mkString)

        var templateType = "play.api.templates.Template%s[%s%s]".format(
          params.flatten.size,
          params.flatten.map {
            case a if a.mods.isByNameParam => a.tpt.children(1).toString
            case a => filterType(a.tpt.toString)
          }.mkString(","),
          (if (params.flatten.isEmpty) "" else ",") + returnType)

        val f = "def f:%s = %s => apply%s".format(
          functionType,
          params.map(group => "(" + group.map(_.name.toString).mkString(",") + ")").mkString(" => "),
          params.map(group => "(" + group.map { p =>
            p.name.toString + Option(p.tpt.toString).filter(_.startsWith("_root_.scala.<repeated>")).map(_ => ":_*").getOrElse("")
          }.mkString(",") + ")").mkString)

        (renderCall, f, templateType)
      }

      class CompilerInstance {

        def additionalClassPathEntry: Option[String] = None

        lazy val compiler = {

          val settings = new Settings

          val scalaObjectSource = Class.forName("scala.ScalaObject").getProtectionDomain.getCodeSource

          // is null in Eclipse/OSGI but luckily we don't need it there
          if (scalaObjectSource != null) {
            val compilerPath = Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource.getLocation.getFile
            val libPath = scalaObjectSource.getLocation.getFile
            val pathList = List(compilerPath, libPath)
            val origBootclasspath = settings.bootclasspath.value
            settings.bootclasspath.value = ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList) mkString File.pathSeparator
          }

          val compiler = new Global(settings, new ConsoleReporter(settings) {
            override def printMessage(pos: Position, msg: String) = ()
          })

          new compiler.Run

          compiler
        }
      }

      trait TreeCreationMethods {

        val global: scala.tools.nsc.interactive.Global

        val randomFileName = {
          val r = new java.util.Random
          () => "file" + r.nextInt
        }

        def treeFrom(src: String): global.Tree = {
          val file = new BatchSourceFile(randomFileName(), src)
          treeFrom(file)
        }

        def treeFrom(file: SourceFile): global.Tree = {
          import tools.nsc.interactive.Response

          type Scala29Compiler = {
            def askParsedEntered(file: SourceFile, keepLoaded: Boolean, response: Response[global.Tree]): Unit
            def askType(file: SourceFile, forceReload: Boolean, respone: Response[global.Tree]): Unit
          }

          val newCompiler = global.asInstanceOf[Scala29Compiler]

          val r1 = new Response[global.Tree]
          newCompiler.askParsedEntered(file, true, r1)
          r1.get.left.get
        }

      }

      object CompilerInstance extends CompilerInstance

      object PresentationCompiler extends TreeCreationMethods {
        val global = CompilerInstance.compiler

        def shutdown() {
          global.askShutdown()
        }
      }

    }

  }

  /* ------- */

  import scala.util.parsing.input.{ Position, OffsetPosition, NoPosition }

  case class Source(code: String, pos: Position = NoPosition)

  object Source {

    import scala.collection.mutable.ListBuffer

    def finalSource(template: File, generatedTokens: Seq[Any]): String = {
      val scalaCode = new StringBuilder
      val positions = ListBuffer.empty[(Int, Int)]
      val lines = ListBuffer.empty[(Int, Int)]
      serialize(generatedTokens, scalaCode, positions, lines)
      scalaCode + """
                /*
                    -- GENERATED --
                    DATE: """ + new java.util.Date + """
                    SOURCE: """ + template.getAbsolutePath.replace(File.separator, "/") + """
                    HASH: """ + Hash(Path(template).byteArray) + """
                    MATRIX: """ + positions.map { pos =>
        pos._1 + "->" + pos._2
      }.mkString("|") + """
                    LINES: """ + lines.map { line =>
        line._1 + "->" + line._2
      }.mkString("|") + """
                    -- GENERATED --
                */
            """
    }

    private def serialize(parts: Seq[Any], source: StringBuilder, positions: ListBuffer[(Int, Int)], lines: ListBuffer[(Int, Int)]) {
      parts.foreach {
        case s: String => source.append(s)
        case Source(code, pos @ OffsetPosition(_, offset)) => {
          source.append("/*" + pos + "*/")
          positions += (source.length -> offset)
          lines += (source.toString.split('\n').size -> pos.line)
          source.append(code)
        }
        case Source(code, NoPosition) => source.append(code)
        case s: Seq[any] => serialize(s, source, positions, lines)
      }
    }

  }

  /* ------ */

  trait Appendable[T] {
    def +(other: T): T
    override def equals(x: Any): Boolean = super.equals(x)
    override def hashCode() = super.hashCode()
  }

  trait Format[T <: Appendable[T]] {
    def raw(text: String): T
    def escape(text: String): T
  }

  case class BaseScalaTemplate[T <: Appendable[T], F <: Format[T]](format: F) {

    def _display_(o: Any)(implicit m: Manifest[T]): T = {
      o match {
        case escaped if escaped != null && escaped.getClass == m.erasure => escaped.asInstanceOf[T]
        case () => format.raw("")
        case None => format.raw("")
        case Some(v) => _display_(v)
        case xml: scala.xml.NodeSeq => format.raw(xml.toString)
        case escapeds: TraversableOnce[_] => escapeds.foldLeft(format.raw(""))(_ + _display_(_))
        case escapeds: Array[_] => escapeds.foldLeft(format.raw(""))(_ + _display_(_))
        case string: String => format.escape(string)
        case v if v != null => _display_(v.toString)
        case _ => format.raw("")
      }
    }

  }

  /* ------ */

  object TemplateMagic {

    // --- UTILS

    def defining[T](t: T)(handler: T => Any) = {
      handler(t)
    }

    def using[T](t: T) = t

    // --- IF

    implicit def iterableToBoolean(x: Iterable[_]) = x != null && !x.isEmpty
    implicit def optionToBoolean(x: Option[_]) = x != null && x.isDefined
    implicit def stringToBoolean(x: String) = x != null && !x.isEmpty

    // --- JAVA

    implicit def javaCollectionToScala[T](x: java.lang.Iterable[T]) = {
      import scala.collection.JavaConverters._
      x.asScala
    }

    // --- DEFAULT

    case class Default(default: Any) {
      def ?:(x: Any) = x match {
        case "" => default
        case Nil => default
        case false => default
        case 0 => default
        case None => default
        case _ => x
      }
    }

    implicit def anyToDefault(x: Any) = Default(x)

    // --- DATE

    class RichDate(date: java.util.Date) {

      def format(pattern: String) = {
        new java.text.SimpleDateFormat(pattern).format(date)
      }

    }

    implicit def richDate(date: java.util.Date) = new RichDate(date)

    // --- STRING

    class RichString(string: String) {

      def when(predicate: => Boolean) = {
        predicate match {
          case true => string
          case false => ""
        }
      }

    }

    implicit def richString(string: String) = new RichString(string)

  }

}
