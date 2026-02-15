package vsp.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "vsp")
public class LocationhandlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocationhandlerApplication.class, args);
    }
}
