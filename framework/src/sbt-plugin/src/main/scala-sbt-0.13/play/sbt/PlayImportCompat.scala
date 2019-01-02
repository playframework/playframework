/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt.Keys.logManager
import sbt.{ Def, Level, LogManager, Logger, Scope, Settings, State }

/**
 * Fix compatibility issues for PlayImport. This is the version compatible with sbt 0.13.
 */
private[sbt] trait PlayImportCompat {

  /**
   * Add this to your build.sbt, eg:
   *
   * {{{
   *   emojiLogs
   * }}}
   */
  lazy val emojiLogs = logManager ~= { lm =>
    new LogManager {
      def apply(data: Settings[Scope], state: State, task: Def.ScopedKey[_], writer: java.io.PrintWriter) = {
        val l = lm.apply(data, state, task, writer)
        val FailuresErrors = "(?s).*(\\d+) failures?, (\\d+) errors?.*".r
        new Logger {
          def filter(s: String) = {
            val filtered = s.replace("\033[32m+\033[0m", "\u2705 ")
              .replace("\033[33mx\033[0m", "\u274C ")
              .replace("\033[31m!\033[0m", "\uD83D\uDCA5 ")
            filtered match {
              case FailuresErrors("0", "0") => filtered + " \uD83D\uDE04"
              case FailuresErrors(_, _) => filtered + " \uD83D\uDE22"
              case _ => filtered
            }
          }
          def log(level: Level.Value, message: => String) = l.log(level, filter(message))
          def success(message: => String) = l.success(message)
          def trace(t: => Throwable) = l.trace(t)

          override def ansiCodesSupported = l.ansiCodesSupported
        }
      }
    }
  }

}
