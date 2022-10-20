/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api;

/** A UsefulException is something useful to display in the User browser. */
public abstract class UsefulException extends RuntimeException {

  /** Exception title. */
  public String title;

  /** Exception description. */
  public String description;

  /** Exception cause if defined. */
  public Throwable cause;

  /** Unique id for this exception. */
  public String id;

  public UsefulException(String message, Throwable cause) {
    super(message, cause);
  }

  public UsefulException(String message) {
    super(message);
  }

  public String toString() {
    return "@" + id + ": " + getMessage();
  }
}
