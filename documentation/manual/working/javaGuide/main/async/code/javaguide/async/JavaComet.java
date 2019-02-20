/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async;

import com.fasterxml.jackson.databind.node.ObjectNode;
import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import org.junit.Test;

// #comet-imports
import akka.stream.javadsl.Source;
import play.core.j.JavaHandlerComponents;
import play.libs.Comet;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
// #comet-imports

import play.test.WithApplication;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

public class JavaComet extends WithApplication {

  public static class Controller1 extends MockJavaAction {

    Controller1(JavaHandlerComponents javaHandlerComponents) {
      super(javaHandlerComponents);
    }

    // #comet-string
    public static Result index() {
      final Source source = Source.from(Arrays.asList("kiki", "foo", "bar"));
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
      final Source source = Source.from(Collections.singletonList(objectNode));
      return ok().chunked(source.via(Comet.json("parent.cometMessage"))).as(Http.MimeTypes.HTML);
    }
    // #comet-json
  }

  @Test
  public void foreverIframe() {
    String content =
        contentAsString(
            MockJavaActionHelper.call(
                new Controller1(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat),
            mat);
    assertThat(
        content,
        containsString("<script type=\"text/javascript\">parent.cometMessage('kiki');</script>"));
    assertThat(
        content,
        containsString("<script type=\"text/javascript\">parent.cometMessage('foo');</script>"));
    assertThat(
        content,
        containsString("<script type=\"text/javascript\">parent.cometMessage('bar');</script>"));
  }

  @Test
  public void foreverIframeWithJson() {
    String content =
        contentAsString(
            MockJavaActionHelper.call(
                new Controller2(app.injector().instanceOf(JavaHandlerComponents.class)),
                fakeRequest(),
                mat),
            mat);
    assertThat(
        content,
        containsString(
            "<script type=\"text/javascript\">parent.cometMessage({\"foo\":\"bar\"});</script>"));
  }
}
