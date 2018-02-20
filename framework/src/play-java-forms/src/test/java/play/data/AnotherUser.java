/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import java.util.*;

import play.data.validation.Constraints.Validate;
import play.data.validation.Constraints.Validatable;

import play.data.validation.ValidationError;

@Validate
public class AnotherUser implements Validatable<List<ValidationError>> {

    private String name;
    private final List<String> emails = new ArrayList<>();
    private Optional<String> company = Optional.empty();

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCompany(Optional<String> company) {
        this.company = company;
    }

    public Optional<String> getCompany() {
        return company;
    }

    public List<String> getEmails() {
        return emails;
    }

    @Override
    public List<ValidationError> validate() {
        final List<ValidationError> errors = new ArrayList<>();
        if (this.name != null && !this.name.equals("Kiki")) {
            errors.add(new ValidationError("name", "Name not correct"));
            errors.add(new ValidationError("", "Form could not be processed"));
        }
        return errors; // null or empty list are handled equal
    }

}
