/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import play.data.format.Formats.DateTime;
import play.data.validation.Constraints;
import play.data.validation.TestConstraints.AnotherI18NConstraint;
import play.data.validation.TestConstraints.I18Constraint;

import java.util.Date;
import java.util.List;

public class Subtask {

  @Constraints.Min(10)
  public Long id;

  @Constraints.Required public String name;

  public Boolean done = true;

  @Constraints.Required
  @DateTime(pattern = "dd/MM/yyyy")
  public Date dueDate;

  public Date endDate;

  @I18Constraint(value = "patterns.zip")
  public String zip;

  @AnotherI18NConstraint(value = "patterns.zip")
  public String anotherZip;

  public List<String> emails;
}
