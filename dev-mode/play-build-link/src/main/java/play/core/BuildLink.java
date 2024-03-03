/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core;

import java.io.File;
import java.util.Map;

/**
 * Interface used by the Play build plugin to communicate with an embedded Play server. BuildLink
 * objects are created by the plugin's run command and provided to Play's NettyServer devMode
 * methods.
 *
 * <p>This interface is written in Java and uses only Java types so that communication can work even
 * when the plugin and embedded Play server are built with different versions of Scala.
 */
public interface BuildLink {

  /**
   * Check if anything has changed, and if so, return an updated classloader.
   *
   * <p>This method is called multiple times on every request, so it is advised that change
   * detection happens asynchronously to this call, and that this call just check a boolean.
   *
   * @return Either
   *     <ul>
   *       <li>Throwable - If something went wrong (eg, a compile error). {@link
   *           play.api.PlayException} and its sub types can be used to provide specific details on
   *           compile errors or other exceptions.
   *       <li>ClassLoader - If the classloader has changed, and the application should be reloaded.
   *       <li>null - If nothing changed.
   *     </ul>
   */
  Object reload();

  /**
   * Find the original source file for the given class name and line number.
   *
   * <p>When the application throws an exception, this will be called for every element in the stack
   * trace from top to bottom until a source file may be found, so that the browser can render the
   * line of code that threw the exception.
   *
   * <p>If the class is generated (eg a template), then the original source file should be returned,
   * and the line number should be mapped back to the line number in the original source file, if
   * possible.
   *
   * @param className The name of the class to find the source for.
   * @param line The line number the exception was thrown at.
   * @return Either:
   *     <ul>
   *       <li>[File, Integer] - The source file, and the passed in line number, if the source
   *           wasn't generated, or if it was generated, and the line number could be mapped, then
   *           the original source file and the mapped line number.
   *       <li>[File, null] - If the source was generated but the line number couldn't be mapped,
   *           then just the original source file and null for the unmappable line number.
   *       <li>null - If no source file could be found for the class name.
   *     </ul>
   */
  Object[] findSource(String className, Integer line);

  /**
   * Get the path of the project. This is used by methods such as {@code
   * play.api.Application#getFile}.
   *
   * @return The path of the project.
   */
  File projectPath();

  /**
   * Force the application to reload on the next invocation of reload.
   *
   * <p>This is invoked by plugins for example that change something on the classpath or something
   * about the application that requires a reload, for example, the evolutions plugin.
   */
  void forceReload();

  /**
   * Returns a list of application settings configured in the build system, usually via {@code
   * PlayKeys.devSettings}.
   *
   * @return The settings.
   */
  Map<String, String> settings();
}
