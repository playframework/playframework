/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing.sird

import play.api.mvc.PathBindable

/**
 * An extractor that extracts from a String using a [[PathBindable]].
 */
class PathBindableExtractor[T](implicit pb: PathBindable[T]) {
  def unapply(s: String): Option[T] = {
    pb.bind("anon", s).right.toOption
  }
}

/**
 * Extractors that bind types from paths using [[PathBindable]].
 */
trait PathBindableExtractors {
  /**
   * An int extractor.
   */
  val int = new PathBindableExtractor[Int]

  /**
   * A long extractor.
   */
  val long = new PathBindableExtractor[Long]

  /**
   * A boolean extractor.
   */
  val bool = new PathBindableExtractor[Boolean]

  /**
   * A float extractor.
   */
  val float = new PathBindableExtractor[Float]

  /**
   * A double extractor.
   */
  val double = new PathBindableExtractor[Double]
}