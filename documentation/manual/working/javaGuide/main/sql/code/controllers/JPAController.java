/*
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package controllers;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.EntityManager;

import play.mvc.*;
//#jpa-controller-transactional-imports
import play.db.jpa.Transactional;
//#jpa-controller-transactional-imports

//#jpa-controller-api-imports
import play.db.jpa.JPAApi;
//#jpa-controller-api-imports

public class JPAController extends Controller {

    private JPAApi jpaApi;

    //#jpa-controller-api-inject
    @Inject
    public JPAController(JPAApi api) {
        this.jpaApi = api;
    }
    //#jpa-controller-api-inject

    //#jpa-controller-transactional-action
    @Transactional
    public Result index() {
        EntityManager em = play.db.jpa.JPA.em(ctx());
        // do something with the entity manager, per instance
        // save, update or query model objects.
        return ok("A Transactional action");
    }
    //#jpa-controller-transactional-action

    //#jpa-controller-transactional-readonly
    @Transactional(readOnly = true)
    public Result list() {
        EntityManager em = play.db.jpa.JPA.em(ctx());
        // query model objects with the entity manager
        return ok("A Transactional read-only action");
    }
    //#jpa-controller-transactional-readonly

    //#jpa-withTransaction-function
    // No @Transactional annotation
    public Result querySomething() {
        // lambda is an instance of Function<EntityManager, Long>
        final Long maxAge = jpaApi.withTransaction(entityManager -> {
            Query query = entityManager.createNativeQuery("select max(age) from people");
            return (Long) query.getSingleResult();
        });
        return ok("Max age: " + maxAge);
    }
    //#jpa-withTransaction-function

    //#jpa-withTransaction-consumer
    // No @Transactional annotation
    public Result updateSomething() {
        // lambda is an instance of Consumer<EntityManager>
        jpaApi.withTransaction(entityManager -> {
            Query query = entityManager.createNativeQuery("update people set active = 1 where age > 18");
            query.executeUpdate();
        });
        return ok("Sucessfully updated");
    }
    //#jpa-withTransaction-consumer
}