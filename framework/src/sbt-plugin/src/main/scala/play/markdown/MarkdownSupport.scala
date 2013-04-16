package play.markdown

import java.io.{ IOException, File }
import org.pegdown.plugins._
import org.pegdown.ast._
import org.pegdown._
import scalax.file.Path
import java.util

trait MarkdownSupport {
  def markdownToHtml(markdown: String, pagePath: String, root: File): String = {

    val link: (String => (String, String)) = _ match {
      case link if link.contains("|") => {
        val parts = link.split('|')
        (parts.tail.head, parts.head)
      }
      case image if image.endsWith(".png") => {
        val link = image match {
          case full if full.startsWith("http://") => full
          case absolute if absolute.startsWith("/") => "resources/manual" + absolute
          case relative => "resources/" + pagePath + "/" + relative
        }
        (link, """<img src="""" + link + """"/>""")
      }
      case link => {
        (link, link)
      }
    }

    val processor = new PegDownProcessor(Extensions.ALL, PegDownPlugins.builder()
      .withPlugin(classOf[CodeReferenceParser]).build)
    val links = new LinkRenderer {
      override def render(node: WikiLinkNode) = {
        val (href, text) = link(node.getText)
        new LinkRenderer.Rendering(href, text)
      }
    }

    val astRoot = processor.parseMarkdown(markdown.toCharArray)
    new ToHtmlSerializer(links, util.Arrays.asList(new CodeReferenceSerializer(root, pagePath))).toHtml(astRoot)
  }

  // Directives to insert code, skip code and replace code
  private val Insert = """.*###insert: (.*?)(?:###.*)?""".r
  private val SkipN = """.*###skip:\s*(\d+).*""".r
  private val Skip = """.*###skip.*""".r
  private val ReplaceNext = """.*###replace: (.*?)(?:###.*)?""".r

  private class CodeReferenceSerializer(root: File, pagePath: String) extends ToHtmlSerializerPlugin {
    def visit(node: Node, visitor: Visitor, printer: Printer) = node match {
      case code: CodeReferenceNode => {

        // Label is after the #, or if no #, then is the link label
        val (source, label) = code.getSource.split("#", 2) match {
          case Array(source, label) => (source, label)
          case Array(source) => (source, code.getLabel)
        }

        // The file is either relative to current page page or absolute, under the root
        val sourceFile = if (source.startsWith("/")) {
          new File(root, source.drop(1))
        } else {
          new File(new File(root, pagePath), source)
        }

        if (!sourceFile.exists()) {
          throw new IOException("Could not find source file: " + sourceFile)
        }

        // Find the code segment
        val sourceCode = Path(sourceFile).lines()
        val notLabel = (s: String) => !s.contains("#" + label)
        val segment = sourceCode dropWhile (notLabel) drop (1) takeWhile (notLabel)
        if (segment.isEmpty) {
          throw new IllegalArgumentException("Could not find segment " + label + " in source file " + sourceFile)
        }

        // Calculate the indent, which is equal to the smallest indent of any line, excluding lines that only consist
        // of space characters
        val indent = segment map { line =>
          if (!line.exists(_ != ' ')) None else Some(line.indexWhere(_ != ' '))
        } reduce ((i1, i2) => (i1, i2) match {
          case (None, None) => None
          case (i, None) => i
          case (None, i) => i
          case (Some(i1), Some(i2)) => Some(math.min(i1, i2))
        }) getOrElse (0)

        // Process directives in segment
        case class State(buffer: StringBuilder = new StringBuilder, skip: Option[Int] = None) {
          def dropIndentAndAppendLine(s: String): State = {
            buffer.append(s.drop(indent)).append("\n")
            this
          }
          def appendLine(s: String): State = {
            buffer.append(s).append("\n")
            this
          }
        }
        val compiledSegment = (segment.foldLeft(State()) { (state, line) =>
          state.skip match {
            case Some(n) if (n > 1) => state.copy(skip = Some(n - 1))
            case Some(n) => state.copy(skip = None)
            case None => line match {
              case Insert(code) => state.appendLine(code)
              case SkipN(n) => state.copy(skip = Some(n.toInt))
              case Skip() => state
              case ReplaceNext(code) => state.appendLine(code).copy(skip = Some(1))
              case _ => state.dropIndentAndAppendLine(line)
            }
          }
        }).buffer /* Drop last newline */ .dropRight(1).toString()

        // Guess the type of the file
        val fileType = source.split("\\.") match {
          case withExtension if (withExtension.length > 1) => Some(withExtension.last)
          case _ => None
        }

        // And visit it
        fileType.map(t => new VerbatimNode(compiledSegment, t)).getOrElse(new VerbatimNode(compiledSegment)).accept(visitor)

        true
      }
      case _ => false
    }
  }

}
