/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data;

import java.util.*;

public class AnotherUser {

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

}
