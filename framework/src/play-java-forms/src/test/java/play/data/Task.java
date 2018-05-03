/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import java.util.Date;

import play.data.format.Formats.DateTime;

import play.data.validation.Constraints;
import play.data.validation.TestConstraints.I18Constraint;
import play.data.validation.TestConstraints.AnotherI18NConstraint;

public class Task {

    @Constraints.Min(10)
    private Long id;

    @Constraints.Required
    private String name;

    private Boolean done = true;

    @Constraints.Required
    @DateTime(pattern = "dd/MM/yyyy")
    private Date dueDate;

    private Date endDate;

    @I18Constraint(value = "patterns.zip")
    private String zip;

    @AnotherI18NConstraint(value = "patterns.zip")
    private String anotherZip;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getAnotherZip() {
        return anotherZip;
    }

    public void setAnotherZip(String anotherZip) {
        this.anotherZip = anotherZip;
    }

}
