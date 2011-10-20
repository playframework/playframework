package play.api

import java.io.File

/**
 * Helper for PlayException
 */
object PlayException {

  private val generator = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis)

  /**
   * Generate a new unique exception id.
   */
  def nextId = java.lang.Long.toString(generator.incrementAndGet, 26)

  /**
   * Add source attachment to a Play exception
   */
  trait ExceptionSource {
    self: PlayException =>

    /**
     * Error line if defined.
     */
    def line: Option[Int]

    /**
     * Column position if defined.
     */
    def position: Option[Int]

    /**
     * Input stream used to read the source coutent.
     */
    def input: Option[scalax.io.Input]

    /**
     * Source name.
     */
    def sourceName: Option[String]

    /**
     * Extract interesting lines to be displayed to the user.
     *
     * @param border Number of lines to use a border.
     */
    def interestingLines(border: Int = 4): Option[(Int, Seq[String], Int)] = {
      for (f <- input; l <- line; val (first, last) = f.slurpString.split('\n').splitAt(l - 1); focus <- last.headOption) yield {
        val before = first.takeRight(border)
        val after = last.drop(1).take(border)
        val firstLine = l - before.size
        val errorLine = before.size
        (firstLine, (before :+ focus) ++ after, errorLine)
      }
    }

  }

  /**
   * Add any attachment to a Play exception
   */
  trait ExceptionAttachment {
    self: PlayException =>

    /**
     * Content title.
     */
    def subTitle: String

    /**
     * Content to be displayed.
     */
    def content: String

  }

  /**
   * Add a rich description to a Play exception
   */
  trait RichDescription {
    self: PlayException =>

    /**
     * The new description formatted as HTML.
     */
    def htmlDescription: String

  }

}

/**
 * Root exception for all Play problems.
 *
 * @param title The problem title.
 * @param description The problem description.
 * @param cause The cause exception if exists.
 */
case class PlayException(title: String, description: String, cause: Option[Throwable] = None) extends RuntimeException("%s [%s]".format(title, description), cause.orNull) {

  /**
   * The exception id, useful to retrieve problems in log file.
   */
  val id = PlayException.nextId

}

/**
 * Generic exception for unexpected cases.
 */
case class UnexpectedException(message: Option[String] = None, unexpected: Option[Throwable] = None) extends PlayException(
  "Unexpected exception",
  message.getOrElse {
    unexpected.map(t => "%s: %s".format(t.getClass.getSimpleName, t.getMessage)).getOrElse("")
  },
  unexpected)

