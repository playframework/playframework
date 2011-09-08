package play.templates

case class Html(text:String) extends Appendable[Html] {
    val buffer = new StringBuilder(text)

    def +(other:play.templates.Html) = {
        buffer.append(other.buffer)
        this
    }
    override def toString = buffer.toString
    
}

object Html {
    def empty = Html("")
}

object HtmlFormat extends Format[Html] {
    def raw(text:String) = Html(text)
    def escape(text:String) = Html(text.replace("<","&lt;"))
}