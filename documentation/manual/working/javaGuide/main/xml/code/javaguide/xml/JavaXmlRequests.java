/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.xml;

import org.w3c.dom.Document;
import play.libs.XPath;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class JavaXmlRequests extends Controller {
  // #xml-hello
  public Result sayHello(Http.Request request) {
    Document dom = request.body().asXml();
    if (dom == null) {
      return badRequest("Expecting Xml data");
    } else {
      String name = XPath.selectText("//name", dom);
      if (name == null) {
        return badRequest("Missing parameter [name]");
      } else {
        return ok("Hello " + name);
      }
    }
  }

  // #xml-hello

  // #xml-hello-bodyparser
  @BodyParser.Of(BodyParser.Xml.class)
  public Result sayHelloBP(Http.Request request) {
    Document dom = request.body().asXml();
    if (dom == null) {
      return badRequest("Expecting Xml data");
    } else {
      String name = XPath.selectText("//name", dom);
      if (name == null) {
        return badRequest("Missing parameter [name]");
      } else {
        return ok("Hello " + name);
      }
    }
  }

  // #xml-hello-bodyparser

  // #xml-reply
  @BodyParser.Of(BodyParser.Xml.class)
  public Result replyHello(Http.Request request) {
    Document dom = request.body().asXml();
    if (dom == null) {
      return badRequest("Expecting Xml data");
    } else {
      String name = XPath.selectText("//name", dom);
      if (name == null) {
        return badRequest("<message \"status\"=\"KO\">Missing parameter [name]</message>")
            .as("application/xml");
      } else {
        return ok("<message \"status\"=\"OK\">Hello " + name + "</message>").as("application/xml");
      }
    }
  }
  // #xml-reply
}
