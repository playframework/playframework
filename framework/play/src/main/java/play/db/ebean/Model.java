package play.db.ebean;

import java.util.*;
import java.beans.*;
import java.lang.reflect.*;

import com.avaje.ebean.*;

import play.libs.F.*;
import static play.libs.F.*;

import org.springframework.beans.*;

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
    
    public void save() {
        Ebean.save(this);
    }
    
    public void save(String server) {
        Ebean.getServer(server).save(this);
    }
    
    public void update() {
        Ebean.update(this);
    }
    
    public void update(String server) {
        Ebean.getServer(server).update(this);
    }
    
    public void update(Object id) {
        _setId(id);
        Ebean.update(this);
    }
    
    public void update(Object id, String server) {
        _setId(id);
        Ebean.getServer(server).update(this);
    }
    
    public void delete() {
        Ebean.delete(this);
    }
    
    public void delete(String server) {
        Ebean.getServer(server).delete(this);
    }
    
    public void refresh() {
        Ebean.refresh(this);
    }
    
    public void refresh(String server) {
        Ebean.getServer(server).refresh(this);
    }
    
    public static class Finder<I,T> implements Query<T> {
        
        private final Class<I> idType;
        private final Class<T> type;
        private final String serverName;
        private Query<T> query;
        
        public Finder(Class<I> idType, Class<T> type) {
            this("default", idType, type);
        }
        
        public Finder(String serverName, Class<I> idType, Class<T> type) {
            this.type = type;
            this.idType = idType;
            this.serverName = serverName;
        }
        
        private EbeanServer server() {
            return Ebean.getServer(serverName);
        }
        
        public Finder<I,T> on(String server) {
            return new Finder(server, idType, type);
        }
        
        public List<T> all() {
            return server().find(type).findList();
        }

        public T byId(I id) {
            return server().find(type, id);
        }

        public T ref(I id) {
             return server().getReference(type, id);
        }
        
        public Filter<T> filter() {
            return server().filter(type);
        }
        
        public Query<T> query() {
            return server().find(type);
        }
        
        public I nextId() {
            return (I)server().nextId(type);
        }
        
        public void cancel() {
            query().cancel();
        }
        
        public Query<T> copy() {
            return query().copy();
        }
        
        public Query<T> fetch(String path) {
            return query().fetch(path);
        }
        
        public Query<T> fetch(String path, FetchConfig joinConfig) {
            return query().fetch(path, joinConfig);
        }
        
        public Query<T> fetch(String path, String fetchProperties) {
            return query().fetch(path, fetchProperties);
        }
        
        public Query<T> fetch(String assocProperty, String fetchProperties, FetchConfig fetchConfig) {
            return query().fetch(assocProperty, fetchProperties, fetchConfig);
        }
        
        public ExpressionList<T> filterMany(String propertyName) {
            return query().filterMany(propertyName);
        }

        public FutureIds<T> findFutureIds() {
            return query().findFutureIds();
        }

        public FutureList<T> findFutureList() {
            return query().findFutureList();
        }

        public FutureRowCount<T> findFutureRowCount() {
            return query().findFutureRowCount();
        }

        public List<Object> findIds() {
            return query().findIds();
        }

        public List<T> findList() {
            return query().findList();
        }

        public Map<?,T> findMap() {
            return query().findMap();
        }
        
        public <K> Map<K,T> findMap(String a, Class<K> b) {
            return query().findMap(a,b);
        }

        public PagingList<T> findPagingList(int pageSize) {
            return query().findPagingList(pageSize);
        }

        public int findRowCount() {
            return query().findRowCount();
        }

        public Set<T> findSet() {
            return query().findSet();
        }

        public T findUnique() {
            return query().findUnique();
        }
        
        public void findVisit(QueryResultVisitor<T> visitor) {
            query().findVisit(visitor);
        }
        
        public QueryIterator<T> findIterate() {
            return query().findIterate();
        }

        public ExpressionFactory getExpressionFactory() {
            return query().getExpressionFactory();
        }

        public int getFirstRow() {
            return query().getFirstRow();
        }

        public String getGeneratedSql() {
            return query().getGeneratedSql();
        }

        public int getMaxRows() {
            return query().getMaxRows();
        }

        public RawSql getRawSql() {
            return query().getRawSql();
        }

        public Query.Type getType() {
            return query().getType();
        }
        
        public UseIndex getUseIndex() {
            return query().getUseIndex();
        }

        public ExpressionList<T> having() {
            return query().having();
        }

        public Query<T> having(com.avaje.ebean.Expression addExpressionToHaving) {
            return query().having(addExpressionToHaving);
        }

        public Query<T> having(String addToHavingClause) {
            return query().having(addToHavingClause);
        }

        public boolean isAutofetchTuned() {
            return query().isAutofetchTuned();
        }

        public Query<T> join(String path) {
            return query().join(path);
        }

        public Query<T> join(String path, JoinConfig joinConfig) {
            return query().join(path, joinConfig);
        }

        public Query<T> join(String assocProperty, String fetchProperties) {
            return query().join(assocProperty, fetchProperties);
        }

        public Query<T> join(String assocProperty, String fetchProperties, JoinConfig joinConfig) {
            return query().join(assocProperty, fetchProperties, joinConfig);
        }

        public OrderBy<T> order() {
            return query().order();
        }

        public Query<T> order(String orderByClause) {
            return query().order(orderByClause);
        }

        public OrderBy<T> orderBy() {
            return query().orderBy();
        }

        public Query<T> orderBy(String orderByClause) {
            return query().orderBy(orderByClause);
        }

        public Query<T> select(String fetchProperties) {
            return query().select(fetchProperties);
        }

        public Query<T> setAutofetch(boolean autofetch) {
            return query().setAutofetch(autofetch);
        }

        public Query<T> setBackgroundFetchAfter(int backgroundFetchAfter) {
            return query().setBackgroundFetchAfter(backgroundFetchAfter);
        }

        public Query<T> setBufferFetchSizeHint(int fetchSize) {
            return query().setBufferFetchSizeHint(fetchSize);
        }

        public Query<T> setDistinct(boolean isDistinct) {
            return query().setDistinct(isDistinct);
        }

        public Query<T> setFirstRow(int firstRow) {
            return query().setFirstRow(firstRow);
        }

        public Query<T> setId(Object id) {
            return query().setId(id);
        }

        public Query<T> setListener(QueryListener<T> queryListener) {
            return query().setListener(queryListener);
        }

        public Query<T> setLoadBeanCache(boolean loadBeanCache) {
            return query().setLoadBeanCache(loadBeanCache);
        }

        public Query<T> setMapKey(String mapKey) {
            return query().setMapKey(mapKey);
        }

        public Query<T> setMaxRows(int maxRows) {
            return query().setMaxRows(maxRows);
        }

        public Query<T> setOrder(OrderBy<T> orderBy) {
            return query().setOrder(orderBy);
        }

        public Query<T> setOrderBy(OrderBy<T> orderBy) {
            return query().setOrderBy(orderBy);
        }

        public Query<T> setParameter(int position, Object value) {
            return query().setParameter(position, value);
        }

        public Query<T> setParameter(String name, Object value) {
            return query().setParameter(name, value);
        }

        public Query<T> setQuery(String oql) {
            return query().setQuery(oql);
        }

        public Query<T> setRawSql(RawSql rawSql) {
            return query().setRawSql(rawSql);
        }

        public Query<T> setReadOnly(boolean readOnly) {
            return query().setReadOnly(readOnly);
        }

        public Query<T> setTimeout(int secs) {
            return query().setTimeout(secs);
        }

        public Query<T> setUseCache(boolean useBeanCache) {
            return query().setUseCache(useBeanCache);
        }

        public Query<T> setUseQueryCache(boolean useQueryCache) {
            return query().setUseQueryCache(useQueryCache);
        }
        
        public Query<T> setUseIndex(UseIndex useIndex) {
            return query().setUseIndex(useIndex);
        }

        public Query<T> setVanillaMode(boolean vanillaMode) {
            return query().setVanillaMode(vanillaMode);
        }

        public ExpressionList<T> where() {
            return query().where();
        }

        public Query<T> where(com.avaje.ebean.Expression expression) {
            return query().where(expression);
        }

        public Query<T> where(String addToWhereClause) {
            return query().where(addToWhereClause);
        }
        
    }
    
}