/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PathsTest {

  @Test
  public void relativePathShouldReturnSiblingPathWithoutCommonRoot() {
    final String startPath = "/playframework";
    final String targetPath = "/one";

    assertEquals("one", Paths.relative(startPath, targetPath));
  }

  @Test
  public void relativeShouldReturnSiblingPathWithoutCommonRoot() {
    final String startPath = "/one/two";
    final String targetPath = "/one/two/asset.js";

    assertEquals("two/asset.js", Paths.relative(startPath, targetPath));
  }

  @Test
  public void relativeIncludeOneParentDirAndLastCommonElementOfTargetRouteWithNoTrailingSlash() {
    final String startPath = "/one/two";
    final String targetPath = "/one";

    assertEquals("../one", Paths.relative(startPath, targetPath));
  }

  @Test
  public void relativePathShouldIncludeOneParentDirectoryAndNoLastCommonElement() {
    final String startPath = "/one/two/";
    final String targetPath = "/one/";

    assertEquals("../", Paths.relative(startPath, targetPath));
  }

  @Test
  public void relativePathShouldIncludeTwoParentDirectory() {
    final String startPath = "/one/two";
    final String targetPath = "/one-b/two-b";

    assertEquals("../one-b/two-b", Paths.relative(startPath, targetPath));
  }

  @Test
  public void relativePathShouldNoCommonRootSegmentsAndIncludeThreeParentDirectories() {
    final String startPath = "/one/two/three";
    final String targetPath = "/one-b/two-b/asset.js";

    assertEquals("../../one-b/two-b/asset.js", Paths.relative(startPath, targetPath));
  }

  @Test
  public void relativePathShouldHaveTwoCommonRootSegmentsAndIncludeTwoParentDirectories() {
    final String startPath = "/one/two/three/four";
    final String targetPath = "/one/two/three-b/four-b/asset.js";

    assertEquals("../three-b/four-b/asset.js", Paths.relative(startPath, targetPath));
  }

  @Test
  public void relativePathShouldRetainTrailingForwardSlashIfItExistsInCall() {
    final String startPath = "/one/two";
    final String targetPath = "/one/two-c/";

    assertEquals("two-c/", Paths.relative(startPath, targetPath));
  }

  @Test
  public void relativePathReturnCurrentDir() {
    final String startPath = "/one/two";
    final String targetPath = "/one/two";

    assertEquals(".", Paths.relative(startPath, targetPath));
  }

  @Test
  public void relativePathReturnCurrentDirIncludeFourParentDirectories() {
    final String startPath = "/one/two//three/../three-b/./four/";
    final String targetPath = "/one-b//two-b/./";

    assertEquals("../../../../one-b/two-b/", Paths.relative(startPath, targetPath));
  }

  @Test
  public void canonicalPathReturnHandlesParentDirectories() {
    final String targetPath = "/one/two/../two-b/three";

    assertEquals("/one/two-b/three", Paths.canonical(targetPath));
  }

  @Test
  public void canonicalPathHandlesCurrentDirectories() {
    final String targetPath = "/one/two/./three";

    assertEquals("/one/two/three", Paths.canonical(targetPath));
  }

  @Test
  public void canonicalPathHandlesMultipleDirectorySeparators() {
    final String targetPath = "/one/two//three";

    assertEquals("/one/two/three", Paths.canonical(targetPath));
  }
}
