package vsp.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "vsp")
public class ExternalRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExternalRestApplication.class, args);
    }
}
