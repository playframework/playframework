package sbt

import play.api._
import play.core._
import Keys._
import PlayExceptions._

trait PlayReloader {
  this: PlayCommands =>

  // ----- Reloader

  def newReloader(state: State, playReload: TaskKey[sbt.inc.Analysis], baseLoader: ClassLoader) = {

    val extracted = Project.extract(state)

    new SBTLink {

      def projectPath = extracted.currentProject.base

      def watchFiles = {
        inAllDependencies(extracted.currentRef, baseDirectory, Project structure state).map {currentProjectPath =>
        if (currentProjectPath != projectPath)
          (currentProjectPath / "src" / "main" ** "*") +++ (currentProjectPath / "app" ** "*") +++ (currentProjectPath / "conf" ** "*") 
        else 
          (currentProjectPath / "conf" ** "*")  
        }.foldLeft(PathFinder.empty)(_ +++ _).get
      }

      // ----- Internal state used for reloading is kept here

      var currentApplicationClassLoader: Option[ClassLoader] = None

      var reloadNextTime = false
      var currentProducts = Map.empty[java.io.File, Long]
      var currentAnalysis = Option.empty[sbt.inc.Analysis]
      var lastHash = Option.empty[String]

      def markdownToHtml(markdown: String, link: String => (String, String)) = {
        import org.pegdown._
        import org.pegdown.ast._

        val processor = new PegDownProcessor(Extensions.ALL)
        val links = new LinkRenderer {
          override def render(node: WikiLinkNode) = {
            val (href, text) = link(node.getText)
            new LinkRenderer.Rendering(href, text)
          }
        }

        processor.markdownToHtml(markdown, links)
      }

      def forceReload() {
        reloadNextTime = true
        lastHash = None
      }

      def clean() {
        currentApplicationClassLoader = None
        currentProducts = Map.empty[java.io.File, Long]
        currentAnalysis = None
      }

      def updateAnalysis(newAnalysis: sbt.inc.Analysis) = {
        val classFiles = newAnalysis.stamps.allProducts ++ watchFiles
        val newProducts = classFiles.map { classFile =>
          classFile -> classFile.lastModified
        }.toMap
        val updated = if (newProducts != currentProducts || reloadNextTime) {
          Some(newProducts)
        } else {
          None
        }
        updated.foreach(currentProducts = _)
        currentAnalysis = Some(newAnalysis)

        reloadNextTime = false

        updated
      }

      def findSource(className: String) = {
        val topType = className.split('$').head
        currentAnalysis.flatMap { analysis =>
          analysis.apis.internal.flatMap {
            case (sourceFile, source) => {
              source.api.definitions.find(defined => defined.name == topType).map(_ => {
                sourceFile: java.io.File
              })
            }
          }.headOption
        }
      }

      def remapProblemForGeneratedSources(problem: xsbti.Problem) = {

        problem.position.sourceFile.collect {

          // Templates
          case play.templates.MaybeGeneratedSource(generatedSource) => {
            new xsbti.Problem {
              def message = problem.message
              def position = new xsbti.Position {
                def line = {
                  problem.position.line.map(l => generatedSource.mapLine(l.asInstanceOf[Int])).map(l => xsbti.Maybe.just(l.asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                }
                def lineContent = ""
                def offset = xsbti.Maybe.nothing[java.lang.Integer]
                def pointer = {
                  problem.position.offset.map { offset =>
                    generatedSource.mapPosition(offset.asInstanceOf[Int]) - IO.read(generatedSource.source.get).split('\n').take(problem.position.line.map(l => generatedSource.mapLine(l.asInstanceOf[Int])).get - 1).mkString("\n").size - 1
                  }.map { p =>
                    xsbti.Maybe.just(p.asInstanceOf[java.lang.Integer])
                  }.getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                }
                def pointerSpace = xsbti.Maybe.nothing[String]
                def sourceFile = xsbti.Maybe.just(generatedSource.source.get)
                def sourcePath = xsbti.Maybe.just(sourceFile.get.getCanonicalPath)
              }
              def severity = problem.severity
            }
          }

          // Routes files
          case play.core.Router.RoutesCompiler.MaybeGeneratedSource(generatedSource) => {
            new xsbti.Problem {
              def message = problem.message
              def position = new xsbti.Position {
                def line = {
                  problem.position.line.flatMap(l => generatedSource.mapLine(l.asInstanceOf[Int])).map(l => xsbti.Maybe.just(l.asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                }
                def lineContent = ""
                def offset = xsbti.Maybe.nothing[java.lang.Integer]
                def pointer = xsbti.Maybe.nothing[java.lang.Integer]
                def pointerSpace = xsbti.Maybe.nothing[String]
                def sourceFile = xsbti.Maybe.just(new File(generatedSource.source.get.path))
                def sourcePath = xsbti.Maybe.just(sourceFile.get.getCanonicalPath)
              }
              def severity = problem.severity
            }
          }

        }.getOrElse {
          problem
        }

      }

      def getProblems(incomplete: Incomplete): Seq[xsbti.Problem] = {
        (Compiler.allProblems(incomplete) ++ {
          Incomplete.linearize(incomplete).filter(i => i.node.isDefined && i.node.get.isInstanceOf[ScopedKey[_]]).flatMap { i =>
            val JavacError = """\[error\]\s*(.*[.]java):(\d+):\s*(.*)""".r
            val JavacErrorInfo = """\[error\]\s*([a-z ]+):(.*)""".r
            val JavacErrorPosition = """\[error\](\s*)\^\s*""".r

            Project.runTask(streamsManager, state).map(_._2).get.toEither.right.toOption.map { streamsManager =>
              var first: (Option[(String, String, String)], Option[Int]) = (None, None)
              var parsed: (Option[(String, String, String)], Option[Int]) = (None, None)
              Output.lastLines(i.node.get.asInstanceOf[ScopedKey[_]], streamsManager).map(_.replace(scala.Console.RESET, "")).map(_.replace(scala.Console.RED, "")).collect {
                case JavacError(file, line, message) => parsed = Some((file, line, message)) -> None
                case JavacErrorInfo(key, message) => parsed._1.foreach { o =>
                  parsed = Some((parsed._1.get._1, parsed._1.get._2, parsed._1.get._3 + " [" + key.trim + ": " + message.trim + "]")) -> None
                }
                case JavacErrorPosition(pos) => {
                  parsed = parsed._1 -> Some(pos.size)
                  if (first == (None, None)) {
                    first = parsed
                  }
                }
              }
              first
            }.collect {
              case (Some(error), maybePosition) => new xsbti.Problem {
                def message = error._3
                def position = new xsbti.Position {
                  def line = xsbti.Maybe.just(error._2.toInt)
                  def lineContent = ""
                  def offset = xsbti.Maybe.nothing[java.lang.Integer]
                  def pointer = maybePosition.map(pos => xsbti.Maybe.just((pos - 1).asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                  def pointerSpace = xsbti.Maybe.nothing[String]
                  def sourceFile = xsbti.Maybe.just(file(error._1))
                  def sourcePath = xsbti.Maybe.just(error._1)
                }
                def severity = xsbti.Severity.Error
              }
            }

          }
        }).map(remapProblemForGeneratedSources)
      }

      private val classLoaderVersion = new java.util.concurrent.atomic.AtomicInteger(0)

      private def newClassLoader = {
        val loader = new java.net.URLClassLoader(
          Project.runTask(dependencyClasspath in Runtime, state).map(_._2).get.toEither.right.get.map(_.data.toURI.toURL).toArray,
          baseLoader) {

          val version = classLoaderVersion.incrementAndGet

          override def toString = {
            "ReloadableClassLoader(v" + version + ") {" + {
              getURLs.map(_.toString).mkString(", ")
            } + "}"
          }

        }
        currentApplicationClassLoader = Some(loader)
        loader
      }

      def reload: Either[Throwable, Option[ClassLoader]] = {

        PlayProject.synchronized {

          val hash = Project.runTask(playHash, state).map(_._2).get.toEither.right.get

          lastHash.filter(_ == hash).map { _ => Right(None) }.getOrElse {

            lastHash = Some(hash)

            val r = Project.runTask(playReload, state).map(_._2).get.toEither
              .left.map { incomplete =>
                lastHash = None
                Incomplete.allExceptions(incomplete).headOption.map {
                  case e: PlayException => e
                  case e: xsbti.CompileFailed => {
                    getProblems(incomplete).headOption.map(CompilationException(_)).getOrElse {
                      UnexpectedException(Some("Compilation failed without reporting any problem!?"), Some(e))
                    }
                  }
                  case e => UnexpectedException(unexpected = Some(e))
                }.getOrElse {
                  UnexpectedException(Some("Compilation task failed without any exception!?"))
                }
              }
              .right.map { compilationResult =>
                updateAnalysis(compilationResult).map { _ =>
                  newClassLoader
                }
              }

            r

          }

        }

      }

      def runTask(task: String): Option[Any] = {

        val parser = Act.scopedKeyParser(state)
        val Right(sk: ScopedKey[Task[_]]) = complete.DefaultParsers.result(parser, task)
        val result = Project.runTask(sk, state).map(_._2)

        result.flatMap(_.toEither.right.toOption)

      }

      def definedTests: Seq[String] = {
        Project.runTask(Keys.definedTests in Test, state).map(_._2).get.toEither
          .left.map { incomplete =>
            Incomplete.allExceptions(incomplete).headOption.map {
              case e: PlayException => e
              case e: xsbti.CompileFailed => {
                getProblems(incomplete).headOption.map(CompilationException(_)).getOrElse {
                  UnexpectedException(Some("Compilation failed without reporting any problem!?"), Some(e))
                }
              }
              case e => UnexpectedException(unexpected = Some(e))
            }.getOrElse(
              UnexpectedException(Some("Compilation task failed without any exception!?")))
          }
          .right.map(_.map(_.name))
          .left.map(throw _)
          .right.get
      }

      def runTests(only: Seq[String], callback: Any => Unit): Either[String, Boolean] = {

        try {
          if (only == Nil) {
            Command.process("test", state)
            Right(true)
          } else {
            Command.process("test-only " + only.mkString(" "), state)
            Right(true)
          }
        } catch {
          case incomplete: sbt.Incomplete => {
            Left({
              Incomplete.allExceptions(incomplete).headOption.map {
                case e: xsbti.CompileFailed => "Compilation failed"
                case e => e.getMessage
              }.getOrElse("Unexpected failure")
            })
          }
          case unexpected => {
            Left("Unexpected failure [" + unexpected.getClass.getName + "]")
          }
        }

      }

    }

  }
}