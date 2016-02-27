package play.db.jpa;

import play.mvc.Http;

import javax.persistence.EntityManager;
import java.util.ArrayDeque;
import java.util.Deque;

public class JPAEntityManagerContext extends ThreadLocal<Deque<EntityManager>> {

    private static final String CURRENT_ENTITY_MANAGER = "entityManagerContext";

    @Override
    public Deque<EntityManager> initialValue() {
        return new ArrayDeque<>();
    }

    /**
     * Get the default EntityManager for this thread.
     *
     * @throws RuntimeException if no EntityManager is bound to the current Http.Context or the current Thread.
     * @return the EntityManager
     */
    public EntityManager em() {
        Http.Context context = Http.Context.current.get();
        Deque<EntityManager> ems = this.emStack(true);

        if (ems.isEmpty()) {
            if (context != null) {
                throw new RuntimeException("No EntityManager found in the context. Try to annotate your action method with @play.db.jpa.Transactional");
            } else {
                throw new RuntimeException("No EntityManager bound to this thread. Try wrapping this call in JPAApi.withTransaction, or ensure that the HTTP context is setup on this thread.");
            }
        }

        return ems.peekFirst();
    }

    /**
     * Get the EntityManager stack.
     */
    @SuppressWarnings("unchecked")
    public Deque<EntityManager> emStack(boolean threadLocalFallback) {
        Http.Context context = Http.Context.current.get();
        if (context != null) {
            Object emsObject = context.args.get(CURRENT_ENTITY_MANAGER);
            if (emsObject != null) {
                return (Deque<EntityManager>) emsObject;
            } else {
                Deque<EntityManager> ems = new ArrayDeque<>();
                context.args.put(CURRENT_ENTITY_MANAGER, ems);
                return ems;
            }
        } else {
            // Not a web request
            if (threadLocalFallback) {
                return this.get();
            } else {
                throw new RuntimeException("No Http.Context is present. If you want to invoke this method outside of a HTTP request, you need to wrap the call with JPA.withTransaction instead.");
            }
        }
    }

    public void push(EntityManager em, boolean threadLocalFallback) {
        Deque<EntityManager> ems = this.emStack(threadLocalFallback);
        if (em != null) {
            ems.push(em);
        }
    }

    public void pop(boolean threadLocalFallback) {
        Deque<EntityManager> ems = this.emStack(threadLocalFallback);
        if (ems.isEmpty()) {
            throw new IllegalStateException("Tried to remove the EntityManager, but none was set.");
        }
        ems.pop();
    }

    /**
     * Pushes or pops the EntityManager stack depending on the value of the
     * em argument. If em is null, then the current EntityManager is popped. If em
     * is non-null, then em is pushed onto the stack and becomes the current EntityManager.
     *
     * @deprecated use push or pop methods
     */
    @Deprecated
    public void pushOrPopEm(EntityManager em, boolean threadLocalFallback) {
        Deque<EntityManager> ems = this.emStack(threadLocalFallback);
        if (em != null) {
            ems.push(em);
        } else {
            if (ems.isEmpty()) {
                throw new IllegalStateException("Tried to remove the EntityManager, but none was set.");
            }
            ems.pop();
        }
    }
}