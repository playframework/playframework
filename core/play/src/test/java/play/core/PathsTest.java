/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PathsTest {

  @Test
  void relativePathShouldReturnSiblingPathWithoutCommonRoot() {
    final String startPath = "/playframework";
    final String targetPath = "/one";

    assertEquals("one", Paths.relative(startPath, targetPath));
  }

  @Test
  void relativeShouldReturnSiblingPathWithoutCommonRoot() {
    final String startPath = "/one/two";
    final String targetPath = "/one/two/asset.js";

    assertEquals("two/asset.js", Paths.relative(startPath, targetPath));
  }

  @Test
  void relativeIncludeOneParentDirAndLastCommonElementOfTargetRouteWithNoTrailingSlash() {
    final String startPath = "/one/two";
    final String targetPath = "/one";

    assertEquals("../one", Paths.relative(startPath, targetPath));
  }

  @Test
  void relativePathShouldIncludeOneParentDirectoryAndNoLastCommonElement() {
    final String startPath = "/one/two/";
    final String targetPath = "/one/";

    assertEquals("../", Paths.relative(startPath, targetPath));
  }

  @Test
  void relativePathShouldIncludeTwoParentDirectory() {
    final String startPath = "/one/two";
    final String targetPath = "/one-b/two-b";

    assertEquals("../one-b/two-b", Paths.relative(startPath, targetPath));
  }

  @Test
  void relativePathShouldNoCommonRootSegmentsAndIncludeThreeParentDirectories() {
    final String startPath = "/one/two/three";
    final String targetPath = "/one-b/two-b/asset.js";

    assertEquals("../../one-b/two-b/asset.js", Paths.relative(startPath, targetPath));
  }

  @Test
  void relativePathShouldHaveTwoCommonRootSegmentsAndIncludeTwoParentDirectories() {
    final String startPath = "/one/two/three/four";
    final String targetPath = "/one/two/three-b/four-b/asset.js";

    assertEquals("../three-b/four-b/asset.js", Paths.relative(startPath, targetPath));
  }

  @Test
  void relativePathShouldRetainTrailingForwardSlashIfItExistsInCall() {
    final String startPath = "/one/two";
    final String targetPath = "/one/two-c/";

    assertEquals("two-c/", Paths.relative(startPath, targetPath));
  }

  @Test
  void relativePathReturnCurrentDir() {
    final String startPath = "/one/two";
    final String targetPath = "/one/two";

    assertEquals(".", Paths.relative(startPath, targetPath));
  }

  @Test
  void relativePathReturnCurrentDirIncludeFourParentDirectories() {
    final String startPath = "/one/two//three/../three-b/./four/";
    final String targetPath = "/one-b//two-b/./";

    assertEquals("../../../../one-b/two-b/", Paths.relative(startPath, targetPath));
  }

  @Test
  void canonicalPathReturnHandlesParentDirectories() {
    final String targetPath = "/one/two/../two-b/three";

    assertEquals("/one/two-b/three", Paths.canonical(targetPath));
  }

  @Test
  void canonicalPathHandlesCurrentDirectories() {
    final String targetPath = "/one/two/./three";

    assertEquals("/one/two/three", Paths.canonical(targetPath));
  }

  @Test
  void canonicalPathHandlesMultipleDirectorySeparators() {
    final String targetPath = "/one/two//three";

    assertEquals("/one/two/three", Paths.canonical(targetPath));
  }
}
