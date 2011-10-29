package play.data.models

import java.util.Date
import javax.persistence.Entity

import reflect.BeanProperty
import annotation.target.field

@Entity
class Task extends play.db.ebean.Model {

  type Id = javax.persistence.Id @field
  type Min = play.data.validation.Constraints.Min @field
  type Required = play.data.validation.Constraints.Required @field
  type DateTime = play.data.format.Formats.DateTime @field

  @Id
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

