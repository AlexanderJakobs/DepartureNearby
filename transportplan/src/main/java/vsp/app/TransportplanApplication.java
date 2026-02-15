package vsp.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application für Transportplan.
 */
@SpringBootApplication(scanBasePackages = "vsp")
public class TransportplanApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransportplanApplication.class, args);
    }
}
