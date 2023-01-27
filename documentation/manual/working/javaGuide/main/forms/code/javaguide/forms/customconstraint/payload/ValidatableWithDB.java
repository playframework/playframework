/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.customconstraint.payload;

// #interface
import play.data.validation.Constraints.ValidationPayload;
import play.db.Database;

public interface ValidatableWithDB<T> {
  public T validate(final Database db, final ValidationPayload payload);
}
// #interface
