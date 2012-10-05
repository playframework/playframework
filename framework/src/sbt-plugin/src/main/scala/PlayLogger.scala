package sbt

import xsbti.{Maybe, Position}

object InPlaceLogger {

  private var lastMessage: String = null

  def log(message: String) {
    val logged = "[info] " + message + "\r"
    print(logged)
    lastMessage = logged
  }

  def clear() {
    if (lastMessage != null) {
      print(" " * lastMessage.size + "\r")
      lastMessage = null
    }
  }

}

object PlayLogManager {

  def default(playPositionMapper: Position => Option[Position])(extra: ScopedKey[_] => Seq[AbstractLogger]) =
    new PlayLogManager(extra, playPositionMapper)

}

class PlayLogManager(extra: ScopedKey[_] => Seq[AbstractLogger], playPositionMapper: Position => Option[Position]) extends LogManager {
  //reintroduce missing methods
  def defaultScreen: AbstractLogger = ConsoleLogger()
  //reintroduce missing methods
  def defaultBacked(useColor: Boolean = ConsoleLogger.formatEnabled): java.io.PrintWriter => ConsoleLogger =
    to => ConsoleLogger(ConsoleLogger.printWriterOut(to), useColor = useColor)

  val screen = defaultScreen
  val backed = defaultBacked()
  val sourcePositionFilter = new PlaySourcePositionFilter(playPositionMapper)

  def apply(data: Settings[Scope], state: State, task: ScopedKey[_], to: java.io.PrintWriter): Logger = {
    new FilterLogger(
      delegate = LogManager.defaultLogger(data, state, task, screen, backed(to), extra(task).toList).asInstanceOf[AbstractLogger]
    ) {

      override def log(level: Level.Value, message: => String): Unit =  {
        if (atLevel(level)) {
          sourcePositionFilter.filter(level, message) { (level, message) =>
            InPlaceLogger.clear()
            if (!message.contains("Running java with options -classpath")) {
              if (filtered(message)) {
                InPlaceLogger.log(message)
              } else {
                super.log(level, message)
              }
            }
          }
        }
      }

      def filtered(message: String) = {
        message.startsWith("Resolving ") && message.endsWith("...")
      }

    }
  }
}

/**
 * This class is a temporary solution to mapping compiler errors of generated source files back to the original source
 * files.  Once we upgrade to SBT 0.13, we can basically throw this away, and replace it with a single configuration
 * line in PlaySettings, assigning sourcePositionMappers := playPositionMappers.  This also explains why this class
 * works the way it does (creating the intermediate position and mapping that), it's reusing the mappers that written
 * for the future features.
 */
class PlaySourcePositionFilter(val playPositionMapper: Position => Option[Position]) {
  import scala.collection.mutable
  import play.templates.{MaybeGeneratedSource => TemplateSource}
  import play.router.RoutesCompiler.{MaybeGeneratedSource => RoutesSource}

  val CompileError = """(?s)(.*\.scala):(\d+): (.*)""".r
  val Pointer = """([\t ]*)\^""".r
  val CompilationFailed = """.*Compilation failed"""
  private val buffer = mutable.ArrayBuffer[(Level.Value, String)]()
  private var current: Option[(File, Int, String)] = None


  def filter(level: Level.Value, message: String)(log: (Level.Value, => String) => Unit) {
    buffer.synchronized {
      if (current.isDefined) {
        if (level < Level.Warn || buffer.size > 2) {
          // Uh oh, don't know what's going on here. Continue as if nothing happened.
          continueLogging(level, message, log)
        } else {
          message match {
            // We have successfully captured a compiler error, map and log it.
            case Pointer(spaces) => mapAndLogCurrentError(level, spaces, log)
            // Another one? Log the current buffer, and process the new one.
            case CompileError(filename, line, error) => {
              drain(log)
              current = None
              matchCompileError(level, message, filename, line, error, log)
            }
            // End of compilation, continue as if nothing happened.
            case CompilationFailed => continueLogging(level, message, log)
            // We're in the middle of buffering a compiler error
            case _ => buffer.append((level, message))
          }
        }
      } else {
        if (level >= Level.Warn) {
          message match {
            // It's a compile error, check if it's for play generated sources
            case CompileError(filename, line, error) => {
              matchCompileError(level, message, filename, line, error, log)
            }
            // It's not a compile error, continue as normal
            case _ => log(level, message)
          }
        } else {
          log(level, message)
        }
      }
    }
  }

  def matchCompileError(level: Level.Value, message: String, filename: String, line: String, error: String,
                        log: (Level.Value, => String) => Unit) {
    val file = new File(filename)
    file match {
      case TemplateSource(generatedSource) => {
        buffer.append((level, message))
        current = Some((file, line.toInt, error))
      }
      case RoutesSource(generatedSource) => {
        buffer.append((level, message))
        current = Some((file, line.toInt, error))
      }
      case _ => log(level, message)
    }
  }

  def mapAndLogCurrentError(level: Level.Value, spaces: String, log: (Level.Value, => String) => Unit) {
    // Create a position
    val (file, lineNo, message) = current.get
    val position = new Position {
      def line() = Maybe.just(lineNo)
      def lineContent() = buffer(buffer.size - 1)._2
      lazy val offset = Maybe.just((IO.read(file).split("\n").slice(0, lineNo - 1).map(_.length() + 1).reduce((a, b) => a + b) + spaces.length).asInstanceOf[java.lang.Integer])
      def pointer() = Maybe.just(spaces.length + 1)
      def pointerSpace() = Maybe.just(spaces)
      def sourcePath() = Maybe.just(file.name)
      def sourceFile() = Maybe.just(file)
    }
    // Map the position
    val pos = playPositionMapper(position).get

    // Log it, this code copied a little out of SBT
    import sbt.Logger.m2o
    val sourcePrefix = m2o(pos.sourcePath).getOrElse("")
    val lineNumberString = m2o(pos.line).map(":" + _ + ":").getOrElse(":") + " "
    log(level, sourcePrefix + lineNumberString + message)
    buffer.slice(1, buffer.size - 1).foreach(m => log(m._1, m._2))

    val lineContent = pos.lineContent
    if(!lineContent.isEmpty) {
      log(level, lineContent)
      for(space <- m2o(pos.pointerSpace))
        log(level, space + "^") // pointer to the column position of the error/warning
    }

    current = None
    buffer.clear()
  }

  def continueLogging(level: Level.Value, message: String, log: (Level.Value, => String) => Unit) {
    drain(log)
    log(level, message)
    current = None
  }

  def drain(log: (Level.Value, => String) => Unit) {
    buffer foreach { m =>
      log(m._1, m._2)
    }
    buffer.clear()
  }
}