/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.json

case class JsonConfiguration(naming: JsonNaming)

trait LowPriorityDefaultJsonConfigurationImplicit {

  implicit val defaultConfiguration: JsonConfiguration = JsonConfiguration(JsonNaming.Identity)

}

object JsonConfiguration extends LowPriorityDefaultJsonConfigurationImplicit

/**
 * Naming strategy, to map each class property to the corresponding column.
 */
trait JsonNaming extends (String => String) {
  /**
   * Returns the column name for the class property.
   *
   * @param property the name of the case class property
   */
  def apply(property: String): String
}

/** Naming companion */
object JsonNaming {

  /**
   * For each class property, use the name
   * as is for its column (e.g. fooBar -> fooBar).
   */
  object Identity extends JsonNaming {

    override def apply(property: String): String = property
  }

  /**
   * For each class property, use the snake case equivalent
   * to name its column (e.g. fooBar -> foo_bar).
   */
  object SnakeCase extends JsonNaming {

    def apply(property: String): String = {
      val length = property.length
      val result = new StringBuilder(length * 2)
      var resultLength = 0
      var wasPrevTranslated = false
      for (i <- 0 until length) {
        var c = property.charAt(i)
        if (i > 0 || i != '_') {
          if (Character.isUpperCase(c)) {
            // append a underscore if the previous result wasn't translated
            if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
              result.append('_')
              resultLength += 1
            }
            c = Character.toLowerCase(c)
            wasPrevTranslated = true
          } else {
            wasPrevTranslated = false
          }
          result.append(c)
          resultLength += 1
        }
      }

      // builds the final string
      result.toString()
    }

    /** Naming using a custom transformation function. */
    def apply(transformation: String => String): JsonNaming =
      new JsonNaming {
        def apply(property: String): String = transformation(property)
      }
  }

}