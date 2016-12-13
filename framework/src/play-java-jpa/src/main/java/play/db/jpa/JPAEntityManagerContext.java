package play.db.jpa;

import play.mvc.Http;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class JPAEntityManagerContext {

    private static final String CURRENT_ENTITY_MANAGER = "entityManagerContext";

    /**
     * Get the default EntityManager for the current Http.Context.
     *
     * @throws NullPointerException if no EntityManager is bound to the current Http.Context.
     * @return the EntityManager
     * 
     * @deprecated Use {@link #em(play.mvc.Http.Context)} instead
     */
    @Deprecated
    public static EntityManager em() {
        return em(Http.Context.current());
    }

    /**
     * Get the default EntityManager from the given Http.Context.
     *
     * @throws NullPointerException if no EntityManager is bound to the given Http.Context.
     * @return the EntityManager
     */
    public static EntityManager em(final Http.Context ctx) {
        final Deque<EntityManager> ems = emStack(ctx);

        if (ems.isEmpty()) {
            throw new RuntimeException("No EntityManager found in given Http.Context. Try to annotate your action method with @play.db.jpa.Transactional");
        }

        return ems.peekFirst();
    }

    /**
     * Get the EntityManager stack.
     * 
     * @throws NullPointerException if no EntityManager is bound to the given Http.Context.
     */
    @SuppressWarnings("unchecked")
    public static Deque<EntityManager> emStack(final Http.Context context) {
        Objects.requireNonNull(context, "No Http.Context is present. If you want to invoke this method outside of a HTTP request, you need to wrap the call with jpaApi.withTransaction instead.");

        final Object emsObject = context.args.get(CURRENT_ENTITY_MANAGER);
        if (emsObject != null) {
            return (Deque<EntityManager>) emsObject;
        } else {
            final Deque<EntityManager> ems = new ArrayDeque<>();
            context.args.put(CURRENT_ENTITY_MANAGER, ems);
            return ems;
        }
    }

    public static void push(final EntityManager em, final Http.Context context) {
        final Deque<EntityManager> ems = emStack(context);
        if (em != null) {
            ems.push(em);
        }
    }

    public static void pop(final Http.Context context) {
        final Deque<EntityManager> ems = emStack(context);
        if (ems.isEmpty()) {
            throw new IllegalStateException("Tried to remove the EntityManager from the given Http.Context, but none was set.");
        }
        ems.pop();
    }

    public static void closeAllTransactionsAndEntityManagers(final Http.Context context, final boolean doRollback) {
        final Deque<EntityManager> ems = emStack(context);
        
        while(!ems.isEmpty()) {
            final EntityManager em = ems.getFirst();
            if(doRollback && em.getTransaction() != null && em.getTransaction().isActive()) {
                em.getTransaction().setRollbackOnly();
            }
            closeTransactionAndEntityManager(em);
            ems.pop(); // Remove EntityManager from stack
        }
    }

    public static void closeTransactionAndEntityManager(final EntityManager em) {
        final EntityTransaction tx = em.getTransaction();
        if (tx != null && tx.isActive()) {
            if(tx.getRollbackOnly()) {
                tx.rollback();
            } else {
                tx.commit();
            }
        }
        if(em.isOpen()) {
            em.close();
        }
    }

}
