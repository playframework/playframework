/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import play.data.validation.Constraints;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TypeArgumentForm {

    private List<@Constraints.Min(0) Integer> list;

    private Map<@Constraints.MinLength(3) String, @Constraints.Min(6) Integer> map;

    private Optional<@Constraints.MinLength(9) String> optional;

    public List<Integer> getList() {
        return list;
    }

    public void setList(final List<Integer> list) {
        this.list = list;
    }

    public Map<String, Integer> getMap() {
        return map;
    }

    public void setMap(final Map<String, Integer> map) {
        this.map = map;
    }

    public Optional<String> getOptional() {
        return optional;
    }

    public void setOptional(final Optional<String> optional) {
        this.optional = optional;
    }
}
