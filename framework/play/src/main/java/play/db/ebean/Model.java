package play.db.ebean;

import java.util.*;
import java.beans.*;
import java.lang.reflect.*;

import com.avaje.ebean.*;

import play.libs.F.*;
import static play.libs.F.*;

import org.springframework.beans.*;

/**
 * Convenient super-class for Ebean mapped models.
 */
@javax.persistence.MappedSuperclass
public class Model {
    
    // -- Magic to dynamically access the @Id property
    
    @javax.persistence.Transient
    private T2<Method,Method> _idGetSet;
    
    private T2<Method,Method> _idAccessors() {
        if(_idGetSet == null) {
            try {
                Class<?> clazz = this.getClass();
                while(clazz != null) {
                    for(Field f:clazz.getDeclaredFields()) {
                        if(f.isAnnotationPresent(javax.persistence.Id.class)) {
                            PropertyDescriptor idProperty = new BeanWrapperImpl(this).getPropertyDescriptor(f.getName());
                            _idGetSet = T2(idProperty.getReadMethod() , idProperty.getWriteMethod());
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
     * Save (insert) this entity.
     */
    public void save() {
        Ebean.save(this);
    }
    
    /**
     * Save (insert) this entity.
     *
     * @param server The Ebean server to use.
     */
    public void save(String server) {
        Ebean.getServer(server).save(this);
    }
    
    /**
     * Update this entity.
     */
    public void update() {
        Ebean.update(this);
    }
    
    /**
     * Update this entity.
     *
     * @param server The Ebean server to use.
     */
    public void update(String server) {
        Ebean.getServer(server).update(this);
    }
    
    /**
     * Update this entity, by specifying the entity id.
     */
    public void update(Object id) {
        _setId(id);
        Ebean.update(this);
    }
    
    /**
     * Update this entity, by specifying the entity id.
     *
     * @param server The Ebean server to use.
     */
    public void update(Object id, String server) {
        _setId(id);
        Ebean.getServer(server).update(this);
    }
    
    /**
     * Delete this entity.
     */
    public void delete() {
        Ebean.delete(this);
    }
    
    /**
     * Delete this entity.
     *
     * @param server The Ebean server to use.
     */
    public void delete(String server) {
        Ebean.getServer(server).delete(this);
    }
    
    /**
     * Refresh this entity from the db.
     */
    public void refresh() {
        Ebean.refresh(this);
    }
    
    /**
     * Refresh this entity from the db.
     *
     * @param server The Ebean server to use.
     */
    public void refresh(String server) {
        Ebean.getServer(server).refresh(this);
    }
    
    /**
     * Helper for Ebean queries.
     *
     * For detailled documentation, check the Ebean Javadoc.
     */
    public static class Finder<I,T> implements Query<T> {
        
        private final Class<I> idType;
        private final Class<T> type;
        private final String serverName;
        private Query<T> query;
        
        /**
         * Create a finder for entity of type T with id of type I.
         */
        public Finder(Class<I> idType, Class<T> type) {
            this("default", idType, type);
        }
        
        /**
          * Create a finder for entity of type T with id of type I using a specific Ebean server.
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
         * Change the Ebean server.
         */
        public Finder<I,T> on(String server) {
            return new Finder(server, idType, type);
        }
        
        /**
         * Retrieve all entities of the given type.
         */
        public List<T> all() {
            return server().find(type).findList();
        }

        /**
         * Retrieve an entity by Id.
         */
        public T byId(I id) {
            return server().find(type, id);
        }

        /**
         * Retrieve an entity reference for this Id.
         */
        public T ref(I id) {
             return server().getReference(type, id);
        }
        
        /**
         *  Create a filter for sorting and filtering lists of entities locally without going back to the database.
         */
        public Filter<T> filter() {
            return server().filter(type);
        }
        
        /**
         * Create a query.
         */
        public Query<T> query() {
            return server().find(type);
        }
        
        /**
         * Return the next identity value.
         */
        public I nextId() {
            return (I)server().nextId(type);
        }
        
        /**
         * Cancel the query execution if supported by the underlying database and driver.
         */
        public void cancel() {
            query().cancel();
        }
        
        /**
         * Copy this query.
         */
        public Query<T> copy() {
            return query().copy();
        }
        
        /**
         * Specify a path to load including all its properties.
         */
        public Query<T> fetch(String path) {
            return query().fetch(path);
        }
        
        /**
         * Additionally specify a JoinConfig to specify a "query join" and or define the lazy loading query.
         */
        public Query<T> fetch(String path, FetchConfig joinConfig) {
            return query().fetch(path, joinConfig);
        }
        
        /**
         * Specify a path to fetch with its specific properties to include (aka partial object).
         */
        public Query<T> fetch(String path, String fetchProperties) {
            return query().fetch(path, fetchProperties);
        }
        
        /**
         * Additionally specify a FetchConfig to use a separate query or lazy loading to load this path.
         */
        public Query<T> fetch(String assocProperty, String fetchProperties, FetchConfig fetchConfig) {
            return query().fetch(assocProperty, fetchProperties, fetchConfig);
        }
        
        /**
         * This applies a filter on the 'many' property list rather than the root level objects.
         */
        public ExpressionList<T> filterMany(String propertyName) {
            return query().filterMany(propertyName);
        }

        /**
         * Execute find Id's query in a background thread.
         */
        public FutureIds<T> findFutureIds() {
            return query().findFutureIds();
        }

        /**
         * Execute find list query in a background thread.
         */
        public FutureList<T> findFutureList() {
            return query().findFutureList();
        }

        /**
         * Execute find row count query in a background thread.
         */
        public FutureRowCount<T> findFutureRowCount() {
            return query().findFutureRowCount();
        }

        /**
         * Execute the query returning the list of Id's.
         */
        public List<Object> findIds() {
            return query().findIds();
        }

        /**
         * Execute the query returning the list of objects.
         */
        public List<T> findList() {
            return query().findList();
        }

        /**
         * Execute the query returning a map of the objects.
         */
        public Map<?,T> findMap() {
            return query().findMap();
        }
        
        /**
         * Execute the query returning a map of the objects.
         */
        public <K> Map<K,T> findMap(String a, Class<K> b) {
            return query().findMap(a,b);
        }

        /**
         * Return a PagingList for this query.
         */
        public PagingList<T> findPagingList(int pageSize) {
            return query().findPagingList(pageSize);
        }

        /**
         * Return the count of entities this query should return.
         */
        public int findRowCount() {
            return query().findRowCount();
        }

        /**
         * Execute the query returning the set of objects.
         */
        public Set<T> findSet() {
            return query().findSet();
        }

        /**
         * Execute the query returning either a single bean or null (if no matching bean is found).
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
         * Return the ExpressionFactory used by this query.
         */
        public ExpressionFactory getExpressionFactory() {
            return query().getExpressionFactory();
        }

        /**
         * Return the first row value.
         */
        public int getFirstRow() {
            return query().getFirstRow();
        }

        /**
         * Return the sql that was generated for executing this query.
         */
        public String getGeneratedSql() {
            return query().getGeneratedSql();
        }

        /**
         * Return the max rows for this query.
         */
        public int getMaxRows() {
            return query().getMaxRows();
        }

        /**
         * Return the RawSql that was set to use for this query.
         */
        public RawSql getRawSql() {
            return query().getRawSql();
        }

        /**
         * Return the type of query (List, Set, Map, Bean, rowCount etc).
         */
        public Query.Type getType() {
            return query().getType();
        }
        
        public UseIndex getUseIndex() {
            return query().getUseIndex();
        }

        /**
         * Add Expressions to the Having clause return the ExpressionList.
         */
        public ExpressionList<T> having() {
            return query().having();
        }

        /**
         * Add an expression to the having clause returning the query.
         */
        public Query<T> having(com.avaje.ebean.Expression addExpressionToHaving) {
            return query().having(addExpressionToHaving);
        }

        /**
         * 
         */
        public Query<T> having(String addToHavingClause) {
            return query().having(addToHavingClause);
        }

        /**
         * Add additional clause(s) to the having clause.
         */
        public boolean isAutofetchTuned() {
            return query().isAutofetchTuned();
        }

        /**
         * Same as fetch(String)
         */
        public Query<T> join(String path) {
            return query().join(path);
        }

        /**
         * Same as fetch(String, FetchConfig)
         */
        public Query<T> join(String path, JoinConfig joinConfig) {
            return query().join(path, joinConfig);
        }

        /**
         * Same as fetch(String, String).
         */
        public Query<T> join(String assocProperty, String fetchProperties) {
            return query().join(assocProperty, fetchProperties);
        }

        /**
         * Additionally specify a JoinConfig to specify a "query join" and or define the lazy loading query.
         */
        public Query<T> join(String assocProperty, String fetchProperties, JoinConfig joinConfig) {
            return query().join(assocProperty, fetchProperties, joinConfig);
        }

        /**
         * Return the OrderBy so that you can append an ascending or descending property to the order by clause.
         */
        public OrderBy<T> order() {
            return query().order();
        }

        /**
         * Set the order by clause replacing the existing order by clause if there is one.
         */
        public Query<T> order(String orderByClause) {
            return query().order(orderByClause);
        }

        /**
         * Return the OrderBy so that you can append an ascending or descending property to the order by clause.
         */
        public OrderBy<T> orderBy() {
            return query().orderBy();
        }

        /**
         * Set the order by clause replacing the existing order by clause if there is one.
         */
        public Query<T> orderBy(String orderByClause) {
            return query().orderBy(orderByClause);
        }

        /**
         * Explicitly set a comma delimited list of the properties to fetch on the 'main' entity bean (aka partial object).
         */
        public Query<T> select(String fetchProperties) {
            return query().select(fetchProperties);
        }

        /**
         * Explicitly specify whether to use Autofetch for this query.
         */
        public Query<T> setAutofetch(boolean autofetch) {
            return query().setAutofetch(autofetch);
        }

        /**
         * Set the rows after which fetching should continue in a background thread.
         */
        public Query<T> setBackgroundFetchAfter(int backgroundFetchAfter) {
            return query().setBackgroundFetchAfter(backgroundFetchAfter);
        }

        /**
         * A hint which for JDBC translates to the Statement.fetchSize().
         */
        public Query<T> setBufferFetchSizeHint(int fetchSize) {
            return query().setBufferFetchSizeHint(fetchSize);
        }

        /**
         * 
         */
        public Query<T> setDistinct(boolean isDistinct) {
            return query().setDistinct(isDistinct);
        }

        /**
         * Set whether this query uses DISTINCT.
         */
        public Query<T> setFirstRow(int firstRow) {
            return query().setFirstRow(firstRow);
        }

        /**
         * Set the Id value to query.
         */
        public Query<T> setId(Object id) {
            return query().setId(id);
        }

        /**
         * Set a listener to process the query on a row by row basis.
         */
        public Query<T> setListener(QueryListener<T> queryListener) {
            return query().setListener(queryListener);
        }
        
        /**
         * When set to true all the beans from this query are loaded into the bean cache.
         */
        public Query<T> setLoadBeanCache(boolean loadBeanCache) {
            return query().setLoadBeanCache(loadBeanCache);
        }

        /**
         * Set the property to use as keys for a map.
         */
        public Query<T> setMapKey(String mapKey) {
            return query().setMapKey(mapKey);
        }

        /**
         * Set the maximum number of rows to return in the query.
         */
        public Query<T> setMaxRows(int maxRows) {
            return query().setMaxRows(maxRows);
        }

        /**
         * Set an OrderBy object to replace any existing OrderBy clause.
         */
        public Query<T> setOrder(OrderBy<T> orderBy) {
            return query().setOrder(orderBy);
        }

        /**
         * Set an OrderBy object to replace any existing OrderBy clause.
         */
        public Query<T> setOrderBy(OrderBy<T> orderBy) {
            return query().setOrderBy(orderBy);
        }

        /**
         * Set an ordered bind parameter according to its position.
         */
        public Query<T> setParameter(int position, Object value) {
            return query().setParameter(position, value);
        }

        /**
         * Set a named bind parameter.
         */
        public Query<T> setParameter(String name, Object value) {
            return query().setParameter(name, value);
        }

        /**
         * Deprecated.  
         */
        public Query<T> setQuery(String oql) {
            return query().setQuery(oql);
        }

        /**
         * Set RawSql to use for this query.
         */
        public Query<T> setRawSql(RawSql rawSql) {
            return query().setRawSql(rawSql);
        }

        /**
         *  When set to true when you want the returned beans to be read only.
         */
        public Query<T> setReadOnly(boolean readOnly) {
            return query().setReadOnly(readOnly);
        }

        /**
         * Set a timeout on this query.
         */
        public Query<T> setTimeout(int secs) {
            return query().setTimeout(secs);
        }

        /**
         * Set this to true to use the bean cache.
         */
        public Query<T> setUseCache(boolean useBeanCache) {
            return query().setUseCache(useBeanCache);
        }

        /**
         * Set this to true to use the query cache.
         */
        public Query<T> setUseQueryCache(boolean useQueryCache) {
            return query().setUseQueryCache(useQueryCache);
        }
        
        public Query<T> setUseIndex(UseIndex useIndex) {
            return query().setUseIndex(useIndex);
        }

        /**
         * Set this to true and the beans and collections returned will be plain classes rather than Ebean generated dynamic subclasses etc.
         */
        public Query<T> setVanillaMode(boolean vanillaMode) {
            return query().setVanillaMode(vanillaMode);
        }

        /**
         * Add Expressions to the where clause with the ability to chain on the ExpressionList.
         */
        public ExpressionList<T> where() {
            return query().where();
        }

        /**
         * Add a single Expression to the where clause returning the query.
         */
        public Query<T> where(com.avaje.ebean.Expression expression) {
            return query().where(expression);
        }

        /**
         * Add additional clause(s) to the where clause.
         */
        public Query<T> where(String addToWhereClause) {
            return query().where(addToWhereClause);
        }
        
    }
    
}