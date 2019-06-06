/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.Router;
import play.test.Helpers;
import play.test.WithApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertThat;

public class JavaFileUploadTest extends WithApplication {

  @Override
  protected Application provideApplication() {
    Router router = Router.empty();
    play.api.inject.guice.GuiceApplicationBuilder scalaBuilder =
        new play.api.inject.guice.GuiceApplicationBuilder().additionalRouter(router.asScala());
    return GuiceApplicationBuilder.fromScalaBuilder(scalaBuilder).build();
  }

  // #testSyncUpload
  @Test
  public void testFileUpload() throws IOException {
    File file = getFile();
    Http.MultipartFormData.Part<Source<ByteString, ?>> part =
        new Http.MultipartFormData.FilePart<>(
            "picture", "file.pdf", "application/pdf", FileIO.fromFile(file));

    // ###replace:     Http.RequestBuilder request =
    // Helpers.fakeRequest().uri(routes.MyController.upload().url())
    Http.RequestBuilder request =
        Helpers.fakeRequest()
            .uri("/upload")
            .method("POST")
            .header(Http.HeaderNames.CONTENT_TYPE, "multipart/form-data")
            .bodyMultipart(
                Collections.singletonList(part),
                play.libs.Files.singletonTemporaryFileCreator(),
                app.getWrappedApplication().materializer());

    Result result = Helpers.route(app, request);
    String content = Helpers.contentAsString(result);
    // ###replace:     assertThat(content, CoreMatchers.equalTo("File uploaded"));
    assertThat(content, CoreMatchers.containsString("Action Not Found"));
  }
  // #testSyncUpload

  private File getFile() throws IOException {
    String filePath = "/tmp/data/file.pdf";
    java.nio.file.Path tempFilePath = Files.createTempFile(null, null);
    byte[] expectedData = filePath.getBytes();
    Files.write(tempFilePath, expectedData);

    return tempFilePath.toFile();
  }
}
