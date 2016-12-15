/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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
import play.routing.RoutingDsl;
import play.test.Helpers;
import play.test.WithApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertThat;
import static play.mvc.Results.ok;

public class JavaFileUploadTest extends WithApplication {

    @Override
    protected Application provideApplication() {
        Router router = new RoutingDsl()
                .POST("/upload").routeTo(() -> ok("File uploaded")).build();
        play.api.inject.guice.GuiceApplicationBuilder scalaBuilder = new play.api.inject.guice.GuiceApplicationBuilder().additionalRouter(router.asScala());
        return GuiceApplicationBuilder.fromScalaBuilder(scalaBuilder).build();
    }

    //#testSyncUpload
    @Test
    public void testFileUpload() throws IOException {
        File file = getFile();
        Http.MultipartFormData.Part<Source<ByteString, ?>> part = new Http.MultipartFormData.FilePart<>("picture", "Arvind.pdf", "application/pdf", FileIO.fromFile(file));

        //###replace:     Http.RequestBuilder request = new Http.RequestBuilder().uri(routes.MyController.upload().url())
        Http.RequestBuilder request = new Http.RequestBuilder().uri("/upload")
                .method("POST")
                .header(Http.HeaderNames.CONTENT_TYPE, "multipart/form-data")
                .bodyMultipart(
                        Collections.singletonList(part),
                        play.libs.Files.singletonTemporaryFileCreator(),
                        app.getWrappedApplication().materializer()
                );

        Result result = Helpers.route(app, request);
        String content = Helpers.contentAsString(result);
        assertThat(content, CoreMatchers.equalTo("File uploaded"));
    }
    //#testSyncUpload

    private File getFile() throws IOException {
        String filePath ="/com/egnaro/data/Arvind.pdf";
        java.nio.file.Path tempFilePath = Files.createTempFile(null, null);
        byte[] expectedData = filePath.getBytes();
        Files.write(tempFilePath, expectedData);

        return tempFilePath.toFile();
    }
}
