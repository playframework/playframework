package play.api.mvc

object JResults extends Results {
  def writeContent: Writeable[Content] = writeableStringOf_Content[Content]
  def writeString: Writeable[String] = writeableStringOf_String
  def writeEmpty: Writeable[Results.Empty] = writeableStringOf_Empty
  def contentTypeOfString: ContentTypeOf[String] = contentTypeOf_String
  def contentTypeOfContent: ContentTypeOf[Content] = contentTypeOf_Content[Content]
  def contentTypeOfEmpty: ContentTypeOf[Results.Empty] = contentTypeOf_Empty
  def emptyHeaders = Map.empty[String, String]
  def empty = Results.Empty()
}