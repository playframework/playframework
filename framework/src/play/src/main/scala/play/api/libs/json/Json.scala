package play.api.libs.json

object Json {

  /**
   * Parse a String representing a json, and return it as a JsValue
   * @param input a String to parse
   * @return the JsValue representing the string
   */
  def parse(input: String): JsValue = JerksonJson.parse[JsValue](input)

  /**
   * Convert a JsValue to its string representation.
   * @param json the JsValue to convert
   * @return a String with the json representation
   */
  def stringify(json: JsValue): String = JerksonJson.generate(json)

}
