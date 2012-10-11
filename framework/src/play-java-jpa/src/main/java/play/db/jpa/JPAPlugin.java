package play.db.jpa;

import play.*;

import java.io.*;
import java.util.*;

import javax.persistence.*;

/**
 * A Play plugin that automatically manages JPA configuration.
 */
public class JPAPlugin extends Plugin {
    
    private final Application application;
    
    public JPAPlugin(Application application) {
        this.application = application;
    }
    
    // --
    
    private final Map<String,EntityManagerFactory> emfs = new HashMap<String,EntityManagerFactory>();
    
    /**
     * Reads the configuration file and initialises required JPA EntityManagerFactories.
     */
    public void onStart() {

        Configuration jpaConf = Configuration.root().getConfig("jpa");
        
        if(jpaConf != null) {
            for(String key: jpaConf.keys()) {
                String persistenceUnit = jpaConf.getString(key);
                emfs.put(key, Persistence.createEntityManagerFactory(persistenceUnit));
            }
        }
        
    }
    
    private boolean isPluginDisabled() {
        String status =  application.configuration().getString("jpaplugin");
        return status != null && status.equals("disabled");
    }   

    @Override
    public boolean enabled() { 
        return isPluginDisabled() == false;
    }
     
    public void onStop() {
        
        for(EntityManagerFactory emf: emfs.values()) {
            emf.close();
        }
        
    }
    
    public EntityManager em(String key) {
        EntityManagerFactory emf = emfs.get(key);
        if(emf == null) {
            return null;
        }
        return emf.createEntityManager();
    }
    
}