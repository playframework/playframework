/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import java.util.*;
import javax.persistence.*;

import static java.util.stream.Collectors.toList;

@Entity
public class TestEntity {

    @Id
    public Long id;

    public String name;

    public void save() {
        JPA.em().persist(this);
    }

    public void delete() {
        JPA.em().remove(this);
    }

    public static TestEntity find(Long id) {
        return JPA.em().find(TestEntity.class, id);
    }

    public static List<String> allNames() {
        @SuppressWarnings("unchecked")
        List<TestEntity> results = JPA.em().createQuery("from TestEntity order by name").getResultList();
        return results.stream().map(entity -> entity.name).collect(toList());
    }

}
