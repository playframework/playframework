/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import static java.util.stream.Collectors.toList;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class TestEntity {

  @Id public Long id;

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
