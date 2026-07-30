/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import play.mvc.Http.MultipartFormData;

public class SanitizedFilenameTest {
  @Test
  public void sanitizeSingleComponent() {
    MultipartFormData.FilePart<Object> p =
        new MultipartFormData.FilePart<Object>(null, "abc", null, null);
    assertEquals("abc", p.getSanitizedFilename());
  }

  @Test
  public void sanitizeMultipleComponents() {
    MultipartFormData.FilePart<Object> p =
        new MultipartFormData.FilePart<Object>(null, "abc/def/xyz", null, null);
    assertEquals("xyz", p.getSanitizedFilename());
  }

  @Test
  public void sanitizeWithTrailingDots() {
    MultipartFormData.FilePart<Object> p =
        new MultipartFormData.FilePart<Object>(null, "a/b/c/././", null, null);
    assertEquals("c", p.getSanitizedFilename());
  }

  @Test
  public void sanitizeWithLeadingDoubleDots() {
    MultipartFormData.FilePart<Object> p =
        new MultipartFormData.FilePart<Object>(null, "../../../a", null, null);
    assertEquals("a", p.getSanitizedFilename());
  }

  @Test
  public void sanitizeWithNameAfterDoubleDots() {
    MultipartFormData.FilePart<Object> p =
        new MultipartFormData.FilePart<Object>(null, "../../../a/../b", null, null);
    assertEquals("b", p.getSanitizedFilename());
  }

  @Test
  public void sanitizeWithTrailingDoubleDots() {
    MultipartFormData.FilePart<Object> p =
        new MultipartFormData.FilePart<Object>(null, "a/b/c/../..", null, null);
    assertEquals("a", p.getSanitizedFilename());
  }

  @Test
  public void sanitizeWithRedundantSlashesAndDots() {
    MultipartFormData.FilePart<Object> p =
        new MultipartFormData.FilePart<Object>(null, "///a//b/c/.././d/././/", null, null);
    assertEquals("d", p.getSanitizedFilename());
  }

  @Test(expected = RuntimeException.class)
  public void sanitizeThrowsOnEmptyPath() {
    (new MultipartFormData.FilePart<Object>(null, "", null, null)).getSanitizedFilename();
  }

  @Test(expected = RuntimeException.class)
  public void sanitizeThrowsOnCurrentDirectory() {
    (new MultipartFormData.FilePart<Object>(null, ".", null, null)).getSanitizedFilename();
  }

  @Test(expected = RuntimeException.class)
  public void sanitizeThrowsOnDoubleDots() {
    (new MultipartFormData.FilePart<Object>(null, "..", null, null)).getSanitizedFilename();
  }

  @Test(expected = RuntimeException.class)
  public void sanitizeThrowsPastRoot() {
    (new MultipartFormData.FilePart<Object>(null, "a/b/../../..", null, null))
        .getSanitizedFilename();
  }

  @Test(expected = RuntimeException.class)
  public void sanitizeThrowsOnParentAfterResolving() {
    (new MultipartFormData.FilePart<Object>(null, "../a/..", null, null)).getSanitizedFilename();
  }
}
