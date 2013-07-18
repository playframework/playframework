import org.pegdown.ast.{Visitor, Node, WikiLinkNode}
import org.pegdown.plugins.{ToHtmlSerializerPlugin, PegDownPlugins}
import org.pegdown._
import play.console.Colors
import play.doc.{CodeReferenceNode, CodeReferenceParser}
import sbt._
import sbt.Keys._
import sbt.File
import scala.collection.mutable

// Test that all the docs are renderable and valid
object DocValidation {

  case class LinkRef(link: String, file: File, position: Int)

  val validateDocs = TaskKey[Unit]("validate-docs", "Validates the play docs to ensure they compile and that all links resolve.", KeyRanks.APlusTask)
  val ValidateDocsTask = (state, baseDirectory, logManager) map { (s, base, logManager) =>
    val log = s.log

    val markdownFiles = (Path(base) / "manual" ** "*.md").get
    val pages = markdownFiles.map(f => f.getName.dropRight(3) -> f).toMap

    var failed = false

    def doAssertion(desc: String, errors: Seq[_])(onFail: => Unit) {
      if (errors.isEmpty) {
        log.info("[" + Colors.green("pass") + "] " + desc)
      } else {
        failed = true
        onFail
        log.info("[" + Colors.red("fail") + "] " + desc + " (" + errors.size + " errors)")
      }
    }

    val duplicates = markdownFiles
      .filterNot(_.getName.startsWith("_"))
      .groupBy(s => s.getName)
      .filter(v => v._2.size > 1)

    doAssertion("Duplicate markdown file name test", duplicates.toSeq) {
      duplicates.foreach { d =>
        log.error(d._1 + ":\n" + d._2.mkString("\n    "))
      }
    }

    val allLinks = mutable.Set[String]()
    val missingLinks = mutable.ListBuffer[LinkRef]()
    val missingResources = mutable.ListBuffer[LinkRef]()
    val missingCode = mutable.ListBuffer[LinkRef]()
    val missingCodeSegments = mutable.ListBuffer[LinkRef]()

    def testMarkdownFile(markdownFile: File): String = {

      val processor = new PegDownProcessor(Extensions.ALL, PegDownPlugins.builder()
        .withPlugin(classOf[CodeReferenceParser]).build)

      // Link renderer will also verify that all wiki links exist
      val linkRenderer = new LinkRenderer {
        override def render(node: WikiLinkNode) = {
          node.getText match {
            case link if link.contains("|") => {
              val parts = link.split('|')
              val desc = parts.head
              val page = parts.tail.head.trim
              allLinks.add(page.toLowerCase)
              if (!pages.contains(page)) {
                missingLinks.append(LinkRef(page, markdownFile, node.getStartIndex + desc.length + 3))
              }
            }
            case image if image.endsWith(".png") => {
              image match {
                case full if full.startsWith("http://") => full
                case absolute if absolute.startsWith("/") => {
                  // Ensure the file exists
                  val linkFile = new File(base, "manual" + absolute)
                  if (!linkFile.isFile) {
                    missingResources.append(LinkRef(linkFile.getAbsolutePath, markdownFile, node.getStartIndex + 1))
                  }
                }
                case relative => {
                  val linkFile = new File(markdownFile.getParentFile, relative)
                  if (!linkFile.isFile) {
                    missingResources.append(LinkRef(linkFile.getAbsolutePath, markdownFile, node.getStartIndex + 1))
                  }
                }
              }
            }
            case link => {
              val page = link.trim
              allLinks.add(page.toLowerCase)
              if (!pages.contains(page)) {
                missingLinks.append(LinkRef(page, markdownFile, node.getStartIndex + 2))
              }
            }
          }
          new LinkRenderer.Rendering("foo", "bar")
        }
      }

      val codeReferenceSerializer = new ToHtmlSerializerPlugin() {
        def visit(node: Node, visitor: Visitor, printer: Printer) = node match {
          case code: CodeReferenceNode => {

            // Label is after the #, or if no #, then is the link label
            val (source, label) = code.getSource.split("#", 2) match {
              case Array(source, label) => (source, label)
              case Array(source) => (source, code.getLabel)
            }

            // The file is either relative to current page page or absolute, under the root
            val sourceFile = if (source.startsWith("/")) {
              new File(base, source.drop(1))
            } else {
              new File(markdownFile.getParentFile, source)
            }

            val sourcePos = code.getStartIndex + code.getLabel.length + 4
            if (!sourceFile.exists()) {
              missingCode.append(LinkRef(sourceFile.getAbsolutePath, markdownFile, sourcePos))
            } else {
              // Find the code segment
              val sourceCode = scalax.file.Path(sourceFile).lines()
              val notLabel = (s: String) => !s.contains("#" + label)
              val segment = sourceCode dropWhile(notLabel) drop(1) takeWhile(notLabel)
              if (segment.isEmpty) {
                val labelPos = if (code.getSource.contains("#")) {
                  sourcePos + source.length + 1
                } else {
                  code.getStartIndex + 2
                }
                missingCodeSegments.append(LinkRef(label, markdownFile, labelPos))
              }
            }

            true
          }
          case _ => false
        }
      }

      val astRoot = processor.parseMarkdown(scalax.file.Path(markdownFile).chars.toArray)
      new ToHtmlSerializer(linkRenderer, java.util.Arrays.asList[ToHtmlSerializerPlugin](codeReferenceSerializer))
        .toHtml(astRoot)
    }

    markdownFiles.foreach(testMarkdownFile)

    def assertLinksNotMissing(desc: String, links: Seq[LinkRef], errorMessage: String) {
      doAssertion(desc, links) {
        links.foreach { link =>
          // Load the source
          val lines = scalax.file.Path(link.file).lines()
          // Calculate the line and col
          // Tuple is (total chars seen, line no, col no, Option[line])
          val (_, lineNo, colNo, line) = lines.foldLeft((0, 0, 0, None: Option[String])) { (state, line) =>
            state match {
              case (_, _, _, Some(_)) => state
              case (total, l, c, None) => {
                if (total + line.length < link.position) {
                  (total + line.length + 1, l + 1, c, None)
                } else {
                  (0, l + 1, link.position - total + 1, Some(line))
                }
              }
            }
          }
          log.error(errorMessage + " " + link.link + " at " + link.file.getAbsolutePath + ":" + lineNo)
          line.foreach { l =>
            log.error(l)
            log.error(l.take(colNo - 1).map { case '\t' => '\t'; case _ => ' ' } + "^")
          }
        }
      }
    }

    assertLinksNotMissing("Missing wiki links test", missingLinks, "Could not find link")
    assertLinksNotMissing("Missing wiki resources test", missingResources, "Could not find resource")
    assertLinksNotMissing("Missing source files test", missingCode, "Could not find source file")
    assertLinksNotMissing("Missing source segments test", missingCodeSegments, "Could not find source segment")

    val orphanPages = pages.filterNot(page => allLinks.contains(page._1.toLowerCase)).filterNot { page =>
      page._1.startsWith("_") || page._1 == "Home" || page._1.startsWith("Book")
    }
    doAssertion("Orphan pages test", orphanPages.toSeq) {
      orphanPages.foreach { page =>
        log.error("Page " + page._2 + " is not referenced by any links")
      }
    }

    if (failed) {
      throw new RuntimeException("Documentation validation failed")
    }
  }

}

