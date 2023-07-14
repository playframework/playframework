/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import static org.junit.jupiter.api.Assertions.assertTrue;

import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.Router;
import play.test.Helpers;
import play.test.junit5.ApplicationExtension;

public class JavaFileUploadTest {

  @RegisterExtension
  static ApplicationExtension appExtension = new ApplicationExtension(createApplication());

  static Application app = appExtension.getApplication();

  static Application createApplication() {
    Router router = Router.empty();
    play.api.inject.guice.GuiceApplicationBuilder scalaBuilder =
        new play.api.inject.guice.GuiceApplicationBuilder().additionalRouter(router.asScala());
    return GuiceApplicationBuilder.fromScalaBuilder(scalaBuilder).build();
  }

  // #testSyncUpload
  @Test
  void testFileUpload() throws IOException {
    File file = getFile();
    Http.MultipartFormData.Part<Source<ByteString, ?>> part =
        new Http.MultipartFormData.FilePart<>(
            "picture",
            "file.pdf",
            "application/pdf",
            FileIO.fromPath(file.toPath()),
            Files.size(file.toPath()));

    Http.RequestBuilder request =
        Helpers.fakeRequest()
            // ###replace:             .uri(routes.MyController.upload().url())
            .uri("/upload")
            .method("POST")
            .bodyRaw(
                Collections.singletonList(part),
                play.libs.Files.singletonTemporaryFileCreator(),
                app.asScala().materializer());

    Result result = Helpers.route(app, request);
    String content = Helpers.contentAsString(result);
    assertTrue(content.contains("Action Not Found"));
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
