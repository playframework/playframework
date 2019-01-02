/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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

    public void save(EntityManager em) {
        em.persist(this);
    }

    public void delete(EntityManager em) {
        em.remove(this);
    }

    public static TestEntity find(Long id, EntityManager em) {
        return em.find(TestEntity.class, id);
    }

    public static List<String> allNames(EntityManager em) {
        @SuppressWarnings("unchecked")
        List<TestEntity> results = em.createQuery("from TestEntity order by name").getResultList();
        return results.stream().map(entity -> entity.name).collect(toList());
    }

}
