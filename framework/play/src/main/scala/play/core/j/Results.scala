package play.api.mvc

object JResults extends Results {
  def writeContent: Writeable[Content] = writeableOf_Content[Content]
  def writeString: Writeable[String] = Writeable.wString
  def writeEmpty: Writeable[Results.Empty] = writeableOf_Empty
  def contentTypeOfString: ContentTypeOf[String] = contentTypeOf_String
  def contentTypeOf(mimeType: String): ContentTypeOf[Content] = ContentTypeOf(Option(mimeType))
  def contentTypeOfEmpty: ContentTypeOf[Results.Empty] = contentTypeOf_Empty
  def emptyHeaders = Map.empty[String, String]
  def empty = Results.Empty()
}