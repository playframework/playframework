package play.api.mvc

object JResults extends Results {
  def writeContent: Writeable[Content] = writeableOf_Content[Content]
  def writeString: Writeable[String] = Writeable.wString
  def writeEmptyContent: Writeable[Results.EmptyContent] = writeableOf_EmptyContent
  def contentTypeOfString: ContentTypeOf[String] = contentTypeOf_String
  def contentTypeOf(mimeType: String): ContentTypeOf[Content] = ContentTypeOf(Option(mimeType))
  def contentTypeOfEmptyContent: ContentTypeOf[Results.EmptyContent] = contentTypeOf_EmptyContent
  def emptyHeaders = Map.empty[String, String]
  def empty = Results.EmptyContent()
  def async(p: play.api.libs.concurrent.Promise[Result]) = AsyncResult(p)
}