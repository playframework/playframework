/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.ebean;

import play.libs.F;
import play.mvc.*;
import play.mvc.Http.*;

import com.avaje.ebean.*;

/**
 * Wraps an action in an Ebean transaction.
 */
public class TransactionalAction extends Action<Transactional> {
    
    public F.Promise<Result> call(final Context ctx) throws Throwable {
        return Ebean.execute(new TxCallable<F.Promise<Result>>() {
            public F.Promise<Result> call() {
                try {
                    return delegate.call(ctx);
                } catch(RuntimeException e) {
                    throw e;
                } catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        });
    }
    
}
