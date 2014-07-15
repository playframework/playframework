/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import java.io.File

/**
 * The environment for the application.
 *
 * Captures concerns relating to the classloader and the filesystem for the application.
 *
 * @param rootPath The root path that the application is deployed at.
 * @param classLoader The classloader that all application classes and resources can be loaded from.
 * @param mode The mode of the application.
 */
case class Environment(
  rootPath: File,
  classLoader: ClassLoader,
  mode: Mode.Mode)
