/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import javax.persistence.EntityManager;

/**
 * JPA API.
 */
public interface JPAApi {

    public EntityManager em(String key);

}
