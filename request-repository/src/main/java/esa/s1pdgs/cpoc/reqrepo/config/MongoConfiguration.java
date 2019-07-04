/**
 * 
 */
package esa.s1pdgs.cpoc.reqrepo.config;


import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

/**
 * Configuration for the MongoDB client
 * 
 * @author Viveris Technologies
 */
@Configuration
public class MongoConfiguration {

    /**
     * Logger
     */
    private static final Logger LOGGER =
            LogManager.getLogger(MongoConfiguration.class);

    @Value("${mongodb.host}")
    private List<String> mongoDBHost;

    @Value("${mongodb.port}")
    private int mongoDBPort;
    
    @Value("${mongodb.database}")
    private String mongoDBDatabase;

    @Bean
    public MongoClient mongoClient() {
        LOGGER.info("New constructor");
        List<ServerAddress> servers = new ArrayList<>();
        mongoDBHost.forEach(host -> {
            servers.add(new ServerAddress(host, mongoDBPort));
        });
        return new MongoClient(servers);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), mongoDBDatabase);
    }  
}
