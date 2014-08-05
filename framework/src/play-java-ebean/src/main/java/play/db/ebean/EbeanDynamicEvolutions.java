/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.ebean;

import play.api.db.evolutions.DynamicEvolutions;
import play.api.libs.Files;
import play.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.avaje.ebean.*;
import com.avaje.ebean.config.*;
import com.avaje.ebeaninternal.server.ddl.*;
import com.avaje.ebeaninternal.api.*;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A Play module that automatically manages Ebean configuration.
 */
@Singleton
public class EbeanDynamicEvolutions extends DynamicEvolutions {

    private final EbeanConfig config;
    private final Environment environment;

    private final Map<String, EbeanServer> servers = new HashMap<String, EbeanServer>();

    @Inject
    public EbeanDynamicEvolutions(EbeanConfig config, Environment environment) {
        this.config = config;
        this.environment = environment;
        start();
    }

    /**
     * Initialise the Ebean servers.
     */
    public void start() {
        for (Map.Entry<String, ServerConfig> entry : config.serverConfigs().entrySet()) {
            String key = entry.getKey();
            ServerConfig serverConfig = entry.getValue();
            servers.put(key, EbeanServerFactory.create(serverConfig));
        }
    }

    /**
     * Generate evolutions.
     */
    @Override
    public void create() {
        if (!environment.isProd()) {
            for (Map.Entry<String, ServerConfig> entry : config.serverConfigs().entrySet()) {
                String key = entry.getKey();
                ServerConfig serverConfig = entry.getValue();
                String evolutionScript = generateEvolutionScript(servers.get(key), serverConfig);
                if (evolutionScript != null) {
                    File evolutions = environment.getFile("conf/evolutions/" + key + "/1.sql");
                    if (!evolutions.exists() || Files.readFile(evolutions).startsWith("# --- Created by Ebean DDL")) {
                        Files.createDirectory(environment.getFile("conf/evolutions/" + key));
                        Files.writeFileIfChanged(evolutions, evolutionScript);
                    }
                }
            }
        }
    }

    /**
     * Helper method that generates the required evolution to properly run Ebean.
     */
    public static String generateEvolutionScript(EbeanServer server, ServerConfig config) {
        DdlGenerator ddl = new DdlGenerator();
        ddl.setup((SpiEbeanServer)server, config.getDatabasePlatform(), config);
        String ups = ddl.generateCreateDdl();
        String downs = ddl.generateDropDdl();

        if(ups == null || ups.trim().isEmpty()) {
            return null;
        }

        return (
            "# --- Created by Ebean DDL\r\n" +
            "# To stop Ebean DDL generation, remove this comment and start using Evolutions\r\n" +
            "\r\n" +
            "# --- !Ups\r\n" +
            "\r\n" +
            ups +
            "\r\n" +
            "# --- !Downs\r\n" +
            "\r\n" +
            downs
        );
    }

}
