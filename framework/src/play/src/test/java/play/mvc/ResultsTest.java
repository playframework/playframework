/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import play.mvc.Http.HeaderNames;

import static org.junit.Assert.*;

public class ResultsTest {

  private static Path file;

  @BeforeClass
  public static void createFile() throws Exception {
    file = Paths.get("test.tmp");
    Files.createFile(file);
    Files.write(file, "Some content for the file".getBytes(), StandardOpenOption.APPEND);
  }

  @AfterClass
  public static void deleteFile() throws IOException {
    Files.deleteIfExists(file);
  }

  // -- Path tests

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionIfPathIsNull() throws IOException {
    Results.ok().sendPath(null);
  }

  @Test
  public void sendPathWithOKStatus() throws IOException {
    Result result = Results.ok().sendPath(file);
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathWithUnauthorizedStatus() throws IOException {
    Result result = Results.unauthorized().sendPath(file);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathAsAttachmentWithUnauthorizedStatus() throws IOException {
    Result result = Results.unauthorized().sendPath(file, /*inline*/ false);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathAsAttachmentWithOkStatus() throws IOException {
    Result result = Results.unauthorized().sendPath(file, /* inline */ false);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathWithFileName() throws IOException {
    Result result = Results.unauthorized().sendPath(file, "foo.bar");
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendPathInlineWithFileName() throws IOException {
    Result result = Results.unauthorized().sendPath(file, true, "foo.bar");
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  // -- File tests

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionIfFileIsNull() throws IOException {
    Results.ok().sendFile(null);
  }

  @Test
  public void sendFileWithOKStatus() throws IOException {
    Result result = Results.ok().sendFile(file.toFile());
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileWithUnauthorizedStatus() throws IOException {
    Result result = Results.unauthorized().sendFile(file.toFile());
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileAsAttachmentWithUnauthorizedStatus() throws IOException {
    Result result = Results.unauthorized().sendFile(file.toFile(), /* inline */ false);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileAsAttachmentWithOkStatus() throws IOException {
    Result result = Results.unauthorized().sendFile(file.toFile(), /* inline */ false);
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendFileWithFileName() throws IOException {
    Result result = Results.unauthorized().sendFile(file.toFile(), "foo.bar");
    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }

  @Test
  public void sendFileInlineWithFileName() throws IOException {
    Result result = Results.ok().sendFile(file.toFile(), true, "foo.bar");
    assertEquals(result.status(), Http.Status.OK);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"foo.bar\"");
  }
}
