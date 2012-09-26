package play.db.ebean;

import com.avaje.ebean.Ebean;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import play.libs.Yaml;

import java.util.Map;

/**
 * Support for bulk loading test data into Ebean
 */
public class Fixtures {

    public static void load(String filename) {
        load(filename, "default");
    }

    /**
     * Load the fixture from the given filename for the given database.
     *
     * The file must be in YAML format, and must contain the actual types to be saved, for example:
     *
     * <code>
     *  - !!models.User
     *      name: Bob
     *      email: bob@gmail.com
     *      password: secret
     *  - !!models.Project
     *      name: Play
     * </code>
     *
     * The objects may live in any combination of maps or sequences.  YAML anchors and references
     * may be used, for example:
     *
     * <code>
     *  - &bob !!models.User
     *      name: Bob
     *  - !!models.Group
     *      members:
     *          - *bob
     * </code>
     *
     * Note that nested objects will only get saved if you have cascading configured for those properties.
     *
     * Ebean doesn't automatically save ManyToMany relationships for some reason, this method tries to implement some
     * crude support for automatically persisting ManyToMany relations, but it is not guaranteed to work in all
     * circumstances.
     *
     * @param filename The filename to load
     * @param database The database to load the fixture into
     */
    public static void load(String filename, String database) {
        Object data = Yaml.load(filename);
        persist(database, data);
    }

    private static void persist(String database, Object data) {
        if (data instanceof Iterable) {
            for (Object item : (Iterable) data) {
                persist(database, item);
            }
        } else if (data instanceof Map) {
            persist(database, ((Map)data).values());
        } else {
            // Save the object
            Ebean.save(data);

            for (BeanPropertyAssocMany many : ((SpiEbeanServer) Ebean.getServer(database))
                    .getBeanDescriptor(data.getClass()).propertiesManyToMany()) {
                Ebean.saveManyToManyAssociations(data, many.getName());
            }
        }
    }
}
