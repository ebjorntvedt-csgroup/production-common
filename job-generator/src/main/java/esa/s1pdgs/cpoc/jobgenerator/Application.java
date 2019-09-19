package esa.s1pdgs.cpoc.jobgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * L0 job generator application
 * @author Cyrielle Gailliard
 *
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"esa.s1pdgs.cpoc.obs_sdk"})
public class Application {
	
    /**
     * Main application
     * @param args
     */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
