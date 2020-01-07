/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.customconstraint.nopayload;

// #interface
import play.db.Database;

public interface ValidatableWithDB<T> {
  public T validate(final Database db);
}
// #interface
