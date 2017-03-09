/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.data;

import play.data.validation.Constraints;

import java.util.List;

import javax.validation.Valid;

public class ListForm {

    @Valid
    private List<@Constraints.Min(0) Integer> values;

    public List<Integer> getValues() {
        return values;
    }

    public void setValues(final List<Integer> values) {
        this.values = values;
    }
}
