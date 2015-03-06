/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing.sird

import play.api.mvc.PathBindable

/**
 * An extractor that extracts from a String using a [[PathBindable]].
 */
class PathBindableExtractor[T](implicit pb: PathBindable[T]) {
  self =>

  /**
   * Extract s to T if it can be bound, otherwise don't match.
   */
  def unapply(s: String): Option[T] = {
    pb.bind("anon", s).right.toOption
  }

  /**
   * Extract Option[T] only if s is None, Some value that can be bound, otherwise don't match.
   */
  def unapply(s: Option[String]): Option[Option[T]] = {
    s match {
      case None => Some(None)
      case Some(self(value)) => Some(Some(value))
      case _ => None
    }
  }

  /**
   * Extract Seq[T] only if ever element of s can be bound, otherwise don't match.
   */
  def unapply(s: Seq[String]): Option[Seq[T]] = {
    val bound = s.collect {
      case self(value) => value
    }
    if (bound.length == s.length) {
      Some(bound)
    } else {
      None
    }
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