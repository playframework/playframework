/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import play.mvc.Http.HeaderNames;

import static org.junit.Assert.*;

public class ResultsTest {

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionIfPathIsNull() throws IOException {
    Results.ok().sendPath(null);
  }

  @Test
  public void sendPathWithOKStatus() throws IOException {
    Path file = Paths.get("test.tmp");
    Files.createFile(file);
    Result result = Results.ok().sendPath(file);
    Files.delete(file);

    assertEquals(result.status(), Http.Status.OK);
    assertEquals("attachment; filename=\"test.tmp\"", result.header(HeaderNames.CONTENT_DISPOSITION).get());
  }

  @Test
  public void sendPathWithUnauthorizedStatus() throws IOException {
    Path file = Paths.get("test.tmp");
    Files.createFile(file);
    Result result = Results.unauthorized().sendPath(file);
    Files.delete(file);

    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathInlineWithUnauthorizedStatus() throws IOException {
    Path file = Paths.get("test.tmp");
    Files.createFile(file);
    Result result = Results.unauthorized().sendPath(file, true);
    Files.delete(file);

    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "inline; filename=\"test.tmp\"");
  }

  @Test
  public void sendPathWithFileName() throws IOException {
    Path file = Paths.get("test.tmp");
    Files.createFile(file);
    Result result = Results.unauthorized().sendPath(file, false, "foo.bar");
    Files.delete(file);

    assertEquals(result.status(), Http.Status.UNAUTHORIZED);
    assertEquals(result.header(HeaderNames.CONTENT_DISPOSITION).get(), "attachment; filename=\"foo.bar\"");
  }
}
