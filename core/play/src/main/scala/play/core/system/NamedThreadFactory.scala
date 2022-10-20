/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread factory that creates threads that are named.  Threads will be named with the format:
 *
 * {name}-{threadNo}
 *
 * where threadNo is an integer starting from one.
 */
case class NamedThreadFactory(name: String) extends ThreadFactory {
  val threadNo             = new AtomicInteger()
  val backingThreadFactory = Executors.defaultThreadFactory()

  def newThread(r: Runnable) = {
    val thread = backingThreadFactory.newThread(r)
    thread.setName(name + "-" + threadNo.incrementAndGet())
    thread
  }
}
