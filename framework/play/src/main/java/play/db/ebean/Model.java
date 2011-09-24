package play.db.ebean;

import java.util.*;

import com.avaje.ebean.*;

@javax.persistence.MappedSuperclass
public class Model {
    
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