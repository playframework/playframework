package play.db.jpa;

import play.mvc.*;
import play.mvc.Http.*;

import javax.persistence.*;

/**
 * Wraps an action in am JPA transaction.
 */
public class TransactionalAction extends Action<Transactional> {
    
    public Result call(final Context ctx) throws Throwable {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            
            String name = configuration.value();
            em = JPA.em(name);
            JPA.bindForCurrentThread(em);
            
            if(!configuration.readOnly()) {
                tx = em.getTransaction();
                tx.begin();
            }
            
            Result result = delegate.call(ctx);
            
            if(tx != null) {
                if(tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
            
            return result;
            
        } catch(Throwable t) {
            if(tx != null) {
                tx.rollback();
            }
            throw t;
        } finally {
            JPA.bindForCurrentThread(null);
            if(em != null) {
                em.close();
            }
        }
    }
    
}
