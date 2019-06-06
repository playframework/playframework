/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.customconstraint;

// #interface
import play.db.Database;

public interface ValidatableWithDB<T> {
  public T validate(final Database db);
}
// #interface
