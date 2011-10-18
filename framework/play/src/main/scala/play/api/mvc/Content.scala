package play.api.mvc

trait Content {
  def body: String
  def contentType: String
}