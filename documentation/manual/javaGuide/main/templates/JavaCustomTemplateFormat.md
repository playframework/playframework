# Adding support for a custom format to the template engine

The built-in template engine supports common template formats (HTML, XML, etc.) but you can easily add support for your own formats, if needed. This page summarizes the steps to follow to support a custom format.

## Overview of the the templating process

The template engine builds its result by appending static and dynamic content parts of a template. Consider for instance the following template:

```
foo @bar baz
```

It consists in two static parts (`foo ` and ` baz`) around one dynamic part (`bar`). The template engine concatenates these parts together to build its result. Actually, in order to prevent cross-site scripting attacks, the value of `bar` can be escaped before being concatenated to the rest of the result. This escaping process is specific to each format: e.g. in the case of HTML you want to transform “<” into “&amp;lt;”.

How does the template engine know which format correspond to a template file? It looks at its extension: e.g. if it ends with `.scala.html` it associates the HTML format to the file.

In summary, to support your own template format you need to perform the following steps:

* Implement the text integration process for the format ;
* Associate a file extension to the format.

## Implement a format

Implement the `play.templates.Format<A>` interface that has the methods `A raw(String text)` and `A escape(String text)` that will be used to integrate static and dynamic template parts, respectively.

The type parameter `A` of the format defines the result type of the template rendering, e.g. `Html` for a HTML template. This type must be a subtype of the `play.templates.Appendable<A>` trait that defines how to concatenates parts together.

For convenience, Play provides a `play.api.templates.BufferedContent<A>` abstract class that implements `play.templates.Appendable<A>` using a `StringBuilder` to build its result and that implements the `play.mvc.Content` interface so Play knows how to serialize it as an HTTP response body.

In short, you need to write two classes: one defining the result (implementing `play.templates.Appendable<A>`) and one defining the text integration process (implementing `play.templates.Format<A>`). For instance, here is how the HTML format could be defined:

```java
public class Html extends BufferedContent<Html> {
  public Html(StringBuilder buffer) {
    super(buffer);
  }
  String contentType() {
    return "text/html";
  }
}

public class HtmlFormat implements Format<Html> {
  Html raw(String text: String) { … }
  Html escape(String text) { … }
  public static final HtmlFormat instance = new HtmlFormat(); // The build process needs a static reference to the format (see the next section)
}
```

## Associate a file extension to the format

The templates are compiled into a `.scala` files by the build process just before compiling the whole application sources. The `sbt.PlayKeys.templatesTypes` key is a sbt setting of type `Map[String, String]` defining the mapping between file extensions and template formats. For instance, if you want Play to use your own HTML format implementation you have to write the following in your build file to associate the `.scala.html` files to your custom `my.HtmlFormat` format:

```scala
templatesTypes += ("html" -> "my.HtmlFormat.instance")
```

Note that the right side of the arrow contains the fully qualified name of a static value of type `play.templates.Format<?>`.

> **Next:** [[HTTP form submission and validation | JavaForms]]
