package play.markdown

import java.io.File
import org.pegdown.plugins.{PegDownPlugins, ToHtmlSerializerPlugin}
import org.pegdown.ast.{VerbatimNode, Visitor, Node}
import org.pegdown.Printer
import scalax.file.Path
import java.util

object MarkdownSupport {
  def markdownToHtml(markdown: String, pagePath: String, root: File): String = {
    import org.pegdown._
    import org.pegdown.ast._

    val link:(String => (String, String)) = _ match {
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

  class CodeReferenceSerializer(root: File, pagePath: String) extends ToHtmlSerializerPlugin {
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
          throw new IllegalArgumentException("Could not find source file: " + sourceFile)
        }

        // Find the code segment
        val sourceCode = Path(sourceFile).lines()
        val notLabel = (s: String) => !s.contains("#" + label)
        val segment = sourceCode dropWhile(notLabel) drop(1) takeWhile(notLabel) mkString("\n")

        if (segment.isEmpty) {
          throw new IllegalArgumentException("Could not find segment " + label + " in source file " + sourceFile)
        }

        // Guess the type of the file
        val fileType = source.split("\\.") match {
          case withExtension if (withExtension.length > 1) => Some(withExtension.last)
          case _ => None
        }

        // And visit it
        fileType.map(t => new VerbatimNode(segment, t)).getOrElse(new VerbatimNode(segment)).accept(visitor)

        true
      }
      case _ => false
    }
  }

}
