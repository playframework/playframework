/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data;

import java.util.*;

import play.data.validation.Constraints.SelfValidatingAdvanced;
import play.data.validation.Constraints.ValidatableAdvanced;

import play.data.validation.ValidationError;

@SelfValidatingAdvanced
public class AnotherUser implements ValidatableAdvanced {

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
    public List<ValidationError> validateInstance() {
        final List<ValidationError> errors = new ArrayList<>();
        if (this.name != null && !this.name.equals("Kiki")) {
            errors.add(new ValidationError("name", "Name not correct"));
            errors.add(new ValidationError("", "Form could not be processed"));
        }
        return errors; // null or empty list are handled equal
    }

}
