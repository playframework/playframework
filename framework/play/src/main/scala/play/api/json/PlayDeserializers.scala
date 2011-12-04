package play.api.json

import AST._

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import scala.collection.{ Traversable, MapLike, immutable, mutable }
import scala.collection.generic.{ MapFactory, GenericCompanion }

class PlayDeserializers(classLoader: ClassLoader) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig,
    provider: DeserializerProvider, beanDesc: BeanDescription,
    property: BeanProperty) = {
    val klass = javaType.getRawClass
    if (classOf[JsValue].isAssignableFrom(klass) || klass == JsNull.getClass) {
      new JsValueDeserializer(config.getTypeFactory, klass)
    } else null
  }

}
