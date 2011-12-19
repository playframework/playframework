package play.api.mvc

object JResults extends Results {
  def writeContent(codec: Codec): Writeable[Content] = writeableOf_Content[Content](codec)
  def writeString(codec: Codec): Writeable[String] = Writeable.wString(codec)
  def writeBytes: Writeable[Array[Byte]] = Writeable.wBytes
  def writeEmptyContent: Writeable[Results.EmptyContent] = writeableOf_EmptyContent
  def contentTypeOfString(codec: Codec): ContentTypeOf[String] = contentTypeOf_String(codec)
  def contentTypeOf(mimeType: String): ContentTypeOf[Content] = ContentTypeOf(Option(mimeType))
  def contentTypeOfEmptyContent: ContentTypeOf[Results.EmptyContent] = contentTypeOf_EmptyContent
  def noContentType[A]: ContentTypeOf[A] = ContentTypeOf(None)
  def contentTypeOfBytes: ContentTypeOf[Array[Byte]] = ContentTypeOf(Some("application/octet-stream"))
  def emptyHeaders = Map.empty[String, String]
  def empty = Results.EmptyContent()
  def async(p: play.api.libs.concurrent.Promise[Result]) = AsyncResult(p)
  def chunked[A] = new play.api.libs.iteratee.CallbackEnumerator[A]
}