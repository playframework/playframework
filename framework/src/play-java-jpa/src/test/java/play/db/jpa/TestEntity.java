/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import java.util.*;
import javax.persistence.*;

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
        List<String> names = new ArrayList<String>();
        for (TestEntity entity : results) {
            names.add(entity.name);
        }
        return names;
    }

}
