/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.async;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// #comet-imports
import akka.NotUsed;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import play.core.j.JavaHandlerComponents;
import play.libs.Comet;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
// #comet-imports

import play.test.junit5.ApplicationExtension;
import play.Application;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.fakeApplication;

public class JavaComet {

  @RegisterExtension
  static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());
  static Application app = appExtension.getApplication();
  static Materializer mat = appExtension.getMaterializer();

  public static class Controller1 extends MockJavaAction {

    Controller1(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #comet-string
    public static Result index() {
      final Source<String, NotUsed> source = Source.from(Arrays.asList("kiki", "foo", "bar"));
      return ok().chunked(source.via(Comet.string("parent.cometMessage"))).as(Http.MimeTypes.HTML);
    }
    // #comet-string
  }

  public static class Controller2 extends MockJavaAction {

    Controller2(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #comet-json
    public static Result index() {
      final ObjectNode objectNode = Json.newObject();
      objectNode.put("foo", "bar");
      final Source<JsonNode, NotUsed> source = Source.from(Collections.singletonList(objectNode));
      return ok().chunked(source.via(Comet.json("parent.cometMessage"))).as(Http.MimeTypes.HTML);
    }
    // #comet-json
  }

  @Test
  void foreverIframe() {
    String content =
        contentAsString(
            MockJavaActionHelper.call(
                new Controller1(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat),
            mat);
    assertTrue(content.contains("<script>parent.cometMessage('kiki');</script>"));
    assertTrue(content.contains("<script>parent.cometMessage('foo');</script>"));
    assertTrue(content.contains("<script>parent.cometMessage('bar');</script>"));
  }

  @Test
  void foreverIframeWithJson() {
    String content =
        contentAsString(
            MockJavaActionHelper.call(
                new Controller2(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat),
            mat);
    assertTrue(content.contains("<script>parent.cometMessage({\"foo\":\"bar\"});</script>"));
  }
}
