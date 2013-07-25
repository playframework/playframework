package play.db.ebean;

import java.util.*;
import java.beans.*;
import java.lang.reflect.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Query;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.PagingList;
import com.avaje.ebean.FutureRowCount;
import com.avaje.ebean.FutureList;
import com.avaje.ebean.FutureIds;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.QueryListener;
import com.avaje.ebean.QueryIterator;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Filter;
import com.avaje.ebean.QueryResultVisitor;

import play.libs.F.*;
import static play.libs.F.*;

import org.springframework.beans.*;

/**
 * Base-class for Ebean-mapped models that provides convenience methods.
 */
@javax.persistence.MappedSuperclass
public class Model {
    
    // -- Magic to dynamically access the @Id property
    
    @javax.persistence.Transient
    private Tuple<Method,Method> _idGetSet;
    
    private Tuple<Method,Method> _idAccessors() {
        if(_idGetSet == null) {
            try {
                Class<?> clazz = this.getClass();
                while(clazz != null) {
                    for(Field f:clazz.getDeclaredFields()) {
                        if(f.isAnnotationPresent(javax.persistence.Id.class) || f.isAnnotationPresent(javax.persistence.EmbeddedId.class)) {
                            PropertyDescriptor idProperty = new BeanWrapperImpl(this).getPropertyDescriptor(f.getName());
                            _idGetSet = Tuple(idProperty.getReadMethod() , idProperty.getWriteMethod());
                        }
                    }
                    clazz = clazz.getSuperclass();
                }                
                if(_idGetSet == null) {
                    throw new RuntimeException("No @javax.persistence.Id field found in class [" + this.getClass() + "]");                    
                }
            } catch(RuntimeException e) {
                throw e;
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        return _idGetSet;
    }
    
    private Object _getId() {
        try {
            return _idAccessors()._1.invoke(this);
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void _setId(Object id) {
        try {
            _idAccessors()._2.invoke(this,id);
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // --
    
    /**
     * Saves (inserts) this entity.
     */
    public void save() {
        Ebean.save(this);
    }
    
    /**
     * Saves (inserts) this entity.
     *
     * @param server the Ebean server to use
     */
    public void save(String server) {
        Ebean.getServer(server).save(this);
    }
    
    /**
     * Persist a many-to-many association.
     */
    public void saveManyToManyAssociations(String path) {
        Ebean.saveManyToManyAssociations(this, path);
    }
    
    /**
     * Persist a many-to-many association.
     *
     * @param server the Ebean server to use
     */
    public void saveManyToManyAssociations(String server, String path) {
        Ebean.getServer(server).saveManyToManyAssociations(this, path);
    }    
    
    
    /**
     * Deletes a many-to-many association
     * 
     * @parama path name of the many-to-many association we want to delete
     */
    public void deleteManyToManyAssociations(String path) {
        Ebean.deleteManyToManyAssociations(this, path);
    }
    
    /**
     * Updates this entity.
     */
    public void update() {
        Ebean.update(this);
    }
    
    /**
     * Updates this entity, using a specific Ebean server.
     *
     * @param server the Ebean server to use
     */
    public void update(String server) {
        Ebean.getServer(server).update(this);
    }
    
    /**
     * Updates this entity, by specifying the entity ID.
     */
    public void update(Object id) {
        _setId(id);
        Ebean.update(this);
    }
    
    /**
     * Updates this entity, by specifying the entity ID, using a specific Ebean server.
     *
     * @param server the Ebean server to use
     */
    public void update(Object id, String server) {
        _setId(id);
        Ebean.getServer(server).update(this);
    }
    
    /**
     * Deletes this entity.
     */
    public void delete() {
        Ebean.delete(this);
    }
    
    /**
     * Deletes this entity, using a specific Ebean server.
     *
     * @param server the Ebean server to use
     */
    public void delete(String server) {
        Ebean.getServer(server).delete(this);
    }
    
    /**
     * Refreshes this entity from the database.
     */
    public void refresh() {
        Ebean.refresh(this);
    }
    
    /**
     * Refreshes this entity from the database, using a specific Ebean server.
     *
     * @param server the Ebean server to use
     */
    public void refresh(String server) {
        Ebean.getServer(server).refresh(this);
    }
    
    @Override
    public boolean equals(Object other) {
        if(this == other) return true;
        if(other == null || other.getClass() != this.getClass()) return false;
        Object id = _getId();
        Object otherId = ((Model) other)._getId();
        if(id == null) return false;
        if(otherId == null) return false;
        return id.equals(otherId);
    }
    
    @Override
    public int hashCode() {
        Object id = _getId();
        return id == null ? super.hashCode() : id.hashCode();
    }
    
    /**
     * Helper for Ebean queries.
     *
     * @see <a href="http://www.avaje.org/static/javadoc/pub/">Ebean API documentation</a>
     */
    public static class Finder<I,T> implements Query<T> {
        
        private final Class<I> idType;
        private final Class<T> type;
        private final String serverName;

        /**
         * Creates a finder for entity of type <code>T</code> with ID of type <code>I</code>.
         */
        public Finder(Class<I> idType, Class<T> type) {
            this("default", idType, type);
        }
        
        /**
          * Creates a finder for entity of type <code>T</code> with ID of type <code>I</code>, using a specific Ebean server.
          */
        public Finder(String serverName, Class<I> idType, Class<T> type) {
            this.type = type;
            this.idType = idType;
            this.serverName = serverName;
        }
        
        private EbeanServer server() {
            return Ebean.getServer(serverName);
        }
        
        /**
         * Changes the Ebean server.
         */
        public Finder<I,T> on(String server) {
            return new Finder(server, idType, type);
        }
        
        /**
         * Retrieves all entities of the given type.
         */
        public List<T> all() {
            return server().find(type).findList();
        }

        /**
         * Retrieves an entity by ID.
         */
        public T byId(I id) {
            return server().find(type, id);
        }

        /**
         * Retrieves an entity reference for this ID.
         */
        public T ref(I id) {
             return server().getReference(type, id);
        }
        
        /**
         *  Creates a filter for sorting and filtering lists of entities locally without going back to the database.
         */
        public Filter<T> filter() {
            return server().filter(type);
        }
        
        /**
         * Creates a query.
         */
        public Query<T> query() {
            return server().find(type);
        }
        
        /**
         * Returns the next identity value.
         */
        public I nextId() {
            return (I)server().nextId(type);
        }
        
        /**
         * Cancels query execution, if supported by the underlying database and driver.
         */
        public void cancel() {
            query().cancel();
        }
        
        /**
         * Copies this query.
         */
        public Query<T> copy() {
            return query().copy();
        }
        
        /**
         * Specifies a path to load including all its properties.
         */
        public Query<T> fetch(String path) {
            return query().fetch(path);
        }
        
        /**
         * Additionally specifies a <code>JoinConfig</code> to specify a 'query join' and/or define the lazy loading query.
         */
        public Query<T> fetch(String path, FetchConfig joinConfig) {
            return query().fetch(path, joinConfig);
        }
        
        /**
         * Specifies a path to fetch with a specific list properties to include, to load a partial object.
         */
        public Query<T> fetch(String path, String fetchProperties) {
            return query().fetch(path, fetchProperties);
        }
        
        /**
         * Additionally specifies a <code>FetchConfig</code> to use a separate query or lazy loading to load this path.
         */
        public Query<T> fetch(String assocProperty, String fetchProperties, FetchConfig fetchConfig) {
            return query().fetch(assocProperty, fetchProperties, fetchConfig);
        }
        
        /**
         * Applies a filter on the 'many' property list rather than the root level objects.
         */
        public ExpressionList<T> filterMany(String propertyName) {
            return query().filterMany(propertyName);
        }

        /**
         * Executes a find IDs query in a background thread.
         */
        public FutureIds<T> findFutureIds() {
            return query().findFutureIds();
        }

        /**
         * Executes a find list query in a background thread.
         */
        public FutureList<T> findFutureList() {
            return query().findFutureList();
        }

        /**
         * Executes a find row count query in a background thread.
         */
        public FutureRowCount<T> findFutureRowCount() {
            return query().findFutureRowCount();
        }

        /**
         * Executes a query and returns the results as a list of IDs.
         */
        public List<Object> findIds() {
            return query().findIds();
        }

        /**
         * Executes the query and returns the results as a list of objects.
         */
        public List<T> findList() {
            return query().findList();
        }

        /**
         * Executes the query and returns the results as a map of objects.
         */
        public Map<?,T> findMap() {
            return query().findMap();
        }
        
        /**
         * Executes the query and returns the results as a map of the objects.
         */
        public <K> Map<K,T> findMap(String a, Class<K> b) {
            return query().findMap(a,b);
        }

        /**
         * Returns a <code>PagingList</code> for this query.
         */
        public PagingList<T> findPagingList(int pageSize) {
            return query().findPagingList(pageSize);
        }

        /**
         * Returns the number of entities this query should return.
         */
        public int findRowCount() {
            return query().findRowCount();
        }

        /**
         * Executes the query and returns the results as a set of objects.
         */
        public Set<T> findSet() {
            return query().findSet();
        }

        /**
         * Executes the query and returns the results as either a single bean or <code>null</code>, if no matching bean is found.
         */
        public T findUnique() {
            return query().findUnique();
        }
        
        public void findVisit(QueryResultVisitor<T> visitor) {
            query().findVisit(visitor);
        }
        
        public QueryIterator<T> findIterate() {
            return query().findIterate();
        }

        /**
         * Returns the <code>ExpressionFactory</code> used by this query.
         */
        public ExpressionFactory getExpressionFactory() {
            return query().getExpressionFactory();
        }

        /**
         * Returns the first row value.
         */
        public int getFirstRow() {
            return query().getFirstRow();
        }

        /**
         * Returns the SQL that was generated for executing this query.
         */
        public String getGeneratedSql() {
            return query().getGeneratedSql();
        }

        /**
         * Returns the maximum of rows for this query.
         */
        public int getMaxRows() {
            return query().getMaxRows();
        }

        /**
         * Returns the <code>RawSql</code> that was set to use for this query.
         */
        public RawSql getRawSql() {
            return query().getRawSql();
        }

        public UseIndex getUseIndex() {
            return query().getUseIndex();
        }

        /**
         * Returns the query's <code>having</code> clause.
         */
        public ExpressionList<T> having() {
            return query().having();
        }

        /**
         * Adds an expression to the <code>having</code> clause and returns the query.
         */
        public Query<T> having(com.avaje.ebean.Expression addExpressionToHaving) {
            return query().having(addExpressionToHaving);
        }

        /**
         * Adds clauses to the <code>having</code> clause and returns the query.
         */
        public Query<T> having(String addToHavingClause) {
            return query().having(addToHavingClause);
        }

        /**
         * Returns <code>true</code> if this query was tuned by <code>autoFetch</code>.
         */
        public boolean isAutofetchTuned() {
            return query().isAutofetchTuned();
        }

        /**
         * Returns the <code>order by</code> clause so that you can append an ascending or descending property to the <code>order by</code> clause.
         * <p>
         * This is exactly the same as {@link #orderBy}.
         */
        public OrderBy<T> order() {
            return query().order();
        }

        /**
         * Sets the <code>order by</code> clause, replacing the existing <code>order by</code> clause if there is one.
         * <p>
         * This is exactly the same as {@link #orderBy(String)}.
         */
        public Query<T> order(String orderByClause) {
            return query().order(orderByClause);
        }

        /**
         * Returns the <code>order by</code> clause so that you can append an ascending or descending property to the <code>order by</code> clause.
         * <p>
         * This is exactly the same as {@link #order}.
         */
        public OrderBy<T> orderBy() {
            return query().orderBy();
        }

        /**
         * Set the <code>order by</code> clause replacing the existing <code>order by</code> clause if there is one.
         * <p>
         * This is exactly the same as {@link #order(String)}.
         */
        public Query<T> orderBy(String orderByClause) {
            return query().orderBy(orderByClause);
        }

        /**
         * Explicitly sets a comma delimited list of the properties to fetch on the 'main' entity bean, to load a partial object.
         */
        public Query<T> select(String fetchProperties) {
            return query().select(fetchProperties);
        }

        /**
         * Explicitly specifies whether to use 'Autofetch' for this query.
         */
        public Query<T> setAutofetch(boolean autofetch) {
            return query().setAutofetch(autofetch);
        }

        /**
         * Sets the rows after which fetching should continue in a background thread.
         */
        public Query<T> setBackgroundFetchAfter(int backgroundFetchAfter) {
            return query().setBackgroundFetchAfter(backgroundFetchAfter);
        }

        /**
         * Sets a hint, which for JDBC translates to <code>Statement.fetchSize()</code>.
         */
        public Query<T> setBufferFetchSizeHint(int fetchSize) {
            return query().setBufferFetchSizeHint(fetchSize);
        }

        /**
         * Sets whether this query uses <code>DISTINCT</code>.
         */
        public Query<T> setDistinct(boolean isDistinct) {
            return query().setDistinct(isDistinct);
        }

        /**
         * Sets the first row to return for this query.
         */
        public Query<T> setFirstRow(int firstRow) {
            return query().setFirstRow(firstRow);
        }

        /**
         * Sets the ID value to query.
         */
        public Query<T> setId(Object id) {
            return query().setId(id);
        }

        /**
         * Sets a listener to process the query on a row-by-row basis.
         */
        public Query<T> setListener(QueryListener<T> queryListener) {
            return query().setListener(queryListener);
        }
        
        /**
         * When set to <code>true</code>, all the beans from this query are loaded into the bean cache.
         */
        public Query<T> setLoadBeanCache(boolean loadBeanCache) {
            return query().setLoadBeanCache(loadBeanCache);
        }

        /**
         * Sets the property to use as keys for a map.
         */
        public Query<T> setMapKey(String mapKey) {
            return query().setMapKey(mapKey);
        }

        /**
         * Sets the maximum number of rows to return in the query.
         */
        public Query<T> setMaxRows(int maxRows) {
            return query().setMaxRows(maxRows);
        }

        /**
         * Replaces any existing <code>order by</code> clause using an <code>OrderBy</code> object.
         * <p>
         * This is exactly the same as {@link #setOrderBy(com.avaje.ebean.OrderBy)}.
         */
        public Query<T> setOrder(OrderBy<T> orderBy) {
            return query().setOrder(orderBy);
        }

        /**
         * Set an OrderBy object to replace any existing <code>order by</code> clause.
         * <p>
         * This is exactly the same as {@link #setOrder(com.avaje.ebean.OrderBy)}.
         */
        public Query<T> setOrderBy(OrderBy<T> orderBy) {
            return query().setOrderBy(orderBy);
        }

        /**
         * Sets an ordered bind parameter according to its position.
         */
        public Query<T> setParameter(int position, Object value) {
            return query().setParameter(position, value);
        }

        /**
         * Sets a named bind parameter.
         */
        public Query<T> setParameter(String name, Object value) {
            return query().setParameter(name, value);
        }

        /**
         * Sets the OQL query to run
         */
        public Query<T> setQuery(String oql) {
            return server().createQuery(type, oql);
        }

        /**
         * Sets <code>RawSql</code> to use for this query.
         */
        public Query<T> setRawSql(RawSql rawSql) {
            return query().setRawSql(rawSql);
        }

        /**
         * Sets whether the returned beans will be read-only.
         */
        public Query<T> setReadOnly(boolean readOnly) {
            return query().setReadOnly(readOnly);
        }

        /**
         * Sets a timeout on this query.
         */
        public Query<T> setTimeout(int secs) {
            return query().setTimeout(secs);
        }

        /**
         * Sets whether to use the bean cache.
         */
        public Query<T> setUseCache(boolean useBeanCache) {
            return query().setUseCache(useBeanCache);
        }

        /**
         * Sets whether to use the query cache.
         */
        public Query<T> setUseQueryCache(boolean useQueryCache) {
            return query().setUseQueryCache(useQueryCache);
        }
        
        public Query<T> setUseIndex(UseIndex useIndex) {
            return query().setUseIndex(useIndex);
        }

        /**
         * Adds expressions to the <code>where</code> clause with the ability to chain on the <code>ExpressionList</code>.
         */
        public ExpressionList<T> where() {
            return query().where();
        }

        /**
         * Adds a single <code>Expression</code> to the <code>where</code> clause and returns the query.
         */
        public Query<T> where(com.avaje.ebean.Expression expression) {
            return query().where(expression);
        }

        /**
         * Adds additional clauses to the <code>where</code> clause.
         */
        public Query<T> where(String addToWhereClause) {
            return query().where(addToWhereClause);
        }

        /**
         * Return the total hits matched for a lucene text search query.
         */
        @Override
        public int getTotalHits() {
            return query().getTotalHits();
        }

        /**
         * Execute the select with "for update" which should lock the record "on read"
         */
        @Override
        public Query<T> setForUpdate(boolean forUpdate) {
            return query().setForUpdate(forUpdate);
        }

        /**
         * Whether this query is for update
         */
        @Override
        public boolean isForUpdate() {
            return query().isForUpdate();
        }
    }
    
}