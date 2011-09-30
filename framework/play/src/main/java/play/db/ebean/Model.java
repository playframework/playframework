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
    
    public static class Finder<I,T> {
        
        private final Class<I> idType;
        private final Class<T> type;
        private final String serverName;
        
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
        
        public Query<T> where(String addToWhereClause) {
            return server().find(type).where(addToWhereClause);
        }
        
        public Query<T> join(String path) {
            return server().find(type).join(path);
        }
        
        public Query<T> fetch(String path) {
            return server().find(type).fetch(path);
        }
        
        
    }
    
}