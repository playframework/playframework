/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data.models

import java.util.Date
import scala.beans.BeanProperty
import scala.annotation.meta.field

class Task {

  type Min = play.data.validation.Constraints.Min @field
  type Required = play.data.validation.Constraints.Required @field
  type I18NConstraint = play.data.validation.TestConstraints.I18Constraint @field
  type DateTime = play.data.format.Formats.DateTime @field

  @Min(10)
  @BeanProperty
  var id: Long = _

  @Required
  @BeanProperty
  var name: String = _

  @BeanProperty
  var done: Boolean = true

  @BeanProperty
  @Required
  @DateTime(pattern = "dd/MM/yyyy")
  var dueDate: Date = _

  @BeanProperty
  var endDate: Date = _

  @BeanProperty
  @I18NConstraint(value = "patterns.zip")
  var zip: String = _

}

