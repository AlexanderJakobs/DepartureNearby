package vsp.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "vsp")
public class DisplaymanagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisplaymanagerApplication.class, args);
    }
}
