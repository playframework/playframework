/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import java.util.Date;
import java.util.List;
import play.data.format.Formats.DateTime;
import play.data.validation.Constraints;
import play.data.validation.TestConstraints.AnotherI18NConstraint;
import play.data.validation.TestConstraints.I18Constraint;

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
