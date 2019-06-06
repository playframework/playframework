import java.io.FileWriter
import java.util.Date

import com.google.inject.AbstractModule
import play.api._

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() = {
    val writer = new FileWriter(environment.getFile("target/reload.log"), true)
    writer.write(new Date() + " - reloaded\n")
    writer.close()

    if (configuration.getOptional[Boolean]("fail").getOrElse(false)) {
      throw new RuntimeException("fail=true")
    }
  }
}
