package play.data.models

import java.util.Date
import reflect.BeanProperty
import annotation.target.field

class Task {

  type Min = play.data.validation.Constraints.Min @field
  type Required = play.data.validation.Constraints.Required @field
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

}

