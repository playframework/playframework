/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import java.nio.file.FileSystemException
import java.nio.file.Files

import org.specs2.execute.AsResult
import org.specs2.execute.Result
import org.specs2.execute.Skipped
import org.specs2.mutable.Specification

class ExternalAssetsSpec extends Specification {
  "ExternalAssets.resolveFile" should {
    val root = new java.io.File("target/external-assets-root").toPath.toAbsolutePath.normalize

    "accept normalized paths within the configured root" in {
      ExternalAssets.resolveFile(root.toFile, "nested/../asset.txt") must beSome(root.resolve("asset.txt").toFile)
    }

    "reject paths outside the configured root" in {
      ExternalAssets.resolveFile(root.toFile, "../secret.txt") must beNone
      ExternalAssets.resolveFile(root.toFile, "../external-assets-root-private/secret.txt") must beNone
      ExternalAssets.resolveFile(root.toFile, root.resolveSibling("secret.txt").toString) must beNone
    }

    "allow a path below the root to traverse a symbolic link" in {
      val parent  = Files.createTempDirectory("external-assets-spec")
      val root    = Files.createDirectory(parent.resolve("root"))
      val outside = Files.createDirectory(parent.resolve("outside"))
      val asset   = Files.writeString(outside.resolve("asset.txt"), "linked asset")
      val link    = root.resolve("link")

      val result: Result =
        try {
          Files.createSymbolicLink(link, outside)
          val resolved = ExternalAssets.resolveFile(root.toFile, "link/asset.txt")

          AsResult(
            (resolved must beSome(link.resolve("asset.txt").toFile))
              .and(resolved.map(_.toPath.toRealPath()) must beSome(asset.toRealPath()))
          )
        } catch {
          case _: UnsupportedOperationException => Skipped("Symbolic links are not supported by this file system")
          case _: FileSystemException           => Skipped("Symbolic links cannot be created in this environment")
        } finally {
          Files.deleteIfExists(link)
          Files.deleteIfExists(asset)
          Files.deleteIfExists(outside)
          Files.deleteIfExists(root)
          Files.deleteIfExists(parent)
        }

      result
    }
  }
}
