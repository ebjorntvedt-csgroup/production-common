package esa.s1pdgs.cpoc.inbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import esa.s1pdgs.cpoc.inbox.config.InboxPollingConfigurationProperties;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableJpaRepositories("esa.s1pdgs.cpoc.inbox.polling.repo")
@EnableConfigurationProperties(InboxPollingConfigurationProperties.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
