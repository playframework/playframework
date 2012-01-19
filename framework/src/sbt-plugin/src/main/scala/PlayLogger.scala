package sbt

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

  def default(extra: ScopedKey[_] => Seq[AbstractLogger]) = new PlayLogManager(extra)

}

class PlayLogManager(extra: ScopedKey[_] => Seq[AbstractLogger]) extends LogManager {

  val screen = LogManager.defaultScreen
  val backed = LogManager.defaultBacked()

  def apply(data: Settings[Scope], state: State, task: ScopedKey[_], to: java.io.PrintWriter): Logger = {
    new FilterLogger(
      delegate = LogManager.defaultLogger(data, state, task, screen, backed(to), extra(task).toList).asInstanceOf[AbstractLogger]
    ) {

      override def log(level: Level.Value, message: => String) {
        if (atLevel(level)) {
          InPlaceLogger.clear()
          if (filtered(message)) {
            InPlaceLogger.log(message)
          } else {
            super.log(level, message)
          }
        }
      }

      def filtered(message: String) = {
        message.startsWith("Resolving ") && message.endsWith("...")
      }

    }
  }
}