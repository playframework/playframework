package play.db.jpa;

import play.*;
import play.libs.F;
import play.mvc.Http;
import scala.concurrent.ExecutionContext;

import javax.persistence.*;

/**
 * JPA Helpers.
 */
public class JPA {

    // Only used when there's no HTTP context
    static ThreadLocal<EntityManager> currentEntityManager = new ThreadLocal<EntityManager>();

    /**
     * Get the EntityManager for specified persistence unit for this thread.
     */
    public static EntityManager em(String key) {
        Application app = Play.application();
        if(app == null) {
            throw new RuntimeException("No application running");
        }

        JPAPlugin jpaPlugin = app.plugin(JPAPlugin.class);
        if(jpaPlugin == null) {
            throw new RuntimeException("No JPA EntityManagerFactory configured for name [" + key + "]");
        }

        EntityManager em = jpaPlugin.em(key);
        if(em == null) {
            throw new RuntimeException("No JPA EntityManagerFactory configured for name [" + key + "]");
        }

        return em;
    } 

    /**
     * Get the default EntityManager for this thread.
     */
    public static EntityManager em() {
        Http.Context context = Http.Context.current.get();
        if (context != null) {
            EntityManager em = (EntityManager) context.args.get("currentEntityManager");
            if (em == null) {
                throw new RuntimeException("No EntityManager bound to this thread. Try to annotate your action method with @play.db.jpa.Transactional");
            }
            return em;
        }
        // Not a web request
        EntityManager em = currentEntityManager.get();
        if(em == null) {
            throw new RuntimeException("No EntityManager bound to this thread. Try wrapping this call in JPA.withTransaction, or ensure that the HTTP context is setup on this thread.");
        }
        return em;
    }

    /**
     * Bind an EntityManager to the current thread.
     */
    public static void bindForCurrentThread(EntityManager em) {
        Http.Context context = Http.Context.current.get();
        if (context != null) {
            if (em == null) {
                context.args.remove("currentEntityManager");
            } else {
                context.args.put("currentEntityManager", em);
            }
        } else {
            currentEntityManager.set(em);
        }
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
    public static <T> T withTransaction(play.libs.F.Function0<T> block) throws Throwable {
        return withTransaction("default", false, block);
    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
    public static <T> F.Promise<T> withTransactionAsync(play.libs.F.Function0<F.Promise<T>> block) throws Throwable {
        return withTransactionAsync("default", false, block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
    public static void withTransaction(final play.libs.F.Callback0 block) {
        try {
            withTransaction("default", false, new play.libs.F.Function0<Void>() {
                public Void apply() throws Throwable {
                    block.invoke();
                    return null;
                }
            });
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     */
    public static <T> T withTransaction(String name, boolean readOnly, play.libs.F.Function0<T> block) throws Throwable {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {

            em = JPA.em(name);
            JPA.bindForCurrentThread(em);

            if(!readOnly) {
                tx = em.getTransaction();
                tx.begin();
            }

            T result = block.apply();

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
                try { tx.rollback(); } catch(Throwable e) {}
            }
            throw t;
        } finally {
            JPA.bindForCurrentThread(null);
            if(em != null) {
                em.close();
            }
        }
    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     */
    public static <T> F.Promise<T> withTransactionAsync(String name, boolean readOnly, play.libs.F.Function0<F.Promise<T>> block) throws Throwable {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {

            em = JPA.em(name);
            JPA.bindForCurrentThread(em);

            if(!readOnly) {
                tx = em.getTransaction();
                tx.begin();
            }

            F.Promise<T> result = block.apply();

            final EntityManager fem = em;
            final EntityTransaction ftx = tx;

            F.Promise<T> committedResult = result.map(new F.Function<T, T>() {
                @Override
                public T apply(T t) throws Throwable {
                    try {
                        if(ftx != null) {
                            if(ftx.getRollbackOnly()) {
                                ftx.rollback();
                            } else {
                                ftx.commit();
                            }
                        }
                    } finally {
                        fem.close();
                    }
                    return t;
                }
            });

            committedResult.onFailure(new F.Callback<Throwable>() {
                @Override
                public void invoke(Throwable t) {
                    if (ftx != null) {
                        try { if (ftx.isActive()) ftx.rollback(); } catch(Throwable e) {}
                    }
                    fem.close();
                }
            });

            return committedResult;

        } catch(Throwable t) {
            if(tx != null) {
                try { tx.rollback(); } catch(Throwable e) {}
            }
            if(em != null) {
                em.close();
            }
            throw t;
        } finally {
            JPA.bindForCurrentThread(null);
        }
    }
}