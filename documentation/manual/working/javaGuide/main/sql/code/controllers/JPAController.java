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
        return ok("A Transactional action");
    }
    //#jpa-controller-transactional-action

    //#jpa-controller-transactional-readonly
    @Transactional(readOnly = true)
    public Result list() {
        return ok("A Transactional action");
    }
    //#jpa-controller-transactional-readonly

    //#jpa-access-entity-manager
    public void upadateSomething() {
        EntityManager em = jpaApi.em();
        // do something with the entity manager, per instance
        // save, update or query model objects.
    }
    //#jpa-access-entity-manager

    public void runningWithTransaction() {
        //#jpa-withTransaction-function
        // lambda is an instance of Function<EntityManager, Long>
        jpaApi.withTransaction(entityManager -> {
            Query query = entityManager.createNativeQuery("select max(age) from people");
            return (Long) query.getSingleResult();
        });
        //#jpa-withTransaction-function

        //#jpa-withTransaction-runnable
        // lambda is an instance of Runnable
        jpaApi.withTransaction(() -> {
            EntityManager em = jpaApi.em();
            Query query = em.createNativeQuery("update people set active = 1 where age > 18");
            query.executeUpdate();
        });
        //#jpa-withTransaction-runnable
    }
}